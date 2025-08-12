package org.encinet.whosTheSpy;

import org.encinet.whosTheSpy.service.GameService;
import org.encinet.whosTheSpy.service.VoteService;

public final class GameManager {
    private static GameService gameService;
    private static VoteService voteService;

    public static void init(WhosTheSpy instance) {
        gameService = new GameService(instance);
        voteService = new VoteService(gameService);
        gameService.setVoteService(voteService); // 注入VoteService
    }

    public static Game getGame() {
        return gameService.getCurrentGame();
    }

    public static void startGame() {
        gameService.startGame();
    }

    public static void tallyVotes() {
        voteService.tallyVotes(gameService.getCurrentGame());
    }

    public static void resetGame() {
        gameService.resetGame();
    }

    public static void forceEndGame(org.bukkit.command.CommandSender sender) {
        gameService.forceEndGame(sender);
    }

    public static void endGame() {
        gameService.endGame();
    }

    public static void playerQuit(org.bukkit.entity.Player player, boolean notify) {
        gameService.playerQuit(player, notify);
    }
}