package com.yirankuma.yrcloudbackpack.EventListener;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.item.Item;
import com.yirankuma.yrcloudbackpack.Manager.InventoryManager;
import com.yirankuma.yrcloudbackpack.YRCloudBackpack;
import com.yirankuma.yrdatabase.event.PlayerDataInitializeEvent;
import com.yirankuma.yrdatabase.event.PlayerDataPersistEvent;

public class EventListener implements Listener {
    private final InventoryManager inventoryManager;

    public EventListener() {
        inventoryManager = YRCloudBackpack.getInstance().getInventoryManager();
    }

    // 初始化玩家数据（从数据库加载到缓存）
    @EventHandler
    public void onPlayerDataInitialize(PlayerDataInitializeEvent event) {
        String uid = event.getUid();
        Player player = event.getPlayer();

        if (player != null) {
            // YRDatabase已经帮你判断好了，直接加载数据
            if(YRCloudBackpack.getInstance().delay > 0){
                YRCloudBackpack.getInstance().getServer().getScheduler().scheduleDelayedTask(YRCloudBackpack.getInstance(), () -> {
                    inventoryManager.loadPlayerInventory(player);
                }, YRCloudBackpack.getInstance().delay * 20);
            }else{
                inventoryManager.loadPlayerInventory(player);
            }
        }
    }

    // 持久化玩家数据（从缓存保存到数据库）
    @EventHandler
    public void onPlayerDataPersist(PlayerDataPersistEvent event) {
        String uid = event.getUid();
        Player player = event.getPlayer();

        if (player != null) {
            // 先保存到缓存
            inventoryManager.savePlayerInventory(player);

            // 只在需要持久化时保存到数据库（自动排除转服情况）
            if (event.shouldPersist()) {
                inventoryManager.persistPlayerInventory(player);
            }
        }
    }

//    @EventHandler
//    public void onDataPacketReceive(DataPacketReceiveEvent event) {
//        DataPacket dp = event.getPacket();
//        if (dp instanceof NeteaseLoginPacket){
//            System.out.println(((NeteaseLoginPacket) dp).proxyUid);
//        }
//    }
}
