package dev.hammermaces.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks per-mace per-ability cooldowns by player UUID.
 * Key format: UUID:maceId:abilityName
 */
public class CooldownManager {

    private final Map<String, Long> cooldowns = new HashMap<>();

    public boolean isOnCooldown(UUID uuid, String maceId, String ability, int seconds) {
        String key = key(uuid, maceId, ability);
        if (!cooldowns.containsKey(key)) return false;
        return System.currentTimeMillis() - cooldowns.get(key) < seconds * 1000L;
    }

    public int getRemainingSeconds(UUID uuid, String maceId, String ability, int seconds) {
        String key = key(uuid, maceId, ability);
        if (!cooldowns.containsKey(key)) return 0;
        long remaining = (seconds * 1000L) - (System.currentTimeMillis() - cooldowns.get(key));
        return remaining <= 0 ? 0 : (int) Math.ceil(remaining / 1000.0);
    }

    public void setCooldown(UUID uuid, String maceId, String ability) {
        cooldowns.put(key(uuid, maceId, ability), System.currentTimeMillis());
    }

    /** Removes a specific ability's cooldown — used when an ability fails and shouldn't be penalised. */
    public void clearAbility(UUID uuid, String maceId, String ability) {
        cooldowns.remove(key(uuid, maceId, ability));
    }

    public void clearPlayer(UUID uuid) {
        cooldowns.keySet().removeIf(k -> k.startsWith(uuid + ":"));
    }

    private String key(UUID uuid, String maceId, String ability) {
        return uuid + ":" + maceId + ":" + ability;
    }
}
