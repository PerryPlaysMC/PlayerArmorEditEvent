package armoreditevents;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import armoreditevents.PlayerArmorEditEvent.Cause;
import armoreditevents.PlayerArmorEditEvent.ArmorType;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Copy Right ©
 * This code is private
 * Owner: PerryPlaysMC
 * From: 02/2021-Now
 **/

public class ArmorListener implements Listener {

    private JavaPlugin plugin;
    private PluginManager pm;

    public ArmorListener(JavaPlugin plugin) {
        this.plugin = plugin;
        pm = Bukkit.getPluginManager();
        boolean isRegistered = false;
        for(RegisteredListener reg : HandlerList.getRegisteredListeners(plugin))
            if(reg.getListener().getClass().getName().equals(getClass().getName())) {
                isRegistered = true;
                break;
            }
        if(!isRegistered) pm.registerEvents(this, plugin);
    }

    @EventHandler void onInventoryClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        ItemStack[] armor = player.getInventory().getArmorContents();
        if(e.getView().getType() != InventoryType.CRAFTING && e.getView().getTopInventory().getSize() != 5)return;
        if(e.getClick() == ClickType.DOUBLE_CLICK) {
            ItemStack cursor = e.getCursor();
            if(isAir(cursor))return;
            if(cursor.getAmount() == cursor.getType().getMaxStackSize())return;
            int i = -1;
            int amount = 0;
            ItemStack[] newArmor = new ItemStack[4];
            boolean changed = false;
            for(ItemStack item : armor) {
                i++;
                newArmor[i] = item;
                if(item==null)continue;
                if(!item.isSimilar(cursor))continue;
                PlayerArmorEditEvent event = new PlayerArmorEditEvent(player,36+i,item,(null),ArmorType.fromSlot(36+i), Cause.CURSOR_COLLECT);
                pm.callEvent(event);
                if(event.isCancelled()) {
                    newArmor[i] = item.clone();
                    amount += item.getAmount();
                    changed = true;
                    continue;
                }
                if(event.getNewPiece().equals(item))continue;
                changed = true;
            }
            int finalAmount = amount;
            if(changed)
            (new BukkitRunnable() {
                @Override
                public void run() {
                    cursor.setAmount(cursor.getAmount() - finalAmount);
                    for(ItemStack item : player.getInventory().getContents()) {
                        if(item!=null&&item.isSimilar(cursor)) {
                            while(cursor.getAmount() < cursor.getType().getMaxStackSize() && item.getAmount() > 0) {
                                item.setAmount(item.getAmount() - 1);
                                cursor.setAmount(cursor.getAmount() + 1);
                            }
                            break;
                        }
                    }
                    player.getInventory().setArmorContents(newArmor);
                    player.updateInventory();
                    player.setItemOnCursor(cursor);
                }
            }).runTaskLater(plugin, 0);
            return;
        }
        if(e.getSlot() >= 36 && e.getSlot() <= 39) {
            ItemStack oldItem = e.getCurrentItem();
            ItemStack newItem = e.getCursor();
            Cause cause = Cause.SET;
            if(isAir(newItem) || e.isShiftClick() ||
                    (newItem.isSimilar(oldItem) && (newItem.getAmount() + oldItem.getAmount()) < newItem.getType().getMaxStackSize())) cause = Cause.TAKE;
            else if(!isAir(oldItem) && !isAir(newItem)) cause = Cause.SWAP;
            ArmorType aType = ArmorType.fromSlot(e.getSlot());
            PlayerArmorEditEvent event = new PlayerArmorEditEvent(player, e.getSlot(), oldItem, newItem, aType, cause);
            if(ArmorType.fromItem(newItem)==null)event.setAttemptNonArmor(true);
            if(ArmorType.fromItem(newItem)!=null&&ArmorType.fromSlot(e.getSlot())!=ArmorType.fromItem(newItem))event.setAttemptedWrongSlot(true);
            pm.callEvent(event);
            if(event.isCancelled()) {
                e.setCancelled(true);
                return;
            }
            if(cause!= Cause.TAKE && (!event.getNewPiece().equals(newItem) || ((event.isAttemptNonArmor()||event.isAttemptedWrongSlot())&&event.isForced()))) {
                ItemStack newI = event.getNewPiece();
                if(cause== Cause.SET) {
                    ItemStack clone = isAir(newItem) ? new ItemStack(Material.AIR) : newItem.clone();
                    if(!isAir(clone)) clone.setAmount(clone.getAmount() - event.getNewPiece().getAmount());
                    e.setCursor(clone);
                }
                armor[aType.getId()] = newI;
                (new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.getInventory().setArmorContents(armor);
                        player.updateInventory();
                        player.setItemOnCursor(oldItem);
                    }
                }).runTaskLater(plugin, 0);
            }
            ItemStack clone = isAir(oldItem) ? new ItemStack(Material.AIR) : oldItem.clone();
            if(!event.getOldPiece().equals(oldItem) && !isAir(event.getOldPiece())) {
                if(e.getClick() == ClickType.RIGHT||e.getClick()==ClickType.LEFT) {
                    ItemStack item = event.getOldPiece();
                    double amt = e.getClick()==ClickType.RIGHT ? item.getAmount()/2d : item.getAmount();
                    if(amt <= 0.5) amt = 1;
                    item.setAmount((int)amt);
                    e.setCursor(item);
                    if(cause == Cause.TAKE) e.setCurrentItem(null);
                }else {
                    player.getInventory().addItem(event.getOldPiece());
                    if(!isAir(clone))
                        (new BukkitRunnable() {
                            @Override
                            public void run() {
                                player.getInventory().removeItem(clone);
                            }
                        }).runTaskLater(plugin, 0);
                }
            }
            return;
        }
        if(!e.isShiftClick()) return;
        ArmorType aType = ArmorType.fromItem(e.getCurrentItem());
        if(aType == null) return;
        if(!isAir(armor[aType.getId()]))return;
        ItemStack oldItem = armor[aType.getId()];
        ItemStack newItem = e.getCurrentItem();
        PlayerArmorEditEvent event = new PlayerArmorEditEvent(player, e.getSlot(), oldItem, newItem, aType, Cause.SET);
        pm.callEvent(event);
        if(event.isCancelled()) {
            e.setCancelled(true);
            return;
        }
        armor[aType.getId()] = event.getNewPiece();
        (new BukkitRunnable() {
            @Override
            public void run() {
                if(!event.getOldPiece().equals(oldItem))
                    player.getInventory().setItem(e.getSlot(), event.getOldPiece());
                player.getInventory().setArmorContents(armor);
            }
        }).runTaskLater(plugin, 0);
    }

    @EventHandler void onBreak(PlayerItemBreakEvent e) {
        Player player = e.getPlayer();
        ArmorType aType = ArmorType.fromItem(e.getBrokenItem());
        if(aType == null) return;
        ItemStack[] armor = player.getInventory().getArmorContents();
        if(isAir(armor[aType.getId()])||!armor[aType.getId()].equals(e.getBrokenItem()))return;
        PlayerArmorEditEvent event = new PlayerArmorEditEvent(player, 36 + aType.getId(), e.getBrokenItem(), e.getBrokenItem(), aType, Cause.BREAK);
        pm.callEvent(event);
        if(event.isCancelled()) {
            ItemStack newI = e.getBrokenItem();
            newI.setDurability((short)(newI.getDurability()-1));
            armor[aType.getId()] = newI;
            player.getInventory().setArmorContents(armor);
            return;
        }
        if(!event.getNewPiece().isSimilar(e.getBrokenItem())) {
            armor[aType.getId()] = event.getNewPiece();
            player.getInventory().setArmorContents(armor);
        }

    }

    @EventHandler void onPlayerInteractEvent(PlayerInteractEvent e) {
        if(!e.getAction().name().contains("RIGHT") || !e.hasItem()) return;
        ArmorType aType = ArmorType.fromItem(e.getItem());
        if(aType == null) return;
        Player player = e.getPlayer();
        ItemStack[] armor = player.getInventory().getArmorContents();
        if(!isAir(armor[aType.getId()]))return;
        ItemStack newItem = e.getItem();
        PlayerArmorEditEvent event = new PlayerArmorEditEvent(player, player.getInventory().getHeldItemSlot(), (null), newItem, aType, Cause.RIGHT_CLICK);
        pm.callEvent(event);
        if(event.isCancelled()) {
            e.setCancelled(true);
            return;
        }
        if(!isAir(event.getOldPiece()))
            player.getInventory().setItem(player.getInventory().getHeldItemSlot(), event.getOldPiece());
        if(!event.getNewPiece().equals(e.getItem())) {
            armor[aType.getId()] = event.getNewPiece();
            player.getInventory().setArmorContents(armor);
        }
    }

    @EventHandler void onDispense(BlockDispenseArmorEvent e) {
        if(!(e.getTargetEntity() instanceof Player))return;
        ItemStack item = e.getItem();
        ArmorType aType = ArmorType.fromItem(item);
        if(aType == null) return;
        Player player = (Player) e.getTargetEntity();
        ItemStack[] armor = player.getInventory().getArmorContents();
        if(!isAir(armor[aType.getId()]))return;
        ItemStack newItem = e.getItem();
        PlayerArmorEditEvent event = new PlayerArmorEditEvent(player, player.getInventory().getHeldItemSlot(), (null), newItem, aType, Cause.DISPENSER);
        pm.callEvent(event);
        if(event.isCancelled()) {
            e.setCancelled(true);
            return;
        }

        if(!isAir(event.getOldPiece())) {
            Dispenser dispenser = (Dispenser) e.getBlock().getState();
            dispenser.getInventory().addItem(event.getOldPiece());
        }
        if(!event.getNewPiece().equals(item)) {
            armor[aType.getId()] = event.getNewPiece();
            player.getInventory().setArmorContents(armor);
        }
    }


    private boolean isAir(Material mat) {
        return mat.name().endsWith("AIR") && !mat.name().endsWith("AIRS");
    }

    private boolean isAir(ItemStack item) {
        return item == null || isAir(item.getType());
    }

}
