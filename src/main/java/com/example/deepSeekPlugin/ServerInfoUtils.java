package com.example.deepSeekPlugin;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ServerInfoUtils {

    public static String getServerStatus(Server server) {
        // 获取服务器基本信息
        String serverName = server.getName();
        String version = server.getVersion();
        String bukkitVersion = server.getBukkitVersion();
        String worldCount = String.valueOf(server.getWorlds().size());

        // 获取在线玩家信息
        int onlinePlayers = server.getOnlinePlayers().size();
        int maxPlayers = server.getMaxPlayers();
        StringBuilder playerList = new StringBuilder();
        for (Player p : server.getOnlinePlayers()) {
            playerList.append(p.getName()).append(", ");
        }
        if (playerList.length() > 0) {
            playerList.setLength(playerList.length() - 2); // 移除最后的逗号和空格
        }

        // 获取服务器运行时间
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        String uptimeString = formatDuration(uptime);

        // 获取TPS (仅Paper服务器支持)
        String tpsInfo = "未知";
        try {
            double[] tps = Bukkit.getTPS();
            tpsInfo = String.format("%.2f, %.2f, %.2f", tps[0], tps[1], tps[2]);
        } catch (NoSuchMethodError e) {
            // 非Paper服务器
        }

        // 获取内存使用情况
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;

        // 获取CPU使用率
        String cpuUsage = "未知";
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean =
                        (com.sun.management.OperatingSystemMXBean) osBean;
                cpuUsage = String.format("%.2f%%", sunOsBean.getProcessCpuLoad() * 100);
            }
        } catch (Exception e) {
            // 不支持
        }

        // 获取当前时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = sdf.format(new Date());

        return "§a服务器状态信息:\n" +
                "§f服务器名称: §e" + serverName + "\n" +
                "§f服务器版本: §e" + version + " §7(Bukkit: " + bukkitVersion + ")\n" +
                "§f世界数量: §e" + worldCount + "\n" +
                "§f在线玩家: §e" + onlinePlayers + "/" + maxPlayers + "\n" +
                "§f玩家列表: §e" + playerList.toString() + "\n" +
                "§f运行时间: §e" + uptimeString + "\n" +
                "§fTPS (1m,5m,15m): §e" + tpsInfo + "\n" +
                "§f内存使用: §e" + usedMemory + "MB / " + maxMemory + "MB §7(空闲: " + freeMemory + "MB)\n" +
                "§fCPU使用率: §e" + cpuUsage + "\n" +
                "§f当前时间: §e" + currentTime;
    }

    public static List<String> getRecentLogs(File logFile, int maxLines) {
        List<String> logLines = new ArrayList<>();
        if (!logFile.exists()) return logLines;

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }

            // 获取最后 maxLines 行
            int start = Math.max(0, lines.size() - maxLines);
            for (int i = start; i < lines.size(); i++) {
                logLines.add(lines.get(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return logLines;
    }

    private static String formatDuration(long millis) {
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        return String.format("%d天 %02d:%02d:%02d", days, hours, minutes, seconds);
    }
}