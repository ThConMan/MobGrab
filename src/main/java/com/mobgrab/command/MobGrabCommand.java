package com.mobgrab.command;

import com.mobgrab.MobGrab;
import com.mobgrab.util.MobDataUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
            showHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "gui" -> handleGui(sender);
            case "reload" -> handleReload(sender);
            case "update" -> handleUpdate(sender);
            case "give" -> handleGive(sender, args);
            case "list" -> handleList(sender);
            case "save" -> handleSave(sender, args);
            case "delete" -> handleDelete(sender, args);
            default -> sender.sendMessage(
                    Component.text("Unknown subcommand. Use /mobgrab for help.", NamedTextColor.RED));
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(Component.text("MobGrab Commands:", NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true));
        sender.sendMessage(Component.empty());

        helpLine(sender, "/mobgrab gui", "Open mob toggle GUI");
        helpLine(sender, "/mobgrab reload", "Reload config & presets");
        helpLine(sender, "/mobgrab update", "Update plugin from GitHub");
        sender.sendMessage(Component.empty());

        sender.sendMessage(Component.text("Preset Commands:", NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true));
        sender.sendMessage(Component.empty());
        helpLine(sender, "/mobgrab give <player> <preset>", "Give a preset mob item");
        helpLine(sender, "/mobgrab list", "List all available presets");
        helpLine(sender, "/mobgrab save <name>", "Save the mob you're looking at");
        helpLine(sender, "/mobgrab delete <name>", "Delete a saved preset");
    }

    private void helpLine(CommandSender sender, String cmd, String desc) {
        sender.sendMessage(Component.text(" " + cmd, NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.suggestCommand(cmd))
                .hoverEvent(HoverEvent.showText(Component.text("Click to use", NamedTextColor.GRAY)))
                .append(Component.text(" - " + desc, NamedTextColor.GRAY)));
    }

    // ── GUI ──────────────────────────────────────────────

    private void handleGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return;
        }
        if (!player.hasPermission("mobgrab.admin")) {
            player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }
        plugin.getMobToggleGUI().open(player, 0);
    }

    // ── Reload ───────────────────────────────────────────

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("mobgrab.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }
        plugin.getConfigManager().reload();
        plugin.getPresetManager().reload();
        sender.sendMessage(Component.text("MobGrab config & presets reloaded.", NamedTextColor.GREEN));
    }

    // ── Give ─────────────────────────────────────────────

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mobgrab.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /mobgrab give <player> <preset>", NamedTextColor.RED));
            return;
        }

        String targetName = args[1];
        String presetName = args[2].toLowerCase();

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + targetName, NamedTextColor.RED));
            return;
        }

        if (!plugin.getPresetManager().hasPreset(presetName)) {
            sender.sendMessage(Component.text("Unknown preset: " + presetName, NamedTextColor.RED));
            sender.sendMessage(Component.text("Use /mobgrab list to see available presets.", NamedTextColor.GRAY));
            return;
        }

        ItemStack item = plugin.getPresetManager().createPresetItem(presetName);
        if (item == null) {
            sender.sendMessage(Component.text("Failed to create preset item. Check console for errors.", NamedTextColor.RED));
            return;
        }

        // Give item to player (overflow drops on ground)
        var overflow = target.getInventory().addItem(item);
        if (!overflow.isEmpty()) {
            for (ItemStack leftover : overflow.values()) {
                target.getWorld().dropItemNaturally(target.getLocation(), leftover);
            }
        }

        sender.sendMessage(Component.text("Gave ", NamedTextColor.GREEN)
                .append(Component.text(presetName, NamedTextColor.GOLD))
                .append(Component.text(" to ", NamedTextColor.GREEN))
                .append(Component.text(target.getName(), NamedTextColor.YELLOW))
                .append(Component.text("!", NamedTextColor.GREEN)));

        if (sender != target) {
            target.sendMessage(Component.text("You received a ", NamedTextColor.GREEN)
                    .append(Component.text(presetName, NamedTextColor.GOLD))
                    .append(Component.text(" mob item!", NamedTextColor.GREEN)));
        }
    }

    // ── List ─────────────────────────────────────────────

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("mobgrab.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }

        Set<String> presets = plugin.getPresetManager().getPresetNames();
        if (presets.isEmpty()) {
            sender.sendMessage(Component.text("No presets defined. Add them in presets.yml.", NamedTextColor.GRAY));
            return;
        }

        sender.sendMessage(Component.text("Available Presets (" + presets.size() + "):", NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true));
        sender.sendMessage(Component.empty());

        for (String name : presets) {
            Component entry = Component.text(" \u25B6 ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(name, NamedTextColor.YELLOW)
                            .clickEvent(ClickEvent.suggestCommand("/mobgrab give " + sender.getName() + " " + name))
                            .hoverEvent(HoverEvent.showText(Component.text("Click to give to yourself", NamedTextColor.GRAY))));
            sender.sendMessage(entry);
        }
    }

    // ── Save ─────────────────────────────────────────────

    private void handleSave(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return;
        }
        if (!player.hasPermission("mobgrab.admin")) {
            player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /mobgrab save <name>", NamedTextColor.RED));
            return;
        }

        String presetName = args[1].toLowerCase();

        // Validate name
        if (!presetName.matches("[a-z0-9_-]+")) {
            player.sendMessage(Component.text("Preset names can only contain lowercase letters, numbers, hyphens, and underscores.", NamedTextColor.RED));
            return;
        }

        // Raytrace to find entity player is looking at
        RayTraceResult rayTrace = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getLocation().getDirection(),
                6.0,
                e -> !(e instanceof Player)
        );

        if (rayTrace == null || rayTrace.getHitEntity() == null) {
            player.sendMessage(Component.text("Look at a mob and try again.", NamedTextColor.RED));
            return;
        }

        Entity target = rayTrace.getHitEntity();
        plugin.getPresetManager().savePreset(presetName, target);

        player.sendMessage(Component.text("Saved ", NamedTextColor.GREEN)
                .append(Component.text(MobDataUtil.formatEntityName(target.getType()), NamedTextColor.GOLD))
                .append(Component.text(" as preset ", NamedTextColor.GREEN))
                .append(Component.text(presetName, NamedTextColor.YELLOW))
                .append(Component.text("!", NamedTextColor.GREEN)));
    }

    // ── Delete ───────────────────────────────────────────

    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mobgrab.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /mobgrab delete <name>", NamedTextColor.RED));
            return;
        }

        String presetName = args[1].toLowerCase();
        if (plugin.getPresetManager().deletePreset(presetName)) {
            sender.sendMessage(Component.text("Deleted preset: " + presetName, NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Preset not found: " + presetName, NamedTextColor.RED));
        }
    }

    // ── Update ───────────────────────────────────────────

    private void handleUpdate(CommandSender sender) {
        if (!sender.hasPermission("mobgrab.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text("Checking for updates...", NamedTextColor.GRAY));

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpClient client = HttpClient.newHttpClient();

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

                HttpRequest dlRequest = HttpRequest.newBuilder()
                        .uri(URI.create(downloadUrl))
                        .GET()
                        .build();

                HttpResponse<InputStream> dlResponse = client.send(dlRequest, HttpResponse.BodyHandlers.ofInputStream());
                if (dlResponse.statusCode() != 200) {
                    sendSync(sender, Component.text("Download failed (HTTP " + dlResponse.statusCode() + ").", NamedTextColor.RED));
                    return;
                }

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

    // ── Tab Complete ─────────────────────────────────────

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return Stream.of("gui", "reload", "update", "give", "list", "save", "delete")
                    .filter(s -> s.startsWith(prefix))
                    .toList();
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            String prefix = args[1].toLowerCase();

            if (sub.equals("give")) {
                // Tab complete online player names
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(prefix))
                        .toList();
            }

            if (sub.equals("delete")) {
                // Tab complete preset names
                return plugin.getPresetManager().getPresetNames().stream()
                        .filter(n -> n.startsWith(prefix))
                        .toList();
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            // Tab complete preset names
            String prefix = args[2].toLowerCase();
            return plugin.getPresetManager().getPresetNames().stream()
                    .filter(n -> n.startsWith(prefix))
                    .toList();
        }

        return List.of();
    }
}
