package org.encinet.whosTheSpy;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class WhosTheSpy extends JavaPlugin implements Listener {

    private static WhosTheSpy instance;

    public static WhosTheSpy getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig(); // 保存默认配置
        GameManager.init(this);
        // Plugin startup logic
        Commands commands = new Commands(this);
        getCommand("spy").setExecutor(commands);
        getCommand("spy").setTabCompleter(commands);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new VoteGUI(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        GameManager.resetGame();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        GameManager.playerQuit(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Game game = GameManager.getGame();
        if (game != null && game.isGaming()) {
            Player player = event.getPlayer();
            String message = event.getMessage().toLowerCase();
            String civilianWord = game.getCivilianWord().toLowerCase();
            String spyWord = game.getSpyWord().toLowerCase();
            
            // 根据玩家角色应用不同的过滤规则
            if (game.getPlayers().contains(player)) {
                if (player.equals(game.getSpy())) {
                    // 卧底只检查卧底词汇
                    if (message.contains(spyWord)) {
                        event.setCancelled(true);
                        player.sendMessage("§c[!] §r请不要在公屏说出任何有关题目的东西!");
                    }
                } else if (player.equals(game.getWhiteboard())) {
                    // 白板检查所有词汇
                    if (message.contains(civilianWord) || message.contains(spyWord)) {
                        event.setCancelled(true);
                        player.sendMessage("§c[!] §r请不要在公屏说出任何有关题目的东西!");
                    }
                } else {
                    // 平民只检查平民词汇
                    if (message.contains(civilianWord)) {
                        event.setCancelled(true);
                        player.sendMessage("§c[!] §r请不要在公屏说出任何有关题目的东西!");
                    }
                }
            } else {
                // 非游戏玩家检查所有词汇
                if (message.contains(civilianWord) || message.contains(spyWord)) {
                    event.setCancelled(true);
                    player.sendMessage("§c[!] §r请不要在公屏说出任何有关题目的东西!");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Game game = GameManager.getGame();
        if (game != null && game.isGaming()) {
            String message = event.getMessage().toLowerCase();
            // 排除/spy guess命令
            if (message.startsWith("/spy guess")) {
                return;
            }
            Player player = event.getPlayer();
            String civilianWord = game.getCivilianWord().toLowerCase();
            String spyWord = game.getSpyWord().toLowerCase();
            
            // 根据玩家角色应用不同的过滤规则
            if (game.getPlayers().contains(player)) {
                if (player.equals(game.getSpy())) {
                    // 卧底只检查卧底词汇
                    if (message.contains(spyWord)) {
                        event.setCancelled(true);
                        player.sendMessage("§c[!] §r请不要在指令中包含任何有关题目的东西!");
                    }
                } else if (player.equals(game.getWhiteboard())) {
                    // 白板检查所有词汇
                    if (message.contains(civilianWord) || message.contains(spyWord)) {
                        event.setCancelled(true);
                        player.sendMessage("§c[!] §r请不要在指令中包含任何有关题目的东西!");
                    }
                } else {
                    // 平民只检查平民词汇
                    if (message.contains(civilianWord)) {
                        event.setCancelled(true);
                        player.sendMessage("§c[!] §r请不要在指令中包含任何有关题目的东西!");
                    }
                }
            } else {
                // 非游戏玩家检查所有词汇
                if (message.contains(civilianWord) || message.contains(spyWord)) {
                    event.setCancelled(true);
                    player.sendMessage("§c[!] §r请不要在指令中包含任何有关题目的东西!");
                }
            }
        }
    }
}
