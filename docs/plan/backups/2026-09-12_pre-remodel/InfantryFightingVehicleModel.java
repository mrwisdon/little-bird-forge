package io.github.littlebird.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.InfantryFightingVehicleEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * EBAA Wildcat placeholder model: a wheeled hull with a two man cab, a small turret and the
 * autocannon barrel. A box build on purpose - the missing-asset list records it as a placeholder.
 * Unit space matches the IFV geometry class, so the hull really is a size smaller than the tank.
 */
public final class InfantryFightingVehicleModel extends EntityModel<InfantryFightingVehicleEntity> {
    public static final ModelLayerLocation INFANTRY_FIGHTING_VEHICLE_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(LittleBird.MOD_ID, "infantry_fighting_vehicle"),
            "main");
    private static final int TEXTURE_SIZE = 512;

    private final ModelPart root;
    private final ModelPart turret;

    public InfantryFightingVehicleModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        this.turret = body.getChild("turret");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        body.addOrReplaceChild("hull", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-24.0F, 8.0F, -48.0F, 48.0F, 22.0F, 96.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("cab", CubeListBuilder.create()
                .texOffs(0, 128).addBox(-22.0F, 16.0F, -52.0F, 44.0F, 14.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("bench", CubeListBuilder.create()
                .texOffs(0, 320).addBox(-20.0F, 30.0F, 18.0F, 40.0F, 4.0F, 24.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // Four wheels, one per corner.
        for (int side = 0; side < 2; side++) {
            float x = side == 0 ? 21.0F : -27.0F;
            String name = side == 0 ? "wheels_right" : "wheels_left";
            body.addOrReplaceChild(name, CubeListBuilder.create()
                    .texOffs(256, 0).addBox(x, 0.0F, -40.0F, 6.0F, 20.0F, 20.0F)
                    .texOffs(256, 0).addBox(x, 0.0F, 20.0F, 6.0F, 20.0F, 20.0F),
                    PartPose.offset(0.0F, 0.0F, 0.0F));
        }

        PartDefinition turret = body.addOrReplaceChild("turret", CubeListBuilder.create()
                .texOffs(0, 192).addBox(-10.0F, 0.0F, -12.0F, 20.0F, 12.0F, 24.0F),
                PartPose.offset(0.0F, 30.0F, 0.0F));
        turret.addOrReplaceChild("barrel", CubeListBuilder.create()
                .texOffs(0, 240).addBox(-2.0F, 3.0F, -52.0F, 4.0F, 4.0F, 40.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("sight", CubeListBuilder.create()
                .texOffs(200, 192).addBox(-6.0F, 12.0F, -10.0F, 12.0F, 6.0F, 12.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    @Override
    public void setupAnim(InfantryFightingVehicleEntity entity, float limbSwing,
                          float limbSwingAmount, float ageInTicks, float netHeadYaw,
                          float headPitch) {
        turret.yRot = ApacheModelMath.turretYawRadians(
                entity.getTurretYawDeg(), entity.getYRot());
        turret.xRot = ApacheModelMath.turretPitchRadians(
                entity.getTurretPitchDeg(), entity.getXRot());
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float red, float green, float blue,
                               float alpha) {
        root.render(pose, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
