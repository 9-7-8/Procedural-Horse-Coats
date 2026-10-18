package com.example.horsegenetics.neoforge.mixin;

import com.example.horsegenetics.neoforge.client.crawl.LayoutCapture;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * <b>Dev-only: what a screen draws, for the menu crawler's layout audit.</b> Every string
 * of GUI text, every filled box and every textured blit passes through one of these four
 * methods; while {@link LayoutCapture#recording} is on (one frame, when a crawl asks for it)
 * each call is written down with the transform and the scissor it was drawn under. Off, the
 * cost is one static boolean read per call.
 *
 * <p>HEAD injections that change nothing: the draw goes ahead exactly as it would have.
 * The target signatures were read off the 26.1.2 sources (GuiGraphicsExtractor); a rename
 * fails the mod load loudly, since every injection here is required.
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiCaptureMixin {

    @Shadow
    @Final
    private Matrix3x2fStack pose;

    @Inject(method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",
            at = @At("HEAD"))
    private void horsegenetics$captureText(Font font, FormattedCharSequence str, int x, int y, int color,
                                           boolean dropShadow, CallbackInfo ci) {
        if (LayoutCapture.recording) {
            LayoutCapture.text(font, str, x, y, color, pose);
        }
    }

    @Inject(method = "innerFill", at = @At("HEAD"))
    private void horsegenetics$captureFill(RenderPipeline pipeline, TextureSetup textureSetup, int x0, int y0,
                                           int x1, int y1, int color1, Integer color2, CallbackInfo ci) {
        if (LayoutCapture.recording) {
            LayoutCapture.fill(x0, y0, x1, y1, color1, pose);
        }
    }

    @Inject(method = "innerBlit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lcom/mojang/blaze3d/textures/GpuTextureView;"
            + "Lcom/mojang/blaze3d/textures/GpuSampler;IIIIFFFFI)V", at = @At("HEAD"))
    private void horsegenetics$captureBlit(RenderPipeline pipeline, GpuTextureView view, GpuSampler sampler,
                                           int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1,
                                           int color, CallbackInfo ci) {
        if (LayoutCapture.recording) {
            LayoutCapture.blit(x0, y0, x1, y1, pose);
        }
    }

    @Inject(method = "enableScissor", at = @At("HEAD"))
    private void horsegenetics$captureScissorOn(int x0, int y0, int x1, int y1, CallbackInfo ci) {
        if (LayoutCapture.recording) {
            LayoutCapture.scissorOn(x0, y0, x1, y1, pose);
        }
    }

    @Inject(method = "disableScissor", at = @At("HEAD"))
    private void horsegenetics$captureScissorOff(CallbackInfo ci) {
        if (LayoutCapture.recording) {
            LayoutCapture.scissorOff();
        }
    }
}
