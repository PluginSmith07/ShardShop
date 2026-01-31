package com.shardshop.inventory.impl;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.shardshop.ShardShop;
import com.shardshop.config.ConfigManager;
import com.shardshop.economy.EconomyManager;
import com.shardshop.inventory.InventoryButton;
import com.shardshop.inventory.InventoryGUI;
import com.shardshop.shop.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ShopGUI extends InventoryGUI {
    private final ShardShop plugin;
    
    public ShopGUI(ShardShop plugin) {
        this.plugin = plugin;
    }
    
    @Override
    protected Inventory createInventory() {
        ConfigManager cm = plugin.getConfigManager();
        String title = cm.colorize(cm.getConfig().getString("shop.title", "&6&lShard Shop"));
        int rows = cm.getConfig().getInt("shop.rows", 6);
        return Bukkit.createInventory(null, rows * 9, title);
    }
    
    @Override
    public void decorate(Player player) {
        Inventory inv = getInventory();
        inv.clear();
        
        ConfigManager cm = plugin.getConfigManager();
        boolean fillEmpty = cm.getConfig().getBoolean("shop.fill_empty_slots", true);
        
        if (fillEmpty) {
            String fillMat = cm.getConfig().getString("shop.fill_material", "BLACK_STAINED_GLASS_PANE");
            ItemStack filler = XMaterial.matchXMaterial(fillMat)
                .map(XMaterial::parseItem)
                .orElse(XMaterial.BLACK_STAINED_GLASS_PANE.parseItem());
                
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
        }
        
        List<ShopItem> items = plugin.getShopManager().getShopItems();
        
        if (items.isEmpty()) {
            player.sendMessage(cm.getMessage("shop.empty"));
            player.closeInventory();
            return;
        }
        
        for (int i = 0; i < items.size() && i < inv.getSize(); i++) {
            final int slot = i;
            final ShopItem shopItem = items.get(i);
            
            addButton(slot, new InventoryButton()
                .creator(p -> createShopItemDisplay(shopItem))
                .consumer(event -> {
                    Player clicker = (Player) event.getWhoClicked();
                    
                    if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.SHIFT_LEFT) {
                        handlePurchase(clicker, shopItem);
                    } else if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
                        handleSell(clicker, shopItem);
                    }
                })
            );
        }
        
        super.decorate(player);
    }
    
    private ItemStack createShopItemDisplay(ShopItem shopItem) {
        ItemStack display = shopItem.toItemStack();
        if (display == null) return null;
        
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;
        
        ConfigManager cm = plugin.getConfigManager();
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        
        List<String> purchaseLore = cm.getConfig().getStringList("display.purchase_lore");
        for (String line : purchaseLore) {
            String formatted = line
                .replace("%price%", cm.formatPrice(shopItem.getPrice()))
                .replace("%sell_price%", cm.formatSellPrice(plugin.getShopManager().getSellPrice(shopItem.getPrice())));
            lore.add(cm.colorize(formatted));
        }
        
        meta.setLore(lore);
        display.setItemMeta(meta);
        
        return display;
    }
    
    private void handlePurchase(Player player, ShopItem shopItem) {
        ConfigManager cm = plugin.getConfigManager();
        EconomyManager economy = plugin.getEconomyManager();
        
        double price = shopItem.getPrice();
        
        if (!economy.has(player, price)) {
            player.sendMessage(cm.getMessage("purchase.insufficient_funds")
                .replace("%price%", cm.formatPrice(price)));
            playSound(player, "error");
            return;
        }
        
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(cm.getMessage("purchase.insufficient_space"));
            playSound(player, "error");
            return;
        }
        
        economy.withdraw(player, price);
        
        ItemStack item = shopItem.toItemStack();
        if (item != null) {
            player.getInventory().addItem(item);
        }
        
        String itemName = shopItem.getDisplayName() != null ? 
            shopItem.getDisplayName() : 
            XMaterial.matchXMaterial(shopItem.getMaterial()).map(xm -> {
                String name = xm.name().replace("_", " ");
                return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
            }).orElse(shopItem.getMaterial());
        
        player.sendMessage(cm.getMessage("purchase.success")
            .replace("%amount%", "1")
            .replace("%item%", itemName)
            .replace("%price%", cm.formatPrice(price)));
        
        playSound(player, "item_purchase");
    }
    
    private void handleSell(Player player, ShopItem shopItem) {
        ConfigManager cm = plugin.getConfigManager();
        
        if (!plugin.getShopManager().isSellBackEnabled()) {
            player.sendMessage(cm.getMessage("sell.disabled"));
            playSound(player, "error");
            return;
        }
        
        ItemStack toSell = null;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && shopItem.matches(item)) {
                toSell = item;
                break;
            }
        }
        
        if (toSell == null) {
            player.sendMessage(cm.getMessage("sell.no_item"));
            playSound(player, "error");
            return;
        }
        
        double sellPrice = plugin.getShopManager().getSellPrice(shopItem.getPrice());
        
        player.getInventory().removeItem(toSell);
        plugin.getEconomyManager().deposit(player, sellPrice);
        
        String itemName = shopItem.getDisplayName() != null ? 
            shopItem.getDisplayName() : 
            XMaterial.matchXMaterial(shopItem.getMaterial()).map(xm -> {
                String name = xm.name().replace("_", " ");
                return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
            }).orElse(shopItem.getMaterial());
        
        player.sendMessage(cm.getMessage("sell.success")
            .replace("%amount%", "1")
            .replace("%item%", itemName)
            .replace("%price%", cm.formatSellPrice(sellPrice)));
        
        playSound(player, "item_sell");
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