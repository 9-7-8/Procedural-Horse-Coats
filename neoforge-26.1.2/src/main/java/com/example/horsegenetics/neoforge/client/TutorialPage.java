package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.progress.ProgressTask;
import com.example.horsegenetics.neoforge.item.ModItems;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>The Getting Started tab's content.</b> What a new player has to be told, in
 * the order they will need it, with something to look at beside every step.
 *
 * <h2>Six chapters, and every sub-chapter ends in something to do</h2>
 * The page is <b>chapters of sub-chapters</b> (owner, roadmap &sect;24.5):
 * husbandry, basic breeding, genetics, breeding projects, horses in the wild,
 * the villagers. Each sub-chapter carries <b>at least one {@link ProgressTask}</b>
 * - the checklist is not a separate page any more, it is the last thing in the
 * sub-chapter that teaches it. That is what lets a sub-chapter be <i>finished</i>
 * and say so in the contents list, and it is why a reader who has just been told
 * how to hang a stall sign finds the box for it directly underneath rather than
 * on a tab they have to know to look at.
 *
 * <p>The chapter order is a teaching order, not a difficulty order. Husbandry
 * comes first and mentions breeding nowhere: keeping a horse alive and content
 * is a whole game before genetics is, and a player who meets heats and punnett
 * squares in their first ten minutes puts the mod down.
 *
 * <h2>Why the pictures are entities and items, not art</h2>
 * Everything illustrated here is drawn from the game: item stacks through the
 * item renderer, and the horseman and the cowboy as real entities
 * ({@link TutorialPortraits}). Nothing has to be exported, nothing goes stale
 * when a texture changes, and a resource pack the player has on is reflected -
 * which matters most for exactly the case this page exists for, somebody trying
 * to recognise a villager they have never seen.
 *
 * <h2>It is prose, deliberately</h2>
 * A wall of bullet points is faster to write and worse to read, and this is the
 * first thing anybody sees. The flavour is not decoration either: "some horses
 * are magic" and "breeds belong to biomes" are the two facts that make a player
 * go and look at horses rather than walk past them, and they are far more
 * persuasive as an aside than as a heading.
 *
 * <h2>Only what is in the game</h2>
 * Every claim here was checked against the code rather than the wiki, which is
 * stale in several places. Three worth keeping straight, because the obvious
 * thing to write is wrong: a natural cover needs <b>nine-tenths</b> health and
 * not full health; a horse heals only near <b>water</b> and the food half of
 * that gate is gone; and there is <b>no inbreeding penalty of any kind</b>, so
 * the line-breeding sub-chapter says so outright rather than implying a
 * mechanic that does not exist.
 */
public final class TutorialPage {

    /** One illustration beside a step. */
    public enum Art {
        NONE,
        HORSEMAN,
        COWBOY
    }

    /**
     * A sub-chapter: a heading, some paragraphs, a row of items, maybe a
     * portrait, and the checklist tasks it teaches. {@code tasks} is never
     * empty - see the class note.
     */
    public record Step(String heading, List<String> paragraphs, List<ItemStack> icons, Art art,
                       List<ProgressTask> tasks) {
    }

    /** A chapter: a title and its sub-chapters. */
    public record Chapter(String title, List<Step> steps) {
    }

    private static final int ART = 48;
    private static final int ICON = 18;

    private static List<Chapter> chapters;

    private static List<Step> steps;

    private static List<String> headings;

    private TutorialPage() {
    }

    /** The six chapters, in page order. Built once per session; item stacks need the registry. */
    public static List<Chapter> chapters() {
        if (chapters == null) {
            chapters = build();
        }
        return chapters;
    }

    /** Every sub-chapter, flattened into page order - what a section index indexes. */
    public static List<Step> steps() {
        if (steps == null) {
            List<Step> out = new ArrayList<>();
            for (Chapter chapter : chapters()) {
                out.addAll(chapter.steps());
            }
            steps = List.copyOf(out);
        }
        return steps;
    }

    /** Every sub-chapter's heading, in page order. Cached: it is read every frame. */
    public static List<String> headings() {
        if (headings == null) {
            List<String> out = new ArrayList<>();
            for (Step step : steps()) {
                out.add(step.heading());
            }
            headings = List.copyOf(out);
        }
        return headings;
    }

    /** Dropped with the rest of the per-world client state. */
    public static void clear() {
        chapters = null;
        steps = null;
        headings = null;
    }

