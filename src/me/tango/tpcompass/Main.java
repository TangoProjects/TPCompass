package me.tango.tpcompass;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;


public class Main extends JavaPlugin implements Listener {

	ConfigManager conf;

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		loadConfig();
		conf = new ConfigManager(this);
		conf.initCustomConfig();
		conf.getCustomConfig();
	}

	@Override
	public void onDisable() {
		conf.saveCustomConfig();
	}

	boolean namechange = this.getConfig().getBoolean("rename-overwrite");
	int maxblocks = this.getConfig().getInt("max-distance");
	int minblocks = this.getConfig().getInt("min-distance");

	@EventHandler
	public void clockClick(PlayerInteractEvent e) {
		Player p = e.getPlayer();
		World w = p.getWorld();
		if(e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if(p.getInventory().getItemInMainHand().getType().equals(Material.COMPASS)) {
				if(!p.getInventory().getItemInMainHand().containsEnchantment(Enchantment.DURABILITY)) {
					if(isOnCooldown(p)) return;
					Random random = new Random();
					int min = minblocks;
					int max = maxblocks;
					int r = random.nextInt((max-min) + 1) + min;
					Vector d = p.getLocation().getDirection();
					Vector v = d.multiply(r / d.length());
					Location nextLocation = p.getLocation().add(v);
					long blocksmoved = Math.round(nextLocation.distance(p.getLocation()));
					setCompassLoc(p, blocksmoved);
					double x = nextLocation.getX();
					double y = (w.getHighestBlockAt(nextLocation).getY() + 1);
					double z = nextLocation.getZ();
					Location loc = new Location(p.getWorld(), x, y, z, nextLocation.getYaw(), nextLocation.getPitch());
					p.teleport(loc, TeleportCause.PLUGIN);
					p.playSound(p.getEyeLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, Integer.MAX_VALUE, 1);
					putInCooldown(p);
				} else {
					int data = p.getInventory().getItemInMainHand().getDurability();
					if(isRenamed(p) && namechange) {
						if(isOnCooldown(p)) return;

						double x = conf.getCustomConfig().getInt("SavedLocations." + p.getUniqueId() + ".Loc" + data + ".x");
						double y = conf.getCustomConfig().getInt("SavedLocations." + p.getUniqueId() + ".Loc" + data + ".y");
						double z = conf.getCustomConfig().getInt("SavedLocations." + p.getUniqueId() + ".Loc" + data + ".z");
						int pitch = conf.getCustomConfig().getInt("SavedLocations." + p.getUniqueId() + ".Loc" + data + ".pitch");
						int yaw = conf.getCustomConfig().getInt("SavedLocations." + p.getUniqueId() + ".Loc" + data + ".yaw");
						String world = conf.getCustomConfig().getString("SavedLocations." + p.getUniqueId() + ".Loc" + data + ".world");

						p.teleport(new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch));
						p.playSound(p.getEyeLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, Integer.MAX_VALUE, 1);
						putInCooldown(p);
						return;
					}

					if(conf.getCustomConfig().getConfigurationSection("SavedLocations." + p.getUniqueId() + ".Loc" + data) == null) {
						p.sendMessage(ChatColor.RED + "You do not own this compass!");
						p.playSound(p.getEyeLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, Integer.MAX_VALUE, 1);
						return;
					}

					if(isOnCooldown(p)) return;

					double x = conf.getCustomConfig().getInt("SavedLocations." + p.getUniqueId() + ".Loc" + data + ".x");
					double y = conf.getCustomConfig().getInt("SavedLocations." + p.getUniqueId() + ".Loc" + data + ".y");
					double z = conf.getCustomConfig().getInt("SavedLocations." + p.getUniqueId() + ".Loc" + data + ".z");
					int pitch = conf.getCustomConfig().getInt("SavedLocations." + p.getUniqueId() + ".Loc" + data + ".pitch");
					int yaw = conf.getCustomConfig().getInt("SavedLocations." + p.getUniqueId() + ".Loc" + data + ".yaw");
					String world = conf.getCustomConfig().getString("SavedLocations." + p.getUniqueId() + ".Loc" + data + ".world");

					p.teleport(new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch));
					p.playSound(p.getEyeLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, Integer.MAX_VALUE, 1);

					conf.getCustomConfig().set("SavedLocations." + p.getUniqueId() + ".Loc" + data, null);
					conf.saveCustomConfig();

					int slot = p.getInventory().getHeldItemSlot();
					p.getInventory().setItem(slot, new ItemStack(Material.COMPASS, 1));
					p.updateInventory();
					putInCooldown(p);
				}
			}
		}
	}

	public boolean isRenamed(Player p) {
		if(!p.getInventory().getItemInMainHand().getItemMeta().getDisplayName().toString().contains("Compass Activated")) {
			return true;
		}
		return false;
	}

	@EventHandler
	public void heldItemChange(PlayerItemHeldEvent e) {
		Player p = e.getPlayer();
		if(e.getPlayer().getInventory().getItem(e.getNewSlot()) != null) {
			ItemStack i = e.getPlayer().getInventory().getItem(e.getNewSlot());
			if(i.getType() != null && i.getType() == Material.COMPASS) {
				int data = i.getDurability();
				if(conf.getCustomConfig().getConfigurationSection("SavedLocations." + p.getUniqueId() + ".Loc" + data) != null) {
					double x = conf.getCustomConfig().getInt("SavedLocations." + p.getUniqueId() + ".Loc" + data + ".x");
					double y = conf.getCustomConfig().getInt("SavedLocations." + p.getUniqueId() + ".Loc" + data + ".y");
					double z = conf.getCustomConfig().getInt("SavedLocations." + p.getUniqueId() + ".Loc" + data + ".z");
					p.setCompassTarget(new Location(p.getWorld(), x,y,z));
					p.updateInventory();
				}
			}
		}
	}

	public void setCompassLoc(Player p, long travelled) {

		ItemStack compass = p.getInventory().getItemInMainHand();
		if(compass.containsEnchantment(Enchantment.DURABILITY)) return;
		compass.addUnsafeEnchantment(Enchantment.DURABILITY, 1);		

		if(compass.getAmount() > 1) {
			p.getInventory().addItem(new ItemStack(Material.COMPASS, (p.getInventory().getItemInMainHand().getAmount() - 1)));
			compass.setAmount(1);
			p.updateInventory();
		}
		
		ItemMeta meta = compass.getItemMeta();
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		int count = 0;
		int x = (int) p.getLocation().getX();
		int y = (int) p.getLocation().getY();
		int z = (int) p.getLocation().getZ();

		if(conf.getCustomConfig().getConfigurationSection("SavedLocations." + p.getUniqueId()) == null) {
			conf.getCustomConfig().set("SavedLocations." + p.getUniqueId() + ".Loc" + 1 + ".x", x);
			conf.getCustomConfig().set("SavedLocations." + p.getUniqueId() + ".Loc" + 1 + ".y", y);
			conf.getCustomConfig().set("SavedLocations." + p.getUniqueId() + ".Loc" + 1 + ".z", z);
			conf.getCustomConfig().set("SavedLocations." + p.getUniqueId() + ".Loc" + 1 + ".pitch", p.getLocation().getPitch());
			conf.getCustomConfig().set("SavedLocations." + p.getUniqueId() + ".Loc" + 1 + ".yaw", p.getLocation().getYaw());
			conf.getCustomConfig().set("SavedLocations." + p.getUniqueId() + ".Loc" + 1 + ".world", p.getWorld().getName());
			meta.setLore(Arrays.asList(ChatColor.GRAY + "Travelled " + ChatColor.AQUA + travelled + ChatColor.GRAY + " blocks"));
			meta.setDisplayName("Compass Activated");
			compass.setItemMeta(meta);
			compass.setDurability((short) 1); 
		} else {
			count = conf.getCustomConfig().getConfigurationSection("SavedLocations." + p.getUniqueId()).getKeys(false).size(); count++;
			conf.getCustomConfig().set("SavedLocations." + p.getUniqueId() + ".Loc" + count + ".x", x);
			conf.getCustomConfig().set("SavedLocations." + p.getUniqueId() + ".Loc" + count + ".y", y);
			conf.getCustomConfig().set("SavedLocations." + p.getUniqueId() + ".Loc" + count + ".z", z);
			conf.getCustomConfig().set("SavedLocations." + p.getUniqueId() + ".Loc" + count + ".pitch", p.getLocation().getPitch());
			conf.getCustomConfig().set("SavedLocations." + p.getUniqueId() + ".Loc" + count + ".yaw", p.getLocation().getYaw());
			conf.getCustomConfig().set("SavedLocations." + p.getUniqueId() + ".Loc" + count + ".world", p.getWorld().getName());
			meta.setLore(Arrays.asList(ChatColor.GRAY + "Travelled " + ChatColor.AQUA + travelled + ChatColor.GRAY + " blocks"));
			meta.setDisplayName("Compass Activated");
			compass.setItemMeta(meta);
			compass.setDurability((short) count);
		}
		conf.saveCustomConfig();
		p.updateInventory();
	}

	private int seconds = this.getConfig().getInt("cooldown-seconds");
	private HashMap<UUID, Long> hashmap = new HashMap<UUID, Long>();

	public void putInCooldown(Player player) {
		hashmap.put(player.getUniqueId(), System.currentTimeMillis() + (seconds * 1000L));
	}

	public boolean isOnCooldown(Player player) {
		UUID uuid = player.getUniqueId();
		return (hashmap.get(uuid) != null && hashmap.get(uuid) > System.currentTimeMillis());
	}

	public int getCooldownSeconds(Player player) {
		UUID uuid = player.getUniqueId();
		return (int)  ((hashmap.get(uuid) - System.currentTimeMillis()) / 1000L);
	}

	private void loadConfig() {
		try {
			if (!getDataFolder().exists()) getDataFolder().mkdirs();
			File file = new File(getDataFolder(), "config.yml");
			if (!file.exists()) saveDefaultConfig();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
