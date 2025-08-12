package org.encinet.whosTheSpy.service;

import org.bukkit.entity.Player;
import org.encinet.whosTheSpy.Game;
import org.encinet.whosTheSpy.model.PlayerState;
import java.util.*;

public class VoteService {
    private final GameService gameService;
    
    public VoteService(GameService gameService) {
        this.gameService = gameService;
    }
    
    public void tallyVotes(Game game) {
        if (!game.isGaming()) {
            return;
        }

        // 显示投票情况
        broadcastVoteStatus(game);

        // 票数统计
        VoteResult result = countVotes(game);
        Player eliminatedPlayer = result.eliminatedPlayer;
        int maxVotes = result.maxVotes;
        boolean tie = result.tie;
        int abstainVotes = result.abstainVotes;

        // 如果平票，则重新投票
        if (tie || result.voteCounts.isEmpty()) {
            String message = "§e平票！";
            if (result.voteCounts.isEmpty() && abstainVotes > 0) {
                message = "§e所有人都弃权了！";
            }
            game.broadcast(message + "没有人被淘汰，开始新一轮描述和投票。");
            game.clearVotes();
            return; // 开始新一轮
        }

        // 宣布结果
        if (eliminatedPlayer != null) {
            game.broadcastToAll("§e" + eliminatedPlayer.getName() + " §c被投票出局，获得了 §e" + maxVotes + " §c票。");
            game.setPlayerState(eliminatedPlayer, PlayerState.SPECTATING); // 状态变为观战

            // 检查游戏是否结束
            if (eliminatedPlayer.equals(game.getSpy())) {
                // 如果卧底被抓住了，检查白板是否存活
                if (game.getWhiteboard() != null
                        && game.getPlayers().contains(game.getWhiteboard())) {
                    game.broadcastToAll("§a卧底被抓住了！§f白板胜利！");
                } else {
                    game.broadcastToAll("§a卧底被抓住了！平民胜利！");
                }
                gameService.endGame();
            } else if (eliminatedPlayer.equals(game.getWhiteboard())) {
                game.broadcastToAll("§f白板被投票出局！游戏继续。");
                game.clearVotes(); // 清票继续
            } else {
                game.broadcastToAll("§c被淘汰的不是卧底！游戏继续。");
                // 卧底胜利判定：当 卧底+白板 >= 平民时
                int remainingPlayers = game.getActivePlayers().size();
                int spyAndWhiteboardCount = 0;
                if (game.getActivePlayers().contains(game.getSpy())) {
                    spyAndWhiteboardCount++;
                }
                if (game.getWhiteboard() != null
                        && game.getActivePlayers().contains(game.getWhiteboard())) {
                    spyAndWhiteboardCount++;
                }

                if (remainingPlayers <= 2 || spyAndWhiteboardCount >= (remainingPlayers - spyAndWhiteboardCount)) {
                    game.broadcastToAll("§c场上平民数量不足，卧底胜利！");
                    // 通过GameService结束游戏
                    gameService.endGame();
                } else {
                    // 清除投票，开始下一轮
                    game.clearVotes();
                }
            }
        }
    }
    
    private VoteResult countVotes(Game game) {
        Map<Player, Integer> voteCounts = new HashMap<>();
        int abstainVotes = 0;
        for (Player votedFor : game.getVotes().values()) {
            if (votedFor == null) {
                abstainVotes++;
            } else {
                voteCounts.put(votedFor, voteCounts.getOrDefault(votedFor, 0) + 1);
            }
        }

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

        return new VoteResult(voteCounts, abstainVotes, eliminatedPlayer, maxVotes, tie);
    }
    
    private static class VoteResult {
        final Map<Player, Integer> voteCounts;
        final int abstainVotes;
        final Player eliminatedPlayer;
        final int maxVotes;
        final boolean tie;

        VoteResult(Map<Player, Integer> voteCounts, int abstainVotes, Player eliminatedPlayer, int maxVotes,
                boolean tie) {
            this.voteCounts = voteCounts;
            this.abstainVotes = abstainVotes;
            this.eliminatedPlayer = eliminatedPlayer;
            this.maxVotes = maxVotes;
            this.tie = tie;
        }
    }
    
    private void broadcastVoteStatus(Game game) {
        game.broadcastToAll("§6===== §e投票结果 §6=====");
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
                game.broadcastToAll(voterNames.toString() + " §7投给§f " + voted.getName());
            } else {
                game.broadcastToAll(voterNames.toString() + " §7选择了§c弃权");
            }
        }
    }
}