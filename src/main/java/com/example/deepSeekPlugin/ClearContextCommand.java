package com.example.deepSeekPlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClearContextCommand implements CommandExecutor {
    private final ConversationManager conversationManager;

    public ClearContextCommand(ConversationManager conversationManager) {
        this.conversationManager = conversationManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c只有玩家可以使用此命令");
            return true;
        }

        Player player = (Player) sender;
        conversationManager.clearPlayerConversation(player);
        player.sendMessage("§a已清除你的对话上下文！");
        return true;
    }
}