package org.encinet.whosTheSpy;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class VoteGUI implements Listener {

    private static final String GUI_TITLE = "§e投票";

    public static void open(Player player) {
        Game game = GameManager.getGame();
        String title = GUI_TITLE;
        Player votedFor = game.getVotes().get(player);
        if (votedFor != null) {
            title = "§e投票 §f| §a你当前选中: §e" + votedFor.getName();
        } else if (game.getVotes().containsKey(player)) {
            title = "§e投票 §f| §a你当前选中: §c弃权";
        }
        int size = (int) Math.ceil((game.getPlayers().size() + 1) / 9.0) * 9;
        if (size == 0) size = 9;
        Inventory gui = Bukkit.createInventory(null, size, title);

        for (Player p : game.getPlayers()) {
            if (!player.getUniqueId().equals(p.getUniqueId())) {
                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                if (meta != null) {
                    meta.setOwningPlayer(p);
                    meta.setDisplayName("§f" + p.getName());
                    skull.setItemMeta(meta);
                }
                gui.addItem(skull);
            }
        }

        // 弃权按钮
        ItemStack abstainItem = new ItemStack(Material.BARRIER);
        ItemMeta abstainMeta = abstainItem.getItemMeta();
        if (abstainMeta != null) {
            abstainMeta.setDisplayName("§c弃权");
            abstainItem.setItemMeta(abstainMeta);
        }
        gui.setItem(size - 1, abstainItem);


        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String viewTitle = event.getView().getTitle();
        if (!viewTitle.startsWith(GUI_TITLE)) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType().isAir()) {
            return;
        }

        Game game = GameManager.getGame();

        if (clickedItem.getType() == Material.BARRIER) {
            game.vote(player, null); // 弃权
            player.sendMessage("§a你选择了弃权");
            player.closeInventory();

            // 检查是否所有人都投票了
            if (game.getVotes().size() == game.getPlayers().size()) {
                GameManager.tallyVotes();
            }
            return;
        }

        if (clickedItem.getType() != Material.PLAYER_HEAD) {
            return;
        }

        SkullMeta meta = (SkullMeta) clickedItem.getItemMeta();
        if (meta == null || meta.getOwningPlayer() == null) {
            return;
        }

        Player target = (Player) meta.getOwningPlayer();

        game.vote(player, target);
        player.sendMessage("§a你投票给了 §e" + target.getName());
        player.closeInventory();

        // 检查是否所有人都投票了
        if (game.getVotes().size() == game.getPlayers().size()) {
            GameManager.tallyVotes();
        }
    }
}
