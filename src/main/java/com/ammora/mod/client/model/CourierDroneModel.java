package com.ammora.mod.client.model;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.entity.CourierDroneEntity;
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
 * Animated model for the Courier Drone with 4 spinning rotors, camera sensor pod, and magnetic clamp.
 */
public class CourierDroneModel extends HierarchicalModel<CourierDroneEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "courier_drone"), "main");

    private final ModelPart fuselage;
    private final ModelPart rotorFl;
    private final ModelPart rotorFr;
    private final ModelPart rotorBl;
    private final ModelPart rotorBr;

    public CourierDroneModel(ModelPart root) {
        this.fuselage = root.getChild("fuselage");
        this.rotorFl = this.fuselage.getChild("rotor_fl");
        this.rotorFr = this.fuselage.getChild("rotor_fr");
        this.rotorBl = this.fuselage.getChild("rotor_bl");
        this.rotorBr = this.fuselage.getChild("rotor_br");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Fuselage (chassis, sensor, arms, skids, clamp) offset at y = 18.0F
        PartDefinition fuselage = partdefinition.addOrReplaceChild(
                "fuselage",
                CubeListBuilder.create()
                        // Central carbon chassis
                        .texOffs(0, 0).addBox(-3.5F, -1.5F, -3.5F, 7.0F, 3.0F, 7.0F)
                        // Optical front camera sensor pod
                        .texOffs(28, 0).addBox(-1.5F, -0.5F, -4.5F, 3.0F, 2.0F, 1.0F)
                        // Top dome antenna
                        .texOffs(36, 0).addBox(-1.0F, -2.5F, -1.0F, 2.0F, 1.0F, 2.0F)
                        // 4 Rotor diagonal carbon arms
                        .texOffs(0, 11).addBox(-6.5F, -0.5F, -6.5F, 3.0F, 1.0F, 3.0F)
                        .texOffs(0, 11).addBox(3.5F, -0.5F, -6.5F, 3.0F, 1.0F, 3.0F)
                        .texOffs(0, 11).addBox(-6.5F, -0.5F, 3.5F, 3.0F, 1.0F, 3.0F)
                        .texOffs(0, 11).addBox(3.5F, -0.5F, 3.5F, 3.0F, 1.0F, 3.0F)
                        // Landing skids
                        .texOffs(0, 16).addBox(-3.5F, 1.5F, -4.0F, 1.0F, 2.0F, 8.0F)
                        .texOffs(0, 16).addBox(2.5F, 1.5F, -4.0F, 1.0F, 2.0F, 8.0F)
                        // Magnetic cargo clamp
                        .texOffs(0, 27).addBox(-1.5F, 1.5F, -1.5F, 3.0F, 1.0F, 3.0F),
                PartPose.offset(0.0F, 18.0F, 0.0F)
        );

        // 4 Spinning Rotors
        CubeListBuilder rotorBlades = CubeListBuilder.create()
                .texOffs(0, 32).addBox(-1.0F, -0.2F, -1.0F, 2.0F, 0.4F, 2.0F)
                .texOffs(0, 36).addBox(-4.0F, 0.0F, -0.5F, 8.0F, 0.2F, 1.0F)
                .texOffs(0, 36).addBox(-0.5F, 0.0F, -4.0F, 1.0F, 0.2F, 8.0F);

        fuselage.addOrReplaceChild("rotor_fl", rotorBlades, PartPose.offset(-5.0F, -1.0F, -5.0F));
        fuselage.addOrReplaceChild("rotor_fr", rotorBlades, PartPose.offset(5.0F, -1.0F, -5.0F));
        fuselage.addOrReplaceChild("rotor_bl", rotorBlades, PartPose.offset(-5.0F, -1.0F, 5.0F));
        fuselage.addOrReplaceChild("rotor_br", rotorBlades, PartPose.offset(5.0F, -1.0F, 5.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    public ModelPart getFuselage() {
        return this.fuselage;
    }

    @Override
    public ModelPart root() {
        return this.fuselage;
    }

    @Override
    public void setupAnim(CourierDroneEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // High-speed rotor rotation
        float spin = ageInTicks * 1.8F;
        this.rotorFl.yRot = spin;
        this.rotorBr.yRot = spin;
        this.rotorFr.yRot = -spin;
        this.rotorBl.yRot = -spin;

        // Subtle hovering bobbing
        this.fuselage.y = 18.0F + Mth.sin(ageInTicks * 0.2F) * 0.6F;

        // Drone flight banking and pitch orientation
        this.fuselage.xRot = headPitch * ((float) Math.PI / 180F) * 0.4F;
        this.fuselage.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.fuselage.zRot = 0.0F;
    }
}
