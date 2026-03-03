package com.yirankuma.yrcloudbackpack.Manager;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.inventory.PlayerInventory;
import cn.nukkit.inventory.PlayerOffhandInventory;
import cn.nukkit.item.Item;
import com.google.gson.Gson;
import com.yirankuma.yrcloudbackpack.Schemas.Schemas;
import com.yirankuma.yrcloudbackpack.YRCloudBackpack;
import com.yirankuma.yrdatabase.api.CacheStrategy;
import com.yirankuma.yrdatabase.api.DatabaseManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class InventoryManager {
    private final YRCloudBackpack plugin;
    private final String schemaName;
    private final DatabaseManager dbManager;

    public InventoryManager(YRCloudBackpack plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.schemaName = plugin.getInventorySchemaName();
        this.dbManager = dbManager;
    }


public CompletableFuture<Void> initialize() {
    CompletableFuture<Void> result = CompletableFuture.completedFuture(null);
    // Create or check player_inventory table
    result = result.thenCompose(v -> dbManager.ensureTable(this.schemaName, Schemas.inventorySchema())
            .thenAccept(ensured -> {
                if (ensured) {
                    plugin.getLogger().info("玩家背包数据表就绪!");
                } else {
                    plugin.getLogger().warning("玩家背包数据表初始化失败!");
                }
            })
            .exceptionally(ex -> {
                plugin.getLogger().error("玩家背包数据表初始化错误: " + ex.getMessage());
                ex.printStackTrace();
                return null;
            }));

    return result;
}

    public void loadPlayerInventory(Player player,String uid) {
        dbManager.exists(this.schemaName, uid)
                .thenAccept(exists -> {
                    if (!exists) {
                        // 新玩家 没有缓存数据，保存当前背包作为初始数据
                        Server.getInstance().getLogger().info("玩家 "+player.getName()+" 首次来到本服务器!正在保存其背包数据至缓存!");
                        savePlayerInventory(player,uid,CacheStrategy.CACHE_ONLY);
                    }else{
                        dbManager.get(this.schemaName, uid)
                                .thenAccept(optional -> optional.ifPresent(data -> {
                                    // 有缓存数据，加载到玩家背包
                                    String inventoryJson = (String) data.get("inventory_data");
                                    String armorJson = (String) data.get("armor_data");
                                    String offhandJson = (String) data.get("offhand_data");
                                    loadInventoryFromJson(player, inventoryJson, armorJson, offhandJson);
                                    Server.getInstance().getLogger().info("玩家 "+player.getName()+" 的背包数据已同步完毕!");
                                    player.sendMessage("您的背包数据已从云端同步完毕!");
                                }))
                                .exceptionally(throwable -> {
                                    Server.getInstance().getLogger().error("加载玩家 "+player.getName()+" 背包数据失败!", throwable);
                                    return null;
                                });
                    }
                });
    }

    public void
    savePlayerInventory(Player player,String uid,CacheStrategy cacheStrategy) {
        String playerName = player.getName(); // 获取玩家名称
        String inventoryJson = serializeInventoryToJson(player.getInventory());
        String armorJson = serializeArmorToJson(player.getInventory());
        String offhandJson = serializeOffhandToJson(player.getOffhandInventory());
        // 避免地图缓存
        player.getInventory().clearAll();
        player.getOffhandInventory().clearAll();
        // 包含所有需要保存的数据
        Map<String, Object> inventoryData = Map.of(
                "id",uid,
                "player_name", playerName,  // 添加玩家名称
                "inventory_data", inventoryJson,
                "armor_data", armorJson,
                "offhand_data", offhandJson
        );

        dbManager.set(this.schemaName, uid, inventoryData,cacheStrategy)
                .thenAccept(success -> {
                    if (success) {
                        if(cacheStrategy.equals(CacheStrategy.CACHE_ONLY)){
                            Server.getInstance().getLogger().info("玩家仅转服，保存玩家 " + player.getName() +" 背包数据到缓存!");
                        }else if(cacheStrategy.equals(CacheStrategy.CACHE_FIRST)){
                            Server.getInstance().getLogger().info("玩家已退服，保存玩家 " + player.getName() +" 背包数据到缓存和数据库!");
                        }
                    }else{
                        Server.getInstance().getLogger().warning("保存玩家 " + player.getName() +" 背包数据失败! 策略: " + cacheStrategy);
                    }
                })
                .exceptionally(throwable -> {
                    Server.getInstance().getLogger().error("保存玩家 " + player.getName()+" 背包数据失败!", throwable);
                    return null;
                });
    }

    private String serializeInventoryToJson(PlayerInventory inventory) {
        Map<String, Object> inventoryData = new HashMap<>();

        // 遍历所有背包槽位
        for (Map.Entry<Integer, Item> entry : inventory.slots.entrySet()) {
            int slot = entry.getKey();
            Item item = entry.getValue();

            // 跳过空物品
            if (item == null || item.getId() == 0) {
                continue;
            }

            Map<String, Object> itemData = new HashMap<>();
            itemData.put("identifier", item.getNamespaceId());
            itemData.put("damage", item.getDamage());
            itemData.put("count", item.getCount());
            itemData.put("name", item.getName());

            // 处理NBT数据
            if (item.hasCompoundTag()) {
                byte[] compoundTag = item.getCompoundTag();
                if (compoundTag != null && compoundTag.length > 0) {
                    // 将字节数组转换为Base64字符串存储
                    itemData.put("nbt", java.util.Base64.getEncoder().encodeToString(compoundTag));
                } else {
                    itemData.put("nbt", null);
                }
            } else {
                itemData.put("nbt", null);
            }

//            // 添加其他可能需要的属性
//            itemData.put("customName", item.getCustomName());
//            itemData.put("lore", item.getLore());

            // 使用槽位作为key
            inventoryData.put(String.valueOf(slot), itemData);
        }

//        // 添加背包元数据
//        Map<String, Object> metadata = new HashMap<>();
//        metadata.put("size", inventory.getSize());
//        metadata.put("serializedAt", System.currentTimeMillis());
//        inventoryData.put("_metadata", metadata);

        // 转换为JSON字符串
        try {
            return new Gson().toJson(inventoryData);
        } catch (Exception e) {
            Server.getInstance().getLogger().error("序列化背包数据失败", e);
            return "";
        }
    }

    private String serializeArmorToJson(PlayerInventory inventory) {
        Map<String, Object> armorData = new HashMap<>();

        // 获取装备槽位的物品
        Item[] armorContents = inventory.getArmorContents();
        String[] armorSlots = {"helmet", "chestplate", "leggings", "boots"};

        for (int i = 0; i < armorContents.length && i < armorSlots.length; i++) {
            Item item = armorContents[i];

            if (item == null || item.getId() == 0) {
                armorData.put(armorSlots[i], null);
                continue;
            }

            Map<String, Object> itemData = new HashMap<>();
            itemData.put("identifier", item.getNamespaceId());
            itemData.put("damage", item.getDamage());
            itemData.put("count", item.getCount());
            itemData.put("name", item.getName());

            // 处理NBT数据
            if (item.hasCompoundTag()) {
                byte[] compoundTag = item.getCompoundTag();
                if (compoundTag != null && compoundTag.length > 0) {
                    itemData.put("nbt", java.util.Base64.getEncoder().encodeToString(compoundTag));
                } else {
                    itemData.put("nbt", null);
                }
            } else {
                itemData.put("nbt", null);
            }

            itemData.put("customName", item.getCustomName());
            itemData.put("lore", item.getLore());

            armorData.put(armorSlots[i], itemData);
        }


//        // 添加装备元数据
//        Map<String, Object> metadata = new HashMap<>();
//        metadata.put("serializedAt", System.currentTimeMillis());
//        armorData.put("_metadata", metadata);

        try {
            return new Gson().toJson(armorData);
        } catch (Exception e) {
            Server.getInstance().getLogger().error("序列化装备数据失败", e);
            return "";
        }
    }


    private String serializeOffhandToJson(PlayerOffhandInventory offhandInventory) {
        Map<String, Object> armorData = new HashMap<>();

        // 获取装备槽位的物品
        Item offHandItem = offhandInventory.getItem(0);
        String slotKey = "offhand";
        if (offHandItem == null || offHandItem.getId() == 0) {
            armorData.put(slotKey, null);
        } else {
            Map<String, Object> itemData = new HashMap<>();
            itemData.put("identifier", offHandItem.getNamespaceId());
            itemData.put("damage", offHandItem.getDamage());
            itemData.put("count", offHandItem.getCount());
            itemData.put("name", offHandItem.getName());

            // 处理NBT数据
            if (offHandItem.hasCompoundTag()) {
                byte[] compoundTag = offHandItem.getCompoundTag();
                if (compoundTag != null && compoundTag.length > 0) {
                    itemData.put("nbt", java.util.Base64.getEncoder().encodeToString(compoundTag));
                } else {
                    itemData.put("nbt", null);
                }
            } else {
                itemData.put("nbt", null);
            }

            itemData.put("customName", offHandItem.getCustomName());
            itemData.put("lore", offHandItem.getLore());

            armorData.put(slotKey, itemData);
        }
        try {
            return new Gson().toJson(armorData);
        } catch (Exception e) {
            Server.getInstance().getLogger().error("序列化副手数据失败", e);
            return "";
        }
    }

    private void loadInventoryFromJson(Player player, String inventoryJson, String armorJson, String offhandJson) {
        try {
            // 加载背包数据
            PlayerInventory inventory = player.getInventory();
            PlayerOffhandInventory offhandInventory = player.getOffhandInventory();
            if (inventoryJson != null && !inventoryJson.isEmpty()) {
                Gson gson = new Gson();
                Map<String, Object> inventoryData = gson.fromJson(inventoryJson, Map.class);
                inventory.clearAll(); // 清空当前背包

                for (Map.Entry<String, Object> entry : inventoryData.entrySet()) {
                    String key = entry.getKey();

                    // 跳过元数据
                    if (key.equals("_metadata")) {
                        continue;
                    }

                    try {
                        int slot = Integer.parseInt(key);
                        Map<String, Object> itemData = (Map<String, Object>) entry.getValue();

                        Item item = createItemFromData(itemData);
                        if (item != null) {
                            inventory.setItem(slot, item);
                        }
                    } catch (NumberFormatException e) {
                        // 忽略非数字槽位
                    }
                }
            }

            // 加载装备数据
            if (armorJson != null && !armorJson.isEmpty()) {
                Gson gson = new Gson();
                Map<String, Object> armorData = gson.fromJson(armorJson, Map.class);

                String[] armorSlots = {"helmet", "chestplate", "leggings", "boots"};
                Item[] armorItems = new Item[4];

                for (int i = 0; i < armorSlots.length; i++) {
                    Object itemDataObj = armorData.get(armorSlots[i]);
                    if (itemDataObj != null) {
                        Map<String, Object> itemData = (Map<String, Object>) itemDataObj;
                        armorItems[i] = createItemFromData(itemData);
                    }
                }

                inventory.setArmorContents(armorItems);
            }
            // 加载副手
            if (offhandJson != null && !offhandJson.isEmpty()) {
                Gson gson = new Gson();
                Map<String, Object> offhandData = gson.fromJson(offhandJson, Map.class);

                String slotName = "offhand";
                Item offhandItem = null;

                Object itemDataObj = offhandData.get(slotName);
                if (itemDataObj != null) {
                    Map<String, Object> itemData = (Map<String, Object>) itemDataObj;
                    offhandItem = createItemFromData(itemData);
                }
                if (offhandItem != null) {
                    offhandInventory.setItem(0, offhandItem);
                }
            }

        } catch (Exception e) {
            Server.getInstance().getLogger().error("反序列化背包数据失败", e);
        }
    }

    private Item createItemFromData(Map<String, Object> itemData) {
        try {
            String identifier = itemData.get("identifier").toString();
            int damage = ((Double) itemData.get("damage")).intValue();
            int count = ((Double) itemData.get("count")).intValue();

            Item item = Item.fromString(identifier);
            item.setDamage(damage);
            item.setCount(count);

//            // 设置自定义名称
//            String customName = (String) itemData.get("customName");
//            if (customName != null && !customName.isEmpty()) {
//                item.setCustomName(customName);
//            }
//
//            // 设置描述
//            Object loreObj = itemData.get("lore");
//            if (loreObj instanceof List) {
//                List<String> lore = (List<String>) loreObj;
//                item.setLore(lore.toArray(new String[0]));
//            }

            // 恢复NBT数据
            String nbtBase64 = (String) itemData.get("nbt");
            if (nbtBase64 != null && !nbtBase64.isEmpty()) {
                try {
                    byte[] nbtData = java.util.Base64.getDecoder().decode(nbtBase64);
                    item.setCompoundTag(nbtData);
                } catch (Exception e) {
                    Server.getInstance().getLogger().warning("恢复物品NBT数据失败: " + e.getMessage());
                }
            }

            return item;
        } catch (Exception e) {
            Server.getInstance().getLogger().error("创建物品失败", e);
            return null;
        }
    }
}