package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.particle.HoofprintOptions;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;

/**
 * <b>A glowing hoofprint lying flat on the ground</b>, toe pointing the way the
 * horse went, fading from one colour to another as it cools.
 *
 * <p>Three departures from an ordinary particle, each copied from a vanilla one
 * that already does it:
 * <ul>
 *   <li><b>Flat, not facing the camera</b> - {@code ShriekParticle} draws its
 *       quad at a fixed rotation instead of the camera's, and draws it twice,
 *       once per face, so it shows from either side. This is the same with the
 *       tilt at a right angle and the horse's heading added.</li>
 *   <li><b>Full brightness</b> - the block-light half of the light coordinates
 *       held at 15, as the shriek does, so a print glows in the dark. It does
 *       not light the ground around it; only a light block could, and a trail
 *       of those on a dominant gene is too many block updates.</li>
 *   <li><b>Still</b> - no velocity, no gravity, no physics. The four-argument
 *       constructor is used because the seven-argument one adds random drift.</li>
 * </ul>
 *
 * <p><b>Not verified in-game.</b> Written against the 26.1.2 sources; the
 * rotation maths is reasoned, not seen - if the toe points backwards, add
 * {@code PI} to {@link #heading}; if the print is invisible from above, the two
 * faces' pitches are the wrong way round.
 */
public final class HoofprintParticle extends SingleQuadParticle {

    /** Four seconds on the ground. */
    private static final int LIFETIME = 80;

    /** Half the print's width at scale 1 - a quarter of a block across, about a hoof. */
    private static final float HALF_SIZE = 0.13F;

    /** The share of its life a print holds full strength before it starts to fade. */
    private static final float HOLD = 0.35F;

    private final float r0;
    private final float g0;
    private final float b0;
    private final float r1;
    private final float g1;
    private final float b1;
    private final float heading;
    private final boolean glow;

    private HoofprintParticle(ClientLevel level, double x, double y, double z,
                              HoofprintOptions o, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.lifetime = LIFETIME;
        this.quadSize = HALF_SIZE * Math.max(0.3F, o.scale());
        this.r0 = ((o.fromColor() >> 16) & 0xFF) / 255.0F;
        this.g0 = ((o.fromColor() >> 8) & 0xFF) / 255.0F;
        this.b0 = (o.fromColor() & 0xFF) / 255.0F;
        this.r1 = ((o.toColor() >> 16) & 0xFF) / 255.0F;
        this.g1 = ((o.toColor() >> 8) & 0xFF) / 255.0F;
        this.b1 = (o.toColor() & 0xFF) / 255.0F;
        // Minecraft's yaw has the horse facing (-sin, 0, cos); the sprite's top
        // edge, once laid flat, points along -Z, and a Y rotation of PI - yaw
        // turns -Z onto that heading.
        this.heading = (float) (Math.PI - Math.toRadians(o.yaw()));
        this.glow = o.glow();
        this.setColor(r0, g0, b0);
    }

    @Override
    public void tick() {
        super.tick();
        float t = Mth.clamp((float) this.age / this.lifetime, 0.0F, 1.0F);
        this.setColor(Mth.lerp(t, r0, r1), Mth.lerp(t, g0, g1), Mth.lerp(t, b0, b1));
    }

    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTick) {
        float t = Mth.clamp((this.age + partialTick) / this.lifetime, 0.0F, 1.0F);
        this.alpha = t < HOLD ? 1.0F : 1.0F - (t - HOLD) / (1.0F - HOLD);
        // Upper face, then the lower one - the shriek's pair, laid flat.
        Quaternionf rotation = new Quaternionf().rotationYXZ(heading, (float) (-Math.PI / 2), 0.0F);
        this.extractRotatedQuad(state, camera, rotation, partialTick);
        rotation.rotationYXZ((float) (heading - Math.PI), (float) (Math.PI / 2), 0.0F);
        this.extractRotatedQuad(state, camera, rotation, partialTick);
    }

    /** Full block light for a glowing print; the world's own light for a black one. */
    @Override
    public int getLightCoords(float partialTick) {
        return glow ? LightCoordsUtil.withBlock(super.getLightCoords(partialTick), 15)
                : super.getLightCoords(partialTick);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    /** Registered in {@code ClientSetup} against {@code ModParticles.HOOFPRINT}. */
    public static final class Provider implements ParticleProvider<HoofprintOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(HoofprintOptions options, ClientLevel level, double x, double y, double z,
                                       double xAux, double yAux, double zAux, RandomSource random) {
            return new HoofprintParticle(level, x, y, z, options, sprites.get(random));
        }
    }
}
