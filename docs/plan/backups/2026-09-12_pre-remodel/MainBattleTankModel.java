package io.github.littlebird.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.MainBattleTankEntity;
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
 * M1A5 placeholder model: a low, wide hull on two tracks with a rotating turret and its 120mm
 * barrel. Deliberately a box build, not a detailed airframe - the missing-asset list records that
 * it is a placeholder until real art arrives. Unit space matches the geometry class: nose along
 * -Z, 16 units per block, turret swinging around the ring at the roof.
 */
public final class MainBattleTankModel extends EntityModel<MainBattleTankEntity> {
    public static final ModelLayerLocation MAIN_BATTLE_TANK_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(LittleBird.MOD_ID, "main_battle_tank"), "main");
    private static final int TEXTURE_SIZE = 512;

    private final ModelPart root;
    private final ModelPart turret;

    public MainBattleTankModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        this.turret = body.getChild("turret");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Hull: 7 blocks long, 3.5 wide, roof at 2.1 blocks.
        body.addOrReplaceChild("hull", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-28.0F, 8.0F, -56.0F, 56.0F, 26.0F, 112.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("glacis", CubeListBuilder.create()
                .texOffs(0, 144).addBox(-26.0F, 20.0F, -60.0F, 52.0F, 14.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // Tracks and the wheels that read as tracks.
        body.addOrReplaceChild("track_left", CubeListBuilder.create()
                .texOffs(0, 176).addBox(-31.0F, 0.0F, -54.0F, 5.0F, 12.0F, 108.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("track_right", CubeListBuilder.create()
                .texOffs(0, 176).addBox(26.0F, 0.0F, -54.0F, 5.0F, 12.0F, 108.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("engine_deck", CubeListBuilder.create()
                .texOffs(0, 304).addBox(-26.0F, 34.0F, 20.0F, 52.0F, 6.0F, 32.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Turret on the ring, with the barrel reaching forward down -Z.
        PartDefinition turret = body.addOrReplaceChild("turret", CubeListBuilder.create()
                .texOffs(0, 240).addBox(-14.0F, 0.0F, -18.0F, 28.0F, 14.0F, 34.0F),
                PartPose.offset(0.0F, 34.0F, 0.0F));
        turret.addOrReplaceChild("barrel", CubeListBuilder.create()
                .texOffs(0, 288).addBox(-3.0F, 4.0F, -74.0F, 6.0F, 6.0F, 56.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        turret.addOrReplaceChild("cupola", CubeListBuilder.create()
                .texOffs(224, 240).addBox(-7.0F, 14.0F, -8.0F, 14.0F, 6.0F, 14.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    @Override
    public void setupAnim(MainBattleTankEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // The turret's synced attitude is absolute, so the mesh shows the swing against the hull.
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
