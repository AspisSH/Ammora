package com.ammora.mod.entity.ai;

import com.ammora.mod.entity.CourierBeeEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * AI goal directing the courier bee towards the purchasing player to deliver goods.
 */
public class CourierFlyToPlayerGoal extends Goal {

    private final CourierBeeEntity bee;
    private Player targetPlayer;

    public CourierFlyToPlayerGoal(CourierBeeEntity bee) {
        this.bee = bee;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (bee.isDelivered() || bee.getTargetPlayerUuid() == null) {
            return false;
        }
        this.targetPlayer = bee.level().getPlayerByUUID(bee.getTargetPlayerUuid());
        return this.targetPlayer != null && this.targetPlayer.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        if (this.targetPlayer != null) {
            this.bee.getNavigation().moveTo(this.targetPlayer, 0.35D);
        }
    }

    @Override
    public void tick() {
        if (this.targetPlayer == null) return;

        Vec3 playerEye = this.targetPlayer.getEyePosition();
        Vec3 beePos = this.bee.position();
        double distSqr = beePos.distanceToSqr(playerEye);

        // Turn face towards player
        this.bee.getLookControl().setLookAt(this.targetPlayer, 30.0F, 30.0F);

        // Smooth 3D aerial propulsion towards player (slowed down 4x for gentle, majestic flight)
        Vec3 dir = playerEye.subtract(beePos);
        double dist = Math.sqrt(distSqr);
        if (dist > 0.1) {
            Vec3 moveVec = dir.normalize().scale(0.0875D);
            this.bee.setDeltaMovement(this.bee.getDeltaMovement().scale(0.75D).add(moveVec));
        }

        // Delivery arrival distance: <= 3.0 blocks (9.0 squared distance)
        if (distSqr <= 9.0D) {
            this.bee.completeDelivery(this.targetPlayer);
        }
    }
}
