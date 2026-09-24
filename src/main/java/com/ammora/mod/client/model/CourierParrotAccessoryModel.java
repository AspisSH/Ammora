package com.ammora.mod.client.model;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.entity.CourierParrotEntity;
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
 * Model for the courier parrot postal uniform cap.
 */
public class CourierParrotAccessoryModel extends HierarchicalModel<CourierParrotEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "courier_parrot_accessories"), "main");

    private final ModelPart cap;

    public CourierParrotAccessoryModel(ModelPart root) {
        this.cap = root.getChild("cap");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Postal cap designed to sit snugly atop Parrot head box (-1.0 to 1.0 X, -1.5 to 1.5 Y, -1.0 to 1.0 Z)
        partdefinition.addOrReplaceChild("cap", CubeListBuilder.create()
                // Crown: width 2.4F, height 1.2F, depth 2.4F atop head
                .texOffs(0, 0).addBox(-1.2F, -2.7F, -1.4F, 2.4F, 1.2F, 2.4F)
                // Visor: width 2.4F, height 0.4F, depth 1.0F protruding over beak
                .texOffs(0, 9).addBox(-1.2F, -1.9F, -2.4F, 2.4F, 0.4F, 1.0F)
                // Gold Cockade: width 0.8F, height 0.8F, depth 0.2F on front of crown
                .texOffs(17, 0).addBox(-0.4F, -2.5F, -1.5F, 0.8F, 0.8F, 0.2F),
                PartPose.ZERO
        );

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.cap;
    }

    @Override
    public void setupAnim(CourierParrotEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Transformations are applied via parent head translateAndRotate in layer
    }
}
