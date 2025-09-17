package me.usainsrht.sit.listeners;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import me.usainsrht.sit.Sit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.persistence.PersistentDataType;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j(topic = "DismountListener")
public class DismountListener implements Listener {
    private NamespacedKey logInFix = new NamespacedKey("sit","fixme");
    private Map<UUID, Instant> suffocationTimeout = new ConcurrentHashMap<>();

    private void dismountPlayer(Entity seat, Entity player){
        Location dismountSpot = seat == null ? player.getLocation() : seat.getLocation();
        dismountSpot = dismountSpot.add(0,0.5,0);
        double xDiff = (dismountSpot.getX() - dismountSpot.getBlockX()) - 0.5;
        double zDiff = (dismountSpot.getZ() - dismountSpot.getBlockZ()) - 0.5;
        Block headBlock = dismountSpot.getBlock().getRelative(0,1,0);
        //log.info("headblock is at {}", headBlock.getLocation());
        boolean tryForward = !headBlock.isPassable();
        Block forwardBlock = null;
        Block forwardUnderBlock = null;
        if(tryForward){
            //log.info("trying forward");
            if(Math.abs(xDiff) > 0.08){
                forwardBlock = dismountSpot.getBlock().getRelative((int)Math.round(Math.signum(xDiff)), 0, 0);
            }else if(Math.abs(zDiff) > 0.08){
                forwardBlock = dismountSpot.getBlock().getRelative(0, 0, (int)Math.round(Math.signum(zDiff)));
            }

            if(forwardBlock != null){
                //log.info("found forward block");
                forwardUnderBlock = forwardBlock.getRelative(0,-2,0);
                //log.info("forward block is {} at {}", forwardBlock.getType(), forwardBlock.getLocation());
                //log.info("forward under block is {} at {}", forwardUnderBlock.getType(), forwardUnderBlock.getLocation());
            }
        }
        if(forwardBlock != null && forwardBlock.isPassable() && !forwardUnderBlock.isPassable()){
            //log.info("using forward block strategy");
            dismountSpot = forwardUnderBlock.getLocation().toCenterLocation();
            dismountSpot.setY(dismountSpot.getBlockY() + 1);
            //log.info("teleporting to {}", dismountSpot);
        }else if(Math.abs(xDiff) > 0.08){
            dismountSpot.setX(dismountSpot.getX() + (xDiff * 2d));
        }else if(Math.abs(zDiff) > 0.08){
            dismountSpot.setZ(dismountSpot.getZ() + (zDiff * 2d));
        }
        dismountSpot.setYaw(player.getYaw());
        dismountSpot.setPitch(player.getPitch());
        this.suffocationTimeout.put(player.getUniqueId(), Instant.now().plusSeconds(1));
        player.teleportAsync(dismountSpot, PlayerTeleportEvent.TeleportCause.PLUGIN).thenAccept((b) -> {
            if(seat != null) {
                seat.getScheduler().run(Sit.getInstance(), (t) -> {
                    seat.remove();
                }, null);
            }
        });
    }

    @EventHandler
    public void onDismount(EntityDismountEvent e) {
        if (e.getDismounted().hasMetadata("stair")) {
            this.dismountPlayer(e.getDismounted(), e.getEntity());
        }
    }

    @EventHandler
    public void onDisconnect(PlayerQuitEvent e){
        if(e.getPlayer().getVehicle() != null && e.getPlayer().getVehicle().hasMetadata("stair")){
            e.getPlayer().leaveVehicle();
            e.getPlayer().getPersistentDataContainer().set(this.logInFix, PersistentDataType.BOOLEAN, true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e){
        if(e.getPlayer().getPersistentDataContainer().has(this.logInFix)){
            e.getPlayer().getPersistentDataContainer().remove(this.logInFix);
            this.dismountPlayer(null, e.getPlayer());
        }
    }

    @EventHandler
    public void onSuffocate(EntityDamageEvent e){
        if(!(e.getEntity() instanceof Player player)){
            return;
        }
        if(e.getCause() != EntityDamageEvent.DamageCause.SUFFOCATION){
            return;
        }
        if(this.suffocationTimeout.getOrDefault(player.getUniqueId(), Instant.EPOCH).isAfter(Instant.now())) {
            e.setCancelled(true);
            e.setDamage(0);
        }
    }
}
