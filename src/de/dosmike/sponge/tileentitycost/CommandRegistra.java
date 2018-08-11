package de.dosmike.sponge.tileentitycost;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.math.BigInteger;
import java.util.Optional;

public class CommandRegistra {
	public enum TecPointCommandMethod { give, take, set };
	public static void register () {
		Sponge.getCommandManager().register(TileEntityCost.getInstance(),
				CommandSpec.builder().arguments(
						GenericArguments.onlyOne(
								GenericArguments.optional(
										GenericArguments.requiringPermission(
												GenericArguments.user(Text.of("Player")),
												"tec.command.tecbalance.others"
										)
								)
						)
				).executor((src, args) -> {

					if (!args.hasAny("Player") && !(src instanceof Player))
						throw new CommandException(Text.of("Console must specify a user"));

					User target;
					if (args.hasAny("Player"))
						target =  args.<User>getOne("Player").get();
					else
						target = (Player)src;

					Optional<TileEntityAccount> acc = TileEntityAccountManager.getAccount(target.getUniqueId());
					if (!acc.isPresent())
						throw new CommandException(Text.of("[TEC] Could not load the TEC account"));

					BigInteger balance = acc.get().getBalance();
					if ((src instanceof Player) && ((Player) src).getUniqueId().equals(target.getUniqueId()))
						src.sendMessage(Text.of(TextColors.GREEN, "[TEC] You currently have ", balance, " TEC-Points"));
					else
						src.sendMessage(Text.of(TextColors.GREEN, "[TEC] ", target.getName(), " currently has ", balance, " TEC-Points"));

					return CommandResult.success();
				}).permission("tec.command.tecbalance.base")
				.build(), "tecbalance");

		Sponge.getCommandManager().register(TileEntityCost.getInstance(),
				CommandSpec.builder().arguments(
						GenericArguments.none()
				).executor((src, args) -> {
					if (!(src instanceof Player)) throw new CommandException(Text.of("Only available for players"));

					Player player = (Player)src;
					Optional<ItemStack> is = player.getItemInHand(HandTypes.MAIN_HAND);
					if (!is.isPresent())
						throw new CommandException(Text.of("[TEC] Hold an item and use this command if you want to check how much TEC-Points a block costs"));

					Optional<BlockType> bt = is.get().getType().getBlock();
					if (!bt.isPresent()) {
						if (TileEntityCost.canGetPrice(is.get().getType().getId())) {
							BigInteger price = TileEntityCost.getCostFor(bt.get());
							src.sendMessage(Text.of(TextColors.GREEN, "[TEC] Placing this block will cost ", price, " TEC-Points"));
							return CommandResult.success();
						} else
							throw new CommandException(Text.of("[TEC] Can't get block from item"));
					}

					BigInteger price = TileEntityCost.getCostFor(bt.get());
					if (TileEntityCost.hasTecBlockId(bt.get())) {
						src.sendMessage(Text.of(TextColors.GREEN, "[TEC] Placing this block will cost ", price, " TEC-Points"));
					} else if (TileEntityCost.hasTecModId(bt.get())) {
						Optional<Boolean> isTE = TileEntityCost.isTileEntity(bt.get());
						if (!isTE.isPresent())
							src.sendMessage(Text.of(TextColors.GREEN, "[TEC] Didn't detect if this block has a tile entity yet, but placing blocks from this mod usually costs ", price, "TEC-Points"));
						else if (isTE.get())
							src.sendMessage(Text.of(TextColors.GREEN, "[TEC] Placing this block will cost ", price, " TEC-Points"));
						else
							src.sendMessage(Text.of(TextColors.GREEN, "[TEC] This block has no tile entity and thus is free"));
					} else { //of unknown mods
						Optional<Boolean> isTE = TileEntityCost.isTileEntity(bt.get());
						if (!isTE.isPresent())
							src.sendMessage(Text.of(TextColors.GREEN, "[TEC] Didn't detect if this block has a tile entity yet, but placing blocks from this mod usually costs ", price, "TEC-Points"));
						else if (isTE.get())
							src.sendMessage(Text.of(TextColors.GREEN, "[TEC] Placing this block will cost ", price, " TEC-Points"));
						else
							src.sendMessage(Text.of(TextColors.GREEN, "[TEC] This block has no tile entity and thus is free"));
					}

					return CommandResult.success();
				}).permission("tec.command.teccost.base")
						.build(), "teccost", "tecost");

		Sponge.getCommandManager().register(TileEntityCost.getInstance(),
				CommandSpec.builder().arguments(
						GenericArguments.seq(
								GenericArguments.userOrSource(Text.of("Player")),
								GenericArguments.enumValue(Text.of("Method"), TecPointCommandMethod.class),
								GenericArguments.integer(Text.of("Amount"))
						)
				).executor((src, args) -> {
					Optional<User> target = args.<User>getOne("Player");
					Optional<TileEntityAccount> acc = TileEntityAccountManager.getAccount(target.get().getUniqueId());
					if (!acc.isPresent())
						throw new CommandException(Text.of("[TEC] Could not load your TEC account"));
					BigInteger amount = BigInteger.valueOf(args.<Integer>getOne("Amount").get());
					if (amount.compareTo(BigInteger.ZERO)<=0)
						throw new CommandException(Text.of("[TEC] Can't specify negative TEC-Points"));

					switch (args.<TecPointCommandMethod>getOne("Method").get()) {
						case give:
							acc.get().deposit(amount);
							TileEntityAccountManager.synchronize(acc.get());
							target.get().getPlayer().ifPresent(player->player.sendMessage(Text.of(TextColors.GREEN, "[TEC] ", src.getName(), " gave you ", amount, " TEC-Points (You now have ", acc.get().getBalance(), ")")));
							src.sendMessage(Text.of(TextColors.GREEN, "[TEC] You gave ", amount, " TEC-Points to ", target.get().getName()," (They now have ", acc.get().getBalance(), ")"));
							break;
						case take:
							acc.get().withdraw(amount);
							TileEntityAccountManager.synchronize(acc.get());
							target.get().getPlayer().ifPresent(player->player.sendMessage(Text.of(TextColors.GREEN, "[TEC] ", src.getName(), " took ", amount, " of your TEC-Points (You now have ", acc.get().getBalance(), ")")));
							src.sendMessage(Text.of(TextColors.GREEN, "[TEC] You took ", amount, " TEC-Points from ", target.get().getName()," (They now have ", acc.get().getBalance(), ")"));
							break;
						case set:
							acc.get().withdraw(acc.get().getBalance());
							acc.get().deposit(amount);
							TileEntityAccountManager.synchronize(acc.get());
							target.get().getPlayer().ifPresent(player->player.sendMessage(Text.of(TextColors.GREEN, "[TEC] ", src.getName(), " took ", amount, " of your TEC-Points (You now have ", acc.get().getBalance(), ")")));
							src.sendMessage(Text.of(TextColors.GREEN, "[TEC] You took ", amount, " TEC-Points from ", target.get().getName()," (They now have ", acc.get().getBalance(), ")"));
							break;
						default:
							throw new CommandException(Text.of("Unsupported method"));
					}

					return CommandResult.success();
				}).permission("tec.command.tecpoints.base")
						.build(), "tecpoints", "tecs");
	}
}
