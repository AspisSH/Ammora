package com.ammora.mod.client.model;

import com.ammora.mod.entity.CourierAllayEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.AllayModel;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Model wrapper for Courier Allay satisfying HierarchicalModel<CourierAllayEntity> and ArmedModel type bounds.
 */
public class CourierAllayModel extends HierarchicalModel<CourierAllayEntity> implements ArmedModel {

    private final AllayModel delegate;
    private final ModelPart root;

    public CourierAllayModel(ModelPart root) {
        this.root = root;
        this.delegate = new AllayModel(root);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(CourierAllayEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.delegate.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // Explicitly raise arms forward in item-holding pose when carrying delivery
        if (entity.getDeliveredItem() != null && !entity.getDeliveredItem().isEmpty()) {
            ModelPart body = this.delegate.root().getChild("body");
            float armAngle = -1.1F; // ~63 degrees forward
            body.getChild("right_arm").xRot = armAngle;
            body.getChild("left_arm").xRot = armAngle;
            body.getChild("right_arm").yRot = 0.28F;
            body.getChild("left_arm").yRot = -0.28F;
            body.getChild("right_arm").zRot = 0.12F;
            body.getChild("left_arm").zRot = -0.12F;
        }
    }

    @Override
    public void translateToHand(HumanoidArm arm, PoseStack poseStack) {
        this.delegate.translateToHand(arm, poseStack);
    }
}
