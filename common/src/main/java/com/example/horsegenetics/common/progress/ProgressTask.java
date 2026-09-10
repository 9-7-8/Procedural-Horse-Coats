package com.example.horsegenetics.common.progress;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>The checklist that teaches the mod.</b> Every task a player can tick off,
 * in the order somebody learning would meet them.
 *
 * <h2>Why this is in {@code common/}</h2>
 * It is a list of ids and English, with no Minecraft in it, so it is testable
 * without a game and it survives the version port. The <i>hooks</i> that
 * complete a task are necessarily Minecraft-side and live next to whatever
 * event already existed; this is only the vocabulary they share.
 *
 * <h2>Every task here is wired</h2>
 * A checklist with an item nobody can tick is worse than a shorter checklist -
 * it reads as a bug, and the player cannot tell which. So the rule is that a
 * task exists here only once something calls
 * {@code HorseProgress.complete} for it, and {@code ProgressTaskWiringTest}
 * is the reason that stays true.
 *
 * <h2>The hint is not flavour</h2>
 * A player looking at an unticked box wants to know what to <i>do</i>, and the
 * title is too short to say. The hint is the sentence that would otherwise send
 * them to the wiki.
 */
public enum ProgressTask {

    // ---- first steps -------------------------------------------------
    TAME_MARE(Group.FIRST_STEPS, "Tame a mare",
            "Ride a wild horse until it stops throwing you off. A mare has a pink symbol after her name."),
    TAME_STALLION(Group.FIRST_STEPS, "Tame a stallion",
            "The same, for a horse with a blue symbol. You need one of each to breed."),
    NAME_HORSE(Group.FIRST_STEPS, "Name a horse",
            "Right-click one of yours with a name tag. Horses come with names already; this makes one yours."),
    BARN_NAME(Group.FIRST_STEPS, "Give a horse a barn name",
            "Open a tamed horse's inventory, click the i hanging off the left of the window, and type "
                    + "into the box on Overview. A barn name is what you actually call it; its real "
                    + "name stays as it is."),
    SHEAR_HORSE(Group.FIRST_STEPS, "Shear a horse for hair",
            "Right-click one with shears. Horse hair is the base material for nearly everything "
                    + "this mod adds, and it grows back."),
    BREED_FOAL(Group.FIRST_STEPS, "Breed your first foal",
            "Feed a golden carrot to a tamed mare and a tamed stallion standing together."),
    MILK_MARE(Group.CARE, "Milk a mare",
            "Right-click a tamed adult mare with an empty bucket. Stallions decline, at some length."),

    // ---- reading a horse ---------------------------------------------
    GENE_BOOK(Group.GENETICS, "Take a gene off a horse",
            "Right-click any horse while holding a book. The book grabs one of its genes at random."),
    BUILD_SHELF(Group.GENETICS, "Build an Equine Research Shelf",
            "One bookshelf and two horse hair. Right-click it to open."),
    FILE_PAPER(Group.GENETICS, "File a paper into the shelf",
            "On the shelf's Store tab. The paper goes in and the gene joins that shelf's list."),
    COPY_PAPER(Group.GENETICS, "Copy a paper on the shelf",
            "Craft tab: pick a filed gene, add a blank book, and wait. Rarer genes take longer."),

    // ---- putting a gene into a horse ---------------------------------
    GENE_CARROT(Group.SPLICING, "Craft a gene carrot",
            "A golden carrot, that gene's research paper, horse hair, and an ingot set by its rarity."),
    USE_GENE_CARROT(Group.SPLICING, "Feed a gene carrot to a parent",
            "That parent will pass the gene on instead of leaving it to a coin flip."),
    CARROT_STABILIZER(Group.SPLICING, "Use a stabilizer carrot",
            "The fed parent passes on its dominant copy at every locus where it has one."),
    CARROT_MAGNIFIER(Group.SPLICING, "Use a magnifier carrot",
            "The mirror of the stabilizer - the recessive copy instead."),
    CARROT_UNKNOWN_GENE(Group.SPLICING, "Use an unknown gene splice carrot",
            "Rolls one random gene and redraws that parent's contribution to it."),
    CARROT_UNKNOWN_EPIGENETIC(Group.SPLICING, "Use an unknown epigenetic splice carrot",
            "Re-rolls the fine detail - bay points, dapples, splash edges - without touching the genes."),

