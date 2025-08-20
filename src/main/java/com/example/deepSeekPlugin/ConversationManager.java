package com.example.deepSeekPlugin;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.Server;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConversationManager {
    private final Map<UUID, List<Map<String, String>>> conversations;
    private final ConfigManager configManager;
    private final File dataFile;
    private final DeepSeekPlugin plugin;

    public ConversationManager(ConfigManager configManager, DeepSeekPlugin plugin) {
        this.conversations = new ConcurrentHashMap<>();
        this.configManager = configManager;
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "conversations.yml");
        loadConversations();
    }

    public synchronized List<Map<String, String>> getPlayerConversation(Player player) {
        return conversations.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
    }

    public synchronized void addUserMessage(Player player, String message) {
        List<Map<String, String>> history = getPlayerConversation(player);
        history.add(Map.of("role", "user", "content", message));
        trimConversation(history);
    }

    public synchronized void addAssistantMessage(Player player, String message) {
        List<Map<String, String>> history = getPlayerConversation(player);
        history.add(Map.of("role", "assistant", "content", message));
        trimConversation(history);
    }

    public synchronized void clearPlayerConversation(Player player) {
        conversations.remove(player.getUniqueId());
    }

    public synchronized List<Map<String, String>> buildMessageList(Player player) {
        List<Map<String, String>> messages = new ArrayList<>();

        // 添加系统提示 - 正确替换玩家名称
        String systemPrompt = configManager.getSystemPrompt()
                .replace("{player}", player.getName())
                .replace("{displayname}", player.getDisplayName())
                .replace("{world}", player.getWorld().getName())
                .replace("{location}", formatLocation(player.getLocation()))
                .replace("{server_status}", getServerStatusPlaceholder());
        messages.add(Map.of("role", "system", "content", systemPrompt));

        // 添加对话历史
        List<Map<String, String>> history = getPlayerConversation(player);
        messages.addAll(history);

        // 调试日志
        plugin.getLogger().info("为玩家 " + player.getName() + " 构建的消息列表:");
        plugin.getLogger().info("系统提示: " + systemPrompt);
        for (Map<String, String> msg : messages) {
            plugin.getLogger().info(msg.get("role") + ": " + msg.get("content"));
        }

        return messages;
    }

    public synchronized void saveAllConversations() {
        FileConfiguration dataConfig = new YamlConfiguration();

        for (Map.Entry<UUID, List<Map<String, String>>> entry : conversations.entrySet()) {
            dataConfig.set(entry.getKey().toString(), entry.getValue());
        }

        try {
            dataConfig.save(dataFile);
            plugin.getLogger().info("已保存 " + conversations.size() + " 个玩家的对话历史");
        } catch (IOException e) {
            plugin.getLogger().severe("保存对话历史时出错: " + e.getMessage());
        }
    }

    private synchronized void loadConversations() {
        if (!dataFile.exists()) {
            plugin.getLogger().info("未找到对话历史文件，将创建新文件");
            return;
        }

        FileConfiguration dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        int count = 0;

        for (String key : dataConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                List<Map<String, String>> conversation = (List<Map<String, String>>) dataConfig.getList(key);

                if (conversation != null) {
                    conversations.put(uuid, conversation);
                    count++;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("加载对话历史时出错 (UUID: " + key + "): " + e.getMessage());
            }
        }

        plugin.getLogger().info("已加载 " + count + " 个玩家的对话历史");
    }

    private void trimConversation(List<Map<String, String>> history) {
        int maxHistory = configManager.getMaxHistory();
        if (history.size() <= maxHistory * 2) return;

        // 保留最近的对话
        int toRemove = history.size() - maxHistory * 2;
        history.subList(0, toRemove).clear();
    }

    private String getServerStatusPlaceholder() {
        if (!configManager.isServerInfoEnabled()) {
            return "服务器状态信息不可用";
        }

        try {
            // 只包含基本信息，避免过长
            Server server = plugin.getServer();
            return String.format(
                    "服务器名称: %s, 版本: %s, 在线玩家: %d/%d",
                    server.getName(),
                    server.getBukkitVersion(),
                    server.getOnlinePlayers().size(),
                    server.getMaxPlayers()
            );
        } catch (Exception e) {
            return "服务器状态信息获取失败";
        }
    }

    private String formatLocation(Location location) {
        return String.format("世界: %s, X: %d, Y: %d, Z: %d",
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }
}