package dev.hammermaces.listeners.paraso;

import dev.hammermaces.HammerMacesPlugin;
import dev.hammermaces.managers.MaceConfig;
import dev.hammermaces.managers.MaceManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Nausea passive for The Afterthought (soulbound sword).
 * Hits inflict Nausea I with a per-target cooldown.
 */
public class NauseaPassiveListener implements Listener {

    private final HammerMacesPlugin plugin;
    private final MaceManager maceManager;
    private final Map<UUID, Long> lastNausea = new HashMap<>();

    public NauseaPassiveListener(HammerMacesPlugin plugin) {
        this.plugin      = plugin;
        this.maceManager = plugin.getMaceManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!maceManager.isSoulboundMace(held)) return;
        if (!"the_afterthought".equals(maceManager.getMaceType(held))) return;
        if (!maceManager.isOwner(held, player.getName())) return;

        MaceConfig cfg = plugin.getMaceConfigManager().getMaceConfig("the_afterthought");
        if (cfg == null || !cfg.isNauseaPassiveEnabled()) return;

        UUID targetId = target.getUniqueId();
        long cooldownMs = cfg.getNauseaCooldownPerTarget() * 1000L;
        Long last = lastNausea.get(targetId);

        if (last != null && System.currentTimeMillis() - last < cooldownMs) return;

        lastNausea.put(targetId, System.currentTimeMillis());
        target.addPotionEffect(new PotionEffect(
            PotionEffectType.NAUSEA, cfg.getNauseaDuration(), 0, false, true, true));
    }
}
