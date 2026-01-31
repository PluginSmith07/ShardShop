package com.shardshop.commands;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.shardshop.ShardShop;
import com.shardshop.config.ConfigManager;
import com.shardshop.inventory.impl.ConfirmationGUI;
import com.shardshop.inventory.impl.ShopGUI;
import com.shardshop.shop.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ShopCommand implements CommandExecutor, TabCompleter {
    private final ShardShop plugin;
    
    public ShopCommand(ShardShop plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("shop")) {
            return handleShopCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("shopadmin")) {
            return handleAdminCommand(sender, args);
        }
        return false;
    }
    
    private boolean handleShopCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }
        
        Player player = (Player) sender;
        ConfigManager cm = plugin.getConfigManager();
        
        if (args.length == 0) {
            if (!player.hasPermission("shardshop.use")) {
                player.sendMessage(cm.getMessage("admin.no_permission"));
                return true;
            }
            
            if (plugin.getShopManager().getShopItems().isEmpty()) {
                player.sendMessage(cm.getMessage("shop.empty"));
                return true;
            }
            
            ShopGUI shopGUI = new ShopGUI(plugin);
            plugin.getGuiManager().openGUI(shopGUI, player);
            playSound(player, "shop_open");
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "balance":
            case "bal":
                return handleBalance(player, args);
            case "pay":
                return handlePay(player, args);
            default:
                player.sendMessage(cm.colorize("&cUsage: /shop [balance|pay]"));
                return true;
        }
    }
    
    private boolean handleBalance(Player player, String[] args) {
        ConfigManager cm = plugin.getConfigManager();
        
        if (!player.hasPermission("shardshop.balance")) {
            player.sendMessage(cm.getMessage("admin.no_permission"));
            return true;
        }
        
        if (args.length == 1) {
            double balance = plugin.getEconomyManager().getBalance(player);
            player.sendMessage(cm.getMessage("economy.balance")
                .replace("%balance%", cm.formatPrice(balance)));
        } else {
            if (!player.hasPermission("shardshop.balance.others")) {
                player.sendMessage(cm.getMessage("admin.no_permission"));
                return true;
            }
            
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                player.sendMessage(cm.getMessage("economy.player_not_found")
                    .replace("%player%", args[1]));
                return true;
            }
            
            double balance = plugin.getEconomyManager().getBalance(target);
            player.sendMessage(cm.getMessage("economy.balance_other")
                .replace("%player%", target.getName())
                .replace("%balance%", cm.formatPrice(balance)));
        }
        
        return true;
    }
    
    private boolean handlePay(Player player, String[] args) {
        ConfigManager cm = plugin.getConfigManager();
        
        if (!player.hasPermission("shardshop.pay")) {
            player.sendMessage(cm.getMessage("admin.no_permission"));
            return true;
        }
        
        if (args.length < 3) {
            player.sendMessage(cm.colorize("&cUsage: /shop pay <player> <amount>"));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(cm.getMessage("economy.player_not_found")
                .replace("%player%", args[1]));
            return true;
        }
        
        if (target.equals(player)) {
            player.sendMessage(cm.getMessage("economy.cannot_pay_self"));
            return true;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
            if (amount <= 0) {
                player.sendMessage(cm.getMessage("economy.invalid_amount"));
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(cm.getMessage("economy.invalid_amount"));
            return true;
        }
        
        if (!plugin.getEconomyManager().has(player, amount)) {
            player.sendMessage(cm.getMessage("economy.insufficient_funds")
                .replace("%amount%", cm.formatPrice(amount)));
            return true;
        }
        
        plugin.getEconomyManager().transfer(player, target, amount);
        
        player.sendMessage(cm.getMessage("economy.pay_sent")
            .replace("%player%", target.getName())
            .replace("%amount%", cm.formatPrice(amount)));
        
        target.sendMessage(cm.getMessage("economy.pay_received")
            .replace("%player%", player.getName())
            .replace("%amount%", cm.formatPrice(amount)));
        
        playSound(player, "item_purchase");
        playSound(target, "item_purchase");
        
        return true;
    }
    
    private boolean handleAdminCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }
        
        Player player = (Player) sender;
        ConfigManager cm = plugin.getConfigManager();
        
        if (!player.hasPermission("shardshop.admin")) {
            player.sendMessage(cm.getMessage("admin.no_permission"));
            return true;
        }
        
        if (args.length == 0) {
            sendAdminHelp(player);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "add":
                return handleAdd(player, args);
            case "addname":
                return handleAddName(player, args);
            case "remove":
                return handleRemove(player);
            case "reload":
                return handleReload(player);
            case "list":
                return handleList(player);
            case "clear":
                return handleClear(player);
            case "set":
                return handleSetBalance(player, args);
            case "give":
                return handleGiveBalance(player, args);
            case "take":
                return handleTakeBalance(player, args);
            default:
                sendAdminHelp(player);
                return true;
        }
    }
    
    private boolean handleAdd(Player player, String[] args) {
        ConfigManager cm = plugin.getConfigManager();
        
        if (args.length < 2) {
            player.sendMessage(cm.colorize("&cUsage: /shopadmin add <price>"));
            return true;
        }
        
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType().isAir()) {
            player.sendMessage(cm.getMessage("admin.hold_item"));
            return true;
        }
        
        double price;
        try {
            price = Double.parseDouble(args[1]);
            if (price <= 0) {
                player.sendMessage(cm.getMessage("admin.invalid_price"));
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(cm.getMessage("admin.invalid_price"));
            return true;
        }
        
        if (plugin.getShopManager().itemExists(heldItem)) {
            player.sendMessage(cm.getMessage("admin.item_already_exists"));
            return true;
        }
        
        ShopItem shopItem = new ShopItem(heldItem, price);
        String itemName = heldItem.hasItemMeta() && heldItem.getItemMeta().hasDisplayName() ?
            heldItem.getItemMeta().getDisplayName() :
            XMaterial.matchXMaterial(heldItem).name();
        
        String confirmTitle = cm.getMessageNoPrefix("confirmation.title_add")
            .replace("%item%", itemName);
        
        ConfirmationGUI confirmGUI = new ConfirmationGUI(
            plugin,
            confirmTitle,
            () -> {
                plugin.getShopManager().addItem(shopItem);
                player.sendMessage(cm.getMessage("admin.item_added")
                    .replace("%item%", itemName)
                    .replace("%price%", cm.formatPrice(price)));
                playSound(player, "item_added");
            },
            null
        );
        
        plugin.getGuiManager().openGUI(confirmGUI, player);
        playSound(player, "confirmation_open");
        
        return true;
    }
    
    private boolean handleAddName(Player player, String[] args) {
        ConfigManager cm = plugin.getConfigManager();
        
        if (args.length < 3) {
            player.sendMessage(cm.colorize("&cUsage: /shopadmin addname <material> <price>"));
            return true;
        }
        
        String materialName = args[1].toUpperCase();
        XMaterial xMaterial = XMaterial.matchXMaterial(materialName).orElse(null);
        
        if (xMaterial == null) {
            player.sendMessage(cm.getMessage("admin.invalid_material")
                .replace("%material%", materialName));
            return true;
        }
        
        double price;
        try {
            price = Double.parseDouble(args[2]);
            if (price <= 0) {
                player.sendMessage(cm.getMessage("admin.invalid_price"));
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(cm.getMessage("admin.invalid_price"));
            return true;
        }
        
        ItemStack item = xMaterial.parseItem();
        if (item == null) {
            player.sendMessage(cm.getMessage("admin.invalid_material")
                .replace("%material%", materialName));
            return true;
        }
        
        if (plugin.getShopManager().itemExists(item)) {
            player.sendMessage(cm.getMessage("admin.item_already_exists"));
            return true;
        }
        
        ShopItem shopItem = new ShopItem(item, price);
        
        String confirmTitle = cm.getMessageNoPrefix("confirmation.title_add")
            .replace("%item%", xMaterial.name());
        
        ConfirmationGUI confirmGUI = new ConfirmationGUI(
            plugin,
            confirmTitle,
            () -> {
                plugin.getShopManager().addItem(shopItem);
                player.sendMessage(cm.getMessage("admin.item_added")
                    .replace("%item%", xMaterial.name())
                    .replace("%price%", cm.formatPrice(price)));
                playSound(player, "item_added");
            },
            null
        );
        
        plugin.getGuiManager().openGUI(confirmGUI, player);
        playSound(player, "confirmation_open");
        
        return true;
    }
    
    private boolean handleRemove(Player player) {
        ConfigManager cm = plugin.getConfigManager();
        
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType().isAir()) {
            player.sendMessage(cm.getMessage("admin.hold_item"));
            return true;
        }
        
        if (!plugin.getShopManager().itemExists(heldItem)) {
            player.sendMessage(cm.getMessage("admin.item_not_found"));
            return true;
        }
        
        String itemName = heldItem.hasItemMeta() && heldItem.getItemMeta().hasDisplayName() ?
            heldItem.getItemMeta().getDisplayName() :
            XMaterial.matchXMaterial(heldItem).name();
        
        String confirmTitle = cm.getMessageNoPrefix("confirmation.title_remove")
            .replace("%item%", itemName);
        
        ItemStack itemToRemove = heldItem.clone();
        
        ConfirmationGUI confirmGUI = new ConfirmationGUI(
            plugin,
            confirmTitle,
            () -> {
                if (plugin.getShopManager().removeItem(itemToRemove)) {
                    player.sendMessage(cm.getMessage("admin.item_removed")
                        .replace("%item%", itemName));
                    playSound(player, "item_removed");
                } else {
                    player.sendMessage(cm.getMessage("admin.item_not_found"));
                }
            },
            null
        );
        
        plugin.getGuiManager().openGUI(confirmGUI, player);
        playSound(player, "confirmation_open");
        
        return true;
    }
    
    private boolean handleReload(Player player) {
        ConfigManager cm = plugin.getConfigManager();
        
        try {
            cm.reloadConfigs();
            plugin.getShopManager().loadShopItems();
            plugin.getEconomyManager().loadBalances();
            player.sendMessage(cm.getMessage("admin.reload_success"));
            playSound(player, "item_added");
        } catch (Exception e) {
            player.sendMessage(cm.getMessage("errors.command_error"));
            e.printStackTrace();
        }
        
        return true;
    }
    
    private boolean handleList(Player player) {
        ConfigManager cm = plugin.getConfigManager();
        List<ShopItem> items = plugin.getShopManager().getShopItems();
        
        if (items.isEmpty()) {
            player.sendMessage(cm.getMessage("shop.empty"));
            return true;
        }
        
        player.sendMessage(cm.getMessageNoPrefix("admin.list_header"));
        
        for (int i = 0; i < items.size(); i++) {
            ShopItem item = items.get(i);
            String itemName = item.getDisplayName() != null ? 
                item.getDisplayName() : 
                item.getMaterial();
                
            player.sendMessage(cm.getMessageNoPrefix("admin.list_item")
                .replace("%index%", String.valueOf(i + 1))
                .replace("%item%", itemName)
                .replace("%price%", String.format("%.2f", item.getPrice())));
        }
        
        player.sendMessage(cm.getMessageNoPrefix("admin.list_footer"));
        
        return true;
    }
    
    private boolean handleClear(Player player) {
        ConfigManager cm = plugin.getConfigManager();
        
        String confirmTitle = cm.getMessageNoPrefix("confirmation.title_clear");
        
        ConfirmationGUI confirmGUI = new ConfirmationGUI(
            plugin,
            confirmTitle,
            () -> {
                int count = plugin.getShopManager().getShopItems().size();
                plugin.getShopManager().clearItems();
                player.sendMessage(cm.getMessage("admin.clear_success")
                    .replace("%count%", String.valueOf(count)));
                playSound(player, "item_removed");
            },
            null
        );
        
        plugin.getGuiManager().openGUI(confirmGUI, player);
        playSound(player, "confirmation_open");
        
        return true;
    }
    
    private boolean handleSetBalance(Player player, String[] args) {
        ConfigManager cm = plugin.getConfigManager();
        
        if (args.length < 3) {
            player.sendMessage(cm.colorize("&cUsage: /shopadmin set <player> <amount>"));
            return true;
        }
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
            if (amount < 0) {
                player.sendMessage(cm.getMessage("economy.invalid_amount"));
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(cm.getMessage("economy.invalid_amount"));
            return true;
        }
        
        plugin.getEconomyManager().setBalance(target, amount);
        
        player.sendMessage(cm.getMessage("economy.balance_set")
            .replace("%player%", target.getName())
            .replace("%amount%", cm.formatPrice(amount)));
        
        playSound(player, "item_added");
        
        return true;
    }
    
    private boolean handleGiveBalance(Player player, String[] args) {
        ConfigManager cm = plugin.getConfigManager();
        
        if (args.length < 3) {
            player.sendMessage(cm.colorize("&cUsage: /shopadmin give <player> <amount>"));
            return true;
        }
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
            if (amount <= 0) {
                player.sendMessage(cm.getMessage("economy.invalid_amount"));
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(cm.getMessage("economy.invalid_amount"));
            return true;
        }
        
        plugin.getEconomyManager().deposit(target, amount);
        
        player.sendMessage(cm.getMessage("economy.balance_given")
            .replace("%player%", target.getName())
            .replace("%amount%", cm.formatPrice(amount)));
        
        if (target.isOnline()) {
            target.getPlayer().sendMessage(cm.getMessage("economy.balance_received")
                .replace("%amount%", cm.formatPrice(amount)));
        }
        
        playSound(player, "item_added");
        
        return true;
    }
    
    private boolean handleTakeBalance(Player player, String[] args) {
        ConfigManager cm = plugin.getConfigManager();
        
        if (args.length < 3) {
            player.sendMessage(cm.colorize("&cUsage: /shopadmin take <player> <amount>"));
            return true;
        }
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
            if (amount <= 0) {
                player.sendMessage(cm.getMessage("economy.invalid_amount"));
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(cm.getMessage("economy.invalid_amount"));
            return true;
        }
        
        if (!plugin.getEconomyManager().has(target, amount)) {
            player.sendMessage(cm.getMessage("economy.player_insufficient_funds")
                .replace("%player%", target.getName()));
            return true;
        }
        
        plugin.getEconomyManager().withdraw(target, amount);
        
        player.sendMessage(cm.getMessage("economy.balance_taken")
            .replace("%player%", target.getName())
            .replace("%amount%", cm.formatPrice(amount)));
        
        if (target.isOnline()) {
            target.getPlayer().sendMessage(cm.getMessage("economy.balance_removed")
                .replace("%amount%", cm.formatPrice(amount)));
        }
        
        playSound(player, "item_removed");
        
        return true;
    }
    
    private void sendAdminHelp(Player player) {
        ConfigManager cm = plugin.getConfigManager();
        player.sendMessage(cm.colorize("&6&m          &r &6ShardShop Admin &6&m          "));
        player.sendMessage(cm.colorize("&e/shopadmin add <price> &7- Add held item"));
        player.sendMessage(cm.colorize("&e/shopadmin addname <material> <price> &7- Add by name"));
        player.sendMessage(cm.colorize("&e/shopadmin remove &7- Remove held item"));
        player.sendMessage(cm.colorize("&e/shopadmin list &7- List all items"));
        player.sendMessage(cm.colorize("&e/shopadmin clear &7- Clear all items"));
        player.sendMessage(cm.colorize("&e/shopadmin reload &7- Reload configs"));
        player.sendMessage(cm.colorize("&e/shopadmin set <player> <amount> &7- Set balance"));
        player.sendMessage(cm.colorize("&e/shopadmin give <player> <amount> &7- Give money"));
        player.sendMessage(cm.colorize("&e/shopadmin take <player> <amount> &7- Take money"));
        player.sendMessage(cm.colorize("&6&m                                  "));
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
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (command.getName().equalsIgnoreCase("shop")) {
            if (args.length == 1) {
                completions.addAll(Arrays.asList("balance", "bal", "pay"));
                return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
            }
            
            if (args.length == 2 && (args[0].equalsIgnoreCase("balance") || args[0].equalsIgnoreCase("bal") || args[0].equalsIgnoreCase("pay"))) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
            
            return completions;
        }
        
        if (!sender.hasPermission("shardshop.admin")) {
            return completions;
        }
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("add", "addname", "remove", "reload", "list", "clear", "set", "give", "take"));
            return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("addname")) {
            return Arrays.stream(XMaterial.values())
                .map(XMaterial::name)
                .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                .limit(50)
                .collect(Collectors.toList());
        }
        
        if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take"))) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("addname"))) {
            completions.add("<price>");
        }
        
        if (args.length == 3 && args[0].equalsIgnoreCase("addname")) {
            completions.add("<price>");
        }
        
        if (args.length == 3 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take"))) {
            completions.add("<amount>");
        }
        
        return completions;
    }
}