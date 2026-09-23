package com.ammora.mod.entity.ai;

import com.ammora.mod.entity.CourierBeeEntity;
import com.ammora.mod.entity.ICourierEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * AI goal directing any flying courier mob smoothly and directly towards the purchasing player to deliver goods.
 */
public class CourierFlyToPlayerGoal extends Goal {

    private final ICourierEntity courier;
    private final Mob mob;
    private Player targetPlayer;

    public CourierFlyToPlayerGoal(ICourierEntity courier) {
        this.courier = courier;
        this.mob = courier.asMob();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public CourierFlyToPlayerGoal(CourierBeeEntity bee) {
        this((ICourierEntity) bee);
    }

    @Override
    public boolean canUse() {
        if (courier.isDelivered() || courier.getTargetPlayerUuid() == null) {
            return false;
        }
        this.targetPlayer = mob.level().getPlayerByUUID(courier.getTargetPlayerUuid());
        return this.targetPlayer != null && this.targetPlayer.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        this.mob.setNoGravity(true);
        if (this.targetPlayer != null && this.mob.getNavigation() != null) {
            this.mob.getNavigation().moveTo(this.targetPlayer, 1.25D);
        }
    }

    @Override
    public void stop() {
        if (this.mob.getNavigation() != null) {
            this.mob.getNavigation().stop();
        }
    }

    @Override
    public void tick() {
        if (this.targetPlayer == null) return;

        Vec3 targetPos = this.targetPlayer.getEyePosition();
        Vec3 mobCenter = this.mob.position().add(0, this.mob.getBbHeight() * 0.5D, 0);
        Vec3 toPlayer = targetPos.subtract(mobCenter);
        double distSqr = toPlayer.lengthSqr();
        double dist = Math.sqrt(distSqr);

        // Turn face and body directly forwards towards target player
        if (dist > 0.001D) {
            float targetYaw = (float) (Mth.atan2(toPlayer.z, toPlayer.x) * (180.0D / Math.PI)) - 90.0F;
            this.mob.setYRot(targetYaw);
            this.mob.yBodyRot = targetYaw;
            this.mob.yHeadRot = targetYaw;

            double horizDist = Math.sqrt(toPlayer.x * toPlayer.x + toPlayer.z * toPlayer.z);
            float targetPitch = (float) (-(Mth.atan2(toPlayer.y, horizDist) * (180.0D / Math.PI)));
            this.mob.setXRot(targetPitch);
        }

        this.mob.getLookControl().setLookAt(this.targetPlayer, 30.0F, 30.0F);
        if (this.mob.getMoveControl() != null) {
            this.mob.getMoveControl().setWantedPosition(targetPos.x, targetPos.y, targetPos.z, 1.25D);
        }

        this.mob.setNoGravity(true);

        // Steady forward aerial propulsion towards player
        if (dist > 2.0D) {
            Vec3 dir = toPlayer.normalize();
            double flightSpeed = 0.28D;
            Vec3 targetVel = dir.scale(flightSpeed);
            Vec3 currentVel = this.mob.getDeltaMovement();
            this.mob.setDeltaMovement(
                    currentVel.x * 0.6D + targetVel.x * 0.4D,
                    currentVel.y * 0.6D + targetVel.y * 0.4D,
                    currentVel.z * 0.6D + targetVel.z * 0.4D
            );
            this.mob.hasImpulse = true;
        }

        // Delivery arrival distance: <= 3.2 blocks (10.24 squared distance)
        if (distSqr <= 10.24D) {
            this.courier.completeDelivery(this.targetPlayer);
        }
    }
}
