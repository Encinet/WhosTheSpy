package org.encinet.whosTheSpy;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class GameManager {

    private static WhosTheSpy plugin;
    private static final List<WordPair> wordList = new ArrayList<>();
    private static final Random random = new Random();

    public static void init(WhosTheSpy instance) {
        plugin = instance;
        loadWords();
    }

    private static void loadWords() {
        wordList.clear();
        List<Map<?, ?>> words = plugin.getConfig().getMapList("words");
        for (Map<?, ?> wordMap : words) {
            String civilianWord = (String) wordMap.get("civilian");
            String spyWord = (String) wordMap.get("spy");
            if (civilianWord != null && spyWord != null) {
                wordList.add(new WordPair(civilianWord, spyWord));
            }
        }
    }

    private record WordPair(String civilianWord, String spyWord) {
    }

    private static Game game;

    public static Game getGame() {
        if (game == null) {
            if (wordList.isEmpty()) {
                // 可以在这里添加一条日志消息，提示服主词库为空
                return null;
            }
            WordPair selectedPair = wordList.get(random.nextInt(wordList.size()));
            game = new Game(selectedPair.civilianWord(), selectedPair.spyWord());
        }
        return game;
    }

    public static void startGame() {
        Game currentGame = getGame();
        if (currentGame.isGaming()) {
            return;
        }
        currentGame.setGaming(true);

        Set<Player> playerSet = currentGame.getPlayers();
        List<Player> players = new ArrayList<>(playerSet);
        int numPlayers = players.size();
        Collections.shuffle(players);

        // 分配卧底和白板
        Player spy = players.remove(0);
        currentGame.setSpy(spy);

        Player whiteboard = null;
        // 玩家人数大于等于4人时，开始有概率设置白板
        // 4人时50%，每增加一人增加10%，9人时达到100%
        if (numPlayers >= 4) {
            if (random.nextDouble() < (0.5 + (numPlayers - 4) * 0.1)) {
                whiteboard = players.remove(0);
                currentGame.setWhiteboard(whiteboard);
            }
        }

        for (Player player : currentGame.getPlayers()) {
            if (player.equals(spy)) {
                currentGame.setPlayerWord(player, currentGame.getSpyWord());
                player.sendMessage("§c你是卧底！你的词是: " + currentGame.getSpyWord());
            } else if (player.equals(whiteboard)) {
                currentGame.setPlayerWord(player, "白板");
                player.sendMessage("§f你是白板！你没有词语。");
                player.sendMessage("§e在游戏中，你可以使用 §f/spy guess <词语> §e来猜测别人的词语，猜对则胜利，猜错则出局。");
            } else {
                currentGame.setPlayerWord(player, currentGame.getCivilianWord());
                player.sendMessage("§a你是平民！你的词是: " + currentGame.getCivilianWord());
            }
            player.sendMessage("§e游戏开始！请轮流描述你手中的词语，但不要直接说出");
            player.sendMessage("§e描述完毕后，使用 §f/spy vote §e来投票选出你认为是卧底的玩家");
        }

        String message = "§b游戏开始！总共有 " + currentGame.getPlayers().size() + " 名玩家，其中 1 名是卧底";
        // if (whiteboard != null) {
        // message += "，1 名是白板";
        // }
        currentGame.broadcast(message);
    }

    public static void resetGame() {
        if (game != null) {
            game = null;
        }
    }

    public static void forceEndGame(CommandSender sender) {
        Game currentGame = getGame();
        if (currentGame != null && currentGame.isGaming()) {
            currentGame.broadcast("§c游戏已被管理员 " + sender.getName() + " 强制结束！");
            resetGame();
        } else {
            sender.sendMessage("§c当前没有正在进行的游戏。");
        }
    }


    public static void tallyVotes() {
        Game currentGame = getGame();
        if (!currentGame.isGaming()) {
            return;
        }

        // 显示投票情况
        broadcastVoteStatus(currentGame);

        // 票数统计
        Map<Player, Integer> voteCounts = new HashMap<>();
        int abstainVotes = 0;
        for (Player votedFor : currentGame.getVotes().values()) {
            if (votedFor == null) {
                abstainVotes++;
            } else {
                voteCounts.put(votedFor, voteCounts.getOrDefault(votedFor, 0) + 1);
            }
        }

        // 找出得票最多的玩家
        Player eliminatedPlayer = null;
        int maxVotes = 0;
        boolean tie = false;

        if (!voteCounts.isEmpty()) {
            List<Map.Entry<Player, Integer>> sortedVotes = new ArrayList<>(voteCounts.entrySet());
            sortedVotes.sort(Map.Entry.<Player, Integer>comparingByValue().reversed());

            eliminatedPlayer = sortedVotes.get(0).getKey();
            maxVotes = sortedVotes.get(0).getValue();

            if (sortedVotes.size() > 1 && sortedVotes.get(1).getValue() == maxVotes) {
                tie = true;
            }
        }


        // 如果平票，则重新投票
        if (tie || voteCounts.isEmpty()) {
            String message = "§e平票！";
            if (voteCounts.isEmpty() && abstainVotes > 0) {
                message = "§e所有人都弃权了！";
            }
            currentGame.broadcast(message + "没有人被淘汰，开始新一轮描述和投票。");
            currentGame.clearVotes();
            return; // 保持PLAYING状态，开始新一轮
        }

        // 宣布结果
        if (eliminatedPlayer != null) {
            plugin.getServer()
                    .broadcastMessage("§e" + eliminatedPlayer.getName() + " §c被投票出局，获得了 §e" + maxVotes + " §c票。");
            currentGame.removePlayer(eliminatedPlayer); // 从游戏中移除玩家

            // 检查游戏是否结束
            if (eliminatedPlayer.equals(currentGame.getSpy())) {
                // 如果卧底被抓住了，检查白板是否存活
                if (currentGame.getWhiteboard() != null
                        && currentGame.getPlayers().contains(currentGame.getWhiteboard())) {
                    plugin.getServer().broadcastMessage("§a卧底被抓住了！§f白板胜利！");
                } else {
                    plugin.getServer().broadcastMessage("§a卧底被抓住了！平民胜利！");
                }
                endGame();
            } else if (eliminatedPlayer.equals(currentGame.getWhiteboard())) {
                plugin.getServer().broadcastMessage("§f白板被投票出局！游戏继续。");
                currentGame.clearVotes(); // 清票继续
            } else {
                plugin.getServer().broadcastMessage("§c被淘汰的不是卧底！游戏继续。");
                // 卧底胜利判定：当 卧底+白板 >= 平民时
                int remainingPlayers = currentGame.getPlayers().size();
                int spyAndWhiteboardCount = 0;
                if (currentGame.getPlayers().contains(currentGame.getSpy())) {
                    spyAndWhiteboardCount++;
                }
                if (currentGame.getWhiteboard() != null
                        && currentGame.getPlayers().contains(currentGame.getWhiteboard())) {
                    spyAndWhiteboardCount++;
                }

                if (remainingPlayers <= 2 || spyAndWhiteboardCount >= (remainingPlayers - spyAndWhiteboardCount)) {
                    plugin.getServer().broadcastMessage("§c场上平民数量不足，卧底胜利！");
                    endGame();
                } else {
                    // 清除投票，开始下一轮
                    currentGame.clearVotes();
                }
            }
        }
    }

    public static void endGame() {
        Game currentGame = getGame();
        if (currentGame != null && currentGame.isGaming()) {
            announcePlayerWords(currentGame);
            currentGame.broadcast("§e游戏结束！§75秒后将重置游戏房间。");
            // 延迟重置
            plugin.getServer().getScheduler().runTaskLater(plugin, GameManager::resetGame, 100L); // 5秒
        }
    }

    private static void broadcastVoteStatus(Game game) {
        game.broadcast("§e--- §f投票情况 §e---");
        Map<Player, List<Player>> votesToPlayer = new HashMap<>();
        for (Map.Entry<Player, Player> entry : game.getVotes().entrySet()) {
            Player voter = entry.getKey();
            Player voted = entry.getValue();
            if (voted != null) {
                votesToPlayer.computeIfAbsent(voted, k -> new ArrayList<>()).add(voter);
            } else {
                votesToPlayer.computeIfAbsent(null, k -> new ArrayList<>()).add(voter);
            }
        }

        for (Map.Entry<Player, List<Player>> entry : votesToPlayer.entrySet()) {
            Player voted = entry.getKey();
            List<Player> voters = entry.getValue();
            StringBuilder voterNames = new StringBuilder();
            for (int i = 0; i < voters.size(); i++) {
                voterNames.append(voters.get(i).getName());
                if (i < voters.size() - 1) {
                    voterNames.append("、");
                }
            }
            if (voted != null) {
                game.broadcast(voterNames.toString() + " §7投给§f " + voted.getName());
            } else {
                game.broadcast(voterNames.toString() + " §7选择了§c弃权");
            }
        }
    }

    private static void announcePlayerWords(Game game) {
        game.broadcast("§e--- §f玩家词条公布 §e---");
        Map<String, List<String>> wordToPlayers = new HashMap<>();
        for (Map.Entry<Player, String> entry : game.getPlayerWords().entrySet()) {
            String playerName = entry.getKey().getName();
            String word = entry.getValue();
            wordToPlayers.computeIfAbsent(word, k -> new ArrayList<>()).add(playerName);
        }

        for (Map.Entry<String, List<String>> entry : wordToPlayers.entrySet()) {
            String word = entry.getKey();
            List<String> playerNames = entry.getValue();
            game.broadcast(word + ": " + String.join(", ", playerNames));
        }
    }

    public static void playerQuit(Player player) {
        Game currentGame = getGame();
        if (currentGame == null || !currentGame.getPlayers().contains(player)) {
            return;
        }

        currentGame.broadcast("§e" + player.getName() + " §c退出了游戏。");

        if (currentGame.isGaming()) {
            if (player.equals(currentGame.getSpy())) {
                if (currentGame.getWhiteboard() != null
                        && currentGame.getPlayers().contains(currentGame.getWhiteboard())) {
                    currentGame.broadcast("§c卧底退出了游戏！§f白板胜利！");
                } else {
                    currentGame.broadcast("§c卧底退出了游戏！§a平民胜利！");
                }
                endGame();
            } else if (player.equals(currentGame.getWhiteboard())) {
                currentGame.broadcast("§f白板退出了游戏！游戏继续。");
                currentGame.removePlayer(player);
            } else {
                currentGame.removePlayer(player); // 从游戏中移除
                if (currentGame.getPlayers().size() <= 2) {
                    plugin.getServer().broadcastMessage("§c场上只剩下两名玩家，卧底胜利！");
                    endGame();
                } else {
                    // 如果在投票阶段，检查是否可以结束投票
                    if (currentGame.getVotes().size() >= currentGame.getPlayers().size()
                            && currentGame.getPlayers().size() > 0) {
                        tallyVotes();
                    }
                }
            }
        } else {
            currentGame.removePlayer(player);
        }
    }
}