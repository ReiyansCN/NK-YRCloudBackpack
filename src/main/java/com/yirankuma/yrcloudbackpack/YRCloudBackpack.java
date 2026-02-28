package com.yirankuma.yrcloudbackpack;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import com.yirankuma.yrcloudbackpack.EventListener.EventListener;
import com.yirankuma.yrcloudbackpack.Manager.InventoryManager;
import com.yirankuma.yrdatabase.YRDatabase;

public class YRCloudBackpack extends PluginBase {
    private static YRCloudBackpack instance;
    private Config config;
    private InventoryManager inventoryManager;

    public int delay = 0;
    
    @Override
    public void onEnable() {
        instance = this;

        // 初始化配置文件
        saveDefaultConfig();
        config = getConfig();

        //初始化加载数据延迟
        delay = config.getInt("load_delay");
        
        // 检测是否启用了 YRDatabase 插件
        if (getServer().getPluginManager().getPlugin("YRDatabase") == null) {
            getLogger().error("YRCloudBackpack 插件需要 YRDatabase 插件支持，请先启用 YRDatabase 插件");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // 初始化背包管理器
        inventoryManager = new InventoryManager(this);
        
        getLogger().info("YRCloudBackpack 插件已启用");
        getLogger().info("使用数据表名称: " + getInventorySchemaName());
        
        getServer().getPluginManager().registerEvents(new EventListener(), this);
    }
    
    public static YRCloudBackpack getInstance() {
        return instance;
    }
    
    public String getInventorySchemaName() {
        return config.getString("inventory_schema_name", "player_inventory");
    }
    
    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }
}
