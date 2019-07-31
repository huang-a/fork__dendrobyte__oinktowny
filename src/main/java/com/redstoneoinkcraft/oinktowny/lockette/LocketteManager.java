package com.redstoneoinkcraft.oinktowny.lockette;

import com.redstoneoinkcraft.oinktowny.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Level;

/**
 * OinkTowny created/started by Mark Bacon (Mobkinz78/Dendrobyte)
 * Please do not use or edit without permission!
 * If you have any questions, reach out to me on Twitter: @Mobkinz78
 * §
 */
public class LocketteManager {

    // TODO (optimization): For editing a double chest, instead of searching for 'otherHalf' of chest, just get the double-chest-id and skip to it.

    private static LocketteManager instance = new LocketteManager();
    String prefix = Main.getInstance().getPrefix();
    private String editorPrefix = ChatColor.DARK_GRAY + "[" + ChatColor.YELLOW + "LocketteEditor" + ChatColor.DARK_GRAY + "]" + ChatColor.YELLOW + " ";

    private LocketteManager(){}

    public static LocketteManager getInstance(){
        return instance;
    }

    private HashMap<Chest, List<String>> privatedChests = new HashMap<>();
    public void loadChests(){
        try {
            ConfigurationSection chests = Main.getInstance().getConfig().getConfigurationSection("lockette-data.chests");
            for (String chestNum : chests.getKeys(false)) {
                // Get the chest
                Location storedLoc = new Location(Bukkit.getWorld(chests.getString(chestNum + ".location.world")), chests.getInt(chestNum + ".location.x"), chests.getInt(chestNum + ".location.y"), chests.getInt(chestNum + ".location.z"));
                Block block = storedLoc.getBlock();
                if (block.getType() != Material.CHEST) {
                    System.out.println("ERROR: Lockette chest location in config not a chest.");
                }

                // Get the added player names
                List<String> playerNames = chests.getStringList(chestNum + ".added-players");
                playerNames.add(chests.getString(chestNum + ".owner"));

                // Add them to the map
                privatedChests.put((Chest) block.getState(), playerNames);
            }
            System.out.println(prefix + "All chests successfully loaded!");
            System.out.println("Privated chests hashmap: " + privatedChests.toString());
            return;
        } catch (NullPointerException e){
            Bukkit.getLogger().log(Level.WARNING, prefix + "No Lockette chests loaded!");
        }
    }

    // For after editing the file
    public void reloadChests(){
        System.out.println(prefix + "Initiating reload of Lockette chests...");
        privatedChests.clear();
        loadChests();
    }

    // When players place a chest, they must activate it as private within 30 seconds
    public HashMap<Chest, Player> activeChests = new HashMap<>(2);
    public void addActiveChest(Chest chest, Player player){
        activeChests.put(chest, player);
        LocketteActiveChestTimer lact = new LocketteActiveChestTimer(chest, player);
        lact.runTaskTimer(Main.getInstance(), 0L, 20L);
        activeTimers.put(player, lact);
    }
    public void removeActiveChest(Chest chest){
        activeChests.remove(chest);
    }
    public boolean chestIsActive(Chest chest){
        return activeChests.containsKey(chest);
    }
    public Player activeChestPlayer(Chest chest){
        return activeChests.get(chest);
    }

    public HashMap<Player, LocketteActiveChestTimer> activeTimers = new HashMap<>(2);
    public HashMap<Player, LocketteActiveChestTimer> getActiveTimers(){
        return activeTimers;
    }

