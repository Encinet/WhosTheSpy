package org.encinet.whosTheSpy.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.encinet.whosTheSpy.Game;
import org.encinet.whosTheSpy.WhosTheSpy;
import org.encinet.whosTheSpy.model.PlayerState;
import org.encinet.whosTheSpy.util.WordLoader;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import java.util.*;

public class GameService {
    private Game currentGame;
    private final WordLoader wordLoader;
    private final WhosTheSpy plugin;
    private BukkitTask actionBarTask;
    private final Random random = new Random();
    private VoteService voteService;

    public GameService(WhosTheSpy plugin) {
        this.plugin = plugin;
        this.wordLoader = new WordLoader(plugin);
        this.wordLoader.loadWords();
    }

    public void setVoteService(VoteService voteService) {
        this.voteService = voteService;
    }

    public Game getCurrentGame() {
        if (currentGame == null) {
            WordLoader.WordPair selectedPair = wordLoader.getRandomWordPair();
            if (selectedPair == null)
                return null;
            currentGame = new Game(selectedPair.civilianWord(), selectedPair.spyWord());
        }
        return currentGame;
    }

    public void endGame() {
        // 停止actionbar任务
        if (actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
        Game game = getCurrentGame();
        if (game != null && game.isGaming()) {
            announcePlayerWords(game);
            game.broadcast("§e游戏结束！§75秒后将重置游戏房间。");
            // 延迟重置
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                resetGame();
                game.setGaming(false); // 确保游戏状态被正确重置
            }, 100L); // 5秒
        }
    }

    private void announcePlayerWords(Game game) {
        game.broadcastToAll("§6===== §e游戏词条公布 §6=====");
        Map<String, List<String>> wordToPlayers = new HashMap<>();
        for (Map.Entry<Player, String> entry : game.getPlayerWords().entrySet()) {
            String playerName = entry.getKey().getName();
            String word = entry.getValue();
            wordToPlayers.computeIfAbsent(word, k -> new ArrayList<>()).add(playerName);
        }

        for (Map.Entry<String, List<String>> entry : wordToPlayers.entrySet()) {
            String word = entry.getKey();
            List<String> playerNames = entry.getValue();

            // 根据词条确定阵营
            String role;
            if (word.equals(game.getSpyWord())) {
                role = "§c[卧底]";
            } else if (word.equals("白板")) {
                role = "§f[白板]";
            } else {
                role = "§a[平民]";
            }

            String formattedNames = "§a" + String.join("§7, §a", playerNames);
            game.broadcastToAll(role + " §7词条: §f" + word + " §7→ " + formattedNames);
        }
    }

    public void resetGame() {
        currentGame = null;
    }

    public void forceEndGame(org.bukkit.command.CommandSender sender) {
        Game game = getCurrentGame();
        if (game != null && game.isGaming()) {
            game.broadcast("§c游戏已被管理员 " + sender.getName() + " 强制结束！");
            resetGame();
            sender.sendMessage("§c成功结束。");
        } else {
            sender.sendMessage("§c当前没有正在进行的游戏。");
        }
    }

    public void playerQuit(org.bukkit.entity.Player player, boolean notify) {
        Game game = getCurrentGame();
        if (game == null || !game.getPlayers().contains(player)) {
            return;
        }

        // 只有仍在游戏的玩家会收到退出消息
        if (notify) {
            game.broadcast("§e" + player.getName() + " §c退出了游戏。");
        }

        if (game.isGaming()) {
            if (player.equals(game.getSpy())) {
                if (game.getWhiteboard() != null
                        && game.getPlayers().contains(game.getWhiteboard())) {
                    game.broadcastToAll("§c卧底退出了游戏！§f白板胜利！");
                } else {
                    game.broadcastToAll("§c卧底退出了游戏！§a平民胜利！");
                }
                endGame();
            } else if (player.equals(game.getWhiteboard())) {
                if (notify) {
                    game.broadcastToAll("§f白板退出了游戏！游戏继续。");
                }
                game.setPlayerState(player, PlayerState.SPECTATING);
            } else {
                game.setPlayerState(player, PlayerState.SPECTATING); // 状态变为观战
                if (game.getActivePlayers().size() <= 2) {
                    game.broadcastToAll("§c场上只剩下两名玩家，卧底胜利！");
                    endGame();
                } else {
                    // 如果在投票阶段，检查是否可以结束投票
                    if (game.getVotes().size() >= game.getPlayers().size()
                            && game.getPlayers().size() > 0) {
                        voteService.tallyVotes(game);
                    }
                }
            }
        } else {
            game.removePlayer(player);
        }
    }

    public void startGame() {
        Game game = getCurrentGame();
        if (game.isGaming()) {
            return;
        }
        game.setGaming(true);

        Set<Player> playerSet = game.getPlayers();
        List<Player> players = new ArrayList<>(playerSet);
        int numPlayers = players.size();
        Collections.shuffle(players);

        // 分配卧底和白板
        Player spy = players.remove(0);
        game.setSpy(spy);

        Player whiteboard = null;
        // 玩家人数大于等于4人时，开始有概率设置白板
        // 4人时50%，每增加一人增加10%，9人时达到100%
        if (numPlayers >= 4) {
            if (random.nextDouble() < (0.5 + (numPlayers - 4) * 0.1)) {
                whiteboard = players.remove(0);
                game.setWhiteboard(whiteboard);
            }
        }

        for (Player player : game.getActivePlayers()) {
            if (player.equals(spy)) {
                game.setPlayerWord(player, game.getSpyWord());
                game.setRoleDisplay(player, "§c[卧底] " + game.getSpyWord());
                player.sendMessage("§c你是卧底！你的词是: " + game.getSpyWord());
            } else if (player.equals(whiteboard)) {
                game.setPlayerWord(player, "白板");
                game.setRoleDisplay(player, "§f[白板] 无词");
                player.sendMessage("§6===== §f白板玩家 §6=====");
                player.sendMessage("§f你没有任何词语！");
                player.sendMessage("§e特殊能力：可使用 §b/spy guess <词语>");
                player.sendMessage("§e• 猜中词语：§a立即胜利");
                player.sendMessage("§e• 猜错词语：§c立即出局");
            } else {
                game.setPlayerWord(player, game.getCivilianWord());
                game.setRoleDisplay(player, "§a[平民] " + game.getCivilianWord());
                player.sendMessage("§a你是平民！你的词是: " + game.getCivilianWord());
            }
            player.sendMessage("§6===== §e游戏规则 §6=====");
            player.sendMessage("§e1. 轮流描述自己的词语，§c不要直接说出词语");
            player.sendMessage("§e2. 描述完毕后输入 §b/spy vote §e进行投票");
            player.sendMessage("§e3. 得票最多者将被淘汰");
            player.sendMessage("§e4. 找出卧底则平民胜利，卧底存活到最后则卧底胜利");
        }

        String message = "§b游戏开始！总共有 " + game.getPlayers().size() + " 名玩家，其中 1 名是卧底";
        if (whiteboard != null) {
            message += "，1 名是白板";
        }
        game.broadcast(message);

        // 启动actionbar定时任务
        if (actionBarTask != null) {
            actionBarTask.cancel();
        }
        int interval = plugin.getConfig().getInt("actionbar-interval", 10);
        actionBarTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::updateActionBars,
                0, // 立即开始
                interval // 更新间隔
        );
    }

    private void updateActionBars() {
        Game game = getCurrentGame();
        if (game == null || !game.isGaming())
            return;

        for (Player player : game.getPlayers()) {
            String display = game.getRoleDisplay(player);
            if (!display.isEmpty()) {
                player.spigot().sendMessage(
                        ChatMessageType.ACTION_BAR,
                        TextComponent.fromLegacyText(display));
            }
        }
    }
}