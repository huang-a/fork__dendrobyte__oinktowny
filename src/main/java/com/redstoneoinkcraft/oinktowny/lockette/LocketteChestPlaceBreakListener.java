package com.redstoneoinkcraft.oinktowny.lockette;

import com.redstoneoinkcraft.oinktowny.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * OinkTowny created/started by Mark Bacon (Mobkinz78/Dendrobyte)
 * Please do not use or edit without permission!
 * If you have any questions, reach out to me on Twitter: @Mobkinz78
 * §
 */
public class LocketteChestPlaceBreakListener implements Listener {

    String prefix = Main.getInstance().getPrefix();
    LocketteManager lm = LocketteManager.getInstance();

    @EventHandler
    public void onChestPlace(BlockPlaceEvent event){
        if(event.getBlockPlaced().getType() != Material.CHEST) return;
        Player player = event.getPlayer();
        Chest chest = (Chest)event.getBlock().getState();
        if(!Main.getInstance().isTownyWorld(player.getWorld().getName())) return;
        player.sendMessage(prefix + ChatColor.AQUA + "If you'd like to private this chest, " + ChatColor.YELLOW + ChatColor.BOLD + "SHIFT + RIGHT CLICK");
        lm.addActiveChest(chest, player);
    }

    @EventHandler
    public void onChestBreak(BlockBreakEvent event){
        if(event.getBlock().getType() != Material.CHEST) return;
        Player player = event.getPlayer();
        if(player.isOp()){ // Allow ops to break chests for extenuating circumstances. Permission later?
            return;
        }
        if(!lm.playerOwnsChest(player, (Chest)event.getBlock().getState())) {
            event.setCancelled(true); // Don't need to worry about world, since technically no private chests can exist in other worlds
            player.sendMessage(prefix + ChatColor.RED + "This chest is privately owned. Only the owner can break it.");
            return;
        } else {
            if(player.isSneaking()){
                player.sendMessage(prefix + "Privately owned chest broken.");
                lm.removeChest((Chest)event.getBlock().getState());
                return;
            }
            player.sendMessage(prefix + "This chest is privated. To break it, " + ChatColor.YELLOW + ChatColor.BOLD + "SHIFT + LEFT CLICK");
            event.setCancelled(true);
        }

    }

}