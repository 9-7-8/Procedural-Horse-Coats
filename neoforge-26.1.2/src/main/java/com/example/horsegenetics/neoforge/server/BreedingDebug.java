package com.example.horsegenetics.neoforge.server;

import com.example.horsegenetics.neoforge.ServerConfig;
import com.example.horsegenetics.common.genetics.BreedingReport;
import com.example.horsegenetics.common.genetics.GeneCodeDisplay;
import com.example.horsegenetics.common.genetics.Genome;
import com.example.horsegenetics.common.horse.HorseRecord;
import com.example.horsegenetics.common.trait.Condition;
import com.example.horsegenetics.common.trait.Traits;
import com.example.horsegenetics.common.trait.Viability;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * <b>The dev build's account of a breeding.</b> Every locus, what each parent
 * held, what the foal drew, and what the foal's body came out at.
 *
 * <h2>Why a draw needs a witness</h2>
 * A Mendelian draw is invisible. The odds, sex linkage, the epigenetic copy that
 * travelled with an allele, a breeding carrot's bias - all of it resolves inside
 * one call, and the only evidence afterwards is a single foal, which is one
 * sample of a distribution. "Every foal came out chestnut" and "five foals
 * happened to come out chestnut" look identical from the paddock. These lines
 * are the other half of that, and they are why a suspicion about a gene can be
 * settled in one session instead of over a week of foals.
 *
 * <h2>Two channels, two audiences</h2>
 * <ul>
 *   <li>The <b>log</b> gets everything: one line per registered gene, always,
 *       whenever {@link DebugAnnounce} is on. It is too wide and too long to
 *       read in a chat box and it is exactly what you want in a pasted log.</li>
 *   <li><b>Chat</b> gets a summary and the notable loci - the ones the foal
 *       expresses or landed on a combination neither parent had - and only
 *       where {@link ServerConfig#debugTools()} is on: a dev run by default,
 *       or a server the owner switched it on for. Nobody playing a real world
 *       wants sixty lines per foal.</li>
 * </ul>
 *
 * <p><b>Nothing here is on the production path except the miscarriage line</b>,
 * which is not here at all - it is in {@link LethalFoalHandler}, because it is
 * a gameplay event rather than a diagnostic.
 */
public final class BreedingDebug {

    /** Beyond this many notable loci, chat gets a count instead of the list. */
    private static final int CHAT_LINE_BUDGET = 12;

    private BreedingDebug() {
    }

    /**
     * Report one draw. Safe to call for a pairing that is about to be refused -
     * it is called before the viability check on purpose, because a miscarriage
     * is the case where this log is the only record that anything happened.
     */
    public static void reportDraw(HorseRecord damRecord, HorseRecord sireRecord,
                                  Genome dam, Genome sire, Genome foal,
                                  Traits foalTraits, @Nullable Player breeder) {
        if (!DebugAnnounce.enabled()) {
            return;
        }
        String heading = damRecord.displayName() + " x " + sireRecord.displayName();

        DebugAnnounce.log("Breeding", "--- " + heading + " ---");
        DebugAnnounce.log("Breeding", "dam   " + GeneCodeDisplay.shortForm(dam.genotype()));
        DebugAnnounce.log("Breeding", "sire  " + GeneCodeDisplay.shortForm(sire.genotype()));
        DebugAnnounce.log("Breeding", "foal  " + GeneCodeDisplay.shortForm(foal.genotype()));
        DebugAnnounce.log("Breeding", "foal code " + foal.genotype().toCode());
        DebugAnnounce.log("Breeding", String.format(
                "foal body: speed %.4f  health %.2f  jump %.3f  size %.3f  viability %s",
                foalTraits.speed(), foalTraits.health(), foalTraits.jump(), foalTraits.scale(),
                foalTraits.viability()));
        for (Condition condition : foalTraits.conditions()) {
            DebugAnnounce.log("Breeding", "condition: " + condition.name()
                    + " [" + condition.severity() + "]");
        }
        for (String line : BreedingReport.full(dam.genotype(), sire.genotype(), foal.genotype())) {
            DebugAnnounce.log("Breeding", "  " + line);
        }

        if (!ServerConfig.debugTools() || breeder == null) {
            return;
        }
        List<String> notable = BreedingReport.notable(dam.genotype(), sire.genotype(), foal.genotype());
        tell(breeder, heading, ChatFormatting.WHITE);
        tell(breeder, String.format("  body: speed %.4f  health %.1f  jump %.2f  size %.2f",
                foalTraits.speed(), foalTraits.health(), foalTraits.jump(), foalTraits.scale()),
                ChatFormatting.GRAY);
        if (foalTraits.viability() != Viability.VIABLE) {
            tell(breeder, "  viability: " + foalTraits.viability(), ChatFormatting.RED);
        }
        if (notable.size() > CHAT_LINE_BUDGET) {
            tell(breeder, "  " + notable.size() + " loci of interest - see the log for all of them",
                    ChatFormatting.DARK_GRAY);
            return;
        }
        for (String line : notable) {
            tell(breeder, "  " + line, ChatFormatting.DARK_GRAY);
        }
    }

    private static void tell(Player breeder, String message, ChatFormatting colour) {
        breeder.sendSystemMessage(Component.literal("[Breeding] ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(message).withStyle(colour)));
    }
}
