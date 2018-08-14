package com.starwarsfreak219.flightparticle;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class FlightParticle
  extends JavaPlugin
  implements Listener
{
  public void onEnable()
  {
	Check();
    Bukkit.getPluginManager().registerEvents(this, this);
  
    FileConfiguration config = getConfig();
    config.options().copyDefaults(true);
    saveConfig();
  }

  public void onDisable()
  {
    getServer().getPluginManager().disablePlugin(this);
  }
  private void Check() {
	  
  { 
      Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable()
      {
        public void run()
        {
        	for (Player p : Bukkit.getServer().getOnlinePlayers())
        	if ((p.isFlying()) && p.hasPermission(getConfig().getString("Permission"))) {
            Location loc = p.getLocation().subtract(0, 0.4, 0);
            ParticleEffect.FIREWORKS_SPARK.display(0.1F, 0.0F, 0.1F, 0F, 5, loc, 257D);
          }
        }
  		}, 0L, 1L);
    }
   }
  }
