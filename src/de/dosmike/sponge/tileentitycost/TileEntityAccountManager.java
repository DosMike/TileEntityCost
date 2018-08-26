package de.dosmike.sponge.tileentitycost;

import de.dosmike.sponge.autosql.AutoSQL;
import org.spongepowered.api.entity.living.player.Player;

import java.lang.reflect.Field;
import java.util.*;

public class TileEntityAccountManager {

    static final Map<UUID, TileEntityAccount> accounts = new HashMap<>();
    static AutoSQL<TileEntityAccount> sql;
    private static final Set<UUID> dirty = new HashSet<>();

    private static Field dbKey;
    static void init() {
        try {
            dbKey = TileEntityAccount.class.getField("playerID");
            sql = new AutoSQL<>("jdbc:h2:./config/tileentitycost/database", TileEntityCost.getAsyncScheduler(), TileEntityAccount.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void loadAccount(Player player) {
        TileEntityCost.getAsyncScheduler().submit(()->{
            try {
                TileEntityAccount account =
                        sql.selectOne(dbKey, player.getUniqueId()).get().orElse(new TileEntityAccount(player.getUniqueId()));
                synchronized (accounts) {
                    accounts.put(account.playerID, account);
                }
            } catch (Exception e) {
                TileEntityCost.w(String.format("Could not load TEC account for %s(%s)", player.getName(), player.getUniqueId()));
                e.printStackTrace();
            }
        });
    }

    static void unloadAccount(Player player) {
        synchronized (accounts) {
            accounts.remove(player.getUniqueId());
        }
    }

    static void synchronize() {
        synchronized (dirty) {
            for (UUID uid : dirty) {
                getAccount(uid).ifPresent(account->
                    sql.insertOrUpdate(account, dbKey)
                );
            }
        }
    }

    /** notify the manager to synchhronize the account with the database in the future */
    public static void markForUpdate(TileEntityAccount account) {
        synchronized (dirty) {
            dirty.add(account.playerID);
        }
    }

    /** @return the account for this player or empty if the player is not online */
    public static Optional<TileEntityAccount> getAccount(UUID player) {
        synchronized (accounts) {
            return Optional.ofNullable(accounts.get(player));
        }
    }
}
