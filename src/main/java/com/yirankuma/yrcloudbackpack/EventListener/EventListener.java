package com.yirankuma.yrcloudbackpack.EventListener;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import com.yirankuma.yrcloudbackpack.Manager.InventoryManager;
import com.yirankuma.yrcloudbackpack.YRCloudBackpack;
import com.yirankuma.yrdatabase.api.CacheStrategy;
import com.yirankuma.yrdatabase.nukkit.event.NukkitPlayerDataInitEvent;
import com.yirankuma.yrdatabase.nukkit.event.NukkitPlayerDataSaveEvent;

public class EventListener implements Listener {
    private final InventoryManager inventoryManager;

    public EventListener() {
        inventoryManager = YRCloudBackpack.getInstance().getInventoryManager();
    }

    // 玩家进入初始化玩家数据（从数据库加载到缓存）
    @EventHandler
    public void onPlayerRealJoin(NukkitPlayerDataInitEvent event) {
        String uid = event.getPlayerId();
        Player player = Server.getInstance().getPlayer(event.getPlayerName());
        if (player != null) {
            // YRDatabase已经帮你判断好了，直接加载数据
            if (YRCloudBackpack.getInstance().delay > 0) {
                YRCloudBackpack.getInstance().getServer().getScheduler().scheduleDelayedTask(YRCloudBackpack.getInstance(), () -> inventoryManager.loadPlayerInventory(player, uid), YRCloudBackpack.getInstance().delay * 20);
            } else {
                inventoryManager.loadPlayerInventory(player, uid);
            }
        }
    }

    //玩家退出时 仅保存到缓存
    @EventHandler
    public void onPlayerRealQuit(NukkitPlayerDataSaveEvent event) {
        String uid = event.getPlayerId();
        Player player = Server.getInstance().getPlayer(event.getPlayerName());

        if (player != null) {
            // 玩家转服 仅保存到缓存 玩家退出则持久
            if (event.shouldPersist()) {
                inventoryManager.savePlayerInventory(player, uid, CacheStrategy.CACHE_FIRST);
            } else {
                inventoryManager.savePlayerInventory(player, uid, CacheStrategy.CACHE_ONLY);
            }
        }
    }
}