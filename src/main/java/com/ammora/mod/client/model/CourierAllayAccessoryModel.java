package com.ammora.mod.client.model;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.entity.CourierAllayEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * Model for the Courier Allay postman cap with blue crown, visor, and gold cockade.
 */
public class CourierAllayAccessoryModel extends HierarchicalModel<CourierAllayEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "courier_allay_accessories"), "main");

    private final ModelPart root;
    private final ModelPart head;

    public CourierAllayAccessoryModel(ModelPart root) {
        this.root = root.getChild("root");
        this.head = this.root.getChild("head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Match AllayModel root and head positions (root at y=23.5F, head at y=-3.99F)
        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 23.5F, 0.0F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, -3.99F, 0.0F));

        // Cap Crown, Visor, and Gold Cockade fitted to Allay's 5x5x5 head
        // Head top is y = -5.0F, front is z = -2.5F
        head.addOrReplaceChild("cap", CubeListBuilder.create()
                // Crown: width 4.6F, height 2.0F, depth 4.6F sitting atop head
                .texOffs(0, 0).addBox(-2.3F, -6.8F, -2.3F, 4.6F, 2.0F, 4.6F)
                // Visor: width 4.6F, height 0.6F, depth 1.8F extending forward
                .texOffs(0, 9).addBox(-2.3F, -5.4F, -4.1F, 4.6F, 0.6F, 1.8F)
                // Gold Cockade: width 1.4F, height 1.4F, depth 0.4F on front of crown
                .texOffs(17, 0).addBox(-0.7F, -6.4F, -2.5F, 1.4F, 1.4F, 0.4F),
                PartPose.ZERO
        );

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(CourierAllayEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        float f3 = ageInTicks * 9.0F * (float) (Math.PI / 180.0);
        float f4 = Math.min(limbSwingAmount / 0.3F, 1.0F);
        float f5 = 1.0F - f4;

        this.head.xRot = headPitch * (float) (Math.PI / 180.0);
        this.head.yRot = netHeadYaw * (float) (Math.PI / 180.0);
        this.root.y = 23.5F + (float) Math.cos((double) f3) * 0.25F * f5;
    }
}