    public void makeNewPrivateChest(Chest chest, Player player, boolean isDouble, Chest otherHalf){
        Location loc = chest.getLocation();
        int chestNum = Main.getInstance().getConfig().getInt("lockette-data.chest-counter");
        Main.getInstance().getConfig().set("lockette-data.chests." + chestNum + ".location.world", loc.getWorld().getName());
        Main.getInstance().getConfig().set("lockette-data.chests." + chestNum + ".location.x", loc.getBlockX());
        Main.getInstance().getConfig().set("lockette-data.chests." + chestNum + ".location.y", loc.getBlockY());
        Main.getInstance().getConfig().set("lockette-data.chests." + chestNum + ".location.z", loc.getBlockZ());
        Main.getInstance().getConfig().set("lockette-data.chests." + chestNum + ".owner", player.getUniqueId() + ":" + player.getName());
        Main.getInstance().getConfig().set("lockette-data.chests." + chestNum + ".added-players", player.getUniqueId() + ":" + player.getName());
        if(!isDouble){
            Main.getInstance().getConfig().set("lockette-data.chests." + chestNum + ".doublechest", false);
        } else { // isDouble
            Main.getInstance().getConfig().set("lockette-data.chests." + chestNum + ".doublechest", true);
            /* This seriously needs to be turned into a method... */
            ConfigurationSection chests = Main.getInstance().getConfig().getConfigurationSection("chests");
            for(String existingChestNums : chests.getKeys(false)) {
                Location storedLoc = new Location(Bukkit.getWorld(chests.getString(chestNum + ".location.world")), chests.getInt(chestNum + ".location.x"), chests.getInt(chestNum + ".location.y"), chests.getInt(chestNum + ".location.z"));
                if(storedLoc.equals(makeFakeLocationForRealChest(otherHalf))){
                    chests.set(chestNum + ".other-half-id", Integer.parseInt(existingChestNums));
                    chests.set(existingChestNums + ".doublechest", true);
                    chests.set(existingChestNums + ".other-half-id", chestNum);
                    break;
                }
            }
            // Set the other-half-id to that chest
            // Set the other ches'ts other-half-id to this chest
        }

        Main.getInstance().getConfig().set("lockette-data.chest-counter", chestNum+1);
        Main.getInstance().saveConfig();
        reloadChests();
    }

    public boolean isLocketteChest(Chest chest){
        return privatedChests.containsKey(chest);
    }

    public boolean playerOwnsChest(Player player, Chest chest){
        // Owner should be last on the list. No need to check actual name.
        System.out.println("privatedChests: " + privatedChests.toString());
        System.out.println("index 0: " + privatedChests.get(chest).get(0));
        System.out.println("size: " + privatedChests.get(chest).size());
        String lastPlayer = privatedChests.get(chest).get(privatedChests.get(chest).size()-1);
        return lastPlayer.substring(0, lastPlayer.indexOf(":")).equalsIgnoreCase(player.getUniqueId().toString());
    }

    public boolean playerCanAccessChest(Player player, Chest chest){
        boolean potentialNameChange = false;
        for(String string : privatedChests.get(chest)){
            if(string.substring(string.indexOf(":")+1).equalsIgnoreCase(player.getName())) return true;
            // If their name isn't there, check to see if it changed and their UUID is on file
            if(player.getUniqueId().toString().equalsIgnoreCase(string.substring(0, string.indexOf(":")))) potentialNameChange = true;

        }
        if(potentialNameChange){
            refreshChest(chest, player.getUniqueId(), player.getName());
            return playerCanAccessChest(player, chest);
        }
        return false;
    }

    // I don't trust the location check
    public Location makeFakeLocationForRealChest(Chest currentChest){
        return new Location(Bukkit.getWorld(currentChest.getWorld().getName()), currentChest.getLocation().getBlockX(), currentChest.getLocation().getBlockY(), currentChest.getLocation().getBlockZ());
    }

    public boolean removeChest(Chest chest){
        boolean passed = false;
        ConfigurationSection chests = Main.getInstance().getConfig().getConfigurationSection("chests");
        for(String chestNum : chests.getKeys(false)) {
            Location storedLoc = new Location(Bukkit.getWorld(chests.getString(chestNum + ".location.world")), chests.getInt(chestNum + ".location.x"), chests.getInt(chestNum + ".location.y"), chests.getInt(chestNum + ".location.z"));
            if(storedLoc.equals(makeFakeLocationForRealChest(chest))){
                chests.set(chestNum, null);
                Main.getInstance().saveConfig();
                passed = true;
            }
        }
        reloadChests();
        return passed;
    }

    /* Edit menu for players. If they're in the list, they edit by chatting */
    HashMap<Player, Chest> playersEditing = new HashMap<>();
    public HashMap<Player, Chest> getPlayersEditing(){
        return playersEditing;
    }
    public boolean isPlayerEditingChest(Player player){
        return playersEditing.containsKey(player);
    }

