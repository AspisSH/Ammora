package com.ammora.mod.client.model;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.entity.CourierBeeEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Model for the courier bee postal cap and leather mailbag accessories.
 */
public class CourierBeeAccessoryModel extends HierarchicalModel<CourierBeeEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "courier_bee_accessories"), "main");

    private final ModelPart bone;

    public CourierBeeAccessoryModel(ModelPart root) {
        this.bone = root.getChild("bone");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition bone = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        // Cap Crown, Visor, and Gold Cockade
        // Top of bee body is y = -4.0F, front is z = -5.0F
        bone.addOrReplaceChild("cap", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.0F, -7.0F, -5.0F, 6.0F, 3.0F, 5.0F)
                .texOffs(0, 9).addBox(-3.0F, -5.0F, -7.0F, 6.0F, 1.0F, 2.0F)
                .texOffs(17, 0).addBox(-1.0F, -6.5F, -5.2F, 2.0F, 2.0F, 1.0F),
                PartPose.ZERO
        );

        // Mail Satchel & Leather Shoulder Strap
        // Side of bee body is x = 3.5F
        bone.addOrReplaceChild("satchel", CubeListBuilder.create()
                .texOffs(0, 13).addBox(3.55F, -1.0F, -2.5F, 2.0F, 5.0F, 5.0F)
                .texOffs(0, 24).addBox(-3.6F, -4.2F, -1.5F, 7.3F, 1.0F, 3.0F),
                PartPose.ZERO
        );

        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    public ModelPart root() {
        return this.bone;
    }

    @Override
    public void setupAnim(CourierBeeEntity bee, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.bone.xRot = 0.0F;
        this.bone.yRot = 0.0F;
        this.bone.zRot = 0.0F;

        boolean flag = bee.onGround() && bee.getDeltaMovement().lengthSqr() < 1.0E-7;
        if (!bee.isAngry()) {
            if (!flag) {
                float f1 = Mth.cos(ageInTicks * 0.18F);
                this.bone.xRot = 0.1F + f1 * (float) Math.PI * 0.025F;
                this.bone.y = 19.0F - Mth.cos(ageInTicks * 0.18F) * 0.9F;
            } else {
                this.bone.y = 19.0F;
            }
        } else {
            this.bone.y = 19.0F;
        }

        float roll = bee.getRollAmount(limbSwingAmount);
        if (roll > 0.0F) {
            this.bone.xRot = net.minecraft.client.model.ModelUtils.rotlerpRad(this.bone.xRot, 3.0915928F, roll);
        }
    }
}
