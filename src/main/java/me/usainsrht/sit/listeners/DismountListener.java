package me.usainsrht.sit.listeners;

import me.usainsrht.sit.Sit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;


public class DismountListener implements Listener {

    @EventHandler
    public void onDismount(EntityDismountEvent e) {
        if (e.getDismounted().hasMetadata("stair")) {
            if(e.getEntity() instanceof Player player){
                player.setNoDamageTicks(20);
            }
            Location dismountSpot = e.getDismounted().getLocation().add(0,0,0);
            double xDiff = (dismountSpot.getX() - dismountSpot.getBlockX()) - 0.5;
            double zDiff = (dismountSpot.getZ() - dismountSpot.getBlockZ()) - 0.5;
            if(Math.abs(xDiff) > 0.08){
                dismountSpot.setX(dismountSpot.getX() + (xDiff * 2d));
            }else if(Math.abs(zDiff) > 0.08){
                dismountSpot.setZ(dismountSpot.getZ() + (zDiff * 2d));
            }
            dismountSpot.setYaw(e.getEntity().getYaw());
            dismountSpot.setPitch(e.getEntity().getPitch());
            e.getDismounted().remove();
            e.getEntity().teleportAsync(dismountSpot).thenAccept((b) -> {
                if(e.getEntity() instanceof Player player){
                    player.setNoDamageTicks(20);
                }
            });
        }
    }
}
