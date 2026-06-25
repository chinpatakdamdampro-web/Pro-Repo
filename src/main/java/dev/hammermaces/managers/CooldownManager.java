package dev.hammermaces.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks active cooldowns for each player's mace abilities.
 * Key format: UUID:maceId:abilityName → System.currentTimeMillis() of when ability was last used
 */
public class CooldownManager {

    private final Map<String, Long> cooldowns = new HashMap<>();

    /**
     * Check if an ability is still on cooldown.
     * @param uuid        Player UUID
     * @param maceId      e.g. "hammer_of_poseidon"
     * @param abilityName e.g. "cold_aura"
     * @param seconds     Cooldown duration in seconds from the mace config
     * @return true if still on cooldown
     */
    public boolean isOnCooldown(UUID uuid, String maceId, String abilityName, int seconds) {
        String key = buildKey(uuid, maceId, abilityName);
        if (!cooldowns.containsKey(key)) return false;
        long elapsed = System.currentTimeMillis() - cooldowns.get(key);
        return elapsed < (seconds * 1000L);
    }

    /**
     * Returns how many seconds are remaining on a cooldown. Returns 0 if not on cooldown.
     */
    public int getRemainingSeconds(UUID uuid, String maceId, String abilityName, int seconds) {
        String key = buildKey(uuid, maceId, abilityName);
        if (!cooldowns.containsKey(key)) return 0;
        long elapsed = System.currentTimeMillis() - cooldowns.get(key);
        long remaining = (seconds * 1000L) - elapsed;
        if (remaining <= 0) return 0;
        return (int) Math.ceil(remaining / 1000.0);
    }

    /**
     * Sets the cooldown start time for an ability to right now.
     */
    public void setCooldown(UUID uuid, String maceId, String abilityName) {
        cooldowns.put(buildKey(uuid, maceId, abilityName), System.currentTimeMillis());
    }

    /**
     * Clears all cooldowns for a player (e.g. on logout/reload).
     */
    public void clearPlayer(UUID uuid) {
        cooldowns.keySet().removeIf(k -> k.startsWith(uuid.toString() + ":"));
    }

    private String buildKey(UUID uuid, String maceId, String abilityName) {
        return uuid.toString() + ":" + maceId + ":" + abilityName;
    }
}
