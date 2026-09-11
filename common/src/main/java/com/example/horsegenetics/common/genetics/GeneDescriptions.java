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
                    "The dilution locus - one gene, five alleles (Cr, prl, sun, sno, N). One cream "
                            + "copy is a partial dilution (palomino / buckskin / smoky black); two "
                            + "cream, or cream beside any of the three recessives, is a full double "
                            + "dilute. Pearl needs two copies for a milder uniform dilution; "
                            + "sunshine needs two and reads like champagne; snowdrop needs two and "
                            + "is indistinguishable from a double cream. That last part is the "
                            + "point: a horse that looks like a cremello may carry one cream allele "
                            + "or none, and only its genotype says which."),
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
            Map.entry("horsegenetics.extreme_white_dominant",
                    "A magical gene that paints nothing at all and changes a rule instead: while "
                            + "it is present, a white texel is final. Every marking still runs in "
                            + "the ordinary order and every one of them is discarded wherever the "
                            + "coat is already white, so the horse's natural white markings come "
                            + "out on top of the magic instead of under it. Silent on a horse with "
                            + "no white. One copy is the whole of it."),
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
                    "A magical recessive that paints nothing: the horse trails a particle as it "
                            + "moves - flames, souls, snow, hearts, portal motes. Forty variants "
                            + "share one locus, so a horse shows at most two, ever. One wild-type "
                            + "copy silences the lot, so it takes two variant copies to see "
                            + "anything; between two variants the lower-ranked one shows and some "
                            + "families show both at once. Colour, body site and density are drawn "
                            + "per allele copy and inherited with it."),
            Map.entry("horsegenetics.rainbow_dust",
                    "A magical recessive that paints nothing: Rbw/Rbw kicks coloured dust off all "
                            + "four hooves as it walks, fading from one colour to the next and "
                            + "working round the whole rainbow. One copy shows nothing. How fast "
                            + "the colour turns is drawn per allele copy and inherited with it."),
            Map.entry("horsegenetics.molten_hooves",
                    "A magical DOMINANT that paints nothing - the only dominant one among the "
                            + "trails: one Mlt copy is enough, and the horse leaves burning "
                            + "hoofprints that fade out behind it as it moves. Nothing catches "
                            + "fire. Two copies behave exactly the same but draw the trail from "
                            + "both copies' colours at once. The colour is drawn per allele copy "
                            + "and inherited with it, so a line breeds true to its own fire."),
            Map.entry("horsegenetics.fireproof",
                    "A magical recessive that paints nothing: two copies and neither the horse "
                            + "nor its rider takes fire damage, and the horse swims THROUGH lava rather "
                            + "than over it. Wild horses carry it and never show it, so a fireproof horse "
                            + "is always one somebody bred."),
            Map.entry("horsegenetics.bird_boned",
                    "A magical recessive that paints nothing: two copies and neither the horse "
                            + "nor its rider takes any falling damage, from any height. It removes fall "
                            + "damage, not falling."),
            Map.entry("horsegenetics.ocean_born",
                    "A magical recessive that paints nothing: two copies and neither the horse "
                            + "nor its rider can drown. It says nothing about how fast the horse swims or "
                            + "how long it lasts under - those are other loci, on purpose."),
            Map.entry("horsegenetics.hydrophobic",
                    "A magical recessive that paints nothing, and the odd one out: it RESTORES "
                            + "vanilla behaviour rather than adding to it. Two copies and the horse "
                            + "throws its rider in deep water and heads for shore, losing the swimming "
                            + "assist every other tamed horse here gets."),
            Map.entry("horsegenetics.hot_blooded",
                    "A magical recessive that paints nothing: two copies and snow and ice "
                            + "within a couple of blocks melt away, leaving a thawed circle wherever the "
                            + "horse stands. On a frozen lake it melts the ice it is standing on and is "
                            + "then in the water."),
            Map.entry("horsegenetics.dryad",
                    "A magical recessive that paints nothing: about once a day the horse plants "
                            + "a sapling near where it stands. It plants; it does not fertilise. How "
                            + "often is drawn per allele copy and inherited with it."),
            Map.entry("horsegenetics.intimidating",
                    "A magical recessive that paints nothing: two copies and everything that is "
                            + "not a horse - your cows as readily as any monster - is pushed out of a "
                            + "radius drawn per allele copy. It cannot be pastured with your animals, "
                            + "which is the cost and is deliberate."),
            Map.entry("horsegenetics.meowing",
                    "A magical recessive that paints nothing: two copies and the horse meows "
                            + "occasionally, and creepers keep outside a radius drawn per allele copy. "
                            + "Creepers are the mob that costs you a horse, which is what makes the joke "
                            + "worth breeding."),
            Map.entry("horsegenetics.cleansing_light",
                    "A magical recessive that paints nothing: two copies and undead within a "
                            + "radius drawn per allele copy take steady damage. Anything the aura kills "
                            + "was killed by the HORSE, so you get no experience and no player-only drops "
                            + "- it is defence, not farming."),
            Map.entry("horsegenetics.holy_ward",
                    "A magical recessive that paints nothing: two copies and hostile mobs "
                            + "cannot SPAWN within a radius drawn per allele copy, at least eight blocks "
                            + "and never more than sixteen. A camp that walks. Wild horses carry it and "
                            + "never show it."),
            Map.entry("horsegenetics.echolocate",
                    "A magical recessive that paints nothing: two copies and the horse cries "
                            + "like a bat now and then, outlining everything alive nearby through walls "
                            + "and darkness. It shows you the skeleton and the lost cow without "
                            + "distinguishing between them."),
            Map.entry("horsegenetics.base_alarm",
                    "A magical DOMINANT that paints nothing: one copy is enough, and the horse "
                            + "whinnies when something hostile comes within about sixteen blocks of IT - "
                            + "not of your base, which is not a thing this mod has. Where you stable it "
                            + "is the configuration."),
            Map.entry("horsegenetics.music_enjoyer",
                    "A magical recessive that paints nothing: two copies and the horse's bond "
                            + "rises while a jukebox plays nearby, with hearts. It answers to the same "
                            + "daily bond ceiling as every other source, so it is a pleasant way to spend "
                            + "a day rather than a shortcut past one."),
            Map.entry("horsegenetics.gladiator",
                    "A magical recessive that paints nothing: two copies and the horse attacks "
                            + "hostile mobs that come within reach - but only while nobody is riding it, "
                            + "and it holds its ground rather than chasing. It makes the horse willing; "
                            + "magic fighter is what makes it dangerous."),
            Map.entry("horsegenetics.guardian",
                    "A magical recessive that paints nothing: two copies, tamed, and the horse "
                            + "attacks whatever damaged its OWNER within about sixteen blocks. It never "
                            + "picks the fight and it keeps working while you ride, which is what "
                            + "separates it from a gladiator."),
            Map.entry("horsegenetics.ender_echo",
                    "A magical recessive that paints nothing: two copies and the horse blinks "
                            + "about eight blocks away when something damages it, carrying its rider. "
                            + "Against skeletons it is close to immunity; against a skeleton across a "
                            + "ravine it is a trip you did not plan."),
            Map.entry("horsegenetics.spontaneous_breeding",
                    "A magical recessive that paints nothing and grants no effect at all: two "
                            + "copies, AND another horse nearby with two copies, and the pair breed on "
                            + "their own with no golden carrots. Wild horses carry it and never show it, "
                            + "so the first pair in a world is one somebody bred."),
            Map.entry("horsegenetics.eyesight",
                    "A magical recessive that paints nothing: Cav/Cav is faster in the dark and "
                            + "slower in light, Day/Day the reverse, and one of each cancels. It reads "
                            + "the actual LIGHT LEVEL rather than the clock, so a cave at noon counts and "
                            + "your own torches slow a caveborn horse down."),
            Map.entry("horsegenetics.weather_speed",
                    "A magical codominant that paints nothing: a positive and a negative allele "
                            + "for rain, storms and snowfall, each moving the horse's speed by a "
                            + "percentage drawn per allele copy while its own weather holds. Unlinked to "
                            + "the jump locus. You cannot check it on demand - you wait for the sky."),
            Map.entry("horsegenetics.weather_jump",
                    "A magical codominant that paints nothing: the same idea as the weather "
                            + "speed locus and a completely separate gene, moving jump instead. "
                            + "Inheriting one tells you nothing about the other, which is the whole "
                            + "reason there are two."),
            Map.entry("horsegenetics.food_preference",
                    "A magical DOMINANT that paints nothing: one copy and the horse has one "
                            + "favourite food, which gives a short buff and far more bond than it should. "
                            + "It OVERRIDES the diet locus - a meat-eater that inexplicably loves carrots "
                            + "will eat them, and that is the point."),
            Map.entry("horsegenetics.potion_milk",
                    "A magical incomplete dominant that paints nothing: milk a tamed grown mare "
                            + "with an EMPTY BOTTLE rather than a bucket and get a potion. One copy is "
                            + "weak, two matching copies are strong, and two DIFFERENT copies give one "
                            + "bottle carrying both effects at the weak grade."),
            Map.entry("horsegenetics.egg_layer",
                    "A magical recessive that paints nothing: two copies of the SAME allele and "
                            + "the horse drops an item on the ground every few hours with nothing "
                            + "required from you. One copy shows nothing, and two different alleles show "
                            + "nothing. How often is drawn per allele copy."),
            Map.entry("horsegenetics.singer",
                    "A magical recessive that paints nothing: two copies of the SAME allele and "
                            + "the horse plays the OPENING of a music disc now and then - not the whole "
                            + "thing and never the middle, because a Minecraft sound cannot be started "
                            + "partway through. Duration and volume are per allele copy."),
            Map.entry("horsegenetics.pack_leader",
                    "A magical recessive that paints nothing: two copies of the SAME allele and "
                            + "every creature of one kind within sixteen blocks trails the horse around. "
                            + "One allele per non-hostile mob, and wild horses only ever carry - so a "
                            + "matched pair is always something somebody bred."),
            Map.entry("horsegenetics.spawner",
                    "A magical recessive that paints nothing: two copies of the SAME allele and "
                            + "feeding the horse makes two of one mob, every meal - "
                            + "the food is the only limit. One allele per mob INCLUDING "
                            + "the monsters, and never expressed in the wild."),
            Map.entry("horsegenetics.lycan",
                    "The werewolf locus, and it paints nothing: two copies of the SAME allele and "
                            + "the horse becomes a real animal from dusk to dawn - a wolf, a cat, "
                            + "a chicken, one of thirty-seven shapes. One copy shows nothing, and "
                            + "two different shapes show nothing either. A shifted animal can be "
                            + "done everything to that animal can be done to, cannot be ridden, "
                            + "trails a faint cloud of dust whose colour is inherited, and follows "
                            + "anything that strikes it until sunrise."),
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
                            + "Lethal White, which is at least born."),
            Map.entry("horsegenetics.ppib",
                    "PPIB (HERDA), fragile skin - a recessive disorder the horse lives with rather "
                            + "than dies of: it splits where a saddle sits and never heals cleanly. "
                            + "The commonest survivable recessive here, because the real thing hides "
                            + "inside exactly the working lines a breeder would choose."),
            Map.entry("horsegenetics.prkdc",
                    "PRKDC (SCID), severe combined immunodeficiency - a recessive lethal. The foal "
                            + "is born with no immune system and does not survive."),
            Map.entry("horsegenetics.myo5a",
                    "MYO5A, lavender foal syndrome - a recessive lethal at birth. The real disorder "
                            + "comes with a diluted silvery coat, which is deliberately not drawn: "
                            + "the foal dies in seconds and phase 1 can only remove pigment."),
            Map.entry("horsegenetics.toe1",
                    "TOE1, cerebellar abiotrophy - a recessive disorder of balance. The horse "
                            + "survives but has no idea where its feet are, which makes it the "
                            + "heaviest jump penalty in the mod. Progressive in reality; a flat cost "
                            + "here, because there is no age model."),
            Map.entry("horsegenetics.cvm",
                    "CVM, cervical vertebral malformation - a recessive lethal at birth. Unlike "
                            + "every other natural gene here it has no confirmed causal variant; "
                            + "treating it as one locus is the mod's simplification, not a claim "
                            + "about horse genetics."),
            Map.entry("horsegenetics.gbe1",
                    "GBE1 (GBED), glycogen branching enzyme deficiency - a recessive lethal at "
                            + "birth, and the largest heart reduction in the mod. The foal cannot "
                            + "store or release sugar."),
            Map.entry("horsegenetics.megaesophagus",
                    "Megaesophagus, a slack gullet that will not move milk - a recessive lethal at "
                            + "birth. Like CVM it is a breed condition with no confirmed causal "
                            + "variant, simplified to one locus."),
            Map.entry("horsegenetics.scn4a",
                    "SCN4A (HYPP), periodic paralysis - the first locus that is BOTH a survivable "
                            + "disorder and a lethal. One copy affects the horse (there is no silent "
                            + "carrier), two kill the foal. Wild horses can be born with one copy, "
                            + "because a dominant that never appeared in a founder could never "
                            + "appear at all."),
            Map.entry("horsegenetics.gys1",
                    "GYS1 (PSSM1), tying-up - a dominant disorder, so every copy shows and there is "
                            + "no carrier. Neither combination is lethal, which makes it the mildest "
                            + "and by far the commonest disorder a player will actually meet."),
            Map.entry("horsegenetics.tiger_eye",
                    "A bright amber iris in a horse whose coat is entirely ordinary. It is the first "
                            + "gene in the mod that changes only the eyes, and the reason the "
                            + "eye-colour channel exists at all. Essentially confined to the Puerto "
                            + "Rican Paso Fino, in life and here."),
            Map.entry("horsegenetics.leopard",
                    "The leopard complex (TRPM1), and the only gene in the model that reads other "
                            + "loci to decide what it paints. LP alone gives the appaloosa "
                            + "characteristics - roaning that spares the bony parts, a scatter of "
                            + "white spots, striped hooves, a white-rimmed eye. The bold patterns - "
                            + "leopard, fewspot, blanket, snowcap - come from the PATN1 and PATN2 "
                            + "modifiers, which do nothing at all on a horse without LP."),
            Map.entry("horsegenetics.patn1",
                    "A pure modifier of the leopard complex: it pushes an LP horse toward the "
                            + "spotted end - one LP copy plus PATN1 gives a full-body leopard, and "
                            + "LP/LP plus PATN1 a nearly white fewspot. On a horse with no LP it "
                            + "does nothing whatever and cannot be seen, and no founder rolls it "
                            + "unless it already rolled LP."),
            Map.entry("horsegenetics.patn2",
                    "The second leopard-complex pattern modifier, and a pure modifier like PATN1: "
                            + "on an LP horse it gives the blanket patterns - a white sheet over the "
                            + "hips, spotted or not - and carrying both modifiers gives the "
                            + "semi-leopard between the two. Invisible on a horse without LP."),
            Map.entry("horsegenetics.magic_milk_volume",
                    "How many times a day the horse can be filled from. Wild type is once; the milky "
                            + "allele is worth one to three more, and the copies add. Because it "
                            + "governs a yield kind rather than a gene, it reaches milk, water and "
                            + "lava alike."),
            Map.entry("horsegenetics.magic_meat",
                    "How much meat the horse leaves. Additive rather than exclusive - a meaty horse "
                            + "still drops whatever magic item drop says, and the meat is on top of "
                            + "it. How much is a number written on the allele copy rather than on "
                            + "the allele, so two meaty horses differ."),
            Map.entry("horsegenetics.magic_item_drop",
                    "What the horse leaves behind - diamonds, a spawn egg that puts this horse back, "
                            + "or an enchanted sword. Every variant takes two copies and none of them "
                            + "shows on the living animal, which is the point: worth breeding a line "
                            + "for, and impossible to spot in a field."),
            Map.entry("horsegenetics.magic_on_death",
                    "What happens to the world where the horse died - a lava source, a water source, "
                            + "or a creeper-sized explosion. It says nothing about items; that is "
                            + "magic item drop, and the two are kept apart on purpose."),
            Map.entry("horsegenetics.magic_mob_aura",
                    "How everything else in the world feels about the horse. A warding horse keeps "
                            + "mobs about ten blocks off; a baiting one makes every hostile that can "
                            + "see it pick it over anything else in range, including its rider. Both "
                            + "take two copies, and one of each cancels outright."),
            Map.entry("horsegenetics.magic_night_temper",
                    "What the horse becomes after dark. Eight variants in two families: one hunts and "
                            + "the other runs, and each names who it feels that about - riders, "
                            + "passive animals, monsters, or everything. By day every one of them is "
                            + "an ordinary horse."),
            Map.entry("horsegenetics.magic_night_watch",
                    "What the horse does about you after dark. Five variants that are a progression "
                            + "rather than a list, each one a little closer and a little worse - from "
                            + "staring through walls to standing directly behind you. Any of them, "
                            + "homozygous, also makes the horse silent on its feet at night."),
            Map.entry("horsegenetics.magic_swim_speed",
                    "How fast the horse moves in water, and nothing else. It is unrelated to magic "
                            + "speed - not related-but-separate, unrelated - so a horse can be the "
                            + "slowest thing in the paddock and still cross a river faster than a "
                            + "boat."),
            Map.entry("horsegenetics.magic_water_breathing",
                    "How long the horse lasts under water before it starts to drown. The graded "
                            + "counterpart of the underwater-breathing traversal flag, which is "
                            + "absolute - and independent of magic swim speed, because crossing a "
                            + "river quickly and surviving the bottom of a lake are different "
                            + "problems."),
            Map.entry("horsegenetics.magic_fighter",
                    "What the horse hits for. Wild type is 3 points - a heart and a half. The "
                            + "gladiator allele adds a percentage of that and the wimp allele takes "
                            + "one off, and both copies are added with opposite signs, so a wimp copy "
                            + "really does count against a gladiator one."),
            Map.entry("horsegenetics.shadowcreature",
                    "A gene that takes the front third of the horse away. The head, muzzle, ears and "
                            + "whole neck go flat black, the eyes burn gold with no white around "
                            + "them, and where the black meets the barrel it does not stop in a line "
                            + "but runs out into tapering tentacles over the shoulder."));

    /** The summary for {@code geneKey}, or {@code ""} if there is no entry. */
    public static String of(String geneKey) {
        return BY_KEY.getOrDefault(geneKey, "");
    }

    /** Every gene key that has a written summary. */
    public static java.util.Set<String> keys() {
        return BY_KEY.keySet();
    }
}
