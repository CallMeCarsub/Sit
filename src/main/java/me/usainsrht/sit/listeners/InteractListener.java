package me.usainsrht.sit.listeners;

import me.usainsrht.sit.Sit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;

public class InteractListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSit(PlayerInteractEvent e) {
        if (e.getHand() == null || !e.getHand().equals(EquipmentSlot.HAND)) return;

        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;

        Block block = e.getClickedBlock();
        if(block == null){
            return;
        }

        Block above = block.getRelative(BlockFace.UP);
        if(!above.isPassable() || above.isCollidable()){
            return;
        }
        BlockData blockData = block.getBlockData();

        if (blockData instanceof Stairs) {
            Bisected bisected = (Bisected) blockData;
            if (!bisected.getHalf().equals(Bisected.Half.BOTTOM)) return;
        }
        else if (blockData instanceof Slab) {
            if (!((Slab) blockData).getType().equals(Slab.Type.BOTTOM)) return;
        }else{
            return;
        }

        Player p = e.getPlayer();

        if (p.isSneaking()) return;

        if (!p.getInventory().getItemInMainHand().isEmpty()) return;

        if (p.isInsideVehicle()) return;

        Sit instance = Sit.getInstance();
        //FileConfiguration config = instance.getConfig();


        e.setCancelled(true);

        Location loc = block.getLocation();

        double adderX = 0.5;
        double adderY = 0.5;
        double adderZ = 0.5;


        if (blockData instanceof Directional) {
            BlockFace facing = ((Directional) blockData).getFacing();
            switch (facing) {
                case SOUTH:
                    loc.setYaw(180);
                    adderZ -= 0.1;
                    break;
                case WEST:
                    adderX += 0.1;
                    loc.setYaw(270);
                    break;
                case EAST:
                    adderX -= 0.1;
                    loc.setYaw(90);
                    break;
                case NORTH:
                    adderZ += 0.1;
                    loc.setYaw(0);
                    break;
            }
        }
        else {
            loc.setYaw(p.getLocation().getYaw()+180);
        }

        loc.setX(loc.getX() + adderX);
        loc.setY(loc.getY() + adderY);
        loc.setZ(loc.getZ() + adderZ);

        if (blockData instanceof Stairs) {
            Stairs.Shape shape = ((Stairs) blockData).getShape();
            if (shape == Stairs.Shape.INNER_RIGHT || shape == Stairs.Shape.OUTER_RIGHT) {
                loc.setYaw(loc.getYaw()+45);
            }
            else if (shape == Stairs.Shape.INNER_LEFT || shape == Stairs.Shape.OUTER_LEFT) {
                loc.setYaw(loc.getYaw()-45);
            }
        }

        // create final value to use in lambda
        Entity entity = p.getWorld().spawn(loc, EntityType.ITEM_DISPLAY.getEntityClass(), (seat -> {
            seat.setPersistent(false);
            if (seat instanceof Attributable attributable) {
                // set movement speed to 0 to entity to not move when steering item(carrot on a stick) held
                attributable.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0);
                attributable.getAttribute(Attribute.MAX_HEALTH).setBaseValue(0);
            }
            if(seat instanceof ItemDisplay itemDisplay){
                itemDisplay.setViewRange(0);
            }
            seat.setInvisible(true);
            seat.setInvulnerable(true);
            seat.setSilent(true);
            seat.setMetadata("stair", new FixedMetadataValue(instance, true));

            if (seat instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) seat;
                livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, -1, 1, false, false));
                livingEntity.setInvisible(true);
                livingEntity.setAI(false);
                livingEntity.setHealth(0);
            }
        }));

        entity.addPassenger(p);
    }
}
