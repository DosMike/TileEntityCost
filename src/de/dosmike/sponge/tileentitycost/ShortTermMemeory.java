package de.dosmike.sponge.tileentitycost;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ShortTermMemeory {
    static Map<Object, Object> tokenDataMapping = new HashMap<>();
    static Map<Object, Long> tokenValiditiy = new HashMap<>();
    static final Object mutex = new Object();

    /** memorize a value for a given key */
    public static void memorize(Object key, Object value) {
        synchronized (mutex) {
//            TileEntityCost.l("[STM] Memorizing %s for %s", value.toString(), key.toString());
            tokenDataMapping.put(key, value);
            tokenValiditiy.put(key, System.currentTimeMillis());
        }
    }

    /** tries to retrieve a value from memory
     * @return value if not yet expired */
    public static <T> Optional<T> remember(Object key) {
        synchronized (mutex) {
            T value = (T)tokenDataMapping.get(key);
//            TileEntityCost.l("[STM] Remembering %s from %s", value, key.toString());
            return Optional.ofNullable(value);
        }
    }

    public static void thinkTick() {
        synchronized (mutex) {
            Long now = System.currentTimeMillis();
            tokenValiditiy.entrySet().stream().filter(e ->
                    (now - e.getValue()) >= 100 //2 ticks
            ).map(Map.Entry::getKey).collect(Collectors.toList()).forEach(k -> {
//                TileEntityCost.l("[STM] Forgetting things for %s", k.toString());
                tokenValiditiy.remove(k);
                tokenDataMapping.remove(k);
            });
        }
    }
}
