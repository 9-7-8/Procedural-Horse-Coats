package com.example.horsegenetics.common.genetics;

import java.util.Map;

/**
 * A central table of one-to-three-sentence, human-readable summaries of every
 * built-in {@link Gene}, for the in-game gene browser and tooltips
 * ({@link Gene#description()} reads it).
 *
 * <p>This is the <i>glanceable</i> version only. The authoritative description
 * of each gene - alleles, generation function, wild frequency, dominance,
 * natural vs magical - is {@code wiki/gene-*.html}, and a per-combination
 * sentence lives on each {@link Expression#description()}. When a gene's
 * behaviour changes, update its wiki page; update the line here only if the
 * one-paragraph summary is now wrong.
 *
 * <p>Keyed by {@link Gene#key()}. A gene with no entry (every data-driven gene,
 * today) resolves to {@code ""} - callers treat that as "no summary available".
 */
public final class GeneDescriptions {

    private GeneDescriptions() {}

    private static final Map<String, String> BY_KEY = Map.ofEntries(
            Map.entry("horsegenetics.sex",
                    "The sex chromosome pair, modelled as an ordinary locus so a foal's sex is "
                            + "inherited rather than rolled: X/X is a mare, X/Y a stallion, and Y/Y "
                            + "cannot occur. It never touches the coat; it sits first so a future "
                            + "sex-linked gene can read an already-decided sex."),
            Map.entry("horsegenetics.dhampir",
                    "A magical recessive whose carrier is not silent: one copy gives red eyes with "
                            + "glowing whites and nothing else, which is what makes the locus worth "
                            + "hunting. Two copies give a white, red-eyed animal that burns in "
                            + "daylight and runs for shade or water, with three times the health, "
                            + "half again the speed and twice the jump - and which cannot be fed by "
                            + "any means. It heals only by biting something living."),
            Map.entry("horsegenetics.diet",
                    "What the horse will eat. Twelve narrow diets - raw meat, fish, wheat, cake, "
                            + "potions, lava, one metal, one gem and the rest - each of which needs "
                            + "two identical copies to show, against a wild type that eats ordinary "
                            + "horse feed. A narrow diet refuses almost everything, and what it does "
                            + "accept feeds it far better: a single gold bar takes a metal-eater to "
                            + "full health. No breed carries any of it."),
            Map.entry("horsegenetics.extension",
                    "Whether the horse can make black pigment at all. E lets black through; e/e "
                            + "removes it entirely, leaving only red - a chestnut - on which every "
                            + "gene that merely moves black pigment is invisible."),
            Map.entry("horsegenetics.agouti",
                    "Where black pigment is allowed to sit. a/a leaves black over the whole horse "
                            + "(plain black, or chestnut if extension already removed it); one A "
                            + "copy restricts black toward the points, making a bay. How far it is "
                            + "restricted is the shade locus's business, and that is what makes a "
                            + "bay a blood bay, an ordinary bay, a liver bay or a seal brown."),
            Map.entry("horsegenetics.shade",
                    "The regulatory region beside agouti that decides how far black spreads over a "
                            + "bay. It paints nothing on its own and every horse carries it - a "
                            + "chestnut can hand a seal brown to a grandfoal - but on a bay its "
                            + "dosage, plus whether extension is E/E and whether agouti is A/a, "
                            + "chooses between a bright blood bay and a near-black seal brown."),
            Map.entry("horsegenetics.flaxen",
                    "The pale mane and tail of a chestnut horse - from a few gold strands to a "
                            + "near-white fall of hair - as a dosage of two variant alleles rather "
                            + "than the simple recessive folklore models it as, because no flaxen "
                            + "mutation has ever been identified. It touches the long hair and "
                            + "nothing else, and it is invisible on any horse that makes black "
                            + "hair, which carries and transmits it regardless."),
            Map.entry("horsegenetics.sooty",
                    "The dark countershading that lies over a horse's topline, crest, shoulder and "
                            + "croup and makes it look darker than its base colour says - a muddy "
                            + "palomino, a smutty buckskin, a bay people keep calling black. It "
                            + "works by declining to remove pigment the rest of the coat was about "
                            + "to lose, so it shows most on the colours with the most to keep and "
                            + "does nothing at all on a plain black. A dosage of two variant "
                            + "alleles; no confirmed real-world inheritance exists."),
            Map.entry("horsegenetics.pangare",
                    "Mealy: the pale muzzle, eye rings, belly, flanks and inner legs of an Exmoor "
                            + "pony or a Fjord. It takes red pigment off the soft parts of the "
                            + "horse, which is why it shows brightest on a chestnut, spares a bay's "
                            + "black points entirely, and is invisible on a black. A dosage of two "
                            + "variant alleles; near-fixed in some pony and draft breeds and no "
                            + "confirmed mutation anywhere."),
            Map.entry("horsegenetics.rabicano",
                    "White ticking that starts at the tail dock and the flank and works forward - "
                            + "a frosted, banded coon tail, a scatter of white hairs over the rear "
                            + "barrel and belly, and broken vertical rib bars on the strongest "
                            + "ones. The allele is dominant but how much shows is a per-horse roll "
                            + "that can come out at almost nothing, so a horse recorded as solid "
                            + "can throw a heavily ticked foal. Not classic roan: the tail tells "
                            + "them apart."),
            Map.entry("horsegenetics.manchado",
                    "A rare Argentine pattern: broad clean white over the back, crest and croup "
                            + "with smooth rounded islands of the base colour left inside it, a "
                            + "mostly white tail, and a dark head, belly and lower legs. It takes "
                            + "two copies and no wild horse has them, so the only way to see one is "
                            + "to breed two carriers. Its own locus - it belongs to none of the "
                            + "mapped white-pattern genes, and its real cause is unknown."),
            Map.entry("horsegenetics.champagne",
                    "A dilution that ignores its own dose - one copy or two give the same result. "
                            + "It keeps most red, cuts black hard and feeds some removed black back "
                            + "as red, so it reads off the current colour: gold on chestnut, taupe "
                            + "on black, chocolate points over a gold body on bay. Invisible on white - "
                            + "except the eye, which it dilutes to anything from amber through "
                            + "hazel to a rare olive green, per horse and inherited with the allele."),
            Map.entry("horsegenetics.grey",
                    "Progressive dapple grey, adults only - it lightens the coat toward neutral "
                            + "without shifting hue. How far along the greying is, the dapple size "
                            + "and how long the mane, tail and legs hold colour all vary per horse "
                            + "and are fixed for life (there is no aging). A foal is born its base "
                            + "colour."),
            Map.entry("horsegenetics.matp",
                    "The cream / pearl locus - one gene, three alleles (Cr, prl, N). One cream copy "
                            + "is a partial dilution (palomino / buckskin / smoky black); two pearl "
                            + "is a milder uniform dilution; cream plus pearl, or two cream, is a "
                            + "full double dilute (cremello / perlino). A single pearl copy is an "
                            + "invisible carrier."),
            Map.entry("horsegenetics.magic_zebra",
                    "A magical (invented) gene: a zebra's stripe map painted in hard black - "
                            + "vertical bands off the spine, arcs round the hip, rings down the "
                            + "legs. It subtracts hard from every colour channel, so the stripes "
                            + "read black over any coat at all - cremello, chestnut, grey or "
                            + "dominant white - and their spacing, bend and leg reach vary per "
                            + "horse. The natural zebra locus draws the same map by taking pigment "
                            + "out of the gaps instead."),
            Map.entry("horsegenetics.brindle",
                    "The one X-linked locus. Irregular white streaks running down from the "
                            + "topline over the barrel, quarters and neck, turning crosswise on "
                            + "the upper legs, and deliberately not matching from one side of the "
                            + "horse to the other - they are a record of which X chromosome each "
                            + "patch of skin silenced. A stallion has one copy and can never be a "
                            + "carrier; a mare needs two, so brindle skips the male line for a "
                            + "generation and comes back through the mares."),
            Map.entry("horsegenetics.natural_zebra",
                    "Real zebra striping, done the way a zebra does it: the dark bands are the "
                            + "horse's own colour and the gaps have the pigment taken out of them, "
                            + "so a striped black is black and white and a striped chestnut is red "
                            + "and white. Codominant - one copy gives faint shadow stripes, two "
                            + "take the gaps to white. Vertical bands off the spine, arcs round the "
                            + "hip, rings down the legs, a dark dorsal stripe and muzzle, and a "
                            + "pale belly."),
            Map.entry("horsegenetics.pink_hair",
                    "A magical gene, and the clearest carrier locus in the mod: only two copies do "
                            + "anything, turning the mane and tail hot pink. The pink is blended "
                            + "onto the existing strand shading rather than painted flat. One copy "
                            + "is invisible and worth breeding toward."),
            Map.entry("horsegenetics.dun",
                    "Real-horse TBX3, three alleles with two dominance orders. D lightens the body "
                            + "coat while leaving the points dark - tan on a bay, blue-grey grullo "
                            + "on a black, pale red on a chestnut - and adds primitive markings: a "
                            + "dorsal stripe into the tail, and, on some horses, leg bars, a "
                            + "shoulder bar and a face mask. d1 adds the dorsal stripe with no "
                            + "dilution at all; d2 does neither. So a horse can carry primitive "
                            + "markings without being a dun."),
            Map.entry("horsegenetics.silver",
                    "Silver dapple (PMEL17): dilutes black pigment only, toward chocolate on the "
                            + "body and near-flaxen on the mane and tail, while leaving red "
                            + "untouched. A black becomes chocolate with a pale mane, a bay becomes "
                            + "silver bay, and a chestnut carrying it looks unchanged - it has no "
                            + "black to act on."),
            Map.entry("horsegenetics.mushroom",
                    "The mirror of silver: dilutes red pigment only, and only with two copies, "
                            + "walking a chestnut's red body toward a flat sepia. A black or bay "
                            + "horse carries it almost invisibly, having little red to lose."),
            Map.entry("horsegenetics.roan",
                    "Classic roan: an even salt-and-pepper of white hairs through the whole trunk - "
                            + "shoulder, barrel, back, flank and hip alike - over a base colour it "
                            + "does not change, so a black goes blue roan and a chestnut "
                            + "strawberry. The head stays dark (the roan mask), the mane and tail "
                            + "stay solid, and the dark lower leg rises into the roaning in a "
                            + "point. Not the leopard complex's varnish roan, which is patchy and "
                            + "whitens the face. The density varies per horse and is inherited whole."),
            Map.entry("horsegenetics.tobiano",
                    "Large, smooth-edged white patches that cross the topline - the shape that "
                            + "tells tobiano from frame overo. Patches flow unbroken from the "
                            + "barrel down the legs and over the back; the head stays coloured. "
                            + "Patch size and coverage vary per horse."),
            Map.entry("horsegenetics.ednrb",
                    "Frame overo: big, sharp-edged white splotches across the middle of the "
                            + "side - barrel, flank, shoulder and lower neck - that never cross the "
                            + "back and rarely reach the belly, so colour is left framing them "
                            + "above and below. Broad white face, dark legs, and two "
                            + "sides that need not match; some carriers show almost nothing, which "
                            + "is why frame is tested for. Two copies (O/O) is Overo Lethal White - "
                            + "an all-white foal that does not survive. It has its locus to itself, "
                            + "so it stacks freely with every other white pattern."),
            Map.entry("horsegenetics.kit",
                    "The white-spotting neighbourhood on chromosome 3, here an eight-allele locus "
                            + "covering sabino (SB1), the numbered dominant-white (W) series and "
                            + "the W20 booster. Coverage runs from a few white hairs to an almost "
                            + "entirely white horse. Because it is one locus, a horse is one of "
                            + "these and never two at once; some W homozygotes cannot occur."),
            Map.entry("horsegenetics.mitf",
                    "The first of two splash-white loci (with PAX3): white rising from below in a "
                            + "hard-edged waterline over the legs, belly and face. Splash is really "
                            + "two genes, so a horse can carry a copy here and a copy on PAX3 and "
                            + "be markedly whiter than either alone."),
            Map.entry("horsegenetics.pax3",
                    "The second splash-white locus (with MITF), carrying SW2 and SW4. Same "
                            + "hard-edged waterline of white from below; because it is a separate "
                            + "gene from MITF, splash stacks on splash with no special rule. A "
                            + "large majority of wild horses carry one SW2 copy, so a little splash "
                            + "is close to the baseline look."),
            Map.entry("horsegenetics.milk",
                    "A magical gene that paints nothing. A grown mare can be milked for milk; any "
                            + "horse that is Watr/Watr gives water from a bucket, any that is "
                            + "Lava/Lava gives lava. Both variants are recessive to the wild type "
                            + "and to each other, and one of each is an embryonic lethal - you can "
                            + "only breed a water or lava horse, never catch one."),
            Map.entry("horsegenetics.body_size",
                    "A magical gene, carried by most wild horses, that scales the whole horse. "
                            + "It is codominant: each copy carries a percentage (Big positive, "
                            + "Small negative) and the two add, so every combination differs. The "
                            + "percentage is small on average and is inherited exactly with the "
                            + "allele, so two strong copies are a breeding project. It changes "
                            + "size only, not stats."),
            Map.entry("horsegenetics.magic_speed",
                    "The magical counterpart of the natural speed genes: one Swift copy multiplies "
                            + "a horse's resolved speed up by a per-copy percentage, one Sluggish "
                            + "copy multiplies it down, and two copies add. It multiplies rather "
                            + "than adds, so a magically fast pony is still slower than a magically "
                            + "fast racehorse. Most wild horses carry a copy."),
            Map.entry("horsegenetics.magic_health",
                    "The magical counterpart of the natural health genes: a Hardy copy multiplies "
                            + "max health up, a Frail copy multiplies it down, two copies add. It "
                            + "is deliberately not treated as a disorder, so turning health "
                            + "genetics off leaves it alone - a Frail horse simply has fewer "
                            + "hearts, it is not sick."),
            Map.entry("horsegenetics.magic_jump",
                    "The magical counterpart of jump strength: a Springy copy multiplies jump up, "
                            + "a Leaden copy multiplies it down, two copies add. Same shape as "
                            + "magic speed and magic health, and most wild horses carry a copy."),
            Map.entry("horsegenetics.mane_color",
                    "A magical gene: the mane in any colour, solid or banded. The heterozygote "
                            + "shows both at once in two different colours - the one gene that "
                            + "needs each allele copy's own hue - and each colour is inherited "
                            + "with its copy. Nothing shows on a foal (the foal model has no mane)."),
            Map.entry("horsegenetics.tail_color",
                    "Mane colour's twin, one locus over, for the tail - solid or banded hair in "
                            + "any colour, the heterozygote showing both. Kept separate from the "
                            + "mane gene on purpose, so a red mane with a blue tail is something "
                            + "you can breed. Shows on a foal."),
            Map.entry("horsegenetics.particle",
                    "A magical gene that paints nothing: the horse trails a particle as it moves "
                            + "- flames, souls, snow, hearts, portal motes. Forty variants share "
                            + "one locus, so a horse shows at most two, ever; most pairs hide the "
                            + "lower-ranked copy, some families show both at once. Colour, body "
                            + "site and density are drawn per allele copy and inherited with it."),
            Map.entry("horsegenetics.light",
                    "A magical gene: gold, glowing hooves, mane or eyes, plus torch-strength world "
                            + "light around the horse. Three variant alleles, each dominant to the "
                            + "wild type and to none of the others, so a horse shows every part it "
                            + "carries a copy for."),
            Map.entry("horsegenetics.healer",
                    "A magical gene: only Hlr/Hlr does anything - a healing aura that mends "
                            + "players standing within a few blocks, plus a red stripe down the "
                            + "centre of the mane so you can see which horse it is. The stripe's "
                            + "opacity varies per horse but says nothing about the healing "
                            + "strength."),
            Map.entry("horsegenetics.verdant",
                    "A magical gene that paints nothing: spreads mycelium, moss or grass from the "
                            + "hooves, at most one block at a time. Every variant needs two copies "
                            + "of itself - a mixed pair (say moss and grass) does nothing at all."),
            Map.entry("horsegenetics.lut",
                    "A magical gene that swaps the colour lookup the natural genes resolve "
                            + "through. Only a horse with two identical variant copies is affected: "
                            + "Blupnk/Blupnk resolves every melanin genotype against a blue-and-pink "
                            + "gradient instead of the warm red/black one, for dreamier coats. One "
                            + "copy, or two different variants, shows nothing."),
            Map.entry("horsegenetics.cutie_mark",
                    "A magical, recessive gene: Cutmrk/Cutmrk stamps a little emblem of one to "
                            + "three items on both flanks, on top of every other coat gene. Which "
                            + "items, how many, whether they sit in a row or a triangle, and how big "
                            + "they are are all epigenetic and inherited with the allele. One copy "
                            + "shows nothing."),
            Map.entry("horsegenetics.mstn",
                    "MSTN (myostatin), the sprint / stamina trade-off. Codominant: each C copy "
                            + "adds a little speed and costs two hearts of health. Endurance is "
                            + "paid for in hearts because there is no separate stamina bar."),
            Map.entry("horsegenetics.pdk4",
                    "PDK4, a straightforward speed gene: each A copy adds a small amount of "
                            + "movement speed and nothing else. It is one atomic locus, not a "
                            + "hidden bundle of markers."),
            Map.entry("horsegenetics.ckm",
                    "CKM, the weakest and rarest of the three natural speed genes: each T copy "
                            + "adds a small amount of movement speed."),
            Map.entry("horsegenetics.ryr2",
                    "RYR2: each J copy adds jump strength - the first gene in the mod to move a "
                            + "horse's jump at all."),
            Map.entry("horsegenetics.lcorl",
                    "LCORL / NCAPG, a height gene: each L copy makes the horse taller and adds a "
                            + "little speed and jump. Body scale changes the model and the hitbox "
                            + "together."),
            Map.entry("horsegenetics.hmga2",
                    "HMGA2, the pony gene: each p copy makes the horse smaller and slower with "
                            + "less jump, but adds two hearts of health - small has to buy "
                            + "something."),
            Map.entry("horsegenetics.acan",
                    "ACAN, a dwarfism locus with five alleles. A horse is affected when it has no "
                            + "working copy left, so two different variant alleles together still "
                            + "count. One pairing (D1/D1) is lethal at birth; every other affected "
                            + "combination is a surviving dwarf with reduced health. No wild horse "
                            + "is ever born affected - only two carriers bred together."),
            Map.entry("horsegenetics.b4galt7",
                    "B4GALT7, Friesian dwarfism - the one disorder in the mod a horse lives with: "
                            + "reduced scale and health, but survivable. Recessive; wild horses "
                            + "carry it but are never affected."),
            Map.entry("horsegenetics.plod1",
                    "PLOD1, fragile foal syndrome - a recessive lethal: an affected foal does not "
                            + "survive. Wild horses can carry one copy silently; two carriers bred "
                            + "together are the only way it appears."),
            Map.entry("horsegenetics.rapgef5",
                    "RAPGEF5 (EFIH), the most severe and rarest of the foal lethals - recessive, "
                            + "lethal at birth, carried silently by a small fraction of wild "
                            + "horses."),
            Map.entry("horsegenetics.st14",
                    "ST14, naked foal syndrome - a recessive lethal at birth. The bald coat it "
                            + "causes in life is not drawn (the pipeline can only remove pigment, "
                            + "and a bald mane would read as a white one); the lethality is."),
            Map.entry("horsegenetics.shox",
                    "SHOX, skeletal atavism - a recessive lethal at birth. It sits on the "
                            + "pseudoautosomal region, so it is inherited like an ordinary "
                            + "autosomal gene."),
            Map.entry("horsegenetics.magic_sectoral_heterochromia",
                    "A magical gene that only exists in the heterozygote: six colour alleles "
                            + "(green, blue, brown, hazel, gold, chaos) plus a wild type, and a "
                            + "horse carrying two DIFFERENT colours shows both at once, in a "
                            + "randomly shaped wedge of each iris. Two of the same colour, or "
                            + "anything with a wild-type copy, shows nothing and leaves the horse's "
                            + "own eye colour alone. Chaos takes its colour from its own copy's "
                            + "epigenetics, so it is different on almost every horse."),
            Map.entry("horsegenetics.met",
                    "MET, lethal at conception: when two carriers would produce an affected foal, "
                            + "the pairing simply yields no foal at all. The opposite of Overo "
                            + "Lethal White, which is at least born."));

    /** The summary for {@code geneKey}, or {@code ""} if there is no entry. */
    public static String of(String geneKey) {
        return BY_KEY.getOrDefault(geneKey, "");
    }

    /** Every gene key that has a written summary. */
    public static java.util.Set<String> keys() {
        return BY_KEY.keySet();
    }
}
