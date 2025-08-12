package org.encinet.whosTheSpy;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Commands implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("stop")) {
            if (sender.hasPermission("whosthespy.admin.stop")) {
                GameManager.forceEndGame(sender);
            } else {
                sender.sendMessage("§c你没有权限执行此命令。");
            }
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§c只有玩家才能使用此命令");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // Show help or usage
            player.sendMessage("§e--- §f谁是卧底 §e---");
            player.sendMessage("§a/spy join §7- §f加入游戏房间");
            player.sendMessage("§c/spy leave §7- §f退出游戏房间");
            player.sendMessage("§a/spy ready §7- §f准备开始游戏");
            player.sendMessage("§c/spy unready §7- §f取消准备");
            player.sendMessage("§b/spy vote [player] §7- §f投票给一个玩家");
            if (player.hasPermission("whosthespy.admin.stop")) {
                player.sendMessage("§c/spy stop §7- §f强制结束游戏");
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "join":
                handleJoin(player);
                break;
            case "leave":
                handleLeave(player, false);
                break;
            case "ready":
                handleReady(player);
                break;
            case "unready":
                handleUnready(player);
                break;
            case "vote":
                handleVote(player, args);
                break;
            case "guess":
                handleGuess(player, args);
                break;
            default:
                player.sendMessage("§c未知命令, 使用 §e/spy §c获取帮助");
                break;
        }

        return true;
    }

    private void handleJoin(Player player) {
        Game game = GameManager.getGame();
        if (game.getGamePlayers().containsKey(player)) {
            player.sendMessage("§c你已经在游戏中了");
            return;
        }
        if (game.isGaming()) {
            player.sendMessage("§c游戏已经开始");
            return;
        }
        game.addPlayer(player);
        player.sendMessage("§a你已加入游戏, 输入 §e/spy ready §a准备");
        player.getServer().broadcastMessage(
                "§e" + player.getName() + " §a加入了§f谁是卧底§a游戏 §7(" + game.getPlayers().size() + "人)");
    }

    private void handleReady(Player player) {
        Game game = GameManager.getGame();
        if (!game.getGamePlayers().containsKey(player)) {
            player.sendMessage("§c你不在游戏房间内");
            return;
        }
        if (game.isGaming()) {
            player.sendMessage("§c游戏已经开始");
            return;
        }
        if (game.getReadyPlayers().contains(player)) {
            player.sendMessage("§c你已经准备了");
            return;
        }

        game.addReadyPlayer(player);
        player.sendMessage("§a你已准备，可以输入 §e/spy unready §a取消准备");
        game.getPlayers()
                .forEach(p -> p.sendMessage("§e" + player.getName() + " §a已准备 §7(" + game.getReadyPlayers().size() + "/"
                        + Math.max(4, game.getPlayers().size()) + ")"));

        // Check if enough players are ready to start
        if (game.getActivePlayers().size() >= 4 && game.getReadyPlayers().size() == game.getActivePlayers().size()) {
            // Start the game
            GameManager.startGame();
        }
    }

    private void handleUnready(Player player) {
        Game game = GameManager.getGame();
        if (!game.getGamePlayers().containsKey(player)) {
            player.sendMessage("§c你不在游戏房间内");
            return;
        }
        if (game.isGaming()) {
            player.sendMessage("§c游戏已经开始");
            return;
        }
        if (!game.getReadyPlayers().contains(player)) {
            player.sendMessage("§c你还没有准备");
            return;
        }

        game.removeReadyPlayer(player);
        player.sendMessage("§a你已取消准备，可以输入 §e/spy ready §a再次准备");
        game.getPlayers()
                .forEach(p -> p.sendMessage("§e" + player.getName() + " §c取消了准备 §7(" + game.getReadyPlayers().size()
                        + "/" + game.getPlayers().size() + ")"));
    }

    private void handleLeave(Player player, boolean notify) {
        Game game = GameManager.getGame();
        if (!game.getPlayers().contains(player)) {
            player.sendMessage("§c你不在游戏房间内");
            return;
        }
        if (game.isGaming()) {
            player.sendMessage("§c游戏已经开始，无法退出");
            return;
        }
        game.removePlayer(player);
        player.sendMessage("§a你已退出游戏");
        if (notify) {
            game.getPlayers().forEach(p -> p.sendMessage(
                    "§e" + player.getName() + " §c退出了游戏 §7(" + game.getPlayers().size() + "人)"));
        }
    }

    private void handleVote(Player player, String[] args) {
        Game game = GameManager.getGame();
        if (!game.isGaming()) {
            player.sendMessage("§c游戏还未开始, 无法投票");
            return;
        }
        if (!game.getActivePlayers().contains(player)) {
            player.sendMessage("§c你不在游戏中，无法投票");
            return;
        }

        if (args.length == 1) {
            // 打开投票GUI
            VoteGUI.open(player);
        } else if (args.length == 2) {
            if (args[1].equalsIgnoreCase("[弃权]")) {
                game.vote(player, null); // null for abstention
                player.sendMessage("§a你选择了弃权");
            } else {
                // 直接通过命令投票
                Player target = player.getServer().getPlayer(args[1]);
                if (target == null || !game.getPlayers().contains(target)) {
                    player.sendMessage("§c玩家未找到或不在游戏中");
                    return;
                }
                // 禁止投票给自己
                if (target.equals(player)) {
                    player.sendMessage("§c你不能投票给自己");
                    return;
                }
                game.vote(player, target);
                player.sendMessage("§a你投票给了 §e" + target.getName());
            }

            // 检查是否所有人都投票了
            if (game.getVotes().size() == game.getActivePlayers().size()) {
                GameManager.tallyVotes();
            }
        } else {
            player.sendMessage("§c用法: /spy vote [玩家]");
        }
    }

    private void handleGuess(Player player, String[] args) {
        Game game = GameManager.getGame();
        if (!game.isGaming()) {
            player.sendMessage("§c游戏还未开始");
            return;
        }
        if (!game.getActivePlayers().contains(player)) {
            player.sendMessage("§c你不在游戏中，无法使用此命令");
            return;
        }
        if (game.getWhiteboard() == null || !game.getWhiteboard().equals(player)) {
            player.sendMessage("§c你不是白板，不能使用此命令");
            return;
        }
        if (args.length != 2) {
            player.sendMessage("§c用法: /spy guess <词语>");
            return;
        }

        String guessedWord = args[1];

        // 白板只能通过猜中平民词获胜
        if (guessedWord.equalsIgnoreCase(game.getCivilianWord())) {
            player.getServer().broadcastMessage("§f白板 " + player.getName() + " 猜对了平民词语！白板胜利！");
            GameManager.endGame();
        } else {
            player.getServer().broadcastMessage("§f白板 " + player.getName() + " 猜错了词语！白板出局！");
            // 使用 GameManager 来移除玩家，以统一处理退出逻辑
            GameManager.playerQuit(player, false);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("join", "leave", "ready", "unready", "vote", "guess").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("vote")) {
            Game game = GameManager.getGame();
            if (game.isGaming()) {
                List<String> suggestions = game.getActivePlayers().stream()
                        .filter(p -> !p.equals(sender))
                        .map(Player::getName)
                        .collect(Collectors.toList());
                suggestions.add("[弃权]");
                return suggestions.stream()
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}