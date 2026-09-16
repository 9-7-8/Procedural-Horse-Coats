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
 * {@code HorseProgress.complete} for it, and
 * {@code neoforge-26.1.2/tools/check-progress-tasks.mjs} is the reason that
 * stays true - run it after adding one. (It is a sweep of the call sites, not
 * a test: it proves each task is reachable from somewhere, not that it fires at
 * the right moment. See {@code wiki/known-gaps.html#gap-148}.)
 *
 * <h2>The group is a chapter of the book</h2>
 * {@link Group} is the six chapters of the Getting Started tab, not a filing
 * system of its own. Every task belongs to the chapter that teaches it, and
 * every <i>sub-chapter</i> of that tab ends in at least one of them - which is
 * what lets a sub-chapter be <i>finished</i> and say so in the contents list.
 * The mapping from sub-chapter to tasks lives in {@code TutorialPage}; this
 * enum only says which chapter a task belongs to.
 *
 * <h2>The hint is not flavour</h2>
 * A player looking at an unticked box wants to know what to <i>do</i>, and the
 * title is too short to say. The hint is the sentence that would otherwise send
 * them to the wiki.
 */
public enum ProgressTask {

    // ---- 1. Husbandry -------------------------------------------------
    TAME_MARE(Group.HUSBANDRY, "Tame a mare",
            "Ride a wild horse until it stops throwing you off. A mare has a pink symbol after her name."),
    TAME_STALLION(Group.HUSBANDRY, "Tame a stallion",
            "The same, for a horse with a blue symbol. You need one of each to breed."),
    NAME_HORSE(Group.HUSBANDRY, "Name a horse",
            "Right-click one of yours with a name tag. Horses come with names already; this makes one yours."),
    BARN_NAME(Group.HUSBANDRY, "Give a horse a barn name",
            "Open a tamed horse's inventory, click the i hanging off the left of the window, and type "
                    + "into the box on Overview. A barn name is what you actually call it; its real "
                    + "name stays as it is."),
    FEED_BY_HAND(Group.HUSBANDRY, "Feed a horse by hand",
            "Hold food a horse will eat and use it on one of yours. Feeding by hand is the deliberate "
                    + "half of bonding - the rest accrues just by being near it."),
    FAVOURITE_FOOD(Group.HUSBANDRY, "Find a horse's favourite food",
            "Every horse has one food it likes above all others. Hit on it and you get double the bond, "
                    + "two hearts of healing and a minute of speed - so it is worth working through the list."),
    HEAL_AT_WATER(Group.HUSBANDRY, "Heal a hurt horse",
            "A horse only mends within three blocks of water, and being fed is what pays for it. "
                    + "A starving horse cannot heal at all, and a horse in a herd heals twice as fast."),
    SHEAR_HORSE(Group.HUSBANDRY, "Groom a horse for hair",
            "Right-click one with shears. You are brushing it, not shearing it - the horse enjoys "
                    + "it, and the loose hair that comes away in the brush is the base material "
                    + "for nearly everything this mod adds. Once a day, one to three hair."),
    BUNDLE_HAIR(Group.HUSBANDRY, "Bundle up horse hair",
            "Nine hair into one bundle - how you store a season of grooming, and what the horseman "
                    + "buys by the armful."),
    CRAFT_ROPE(Group.HUSBANDRY, "Twist a braided rope",
            "Horse hair into rope. It is the first thing hair is good for, and the gate on a good "
                    + "deal of the rest."),
    CRAFT_HAIR_CLOTH(Group.HUSBANDRY, "Weave hair cloth",
            "Coarse cloth from braided rope. The horseman pays better for it than for the rope."),
    BOND_HORSE(Group.HUSBANDRY, "Bond with a horse",
            "Ride it, feed it and keep it well. Bond builds slowly and unlocks how it behaves around you."),
    BOND_ATTENTIVE(Group.HUSBANDRY, "Bond a horse to attentive",
            "Bond 31. It starts turning its head to watch you from across the paddock."),
    BOND_APPROACHES(Group.HUSBANDRY, "Bond a horse to approaching",
            "Bond 61. It walks over to you of its own accord, when it can find a way round."),
    BOND_FOLLOWS(Group.HUSBANDRY, "Bond a horse to following",
            "Bond 81. It follows you at a walk and stops a few paces off. The top of the scale."),
    BUILD_STALL(Group.HUSBANDRY, "Give a horse a stall",
            "Bind a stall sign to that horse, then hang it on the wall of a room that closes in. "
                    + "A sign that cannot find an enclosed room refuses to go up rather than guess."),
    HANG_PEN_SIGN(Group.HUSBANDRY, "Set up a holding pen",
            "Hang a holding pen sign on the wall of an enclosed pen. A pen belongs to you rather than "
                    + "to one horse - it is where the horse you tamed a minute ago goes."),
    CRAFT_BLANK_TICKET(Group.HUSBANDRY, "Craft a blank ticket",
            "Paper and horse hair. It does nothing on its own - it is the base every written "
                    + "ticket is crafted up from, and the horseman sells them by the handful."),
    USE_TICKET(Group.HUSBANDRY, "Send a horse to its stall",
            "Use a written ticket on a horse of yours that has a stall. A basic ticket works in the "
                    + "overworld, a bound one anywhere within one world, an interdimensional one anywhere at all."),
    USE_BOUND_TICKET(Group.HUSBANDRY, "Use a bound ticket",
            "The middle tier - anywhere inside one world, the horse dimension included."),
    USE_INTERDIMENSIONAL_TICKET(Group.HUSBANDRY, "Use an interdimensional ticket",
            "The top tier - any world to any other. An eye of ender and blaze powder on top of the "
                    + "bound ticket."),
    USE_PEN_TICKET(Group.HUSBANDRY, "Send a horse to your pen",
            "A holding pen ticket takes any horse you own to your pen, from any world, whether or not "
                    + "it has a stall of its own."),
    WHISTLE_BASIC(Group.HUSBANDRY, "Use a basic whistle",
            "Calls every tamed horse you own within sixteen blocks. Horse hair, an iron nugget and a stick."),
    WHISTLE_GOLDEN(Group.HUSBANDRY, "Use a golden whistle",
            "The middle tier - thirty-two blocks instead of sixteen."),
    WHISTLE_ECHO(Group.HUSBANDRY, "Use an echo whistle",
            "Sixty-four blocks, the longest reach of the three area whistles."),
    WHISTLE_ENDER(Group.HUSBANDRY, "Call a horse with an ender whistle",
            "Bind one to a horse you own and it will come from anywhere, even another dimension. "
                    + "It never rebinds, and it crumbles when that horse dies."),
    DYE_TACK(Group.HUSBANDRY, "Dye a piece of tack",
            "Put a saddle or leather horse armour in an Equestrian Bench and give any of the three "
                    + "rows a dye or an ingot. Seat, bridle and fittings colour separately."),
    MILK_MARE(Group.HUSBANDRY, "Milk a mare",
            "Right-click a tamed adult mare with an empty bucket. Stallions decline, at some length."),

    // ---- 2. Basic breeding --------------------------------------------
    NATURAL_COVER(Group.BASIC_BREEDING, "Let a stallion cover a mare",
            "Leave an entire stallion with a mare while she is in heat, both above nine-tenths health, "
                    + "neither ridden nor on a lead, and fewer than eight horses crowding her. He courts "
                    + "her for three seconds and sees to it himself. No carrot involved."),
    VET_KIT_USE(Group.BASIC_BREEDING, "Read a horse with a vet's kit",
            "Use it on a mare to hear whether she is in heat, how good the half is, or whether she is "
                    + "pregnant - twins included, which nothing else will tell you. On a stallion it "
                    + "says how many of his three daily covers he has made."),
    BREED_FOAL(Group.BASIC_BREEDING, "Get your first foal",
            "However you arrange it. Each parent passes on one of its two copies of every gene, and "
                    + "which one is a coin flip - so the same pair never gives you the same foal twice."),
    FILL_SEED_JAR(Group.BASIC_BREEDING, "Fill a seed jar",
            "Feed an entire stallion of yours a golden carrot, then use an empty jar on him. It "
                    + "counts as one of his three covers for the day and takes his armed carrots with it."),
    USE_SEED_JAR(Group.BASIC_BREEDING, "Use a seed jar on a mare",
            "She has to be in heat. A stallion you have never met can sire a foal this way, which is "
                    + "how a bloodline travels between players."),
    GELD_HORSE(Group.BASIC_BREEDING, "Geld a stallion",
            "Crouch and use a vet's kit on a stallion of your own. He sires nothing afterwards, keeps "
                    + "company the way a mare does, and earns bond a quarter faster. It cannot be undone."),

    // ---- 3. Genetics ---------------------------------------------------
    GENE_BOOK(Group.GENETICS, "Research a gene with a book",
            "Right-click any horse while holding a book. The book records one of its genes at random - the horse keeps it."),
    DISCOVER_GENE(Group.GENETICS, "Discover a gene",
            "Own a living horse that carries something other than the plain baseline. That - not a "
                    + "paper, and not looking at one in a field - is what puts a gene in your database "
                    + "and unlocks its carrot."),
    BUILD_SHELF(Group.GENETICS, "Build an Equine Research Shelf",
            "One bookshelf and two horse hair. Right-click it to open."),
    FILE_PAPER(Group.GENETICS, "File a paper into the shelf",
            "On the shelf's Store tab. The paper goes in and the gene joins that shelf's list."),
    COPY_PAPER(Group.GENETICS, "Copy a paper on the shelf",
            "Craft tab: pick a filed gene, add a blank book, and wait. Rarer genes take longer."),
    GENE_CARROT(Group.GENETICS, "Craft a gene carrot",
            "A golden carrot, that gene's research paper, horse hair, and an ingot set by its rarity."),
    USE_GENE_CARROT(Group.GENETICS, "Feed a gene carrot to a parent",
            "That parent will pass the gene on instead of leaving it to a coin flip. It waits on the "
                    + "horse until the next breeding that actually takes."),
    CARROT_STABILIZER(Group.GENETICS, "Use a stabilizer carrot",
            "The fed parent passes on its dominant copy at every locus where it has one."),
    CARROT_MAGNIFIER(Group.GENETICS, "Use a magnifier carrot",
            "The mirror of the stabilizer - the recessive copy instead."),
    CARROT_UNKNOWN_GENE(Group.GENETICS, "Use an unknown gene splice carrot",
            "Rolls one random gene and redraws that parent's contribution to it. It can never roll a "
                    + "lethal - the random splices draw from a safe pool."),
    CARROT_UNKNOWN_EPIGENETIC(Group.GENETICS, "Use an unknown epigenetic splice carrot",
            "Re-rolls the fine detail - bay points, dapples, splash edges - without touching the genes."),
    CARROT_DILUTION(Group.GENETICS, "Use a dilution splice carrot",
            "Narrows the roll to the genes that lighten a coat - cream, champagne, dun, flaxen, "
                    + "mushroom, grey and tiger eye."),
    CARROT_WHITE(Group.GENETICS, "Use a white splice carrot",
            "Narrows it to the white and spotting genes, roan, and the leopard complex."),
    CARROT_MARKING(Group.GENETICS, "Use a marking splice carrot",
            "Sooty, pangaré, primitive striping and brindle - the genes that draw on a coat rather "
                    + "than recolour it."),
    CARROT_PERFORMANCE(Group.GENETICS, "Use a performance splice carrot",
            "Speed, jump and height - and only ever upward."),
    CARROT_MAGICAL(Group.GENETICS, "Use a magical splice carrot",
            "Everything a real horse could not do."),
    LIGHT_PORTAL(Group.GENETICS, "Light a hay portal",
            "Build a frame of hay bales like a nether portal and right-click it with a golden carrot."),
    ENTER_DIMENSION(Group.GENETICS, "Visit the horse dimension",
            "Stand in the lit portal for ten seconds. Two thousand pens, every one a different genotype, "
                    + "each signed with what it carries - a showroom for genes you have not bred yet."),
    BRING_HORSE_HOME(Group.GENETICS, "Bring a horse back from it",
            "Lead or ride one into the return portal. It comes home with you."),

    // ---- 4. Breeding projects -------------------------------------------
    FOAL_HETEROZYGOUS(Group.PROJECTS, "Breed a foal carrying something it does not show",
            "Two different alleles at one locus. Most foals manage this without being asked - and it "
                    + "is the whole reason a plain-looking horse is worth keeping."),
    FOAL_HOM_DOMINANT(Group.PROJECTS, "Breed a foal homozygous for a dominant allele",
            "Two copies of the same non-baseline allele - it breeds true from here."),
    FOAL_HOM_RECESSIVE(Group.PROJECTS, "Breed a foal homozygous for a recessive allele",
            "Two copies of something neither parent necessarily showed. This is where the surprises are, "
                    + "and what breeding a line back together is for."),

    // ---- 5. Horses in the wild -------------------------------------------
    DISCOVER_BREED(Group.WILD, "Meet a breed",
            "Tame a horse of any named breed and it joins your breed book. Breeds belong to the country "
                    + "they came from, so what is out there depends on the biome you are standing in."),
    BREED_EGG(Group.WILD, "Found a breed from a spawn egg",
            "The horseman sells breed spawn eggs from his middle ranks. One right-click puts a "
                    + "foundation horse of that breed in front of you - the quick way to a breed your "
                    + "own biomes will never give you."),
    FORM_HERD(Group.WILD, "Let your horses form a herd",
            "Keep two horses within ten blocks of each other for long enough and they band up on their "
                    + "own. A herd heals twice as fast, and a foal in one learns you twice as fast."),

    // ---- 6. The villagers -------------------------------------------------
    TRADE_HORSEMAN(Group.PEOPLE, "Trade with a horseman",
            "Put a Horseman's Table near an unemployed villager. He deals in research papers, carrots, "
                    + "tack and breed spawn eggs, and buys horse hair off you from his first day."),
    MEET_COWBOY(Group.PEOPLE, "Meet a cowboy",
            "A Cowboy Hitch makes one. He keeps his own string of four to ten horses and will sell you "
                    + "one - and he only deals in the breeds of the country he came from."),
    TRANSFER_PAPER(Group.PEOPLE, "Sign a transfer paper",
            "How a horse changes hands: sign it against one you own and whoever holds it can claim the horse.");

    /** The headings the checklist is grouped under, in order - the book's six chapters. */
    public enum Group {
        HUSBANDRY("Husbandry"),
        BASIC_BREEDING("Basic breeding"),
        GENETICS("Genetics"),
        PROJECTS("Breeding projects"),
        WILD("Horses in the wild"),
        PEOPLE("The villagers");

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
