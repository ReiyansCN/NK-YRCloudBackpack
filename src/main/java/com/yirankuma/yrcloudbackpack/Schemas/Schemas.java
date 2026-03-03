package com.yirankuma.yrcloudbackpack.Schemas;

import java.util.LinkedHashMap;
import java.util.Map;

public class Schemas {
    // 定义背包表结构
    public static Map<String, String> inventorySchema(){
        Map<String, String> schema = new LinkedHashMap<>();
        schema.put("id", "VARCHAR(36) PRIMARY KEY");            //主键ID 自增
        schema.put("player_name", "VARCHAR(16)");               // 玩家名称
        schema.put("inventory_data", "LONGTEXT");               // 背包物品数据
        schema.put("armor_data", "TEXT");                       // 装备数据
        schema.put("offhand_data", "TEXT");                       // 装备数据
        return schema;
    };
}
