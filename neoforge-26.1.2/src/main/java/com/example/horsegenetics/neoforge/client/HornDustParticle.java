package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.particle.HornDustOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.DustColorTransitionParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.util.RandomSource;

/**
 * <b>Horn dust, falling.</b> Vanilla's colour-fading dust - same sprites, same fade
 * from one colour to the other - with two changes: it has <b>gravity</b>, and it
 * lives long enough to fall from a horn most of the way to the ground.
 *
 * <p>Vanilla's dust cannot simply be sent with a downward velocity: the particle
 * randomises its own direction on spawn and then multiplies its speed by a tenth,
 * so whatever direction it is sent in, it hangs where it appears. Gravity is
 * applied every tick by {@code Particle.tick}, which the dust base class does not
 * override, so setting it is the whole change.
 *
 * <p>Tuned by arithmetic, not by eye: with the base class's 0.96 friction a
 * gravity of 0.06 settles at about 0.06 blocks a tick, and 30-50 ticks of that is
 * a block and a half to two and a half - roughly horn to hoof. <i>Unverified in
 * game.</i>
 */
public final class HornDustParticle extends DustColorTransitionParticle {

    private static final float GRAVITY = 0.06F;
    private static final int MIN_LIFETIME = 30;
    private static final int LIFETIME_SPREAD = 20;

    private HornDustParticle(ClientLevel level, double x, double y, double z,
                             DustColorTransitionOptions options, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0, options, sprites);
        this.gravity = GRAVITY;
        this.lifetime = MIN_LIFETIME + this.random.nextInt(LIFETIME_SPREAD);
    }

    /** Builds the particle from the mod's options, through vanilla's own. */
    public static final class Provider implements ParticleProvider<HornDustOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(HornDustOptions options, ClientLevel level,
                                       double x, double y, double z,
                                       double xAux, double yAux, double zAux, RandomSource random) {
            return new HornDustParticle(level, x, y, z,
                    new DustColorTransitionOptions(options.fromColor(), options.toColor(), 0.6F),
                    sprites);
        }
    }
}
