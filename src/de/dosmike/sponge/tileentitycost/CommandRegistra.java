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
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.BookView;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class CommandRegistra {
	public enum TecPointCommandMethod {GIVE, TAKE, SET};
	public enum TecPointCommandSetType { BLOCK, MOD, ALL};
	public static void register () {
		Sponge.getCommandManager().register(TileEntityCost.getInstance(),
				CommandSpec.builder().arguments(
						GenericArguments.optional(
								GenericArguments.firstParsing(
										GenericArguments.requiringPermission(
												GenericArguments.seq(
														GenericArguments.literal(Text.of("Default"), "default"),
														GenericArguments.integer(Text.of("Amount"))
												),
												"tec.command.tecbalance.setdefault"
										),
										GenericArguments.requiringPermission(
												GenericArguments.user(Text.of("Player")),
												"tec.command.tecbalance.others"
										)
								)
						)
				).executor((src, args) -> {

					if (args.hasAny("Default")) {
						Integer defaultValue = args.<Integer>getOne("Amount").get();
						if (defaultValue < 0)
							throw new CommandException(Text.of("[TEC] Balance for new player can't be negative"));
						TileEntityCost.setDefaultBalance(defaultValue);
						src.sendMessage(Text.of("[TEC] You've set the default account balance for new player to ",defaultValue," TEC-Points"));
						return CommandResult.success();
					}

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
						GenericArguments.optional(
						GenericArguments.requiringPermission(
									GenericArguments.seq(
											GenericArguments.literal(Text.of("set"), "set"),
											GenericArguments.enumValue(Text.of("Mode"), TecPointCommandSetType.class),
											GenericArguments.integer(Text.of("Price"))
									)
							, "tec.command.teccost.set" ))
				).executor((src, args) -> {
					if (!(src instanceof Player)) throw new CommandException(Text.of("Only available for players"));

					Player player = (Player) src;

					if (args.hasAny("set")) {
						TecPointCommandSetType type = args.<TecPointCommandSetType>getOne("Mode").get();
						Integer price = args.<Integer>getOne("Price").orElse(0);
						if (type.equals(TecPointCommandSetType.ALL)) {
							BigInteger old = TileEntityCost.setDefaultPrice(price);
							player.sendMessage(Text.of("[TEC] You've set the default cost for TEs to ", price, " (prev: ", old, ")"));
						} else
							CostPuncher.put(player, price, type.equals(TecPointCommandSetType.MOD));
					} else if (CostPuncher.isPunching(player)) {
						CostPuncher.remove(player);
						player.sendMessage(Text.of("[TEC] You stopped punching costs onto stuff"));
					} else {
						Optional<ItemStack> is = player.getItemInHand(HandTypes.MAIN_HAND);
						if (!is.isPresent() || ItemTypes.AIR.equals(is.get().getType()))
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
						case GIVE:
							acc.get().deposit(amount);
							TileEntityAccountManager.markForUpdate(acc.get());
							target.get().getPlayer().ifPresent(player->player.sendMessage(Text.of(TextColors.GREEN, "[TEC] ", src.getName(), " gave you ", amount, " TEC-Points (You now have ", acc.get().getBalance(), ")")));
							src.sendMessage(Text.of(TextColors.GREEN, "[TEC] You gave ", amount, " TEC-Points to ", target.get().getName()," (They now have ", acc.get().getBalance(), ")"));
							break;
						case TAKE:
							acc.get().withdraw(amount);
							TileEntityAccountManager.markForUpdate(acc.get());
							target.get().getPlayer().ifPresent(player->player.sendMessage(Text.of(TextColors.GREEN, "[TEC] ", src.getName(), " took ", amount, " of your TEC-Points (You now have ", acc.get().getBalance(), ")")));
							src.sendMessage(Text.of(TextColors.GREEN, "[TEC] You took ", amount, " TEC-Points from ", target.get().getName()," (They now have ", acc.get().getBalance(), ")"));
							break;
						case SET:
							acc.get().withdraw(acc.get().getBalance());
							acc.get().deposit(amount);
							TileEntityAccountManager.markForUpdate(acc.get());
							target.get().getPlayer().ifPresent(player->player.sendMessage(Text.of(TextColors.GREEN, "[TEC] ", src.getName(), " took ", amount, " of your TEC-Points (You now have ", acc.get().getBalance(), ")")));
							src.sendMessage(Text.of(TextColors.GREEN, "[TEC] You took ", amount, " TEC-Points from ", target.get().getName()," (They now have ", acc.get().getBalance(), ")"));
							break;
						default:
							throw new CommandException(Text.of("Unsupported method"));
					}

					return CommandResult.success();
				}).permission("tec.command.tecpoints.base")
						.build(), "tecpoints", "tecs");


		Sponge.getCommandManager().register(TileEntityCost.getInstance(),
				CommandSpec.builder().arguments(
						GenericArguments.none()
				).executor((src, args) -> {
					List<Text> lines = new LinkedList<>();

					lines.add(Text.of("TEC-Points limit the amount of TileEntities you can place."));
					if (src.hasPermission("tec.command.teccost.base"))
						lines.add(Text.of("Hold a block and type /teccost to check how expensive a block is."));
					if (src.hasPermission("tec.command.tecbalance.base"))
						lines.add(Text.of("Use /tecbalance to check how much points you got left."));
					if (src.hasPermission("tec.command.tecbalance.others"))
						lines.add(Text.of("And /tecbalance <Player> to check the balance of others."));
					//administrative part
					if (src.hasPermission("tec.command.tecbalance.setdefault"))
						lines.add(Text.of("The default blanace for new players is set with /tecbalance default <Amount>"));
					if (src.hasPermission("tec.command.teccost.set"))
						lines.add(Text.of("With /teccost set <all| mod| block> <amount> you can change the cost for different blocks"));
					if (src.hasPermission("tec.command.tecpoints.base"))
						lines.add(Text.of("And /tecpoints <player> <give| take| set> <amount> is used to manipulate a players account"));

					lines.add(Text.of("/techelp displays this text and /tecsave writes the configs"));

					if (src instanceof Player) {
						List<Text> pages = new LinkedList<>();
						while (!lines.isEmpty()) {
							List<Text> page = new LinkedList<>();
							for (int i = 0; i < 3 && !lines.isEmpty(); i++) {
								page.add(lines.remove(0));
							}
							if (!page.isEmpty())
								pages.add(Text.joinWith(Text.of(Text.NEW_LINE, Text.NEW_LINE), page));
						}

						((Player) src).sendBookView(
						BookView.builder().addPages(pages)
								.title(Text.of("TEC Help"))
								.author(Text.of("TEC Help"))
								.build());
					} else {
						Text plain = Text.joinWith(Text.NEW_LINE, lines);
						src.sendMessage(plain);
					}

					return CommandResult.success();
				}).permission("tec.command.help.base")
						.build(), "techelp");

		Sponge.getCommandManager().register(TileEntityCost.getInstance(),
				CommandSpec.builder().arguments(
						GenericArguments.none()
				).executor((src, args) -> {
					TileEntityCost.getInstance().saveConfigs();
					src.sendMessage(Text.of(TextColors.DARK_GREEN, "[TEC] Configs saved"));

					return CommandResult.success();
				}).permission("tec.command.save.base")
						.build(), "tecsave");
	}
}
