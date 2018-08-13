package de.dosmike.sponge.tileentitycost;

import de.dosmike.sponge.PlacerNBT.Placer;
import de.dosmike.sponge.PlacerNBT.PlacerData;
import de.dosmike.sponge.PlacerNBT.impl.PlacerDataImpl;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.format.TextColors;

import java.math.BigInteger;
import java.util.Optional;
import java.util.UUID;

public class EventListeners {

    static DataQuery blockPlacerQuery = DataQuery.of("UnsafeData", "tecBlockPlacer");
    static boolean setBlockPlacer(TileEntity te, Player player) {
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
        TileEntityCost.regTE(event.getTargetBlock().getState().getType(), event.getTargetBlock().createArchetype().isPresent());
    }

    @Listener
    public void onBlockPlaced(ChangeBlockEvent.Place event, @First Player source) {
        if (event.isCancelled()) return;
        if (!source.hasPermission("tec.account.base")) return;

        Optional<TileEntityAccount> acc = TileEntityAccountManager.getAccount(source.getUniqueId());
        if (!acc.isPresent()) {
            event.setCancelled(true);
            event.getTransactions().forEach(tran->tran.setValid(false));
            source.sendMessage(Text.of(TextColors.RED, "[TEC] Sorry, could not load your TEC account"));
            return;
        }

        BigInteger totalCost = BigInteger.ZERO;
        event.getTransactions().stream().filter(tran-> {
            boolean isTE = tran.getFinal().createArchetype().isPresent();
            TileEntityCost.regTE(tran.getFinal().getState().getType(), isTE);
            return isTE && tran.isValid();
        }).forEach(tran -> {
            BlockType type = tran.getFinal().getState().getType();
            BigInteger cost = TileEntityCost.getCostFor(type);

            if (!acc.get().canAfford(cost)) {
                tran.setValid(false);
            } else {
                totalCost.add(cost);
                TileEntityCost.getSyncScheduler().execute(()->{
                    Optional<TileEntity> ote = tran.getFinal().getLocation().get().getTileEntity();
                    ote.ifPresent(te->{
                        setBlockPlacer(te, source);
                        acc.get().withdraw(cost);
                    });
                    //otherwise the tileentity vaporizezd - maybe by protection plugin?
                });
            }
        });

        if (totalCost.compareTo(BigInteger.ZERO)>=0) {
            TileEntityAccountManager.synchronize(acc.get());
            boolean positiveBalance = acc.get().getBalance().compareTo(BigInteger.ZERO)>0;
            source.sendMessage(ChatTypes.ACTION_BAR, Text.of((positiveBalance?TextColors.GREEN:TextColors.RED), "You have ", acc.get().getBalance(), " TEC-Points left"));
        }
    }

    @Listener
    public void onBlockBreak(ChangeBlockEvent.Break event, @First Player source) {
        if (event.isCancelled()) return;
        if (!source.hasPermission("tec.account.base")) return;

        event.getTransactions().stream().filter(tran-> {
            boolean isTE = tran.getOriginal().createArchetype().isPresent();
            TileEntityCost.regTE(tran.getOriginal().getState().getType(), isTE);
            return isTE && tran.isValid();
        }).forEach(tran -> {
            BlockType type = tran.getOriginal().getState().getType();
            BigInteger cost = TileEntityCost.getCostFor(type);
            if (cost.compareTo(BigInteger.ZERO)<= 0) return;

            Optional<UUID> placer = tran.getOriginal().get(Placer.PLACER);
            if (!placer.isPresent()) {
                source.sendMessage(Text.of("[TEC] This block had no TEC-Points stored"));
                return;
            }
            Optional<Player> target = Sponge.getServer().getPlayer(placer.get());

            Optional<TileEntityAccount> acc = TileEntityAccountManager.getAccount(placer.get());
            if (!acc.isPresent()) {
                TileEntityCost.w(String.format("Could not load TEC account for %s(%s)", source.getName(), source.getUniqueId()));
                return;
            }

            acc.get().deposit(cost);
            TileEntityAccountManager.synchronize(acc.get());
            target.ifPresent(player -> player.sendMessage(ChatTypes.ACTION_BAR, Text.of(TextColors.GREEN, "You now have ", acc.get().getBalance(), " TEC-Points")));
        });
    }

}
