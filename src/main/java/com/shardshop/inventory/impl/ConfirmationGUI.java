package com.shardshop.inventory.impl;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.shardshop.ShardShop;
import com.shardshop.config.ConfigManager;
import com.shardshop.inventory.InventoryButton;
import com.shardshop.inventory.InventoryGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConfirmationGUI extends InventoryGUI {
    private final ShardShop plugin;
    private final String title;
    private final Runnable onConfirm;
    private final Runnable onCancel;
    private static final Map<UUID, BukkitTask> timeoutTasks = new HashMap<>();
    
    public ConfirmationGUI(ShardShop plugin, String title, Runnable onConfirm, Runnable onCancel) {
        this.plugin = plugin;
        this.title = title;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }
    
    @Override
    protected Inventory createInventory() {
        ConfigManager cm = plugin.getConfigManager();
        String confirmTitle = cm.colorize(cm.getConfig().getString("confirmation.title", "&c&lConfirm Action"));
        if (title != null) {
            confirmTitle = cm.colorize(title);
        }
        return Bukkit.createInventory(null, 27, confirmTitle);
    }
    
    @Override
    public void decorate(Player player) {
        Inventory inv = getInventory();
        inv.clear();
        
        ItemStack filler = XMaterial.GRAY_STAINED_GLASS_PANE.parseItem();
        if (filler != null) {
            ItemMeta meta = filler.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(" ");
                filler.setItemMeta(meta);
            }
            
            for (int i = 0; i < inv.getSize(); i++) {
                inv.setItem(i, filler);
            }
        }
        
        ConfigManager cm = plugin.getConfigManager();
        int confirmSlot = cm.getConfig().getInt("confirmation.confirm_slot", 11);
        int cancelSlot = cm.getConfig().getInt("confirmation.cancel_slot", 15);
        
        addButton(confirmSlot, new InventoryButton()
            .creator(p -> createConfirmButton())
            .consumer(event -> {
                Player clicker = (Player) event.getWhoClicked();
                cancelTimeout(clicker.getUniqueId());
                clicker.closeInventory();
                playSound(clicker, "item_added");
                if (onConfirm != null) {
                    onConfirm.run();
                }
            })
        );
        
        addButton(cancelSlot, new InventoryButton()
            .creator(p -> createCancelButton())
            .consumer(event -> {
                Player clicker = (Player) event.getWhoClicked();
                cancelTimeout(clicker.getUniqueId());
                clicker.closeInventory();
                clicker.sendMessage(cm.getMessage("confirmation.cancelled"));
                playSound(clicker, "error");
                if (onCancel != null) {
                    onCancel.run();
                }
            })
        );
        
        super.decorate(player);
        startTimeout(player);
    }
    
    private ItemStack createConfirmButton() {
        ConfigManager cm = plugin.getConfigManager();
        ItemStack item = XMaterial.LIME_WOOL.parseItem();
        if (item == null) return null;
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(cm.colorize(cm.getMessageNoPrefix("confirmation.confirm_button")));
            meta.setLore(Arrays.asList(
                cm.colorize("&7Click to confirm this action")
            ));
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private ItemStack createCancelButton() {
        ConfigManager cm = plugin.getConfigManager();
        ItemStack item = XMaterial.RED_WOOL.parseItem();
        if (item == null) return null;
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(cm.colorize(cm.getMessageNoPrefix("confirmation.cancel_button")));
            meta.setLore(Arrays.asList(
                cm.colorize("&7Click to cancel this action")
            ));
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private void startTimeout(Player player) {
        ConfigManager cm = plugin.getConfigManager();
        int timeout = cm.getConfig().getInt("confirmation.timeout_seconds", 30);
        
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && player.getOpenInventory().getTopInventory().equals(getInventory())) {
                player.closeInventory();
                player.sendMessage(cm.getMessage("confirmation.expired"));
                playSound(player, "error");
            }
            timeoutTasks.remove(player.getUniqueId());
        }, timeout * 20L);
        
        timeoutTasks.put(player.getUniqueId(), task);
    }
    
    private void cancelTimeout(UUID uuid) {
        BukkitTask task = timeoutTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }
    
    private void playSound(Player player, String soundKey) {
        ConfigManager cm = plugin.getConfigManager();
        if (!cm.getConfig().getBoolean("sounds.enabled", true)) return;
        
        String soundName = cm.getConfig().getString("sounds." + soundKey);
        if (soundName == null) return;
        
        float volume = (float) cm.getConfig().getDouble("sounds.volume", 1.0);
        float pitch = (float) cm.getConfig().getDouble("sounds.pitch", 1.0);
        
        XSound.matchXSound(soundName).ifPresent(xSound -> 
            xSound.play(player, volume, pitch)
        );
    }
}