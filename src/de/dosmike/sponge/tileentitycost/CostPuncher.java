package de.dosmike.sponge.tileentitycost;

import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CostPuncher {
    static private Map<UUID, Integer> punchPrice = new HashMap<>();
    static private Map<UUID, Long> punchTime = new HashMap<>();
    static private Map<UUID, Boolean> punchMod = new HashMap<>();

    private static final Object mutex = new Object();

    public static void put(Player player, Integer price, boolean mod) {
        synchronized (mutex) {
            punchPrice.put(player.getUniqueId(), price);
            punchTime.put(player.getUniqueId(), System.currentTimeMillis());
            punchMod.put(player.getUniqueId(), mod);
            if (price < 0 && mod)
                player.sendMessage(Text.of("[TEC] Punch a block to remove that mods specific costs!"));
            else if (price < 0)
                player.sendMessage(Text.of("[TEC] Punch a block to remove the block specific cost!"));
            else if (price > 0 && mod)
                player.sendMessage(Text.of("[TEC] Punch a block to set the default cost for that blocks mod!"));
            else if (price > 0)
                player.sendMessage(Text.of("[TEC] Punch a block to set the specific cost for this block!"));
        }
    }
    public static void updatePrice(Player player, Location<World> block) {
        synchronized (mutex) {
            if (!punchTime.containsKey(player.getUniqueId())) return;
            if ((System.currentTimeMillis() - punchTime.get(player.getUniqueId())) > 5000) {
                player.sendMessage(Text.of(TextColors.GRAY, "[TEC] Type /teccost when you're done punching costs"));
                punchTime.put(player.getUniqueId(), System.currentTimeMillis() + 5000); //remind again in 10 seconds
            }
            if (block.getBlockType().equals(BlockTypes.AIR)) return;
            if (punchMod.get(player.getUniqueId())) {
                String modID = block.getBlockType().getId();
                modID = modID.indexOf(':') < 0 ? "minecraft" : modID.substring(0, modID.indexOf(':'));
                Integer price = punchPrice.get(player.getUniqueId());
                BigInteger old = TileEntityCost.setModPrice(modID, price);
                if (price > 0)
                    player.sendMessage(Text.of("[TEC] You changed the cost for block from ", modID, " to ", price, " (prev: ", old, ")"));
                else
                    player.sendMessage(Text.of("[TEC] You removed the default cost from ", modID, " (prev: ", old, ")"));
            } else block.getTileEntity().ifPresent(te -> {
                Integer price = punchPrice.get(player.getUniqueId());
                BigInteger old = TileEntityCost.setBlockPrice(block.getBlockType(), price);
                if (price > 0)
                    player.sendMessage(Text.of("[TEC] You changed the cost for block from ", block.getBlockType().getId(), " to ", price, " (prev: ", old, ")"));
                else
                    player.sendMessage(Text.of("[TEC] You removed the block specific cost for ", block.getBlockType().getId(), " (prev: ", old, ")"));
            });
        }
    }

    public static boolean isPunching(Player player) {
        synchronized (mutex) {
            return punchPrice.containsKey(player.getUniqueId());
        }
    }

    public static void remove(Player player) {
        synchronized (mutex) {
            punchPrice.remove(player.getUniqueId());
            punchTime.remove(player.getUniqueId());
            punchMod.remove(player.getUniqueId());
        }
    }
}
