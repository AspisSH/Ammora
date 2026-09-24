package com.ammora.mod.client.model;

import com.ammora.mod.entity.CourierParrotEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.ParrotModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * Model wrapper for Courier Parrot satisfying HierarchicalModel<CourierParrotEntity> type bounds.
 */
public class CourierParrotModel extends HierarchicalModel<CourierParrotEntity> {

    private final ParrotModel delegate;
    private final ModelPart root;

    public CourierParrotModel(ModelPart root) {
        this.root = root;
        this.delegate = new ParrotModel(root);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    public ModelPart getHead() {
        return this.delegate.root().getChild("head");
    }

    @Override
    public void setupAnim(CourierParrotEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.delegate.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    }
}
