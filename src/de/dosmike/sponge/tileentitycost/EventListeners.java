package de.dosmike.sponge.tileentitycost;

import de.dosmike.sponge.PlacerNBT.Placer;
import de.dosmike.sponge.PlacerNBT.PlacerData;
import de.dosmike.sponge.PlacerNBT.impl.PlacerDataImpl;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.TileEntityArchetype;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class EventListeners {

    static DataQuery blockPlacerQuery = DataQuery.of("UnsafeData", "tecBlockPlacer");
    static boolean setBlockPlacer(TileEntity te, User player) {
        PlacerData data = new PlacerDataImpl(player.getUniqueId());
        DataTransactionResult res = te.offer(data);
        return res.isSuccessful();
    }

    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event) {
        TileEntityAccountManager.loadAccount(event.getTargetEntity());
    }

    @Listener
    public void onPlayerDisconnect(ClientConnectionEvent.Disconnect event) {
        TileEntityAccountManager.unloadAccount(event.getTargetEntity());
    }

    @Listener
    public void onInteractBlock(InteractBlockEvent event, @First Player player) {
        boolean isTE = event.getTargetBlock().getLocation().flatMap(Location::getTileEntity).isPresent();
        if (!event.getTargetBlock().getLocation().isPresent()) return;
        TileEntityCost.regTE(event.getTargetBlock().getState().getType(), isTE);
        if (isTE && CostPuncher.isPunching(player)) {
            event.getTargetBlock().getLocation().ifPresent(loc->CostPuncher.updatePrice(player, loc));
            event.setCancelled(true);
        }
        if (isTE) {
            Location<World> at = event.getTargetBlock().getLocation().get();
            Optional<UUID> a = at.getTileEntity().get().get(Placer.PLACER);
            if (!a.isPresent()) {
//                TileEntityCost.l("Post setting placer to %s", player.getName());
                at.getTileEntity().get().offer(new PlacerDataImpl(player.getUniqueId()));
                ShortTermMemeory.memorize(at, player.getUniqueId());
            } else {
                ShortTermMemeory.memorize(at, a.get());
            }
        }
    }

    @Listener
    public void onBlockPlaced(ChangeBlockEvent.Place event) {
        if (event.isCancelled()) return;
        Optional<User> source = event.getCause().first(Player.class).map(p->(User)p);
        if (!source.isPresent()) source = event.getContext().get(EventContextKeys.OWNER);
        if (!source.isPresent()) {
//            TileEntityCost.w("No user to blame"); //natural events / ender man / other
            return;
        }

        if (!source.get().hasPermission("tec.account.base")) return;

        Optional<TileEntityAccount> acc = TileEntityAccountManager.getAccount(source.get().getUniqueId());
        if (!acc.isPresent()) {
            event.setCancelled(true);
            event.getTransactions().forEach(tran->tran.setValid(false));
            source.get().getPlayer().ifPresent(player->
                    player.sendMessage(Text.of(TextColors.RED, "[TEC] Sorry, could not load your TEC account")));
            return;
        }

        handleBlockPlacements(source.get(), acc.get(), event.getTransactions());
    }

    void handleBlockPlacements(User source, TileEntityAccount acc, Collection<Transaction<BlockSnapshot>> transactions) {
        BigInteger totalCost = BigInteger.ZERO;

        transactions.stream().filter(Transaction::isValid).forEach(tran -> {
            BlockType type = tran.getFinal().getState().getType();
            BigInteger cost = TileEntityCost.getCostFor(type);

            if (!acc.canAfford(cost)) {
                tran.setValid(false);
                source.getPlayer().ifPresent(player->player.sendMessage(ChatTypes.ACTION_BAR, Text.of(TextColors.RED, "You don't have enough TEC-Points (", acc.getBalance(), " or ", cost, ")")));
            } else {
                totalCost.add(cost);
                TileEntityCost.getSyncScheduler().schedule(()->{
                    Optional<TileEntity> ote = tran.getFinal().getLocation().get().getTileEntity();
                    ote.ifPresent(te->{
                        tran.setCustom(tran.getDefault().with(Placer.PLACER, source.getUniqueId()).orElseGet(tran::getDefault));
                        PlacerData data = new PlacerDataImpl(source.getUniqueId());
                        tran.getFinal().getLocation().ifPresent(loc->loc.offer(data));
                        TileEntityCost.regTE(tran.getFinal().getState().getType(), true);
                        if (!setBlockPlacer(tran.getFinal().getLocation().get().getTileEntity().get(), source)) {
                            TileEntityCost.w("Could not set placer in te %s for %s", tran.getFinal().getState().getType(), source.getName());
                        } else
                            acc.withdraw(cost);

                        TileEntityAccountManager.markForUpdate(acc);
                        boolean positiveBalance = acc.getBalance().compareTo(BigInteger.ZERO)>0;
                        source.getPlayer().ifPresent(player->player.sendMessage(ChatTypes.ACTION_BAR, Text.of((positiveBalance?TextColors.GREEN:TextColors.RED), "You now have ", acc.getBalance(), " TEC-Points")));
                    });
                    //otherwise the tileentity vaporizezd - maybe by protection plugin?
                } , 70, TimeUnit.MILLISECONDS); //a bit later
            }
        });
    }

    @Listener
    public void onBlockBreak(ChangeBlockEvent.Break event) {
        if (event.isCancelled()) return;
        //if (!source.hasPermission("tec.account.base")) return;

        Optional<Player> source = event.getCause().first(Player.class);

        event.getTransactions().stream().filter(Transaction::isValid).forEach(tran -> {
            BlockType type = tran.getOriginal().getState().getType();

            Optional<TileEntity> ote = tran.getOriginal().getLocation().flatMap(Location::getTileEntity);
            TileEntityCost.regTE(tran.getOriginal().getState().getType(), ote.isPresent());
//            if (TileEntityCost.isTileEntity(type).orElse(false) && !source.isPresent()) {
//                tran.setValid(false);
//                event.setCancelled(true);
//                return;
//            }

            BigInteger cost = TileEntityCost.getCostFor(type);
            if (cost.compareTo(BigInteger.ZERO)<= 0) return;

            Optional<UUID> placer = ote.flatMap(te->te.get(Placer.PLACER));
            if (!placer.isPresent()) placer = tran.getOriginal().get(Placer.PLACER);
            if (!placer.isPresent()) //last resort, for things to do strange stuff to NBTs
                placer = ShortTermMemeory.remember(tran.getOriginal().getLocation().orElse(null));
            if (!placer.isPresent()) {
//                source.ifPresent(player->player.sendMessage(Text.of("[TEC] This block had no TEC-Points stored")));
                return;
            }
            Optional<User> target = TileEntityCost.getUserStorage().get(placer.get());

            Optional<TileEntityAccount> acc = TileEntityAccountManager.getAccount(placer.get());
            if (!acc.isPresent()) {
                TileEntityCost.w(String.format("Could not load TEC account for %s", placer.get()));
                return;
            }

            acc.get().deposit(cost);
            TileEntityAccountManager.markForUpdate(acc.get());
            target.flatMap(User::getPlayer).ifPresent(player ->
                    player.sendMessage(ChatTypes.ACTION_BAR, Text.of(TextColors.GREEN, "You now have ", acc.get().getBalance(), " TEC-Points")));
        });
    }

}
