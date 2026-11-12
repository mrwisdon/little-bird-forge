package io.github.littlebird.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.littlebird.LittleBird;
import io.github.littlebird.network.LittleBirdTracerPacket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Client-only tracer bullets: each round is drawn as a short streak flying from
 * the muzzle to its impact point, with a muzzle flash at the start and impact
 * sparks on arrival. Nothing is drawn for the aim prediction - that stays a HUD
 * reticle.
 */
@Mod.EventBusSubscriber(modid = LittleBird.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class LittleBirdTracerRenderer {
    private static final float CORE_HALF_WIDTH = 0.014F;
    private static final float GLOW_HALF_WIDTH = 0.032F;
    private static final List<Tracer> TRACERS = new ArrayList<>();
    private static ClientLevel activeLevel;

    private LittleBirdTracerRenderer() {
    }

    /** Called from the tracer packet on the client thread. */
    public static void add(LittleBirdTracerPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            clear();
            return;
        }
        ensureLevel(level);
        Vec3 start = new Vec3(packet.startX(), packet.startY(), packet.startZ());
        Vec3 end = new Vec3(packet.endX(), packet.endY(), packet.endZ());
        Vec3 segment = end.subtract(start);
        double length = segment.length();
        if (length < 1.0E-6D) {
            return;
        }
        Vec3 direction = segment.scale(1.0D / length);
        TRACERS.add(new Tracer(start, end, direction, length, packet.impactType(),
                packet.station(), currentTime(level, minecraft.getFrameTime())));
        if (packet.station() != LittleBirdTracerPacket.STATION_CANISTER) {
            level.addParticle(ParticleTypes.SMOKE, start.x, start.y, start.z,
                    -direction.x * 0.01D, 0.012D, -direction.z * 0.01D);
        }
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            clear();
            return;
        }
        ensureLevel(level);
        double now = currentTime(level, minecraft.getFrameTime());
        Iterator<Tracer> iterator = TRACERS.iterator();
        while (iterator.hasNext()) {
            Tracer tracer = iterator.next();
            double elapsed = Math.max(0.0D, now - tracer.spawnTime);
            if (!tracer.renderedOnce && LittleBirdTracerTimeline.isStaleWithoutRender(elapsed)) {
                iterator.remove();
                continue;
            }
            if (tracer.renderedOnce && !tracer.impactEmitted
                    && LittleBirdTracerTimeline.hasArrived(elapsed, tracer.length)) {
                emitImpact(level, tracer);
                tracer.impactEmitted = true;
            }
            if (tracer.impactEmitted
                    && LittleBirdTracerTimeline.isExpired(elapsed, tracer.length)) {
                iterator.remove();
            }
        }
    }

    private static void emitImpact(ClientLevel level, Tracer tracer) {
        if (tracer.impactType == LittleBirdTracerPacket.IMPACT_BLOCK) {
            BlockPos inside = BlockPos.containing(tracer.end.add(tracer.direction.scale(0.02D)));
            BlockState state = level.getBlockState(inside);
            if (!state.isAir()) {
                BlockParticleOption fragments = new BlockParticleOption(ParticleTypes.BLOCK, state);
                for (int i = 0; i < 3; i++) {
                    level.addParticle(fragments, tracer.end.x, tracer.end.y, tracer.end.z,
                            random(level, 0.1D), 0.03D + level.random.nextDouble() * 0.08D,
                            random(level, 0.1D));
                }
            }
            level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    tracer.end.x, tracer.end.y, tracer.end.z,
                    random(level, 0.07D), 0.03D, random(level, 0.07D));
        } else if (tracer.impactType == LittleBirdTracerPacket.IMPACT_ENTITY) {
            for (int i = 0; i < 2; i++) {
                level.addParticle(ParticleTypes.CRIT,
                        tracer.end.x, tracer.end.y, tracer.end.z,
                        random(level, 0.09D), random(level, 0.09D), random(level, 0.09D));
            }
        }
    }

    private static double random(ClientLevel level, double scale) {
        return (level.random.nextDouble() - 0.5D) * scale;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || TRACERS.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.level != activeLevel) {
            clear();
            return;
        }
        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = pose.last().pose();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer vertices = buffers.getBuffer(RenderType.lightning());
        double now = currentTime(minecraft.level, event.getPartialTick());
        for (Tracer tracer : TRACERS) {
            double elapsed = Math.max(0.0D, now - tracer.spawnTime);
            Vec3 reference = Math.abs(tracer.direction.y) < 0.95D
                    ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
            Vec3 side = tracer.direction.cross(reference).normalize();
            Vec3 up = side.cross(tracer.direction).normalize();
            drawMuzzleFlash(vertices, matrix, tracer, side, up, elapsed);
            double front = LittleBirdTracerTimeline.frontDistance(elapsed, tracer.length);
            double tail = LittleBirdTracerTimeline.tailDistance(front);
            if (front <= tail) {
                tracer.renderedOnce = true;
                continue;
            }
            Vec3 from = tracer.start.add(tracer.direction.scale(tail));
            Vec3 to = tracer.start.add(tracer.direction.scale(front));
            float fade = (float) LittleBirdTracerTimeline.arrivalFade(elapsed, tracer.length);
            int glow = tracer.station == 0 ? 0xFF9628 : 0xFF783C;
            float widthScale = tracer.station == LittleBirdTracerPacket.STATION_CANISTER ? 2.0F : 1.0F;
            float glowWidth = GLOW_HALF_WIDTH * widthScale;
            float coreWidth = CORE_HALF_WIDTH * widthScale;
            drawRibbon(vertices, matrix, from, to, side, glowWidth, glow, alpha(70, fade));
            drawRibbon(vertices, matrix, from, to, up, glowWidth, glow, alpha(70, fade));
            drawRibbon(vertices, matrix, from, to, side, coreWidth, 0xFFEEAA, alpha(235, fade));
            drawRibbon(vertices, matrix, from, to, up, coreWidth, 0xFFEEAA, alpha(235, fade));
            tracer.renderedOnce = true;
        }
        buffers.endBatch(RenderType.lightning());
        pose.popPose();
    }

    private static void drawMuzzleFlash(VertexConsumer vertices, Matrix4f matrix,
                                        Tracer tracer, Vec3 side, Vec3 up, double elapsed) {
        if (tracer.muzzleRendered || tracer.station == LittleBirdTracerPacket.STATION_CANISTER) {
            return;
        }
        float fade = (float) LittleBirdTracerTimeline.muzzleFlashFade(elapsed);
        if (fade > 0.0F) {
            Vec3 tip = tracer.start.add(tracer.direction.scale(0.18D));
            drawTapered(vertices, matrix, tracer.start, tip, side, 0.06F, 0.008F,
                    0xFFD23C, alpha(215, fade), alpha(40, fade));
            drawTapered(vertices, matrix, tracer.start, tip, up, 0.06F, 0.008F,
                    0xFFF0C0, alpha(225, fade), alpha(50, fade));
        }
        if (fade <= 0.0F) {
            tracer.muzzleRendered = true;
        }
    }

    private static void drawRibbon(VertexConsumer vertices, Matrix4f matrix, Vec3 from, Vec3 to,
                                   Vec3 normal, float halfWidth, int colour, int alpha) {
        Vec3 offset = normal.scale(halfWidth);
        int red = (colour >> 16) & 0xFF;
        int green = (colour >> 8) & 0xFF;
        int blue = colour & 0xFF;
        vertex(vertices, matrix, from.subtract(offset), red, green, blue, alpha);
        vertex(vertices, matrix, from.add(offset), red, green, blue, alpha);
        vertex(vertices, matrix, to.add(offset), red, green, blue, alpha);
        vertex(vertices, matrix, to.subtract(offset), red, green, blue, alpha);
        vertex(vertices, matrix, to.subtract(offset), red, green, blue, alpha);
        vertex(vertices, matrix, to.add(offset), red, green, blue, alpha);
        vertex(vertices, matrix, from.add(offset), red, green, blue, alpha);
        vertex(vertices, matrix, from.subtract(offset), red, green, blue, alpha);
    }

    private static void drawTapered(VertexConsumer vertices, Matrix4f matrix, Vec3 from, Vec3 to,
                                    Vec3 normal, float startWidth, float endWidth, int colour,
                                    int startAlpha, int endAlpha) {
        Vec3 startOffset = normal.scale(startWidth);
        Vec3 endOffset = normal.scale(endWidth);
        int red = (colour >> 16) & 0xFF;
        int green = (colour >> 8) & 0xFF;
        int blue = colour & 0xFF;
        vertex(vertices, matrix, from.subtract(startOffset), red, green, blue, startAlpha);
        vertex(vertices, matrix, from.add(startOffset), red, green, blue, startAlpha);
        vertex(vertices, matrix, to.add(endOffset), red, green, blue, endAlpha);
        vertex(vertices, matrix, to.subtract(endOffset), red, green, blue, endAlpha);
        vertex(vertices, matrix, to.subtract(endOffset), red, green, blue, endAlpha);
        vertex(vertices, matrix, to.add(endOffset), red, green, blue, endAlpha);
        vertex(vertices, matrix, from.add(startOffset), red, green, blue, startAlpha);
        vertex(vertices, matrix, from.subtract(startOffset), red, green, blue, startAlpha);
    }

    private static int alpha(int base, float fade) {
        return Math.max(0, Math.min(255, Math.round(base * fade)));
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, Vec3 point,
                               int red, int green, int blue, int alpha) {
        vertices.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, alpha)
                .endVertex();
    }

    private static double currentTime(ClientLevel level, float partialTick) {
        return level.getGameTime() + Math.max(0.0F, Math.min(1.0F, partialTick));
    }

    private static void ensureLevel(ClientLevel level) {
        if (activeLevel != level) {
            clear();
            activeLevel = level;
        }
    }

    private static void clear() {
        TRACERS.clear();
        activeLevel = null;
    }

    private static final class Tracer {
        private final Vec3 start;
        private final Vec3 end;
        private final Vec3 direction;
        private final double length;
        private final int impactType;
        private final int station;
        private final double spawnTime;
        private boolean renderedOnce;
        private boolean muzzleRendered;
        private boolean impactEmitted;

        private Tracer(Vec3 start, Vec3 end, Vec3 direction, double length,
                        int impactType, int station, double spawnTime) {
            this.start = start;
            this.end = end;
            this.direction = direction;
            this.length = length;
            this.impactType = impactType;
            this.station = station;
            this.spawnTime = spawnTime;
        }
    }
}