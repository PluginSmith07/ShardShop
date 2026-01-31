package com.shardshop.shop;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShopItem implements ConfigurationSerializable {
    private String material;
    private double price;
    private String displayName;
    private List<String> lore;
    private Map<String, Integer> enchantments;
    private Set<String> itemFlags;
    
    public ShopItem(ItemStack item, double price) {
        XMaterial xMat = XMaterial.matchXMaterial(item);
        this.material = xMat.name();
        this.price = price;
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            this.displayName = meta.hasDisplayName() ? meta.getDisplayName() : null;
            this.lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            
            this.enchantments = new HashMap<>();
            meta.getEnchants().forEach((ench, level) -> {
                XEnchantment.matchXEnchantment(ench.getKey().getKey()).ifPresent(xe -> 
                    enchantments.put(xe.name(), level)
                );
            });
            
            this.itemFlags = meta.getItemFlags().stream()
                .map(ItemFlag::name)
                .collect(Collectors.toSet());
        } else {
            this.lore = new ArrayList<>();
            this.enchantments = new HashMap<>();
            this.itemFlags = new HashSet<>();
        }
    }
    
    public ItemStack toItemStack() {
        ItemStack item = XMaterial.matchXMaterial(material)
            .map(XMaterial::parseItem)
            .orElse(null);
            
        if (item == null) return null;
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (displayName != null) {
                meta.setDisplayName(displayName);
            }
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            
            if (enchantments != null) {
                enchantments.forEach((enchName, level) -> {
                    XEnchantment.matchXEnchantment(enchName).ifPresent(xe -> {
                        Enchantment ench = xe.getEnchant();
                        if (ench != null) {
                            meta.addEnchant(ench, level, true);
                        }
                    });
                });
            }
            
            if (itemFlags != null) {
                itemFlags.forEach(flagName -> {
                    try {
                        meta.addItemFlags(ItemFlag.valueOf(flagName));
                    } catch (IllegalArgumentException ignored) {
                    }
                });
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    public boolean matches(ItemStack item) {
        if (item == null) return false;
        XMaterial itemMat = XMaterial.matchXMaterial(item);
        if (!itemMat.name().equals(material)) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return displayName == null;
        
        String itemDisplayName = meta.hasDisplayName() ? meta.getDisplayName() : null;
        if (!Objects.equals(displayName, itemDisplayName)) return false;
        
        Map<String, Integer> itemEnchants = new HashMap<>();
        meta.getEnchants().forEach((ench, level) -> {
            XEnchantment.matchXEnchantment(ench.getKey().getKey()).ifPresent(xe -> 
                itemEnchants.put(xe.name(), level)
            );
        });
        
        return Objects.equals(enchantments, itemEnchants);
    }
    
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("material", material);
        map.put("price", price);
        if (displayName != null) map.put("displayName", displayName);
        if (lore != null && !lore.isEmpty()) map.put("lore", lore);
        if (enchantments != null && !enchantments.isEmpty()) map.put("enchantments", enchantments);
        if (itemFlags != null && !itemFlags.isEmpty()) map.put("itemFlags", new ArrayList<>(itemFlags));
        return map;
    }
    
    public static ShopItem deserialize(Map<String, Object> map) {
        ShopItem item = new ShopItem();
        item.material = (String) map.get("material");
        item.price = ((Number) map.get("price")).doubleValue();
        item.displayName = (String) map.get("displayName");
        
        Object loreObj = map.get("lore");
        item.lore = loreObj instanceof List ? (List<String>) loreObj : new ArrayList<>();
        
        Object enchObj = map.get("enchantments");
        if (enchObj instanceof Map) {
            item.enchantments = new HashMap<>();
            ((Map<?, ?>) enchObj).forEach((k, v) -> 
                item.enchantments.put(k.toString(), ((Number) v).intValue())
            );
        } else {
            item.enchantments = new HashMap<>();
        }
        
        Object flagsObj = map.get("itemFlags");
        if (flagsObj instanceof List) {
            item.itemFlags = new HashSet<>((List<String>) flagsObj);
        } else {
            item.itemFlags = new HashSet<>();
        }
        
        return item;
    }
}