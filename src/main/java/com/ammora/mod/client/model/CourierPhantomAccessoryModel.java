package com.ammora.mod.client.model;

import com.ammora.mod.AmmoraMod;
import com.ammora.mod.entity.CourierPhantomEntity;
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
 * Model for the Courier Phantom postman cap fitted to its flat aerodynamic head.
 */
public class CourierPhantomAccessoryModel extends HierarchicalModel<CourierPhantomEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(AmmoraMod.MODID, "courier_phantom_accessories"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;

    public CourierPhantomAccessoryModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Match PhantomModel body (rotation -0.1F x) and head (offset 0, 1, -7, rotation 0.2F x)
        PartDefinition body = partdefinition.addOrReplaceChild(
                "body",
                CubeListBuilder.create(),
                PartPose.rotation(-0.1F, 0.0F, 0.0F)
        );
        PartDefinition head = body.addOrReplaceChild(
                "head",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 1.0F, -7.0F, 0.2F, 0.0F, 0.0F)
        );

        // Cap Crown, Visor, and Cockade fitted to Phantom's 7x3x5 head
        // Phantom head box: (-4.0F, -2.0F, -5.0F, 7.0F, 3.0F, 5.0F). Centered around x = -0.5F.
        // Head top is y = -2.0F, front is z = -5.0F
        head.addOrReplaceChild("cap", CubeListBuilder.create()
                // Crown: width 5.4F, height 2.2F, depth 4.4F sitting atop head
                .texOffs(0, 0).addBox(-3.2F, -4.2F, -4.6F, 5.4F, 2.2F, 4.4F)
                // Visor: width 5.4F, height 0.7F, depth 2.0F extending forward over eyes
                .texOffs(0, 9).addBox(-3.2F, -2.6F, -6.6F, 5.4F, 0.7F, 2.0F)
                // Gold Cockade: width 1.6F, height 1.6F, depth 0.4F on front of crown
                .texOffs(17, 0).addBox(-1.3F, -3.8F, -4.8F, 1.6F, 1.6F, 0.4F),
                PartPose.ZERO
        );

        // Belly Delivery Parcel Crate and Harness Straps fitted under phantom belly
        // Phantom body box is (-3.0F, -2.0F, -8.0F, 5.0F, 3.0F, 9.0F). Bottom of belly is y = 1.0F.
        body.addOrReplaceChild("belly_crate", CubeListBuilder.create()
                // Wooden parcel crate
                .texOffs(0, 32).addBox(-2.6F, 0.9F, -5.5F, 4.2F, 3.2F, 5.0F)
                // Front harness strap
                .texOffs(0, 44).addBox(-2.8F, 0.8F, -4.8F, 4.6F, 3.4F, 1.0F)
                // Rear harness strap
                .texOffs(0, 44).addBox(-2.8F, 0.8F, -2.2F, 4.6F, 3.4F, 1.0F),
                PartPose.ZERO
        );

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(CourierPhantomEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Phantom head moves rigidly with the body in vanilla PhantomModel
    }
}
