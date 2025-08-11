package org.encinet.whosTheSpy;

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
        if (game.isGaming()) {
            String message = event.getMessage().toLowerCase();
            String civilianWord = game.getCivilianWord().toLowerCase();
            String spyWord = game.getSpyWord().toLowerCase();
            if (message.contains(civilianWord) || message.contains(spyWord)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§c[!] §r请不要在公屏说出任何有关题目的东西!");
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
            String civilianWord = game.getCivilianWord().toLowerCase();
            String spyWord = game.getSpyWord().toLowerCase();
            if (message.contains(civilianWord) || message.contains(spyWord)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§c[!] §r请不要在指令中包含任何有关题目的东西!");
            }
        }
    }
}
