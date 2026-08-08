package me.wega.offlinepotiontimer;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class OfflinePotionTimer extends JavaPlugin implements Listener {
    private File potionTimesFile;
    private FileConfiguration potionTimes;

    @Override
    public void onEnable() {
        this.loadPotionTimes();

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        for (final Player player : Bukkit.getOnlinePlayers())
            savePotionEffects(player);
    }

    private void loadPotionTimes() {
        if (!getDataFolder().exists())
            getDataFolder().mkdirs();

        potionTimesFile = new File(getDataFolder(), "saved-potion-times.yml");

        if (!potionTimesFile.exists()) {
            try {
                potionTimesFile.createNewFile();
            } catch (IOException exception) {
                getLogger().severe("Could not create potion-times.yml.");
                exception.printStackTrace();
            }
        }

        potionTimes = YamlConfiguration.loadConfiguration(potionTimesFile);
    }

    private void savePotionEffects(final Player player) {
        final String playerPath = player.getUniqueId().toString();

        potionTimes.set(playerPath, null);

        for (final PotionEffect effect : player.getActivePotionEffects()) {
            final PotionEffectType type = effect.getType();
            final String effectPath = playerPath + ".effects." + type.getName();

            final long expirationTime = System.currentTimeMillis()
                    + effect.getDuration() * 50L;

            potionTimes.set(effectPath + ".expiration", expirationTime);
            potionTimes.set(effectPath + ".amplifier", effect.getAmplifier());
            potionTimes.set(effectPath + ".ambient", effect.isAmbient());
            potionTimes.set(effectPath + ".particles", effect.hasParticles());
        }

        this.savePotionTimes();
    }

    private void restorePotionEffects(final Player player) {
        final UUID uuid = player.getUniqueId();
        final String playerPath = uuid.toString();
        final ConfigurationSection effectsSection = potionTimes
                .getConfigurationSection(playerPath + ".effects");

        if (effectsSection == null)
            return;

        final long currentTime = System.currentTimeMillis();

        for (final String effectName : effectsSection.getKeys(false)) {
            final PotionEffectType type = PotionEffectType.getByName(effectName);

            if (type == null)
                continue;

            final String effectPath = playerPath + ".effects." + effectName;
            final long expirationTime = potionTimes
                    .getLong(effectPath + ".expiration");

            final long remainingMilliseconds = expirationTime - currentTime;
            final int remainingTicks = (int) (remainingMilliseconds / 50L);

            player.removePotionEffect(type);

            if (remainingTicks <= 0)
                continue;

            final int amplifier = potionTimes
                    .getInt(effectPath + ".amplifier");

            final boolean ambient = potionTimes
                    .getBoolean(effectPath + ".ambient");

            final boolean particles = potionTimes
                    .getBoolean(effectPath + ".particles");

            player.addPotionEffect(
                    new PotionEffect(
                            type,
                            remainingTicks,
                            amplifier,
                            ambient,
                            particles
                    ),
                    true
            );
        }

        potionTimes.set(playerPath, null);
        this.savePotionTimes();
    }

    private void savePotionTimes() {
        try {
            potionTimes.save(potionTimesFile);
        } catch (IOException exception) {
            getLogger().severe("Could not save potion-times.yml.");
            exception.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        Bukkit.getScheduler().runTask(
                this,
                () -> restorePotionEffects(player)
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        savePotionEffects(event.getPlayer());
    }
}
