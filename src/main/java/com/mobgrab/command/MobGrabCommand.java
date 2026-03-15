package com.mobgrab.command;

import com.mobgrab.MobGrab;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;

public final class MobGrabCommand implements CommandExecutor, TabCompleter {

    private static final String GITHUB_REPO = "ThConMan/MobGrab";
    private static final String API_URL = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";

    private final MobGrab plugin;

    public MobGrabCommand(MobGrab plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("MobGrab Commands:", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("/mobgrab gui", NamedTextColor.YELLOW)
                    .append(Component.text(" - Open mob toggle GUI", NamedTextColor.GRAY)));
            sender.sendMessage(Component.text("/mobgrab reload", NamedTextColor.YELLOW)
                    .append(Component.text(" - Reload config", NamedTextColor.GRAY)));
            sender.sendMessage(Component.text("/mobgrab update", NamedTextColor.YELLOW)
                    .append(Component.text(" - Update plugin from GitHub", NamedTextColor.GRAY)));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "gui" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
                    return true;
                }
                if (!player.hasPermission("mobgrab.admin")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                plugin.getMobToggleGUI().open(player, 0);
            }
            case "reload" -> {
                if (!sender.hasPermission("mobgrab.admin")) {
                    sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                plugin.getConfigManager().reload();
                sender.sendMessage(Component.text("MobGrab config reloaded.", NamedTextColor.GREEN));
            }
            case "update" -> {
                if (!sender.hasPermission("mobgrab.admin")) {
                    sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                handleUpdate(sender);
            }
            default -> sender.sendMessage(
                    Component.text("Unknown subcommand. Use /mobgrab for help.", NamedTextColor.RED));
        }

        return true;
    }

    private void handleUpdate(CommandSender sender) {
        sender.sendMessage(Component.text("Checking for updates...", NamedTextColor.GRAY));

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpClient client = HttpClient.newHttpClient();

                // Fetch latest release info
                HttpRequest apiRequest = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Accept", "application/vnd.github.v3+json")
                        .GET()
                        .build();

                HttpResponse<InputStream> apiResponse = client.send(apiRequest, HttpResponse.BodyHandlers.ofInputStream());
                if (apiResponse.statusCode() != 200) {
                    sendSync(sender, Component.text("Failed to check for updates (HTTP " + apiResponse.statusCode() + ").", NamedTextColor.RED));
                    return;
                }

                JsonObject release = JsonParser.parseReader(new InputStreamReader(apiResponse.body())).getAsJsonObject();
                String latestTag = release.get("tag_name").getAsString();
                String latestVersion = latestTag.startsWith("v") ? latestTag.substring(1) : latestTag;
                String currentVersion = plugin.getDescription().getVersion();

                if (currentVersion.equals(latestVersion)) {
                    sendSync(sender, Component.text("Already up to date! ", NamedTextColor.GREEN)
                            .append(Component.text("(v" + currentVersion + ")", NamedTextColor.GRAY)));
                    return;
                }

                // Find jar asset
                JsonArray assets = release.getAsJsonArray("assets");
                String downloadUrl = null;
                for (var element : assets) {
                    JsonObject asset = element.getAsJsonObject();
                    if (asset.get("name").getAsString().equals("MobGrab.jar")) {
                        downloadUrl = asset.get("browser_download_url").getAsString();
                        break;
                    }
                }

                if (downloadUrl == null) {
                    sendSync(sender, Component.text("No MobGrab.jar found in latest release.", NamedTextColor.RED));
                    return;
                }

                sendSync(sender, Component.text("Updating ", NamedTextColor.YELLOW)
                        .append(Component.text("v" + currentVersion, NamedTextColor.RED))
                        .append(Component.text(" -> ", NamedTextColor.GRAY))
                        .append(Component.text("v" + latestVersion, NamedTextColor.GREEN))
                        .append(Component.text("...", NamedTextColor.YELLOW)));

                // Download new jar
                HttpRequest dlRequest = HttpRequest.newBuilder()
                        .uri(URI.create(downloadUrl))
                        .GET()
                        .build();

                HttpResponse<InputStream> dlResponse = client.send(dlRequest, HttpResponse.BodyHandlers.ofInputStream());
                if (dlResponse.statusCode() != 200) {
                    sendSync(sender, Component.text("Download failed (HTTP " + dlResponse.statusCode() + ").", NamedTextColor.RED));
                    return;
                }

                // Delete old jar(s) and write new one
                File pluginsDir = plugin.getDataFolder().getParentFile();
                for (File file : pluginsDir.listFiles()) {
                    if (file.getName().matches("MobGrab.*\\.jar")) {
                        file.delete();
                    }
                }

                Path target = pluginsDir.toPath().resolve("MobGrab.jar");
                try (InputStream in = dlResponse.body()) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }

                sendSync(sender, Component.text("Updated to v" + latestVersion + "! ", NamedTextColor.GREEN)
                        .append(Component.text("Restart the server to apply.", NamedTextColor.YELLOW)
                                .decoration(TextDecoration.BOLD, true)));

            } catch (Exception e) {
                plugin.getLogger().warning("Update failed: " + e.getMessage());
                sendSync(sender, Component.text("Update failed: " + e.getMessage(), NamedTextColor.RED));
            }
        });
    }

    private void sendSync(CommandSender sender, Component message) {
        plugin.getServer().getScheduler().runTask(plugin, () -> sender.sendMessage(message));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return Stream.of("gui", "reload", "update")
                    .filter(s -> s.startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