    // ---- what breeding can produce -----------------------------------
    FOAL_HETEROZYGOUS(Group.BREEDING, "Breed a foal carrying something it does not show",
            "Two different alleles at one locus. Most foals manage this without being asked."),
    FOAL_HOM_DOMINANT(Group.BREEDING, "Breed a foal homozygous for a dominant allele",
            "Two copies of the same non-baseline allele - it breeds true from here."),
    FOAL_HOM_RECESSIVE(Group.BREEDING, "Breed a foal homozygous for a recessive allele",
            "Two copies of something neither parent necessarily showed. This is where the surprises are."),

    // ---- the horse dimension -----------------------------------------
    LIGHT_PORTAL(Group.DIMENSION, "Light a hay portal",
            "Build a frame of hay bales like a nether portal and right-click it with a golden carrot."),
    ENTER_DIMENSION(Group.DIMENSION, "Visit the horse dimension",
            "Stand in the lit portal for ten seconds. Two thousand pens, every one a different genotype."),
    BRING_HORSE_HOME(Group.DIMENSION, "Bring a horse back from it",
            "Lead or ride one into the return portal. It comes home with you."),

    // ---- living with horses ------------------------------------------
    BOND_HORSE(Group.CARE, "Bond with a horse",
            "Ride it, feed it and keep it well. Bond builds slowly and unlocks how it behaves around you."),
    BOND_ATTENTIVE(Group.CARE, "Bond a horse to attentive",
            "Bond 31. It starts turning its head to watch you from across the paddock."),
    BOND_APPROACHES(Group.CARE, "Bond a horse to approaching",
            "Bond 61. It walks over to you of its own accord, when it can find a way round."),
    BOND_FOLLOWS(Group.CARE, "Bond a horse to following",
            "Bond 81. It follows you at a walk and stops a few paces off. The top of the scale."),
    BUILD_STALL(Group.CARE, "Give a horse a stall",
            "Hang a stall sign bound to that horse on the wall of a room it can stand in."),
    WHISTLE_BASIC(Group.CARE, "Use a basic whistle",
            "Calls every tamed horse you own nearby. Horse hair, an iron nugget and a stick."),
    WHISTLE_ECHO(Group.CARE, "Use an echo whistle",
            "The middle tier - it reaches further."),
    WHISTLE_GOLDEN(Group.CARE, "Use a golden whistle",
            "The longest reach of the three."),
    FILL_SEED_JAR(Group.CARE, "Fill a seed jar",
            "Stores a stallion's genetics so a mare can be bred with him later, from anywhere."),

    // ---- other people -------------------------------------------------
    TRADE_HORSEMAN(Group.PEOPLE, "Trade with a horseman",
            "Put a Horseman's Table near an unemployed villager. He deals in research papers."),
    MEET_COWBOY(Group.PEOPLE, "Meet a cowboy",
            "A Cowboy Hitch makes one. He keeps his own horses and will sell you one."),
    TRANSFER_PAPER(Group.PEOPLE, "Sign a transfer paper",
            "How a horse changes hands: sign it against one you own and whoever holds it can claim the horse.");

    /** The headings the checklist is grouped under, in order. */
    public enum Group {
        FIRST_STEPS("First steps"),
        GENETICS("Reading a horse"),
        SPLICING("Putting a gene into a horse"),
        BREEDING("What breeding can produce"),
        DIMENSION("The horse dimension"),
        CARE("Living with horses"),
        PEOPLE("Other people");

        private final String title;

        Group(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }
    }

    private final Group group;
    private final String title;
    private final String hint;

    ProgressTask(Group group, String title, String hint) {
        this.group = group;
        this.title = title;
        this.hint = hint;
    }

    public Group group() {
        return group;
    }

    public String title() {
        return title;
    }

    public String hint() {
        return hint;
    }

    /** The stable id this is saved and synced under - never the ordinal. */
    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /** The one task with this id, or {@code null} for an id this build does not have. */
    public static ProgressTask byId(String id) {
        return BY_ID.get(id);
    }

    private static final Map<String, ProgressTask> BY_ID = new LinkedHashMap<>();

    static {
        for (ProgressTask task : values()) {
            BY_ID.put(task.id(), task);
        }
    }

    /** Every task in one group, in declaration order. */
    public static List<ProgressTask> inGroup(Group group) {
        List<ProgressTask> out = new ArrayList<>();
        for (ProgressTask task : values()) {
            if (task.group() == group) {
                out.add(task);
            }
        }
        return List.copyOf(out);
    }
}
