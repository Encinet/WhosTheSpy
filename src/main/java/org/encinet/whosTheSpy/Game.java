package org.encinet.whosTheSpy;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 代表一局“谁是卧底”游戏。
 * 此类管理游戏状态、玩家、词语、角色和投票。
 */
public class Game {

    /**
     * 游戏是否处于游戏状态。
     */
    private boolean gaming = false;
    /**
     * 当前在游戏中的所有玩家的集合。
     */
    private final Set<Player> players = new HashSet<>();
    /**
     * 已表示准备好开始游戏的玩家的集合。
     */
    private final Set<Player> readyPlayers = new HashSet<>();
    /**
     * 平民玩家的词语。
     */
    private String civilianWord;
    /**
     * 卧底玩家的词语。
     */
    private String spyWord;
    /**
     * 被分配为卧底角色的玩家。
     */
    private Player spy;
    /**
     * 被分配为白板角色的玩家 (没有词语)。
     */
    private Player whiteboard;
    /**
     * 存储投票的映射。
     * Key: 投票的玩家 (voter)
     * Value: 被投票的玩家 (target)
     */
    private final Map<Player, Player> votes = new HashMap<>();
    /**
     * 存储分配给每个玩家的词语的映射。
     * Key: 玩家 (player)
     * Value: 分配给玩家的词语 (word)
     */
    private final Map<Player, String> playerWords = new HashMap<>();

    /**
     * 用给定的词语初始化一个新游戏。
     * @param civilianWord 平民的词语
     * @param spyWord 卧底的词语
     */
    public Game(String civilianWord, String spyWord) {
        this.civilianWord = civilianWord;
        this.spyWord = spyWord;
    }

    // Getter和Setter

    public boolean isGaming() {
        return gaming;
    }

    public void setGaming(boolean gaming) {
        this.gaming = gaming;
    }

    public Set<Player> getPlayers() {
        return players;
    }

    public Set<Player> getReadyPlayers() {
        return readyPlayers;
    }

    public String getCivilianWord() {
        return civilianWord;
    }

    public String getSpyWord() {
        return spyWord;
    }

    public Player getSpy() {
        return spy;
    }

    public void setSpy(Player spy) {
        this.spy = spy;
    }

    public Player getWhiteboard() {
        return whiteboard;
    }

    public void setWhiteboard(Player whiteboard) {
        this.whiteboard = whiteboard;
    }

    public Map<Player, Player> getVotes() {
        return votes;
    }

    public Map<Player, String> getPlayerWords() {
        return playerWords;
    }

    /**
     * 将一个玩家添加到游戏中。
     * @param player 要添加的玩家
     */
    public void addPlayer(Player player) {
        players.add(player);
    }

    /**
     * 从游戏中移除一个玩家及其相关数据 (准备状态, 投票)。
     * @param player 要移除的玩家
     */
    public void removePlayer(Player player) {
        players.remove(player);
        readyPlayers.remove(player);
        votes.remove(player);
    }

    /**
     * 将一个玩家标记为已准备。
     * @param player 要标记为已准备的玩家
     */
    public void addReadyPlayer(Player player) {
        readyPlayers.add(player);
    }

    /**
     * 取消一个玩家的准备状态。
     * @param player 要取消准备状态的玩家
     */
    public void removeReadyPlayer(Player player) {
        readyPlayers.remove(player);
    }

    /**
     * 记录一个玩家对另一个玩家的投票。
     * @param voter 投票的玩家
     * @param target 被投票的玩家
     */
    public void vote(Player voter, Player target) {
        votes.put(voter, target);
    }

    /**
     * 清除所有已记录的投票。
     */
    public void clearVotes() {
        votes.clear();
    }

    /**
     * 为一个玩家分配一个特定的词语。
     * @param player 玩家
     * @param word 分配的词语
     */
    public void setPlayerWord(Player player, String word) {
        playerWords.put(player, word);
    }

    /**
     * 向游戏中的所有玩家发送一条消息。
     * @param message 要发送的消息
     */
    public void broadcast(String message) {
        for (Player player : players) {
            player.sendMessage(message);
        }
    }
}