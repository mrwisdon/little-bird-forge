package io.github.littlebird.client;

import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.JetEntity;
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
 * B-2 Spirit, generated from the Blender box export: a flying wing, so there is no tail,
 * no canard and no turret to articulate - the whole airframe banks with the entity, the
 * same way {@link Su57Model} does.
 *
 * <p>GENERATED FILE - do not edit by hand. Rebuild with tools/generate_b2_model.py, which
 * reads tools/data/b2_mc.json (exported from the B-2 Blender scene). The export is already
 * in model space (+X right, +Y up, -Z nose, 16 units per block), scaled to the 15-block
 * B2Geometry envelope: nose -120.0F, tail 120.0F, half span 296.0F units.
 *
 * <p>The exporter works on a half-metre cell grid, so wide wing slabs are split by the
 * generator until every box's Minecraft UV patch fits its material zone of the 512x512
 * sheet. The split changes no geometry: the 321 cubes keep the exact volume and outer
 * bounds of the 174 exported boxes.
 */
public final class B2Model extends EntityModel<JetEntity> {
    public static final ModelLayerLocation B2_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(LittleBird.MOD_ID, "b2"), "main");
    private static final int TEXTURE_SIZE = 512;

    private final ModelPart root;

    public B2Model(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // ---- flying wing, 321 cubes from 174 exported boxes ----
        body.addOrReplaceChild("box_0000_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-296.0F, 24.0F, 72.0F, 112.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0000_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-184.0F, 24.0F, 72.0F, 112.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0001_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-288.0F, 24.0F, 64.0F, 112.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0001_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-176.0F, 24.0F, 64.0F, 120.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0002_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-288.0F, 24.0F, 80.0F, 112.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0002_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-176.0F, 24.0F, 80.0F, 104.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0003_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-280.0F, 24.0F, 56.0F, 112.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0003_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-168.0F, 24.0F, 56.0F, 112.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0004", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-272.0F, 24.0F, 88.0F, 80.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0005_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-264.0F, 24.0F, 48.0F, 120.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0005_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-144.0F, 24.0F, 48.0F, 120.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0006", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-264.0F, 24.0F, 96.0F, 64.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0007_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-256.0F, 24.0F, 40.0F, 120.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0007_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-136.0F, 24.0F, 40.0F, 120.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0008", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-248.0F, 24.0F, 104.0F, 32.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0009_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-240.0F, 24.0F, 32.0F, 120.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0009_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-120.0F, 24.0F, 32.0F, 120.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0009_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 24.0F, 32.0F, 120.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0009_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(120.0F, 24.0F, 32.0F, 120.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0010", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-240.0F, 24.0F, 112.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0011_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-224.0F, 24.0F, 24.0F, 112.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0011_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-112.0F, 24.0F, 24.0F, 112.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0011_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 24.0F, 24.0F, 112.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0011_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(112.0F, 24.0F, 24.0F, 112.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0012_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-216.0F, 24.0F, 16.0F, 104.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0012_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-112.0F, 24.0F, 16.0F, 112.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0012_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 24.0F, 16.0F, 112.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0012_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(112.0F, 24.0F, 16.0F, 104.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0013", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-208.0F, 16.0F, 56.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0014_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-208.0F, 32.0F, 40.0F, 96.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0014_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-112.0F, 32.0F, 40.0F, 104.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0015_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-208.0F, 32.0F, 48.0F, 80.0F, 8.0F, 24.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0015_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-128.0F, 32.0F, 48.0F, 72.0F, 8.0F, 24.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0016", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-208.0F, 32.0F, 72.0F, 24.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0017", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-200.0F, 16.0F, 48.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0018_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-200.0F, 24.0F, 8.0F, 104.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0018_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-96.0F, 24.0F, 8.0F, 96.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0018_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 24.0F, 8.0F, 96.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0018_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(96.0F, 24.0F, 8.0F, 104.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0019_0", CubeListBuilder.create()
                .texOffs(256, 0).addBox(-200.0F, 32.0F, 24.0F, 24.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0019_1", CubeListBuilder.create()
                .texOffs(256, 0).addBox(-176.0F, 32.0F, 24.0F, 32.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0019_2", CubeListBuilder.create()
                .texOffs(256, 0).addBox(-144.0F, 32.0F, 24.0F, 48.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0019_3", CubeListBuilder.create()
                .texOffs(256, 0).addBox(-96.0F, 32.0F, 24.0F, 48.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0019_4", CubeListBuilder.create()
                .texOffs(256, 0).addBox(-48.0F, 32.0F, 24.0F, 48.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0019_5", CubeListBuilder.create()
                .texOffs(256, 0).addBox(0.0F, 32.0F, 24.0F, 48.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0019_6", CubeListBuilder.create()
                .texOffs(256, 0).addBox(48.0F, 32.0F, 24.0F, 48.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0019_7", CubeListBuilder.create()
                .texOffs(256, 0).addBox(96.0F, 32.0F, 24.0F, 48.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0019_8", CubeListBuilder.create()
                .texOffs(256, 0).addBox(144.0F, 32.0F, 24.0F, 32.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0019_9", CubeListBuilder.create()
                .texOffs(256, 0).addBox(176.0F, 32.0F, 24.0F, 24.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0020_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-192.0F, 16.0F, 40.0F, 72.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0020_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-120.0F, 16.0F, 40.0F, 72.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0021_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-192.0F, 24.0F, 0.0F, 96.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0021_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-96.0F, 24.0F, 0.0F, 96.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0021_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 24.0F, 0.0F, 96.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0021_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(96.0F, 24.0F, 0.0F, 96.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0022_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-192.0F, 32.0F, 16.0F, 96.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0022_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-96.0F, 32.0F, 16.0F, 96.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0022_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 32.0F, 16.0F, 96.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0022_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(96.0F, 32.0F, 16.0F, 96.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0023_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-184.0F, 16.0F, 32.0F, 72.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0023_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-112.0F, 16.0F, 32.0F, 72.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0024_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-184.0F, 32.0F, 8.0F, 88.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0024_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-96.0F, 32.0F, 8.0F, 96.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0024_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 32.0F, 8.0F, 96.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0024_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(96.0F, 32.0F, 8.0F, 88.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0025_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-176.0F, 16.0F, 24.0F, 80.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0025_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-96.0F, 16.0F, 24.0F, 80.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0026", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-176.0F, 16.0F, 48.0F, 104.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0027_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-176.0F, 24.0F, -8.0F, 88.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0027_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-88.0F, 24.0F, -8.0F, 88.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0027_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 24.0F, -8.0F, 88.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0027_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(88.0F, 24.0F, -8.0F, 88.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0028_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-176.0F, 32.0F, 0.0F, 88.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0028_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-88.0F, 32.0F, 0.0F, 88.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0028_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 32.0F, 0.0F, 88.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0028_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(88.0F, 32.0F, 0.0F, 88.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0029", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-176.0F, 32.0F, 72.0F, 104.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0030_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-168.0F, 24.0F, -16.0F, 88.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0030_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-80.0F, 24.0F, -16.0F, 80.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0030_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 24.0F, -16.0F, 80.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0030_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(80.0F, 24.0F, -16.0F, 88.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0031", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-168.0F, 24.0F, 88.0F, 80.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0032_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-168.0F, 32.0F, -8.0F, 88.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0032_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-80.0F, 32.0F, -8.0F, 80.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0032_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 32.0F, -8.0F, 80.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0032_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(80.0F, 32.0F, -8.0F, 88.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0033", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-168.0F, 32.0F, 80.0F, 96.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0034", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-160.0F, 16.0F, 56.0F, 72.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0035", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-160.0F, 24.0F, 96.0F, 64.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0036_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-160.0F, 32.0F, -16.0F, 80.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0036_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-80.0F, 32.0F, -16.0F, 80.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0036_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 32.0F, -16.0F, 80.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0036_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(80.0F, 32.0F, -16.0F, 80.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0037", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-160.0F, 32.0F, 88.0F, 72.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0038", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-152.0F, 16.0F, 16.0F, 72.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0039_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-152.0F, 24.0F, -24.0F, 72.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0039_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-80.0F, 24.0F, -24.0F, 80.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0039_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 24.0F, -24.0F, 80.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0039_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(80.0F, 24.0F, -24.0F, 72.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0040", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-152.0F, 32.0F, 96.0F, 56.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0041_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-144.0F, 24.0F, -32.0F, 72.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0041_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-72.0F, 24.0F, -32.0F, 72.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0041_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 24.0F, -32.0F, 72.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0041_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(72.0F, 24.0F, -32.0F, 72.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0042", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-144.0F, 24.0F, 104.0F, 32.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0043_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-144.0F, 32.0F, -24.0F, 72.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0043_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-72.0F, 32.0F, -24.0F, 72.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0043_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 32.0F, -24.0F, 72.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0043_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(72.0F, 32.0F, -24.0F, 72.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0044", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-136.0F, 16.0F, 8.0F, 56.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0045", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-136.0F, 16.0F, 64.0F, 32.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0046", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-136.0F, 24.0F, 112.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0047_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-136.0F, 32.0F, -32.0F, 72.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0047_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-64.0F, 32.0F, -32.0F, 64.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0047_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 32.0F, -32.0F, 64.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0047_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(64.0F, 32.0F, -32.0F, 72.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0048", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-136.0F, 32.0F, 104.0F, 24.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0049", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-128.0F, 16.0F, 0.0F, 48.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0050_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-128.0F, 24.0F, -40.0F, 64.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0050_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-64.0F, 24.0F, -40.0F, 64.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0050_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 24.0F, -40.0F, 64.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0050_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(64.0F, 24.0F, -40.0F, 64.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0051", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-128.0F, 32.0F, -40.0F, 72.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0052", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-120.0F, 16.0F, -8.0F, 40.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0053_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-120.0F, 24.0F, -48.0F, 120.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0053_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 24.0F, -48.0F, 120.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0054", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-112.0F, 32.0F, -48.0F, 72.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0055_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-112.0F, 40.0F, 8.0F, 56.0F, 8.0F, 24.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0055_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-56.0F, 40.0F, 8.0F, 56.0F, 8.0F, 24.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0055_2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 40.0F, 8.0F, 56.0F, 8.0F, 24.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0055_3", CubeListBuilder.create()
                .texOffs(0, 0).addBox(56.0F, 40.0F, 8.0F, 56.0F, 8.0F, 24.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0056", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-112.0F, 40.0F, 32.0F, 104.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0057", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-112.0F, 40.0F, 40.0F, 88.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0058", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-112.0F, 40.0F, 48.0F, 40.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0059_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-104.0F, 16.0F, -16.0F, 104.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0059_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 16.0F, -16.0F, 104.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0060_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-104.0F, 24.0F, -56.0F, 104.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0060_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 24.0F, -56.0F, 104.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0061", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-104.0F, 32.0F, -56.0F, 48.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0062_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-104.0F, 40.0F, -16.0F, 104.0F, 8.0F, 24.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0062_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 40.0F, -16.0F, 96.0F, 8.0F, 24.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0063", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-104.0F, 40.0F, 56.0F, 32.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0064_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-96.0F, 16.0F, -24.0F, 96.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0064_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 16.0F, -24.0F, 96.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0065_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-96.0F, 24.0F, -64.0F, 96.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0065_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 24.0F, -64.0F, 96.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0066", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-96.0F, 32.0F, -64.0F, 56.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0067_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-96.0F, 40.0F, -24.0F, 96.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0067_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 40.0F, -24.0F, 96.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0068_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-88.0F, 16.0F, -32.0F, 88.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0068_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 16.0F, -32.0F, 88.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0069_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-88.0F, 40.0F, -40.0F, 88.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0069_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 40.0F, -40.0F, 88.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0070", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-80.0F, 16.0F, -40.0F, 24.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0071_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-80.0F, 24.0F, -72.0F, 80.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0071_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 24.0F, -72.0F, 80.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0072", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-80.0F, 32.0F, -72.0F, 24.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0073_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-80.0F, 40.0F, -48.0F, 80.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0073_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 40.0F, -48.0F, 80.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0074", CubeListBuilder.create()
                .texOffs(0, 208).addBox(-72.0F, 0.0F, -8.0F, 16.0F, 24.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0075", CubeListBuilder.create()
                .texOffs(128, 144).addBox(-72.0F, 0.0F, 8.0F, 16.0F, 16.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0076", CubeListBuilder.create()
                .texOffs(128, 144).addBox(-72.0F, 8.0F, -16.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0077", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-72.0F, 16.0F, -48.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0078", CubeListBuilder.create()
                .texOffs(128, 144).addBox(-72.0F, 16.0F, 8.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0079", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-72.0F, 40.0F, -56.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0080", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-64.0F, 16.0F, -56.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0081_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-64.0F, 24.0F, -80.0F, 64.0F, 16.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0081_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0F, 24.0F, -80.0F, 64.0F, 16.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0082", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-64.0F, 40.0F, -64.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0083", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-56.0F, 24.0F, -88.0F, 112.0F, 16.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0084_0", CubeListBuilder.create()
                .texOffs(448, 0).addBox(-56.0F, 40.0F, -72.0F, 24.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0084_1", CubeListBuilder.create()
                .texOffs(448, 0).addBox(-32.0F, 40.0F, -72.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0084_2", CubeListBuilder.create()
                .texOffs(448, 0).addBox(-16.0F, 40.0F, -72.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0084_3", CubeListBuilder.create()
                .texOffs(448, 0).addBox(0.0F, 40.0F, -72.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0084_4", CubeListBuilder.create()
                .texOffs(448, 0).addBox(16.0F, 40.0F, -72.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0084_5", CubeListBuilder.create()
                .texOffs(448, 0).addBox(32.0F, 40.0F, -72.0F, 24.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0085", CubeListBuilder.create()
                .texOffs(448, 0).addBox(-48.0F, 16.0F, -56.0F, 8.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0086", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-48.0F, 16.0F, -8.0F, 96.0F, 8.0F, 32.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0087", CubeListBuilder.create()
                .texOffs(0, 144).addBox(-48.0F, 32.0F, 48.0F, 24.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0088", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-48.0F, 40.0F, -80.0F, 96.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0089_0", CubeListBuilder.create()
                .texOffs(448, 0).addBox(-48.0F, 48.0F, -32.0F, 16.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0089_1", CubeListBuilder.create()
                .texOffs(448, 0).addBox(-32.0F, 48.0F, -32.0F, 8.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0089_2", CubeListBuilder.create()
                .texOffs(448, 0).addBox(-48.0F, 48.0F, -16.0F, 16.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0089_3", CubeListBuilder.create()
                .texOffs(448, 0).addBox(-32.0F, 48.0F, -16.0F, 8.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0089_4", CubeListBuilder.create()
                .texOffs(448, 0).addBox(-24.0F, 48.0F, -32.0F, 8.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0089_5", CubeListBuilder.create()
                .texOffs(448, 0).addBox(-16.0F, 48.0F, -32.0F, 16.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0089_6", CubeListBuilder.create()
                .texOffs(448, 0).addBox(-24.0F, 48.0F, -16.0F, 8.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0089_7", CubeListBuilder.create()
                .texOffs(448, 0).addBox(-16.0F, 48.0F, -16.0F, 16.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0089_8", CubeListBuilder.create()
                .texOffs(448, 0).addBox(0.0F, 48.0F, -32.0F, 16.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0089_9", CubeListBuilder.create()
                .texOffs(448, 0).addBox(16.0F, 48.0F, -32.0F, 8.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0089_10", CubeListBuilder.create()
                .texOffs(448, 0).addBox(0.0F, 48.0F, -16.0F, 16.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0089_11", CubeListBuilder.create()
                .texOffs(448, 0).addBox(16.0F, 48.0F, -16.0F, 8.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0089_12", CubeListBuilder.create()
                .texOffs(448, 0).addBox(24.0F, 48.0F, -32.0F, 8.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0089_13", CubeListBuilder.create()
                .texOffs(448, 0).addBox(32.0F, 48.0F, -32.0F, 16.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0089_14", CubeListBuilder.create()
                .texOffs(448, 0).addBox(24.0F, 48.0F, -16.0F, 8.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0089_15", CubeListBuilder.create()
                .texOffs(448, 0).addBox(32.0F, 48.0F, -16.0F, 16.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0090", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-40.0F, 16.0F, -72.0F, 80.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0091_0", CubeListBuilder.create()
                .texOffs(448, 0).addBox(-40.0F, 16.0F, -48.0F, 24.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0091_1", CubeListBuilder.create()
                .texOffs(448, 0).addBox(-16.0F, 16.0F, -48.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0091_2", CubeListBuilder.create()
                .texOffs(448, 0).addBox(0.0F, 16.0F, -48.0F, 24.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0091_3", CubeListBuilder.create()
                .texOffs(448, 0).addBox(24.0F, 16.0F, -48.0F, 24.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0092_0", CubeListBuilder.create()
                .texOffs(448, 0).addBox(-40.0F, 16.0F, -40.0F, 24.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0092_1", CubeListBuilder.create()
                .texOffs(448, 0).addBox(-16.0F, 16.0F, -40.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0092_2", CubeListBuilder.create()
                .texOffs(448, 0).addBox(0.0F, 16.0F, -40.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0092_3", CubeListBuilder.create()
                .texOffs(448, 0).addBox(16.0F, 16.0F, -40.0F, 24.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0093", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-40.0F, 24.0F, -96.0F, 80.0F, 16.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0094", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-40.0F, 40.0F, -88.0F, 80.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0095_0", CubeListBuilder.create()
                .texOffs(448, 0).addBox(-40.0F, 40.0F, -64.0F, 24.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0095_1", CubeListBuilder.create()
                .texOffs(448, 0).addBox(-16.0F, 40.0F, -64.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0095_2", CubeListBuilder.create()
                .texOffs(448, 0).addBox(0.0F, 40.0F, -64.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0095_3", CubeListBuilder.create()
                .texOffs(448, 0).addBox(16.0F, 40.0F, -64.0F, 24.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0096", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-40.0F, 48.0F, 0.0F, 80.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0097", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-32.0F, 16.0F, -80.0F, 24.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0098", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-32.0F, 16.0F, -56.0F, 64.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0099", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-32.0F, 32.0F, -104.0F, 64.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0100_0", CubeListBuilder.create()
                .texOffs(448, 0).addBox(-32.0F, 32.0F, -48.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0100_1", CubeListBuilder.create()
                .texOffs(448, 0).addBox(-16.0F, 32.0F, -48.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0100_2", CubeListBuilder.create()
                .texOffs(448, 0).addBox(0.0F, 32.0F, -48.0F, 24.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0101", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-32.0F, 40.0F, -96.0F, 64.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0102", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-24.0F, 24.0F, -104.0F, 48.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0103", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-24.0F, 32.0F, -72.0F, 48.0F, 8.0F, 24.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0104", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-24.0F, 32.0F, -40.0F, 48.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0105", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-24.0F, 40.0F, -56.0F, 48.0F, 16.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0106", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-24.0F, 48.0F, -72.0F, 48.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0107", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-24.0F, 48.0F, -48.0F, 48.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0108", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-16.0F, 16.0F, -88.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0109", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-16.0F, 24.0F, -112.0F, 32.0F, 16.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0110", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-16.0F, 40.0F, -104.0F, 32.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0111", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-16.0F, 48.0F, -80.0F, 32.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0112", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-16.0F, 56.0F, -64.0F, 32.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0113", CubeListBuilder.create()
                .texOffs(0, 208).addBox(-8.0F, 0.0F, -72.0F, 16.0F, 16.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0114", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-8.0F, 32.0F, -120.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0115", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-8.0F, 40.0F, -112.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0116", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-8.0F, 48.0F, -88.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0117", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-8.0F, 56.0F, -72.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0118", CubeListBuilder.create()
                .texOffs(0, 0).addBox(8.0F, 16.0F, -88.0F, 8.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0119_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(8.0F, 32.0F, 40.0F, 104.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0119_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(112.0F, 32.0F, 40.0F, 96.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0120", CubeListBuilder.create()
                .texOffs(0, 0).addBox(8.0F, 40.0F, 32.0F, 104.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0121", CubeListBuilder.create()
                .texOffs(0, 0).addBox(16.0F, 16.0F, -80.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0122_0", CubeListBuilder.create()
                .texOffs(256, 0).addBox(16.0F, 16.0F, 24.0F, 40.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0122_1", CubeListBuilder.create()
                .texOffs(256, 0).addBox(56.0F, 16.0F, 24.0F, 40.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0122_2", CubeListBuilder.create()
                .texOffs(256, 0).addBox(96.0F, 16.0F, 24.0F, 32.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0122_3", CubeListBuilder.create()
                .texOffs(256, 0).addBox(128.0F, 16.0F, 24.0F, 40.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0123_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(16.0F, 24.0F, 40.0F, 120.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0123_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(136.0F, 24.0F, 40.0F, 120.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0124_0", CubeListBuilder.create()
                .texOffs(0, 144).addBox(24.0F, 24.0F, 48.0F, 56.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0124_1", CubeListBuilder.create()
                .texOffs(0, 144).addBox(80.0F, 24.0F, 48.0F, 32.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0124_2", CubeListBuilder.create()
                .texOffs(0, 144).addBox(112.0F, 24.0F, 48.0F, 32.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0124_3", CubeListBuilder.create()
                .texOffs(0, 144).addBox(144.0F, 24.0F, 48.0F, 32.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0124_4", CubeListBuilder.create()
                .texOffs(0, 144).addBox(176.0F, 24.0F, 48.0F, 32.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0124_5", CubeListBuilder.create()
                .texOffs(0, 144).addBox(208.0F, 24.0F, 48.0F, 56.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0125", CubeListBuilder.create()
                .texOffs(0, 144).addBox(24.0F, 32.0F, 48.0F, 24.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0126", CubeListBuilder.create()
                .texOffs(0, 0).addBox(24.0F, 40.0F, 40.0F, 88.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0127_0", CubeListBuilder.create()
                .texOffs(128, 144).addBox(32.0F, 32.0F, -48.0F, 40.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0127_1", CubeListBuilder.create()
                .texOffs(128, 144).addBox(72.0F, 32.0F, -48.0F, 40.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0128", CubeListBuilder.create()
                .texOffs(448, 0).addBox(40.0F, 16.0F, -56.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0129_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(40.0F, 16.0F, 32.0F, 72.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0129_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(112.0F, 16.0F, 32.0F, 72.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0130_0", CubeListBuilder.create()
                .texOffs(448, 0).addBox(40.0F, 32.0F, -64.0F, 24.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0130_1", CubeListBuilder.create()
                .texOffs(448, 0).addBox(64.0F, 32.0F, -64.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0130_2", CubeListBuilder.create()
                .texOffs(448, 0).addBox(80.0F, 32.0F, -64.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0131_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(48.0F, 16.0F, 40.0F, 72.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0131_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(120.0F, 16.0F, 40.0F, 72.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0132", CubeListBuilder.create()
                .texOffs(0, 208).addBox(56.0F, 0.0F, -8.0F, 16.0F, 24.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0133_0", CubeListBuilder.create()
                .texOffs(0, 208).addBox(56.0F, 0.0F, 8.0F, 8.0F, 16.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0133_1", CubeListBuilder.create()
                .texOffs(0, 208).addBox(64.0F, 0.0F, 8.0F, 8.0F, 16.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0134", CubeListBuilder.create()
                .texOffs(128, 144).addBox(56.0F, 8.0F, -16.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0135", CubeListBuilder.create()
                .texOffs(0, 0).addBox(56.0F, 16.0F, -56.0F, 8.0F, 8.0F, 24.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0136", CubeListBuilder.create()
                .texOffs(256, 144).addBox(56.0F, 16.0F, 8.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0137_0", CubeListBuilder.create()
                .texOffs(0, 144).addBox(56.0F, 24.0F, 56.0F, 24.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0137_1", CubeListBuilder.create()
                .texOffs(0, 144).addBox(80.0F, 24.0F, 56.0F, 32.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0137_2", CubeListBuilder.create()
                .texOffs(0, 144).addBox(112.0F, 24.0F, 56.0F, 32.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0137_3", CubeListBuilder.create()
                .texOffs(0, 144).addBox(144.0F, 24.0F, 56.0F, 24.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0137_4", CubeListBuilder.create()
                .texOffs(0, 144).addBox(168.0F, 24.0F, 56.0F, 24.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0137_5", CubeListBuilder.create()
                .texOffs(0, 144).addBox(192.0F, 24.0F, 56.0F, 32.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0137_6", CubeListBuilder.create()
                .texOffs(0, 144).addBox(224.0F, 24.0F, 56.0F, 32.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0137_7", CubeListBuilder.create()
                .texOffs(0, 144).addBox(256.0F, 24.0F, 56.0F, 24.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0138", CubeListBuilder.create()
                .texOffs(0, 0).addBox(56.0F, 32.0F, -72.0F, 24.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0139", CubeListBuilder.create()
                .texOffs(0, 0).addBox(56.0F, 32.0F, -56.0F, 48.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0140", CubeListBuilder.create()
                .texOffs(0, 0).addBox(56.0F, 32.0F, -40.0F, 72.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0141_0", CubeListBuilder.create()
                .texOffs(0, 144).addBox(56.0F, 32.0F, 48.0F, 40.0F, 8.0F, 24.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0141_1", CubeListBuilder.create()
                .texOffs(0, 144).addBox(96.0F, 32.0F, 48.0F, 32.0F, 8.0F, 24.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0141_2", CubeListBuilder.create()
                .texOffs(0, 144).addBox(128.0F, 32.0F, 48.0F, 40.0F, 8.0F, 24.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0141_3", CubeListBuilder.create()
                .texOffs(0, 144).addBox(168.0F, 32.0F, 48.0F, 40.0F, 8.0F, 24.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0142", CubeListBuilder.create()
                .texOffs(0, 0).addBox(56.0F, 40.0F, -64.0F, 8.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0143", CubeListBuilder.create()
                .texOffs(0, 0).addBox(64.0F, 16.0F, -48.0F, 8.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0144", CubeListBuilder.create()
                .texOffs(0, 0).addBox(64.0F, 40.0F, -56.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0145", CubeListBuilder.create()
                .texOffs(0, 0).addBox(72.0F, 16.0F, -40.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0146", CubeListBuilder.create()
                .texOffs(0, 0).addBox(72.0F, 16.0F, 48.0F, 104.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0147_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(72.0F, 24.0F, 72.0F, 112.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0147_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(184.0F, 24.0F, 72.0F, 112.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0148_0", CubeListBuilder.create()
                .texOffs(0, 0).addBox(72.0F, 24.0F, 80.0F, 104.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0148_1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(176.0F, 24.0F, 80.0F, 112.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0149", CubeListBuilder.create()
                .texOffs(0, 0).addBox(72.0F, 32.0F, 72.0F, 104.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0150", CubeListBuilder.create()
                .texOffs(0, 0).addBox(72.0F, 32.0F, 80.0F, 96.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0151", CubeListBuilder.create()
                .texOffs(0, 0).addBox(72.0F, 40.0F, 48.0F, 32.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0152_0", CubeListBuilder.create()
                .texOffs(256, 144).addBox(80.0F, 16.0F, -8.0F, 16.0F, 8.0F, 32.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0152_1", CubeListBuilder.create()
                .texOffs(256, 144).addBox(96.0F, 16.0F, -8.0F, 24.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0152_2", CubeListBuilder.create()
                .texOffs(256, 144).addBox(96.0F, 16.0F, 8.0F, 24.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0153", CubeListBuilder.create()
                .texOffs(0, 0).addBox(88.0F, 16.0F, 56.0F, 72.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0154", CubeListBuilder.create()
                .texOffs(0, 0).addBox(88.0F, 24.0F, 88.0F, 80.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0155", CubeListBuilder.create()
                .texOffs(0, 0).addBox(88.0F, 32.0F, 88.0F, 72.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0156", CubeListBuilder.create()
                .texOffs(0, 0).addBox(96.0F, 24.0F, 96.0F, 64.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0157", CubeListBuilder.create()
                .texOffs(0, 0).addBox(96.0F, 32.0F, 96.0F, 56.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0158", CubeListBuilder.create()
                .texOffs(0, 0).addBox(96.0F, 40.0F, -8.0F, 8.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0159", CubeListBuilder.create()
                .texOffs(0, 0).addBox(104.0F, 16.0F, 64.0F, 32.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0160", CubeListBuilder.create()
                .texOffs(0, 0).addBox(112.0F, 24.0F, 104.0F, 32.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0161", CubeListBuilder.create()
                .texOffs(0, 0).addBox(112.0F, 32.0F, 104.0F, 24.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0162", CubeListBuilder.create()
                .texOffs(0, 0).addBox(120.0F, 16.0F, 0.0F, 8.0F, 8.0F, 24.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0163", CubeListBuilder.create()
                .texOffs(0, 0).addBox(120.0F, 24.0F, 112.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0164", CubeListBuilder.create()
                .texOffs(0, 0).addBox(128.0F, 16.0F, 8.0F, 8.0F, 8.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0165", CubeListBuilder.create()
                .texOffs(0, 0).addBox(136.0F, 16.0F, 16.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0166", CubeListBuilder.create()
                .texOffs(0, 0).addBox(184.0F, 16.0F, 48.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0167", CubeListBuilder.create()
                .texOffs(0, 0).addBox(184.0F, 32.0F, 72.0F, 24.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0168", CubeListBuilder.create()
                .texOffs(0, 0).addBox(192.0F, 24.0F, 88.0F, 80.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0169", CubeListBuilder.create()
                .texOffs(0, 0).addBox(200.0F, 16.0F, 56.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0170", CubeListBuilder.create()
                .texOffs(0, 0).addBox(200.0F, 24.0F, 96.0F, 64.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0171", CubeListBuilder.create()
                .texOffs(0, 0).addBox(216.0F, 24.0F, 104.0F, 32.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0172", CubeListBuilder.create()
                .texOffs(0, 0).addBox(224.0F, 24.0F, 112.0F, 16.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("box_0173", CubeListBuilder.create()
                .texOffs(0, 0).addBox(280.0F, 24.0F, 64.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    @Override
    public void setupAnim(JetEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // A flying wing has no articulated surfaces: the whole body banks with the entity.
    }

    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack pose,
                               com.mojang.blaze3d.vertex.VertexConsumer buffer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        root.render(pose, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