    private static List<Chapter> build() {
        List<Chapter> out = new ArrayList<>();

        // ------------------------------------------------------------------
        // 1. Husbandry - keeping a horse, and nothing about breeding.
        // ------------------------------------------------------------------
        List<Step> husbandry = new ArrayList<>();

        husbandry.add(new Step("Taming, and what a horse is",
                List.of("Horses in this world are not four colours and a saddle. Each one carries a "
                                + "full set of genes, inherited from its parents the way a real horse's "
                                + "are, and its coat is painted from them - so no two are quite alike "
                                + "and none of them was drawn by hand.",
                        "Some of them are not entirely ordinary, either. There are horses that glow, "
                                + "horses that walk on water, and horses that are perfectly friendly "
                                + "until the sun goes down. You will know one when you meet it.",
                        "To tame one, find a wild horse and get on it with nothing in your hand. It "
                                + "will throw you off. Get back on. Keep getting back on until hearts "
                                + "appear. Feeding it first makes it quicker to convince, and a saddle "
                                + "lets you steer it once it is yours.",
                        "Every horse is a mare or a stallion, and it is a gene like any other - a foal "
                                + "inherits its sex rather than being assigned one. Look at the name "
                                + "above its head: a pink ♀ is a mare, a blue ♂ is a stallion, "
                                + "and a grey ♂ is a gelding. (There is a switch in the client "
                                + "config if you would rather not see them.)",
                        "A name tag renames a horse the way it renames anything. Its barn name is "
                                + "something else: open a tamed horse's inventory, click the small i "
                                + "hanging off the left of the window, and type into the box on "
                                + "Overview. That is what you actually call it; the name it was born "
                                + "with stays as it is."),
                List.of(new ItemStack(Items.SADDLE), new ItemStack(Items.WHEAT),
                        new ItemStack(Items.NAME_TAG)), Art.NONE,
                List.of(ProgressTask.TAME_MARE, ProgressTask.TAME_STALLION,
                        ProgressTask.NAME_HORSE, ProgressTask.BARN_NAME)));

        husbandry.add(new Step("Food, water and healing",
                List.of("A horse gets hungry, and it feeds itself. A hungry one goes looking within "
                                + "about ten blocks and eats what it finds - hay bales, crops, grass, "
                                + "moss, mushrooms, flowers, even a cake if you left one out. What it "
                                + "eats is gone, so do not plant your wheat against the paddock fence.",
                        "There is no hunger bar to read. Nothing in the game will tell you a horse is "
                                + "hungry; the only tell is that it wanders off to eat. A pasture with "
                                + "grass in it feeds itself, and a hay bale is the largest single meal "
                                + "there is.",
                        "Healing is the part that catches people out. A hurt horse mends only within "
                                + "three blocks of water, and being fed is what pays for it - a "
                                + "starving horse cannot heal at all. Vanilla's slow trickle is "
                                + "switched off for horses, so water is not optional. Somewhere to "
                                + "drink is the most useful thing you can put in a paddock.",
                        "A horse standing in a herd mends twice as fast as one on its own.",
                        "Feeding by hand is worth bond as well as food - two points, or four if you "
                                + "hit on the one food that particular horse likes best, which also "
                                + "heals it and leaves it briefly quick on its feet. Every horse has a "
                                + "favourite and they do not all share one."),
                List.of(new ItemStack(Items.HAY_BLOCK), new ItemStack(Items.WATER_BUCKET),
                        new ItemStack(Items.WHEAT), new ItemStack(Items.GOLDEN_APPLE)), Art.NONE,
                List.of(ProgressTask.FEED_BY_HAND, ProgressTask.FAVOURITE_FOOD,
                        ProgressTask.HEAL_AT_WATER)));

        husbandry.add(new Step("Grooming, and horse hair",
                List.of("Right-click an adult horse with shears. You are brushing it rather than "
                                + "shearing it: the horse enjoys it, you get one to three horse hair, "
                                + "and it is worth two bond. Once a day, per horse.",
                        "Do this early and often. Horse hair is the base material for nearly "
                                + "everything this mod adds - rope and cloth, the whistles, the stall "
                                + "signs and the tickets, the seed jars, the transfer papers, the "
                                + "research shelf, the vet's kit and every breeding carrot. Almost "
                                + "nothing else can be made without it."),
                List.of(new ItemStack(Items.SHEARS), new ItemStack(ModItems.HORSE_HAIR.get()),
                        new ItemStack(ModItems.HORSE_HAIR_BUNDLE.get()),
                        new ItemStack(ModItems.BRAIDED_ROPE.get()),
                        new ItemStack(ModItems.HAIR_CLOTH.get())), Art.NONE,
                List.of(ProgressTask.SHEAR_HORSE, ProgressTask.BUNDLE_HAIR,
                        ProgressTask.CRAFT_ROPE, ProgressTask.CRAFT_HAIR_CLOTH)));

        husbandry.add(new Step("Bonding",
                List.of("Every horse you tame keeps a private opinion of you: a number from 0 to 100, "
                                + "its bond. Taming only makes a horse yours - bond is whether it "
                                + "likes you, and the two are not the same thing. It is not genetic, "
                                + "it is not inherited, and it is kept per horse and per owner. You "
                                + "can read it on the horse's information screen.",
                        "It goes up by you being there. A horse within about ten blocks of its owner "
                                + "gains a point every couple of minutes, and one you are riding gains "
                                + "a point every forty seconds or so - bareback counts exactly as much "
                                + "as saddled. Feeding by hand is worth two at once, four for a "
                                + "favourite, and grooming is worth two. A gelding earns a quarter "
                                + "more. Nothing ever takes bond away again.",
                        "There is a ceiling of fifteen a day, and it is deliberate. Filling the bar "
                                + "takes a week or so of a horse's life however hard you work at it, "
                                + "because bonding is meant to be the thing that happens while you "
                                + "play rather than an afternoon of standing still. A foal in a herd "
                                + "learns you twice as fast, and starts life already a quarter of the "
                                + "way to wherever its dam had got to.",
                        "What it buys is behaviour, in three steps. Under 31 it is wary and behaves "
                                + "like any other horse. At 31 it turns attentive: it watches you, "
                                + "turning its head to follow you around the paddock. At 61 it "
                                + "approaches - it will walk over to you when it can find a route. At "
                                + "81 it follows you at a walk and stops a few paces short.",
                        "A following horse is really walking, not teleporting. A fence holds it, a "
                                + "closed gate holds it, and it needs a path it can actually take - so "
                                + "a horse that has lost you will stop and wait rather than swim a "
                                + "river after you. Past about thirty blocks it gives up and stands. A "
                                + "horse being ridden or led is doing what it is told instead, so none "
                                + "of this applies until you get off."),
                List.of(new ItemStack(Items.WHEAT), new ItemStack(Items.SUGAR),
                        new ItemStack(Items.SHEARS)), Art.NONE,
                List.of(ProgressTask.BOND_HORSE, ProgressTask.BOND_ATTENTIVE,
                        ProgressTask.BOND_APPROACHES, ProgressTask.BOND_FOLLOWS)));

        husbandry.add(new Step("Stalls, pens and tickets",
                List.of("A stall sign starts blank. Right-click a horse of yours with it and it binds "
                                + "to that horse; hang it on a vertical wall and the room behind it "
                                + "becomes that horse's stall. The sign measures the room and tells you "
                                + "how big it came out.",
                        "It has to be a room. If nothing encloses it, the sign refuses to go up rather "
                                + "than guess, and you keep it. Doors, trapdoors and gates count as the "
                                + "edge whether they are open or shut, and a stall needs no roof. One "
                                + "sign per horse: hang a second and the stall moves, and the old sign "
                                + "pops off for you to pick up.",
                        "Be clear about what a stall is for, because it is easy to assume otherwise: "
                                + "it does nothing for the horse standing in it. No comfort, no "
                                + "healing, no bonus of any kind. It is an address - somewhere a ticket "
                                + "can send the horse.",
                        "Tickets are that delivery. A blank ticket is only the crafting base. A basic "
                                + "ticket works within the overworld, a bound ticket anywhere inside "
                                + "one world, and an interdimensional ticket from anywhere to anywhere. "
                                + "Use one on a horse of yours that has a stall and it goes there.",
                        "A holding pen is the same trick bound to you instead of to a horse. Hang a "
                                + "holding pen sign on an enclosed pen and a holding pen ticket sends "
                                + "any horse you own there, from any world, whether or not it has a "
                                + "stall of its own. That is where the horse you tamed a minute ago "
                                + "goes.",
                        "Tickets are single use and honest about it. Get off the horse first, and if "
                                + "it cannot fit where it is going then nothing is spent and nothing "
                                + "moves."),
                List.of(new ItemStack(ModItems.STALL_SIGN.get()),
                        new ItemStack(ModItems.HOLDING_PEN_SIGN.get()),
                        new ItemStack(ModItems.BASIC_TICKET.get()),
                        new ItemStack(ModItems.INTERDIMENSIONAL_TICKET.get()),
                        new ItemStack(ModItems.HOLDING_PEN_TICKET.get())), Art.NONE,
                List.of(ProgressTask.BUILD_STALL, ProgressTask.HANG_PEN_SIGN,
                        ProgressTask.CRAFT_BLANK_TICKET,
                        ProgressTask.USE_TICKET, ProgressTask.USE_BOUND_TICKET,
                        ProgressTask.USE_INTERDIMENSIONAL_TICKET, ProgressTask.USE_PEN_TICKET)));

        husbandry.add(new Step("Whistles",
                List.of("Three whistles call every tamed horse you own that is standing within reach: "
                                + "sixteen blocks for the basic one, thirty-two for the golden, "
                                + "sixty-four for the echo. They differ in nothing but range. A horse "
                                + "on a lead comes anyway and drops the lead as it goes; a horse "
                                + "somebody is riding stays where it is.",
                        "The ender whistle is a different thing. Bind it to one horse you own and it "
                                + "will come from anywhere at all, across dimensions, as often as you "
                                + "like. It never rebinds to another horse, and when that horse dies "
                                + "the whistle crumbles in your hands.",
                        "None of them care how much a horse likes you."),
                List.of(new ItemStack(ModItems.BASIC_WHISTLE.get()),
                        new ItemStack(ModItems.GOLDEN_WHISTLE.get()),
                        new ItemStack(ModItems.ECHO_WHISTLE.get()),
                        new ItemStack(ModItems.ENDER_WHISTLE.get())), Art.NONE,
                List.of(ProgressTask.WHISTLE_BASIC, ProgressTask.WHISTLE_GOLDEN,
                        ProgressTask.WHISTLE_ECHO, ProgressTask.WHISTLE_ENDER)));

        husbandry.add(new Step("Dyeing your tack",
                List.of("An Equestrian Bench is a saddler's bench - a crafting table, a piece of "
                                + "leather and two horse hair. There is one standing at every cowboy's "
                                + "homestead as well, so you may meet one before you know to want it.",
                        "Open it and you get a slot for the tack, a name field, and three labelled "
                                + "rows. It works on a saddle and on leather horse armour and nothing "
                                + "else - iron, gold and diamond horse armour cannot be dyed at all.",
                        "The three rows colour separately. The seat is the leather you sit on and the "
                                + "bridle is the headstall, noseband and reins; both take any of the "
                                + "sixteen dyes, and they are independent, so a black saddle with a red "
                                + "bridle is two passes rather than a compromise. The fittings are the "
                                + "buckles and rings, and they take a material rather than a dye - "
                                + "iron, gold, copper, netherite, diamond, emerald, amethyst, quartz, "
                                + "redstone, ender pearl, basalt, bone, prismarine, lapis or coal. You "
                                + "are not painting the hardware, you are saying what it is made of.",
                        "An empty row means leave that one alone, not make it colourless. You are only "
                                + "charged for the parts that actually changed, and a row already the "
                                + "colour you asked for costs nothing. A dye will not colour metal and "
                                + "an ingot will not colour leather - in either case no result appears "
                                + "at all, rather than a material quietly vanishing.",
                        "Iron fittings are the way back to plain. The name field writes on the tack "
                                + "and travels with it, so a tack room full of saddles is a tack room "
                                + "you can read - and hovering any piece lists all three of its "
                                + "colours, which the icon alone cannot show you."),
                List.of(new ItemStack(ModItems.EQUESTRIAN_BENCH.get()), new ItemStack(Items.SADDLE),
                        new ItemStack(Items.LEATHER_HORSE_ARMOR), new ItemStack(Items.RED_DYE),
                        new ItemStack(Items.GOLD_INGOT)), Art.NONE,
                List.of(ProgressTask.DYE_TACK)));

        husbandry.add(new Step("Milk a mare",
                List.of("Right-click a tamed adult mare with an empty bucket. Stallions decline, and "
                                + "are not shy about it.",
                        "How often a mare can be milked is itself genetic - most manage it once a day, "
                                + "and there is a locus that makes some of them far more generous. A "
                                + "few horses give something other than milk entirely, which is a thing "
                                + "worth finding out about the hard way."),
                List.of(new ItemStack(Items.BUCKET), new ItemStack(Items.MILK_BUCKET)), Art.NONE,
                List.of(ProgressTask.MILK_MARE)));

        out.add(new Chapter("Husbandry", List.copyOf(husbandry)));

        // ------------------------------------------------------------------
        // 2. Basic breeding - heats, covering and jars. No golden carrots:
        // the owner's call, so the first breeding a player does is the one the
        // horses arrange themselves.
        // ------------------------------------------------------------------
        List<Step> breeding = new ArrayList<>();

        breeding.add(new Step("Heat, and letting them get on with it",
                List.of("Put an entire stallion in with a mare and, sooner or later, they will see to "
                                + "it themselves. No carrot and no hearts you have to trigger - he "
                                + "walks over, courts her for a few seconds, and covers her.",
                        "A mare cycles. She comes into heat, stays in heat a while, goes out of it for "
                                + "about as long, and round again. He can only cover her while she is "
                                + "in heat, and only once per heat - if it does not take, she tries "
                                + "again next time round.",
                        "A good deal has to be true at the moment it happens. Both horses have to be "
                                + "in good health - nine-tenths or better, so one that has been hurt "
                                + "needs to mend first. Neither can be ridden or on a lead. A mare with "
                                + "eight or more horses crowded within sixteen blocks will not be "
                                + "covered at all, so a packed paddock quietly stops breeding by "
                                + "itself. And a stallion manages three covers in a day, after which "
                                + "he is done until tomorrow.",
                        "That crowding rule is also why wild herds so rarely produce foals on their "
                                + "own: they live close together. A paddock of your own with a little "
                                + "room in it does far better."),
                List.of(new ItemStack(Items.LEAD), new ItemStack(Items.HAY_BLOCK)), Art.NONE,
                List.of(ProgressTask.NATURAL_COVER)));

        breeding.add(new Step("Reading a mare with a vet's kit",
                List.of("A vet's kit is shears, leather and an iron ingot, and it lasts sixty-four "
                                + "procedures. The horseman sells them too.",
                        "Use it on a mare and she tells you where she is: in heat, and whether this is "
                                + "the better half of it; out of heat, and how long until the next one; "
                                + "pregnant, and how long to go; or newly foaled and waiting on her "
                                + "foal heat. Use it on a stallion and he tells you how many of his "
                                + "three covers he has made today.",
                        "It is the only thing in the game that will tell you a mare is carrying twins. "
                                + "The information screen deliberately will not."),
                List.of(new ItemStack(ModItems.VET_KIT.get())), Art.NONE,
                List.of(ProgressTask.VET_KIT_USE)));

        breeding.add(new Step("Pregnancy, foaling and twins",
                List.of("A cover that takes makes her pregnant rather than handing you a foal. She "
                                + "carries, slows down noticeably towards the end of it, and then "
                                + "foals. While she is carrying she cannot be bred again and will turn "
                                + "breeding food away.",
                        "Now and then a pregnancy is twins. Usually both live; sometimes one is lost, "
                                + "and rarely both. Whether a mare throws twins at all is in her own "
                                + "genes, so it runs in families.",
                        "After foaling she nurses, and comes back into heat shortly afterwards - the "
                                + "foal heat - so a mare can be covered again quite soon. Keep a foal "
                                + "far from its dam for a day and she weans it.",
                        "The foal is not a copy of either parent. Each parent passes on one of its two "
                                + "copies of every gene, and which one is a coin flip - which is why "
                                + "breeding the same pair twice gives you two different horses, and why "
                                + "breeding is worth doing at all.",
                        "How long all of this takes is a server setting. Out of the box every stage is "
                                + "about a Minecraft day; turned up, a pregnancy can run most of a real "
                                + "mare's eleven months."),
                List.of(), Art.NONE,
                List.of(ProgressTask.BREED_FOAL)));

        breeding.add(new Step("Seed jars",
                List.of("A seed jar lets a stallion sire a foal on a mare he has never met. The empty "
                                + "one is a glass bottle and horse hair.",
                        "Filling it is the owner's act: get an entire stallion of yours interested in "
                                + "breeding, then use the empty jar on him. The jar takes his "
                                + "genetics, his name and his breed with it - and it counts as one of "
                                + "his three covers for that day.",
                        "Using it is simpler: use the filled jar on a mare you own while she is in "
                                + "heat. Out of heat she refuses it and you keep the jar. In heat it is "
                                + "spent whether or not it takes, because it was a real attempt.",
                        "The mare has to be yours. The stallion does not, and that is the point - a "
                                + "jar that changes hands is how a bloodline travels between players. "
                                + "The horseman buys filled ones."),
                List.of(new ItemStack(ModItems.EMPTY_SEED_JAR.get()),
                        new ItemStack(ModItems.STALLION_SEED_JAR.get())), Art.NONE,
                List.of(ProgressTask.FILL_SEED_JAR, ProgressTask.USE_SEED_JAR)));

        breeding.add(new Step("Gelding",
                List.of("Crouch and use a vet's kit on a stallion of your own and he becomes a "
                                + "gelding. It is permanent - there is nothing anywhere that undoes it "
                                + "- and it follows him through sale and through death, because it is "
                                + "written on his record rather than on the animal.",
                        "A gelding sires nothing and fills no jar. He keeps company the way a mare "
                                + "does, stands outside the politics of a band, and settles to you "
                                + "about a quarter faster than a stallion would. Cowboys sell a good "
                                + "many of them, cheaper than an entire horse.",
                        "It is the ordinary answer to a paddock with too many colts in it."),
                List.of(new ItemStack(ModItems.VET_KIT.get())), Art.NONE,
                List.of(ProgressTask.GELD_HORSE)));

        out.add(new Chapter("Basic breeding", List.copyOf(breeding)));

        // ------------------------------------------------------------------
        // 3. Genetics.
        // ------------------------------------------------------------------
        List<Step> genetics = new ArrayList<>();

        genetics.add(new Step("Taking a gene off a horse",
                List.of("Hold a book and right-click a horse. The book records one of that horse's "
                                + "genes at random and becomes a research paper naming it. You cannot "
                                + "pick which, and the horse keeps the gene.",
                        "A completely ordinary horse has nothing to give, and will not cost you the "
                                + "book.",
                        "The paper is not something you read. It is a component: file it in a shelf to "
                                + "copy it, or spend it on a gene carrot."),
                List.of(new ItemStack(Items.BOOK), new ItemStack(ModItems.RESEARCH_PAPER.get())),
                Art.NONE, List.of(ProgressTask.GENE_BOOK)));

        genetics.add(new Step("Genes are earned, not read",
                List.of("A gene joins your database when you have owned a living horse that carries "
                                + "it - tamed, bred, bought, or given to you. Not from paper, and not "
                                + "from looking at one in a field. Knowing a gene is also what unlocks "
                                + "its gene carrot.",
                        "So a paper and a horse do different jobs. A paper is a thing you can copy and "
                                + "spend; a horse is the only thing that can teach you.",
                        "Every horse carries two copies of every gene, and the Genes tab of its "
                                + "information screen lists them side by side. When the two copies "
                                + "differ, the horse is carrying something it may not be showing - and "
                                + "that is the single most useful fact in this book."),
                List.of(), Art.NONE, List.of(ProgressTask.DISCOVER_GENE)));

        genetics.add(new Step("Keep them on a shelf",
                List.of("A research paper is a thing you can lose. File it into an Equine Research "
                                + "Shelf - a bookshelf and two horse hair - and it stops being one: "
                                + "that shelf will copy the gene onto blank books for as long as the "
                                + "original stays in it.",
                        "Copying is not instant. A common gene takes about ten seconds and a truly "
                                + "rare one about a minute, so a shelf is something you set going. "
                                + "Break the shelf and every paper in it drops, so moving one costs you "
                                + "nothing.",
                        "This is what makes knowledge worth having rather than worth hoarding. A gene "
                                + "on your shelf is a gene you can hand to a friend as many times as "
                                + "they have books."),
                List.of(new ItemStack(ModItems.EQUINE_RESEARCH_SHELF.get()),
                        new ItemStack(Items.BOOK),
                        new ItemStack(ModItems.RESEARCH_PAPER.get())), Art.NONE,
                List.of(ProgressTask.BUILD_SHELF, ProgressTask.FILE_PAPER, ProgressTask.COPY_PAPER)));

        genetics.add(new Step("Putting a gene into a foal",
                List.of("Once a gene is in your database you can make its gene carrot - a golden "
                                + "carrot, that gene's research paper, some horse hair, and an ingot "
                                + "whose metal depends on how rare the gene is.",
                        "Feed the carrot to one of the parents, any time. It waits on that horse until "
                                + "the next breeding that actually takes, and then that parent passes "
                                + "the gene on instead of leaving it to a coin flip. Whether the foal "
                                + "shows it still depends on what the other parent brought - some genes "
                                + "need two copies before they do anything at all.",
                        "Carrots can be combined by crafting them together into one carrot carrying "
                                + "several effects, so long as they do not contradict one another. The "
                                + "Recipes tab has every recipe the mod adds, with the gene carrots on "
                                + "their own list."),
                List.of(new ItemStack(ModItems.KNOWN_GENE_SPLICE_CARROT.get()),
                        new ItemStack(ModItems.HORSE_HAIR.get()),
                        new ItemStack(Items.GOLD_INGOT),
                        new ItemStack(Items.DIAMOND)), Art.NONE,
                List.of(ProgressTask.GENE_CARROT, ProgressTask.USE_GENE_CARROT)));

        genetics.add(new Step("The splice carrots",
                List.of("Four carrots work without naming a gene at all. A stabilizer makes the fed "
                                + "parent pass on its dominant copy everywhere it has a choice; a "
                                + "magnifier does the opposite and passes the recessive one. Those two "
                                + "are how you fix a type quickly, or dig out what a horse has been "
                                + "hiding.",
                        "An unknown gene splice rolls one random gene and redraws that parent's "
                                + "contribution to it. An unknown epigenetic splice leaves the genes "
                                + "alone and re-rolls the fine detail instead - the exact bay points, "
                                + "the dapples, the edges of a splash.",
                        "Nothing random can hurt a foal: the random splices draw from a pool that "
                                + "excludes the lethal genes. The deliberate route to a dangerous one "
                                + "is the named carrot, which is as it should be.",
                        "Five more splice carrots narrow the roll to a theme - the dilutions, the "
                                + "white and spotting genes, the markings, performance, or the magical "
                                + "genes."),
                List.of(new ItemStack(ModItems.STABILIZER_CARROT.get()),
                        new ItemStack(ModItems.MAGNIFIER_CARROT.get()),
                        new ItemStack(ModItems.UNKNOWN_GENE_SPLICE_CARROT.get()),
                        new ItemStack(ModItems.UNKNOWN_EPIGENETIC_SPLICE_CARROT.get())), Art.NONE,
                List.of(ProgressTask.CARROT_STABILIZER, ProgressTask.CARROT_MAGNIFIER,
                        ProgressTask.CARROT_UNKNOWN_GENE, ProgressTask.CARROT_UNKNOWN_EPIGENETIC,
                        ProgressTask.CARROT_DILUTION, ProgressTask.CARROT_WHITE,
                        ProgressTask.CARROT_MARKING, ProgressTask.CARROT_PERFORMANCE,
                        ProgressTask.CARROT_MAGICAL)));

        genetics.add(new Step("A door made of hay",
                List.of("Build a frame of hay bales the way you would build a nether portal, light it "
                                + "with a golden carrot, and stand in it for ten seconds.",
                        "On the other side is a corridor of two thousand pens, each holding a mare and "
                                + "a stallion of one rolled genotype, each signed with the genes they "
                                + "carry. It is a showroom rather than a place to live - it is there so "
                                + "you can see what a gene actually looks like on a horse without "
                                + "breeding for a week to find out.",
                        "Bring your own horses through and they come home with you."),
                List.of(new ItemStack(Items.HAY_BLOCK), new ItemStack(Items.GOLDEN_CARROT)), Art.NONE,
                List.of(ProgressTask.LIGHT_PORTAL, ProgressTask.ENTER_DIMENSION,
                        ProgressTask.BRING_HORSE_HOME)));

        out.add(new Chapter("Genetics", List.copyOf(genetics)));

        // ------------------------------------------------------------------
        // 4. Breeding projects - doing it on purpose.
        // ------------------------------------------------------------------
        List<Step> projects = new ArrayList<>();

        projects.add(new Step("Breeding for a combination",
                List.of("This is the loop the whole mod is built around. Meet horses, take what they "
                                + "will tell you, file it, copy it, splice it, and breed the result "
                                + "into the next generation. Every horse in the paddock gets a little "
                                + "more like the one you had in mind.",
                        "Most foals carry something they do not show - two different copies at some "
                                + "locus - and that is not a failure, it is the raw material. A "
                                + "plain-looking foal out of two interesting parents is very often the "
                                + "most valuable animal you own.",
                        "The Breeding preview tab on this screen reads a mare and a stallion together "
                                + "and tells you what they could make, locus by locus, before you "
                                + "commit a season to it.",
                        "Nothing stops you splicing your way to something spectacular in an afternoon "
                                + "- but anybody can do that, and a spliced horse says so on its own "
                                + "paperwork. Getting there by breeding is the part that makes it "
                                + "yours."),
                List.of(), Art.NONE, List.of(ProgressTask.FOAL_HETEROZYGOUS)));

        projects.add(new Step("Breeding true",
                List.of("A horse with two copies of the same allele breeds true for it: every foal it "
                                + "has gets that copy, whatever the other parent brings. That is the "
                                + "difference between having a trait somewhere in your herd and being "
                                + "able to rely on it.",
                        "Getting there means putting two horses that both carry the thing together and "
                                + "accepting that only some of the foals will be what you wanted. There "
                                + "is no shortcut to it that is not a carrot."),
                List.of(), Art.NONE, List.of(ProgressTask.FOAL_HOM_DOMINANT)));

        projects.add(new Step("Line breeding, and what it costs",
                List.of("Recessives are the hard ones. A gene that needs two copies before it shows "
                                + "can sit in a line for generations without ever appearing, and the "
                                + "usual way to bring one out is to breed relatives back together - a "
                                + "horse to its half-sibling, a granddaughter back to the sire.",
                        "Be clear about what the game does and does not do here. There is no "
                                + "inbreeding penalty in this mod. Nothing tracks how closely two "
                                + "horses are related, nothing warns you, no foal is born weaker for "
                                + "it, and no stallion refuses his own daughter. Line breeding costs "
                                + "you nothing mechanically.",
                        "What it costs is real all the same. Doubling up on the recessive you wanted "
                                + "doubles up on every recessive you did not, and some of the ones in "
                                + "this mod are genuinely bad for the horse that gets two. A wide herd "
                                + "and decent records are the only defence, and that is a matter of how "
                                + "you choose to play rather than a rule the game will enforce for "
                                + "you."),
                List.of(), Art.NONE, List.of(ProgressTask.FOAL_HOM_RECESSIVE)));

        out.add(new Chapter("Breeding projects", List.copyOf(projects)));

        // ------------------------------------------------------------------
        // 5. Horses in the wild.
        // ------------------------------------------------------------------
        List<Step> wild = new ArrayList<>();

        wild.add(new Step("Where the breeds are",
                List.of("Wild herds are not the same everywhere. Breeds belong to the country they "
                                + "came from, and that country decides which biomes they turn up in - "
                                + "the heavy horses to the cold north, the desert breeds to the "
                                + "savanna, the ponies to the hills. A horse you cannot find at home "
                                + "may be three biomes away, and some of them only come out at a "
                                + "particular time of day.",
                        "Tame one of a named breed and it joins your breed book on this screen. "
                                + "Everything you have not met yet sits under question marks until you "
                                + "do.",
                        "There are stables out there too, generated with the world and already full of "
                                + "horses somebody else bred. They are worth finding: the animals in "
                                + "them are better than anything wandering loose, and nothing whatever "
                                + "stops you simply taking them.",
                        "If your own corner of the world will not give you a breed, the horseman "
                                + "sells breed spawn eggs from his middle ranks - one right-click, one "
                                + "foundation horse of that breed, and a line to build from."),
                List.of(new ItemStack(ModItems.BREED_SPAWN_EGG.get())), Art.NONE,
                List.of(ProgressTask.DISCOVER_BREED, ProgressTask.BREED_EGG)));

        wild.add(new Step("Bands, and horses that know each other",
                List.of("Horses keep opinions of each other as well as of you: who they know, who "
                                + "they groom with, who gives way to whom, and who they cannot stand. "
                                + "The Social section of a horse's information screen shows its place "
                                + "in its band, its closest companions and its rival.",
                        "Wild horses live the way free-roaming horses do. A family band is a stallion "
                                + "with his mares and their foals, and it goes where its eldest mare "
                                + "goes - he keeps to the edge, fetches a mare who wanders off, and "
                                + "stands between his mares and any other stallion. Stallions without "
                                + "mares run together as bachelors.",
                        "Young horses leave home a few days after they grow up: colts to the "
                                + "bachelors, fillies to another band - and a filly will not take a "
                                + "suitor who shares a parent with her. Now and then a bachelor "
                                + "challenges a band stallion for his mares. It is a real fight, but "
                                + "nobody dies: the loser yields well before it gets that far and walks "
                                + "away, and the winner takes the band.",
                        "Your own horses band up too. Leave two of them standing together long enough "
                                + "and they form a herd, which mends twice as fast and teaches a foal "
                                + "twice as quickly. Tamed stallions who know each other spar without "
                                + "doing harm, grooming partners stand head to tail, and a mare will go "
                                + "for anything that hurts her foal - though a tamed one will never go "
                                + "for you."),
                List.of(new ItemStack(Items.HAY_BLOCK), new ItemStack(Items.LEAD)), Art.NONE,
                List.of(ProgressTask.FORM_HERD)));

        out.add(new Chapter("Horses in the wild", List.copyOf(wild)));

        // ------------------------------------------------------------------
        // 6. The villagers.
        // ------------------------------------------------------------------
        List<Step> people = new ArrayList<>();

        people.add(new Step("The horseman",
                List.of("The horseman is a villager with a job you will not have seen before. Put a "
                                + "Horseman's Table down near one who has no work and they will take "
                                + "it.",
                        "He buys horse hair from his first day, which makes him the easiest early "
                                + "trade in the mod, and works up through rope and cloth. He sells "
                                + "tack, a whistle and blank tickets early on, then vet's kits, then "
                                + "breeding carrots and breed spawn eggs - and at the top of his trade "
                                + "he deals in research papers and buys filled seed jars.",
                        "Buying a paper is not the same as knowing the gene - see above - but it is "
                                + "how you get one for a horse you have never met."),
                List.of(new ItemStack(ModItems.HORSEMANS_TABLE.get()),
                        new ItemStack(Items.EMERALD)), Art.HORSEMAN,
                List.of(ProgressTask.TRADE_HORSEMAN)));

        people.add(new Step("The cowboy",
                List.of("The cowboy is not a villager at all, and has no profession. A Cowboy Hitch "
                                + "brings one: he sets up a homestead out where the horses are, with a "
                                + "paddock and a string of four to ten horses of his own.",
                        "He sells them outright, if you would rather buy a bloodline than breed one. "
                                + "About half his males are geldings, and they come cheaper.",
                        "He deals only in the breeds of the country he came from. A cowboy out of one "
                                + "part of the world will not sell you something from the other side of "
                                + "it however common it is - so two cowboys in different directions are "
                                + "worth more than one, and hunting a particular breed can mean finding "
                                + "the right man rather than the right biome.",
                        "The two of them come as a pair - they built their posts side by side, and the "
                                + "story in the villages is that they are married. His homestead is "
                                + "also where you will find an Equestrian Bench already standing."),
                List.of(new ItemStack(ModItems.COWBOY_HITCH.get()),
                        new ItemStack(Items.EMERALD)), Art.COWBOY,
                List.of(ProgressTask.MEET_COWBOY)));

        people.add(new Step("Transfer papers",
                List.of("A horse does not change hands by being handed over. Sign a blank transfer "
                                + "paper against a horse you own and it becomes a claim on that one "
                                + "animal; whoever holds the paper can walk up to the horse and take "
                                + "it.",
                        "That is how the cowboy sells you a horse, and it is how you sell one to "
                                + "another player. The horse stays where it is standing until somebody "
                                + "comes for it.",
                        "Ownership moves. Who bred it never does - a horse carries its breeder's name "
                                + "for the whole of its life, and so does every foal out of it."),
                List.of(new ItemStack(ModItems.BLANK_TRANSFER_PAPER.get()),
                        new ItemStack(ModItems.SIGNED_TRANSFER_PAPER.get())), Art.NONE,
                List.of(ProgressTask.TRANSFER_PAPER)));

        out.add(new Chapter("The villagers", List.copyOf(people)));

        return List.copyOf(out);
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    /**
     * <b>Draw one sub-chapter</b> into {@code (x, y, w)} and return the height it
     * wanted. The caller scissors and scrolls; this only lays out, and the
     * checklist items underneath are drawn by the browser, which is what knows
     * what the player has done.
     */
    public static int drawStep(GuiGraphicsExtractor g, Font font, Step step, int x, int y, int w,
                               int mouseX, int mouseY, int headingColour, int bodyColour) {
        int cy = y;
        g.text(font, Component.literal(step.heading()), x, cy, headingColour, false);
        cy += font.lineHeight + 6;

        int textW = step.art() == Art.NONE ? w : w - ART - 8;
        int textTop = cy;
        for (String paragraph : step.paragraphs()) {
            for (String line : GuiText.wrap(font, paragraph, textW)) {
                g.text(font, Component.literal(line), x, cy, bodyColour, false);
                cy += font.lineHeight + 1;
            }
            cy += 6;
        }

        if (step.art() != Art.NONE) {
            int artX = x + w - ART;
            switch (step.art()) {
                case HORSEMAN -> TutorialPortraits.drawHorseman(g, artX, textTop, ART, mouseX, mouseY);
                case COWBOY -> TutorialPortraits.drawCowboy(g, artX, textTop, ART, mouseX, mouseY);
                default -> {
                }
            }
            cy = Math.max(cy, textTop + ART + 4);
        }

        if (!step.icons().isEmpty()) {
            int ix = x;
            for (ItemStack stack : step.icons()) {
                g.fill(ix - 1, cy - 1, ix + 17, cy + 17, 0x33FFFFFF);
                g.fakeItem(stack, ix, cy);
                ix += ICON + 4;
            }
            cy += ICON + 6;
        }
        return cy - y;
    }
}
