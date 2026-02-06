package net.singularitaet.pegasusboots.logic;



import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PegasusBootsLogic {

    // ============================
    // TUNABLE VALUES
    // ============================

    private static final double MAX_HORIZONTAL_SPEED = 1.0;      // Ground cap
    private static final double MID_AIR_ACCEL = 0.025;           // Forward air accel
    private static final double STRAFE_FACTOR = 0.015;           // Side control
    private static final double SNEAK_HORIZONTAL_CAP = 1.0;      // Glide cap
    private static final double SNEAK_GRAVITY_FACTOR = 0.25;     // Slowfall strength
    private static final double MAX_SNEAK_FALL_SPEED = -0.5;     // Vertical safety cap

    // ============================

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;

        if (player.level().isClientSide()) return;

        // ---------------------------------
        // NEW: Require leather boots
        // ---------------------------------
        ItemStack boots = player.getInventory().getArmor(0);
        if (!boots.is(Items.LEATHER_BOOTS)) return;
        // ---------------------------------

        Vec3 velocity = player.getDeltaMovement();
        boolean onGround = player.onGround();
        boolean sneaking = player.isShiftKeyDown();

        // ---------------------------------
        // Ground horizontal safety cap
        // ---------------------------------
        if (onGround) {
            velocity = capHorizontalSpeed(velocity, MAX_HORIZONTAL_SPEED);
        }

        // ---------------------------------
        // Mid-air chaotic momentum
        // ---------------------------------
        if (!onGround) {
            Vec3 lookDir = player.getLookAngle().normalize();

            // Forward momentum (dominant)
            velocity = velocity.add(lookDir.scale(MID_AIR_ACCEL));

            // Strafe influence (delayed control)
            Vec3 strafe = getStrafeVector(player).scale(STRAFE_FACTOR);
            velocity = velocity.add(strafe);

            // ---------------------------------
            // Sneak glide / fall break
            // ---------------------------------
            if (sneaking) {
                // Cap downward momentum
                if (velocity.y < MAX_SNEAK_FALL_SPEED) {
                    velocity = new Vec3(velocity.x, MAX_SNEAK_FALL_SPEED, velocity.z);
                }

                // Reduce gravity
                velocity = new Vec3(
                        velocity.x,
                        velocity.y * SNEAK_GRAVITY_FACTOR,
                        velocity.z
                );

                // Cap horizontal glide speed
                velocity = capHorizontalSpeed(velocity, SNEAK_HORIZONTAL_CAP);
            } else {
                // Normal air safety cap
                velocity = capHorizontalSpeed(velocity, MAX_HORIZONTAL_SPEED);
            }
        }

        player.setDeltaMovement(velocity);
    }

    // ============================
    // Helpers
    // ============================

    private static Vec3 capHorizontalSpeed(Vec3 vel, double max) {
        double speed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        if (speed > max) {
            double scale = max / speed;
            return new Vec3(vel.x * scale, vel.y, vel.z * scale);
        }
        return vel;
    }

    private static Vec3 getStrafeVector(Player player) {
        float forward = player.zza;
        float strafe = player.xxa;

        double yaw = Math.toRadians(player.getYRot());
        double x = (strafe * Math.cos(yaw)) - (forward * Math.sin(yaw));
        double z = (forward * Math.cos(yaw)) + (strafe * Math.sin(yaw));

        return new Vec3(x, 0, z);
    }
}
