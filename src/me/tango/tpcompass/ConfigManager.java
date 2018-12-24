package me.tango.tpcompass;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class ConfigManager {

    Main plugin;

    private FileConfiguration customConfig = null;
    private File customConfigFile = null;
   
    public ConfigManager(Main instance) {
        plugin = instance;
    }
   
    public void initCustomConfig() {
        this.getCustomConfig().options().copyDefaults(true);
        this.saveDefaultCustomConfig();
        this.saveCustomConfig();
    }
   
    public void reloadCustomConfig() {
        if (customConfigFile == null) {
        customConfigFile = new File(plugin.getDataFolder(), "claims.yml");
        }
        customConfig = YamlConfiguration.loadConfiguration(customConfigFile);
    
        Reader defConfigStream = null;
        try {
            defConfigStream = new InputStreamReader(plugin.getResource("claims.yml"), "UTF8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(customConfigFile);
            customConfig.setDefaults(defConfig);
        }
    }
   
    public FileConfiguration getCustomConfig() {
        if (customConfig == null) {
            reloadCustomConfig();
        }
        return customConfig;
    }
   
    public void saveCustomConfig() {
        if (customConfig == null || customConfigFile == null) {
            return;
        }
        try {
            getCustomConfig().save(customConfigFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config to " + customConfigFile, ex);
        }
    }
   
    public void saveDefaultCustomConfig() {
        if (customConfigFile == null) {
            customConfigFile = new File(plugin.getDataFolder(), "claims.yml");
        }
        if (!customConfigFile.exists()) {           
             this.plugin.saveResource("claims.yml", false);
         }
    }
   
}
 