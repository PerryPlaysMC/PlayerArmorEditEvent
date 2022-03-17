# PlayerArmorEditEvent
Detect when a player changes a piece of armor

Usage:
```java
@Override
public void onEnable() {
  {
    new ArmorListener(this);
    //or
    Bukkit.getPluginManager().registerEvents(new ArmorListener(this), this);
  }
  
  Bukkit.getPluginManager().registerEvents(this, this);
}

@EventHandler
void onPlayerArmorEvent(PlayerArmorEditEvent e) {
  e.setForced(true);//allows any item to be worn as armor
  if(e.getNewPiece().getType() == Material.SAND) e.setForced(false);//Don't allow sand to be forced
  if(e.getNewPiece().getType() == Material.DIAMOND_CHESTPLATE) e.setCancelled(true);//Don't allow diamond chestplate to be worn
}
