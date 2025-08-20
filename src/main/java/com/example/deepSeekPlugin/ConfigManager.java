package com.example.deepSeekPlugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class ConfigManager {
    private final JavaPlugin plugin;
    private File configFile;
    private FileConfiguration config;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        reload();
    }

    public void reload() {
        // 确保插件数据目录存在
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        // 如果配置文件不存在，从jar中复制默认配置
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
            plugin.getLogger().info("已创建默认配置文件");
        }

        // 加载配置
        config = YamlConfiguration.loadConfiguration(configFile);

        // 检查并添加缺失的配置项
        addMissingOptions();

        // 保存配置以确保所有选项都被写入
        try {
            config.save(configFile);
            plugin.getLogger().info("配置文件已保存");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "保存配置文件时出错", e);
        }
    }

    private void addMissingOptions() {
        // 默认配置项
        String[] keys = {
                "trigger", "ai_name", "system_prompt", "api_key",
                "model", "max_tokens", "temperature", "max_history",
                "show_thinking_message", "thinking_message",
                "enable_server_info", "server_info_permission", "max_log_lines"
        };

        Object[] defaultValues = {
                "@ai", "§b[DeepSeek助手]", "你是一个Minecraft游戏助手，正在与{player}对话。请用简短、友好的语气回答游戏相关问题，避免复杂术语。",
                "你的API密钥", "deepseek-chat", 500, 0.7, 5,
                true, "§e: 正在思考中，请稍候...",
                true, "deepseek.serverinfo", 10
        };

        boolean changed = false;

        // 检查并添加缺失的配置项
        for (int i = 0; i < keys.length; i++) {
            if (!config.contains(keys[i])) {
                config.set(keys[i], defaultValues[i]);
                changed = true;
                plugin.getLogger().info("添加缺失配置项: " + keys[i] + " = " + defaultValues[i]);
            }
        }

        if (changed) {
            plugin.getLogger().info("已更新配置文件");
        }
    }

    public String getTrigger() {
        return config.getString("trigger", "@ai");
    }

    public String getAIName() {
        return config.getString("ai_name", "§b[DeepSeek]");
    }

    public String getSystemPrompt() {
        return config.getString("system_prompt", "你是一个Minecraft游戏助手，正在与{player}对话。请用简短、友好的语气回答，避免复杂术语。");
    }

    public String getApiKey() {
        return config.getString("api_key", "你的API密钥");
    }

    public String getModel() {
        return config.getString("model", "deepseek-chat");
    }

    public int getMaxTokens() {
        return config.getInt("max_tokens", 500);
    }

    public double getTemperature() {
        return config.getDouble("temperature", 0.7);
    }

    public int getMaxHistory() {
        return config.getInt("max_history", 5);
    }

    public boolean showThinkingMessage() {
        return config.getBoolean("show_thinking_message", true);
    }

    public String getThinkingMessage() {
        return config.getString("thinking_message", "§e: 正在思考中，请稍候...");
    }

    public boolean isServerInfoEnabled() {
        return config.getBoolean("enable_server_info", true);
    }

    public String getServerInfoPermission() {
        return config.getString("server_info_permission", "deepseek.serverinfo");
    }

    public int getMaxLogLines() {
        return config.getInt("max_log_lines", 10);
    }
}