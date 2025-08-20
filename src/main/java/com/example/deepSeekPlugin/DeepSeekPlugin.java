package com.example.deepSeekPlugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.CommandExecutor;

public class DeepSeekPlugin extends JavaPlugin {

    private static DeepSeekPlugin instance;
    private ConfigManager configManager;
    private ConversationManager conversationManager;

    @Override
    public void onEnable() {
        instance = this;

        // 确保数据目录存在
        if (!getDataFolder().exists()) {
            boolean created = getDataFolder().mkdirs();
            if (created) {
                getLogger().info("已创建插件数据目录");
            }
        }

        // 初始化配置管理器
        configManager = new ConfigManager(this);

        // 检查API密钥是否已配置
        if (configManager.getApiKey().equals("你的API密钥")) {
            getLogger().warning("请配置你的DeepSeek API密钥！");
            getLogger().warning("编辑 config.yml 并设置 api_key");
            getLogger().warning("API密钥申请地址: https://platform.deepseek.com/api-keys");
        }

        // 初始化对话管理器
        conversationManager = new ConversationManager(configManager, this);

        // 注册事件监听器
        getServer().getPluginManager().registerEvents(
                new ChatListener(configManager, conversationManager, this), this);

        // 注册命令
        getCommand("deepseekreload").setExecutor(new ReloadCommand(this));
        getCommand("clearcontext").setExecutor(new ClearContextCommand(conversationManager));

        getLogger().info("DeepSeek插件已启用！呼出词: " + configManager.getTrigger());
        getLogger().info("上下文最大轮数: " + configManager.getMaxHistory());
    }

    @Override
    public void onDisable() {
        if (conversationManager != null) {
            conversationManager.saveAllConversations();
        }
        getLogger().info("DeepSeek插件已禁用");
    }

    public static DeepSeekPlugin getInstance() {
        return instance;
    }

    public void reloadPluginConfig() {
        if (configManager == null) {
            configManager = new ConfigManager(this);
        } else {
            configManager.reload();
        }
        getLogger().info("配置已重新加载");
        getLogger().info("API密钥: " + maskApiKey(configManager.getApiKey()));
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) return "****";
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}

// 重载配置命令
class ReloadCommand implements CommandExecutor {
    private final DeepSeekPlugin plugin;

    public ReloadCommand(DeepSeekPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender,
                             org.bukkit.command.Command command,
                             String label, String[] args) {
        plugin.reloadPluginConfig();
        sender.sendMessage("§aDeepSeek配置已重载！");
        return true;
    }
}