package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.neoforge.HorseGenetics;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/**
 * Drops every generated coat on a client resource reload (F3+T, or switching
 * resource pack).
 *
 * <p>Every coat in the game is composed <i>from</i> pack resources - the two
 * white horse templates, the red/black gradient and every alternate LUT - and
 * {@link GeneticCoatTextureFactory} loads each of them once and then keeps both
 * the pixels and the baked result for the rest of the session. Without this
 * listener the mod had no reload path at all, so editing the gradient or a
 * template and pressing F3+T appeared to do <b>nothing</b>: the horses on
 * screen kept their baked textures and the factory kept the pixels of whichever
 * pack happened to be active when the first horse rendered.
 *
 * <p>The listener therefore just calls {@code clear()}. The next horse drawn
 * reloads the templates and recomposes, which is the same work a fresh login
 * does and is why it does not need to be any cleverer than this.
 *
 * <p>Registered in {@link ClientSetup} through
 * {@code AddClientReloadListenersEvent}, which by default sorts mod listeners
 * after every vanilla one - the ordering this wants, since the vanilla
 * {@code TextureManager} has finished its own reload by then.
 */
public final class CoatAssetReload implements ResourceManagerReloadListener {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(HorseGenetics.MOD_ID, "coat_textures");

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        GeneticCoatTextureFactory.clear();
    }
}
