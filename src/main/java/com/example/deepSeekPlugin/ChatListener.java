package com.example.deepSeekPlugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import com.example.deepseekplugin.shaded.gson.Gson;
import com.example.deepseekplugin.shaded.gson.JsonArray;
import com.example.deepseekplugin.shaded.gson.JsonObject;
import com.example.deepseekplugin.shaded.gson.JsonParser;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;

public class ChatListener implements Listener {

    private final ConfigManager configManager;
    private final ConversationManager conversationManager;
    private final DeepSeekPlugin plugin;
    private final Gson gson = new Gson();

    public ChatListener(ConfigManager configManager, ConversationManager conversationManager, DeepSeekPlugin plugin) {
        this.configManager = configManager;
        this.conversationManager = conversationManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        String trigger = configManager.getTrigger();
        Player player = event.getPlayer();

        if (message.startsWith(trigger)) {
            String userMessage = message.substring(trigger.length()).trim();
            if (userMessage.isEmpty()) {
                return;
            }

            // 检查API密钥是否配置
            String apiKey = configManager.getApiKey();
            if (apiKey.equals("你的API密钥") || apiKey.isEmpty()) {
                player.sendMessage(configManager.getAIName() + "§c: 管理员尚未配置API密钥，请联系管理员！");
                plugin.getLogger().warning("玩家 " + player.getName() + " 尝试使用AI，但API密钥未配置");
                return;
            }

            // 给提问玩家发送思考提示
            if (configManager.showThinkingMessage()) {
                String thinkingMsg = configManager.getThinkingMessage();
                player.sendMessage(configManager.getAIName() + thinkingMsg);
                plugin.getLogger().info("向玩家 " + player.getName() + " 发送思考提示: " + thinkingMsg);
            }

            // 在后台线程处理AI请求
            new Thread(() -> {
                plugin.getLogger().info("玩家 " + player.getName() + " 提问: " + userMessage);
                String aiResponse = getAIResponse(player, userMessage);

                // 回到主线程广播消息
                Bukkit.getScheduler().runTask(plugin, () -> {
                    // 广播AI回复给所有玩家
                    String formattedResponse = configManager.getAIName() + "§f: " + aiResponse;
                    Bukkit.broadcastMessage(formattedResponse);
                    plugin.getLogger().info("向所有玩家广播AI回复: " + aiResponse);
                });
            }).start();
        }
    }

    private String getAIResponse(Player player, String userMessage) {
        // 检查服务器信息请求
        if (configManager.isServerInfoEnabled() && player.hasPermission(configManager.getServerInfoPermission())) {
            if (userMessage.toLowerCase().contains("服务器状态") || userMessage.toLowerCase().contains("server status")) {
                return ServerInfoUtils.getServerStatus(plugin.getServer());
            }

            if (userMessage.toLowerCase().contains("日志") || userMessage.toLowerCase().contains("log")) {
                File logFile = new File(plugin.getServer().getWorldContainer(), "logs/latest.log");
                List<String> logs = ServerInfoUtils.getRecentLogs(logFile, configManager.getMaxLogLines());

                if (logs.isEmpty()) {
                    return "§c无法获取服务器日志";
                }

                StringBuilder logResponse = new StringBuilder("§a最近服务器日志:\n");
                for (String log : logs) {
                    logResponse.append("§7").append(log).append("\n");
                }
                return logResponse.toString();
            }
        }

        HttpURLConnection conn = null;
        try {
            // 添加到对话历史
            conversationManager.addUserMessage(player, userMessage);

            URL url = new URL("https://api.deepseek.com/chat/completions");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + configManager.getApiKey());
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);

            // 构建消息列表
            List<Map<String, String>> messages = conversationManager.buildMessageList(player);

            // 构建JSON请求体
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", configManager.getModel());
            requestBody.addProperty("max_tokens", configManager.getMaxTokens());
            requestBody.addProperty("temperature", configManager.getTemperature());

            JsonArray messagesArray = new JsonArray();
            for (Map<String, String> msg : messages) {
                JsonObject messageObj = new JsonObject();
                messageObj.addProperty("role", msg.get("role"));
                messageObj.addProperty("content", msg.get("content"));
                messagesArray.add(messageObj);
            }
            requestBody.add("messages", messagesArray);

            // 记录请求体（调试用）
            String requestBodyJson = gson.toJson(requestBody);
            plugin.getLogger().info("发送到DeepSeek API的请求体: " + requestBodyJson);

            // 发送请求
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBodyJson.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // 检查响应状态
            int responseCode = conn.getResponseCode();
            plugin.getLogger().info("DeepSeek API响应码: " + responseCode);

            if (responseCode != 200) {
                try (InputStream errorStream = conn.getErrorStream();
                     Scanner scanner = new Scanner(errorStream, "UTF-8")) {
                    String errorResponse = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "无错误详情";
                    plugin.getLogger().warning("API错误: HTTP " + responseCode + " - " + errorResponse);
                    return "§c抱歉，思考时遇到了问题 (HTTP " + responseCode + ")";
                }
            }

            // 解析响应
            try (InputStream inputStream = conn.getInputStream();
                 Reader reader = new InputStreamReader(inputStream, "UTF-8")) {
                JsonObject jsonResponse = JsonParser.parseReader(reader).getAsJsonObject();
                plugin.getLogger().info("DeepSeek API响应: " + jsonResponse.toString());
                return parseResponse(jsonResponse, player);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "获取AI响应时出错", e);
            return "§c思考时发生了意外错误: " + e.getMessage();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String parseResponse(JsonObject jsonResponse, Player player) {
        try {
            if (jsonResponse.has("choices") && jsonResponse.get("choices").isJsonArray()) {
                JsonArray choices = jsonResponse.getAsJsonArray("choices");
                if (choices.size() > 0) {
                    JsonObject firstChoice = choices.get(0).getAsJsonObject();
                    if (firstChoice.has("message") && firstChoice.get("message").isJsonObject()) {
                        JsonObject message = firstChoice.getAsJsonObject("message");
                        if (message.has("content") && message.get("content").isJsonPrimitive()) {
                            String content = message.get("content").getAsString();

                            // 添加到对话历史
                            conversationManager.addAssistantMessage(player, content);
                            return content;
                        }
                    }
                }
            }
            return "§c没有收到有效回应";
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "解析响应时出错", e);
            return "§c解析回应时出错: " + e.getMessage();
        }
    }
}