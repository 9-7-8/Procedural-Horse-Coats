package com.example.horsegenetics.common.breed;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The built-in breed registry - 47 real-world breeds plus {@link #UNKNOWN}, the
 * label a lone wild horse / {@code /summon} / spawn-egg horse carries.
 *
 * <p>Every breed here is a first pass: the biome assignments and stat scores
 * come straight from the owner's breed sheet, and the per-gene pool <b>rates</b>
 * are estimates chosen so a herd reads as that breed without being a monoculture.
 * {@code wiki/breeds.html} is the readable version of this file.
 *
 * <h2>Referenced-but-unbuilt</h2>
 * Genes and features the sheet asks for that this mod does not have yet - the
 * leopard complex ({@code Lp} / {@code PATN1}), Tiger Eye ({@code TE}), HYPP,
 * pangare / mealy muzzle, the Fjord's two-tone erect mane, lower-leg feathering
 * - are recorded in each breed's {@link Breed#notes()} and collected in
 * {@code wiki/roadmap.html}. No new gene was added in this pass.
 */
public final class Breeds {

    // gene keys, spelled once
    private static final String EXT = "horsegenetics.extension";
    private static final String AGO = "horsegenetics.agouti";
    private static final String MATP = "horsegenetics.matp";
    private static final String DUN = "horsegenetics.dun";
    private static final String CHAMP = "horsegenetics.champagne";
    private static final String SILVER = "horsegenetics.silver";
    private static final String GREY = "horsegenetics.grey";
    private static final String ROAN = "horsegenetics.roan";
    private static final String TOB = "horsegenetics.tobiano";
    private static final String KIT = "horsegenetics.kit";
    private static final String MITF = "horsegenetics.mitf";
    private static final String PAX3 = "horsegenetics.pax3";
    private static final String EDNRB = "horsegenetics.ednrb";

    /** The "no herd identity" breed. Its founder is the ordinary unconstrained roll. */
    public static final Breed UNKNOWN = Breed.of("unknown", "Unknown")
            .note("Lone wild spawns, /summon and spawn-egg horses. Every gene rolled unconstrained - the pre-breeds behaviour.")
            .build();

    private static final List<Breed> ALL = new ArrayList<>();
    private static final Map<String, Breed> BY_ID = new LinkedHashMap<>();

    static {
        register(akhalTeke());
        register(americanCreamDraft());
        register(americanMiniature());
        register(americanPaint());
        register(andalusian());
        register(appaloosa());
        register(arabian());
        register(bankerHorse());
        register(belgianDraft());
        register(camargue());
        register(canadianHorse());
        register(caspianPony());
        register(clevelandBay());
        register(clydesdale());
        register(connemaraPony());
        register(dalesPony());
        register(dartmoorPony());
        register(exmoorPony());
        register(falabella());
        register(fjord());
        register(friesian());
        register(gypsyVanner());
        register(hackney());
        register(haflinger());
        register(hanoverian());
        register(icelandicHorse());
        register(irishDraught());
        register(karabakh());
        register(kigerMustang());
        register(knabstrupper());
        register(lipizzan());
        register(lusitano());
        register(marwari());
        register(morgan());
        register(mustang());
        register(newForestPony());
        register(pasoFino());
        register(percheron());
        register(przewalski());
        register(puertoRicanPasoFino());
        register(quarterHorse());
        register(shetlandPony());
        register(shire());
        register(standardbred());
        register(suffolkPunch());
        register(tennesseeWalking());
        register(thoroughbred());
        register(trakehner());
        register(welshPony());
    }

    private Breeds() {
    }

    private static void register(Breed b) {
        if (BY_ID.put(b.id(), b) != null) {
            throw new IllegalStateException("duplicate breed id " + b.id());
        }
        ALL.add(b);
    }

    public static List<Breed> all() {
        return List.copyOf(ALL);
    }

    public static Breed get(String id) {
        return BY_ID.getOrDefault(id, UNKNOWN);
    }

    public static Breed getOrUnknown(java.util.Optional<String> id) {
        return id.map(Breeds::get).orElse(UNKNOWN);
    }

    public static String displayName(String id) {
        if (id == null || id.isBlank() || id.equals("unknown")) {
            return "Unknown";
        }
        Breed b = BY_ID.get(id);
        if (b != null) {
            return b.name();
        }
        return Character.toUpperCase(id.charAt(0)) + id.substring(1);
    }

    /** Breeds that can head a wild herd in {@code biomeId} (a "minecraft:plains" style string). */
    public static List<Breed> forBiome(String biomeId) {
        List<Breed> out = new ArrayList<>();
        for (Breed b : ALL) {
            if (b.biomes().contains(biomeId)) {
                out.add(b);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------
    // helpers for the definitions below

    private static double hh(int hands, int inches) {
        return hands + inches / 4.0;
    }

    // ------------------------------------------------------------------
    // the breeds
    // ------------------------------------------------------------------

    private static Breed akhalTeke() {
        return Breed.of("akhal_teke", "Akhal-Teke").commonness(Commonness.UNCOMMON)
                .biomes("minecraft:desert", "minecraft:savanna", "minecraft:badlands", "minecraft:savanna_plateau")
                .extensionAny().agoutiAny()
                .gene(MATP, "Cr", "N", 14).gene(MATP, "Cr", "Cr", 3).gene(MATP, "N", "N", 83)
                .gene(GREY, "G", "g", 5).gene(GREY, "g", "g", 95)
                .gene(DUN, "d1", "d2", 30).gene(DUN, "d2", "d2", 70)
                .height(hh(14, 2), hh(16, 0)).speed(9).jump(5).health(8)
                .note("Metallic / iridescent coat sheen - not modelled (a shader, out of scope).")
                .note("NNF, CVM: early-lethal foal disorders - candidate genes, see roadmap.")
                .build();
    }

    private static Breed americanCreamDraft() {
        return Breed.of("american_cream_draft", "American Cream Draft").commonness(Commonness.RARE)
                .biomes("minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow")
                .extensionChestnut().agoutiAny()
                .fixed(MATP, "Cr")
                .height(hh(15, 0), hh(16, 0)).speed(3).jump(2).health(5)
                .note("Pale cream coat, amber eyes, pink skin - the cream double-dilute already renders; dedicated eye colour is a roadmap gene.")
                .note("EMS / laminitis: age-related, folded into heartiness.")
                .build();
    }

    private static Breed americanMiniature() {
        return Breed.of("american_miniature", "American Miniature").commonness(Commonness.VERY_COMMON)
                .biomes("minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow")
                .extensionAny().agoutiAny()
                .gene(MATP, "Cr", "N", 16).gene(MATP, "prl", "N", 8).gene(MATP, "N", "N", 76)
                .gene(DUN, "D", "d2", 10).gene(DUN, "d1", "d2", 14).gene(DUN, "d2", "d2", 76)
                .gene(CHAMP, "Ch", "c", 8).gene(CHAMP, "c", "c", 92)
                .gene(SILVER, "Z", "z", 8).gene(SILVER, "z", "z", 92)
                .gene(TOB, "To", "to", 22).gene(TOB, "to", "to", 78)
                .gene(ROAN, "Rn", "rn", 12).gene(ROAN, "rn", "rn", 88)
                .gene(KIT, "SB1", "N", 16).gene(KIT, "W20", "N", 12).gene(KIT, "N", "N", 72)
                .height(hh(7, 0), hh(8, 2)).speed(2).jump(1).health(4)
                .note("Under 9.5 hh: the magic-size band bottoms this near the MAGICAL_MIN_SCALE guard - about as small as the model goes.")
                .note("Dwarfism, dental overcrowding, EMS: ACAN dwarfism already exists; the rest fold into heartiness.")
                .build();
    }

    private static Breed americanPaint() {
        return Breed.of("american_paint", "American Paint").commonness(Commonness.VERY_COMMON)
                .biomes("minecraft:plains", "minecraft:savanna", "minecraft:windswept_savanna")
                .extensionAny().agoutiAny()
                .gene(MATP, "Cr", "N", 12).gene(MATP, "N", "N", 88)
                .gene(DUN, "D", "d2", 6).gene(DUN, "d2", "d2", 94)
                .gene(TOB, "To", "to", 48).gene(TOB, "To", "To", 10).gene(TOB, "to", "to", 42)
                .gene(KIT, "SB1", "N", 26).gene(KIT, "W20", "N", 16).gene(KIT, "N", "N", 58)
                .gene(EDNRB, "O", "N", 20).gene(EDNRB, "N", "N", 80)
                .gene(PAX3, "SW2", "N", 30).gene(PAX3, "N", "N", 70)
                .height(hh(14, 2), hh(16, 0)).speed(7).jump(5).health(6)
                .note("Pinto spotting: tobiano + sabino + frame overo + splash, all present. O/O (lethal white) is the real breeding hazard.")
                .note("HYPP: candidate heart-reducing gene, see roadmap. HERDA, PSSM1: age-related, folded into heartiness.")
                .build();
    }

    private static Breed andalusian() {
        return Breed.of("andalusian", "Andalusian").commonness(Commonness.MODERATE)
                .biomes("minecraft:plains", "minecraft:forest", "minecraft:sunflower_plains", "minecraft:meadow")
                .extensionBlackBias().agoutiAny()
                .gene(GREY, "G", "g", 52).gene(GREY, "G", "G", 22).gene(GREY, "g", "g", 26)
                .height(hh(15, 0), hh(16, 2)).speed(6).jump(8).health(5)
                .note("Thick flowing mane and tail, baroque compact build - mane length is a roadmap render item.")
                .note("OCD, laminitis: age-related, folded into heartiness.")
                .build();
    }

    private static Breed appaloosa() {
        return Breed.of("appaloosa", "Appaloosa").commonness(Commonness.MODERATE)
                .biomes("minecraft:taiga", "minecraft:forest", "minecraft:windswept_hills")
                .extensionAny().agoutiAny()
                .gene(ROAN, "Rn", "rn", 10).gene(ROAN, "rn", "rn", 90)
                .height(hh(14, 2), hh(16, 0)).speed(6).jump(7).health(7)
                .note("Leopard complex (Lp + PATN1): NOT BUILT - leopard spots, striped hooves, white sclera, mottled skin. Roadmap 4.2.")
                .note("CSNB rides on Lp/Lp; ERU is age-related. Both fold into heartiness for now.")
                .build();
    }

    private static Breed arabian() {
        return Breed.of("arabian", "Arabian").commonness(Commonness.COMMON)
                .biomes("minecraft:desert", "minecraft:savanna", "minecraft:badlands", "minecraft:savanna_plateau")
                .extensionAny().agoutiBayBias()
                .gene(GREY, "G", "g", 40).gene(GREY, "G", "G", 14).gene(GREY, "g", "g", 46)
                .gene(KIT, "SB1", "N", 14).gene(KIT, "N", "N", 86)
                .height(hh(14, 2), hh(15, 3)).speed(7).jump(6).health(9)
                .note("Dished face and high-carried tail - head profile is not modelled; tail carriage could be a pose tweak (roadmap).")
                .note("SCID, CA, LFS: early-lethal foal disorders - candidate genes, see roadmap.")
                .build();
    }

    private static Breed bankerHorse() {
        return Breed.of("banker_horse", "Banker Horse").commonness(Commonness.RARE).hardy()
                .biomes("minecraft:beach", "minecraft:stony_shore", "minecraft:river")
                .extensionAny().agoutiAny()
                .gene(MATP, "Cr", "N", 12).gene(MATP, "N", "N", 88)
                .gene(DUN, "D", "d2", 14).gene(DUN, "d1", "d2", 16).gene(DUN, "d2", "d2", 70)
                .gene(KIT, "SB1", "N", 20).gene(KIT, "N", "N", 80)
                .height(hh(13, 2), hh(14, 2)).speed(6).jump(4).health(9)
                .note("Primitive markings, Spanish-type head. Feral Colonial Spanish stock - hardy, few genetic issues.")
                .build();
    }

    private static Breed belgianDraft() {
        return Breed.of("belgian_draft", "Belgian Draft").commonness(Commonness.COMMON)
                .biomes("minecraft:plains", "minecraft:forest", "minecraft:meadow")
                .extensionChestnut().agoutiAny()
                .gene(MATP, "Cr", "N", 8).gene(MATP, "prl", "N", 5).gene(MATP, "N", "N", 87)
                .gene(ROAN, "Rn", "rn", 28).gene(ROAN, "rn", "rn", 72)
                .height(hh(16, 0), hh(17, 0)).speed(3).jump(2).health(6)
                .note("Flaxen mane/tail on a chestnut body, heavy muscling - flaxen is a roadmap gene; muscling reads through MSTN.")
                .note("CPL, EMS, anhidrosis: age-related, folded into heartiness.")
                .build();
    }

    private static Breed camargue() {
        return Breed.of("camargue", "Camargue").commonness(Commonness.UNCOMMON)
                .biomes("minecraft:swamp", "minecraft:mangrove_swamp", "minecraft:river", "minecraft:beach")
                .extensionAny().agoutiAny()
                .fixed(GREY, "G")
                .height(hh(13, 2), hh(14, 2)).speed(5).jump(4).health(9)
                .note("Born dark, greys out to white; thick mop-like mane and tail. Melanoma is age-related (folded into heartiness).")
                .build();
    }

    private static Breed canadianHorse() {
        return Breed.of("canadian_horse", "Canadian Horse").commonness(Commonness.RARE).hardy()
                .biomes("minecraft:snowy_taiga", "minecraft:grove", "minecraft:snowy_plains")
                .extensionBlackBias().agoutiAny()
                .gene(GREY, "G", "g", 14).gene(GREY, "g", "g", 86)
                .gene(ROAN, "Rn", "rn", 14).gene(ROAN, "rn", "rn", 86)
                .height(hh(14, 0), hh(16, 0)).speed(6).jump(5).health(9)
                .note("Dense thick mane and tail, 'little iron horse' build. Famously sound.")
                .build();
    }

    private static Breed caspianPony() {
        return Breed.of("caspian_pony", "Caspian Pony").commonness(Commonness.RARE)
                .biomes("minecraft:desert", "minecraft:badlands", "minecraft:savanna")
                .extensionAny().agoutiBayBias()
                .gene(KIT, "SB1", "N", 8).gene(KIT, "N", "N", 92)
                .height(hh(9, 0), hh(11, 2)).speed(5).jump(4).health(7)
                .note("Small, fine-boned, short legs, prominent eyes. 'Fragile bones' left as low heartiness rather than a lethal gene.")
                .build();
    }

    private static Breed clevelandBay() {
        return Breed.of("cleveland_bay", "Cleveland Bay").commonness(Commonness.RARE).hardy()
                .biomes("minecraft:plains", "minecraft:forest", "minecraft:meadow")
                .gene(EXT, "E", "E", 80).gene(EXT, "E", "e", 20)
                .fixed(AGO, "A")
                .gene(PAX3, "SW2", "N", 8).gene(PAX3, "N", "N", 92)
                .height(hh(16, 0), hh(16, 2)).speed(6).jump(6).health(8)
                .note("Always bay with black points, minimal white. Fixed A_ plus an E-heavy extension keeps it off chestnut and black.")
                .build();
    }

    private static Breed clydesdale() {
        return Breed.of("clydesdale", "Clydesdale").commonness(Commonness.UNCOMMON)
                .biomes("minecraft:taiga", "minecraft:snowy_taiga", "minecraft:grove", "minecraft:river")
                .extensionBlackBias().agoutiAny()
                .gene(TOB, "To", "to", 30).gene(TOB, "to", "to", 70)
                .gene(KIT, "SB1", "N", 45).gene(KIT, "SB1", "SB1", 8).gene(KIT, "N", "N", 47)
                .gene(PAX3, "SW2", "N", 35).gene(PAX3, "N", "N", 65)
                .height(hh(16, 2), hh(18, 0)).speed(3).jump(3).health(5)
                .note("Heavy lower-leg feathering - roadmap render layer. Sabino + splash drive the big white legs and face.")
                .note("CPL, sunburn: age-related, folded into heartiness.")
                .build();
    }

    private static Breed connemaraPony() {
        return Breed.of("connemara_pony", "Connemara Pony").commonness(Commonness.MODERATE)
                .biomes("minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "minecraft:windswept_forest", "minecraft:stony_shore")
                .extensionAny().agoutiAny()
                .gene(MATP, "Cr", "N", 10).gene(MATP, "N", "N", 90)
                .gene(DUN, "D", "d2", 8).gene(DUN, "d2", "d2", 92)
                .gene(GREY, "G", "g", 40).gene(GREY, "G", "G", 12).gene(GREY, "g", "g", 48)
                .height(hh(13, 0), hh(14, 2)).speed(5).jump(8).health(8)
                .note("Sturdy compact good bone, often grey. OCD / laminitis are age-related (folded into heartiness).")
                .build();
    }

    private static Breed dalesPony() {
        return Breed.of("dales_pony", "Dales Pony").commonness(Commonness.RARE)
                .biomes("minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "minecraft:taiga")
                .extensionBlackBias().agoutiAny()
                .gene(DUN, "D", "d2", 6).gene(DUN, "d2", "d2", 94)
                .gene(GREY, "G", "g", 14).gene(GREY, "g", "g", 86)
                .gene(ROAN, "Rn", "rn", 20).gene(ROAN, "rn", "rn", 80)
                .height(hh(13, 2), hh(14, 2)).speed(5).jump(6).health(9)
                .note("Heavy feathering, dense bone, hardy moorland type - feathering is a roadmap render item.")
                .note("EMS / laminitis: age-related, folded into heartiness.")
                .build();
    }

    private static Breed dartmoorPony() {
        return Breed.of("dartmoor_pony", "Dartmoor Pony").commonness(Commonness.RARE)
                .biomes("minecraft:windswept_hills", "minecraft:meadow", "minecraft:forest")
                .extensionAny().agoutiAny()
                .gene(MATP, "Cr", "N", 6).gene(MATP, "N", "N", 94)
                .gene(GREY, "G", "g", 14).gene(GREY, "g", "g", 86)
                .gene(ROAN, "Rn", "rn", 12).gene(ROAN, "rn", "rn", 88)
                .height(hh(11, 2), hh(12, 2)).speed(4).jump(5).health(8)
                .note("Small, large head, thick mane and tail. Laminitis / EMS are age-related (folded into heartiness).")
                .build();
    }

    private static Breed exmoorPony() {
        return Breed.of("exmoor_pony", "Exmoor Pony").commonness(Commonness.RARE).hardy()
                .biomes("minecraft:windswept_hills", "minecraft:taiga", "minecraft:snowy_slopes")
                .gene(EXT, "E", "E", 70).gene(EXT, "E", "e", 26).gene(EXT, "e", "e", 4)
                .agoutiBayBias()
                .gene(DUN, "d1", "d2", 30).gene(DUN, "d2", "d2", 70)
                .height(hh(11, 2), hh(12, 3)).speed(4).jump(4).health(10)
                .note("Mealy muzzle and eye rings (pangare) - NOT BUILT, roadmap pigment gene. Double-layered winter coat - cosmetic, out of scope.")
                .note("An ancient, exceptionally hardy landrace - no white, no dilutions.")
                .build();
    }

    private static Breed falabella() {
        return Breed.of("falabella", "Falabella").commonness(Commonness.UNCOMMON)
                .biomes("minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow")
                .extensionAny().agoutiAny()
                .gene(MATP, "Cr", "N", 16).gene(MATP, "prl", "N", 8).gene(MATP, "N", "N", 76)
                .gene(DUN, "D", "d2", 8).gene(DUN, "d2", "d2", 92)
                .gene(CHAMP, "Ch", "c", 8).gene(CHAMP, "c", "c", 92)
                .gene(SILVER, "Z", "z", 10).gene(SILVER, "z", "z", 90)
                .gene(TOB, "To", "to", 20).gene(TOB, "to", "to", 80)
                .gene(KIT, "SB1", "N", 14).gene(KIT, "W20", "N", 10).gene(KIT, "N", "N", 76)
                .gene(PAX3, "SW2", "N", 20).gene(PAX3, "N", "N", 80)
                .height(hh(6, 0), hh(8, 0)).speed(2).jump(1).health(3)
                .note("The smallest breed - the size band sits against the MAGICAL_MIN_SCALE guard.")
                .note("'Fragile bones' left as very low heartiness rather than a lethal gene.")
                .build();
    }

    private static Breed fjord() {
        return Breed.of("fjord", "Norwegian Fjord").commonness(Commonness.UNCOMMON)
                .biomes("minecraft:snowy_taiga", "minecraft:frozen_river", "minecraft:snowy_slopes", "minecraft:grove")
                .gene(EXT, "E", "E", 75).gene(EXT, "E", "e", 25)
                .agoutiBlack()
                .gene(DUN, "D", "d2", 40).gene(DUN, "D", "D", 55).gene(DUN, "d1", "d2", 5)
                .height(hh(13, 2), hh(14, 2)).speed(4).jump(3).health(9)
                .note("Erect two-tone mane (dark centre, light outer) - NOT BUILT, roadmap mane render. Primitive stripes come from the near-fixed dun.")
                .note("Almost every Fjord is a dun (brown/red/grey/white/yellow dun); E_ a/a + D drives the classic brown dun.")
                .note("EMS: age-related, folded into heartiness.")
                .build();
    }

    private static Breed friesian() {
        return Breed.of("friesian", "Friesian").commonness(Commonness.MODERATE)
                .biomes("minecraft:plains", "minecraft:forest", "minecraft:meadow", "minecraft:river")
                .fixed(EXT, "E")
                .fixed(AGO, "a")
                .height(hh(15, 0), hh(17, 0)).speed(5).jump(4).health(4)
                .note("Jet black, heavy feathering, thick wavy mane and tail. Fixed E/E a/a, no white or dilution genes at all.")
                .note("Feathering + wavy mane: roadmap render items.")
                .note("Dwarfism, megaesophagus: dwarfism exists (ACAN); megaesophagus is early-lethal - candidate gene, roadmap.")
                .build();
    }

    private static Breed gypsyVanner() {
        return Breed.of("gypsy_vanner", "Gypsy Vanner").commonness(Commonness.COMMON)
                .biomes("minecraft:plains", "minecraft:river", "minecraft:swamp", "minecraft:meadow")
                .extensionAny().agoutiAny()
                .gene(MATP, "Cr", "N", 8).gene(MATP, "N", "N", 92)
                .gene(DUN, "D", "d2", 6).gene(DUN, "d2", "d2", 94)
                .gene(TOB, "To", "to", 62).gene(TOB, "To", "To", 16).gene(TOB, "to", "to", 22)
                .gene(KIT, "SB1", "N", 24).gene(KIT, "N", "N", 76)
                .gene(PAX3, "SW2", "N", 30).gene(PAX3, "N", "N", 70)
                .height(hh(14, 0), hh(15, 2)).speed(4).jump(3).health(7)
                .note("'Drum' cobby body, heavy feathering, abundant mane and tail - feathering + mane are roadmap render items. Usually tobiano pinto.")
                .note("CPL, EMS: age-related, folded into heartiness.")
                .build();
    }

    private static Breed hackney() {
        return Breed.of("hackney", "Hackney").commonness(Commonness.UNCOMMON)
                .biomes("minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow")
                .extensionAny().agoutiBayBias()
                .gene(ROAN, "Rn", "rn", 10).gene(ROAN, "rn", "rn", 90)
                .gene(PAX3, "SW2", "N", 20).gene(PAX3, "N", "N", 80)
                .height(hh(14, 2), hh(16, 0)).speed(6).jump(3).health(4)
                .note("Upright neck, high knee action - gait/animation is a roadmap item (DMRT3). Refined bone.")
                .note("Osteoarthritis: age-related, folded into heartiness.")
                .build();
    }

    private static Breed haflinger() {
        return Breed.of("haflinger", "Haflinger").commonness(Commonness.COMMON)
                .biomes("minecraft:cherry_grove", "minecraft:meadow", "minecraft:windswept_hills", "minecraft:grove")
                .extensionChestnut().agoutiAny()
                .gene(ROAN, "Rn", "rn", 10).gene(ROAN, "rn", "rn", 90)
                .height(hh(13, 2), hh(15, 0)).speed(5).jump(5).health(8)
                .note("Chestnut body with a striking flaxen (white) mane and tail - flaxen is a roadmap gene. Fixed e/e keeps it chestnut.")
                .note("EMS / laminitis: age-related, folded into heartiness.")
                .build();
    }

    private static Breed hanoverian() {
        return Breed.of("hanoverian", "Hanoverian").commonness(Commonness.VERY_COMMON)
                .biomes("minecraft:plains", "minecraft:forest", "minecraft:meadow", "minecraft:sunflower_plains")
                .extensionAny().agoutiBayBias()
                .gene(GREY, "G", "g", 22).gene(GREY, "g", "g", 78)
                .gene(ROAN, "Rn", "rn", 12).gene(ROAN, "rn", "rn", 88)
                .gene(PAX3, "SW2", "N", 25).gene(PAX3, "N", "N", 75)
                .height(hh(15, 3), hh(17, 1)).speed(7).jump(9).health(5)
                .note("Powerful large-framed uphill warmblood, bred for jumping and dressage.")
                .note("OCD, navicular: age-related, folded into heartiness.")
                .build();
    }

    private static Breed icelandicHorse() {
        return Breed.of("icelandic_horse", "Icelandic Horse").commonness(Commonness.MODERATE).hardy()
                .biomes("minecraft:snowy_plains", "minecraft:ice_spikes", "minecraft:frozen_peaks", "minecraft:jagged_peaks", "minecraft:snowy_slopes")
                .extensionAny().agoutiAny()
                .gene(GREY, "G", "g", 16).gene(GREY, "g", "g", 84)
                .gene(ROAN, "Rn", "rn", 16).gene(ROAN, "rn", "rn", 84)
                .gene(DUN, "D", "d2", 12).gene(DUN, "d2", "d2", 88)
                .gene(TOB, "To", "to", 20).gene(TOB, "to", "to", 80)
                .gene(PAX3, "SW2", "N", 30).gene(PAX3, "N", "N", 70)
                .height(hh(13, 0), hh(14, 0)).speed(5).jump(4).health(10)
                .note("Thick double coat, short legs, long back - the tölt (extra gait) is a roadmap item (DMRT3). Extremely varied colours; extremely hardy.")
                .build();
    }

    private static Breed irishDraught() {
        return Breed.of("irish_draught", "Irish Draught").commonness(Commonness.UNCOMMON)
                .biomes("minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "minecraft:taiga", "minecraft:forest")
                .extensionAny().agoutiAny()
                .gene(MATP, "Cr", "N", 8).gene(MATP, "N", "N", 92)
                .gene(DUN, "D", "d2", 5).gene(DUN, "d2", "d2", 95)
                .gene(GREY, "G", "g", 24).gene(GREY, "g", "g", 76)
                .gene(ROAN, "Rn", "rn", 12).gene(ROAN, "rn", "rn", 88)
                .height(hh(15, 2), hh(17, 0)).speed(5).jump(7).health(8)
                .note("Scopey powerful frame, strong clean limbs - the classic sport-horse foundation. Joint stress is age-related (folded into heartiness).")
                .build();
    }

    private static Breed karabakh() {
        return Breed.of("karabakh", "Karabakh").commonness(Commonness.RARE).hardy()
                .biomes("minecraft:desert", "minecraft:savanna", "minecraft:badlands")
                .extensionAny().agoutiBayBias()
                .gene(MATP, "Cr", "N", 10).gene(MATP, "N", "N", 90)
                .gene(GREY, "G", "g", 24).gene(GREY, "g", "g", 76)
                .height(hh(14, 0), hh(15, 0)).speed(7).jump(5).health(8)
                .note("Small head, muscular arched neck, golden sheen - the sheen is not modelled (a shader).")
                .build();
    }

    private static Breed kigerMustang() {
        return Breed.of("kiger_mustang", "Kiger Mustang").commonness(Commonness.RARE).hardy()
                .biomes("minecraft:badlands", "minecraft:eroded_badlands", "minecraft:wooded_badlands", "minecraft:savanna")
                .extensionAny().agoutiBayBias()
                .gene(MATP, "Cr", "N", 12).gene(MATP, "N", "N", 88)
                .gene(DUN, "D", "d2", 55).gene(DUN, "D", "D", 20).gene(DUN, "d1", "d2", 10).gene(DUN, "d2", "d2", 15)
                .height(hh(13, 0), hh(14, 2)).speed(7).jump(5).health(10)
                .note("Strongly dun (dorsal stripe + leg bars) - the near-fixed dun does this. 'Often lp, To' - leopard complex NOT BUILT (roadmap 4.2).")
                .note("HERDA, GBED: GBED is early-lethal (candidate gene); HERDA is age-related.")
                .build();
    }

    private static Breed knabstrupper() {
        return Breed.of("knabstrupper", "Knabstrupper").commonness(Commonness.UNCOMMON)
                .biomes("minecraft:plains", "minecraft:birch_forest", "minecraft:forest", "minecraft:meadow")
                .extensionAny().agoutiAny()
                .gene(MATP, "Cr", "N", 6).gene(MATP, "N", "N", 94)
                .height(hh(14, 2), hh(15, 2)).speed(6).jump(7).health(6)
                .note("Bold leopard spots on white or base colour - leopard complex (Lp + PATN1) NOT BUILT. Roadmap 4.2.")
                .note("CSNB rides on Lp/Lp; ERU is age-related.")
                .build();
    }

    private static Breed lipizzan() {
        return Breed.of("lipizzan", "Lipizzan").commonness(Commonness.UNCOMMON)
                .biomes("minecraft:plains", "minecraft:windswept_hills", "minecraft:meadow", "minecraft:sunflower_plains")
                .extensionAny().agoutiAny()
                .gene(GREY, "G", "g", 60).gene(GREY, "G", "G", 34).gene(GREY, "g", "g", 6)
                .height(hh(14, 2), hh(16, 1)).speed(5).jump(7).health(7)
                .note("Born dark, greys to white; Roman nose, compact baroque build - head profile not modelled. Melanoma is age-related.")
                .build();
    }

    private static Breed lusitano() {
        return Breed.of("lusitano", "Lusitano").commonness(Commonness.MODERATE)
                .biomes("minecraft:savanna", "minecraft:plains", "minecraft:savanna_plateau")
                .extensionBlackBias().agoutiAny()
                .gene(GREY, "G", "g", 44).gene(GREY, "G", "G", 18).gene(GREY, "g", "g", 38)
                .height(hh(15, 0), hh(16, 0)).speed(6).jump(8).health(6)
                .note("Convex profile, sloping croup, baroque build - head/croup shape not modelled. OCD is age-related.")
                .build();
    }

    private static Breed marwari() {
        return Breed.of("marwari", "Marwari").commonness(Commonness.RARE)
                .biomes("minecraft:jungle", "minecraft:sparse_jungle", "minecraft:bamboo_jungle", "minecraft:savanna")
                .extensionAny().agoutiAny()
                .gene(MATP, "Cr", "N", 10).gene(MATP, "N", "N", 90)
                .gene(DUN, "D", "d2", 8).gene(DUN, "d2", "d2", 92)
                .gene(TOB, "To", "to", 30).gene(TOB, "to", "to", 70)
                .gene(GREY, "G", "g", 16).gene(GREY, "g", "g", 84)
                .height(hh(14, 2), hh(15, 2)).speed(7).jump(5).health(9)
                .note("Inward-curving (lyre-shaped) ear tips - NOT modelled (a mesh change, out of scope for now).")
                .note("Sweet itch: age-related, folded into heartiness.")
                .build();
    }

    private static Breed morgan() {
        return Breed.of("morgan", "Morgan").commonness(Commonness.VERY_COMMON)
                .biomes("minecraft:plains", "minecraft:forest", "minecraft:taiga", "minecraft:meadow")
                .extensionAny().agoutiAny()
                .gene(MATP, "Cr", "N", 8).gene(MATP, "N", "N", 92)
                .gene(DUN, "D", "d2", 5).gene(DUN, "d2", "d2", 95)
                .gene(ROAN, "Rn", "rn", 12).gene(ROAN, "rn", "rn", 88)
                .gene(KIT, "SB1", "N", 16).gene(KIT, "N", "N", 84)
                .height(hh(14, 1), hh(15, 2)).speed(6).jump(6).health(8)
                .note("Slightly crested neck, expressive eyes, compact and refined - a versatile foundation American breed.")
                .note("Cushing's, EMS: age-related, folded into heartiness.")
                .build();
    }

    private static Breed mustang() {
        return Breed.of("mustang", "Mustang").commonness(Commonness.MODERATE).hardy()
                .biomes("minecraft:badlands", "minecraft:eroded_badlands", "minecraft:wooded_badlands", "minecraft:savanna", "minecraft:windswept_savanna")
                .extensionAny().agoutiAny()
                .gene(MATP, "Cr", "N", 14).gene(MATP, "N", "N", 86)
                .gene(DUN, "D", "d2", 20).gene(DUN, "d1", "d2", 14).gene(DUN, "d2", "d2", 66)
                .gene(CHAMP, "Ch", "c", 5).gene(CHAMP, "c", "c", 95)
                .gene(ROAN, "Rn", "rn", 16).gene(ROAN, "rn", "rn", 84)
                .gene(TOB, "To", "to", 16).gene(TOB, "to", "to", 84)
                .gene(KIT, "SB1", "N", 16).gene(KIT, "N", "N", 84)
                .height(hh(13, 2), hh(15, 2)).speed(7).jump(5).health(10)
                .note("Wild-type diversity: every colour, compact build, hard feet. 'Often lp' - leopard complex NOT BUILT (roadmap 4.2).")
                .note("HERDA, GBED: GBED is early-lethal (candidate gene); HERDA is age-related.")
                .build();
    }

    private static Breed newForestPony() {
        return Breed.of("new_forest_pony", "New Forest Pony").commonness(Commonness.COMMON)
                .biomes("minecraft:forest", "minecraft:birch_forest", "minecraft:old_growth_birch_forest", "minecraft:flower_forest")
                .extensionAny().agoutiAny()
                .gene(DUN, "D", "d2", 6).gene(DUN, "d2", "d2", 94)
                .gene(GREY, "G", "g", 18).gene(GREY, "g", "g", 82)
                .gene(ROAN, "Rn", "rn", 14).gene(ROAN, "rn", "rn", 86)
                .height(hh(12, 0), hh(14, 2)).speed(5).jump(6).health(8)
                .note("Large head, thick neck, flashy movement. Laminitis / EMS are age-related (folded into heartiness).")
                .build();
    }

    private static Breed pasoFino() {
        return Breed.of("paso_fino", "Paso Fino").commonness(Commonness.MODERATE)
                .biomes("minecraft:savanna", "minecraft:jungle", "minecraft:sparse_jungle", "minecraft:bamboo_jungle")
                .extensionAny().agoutiAny()
                .gene(MATP, "Cr", "N", 10).gene(MATP, "N", "N", 90)
                .gene(DUN, "D", "d2", 6).gene(DUN, "d2", "d2", 94)
                .gene(ROAN, "Rn", "rn", 12).gene(ROAN, "rn", "rn", 88)
                .gene(KIT, "W20", "N", 10).gene(KIT, "W22", "N", 4).gene(KIT, "N", "N", 86)
                .height(hh(13, 0), hh(15, 2)).speed(5).jump(4).health(6)
                .note("Compact rounded build, naturally smooth lateral gait - gait is a roadmap item (DMRT3).")
                .note("EMS, DSLD: age-related, folded into heartiness.")
                .build();
    }

    private static Breed percheron() {
        return Breed.of("percheron", "Percheron").commonness(Commonness.COMMON)
                .biomes("minecraft:plains", "minecraft:snowy_plains", "minecraft:meadow", "minecraft:sunflower_plains")
                .extensionBlackBias().agoutiAny()
                .gene(GREY, "G", "g", 58).gene(GREY, "G", "G", 30).gene(GREY, "g", "g", 12)
                .height(hh(15, 2), hh(17, 3)).speed(4).jump(3).health(6)
                .note("Large draft with a straight profile and (for its size) fine legs. Grey or black. PSSM1 / anhidrosis are age-related.")
                .build();
    }

    private static Breed przewalski() {
        return Breed.of("przewalski", "Przewalski's Horse").commonness(Commonness.RARE).hardy()
                .biomes("minecraft:desert", "minecraft:badlands", "minecraft:savanna")
                .gene(EXT, "E", "E", 92).gene(EXT, "E", "e", 8)
                .gene(AGO, "A", "A", 88).gene(AGO, "A", "a", 12)
                .fixed(DUN, "D")
                .height(hh(12, 0), hh(14, 0)).speed(6).jump(3).health(10)
                .note("The only never-domesticated true wild horse: stocky dun body, erect black mane, no forelock, no spotting. Mane shape is a roadmap render item.")
                .note("OCD: age-related, folded into heartiness.")
                .build();
    }

    private static Breed puertoRicanPasoFino() {
        return Breed.of("puerto_rican_paso_fino", "Puerto Rican Paso Fino").commonness(Commonness.RARE)
                .biomes("minecraft:jungle", "minecraft:savanna", "minecraft:beach", "minecraft:mangrove_swamp")
                .extensionAny().agoutiAny()
                .gene(MATP, "Cr", "N", 10).gene(MATP, "N", "N", 90)
                .gene(DUN, "D", "d2", 6).gene(DUN, "d2", "d2", 94)
                .height(hh(13, 0), hh(15, 0)).speed(5).jump(4).health(6)
                .note("Tiger Eye (TE) - amber/yellow iris - NOT BUILT. Candidate eye-colour gene, roadmap.")
                .note("Naturally smooth gait - roadmap item (DMRT3). EMS, DSLD: age-related.")
                .build();
    }

    private static Breed quarterHorse() {
        return Breed.of("quarter_horse", "Quarter Horse").commonness(Commonness.EXTREMELY_COMMON)
                .biomes("minecraft:plains", "minecraft:savanna", "minecraft:sunflower_plains", "minecraft:meadow")
                .gene(EXT, "E", "E", 40).gene(EXT, "E", "e", 45).gene(EXT, "e", "e", 15)
                .gene(AGO, "A", "A", 30).gene(AGO, "A", "a", 45).gene(AGO, "a", "a", 25)
                .gene(MATP, "Cr", "N", 14).gene(MATP, "Cr", "Cr", 3).gene(MATP, "N", "N", 83)
                .gene(DUN, "d1", "d2", 22).gene(DUN, "d2", "d2", 78)
                .gene(ROAN, "Rn", "rn", 14).gene(ROAN, "rn", "rn", 86)
                .gene(KIT, "SB1", "N", 14).gene(KIT, "N", "N", 86)
                .height(hh(14, 2), hh(16, 0)).speed(8).jump(5).health(7)
                .note("Massively muscled hindquarters and broad chest - the sprinter. nd1/nd2 give the odd dorsal stripe with no dilution.")
                .note("HYPP: candidate heart-reducing gene, roadmap. PSSM1, HERDA: age-related.")
                .build();
    }

    private static Breed shetlandPony() {
        return Breed.of("shetland_pony", "Shetland Pony").commonness(Commonness.VERY_COMMON)
                .biomes("minecraft:mushroom_fields", "minecraft:snowy_plains", "minecraft:taiga", "minecraft:grove")
                .extensionAny().agoutiAny()
                .gene(DUN, "D", "d2", 8).gene(DUN, "d2", "d2", 92)
                .gene(MATP, "Cr", "N", 8).gene(MATP, "N", "N", 92)
                .gene(TOB, "To", "to", 30).gene(TOB, "to", "to", 70)
                .gene(KIT, "SB1", "N", 14).gene(KIT, "N", "N", 86)
                .height(hh(8, 0), hh(10, 2)).speed(3).jump(3).health(9)
                .note("Very small, thick coat, short legs, heavy head - pound for pound the strongest breed. 'Often lp' - leopard complex NOT BUILT.")
                .note("Hyperlipidemia, EMS: age-related, folded into heartiness.")
                .build();
    }

    private static Breed shire() {
        return Breed.of("shire", "Shire").commonness(Commonness.UNCOMMON)
                .biomes("minecraft:plains", "minecraft:river", "minecraft:meadow", "minecraft:swamp")
                .extensionBlackBias().agoutiAny()
                .gene(KIT, "SB1", "N", 40).gene(KIT, "SB1", "SB1", 6).gene(KIT, "N", "N", 54)
                .gene(PAX3, "SW2", "N", 35).gene(PAX3, "N", "N", 65)
                .height(hh(16, 2), hh(17, 3)).speed(3).jump(2).health(4)
                .note("The tallest breed - the scale band tops out around the natural draught ceiling. Heavy feathering (roadmap), Roman nose (not modelled).")
                .note("CPL, PSSM: age-related, folded into heartiness.")
                .build();
    }

    private static Breed standardbred() {
        return Breed.of("standardbred", "Standardbred").commonness(Commonness.VERY_COMMON)
                .biomes("minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow")
                .extensionAny().agoutiAny()
                .gene(MATP, "Cr", "N", 8).gene(MATP, "N", "N", 92)
                .gene(DUN, "D", "d2", 4).gene(DUN, "d2", "d2", 96)
                .gene(ROAN, "Rn", "rn", 12).gene(ROAN, "rn", "rn", 88)
                .gene(KIT, "SB1", "N", 10).gene(KIT, "N", "N", 90)
                .height(hh(14, 2), hh(16, 2)).speed(9).jump(5).health(6)
                .note("Long powerful body, thick neck, sturdy legs - the harness racer (trot/pace). Gait is a roadmap item (DMRT3).")
                .note("EIPH, DSLD: age-related / exertional, folded into heartiness.")
                .build();
    }

    private static Breed suffolkPunch() {
        return Breed.of("suffolk_punch", "Suffolk Punch").commonness(Commonness.RARE)
                .biomes("minecraft:plains", "minecraft:forest", "minecraft:meadow", "minecraft:sunflower_plains")
                .extensionChestnut().agoutiAny()
                .height(hh(16, 0), hh(17, 2)).speed(3).jump(2).health(5)
                .note("Always chestnut ('Suffolk sorrel'), wide barrel-bodied, no feathering. Fixed e/e, no white or dilution genes.")
                .note("Anhidrosis, obesity: age-related, folded into heartiness. Rare - one of the most endangered breeds.")
                .build();
    }

    private static Breed tennesseeWalking() {
        return Breed.of("tennessee_walking", "Tennessee Walking Horse").commonness(Commonness.VERY_COMMON)
                .biomes("minecraft:plains", "minecraft:forest", "minecraft:swamp", "minecraft:meadow")
                .extensionAny().agoutiAny()
                .gene(MATP, "Cr", "N", 12).gene(MATP, "N", "N", 88)
                .gene(CHAMP, "Ch", "c", 8).gene(CHAMP, "c", "c", 92)
                .gene(ROAN, "Rn", "rn", 10).gene(ROAN, "rn", "rn", 90)
                .gene(KIT, "SB1", "N", 18).gene(KIT, "N", "N", 82)
                .gene(GREY, "G", "g", 12).gene(GREY, "g", "g", 88)
                .gene(PAX3, "SW2", "N", 25).gene(PAX3, "N", "N", 75)
                .height(hh(14, 3), hh(17, 0)).speed(5).jump(4).health(7)
                .note("Long sloping shoulder, long neck, powerful rear drive - the running walk is a roadmap item (DMRT3). Lordosis is age-related.")
                .build();
    }

    private static Breed thoroughbred() {
        return Breed.of("thoroughbred", "Thoroughbred").commonness(Commonness.VERY_COMMON)
                .biomes("minecraft:plains", "minecraft:savanna", "minecraft:sunflower_plains", "minecraft:meadow")
                .extensionAny().agoutiBayBias()
                .gene(GREY, "G", "g", 14).gene(GREY, "g", "g", 86)
                .gene(ROAN, "Rn", "rn", 8).gene(ROAN, "rn", "rn", 92)
                .gene(PAX3, "SW2", "N", 20).gene(PAX3, "N", "N", 80)
                .height(hh(15, 2), hh(17, 0)).speed(10).jump(8).health(4)
                .note("Lean, sleek, flat profile, refined legs - the fastest breed in the game, and among the most fragile. EIPH ('bleeder') is exertional.")
                .build();
    }

    private static Breed trakehner() {
        return Breed.of("trakehner", "Trakehner").commonness(Commonness.COMMON)
                .biomes("minecraft:taiga", "minecraft:forest", "minecraft:old_growth_pine_taiga", "minecraft:grove")
                .extensionAny().agoutiBayBias()
                .gene(GREY, "G", "g", 20).gene(GREY, "g", "g", 80)
                .gene(ROAN, "Rn", "rn", 12).gene(ROAN, "rn", "rn", 88)
                .gene(PAX3, "SW2", "N", 22).gene(PAX3, "N", "N", 78)
                .height(hh(15, 2), hh(17, 0)).speed(7).jump(8).health(5)
                .note("The lightest, most refined warmblood - an elegant head and an athletic build. OCD, bone spavin: age-related.")
                .build();
    }

    private static Breed welshPony() {
        return Breed.of("welsh_pony", "Welsh Pony").commonness(Commonness.VERY_COMMON)
                .biomes("minecraft:windswept_hills", "minecraft:taiga", "minecraft:plains", "minecraft:meadow")
                .extensionAny().agoutiAny()
                .gene(MATP, "Cr", "N", 10).gene(MATP, "N", "N", 90)
                .gene(DUN, "D", "d2", 6).gene(DUN, "d2", "d2", 94)
                .gene(GREY, "G", "g", 30).gene(GREY, "G", "G", 8).gene(GREY, "g", "g", 62)
                .gene(ROAN, "Rn", "rn", 12).gene(ROAN, "rn", "rn", 88)
                .gene(KIT, "SB1", "N", 16).gene(KIT, "N", "N", 84)
                .height(hh(11, 0), hh(14, 2)).speed(4, 6).jump(5).health(8)
                .note("Small with large ears, a crested neck and a floating trot. Laminitis / EMS are age-related (folded into heartiness).")
                .build();
    }
}