    public void initiatePlayerEditing(Player player, Chest chest){
        // No check for world or anything because there should be no reason this method is called should those checks not pass
        if(isPlayerEditingChest(player)){
            player.sendMessage(editorPrefix + "You are already editing a chest!");
            return;
        }
        playersEditing.put(player, chest);
        player.sendMessage(editorPrefix + "ADD <name> - " + ChatColor.GRAY + "Add a player to the chest you just clicked");
        player.sendMessage(editorPrefix + "REMOVE <name> - " + ChatColor.GRAY + "Remove a player from the chest you just clicked");
        player.sendMessage(editorPrefix + "DELETE - " + ChatColor.GRAY + "Remove the chest you just clicked");
        player.sendMessage(editorPrefix + "DONE - " + ChatColor.GRAY + "Leave this edit wizard");
    }

    public boolean addPlayerToChest(Chest chest, UUID playerId, String playerName){
        boolean passed = false;
        ConfigurationSection chests = Main.getInstance().getConfig().getConfigurationSection("chests");
        for(String chestNum : chests.getKeys(false)) {
            Location storedLoc = new Location(Bukkit.getWorld(chests.getString(chestNum + ".location.world")), chests.getInt(chestNum + ".location.x"), chests.getInt(chestNum + ".location.y"), chests.getInt(chestNum + ".location.z"));
            if(storedLoc.equals(makeFakeLocationForRealChest(chest))){
                List<String> playerNames = chests.getStringList(chestNum + ".added-players");
                playerNames.add(playerId + ":" + playerName);
                Main.getInstance().saveConfig();
                passed = true;
            }
        }
        reloadChests();
        return passed;
    }

    // If someone's name changed, check if their UUID is still on the chest
    // TODO: This may cause an issue with the owner's name... Then again, we may be fine since that deals with UUID
    public void refreshChest(Chest chest, UUID playerId, String playerName){
        ConfigurationSection chests = Main.getInstance().getConfig().getConfigurationSection("chests");
        for(String chestNum : chests.getKeys(false)) {
            // Get the chest
            Location storedLoc = new Location(Bukkit.getWorld(chests.getString(chestNum + ".location.world")), chests.getInt(chestNum + ".location.x"), chests.getInt(chestNum + ".location.y"), chests.getInt(chestNum + ".location.z"));
            if(storedLoc.equals(makeFakeLocationForRealChest(chest))){
                List<String> playerNames = chests.getStringList(chestNum + ".added-players");
                for(String name : playerNames){
                    String id = name.substring(0, name.indexOf(":"));
                    if(id.equalsIgnoreCase(playerId.toString())){
                        playerNames.remove(name);
                        playerNames.add(playerId.toString() + ":" + playerName);
                        Main.getInstance().saveConfig();
                        break;
                    }
                }
                break;
            }
        }
        reloadChests();
    }

    public boolean removePlayerFromChest(Chest chest, String playerName){
        boolean passed = false;
        ConfigurationSection chests = Main.getInstance().getConfig().getConfigurationSection("chests");
        for(String chestNum : chests.getKeys(false)) {
            Location storedLoc = new Location(Bukkit.getWorld(chests.getString(chestNum + ".location.world")), chests.getInt(chestNum + ".location.x"), chests.getInt(chestNum + ".location.y"), chests.getInt(chestNum + ".location.z"));
            if(storedLoc.equals(makeFakeLocationForRealChest(chest))){
                List<String> playerNames = chests.getStringList(chestNum + ".added-players");
                for(String name : playerNames){
                    String storedName = name.substring(name.indexOf(":")+1);
                    if(storedName.equalsIgnoreCase(playerName)){
                        playerNames.remove(name);
                        Main.getInstance().saveConfig();
                        passed = true;
                        break;
                    }
                }
                break;
            }
        }
        reloadChests();
        return passed;
    }

    /* Utils */
    public boolean isDoubleChest(Chest chest){
        return chest.getInventory().getHolder() instanceof DoubleChest; // May not work, but want to give it a shot
        // return chest.getInventory().getSize() > 27;
    }

    public Chest getOtherHalfOfDouble(DoubleChest doubleChest, Chest chest){
        Chest leftHalf = (Chest)doubleChest.getLeftSide();
        if (leftHalf.equals(chest)) {
            return (Chest)doubleChest.getRightSide();
        } else {
            return (Chest)doubleChest.getLeftSide();
        }
    }

    public boolean isDoubleAlreadyPrivated(DoubleChest chest){
        Chest leftHalf = (Chest)chest.getLeftSide();
        Chest rightHalf = (Chest)chest.getRightSide();
        return isLocketteChest(leftHalf) || isLocketteChest(rightHalf);
    }

}
