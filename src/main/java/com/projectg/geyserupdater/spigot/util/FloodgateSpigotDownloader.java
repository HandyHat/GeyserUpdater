package com.projectg.geyserupdater.spigot.util;

import com.projectg.geyserupdater.common.logger.UpdaterLogger;
import com.projectg.geyserupdater.common.util.FileUtils;
import com.projectg.geyserupdater.common.util.GeyserProperties;
import com.projectg.geyserupdater.spigot.SpigotUpdater;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;

public class FloodgateSpigotDownloader {
    private static SpigotUpdater plugin;
    private static UpdaterLogger logger;

    /**
     * Download the latest build of Geyser from Jenkins CI for the currently used branch.
     * If enabled in the config, the server will also attempt to restart.
     */
    public static void updateFloodgate() {
        plugin = SpigotUpdater.getPlugin();
        logger = UpdaterLogger.getLogger();

        UpdaterLogger.getLogger().debug("Attempting to download a new build of Floodgate.");

        // Start the process async
        new BukkitRunnable() {
            @Override
            public void run() {
                // Download the newest build and store the success state
                boolean downloadSuccess = downloadFloodgate();
                // No additional code should be run after the following BukkitRunnable
                // Run it synchronously because it isn't thread-safe
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (downloadSuccess) {
                            String successMsg = "The latest build of Floodgate has been downloaded! A restart must occur in order for changes to take effect.";
                            logger.info(successMsg);
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                if (player.hasPermission("gupdater.geyserupdate")) {
                                    player.sendMessage(ChatColor.GREEN + successMsg);
                                }
                            }
                        } else {
                            // fail messages are already sent to the logger in downloadGeyser()
                            String failMsg = "A error(); error occurred when download a new build of Floodgate. Please check the server console for further information!";
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                if (player.hasPermission("gupdater.geyserupdate")) {
                                    player.sendMessage(ChatColor.RED + failMsg);
                                }
                            }
                        }
                    }
                }.runTask(plugin);
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Internal code for downloading the latest build of Geyser from Jenkins CI for the currently used branch.
     *
     * @return true if the download was successful, false if not.
     */
    private static boolean downloadFloodgate() {
        String fileUrl;
        try {
            fileUrl = "https://ci.opencollab.dev/job/GeyserMC/job/Floodgate/job/" + GeyserProperties.getGeyserGitPropertiesValue("git.branch") + "/lastSuccessfulBuild/artifact/spigot/target/floodgate-spigot.jar";
        } catch (IOException e) {
            logger.error("Failed to get the current Geyser branch when attempting to download a new build of Geyser!");
            e.printStackTrace();
            return false;
        }
        // todo: make sure we use the update folder defined in bukkit.yml (it can be changed)
        String outputPath = "plugins/update/floodgate-spigot.jar";
        try {
            FileUtils.downloadFile(fileUrl, outputPath);
        } catch (IOException e) {
            logger.error("Failed to download the newest build of Floodgate");
            e.printStackTrace();
            return false;
        }

        if (!FileUtils.checkFile(outputPath, false)) {
            logger.error("Failed to find the downloaded Geyser build!");
            return false;
        } else {
            return true;
        }
    }
}
