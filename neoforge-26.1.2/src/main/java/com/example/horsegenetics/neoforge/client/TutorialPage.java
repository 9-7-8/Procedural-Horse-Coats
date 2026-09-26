package com.example.horsegenetics.neoforge.client;

import com.example.horsegenetics.common.cart.CartKind;
import com.example.horsegenetics.common.progress.ProgressTask;
import com.example.horsegenetics.neoforge.carts.CartWood;
import com.example.horsegenetics.neoforge.carts.HorseCarts;
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
 * <h2>Seven chapters, and every sub-chapter ends in something to do</h2>
 * The page is <b>chapters of sub-chapters</b> (owner, roadmap &sect;24.5): horses
 * in the wild, husbandry, basic breeding, genetics, magical horses, breeding
 * projects, the villagers. Each sub-chapter carries <b>at least one
 * {@link ProgressTask}</b> - the checklist is not a separate page any more, it is
 * the last thing in the sub-chapter that teaches it. That is what lets a
 * sub-chapter be <i>finished</i> and say so in the contents list, and it is why a
 * reader who has just been told how to hang a stall sign finds the box for it
 * directly underneath rather than on a tab they have to know to look at.
 *
 * <p>The chapter order is a teaching order, not a difficulty order. <b>The wild
 * comes first</b>, because every horse a player meets is wild for as long as it
 * takes them to notice that horses here have a life of their own - bands, ranks,
 * grooming partners, stallions driving a mare back in. Taming lives in that
 * chapter for the same reason: you tame a <i>wild</i> horse, and husbandry is
 * what starts once it is yours. Husbandry then mentions breeding nowhere, because
 * keeping a horse alive and content is a whole game before genetics is, and a
 * player who meets heats and punnett squares in their first ten minutes puts the
 * mod down.
 *
 * <h2>Some boxes tick because you watched, not because you acted</h2>
 * Most of the wild chapter is horses doing things to each other, which a player
 * cannot perform. Those tasks complete through
 * {@code HorseProgress.completeForWatcher} - you were near enough, and looking.
 * Their titles say <i>watch</i> for that reason: an unticked box that reads like
 * an instruction, for something you cannot instruct, reads as a bug.
 *
 * <h2>Why the pictures are entities and items, not art</h2>
 * Everything illustrated here is drawn from the game: item stacks through the
 * item renderer, and the equestrian and the cowboy as real entities
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
 * stale in several places. Worth keeping straight, because the obvious thing to
 * write is wrong: a natural cover needs <b>nine-tenths</b> health and not full
 * health; a horse heals only near <b>water</b> and the food half of that gate is
 * gone; there is <b>no inbreeding penalty of any kind</b>, so the line-breeding
 * sub-chapter says so outright; sparring and displacement do <b>no damage
 * whatever</b>; a takeover fight is capped rather than lethal; and nine dhampirs
 * in ten are the seal brown strain, so "a dhampir burns in daylight" is true of
 * only one in ten of them.
 */
public final class TutorialPage {

    /** One illustration beside a step. */
    public enum Art {
        NONE,
        EQUESTRIAN,
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

    /** The eight chapters, in page order. Built once per session; item stacks need the registry. */
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
        // 1. Horses in the wild - what is out there before any of it is
        // yours, and how to get the first one. Most of this chapter is
        // watched rather than done.
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
                        "If your own corner of the world will not give you a breed, the equestrian "
                                + "scientist sells breed spawn eggs from his middle ranks - one right-click, one "
                                + "foundation horse of that breed, and a line to build from."),
                List.of(new ItemStack(ModItems.BREED_SPAWN_EGG.get())), Art.NONE,
                List.of(ProgressTask.DISCOVER_BREED, ProgressTask.BREED_EGG)));

        wild.add(new Step("Coming to food",
                List.of("A wild horse notices food in your hand from about ten blocks and drifts over "
                                + "to it, stopping a couple of blocks short. Put the food away and it "
                                + "loses interest that instant - it was never coming to you, it was "
                                + "coming to the wheat.",
                        "Every horse also has one food it likes above all others, and that one it "
                                + "notices from twice as far. It does not amble for a favourite. A horse "
                                + "crossing twenty blocks at a flat gallop because of what is in your "
                                + "hand is how you find out what a particular animal loves, and it is "
                                + "worth knowing: a favourite is double the bond, two hearts of "
                                + "healing, and a minute of being quick on its feet.",
                        "What a horse will eat at all is genetic. Most eat what you would expect, but "
                                + "there are horses that want only meat, or only fish, or only metal, "
                                + "and one kind that ignores everything you could possibly be holding. "
                                + "A horse that will not look at a carrot is telling you something "
                                + "about itself."),
                List.of(new ItemStack(Items.WHEAT), new ItemStack(Items.GOLDEN_CARROT),
                        new ItemStack(Items.APPLE)), Art.NONE,
                List.of(ProgressTask.WILD_FOOD_DRIFT, ProgressTask.WILD_FAVOURITE_RUN)));

        wild.add(new Step("Taming a wild horse",
                List.of("The old way works. Find a wild horse and get on it with nothing in your hand. "
                                + "It will throw you off. Get back on. Keep getting back on until hearts "
                                + "appear. Feeding it first makes it quicker to convince, and a saddle "
                                + "lets you steer it once it is yours.",
                        "There is a quieter way that never involves being thrown at all. Crouch, hold "
                                + "food the horse eats, look at it, and stand still. It will walk over "
                                + "of its own accord and take a mouthful every couple of seconds, and "
                                + "every mouthful is a chance at taming - the same chance the riding "
                                + "does. Stand up, look away, or walk off and it stops coming.",
                        "Foals cannot be tamed by either method. They can be bred, bought and led, but "
                                + "a foal is too young to be anybody's."),
                List.of(new ItemStack(Items.SADDLE), new ItemStack(Items.WHEAT),
                        new ItemStack(Items.GOLDEN_APPLE)), Art.NONE,
                List.of(ProgressTask.TAME_MARE, ProgressTask.TAME_STALLION,
                        ProgressTask.CROUCH_FEED_TAME)));

        wild.add(new Step("When it turns on you",
                List.of("A wild horse is not livestock. Hit one and it fights back the way a wolf "
                                + "would: it comes for you, kicks about twice as often as a zombie "
                                + "swings, and rears after every blow that lands. It hits harder at "
                                + "monsters than at people, which is worth remembering the first time "
                                + "one saves you from a zombie without being asked.",
                        "It does not come alone. Every horse of that band within twenty-four blocks "
                                + "turns on you at the same moment, which is how a bad decision in the "
                                + "middle of a herd becomes a very bad one. Break line of sight for "
                                + "three seconds and they forget it entirely - there is no lasting "
                                + "grudge.",
                        "Frighten one instead and it simply runs. A horse already in a fight stands its "
                                + "ground rather than bolting out of it, and so does a sun-sensitive "
                                + "horse that has caught fire, because it has somewhere better to be.",
                        "None of this happens to a tamed horse of your own, and a tamed mare will never "
                                + "go for you even when you have hurt her foal."),
                List.of(new ItemStack(Items.IRON_SWORD), new ItemStack(Items.SHIELD)), Art.NONE,
                List.of(ProgressTask.WILD_KICK, ProgressTask.WILD_BOLT)));

        wild.add(new Step("Reading a wild horse",
                List.of("You do not have to own a horse to open it. Right-click any wild one and it "
                                + "stands still for you while you read it - and it really does stand "
                                + "still, so a horse you are inspecting is a horse you can look at "
                                + "properly.",
                        "The Social section is the one to read out here. It names the horse's role in "
                                + "its band - band stallion, lead mare, band mare, youngster, foal, "
                                + "bachelor lead, bachelor, or on its own - how many of the band-mates "
                                + "nearby it outranks, which horses it keeps company with, which of "
                                + "those is its grooming partner, and which single horse it cannot "
                                + "stand.",
                        "Read two or three horses of one band and the shape of the thing appears: who "
                                + "is in charge, who is on the way out, and which two are inseparable. "
                                + "Everything the rest of this chapter describes is already written "
                                + "there before you see it happen."),
                List.of(), Art.NONE,
                List.of(ProgressTask.WILD_READ_SOCIAL)));

        wild.add(new Step("Sparring",
                List.of("Two entire stallions who already know each other will square up, rear at one "
                                + "another in turn, and shove, for a few seconds at a time. Then one of "
                                + "them gives way and retreats a few blocks, and both of them remember "
                                + "how it went.",
                        "It does no damage. Not a little damage - none at all. Sparring is how rank "
                                + "gets settled without anybody getting hurt, and rank is what decides "
                                + "the next argument about water. Who wins is not random either: "
                                + "condition, size, age and current standing all weigh on it.",
                        "Your own tamed stallions do this too, once they know each other well enough."),
                List.of(), Art.NONE,
                List.of(ProgressTask.WILD_SPAR)));

        wild.add(new Step("Dams and foals",
                List.of("A foal keeps close to its dam - within about six blocks - and if it strays "
                                + "twice that far she breaks off whatever she was doing and goes to "
                                + "fetch it.",
                        "A wild foal that has lost its dam, or never knew her, adopts the nearest mare "
                                + "of its band, and she takes it on. There is no ceremony to it; the "
                                + "foal simply starts following somebody.",
                        "Hurt a foal in front of its dam and she will come for you, and she will keep "
                                + "coming for a while afterwards. A tamed mare defends her foal against "
                                + "everything in the world except you."),
                List.of(), Art.NONE,
                List.of(ProgressTask.WILD_DAM_FOAL)));

        wild.add(new Step("What a band stallion does",
                List.of("A family band is a stallion, his mares, and their foals, and it goes wherever "
                                + "its eldest mare goes. He does not lead it. He works the edges of it, "
                                + "further back than everybody else, and he has two jobs.",
                        "The first is keeping it together. When a mare wanders too far out he comes "
                                + "round <i>behind</i> her rather than at her, ears pinned, and drives "
                                + "her back in until she is where he wants her. The second is keeping "
                                + "other stallions off it: he puts himself physically between his mares "
                                + "and any strange stallion who comes within sixteen blocks, and if that "
                                + "stallion presses closer he charges him off.",
                        "Sooner or later a bachelor challenges him for the lot. That one is a real "
                                + "fight with real blows - and still nobody dies: the blow that would "
                                + "take either horse much below two-fifths of its health is the blow "
                                + "that ends the fight instead. The loser walks away sixteen blocks and "
                                + "is a bachelor from then on; the winner takes the band, mares and "
                                + "foals and all. A band whose stallion is simply gone is claimed with "
                                + "no fight at all.",
                        "Stallions with no mares of their own run together in bachelor bands, which is "
                                + "where the challengers come from, and where the losers go back to."),
                List.of(), Art.NONE,
                List.of(ProgressTask.WILD_BAND_STALLION, ProgressTask.WILD_TAKEOVER_FIGHT)));

        wild.add(new Step("Rank at the water",
                List.of("Rank is not decoration - it decides who drinks. A horse standing at water, or "
                                + "at a hay bale or a crop, can be walked up to by a horse that outranks "
                                + "it clearly enough; the higher one threatens, and the lower one steps "
                                + "aside.",
                        "Then it stops. One shove and the winner settles down to eat rather than "
                                + "pressing its advantage, and it will not try again on the same horse "
                                + "for a good half-minute. Like sparring, it costs nobody any health.",
                        "Plain grass is deliberately exempt. If every tuft in a field were worth "
                                + "arguing over, a band would do nothing else all day."),
                List.of(new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.HAY_BLOCK)), Art.NONE,
                List.of(ProgressTask.WILD_DISPLACE)));

        wild.add(new Step("Grooming and company",
                List.of("Horses keep opinions of each other as well as of you: who they know, who they "
                                + "groom with, who gives way to whom, and who they cannot stand.",
                        "Two horses who have grown close enough become grooming partners, and you will "
                                + "see it happen: they walk together, turn to stand head to tail, and "
                                + "work on each other for a long unhurried while. It forms between two "
                                + "adult mares, or between a dam and her foal - a stallion never gets "
                                + "one, and for this purpose a gelding counts as a mare, which is one of "
                                + "the quieter arguments for gelding a horse you mean to keep.",
                        "Young horses leave home a few days after they grow up: colts to the bachelors, "
                                + "fillies to another band, or to a bachelor who takes them on and "
                                + "founds a band of his own - her brother, who left the same place a "
                                + "minute earlier, as readily as any other. Mares move between bands "
                                + "now and then, to wherever they know the most horses.",
                        "Your own horses band up too. Leave two of them standing together long enough "
                                + "and they form a herd, which mends twice as fast and teaches a foal "
                                + "twice as quickly."),
                List.of(new ItemStack(Items.HAY_BLOCK), new ItemStack(Items.LEAD)), Art.NONE,
                List.of(ProgressTask.WILD_GROOM, ProgressTask.FORM_HERD)));

        wild.add(new Step("Eating, and hunting",
                List.of("A horse feeds itself. A hungry one goes looking within about ten blocks and "
                                + "eats the best thing it can reach - a hay bale for preference, then a "
                                + "cake if you were careless, then crops, then grass and moss and "
                                + "mushrooms and flowers. A horse that is only a little hungry nibbles "
                                + "whatever it happens to be standing on instead.",
                        "What it eats is gone. Grass and moss turn to dirt under a grazing horse the "
                                + "way they do under a sheep, and a crop is simply destroyed - so do not "
                                + "plant your wheat against the paddock fence.",
                        "And not every horse eats plants. A meat-eater with nothing available goes "
                                + "hunting: it walks down a cow or a pig or a chicken, kills it, and "
                                + "eats what drops. A fish-eater goes fishing. This is worth seeing "
                                + "once, and worth knowing before you put one in a paddock with your "
                                + "chickens."),
                List.of(new ItemStack(Items.HAY_BLOCK), new ItemStack(Items.WHEAT),
                        new ItemStack(Items.BEEF), new ItemStack(Items.COD)), Art.NONE,
                List.of(ProgressTask.WILD_GRAZE, ProgressTask.WILD_HUNT)));

        out.add(new Chapter("Horses in the wild", List.copyOf(wild)));

        // ------------------------------------------------------------------
        // 2. Husbandry - keeping a horse, and nothing about breeding.
        // ------------------------------------------------------------------
        List<Step> husbandry = new ArrayList<>();

        husbandry.add(new Step("What a horse is",
                List.of("Horses in this world are not four colours and a saddle. Each one carries a "
                                + "full set of genes, inherited from its parents the way a real horse's "
                                + "are, and its coat is painted from them - so no two are quite alike "
                                + "and none of them was drawn by hand.",
                        "Some of them are not entirely ordinary, either. There are horses that glow, "
                                + "horses that walk on water, and horses that are perfectly friendly "
                                + "until the sun goes down. There is a whole chapter on them further "
                                + "down; you will know one when you meet it.",
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
                List.of(new ItemStack(Items.NAME_TAG), new ItemStack(Items.SADDLE)), Art.NONE,
                List.of(ProgressTask.NAME_HORSE, ProgressTask.BARN_NAME)));

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

        husbandry.add(new Step("Hurt, hungry, and mended",
                List.of("Sooner or later a horse of yours gets hurt, and nothing will quietly fix it "
                                + "for you. This is the one piece of husbandry the mod genuinely "
                                + "changes: vanilla's slow regeneration is switched off for horses, so "
                                + "a hurt horse stays exactly as hurt as it is until you do something "
                                + "about it.",
                        "What it spends on mending is food. Every horse carries an invisible store of "
                                + "it, and healing costs two of that store per health point - so half a "
                                + "heart of damage is a mouthful of grass, and a badly hurt horse is a "
                                + "hay bale. Fill it up first: let the horse eat, or crouch and feed it "
                                + "by hand. A hay bale is the biggest single meal there is, and its "
                                + "favourite food is the one thing it will run across a field for.",
                        "Then stand it within three blocks of water and leave it alone. It mends about "
                                + "a health point a second - twice that if it is in a herd - and it "
                                + "stops dead the moment its food runs out, however much water is in "
                                + "front of it. A starving horse parked beside a lake will sit there "
                                + "hurt indefinitely, and that is the single most common reason somebody "
                                + "thinks healing is broken.",
                        "Two horses will not mend this way at all. A blood-drinker has to bite "
                                + "something living, and a horse on one of the narrow diets is fed "
                                + "straight back to full by its own food instead - one bar of gold, one "
                                + "cake, one potion or one bucket of lava, depending on the animal. "
                                + "Those horses do not need the water at all.",
                        "The vet's kit does not heal anything, and never has. It reads a mare and it "
                                + "gelds a stallion."),
                List.of(new ItemStack(Items.HAY_BLOCK), new ItemStack(Items.WATER_BUCKET),
                        new ItemStack(Items.GOLDEN_CARROT)), Art.NONE,
                List.of(ProgressTask.HORSE_INJURED, ProgressTask.FEED_HUNGRY_HORSE,
                        ProgressTask.HEAL_TO_FULL)));

        husbandry.add(new Step("Grooming, and horse hair",
                List.of("Right-click an adult horse with shears. You are brushing it rather than "
                                + "shearing it: the horse enjoys it, you get one to three horse hair, "
                                + "and it is worth two bond. Once a day, per horse.",
                        "Do this early and often. Horse hair is the base material for nearly "
                                + "everything this mod adds - the whistles, the stall signs and the "
                                + "tickets, the seed jars, the transfer papers, the research shelf, "
                                + "the vet's kit, the equestrian posts and every breeding carrot. "
                                + "Almost nothing else can be made without it.",
                        "It goes in raw. There is no bundling or braiding step to get through "
                                + "first - whatever a recipe wants hair for, it wants hair."),
                List.of(new ItemStack(Items.SHEARS), new ItemStack(ModItems.HORSE_HAIR.get())),
                Art.NONE,
                List.of(ProgressTask.SHEAR_HORSE)));

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
                        ProgressTask.BOND_APPROACHES, ProgressTask.BOND_FOLLOWS,
                        ProgressTask.RIDE_BAREBACK, ProgressTask.STEER_BAREBACK)));

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
                List.of(ProgressTask.BUILD_STALL, ProgressTask.STALL_DOUBLE_GATE,
                        ProgressTask.HANG_PEN_SIGN,
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
                List.of("A Tack Dyeing Bench is a saddler's bench - a crafting table, a piece of "
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
        // 3. Farming and haulage - the carts, and the first chapter that
        // teaches a *stat* rather than a procedure. It sits here, straight
        // after Husbandry, because everything in it needs a tamed saddled
        // horse and nothing in it needs genetics: pulling ability is
        // introduced from scratch below, as a thing you can see on a screen
        // and feel on a road, long before Genetics explains where it came
        // from. One sub-chapter per vehicle, each finished by actually
        // driving that vehicle - the owner's call, 2026-09-18.
        // ------------------------------------------------------------------
        List<Step> farming = new ArrayList<>();

        farming.add(new Step("Wheat, and the hay bale it becomes",
                List.of("A horse on grass feeds itself. A stable full of horses does not, and the answer "
                                + "is a wheat field - because what wheat is for, here, is hay bales.",
                        "Nine wheat makes one bale, and a bale is the largest single meal in the game. "
                                + "A hungry horse looks about ten blocks for something to eat and takes "
                                + "the best thing it can reach: a hay bale first, ahead of crops, ahead "
                                + "of grass, ahead of everything. Set bales down where your horses are "
                                + "and they will come to them and eat them out of the world - you never "
                                + "have to hand anything to anyone.",
                        "That is the whole feeding system for a stable. Grass for a paddock, bales for "
                                + "anywhere that has run out of it, and a hurt horse needs to have eaten "
                                + "before it can heal at all.",
                        "Two warnings. What a horse eats is gone - a crop it can reach is destroyed "
                                + "outright, so never plant against the paddock fence, and a bale you "
                                + "put down is a bale that will disappear. And not every horse eats "
                                + "plants: a meat-eater will go and hunt your chickens instead, and no "
                                + "amount of hay will interest it.",
                        "Everything below this is how to work a field at the speed of a horse rather "
                                + "than at the speed of a hoe."),
                List.of(new ItemStack(Items.WHEAT), new ItemStack(Items.HAY_BLOCK)), Art.NONE,
                List.of(ProgressTask.BALE_HAY)));

        farming.add(new Step("Golden carrots, grown",
                List.of("Golden carrots are the good stuff - and normally they cost eight gold nuggets "
                                + "each, which puts a real ceiling on how many horses you can work with.",
                        "There is another way. Golden carrot seeds grow into golden carrots: an ordinary "
                                + "crop on ordinary farmland, ordinary growth, bone meal and all. A "
                                + "mature plant gives you a golden carrot and more seeds than you "
                                + "planted, so a field of them pays for itself.",
                        "The catch is getting the first seeds. They cannot be crafted from anything, and "
                                + "a golden carrot itself will not plant. The equestrian supplier sells "
                                + "them at his top rank for a serious number of emeralds, a few at a "
                                + "time; otherwise they turn up in the sort of chest you find at the "
                                + "bottom of a dungeon.",
                        "No gold ever comes out of the crop - only golden carrots. It is a way to grow "
                                + "horse food, not a gold mine."),
                List.of(new ItemStack(ModItems.GOLDEN_CARROT_SEEDS.get()),
                        new ItemStack(Items.GOLDEN_CARROT)), Art.NONE,
                List.of(ProgressTask.PLANT_GOLDEN_CARROT, ProgressTask.HARVEST_GOLDEN_CARROT)));

        farming.add(new Step("Carts, and the wheel they all start with",
                List.of("A cart wheel is eight sticks around a plank. Every cart in the game spends two "
                                + "or four of them, so make several.",
                        "After that a cart is planks, wheels, and whatever that particular cart needs - a "
                                + "chest, a hopper, an iron ingot. They come in every wood you have, and "
                                + "that includes woods added by other mods: if your pack has maple, it has "
                                + "maple wagons.",
                        "Right-click the ground to put one down. It sits there until a horse is put to it."),
                List.of(new ItemStack(HorseCarts.WHEEL)), Art.NONE,
                List.of(ProgressTask.PLACE_CART)));

        farming.add(new Step("Pulling ability: why one horse is better in harness",
                List.of("Sit on a saddled horse, stand near a cart, and press R. That is the whole thing - "
                                + "press R again to unhitch.",
                        "What happens next is the horse's business, not the cart's. Every horse has a "
                                + "pulling ability scored from 1 to 10 - it is on its information screen, "
                                + "beside Jump, with a word for what the number means. Five is ordinary. It "
                                + "is inherited like everything else, so it is something you can breed for.",
                        "The rule is worth learning properly, because it decides which horses you keep: "
                                + "pulling ability sets a ceiling, and speed decides how close you get to "
                                + "it. A weak horse is capped on a heavy cart no matter how fast you breed "
                                + "it - it simply cannot get the thing moving any harder. A very strong "
                                + "horse with no speed never reaches the ceiling its shoulders could carry.",
                        "So the best carthorse is neither specialist. A horse that is good at both beats "
                                + "the strongest horse you own and the fastest horse you own, on the same "
                                + "wagon, on the same road. Hold Shift on any cart to see what an ordinary "
                                + "horse keeps pulling it, and look at the Draught block on a horse's own "
                                + "screen to see what that horse would keep.",
                        "One more thing the score buys: a horse at 6 or better carries two riders."),
                List.of(new ItemStack(HorseCarts.item(CartKind.SUPPLY_CART, CartWood.fallback()))), Art.NONE,
                List.of(ProgressTask.HITCH_CART)));

        farming.add(new Step("The wagon",
                List.of("The carriage, and the heaviest thing a horse pulls - which makes it the honest "
                                + "test of a horse. Stripped logs, planks and four wheels.",
                        "It seats four. Feed it chests and its storage grows from thirty-six slots to "
                                + "seventy-two to a hundred and eight; right-click five wool carpets onto "
                                + "it for a roof in that colour.",
                        "A passenger on the box seat drives the horse. On a horse strong enough for two "
                                + "riders, that means a driver up front and somebody riding the horse "
                                + "itself - which is the point at which it stops being a cart and starts "
                                + "being a carriage."),
                List.of(new ItemStack(HorseCarts.item(CartKind.WAGON, CartWood.fallback()))), Art.NONE,
                List.of(ProgressTask.DRIVE_WAGON)));

        farming.add(new Step("The plow",
                List.of("Put a hoe inside it and till a whole field without touching a single block "
                                + "yourself - the horse walks, the ground turns over behind it.",
                        "A shovel instead of a hoe makes dirt paths. An axe strips logs. Right-click the "
                                + "plow to toggle it on and off, so you can drive it home without "
                                + "ploughing up the road."),
                List.of(new ItemStack(HorseCarts.item(CartKind.PLOW, CartWood.fallback()))), Art.NONE,
                List.of(ProgressTask.DRIVE_PLOW)));

        farming.add(new Step("The seed drill",
                List.of("The other half of the plow: it plants seeds on any farmland it is drawn across, "
                                + "and holds nine stacks of them.",
                        "Plow a field, drive the drill over it, and you have sown an acre in the time it "
                                + "takes to walk across it."),
                List.of(new ItemStack(HorseCarts.item(CartKind.SEED_DRILL, CartWood.fallback()))), Art.NONE,
                List.of(ProgressTask.DRIVE_SEED_DRILL)));

        farming.add(new Step("The reaper",
                List.of("Harvests mature crops it is drawn over, and drops them on the ground behind it.",
                        "It only works while a player is sitting on it - so this is the one cart you "
                                + "genuinely have to ride rather than merely pull."),
                List.of(new ItemStack(HorseCarts.item(CartKind.REAPER, CartWood.fallback()))), Art.NONE,
                List.of(ProgressTask.DRIVE_REAPER)));

        farming.add(new Step("The supply cart",
                List.of("Fifty-four stacks on two wheels, one seat, and unlike a chest it shows what is "
                                + "in it - tools, flowers and paintings are drawn sitting in the bed.",
                        "It will fly a banner, as will the animal cart and the wagon. Right-click one "
                                + "onto the back."),
                List.of(new ItemStack(HorseCarts.item(CartKind.SUPPLY_CART, CartWood.fallback()))), Art.NONE,
                List.of(ProgressTask.DRIVE_SUPPLY_CART)));

        farming.add(new Step("The animal cart",
                List.of("Two seats for players or animals, and small animals will climb in by themselves "
                                + "if you leave it standing among them.",
                        "It is the lightest thing there is to pull, which makes it the one a horse with a "
                                + "poor pulling score can still move at a respectable pace. Every cart has "
                                + "a weight, and the tooltip tells you what that one costs."),
                List.of(new ItemStack(HorseCarts.item(CartKind.ANIMAL_CART, CartWood.fallback()))), Art.NONE,
                List.of(ProgressTask.DRIVE_ANIMAL_CART)));

        out.add(new Chapter("Farming and haulage", List.copyOf(farming)));

        // ------------------------------------------------------------------
        // 3. Basic breeding - heats, covering and jars. No golden carrots:
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
                                + "itself. A stallion is never done, though: his first three covers "
                                + "in a day are at full odds and every one after is at half, but he "
                                + "goes on serving every mare in heat you leave him with. Given a "
                                + "choice, a mare takes the stallion who still has covers in hand.",
                        "That crowding rule is also why wild herds so rarely produce foals on their "
                                + "own: they live close together. A paddock of your own with a little "
                                + "room in it does far better."),
                List.of(new ItemStack(Items.LEAD), new ItemStack(Items.HAY_BLOCK)), Art.NONE,
                List.of(ProgressTask.NATURAL_COVER)));

        breeding.add(new Step("Reading a mare with a vet's kit",
                List.of("A vet's kit is shears, leather and an iron ingot, and it lasts sixty-four "
                                + "procedures. The equestrian scientist sells them too.",
                        "Use it on a mare and she tells you where she is: in heat, and whether this is "
                                + "the better half of it; out of heat, and how long until the next one; "
                                + "pregnant, and how long to go; or newly foaled and waiting on her "
                                + "foal heat. Use it on a stallion and he tells you how many covers "
                                + "he has made today, and whether he is past the three that are at "
                                + "full odds.",
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
                        "Most twins are fraternal - two separate draws, no more alike than any brother "
                                + "and sister. About one twin pregnancy in ten is identical instead: "
                                + "one egg that split, so the two foals are a single draw carried "
                                + "twice, the same sex and the same colour and the same markings down "
                                + "to the last streak. There is no breeding for that beyond breeding "
                                + "for twins at all.",
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
                List.of(ProgressTask.BREED_FOAL, ProgressTask.TWINS_FRATERNAL,
                        ProgressTask.TWINS_IDENTICAL)));

        breeding.add(new Step("Seed jars",
                List.of("A seed jar lets a stallion sire a foal on a mare he has never met. The empty "
                                + "one is a glass bottle and horse hair.",
                        "Filling it is the owner's act: get an entire stallion of yours interested in "
                                + "breeding, then use the empty jar on him. The jar takes his "
                                + "genetics, his name and his breed with it - and it counts against "
                                + "his three full-odds covers for that day like any other.",
                        "Using it is simpler: use the filled jar on a mare you own while she is in "
                                + "heat. Out of heat she refuses it and you keep the jar. In heat it is "
                                + "spent whether or not it takes, because it was a real attempt.",
                        "The mare has to be yours. The stallion does not, and that is the point - a "
                                + "jar that changes hands is how a bloodline travels between players. "
                                + "The equestrian scientist buys filled ones."),
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
                List.of(ProgressTask.GELD_HORSE, ProgressTask.OWN_GELDING,
                        ProgressTask.BOND_GELDING)));

        out.add(new Chapter("Basic breeding", List.copyOf(breeding)));

        // ------------------------------------------------------------------
        // 4. Genetics.
        // ------------------------------------------------------------------
        List<Step> genetics = new ArrayList<>();

        genetics.add(new Step("Taking a gene off a horse",
                List.of("Hold a book and right-click a horse. The book records one of that horse's "
                                + "gene pairs at random and becomes a research paper naming it - "
                                + "\"Agouti: A/a\", not just \"Agouti\". You cannot pick which, and the "
                                + "horse keeps what it has.",
                        "The pair is the useful part. A paper saying A/a and a paper saying A/A are "
                                + "different papers, and the carrot you craft from each hands on "
                                + "exactly that combination - so a horse in front of you is the only "
                                + "place an unusual pairing comes from.",
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
                                + "that shelf will copy the pair onto blank books for as long as the "
                                + "original stays in it. One slot per pair, so A/a and A/A both file.",
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
                                + "carrot, a research paper for it, some horse hair, and an ingot "
                                + "whose metal depends on how rare the gene is.",
                        "The carrot grants the pair the paper names, and the carrot's tooltip says "
                                + "which. So which paper you spend decides what the carrot does: a "
                                + "carrier paper hands on one copy, a true-breeding paper hands on two.",
                        "Feed the carrot to one of the parents, any time. It waits on that horse until "
                                + "the next breeding that actually takes, and then that parent passes "
                                + "that pair on instead of leaving it to a coin flip. Whether the foal "
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
                        "Four more narrow the roll to a theme - the dilutions, the white and spotting "
                                + "genes, the markings, or performance. There is a fifth for the "
                                + "magical genes, and it belongs to the next chapter."),
                List.of(new ItemStack(ModItems.STABILIZER_CARROT.get()),
                        new ItemStack(ModItems.MAGNIFIER_CARROT.get()),
                        new ItemStack(ModItems.UNKNOWN_GENE_SPLICE_CARROT.get()),
                        new ItemStack(ModItems.UNKNOWN_EPIGENETIC_SPLICE_CARROT.get())), Art.NONE,
                List.of(ProgressTask.CARROT_STABILIZER, ProgressTask.CARROT_MAGNIFIER,
                        ProgressTask.CARROT_UNKNOWN_GENE, ProgressTask.CARROT_UNKNOWN_EPIGENETIC,
                        ProgressTask.CARROT_DILUTION, ProgressTask.CARROT_WHITE,
                        ProgressTask.CARROT_MARKING, ProgressTask.CARROT_PERFORMANCE)));

        genetics.add(new Step("A door made of hay",
                List.of("Build a frame of cobblestone the way you would build a nether portal, light it "
                                + "with a golden carrot, and stand in it for ten seconds.",
                        "On the other side is a corridor of two thousand pens, each holding a mare and "
                                + "a stallion of one rolled genotype, each signed with the genes they "
                                + "carry. It is a showroom rather than a place to live - it is there so "
                                + "you can see what a gene actually looks like on a horse without "
                                + "breeding for a week to find out.",
                        "Bring your own horses through and they come home with you."),
                List.of(new ItemStack(Items.COBBLESTONE), new ItemStack(Items.GOLDEN_CARROT)), Art.NONE,
                List.of(ProgressTask.LIGHT_PORTAL, ProgressTask.ENTER_DIMENSION,
                        ProgressTask.BRING_HORSE_HOME)));

        out.add(new Chapter("Genetics", List.copyOf(genetics)));

        // ------------------------------------------------------------------
        // 5. Magical horses - its own chapter, not a corner of genetics: a
        // magical gene paints in a different phase, arrives by different
        // routes, and is most of the reason anybody goes looking at horses.
        // ------------------------------------------------------------------
        List<Step> magic = new ArrayList<>();

        magic.add(new Step("What a magical gene is",
                List.of("Every gene so far has worked the same way: it decides which pigment a patch "
                                + "of horse is allowed, and the coat is painted from what survives. A "
                                + "magical gene does not do that. It waits until the coat has been "
                                + "painted and then adds colour on top of the finished animal.",
                        "That is why a magical gene can do things no real pigment could. It can find "
                                + "the black parts of a horse and only touch those; it can find "
                                + "somebody else's white markings and recolour them; it can glow. Some "
                                + "of them change nothing about the coat at all and instead make the "
                                + "horse swim, breathe underwater, fight, drop something when it dies, "
                                + "or watch you from across a field at night.",
                        "They are ordinary genes in every other respect. Two copies, inherited one "
                                + "from each parent, written on the record, readable off the Genes tab, "
                                + "and they join your database the same way anything else does - by "
                                + "your owning a living horse that carries one.",
                        "The magical splice carrot is the deliberate way in once you know one exists: "
                                + "it narrows the random roll to the magical genes and nothing else. "
                                + "The census page on the wiki lists every gene in the mod with what "
                                + "each combination of its alleles actually does, magical and natural "
                                + "together, which is the shortest route to knowing what is out there.",
                        "There is one way in you cannot arrange at all. Every so often a foal bred in "
                                + "captivity is simply born with a magical gene neither of its parents "
                                + "had - a mutation, out of nowhere, about one foal in a thousand. "
                                + "Nothing you feed them and nothing you pair makes it likelier. If it "
                                + "happens to you, you were lucky."),
                List.of(new ItemStack(Items.GOLDEN_CARROT), new ItemStack(Items.AMETHYST_SHARD),
                        new ItemStack(Items.GLOWSTONE_DUST)), Art.NONE,
                List.of(ProgressTask.MAGICAL_GENE_DISCOVERED, ProgressTask.CARROT_MAGICAL,
                        ProgressTask.BREED_MUTATION)));

        magic.add(new Step("The dhampir",
                List.of("There is one breed in the mod that is magical by description rather than by "
                                + "accident. Dhampirs are rare, they found their herds at night, and "
                                + "they turn up in about a dozen biomes - plains and meadows, forests "
                                + "and taiga, savanna, snowy plains, cherry groves.",
                        "They come in two strains and never a mix of the two, which is the thing to "
                                + "understand before you go looking. Nine in ten are seal brown: red "
                                + "eyes, a single silent copy of each of the family traits, and "
                                + "otherwise a perfectly manageable horse. One in ten is the whole "
                                + "animal - white-coated, three times as tough as a horse has any right "
                                + "to be, half again as fast, twice the jump.",
                        "The full one also burns in daylight and drinks blood, and those two facts are "
                                + "most of what owning one is like. Two seal browns will throw a "
                                + "white-coated foal about one time in four; getting the entire package "
                                + "back out of them is a project measured in thousands.",
                        "Taming depends on which one you have found. A seal brown eats ordinary food "
                                + "and tames like anything else, by hand or from the saddle. A white "
                                + "one will not take food from your hand at all - it does not eat food "
                                + "- so there is no crouch-feeding it, and riding it out is the only "
                                + "way. Do that after dark, for its sake rather than yours."),
                List.of(new ItemStack(Items.REDSTONE), new ItemStack(Items.SADDLE)), Art.NONE,
                List.of(ProgressTask.MEET_DHAMPIR, ProgressTask.TAME_DHAMPIR)));

        magic.add(new Step("A magical herd",
                List.of("The other way magic arrives in the wild is not a breed at all. About one wild "
                                + "herd in twenty is a magical version of an ordinary breed - a herd of "
                                + "Friesians, or Shires, or Fell ponies, in which every single horse "
                                + "carries the same one magical gene.",
                        "Two things make it worth stopping for. Every horse in the herd carries it, so "
                                + "you are not hunting one animal in a field; and every one of them "
                                + "<i>shows</i> it rather than carrying it silently, so you can see "
                                + "what you are being offered before you commit to taming anything.",
                        "Their papers say so. The breed reads Magical and names the breed underneath, "
                                + "and it breeds on: a magical horse crossed back into its own breed "
                                + "stays magical, while crossed with anything else it is an ordinary "
                                + "cross like any other.",
                        "Only wild herds are magical this way. A generated stable and a breed spawn "
                                + "egg give you ordinary horses of the breed, and the gene a magical "
                                + "herd carries is never one of the body stats and never a disorder "
                                + "- a magical herd is not a sick one.",
                        "One cowboy in twelve is a different thing again: an arcane dealer, who keeps "
                                + "no breed at all. His horses are Mixed, never gelded, and each one "
                                + "shows eleven or twelve magical genes with no two of his animals "
                                + "carrying the same pair. He is dear, and he is the reliable way to "
                                + "collect what the wild almost never offers. Sometimes one is out on "
                                + "the road rather than at a barn.",
                        "He breeds willy-nilly, though, so what you buy is a gene bomb - a dozen "
                                + "unrelated loci on one animal, and almost never the single thing "
                                + "you wanted on its own. Isolating it is a breeding project: cross "
                                + "out to a plain horse and half the bomb goes each generation, keep "
                                + "the foals that still show what you are after, and breed those back "
                                + "together until it runs true. Sneak and use on any horse, his "
                                + "included, to read its genes before you pay for it."),
                List.of(), Art.NONE,
                List.of(ProgressTask.MEET_MAGICAL_HERD, ProgressTask.TAME_MAGICAL_HORSE,
                        ProgressTask.BUY_ARCANE_HORSE)));

        magic.add(new Step("Sunlight, and blood",
                List.of("Two magical traits change how you have to keep a horse, and they are worth "
                                + "meeting before you own one rather than afterwards.",
                        "A sun-sensitive horse catches fire in daylight and loses about half a heart "
                                + "every couple of seconds while the sky can see it. It knows this, and "
                                + "it will run - properly run, at more than a gallop - for the deepest "
                                + "shade it can reach, press against a fence to get there, and hold "
                                + "that spot until dark rather than wandering back out. Rain saves it, "
                                + "water saves it, and a roof saves it. A stable is not decoration for "
                                + "this horse.",
                        "A blood-drinker cannot be fed and cannot be healed the ordinary way. No food "
                                + "in your hand means anything to it, standing it beside water does "
                                + "nothing at all, and the only way it mends is by biting something "
                                + "living - half a heart off a cow, and then it leaves that particular "
                                + "animal alone for a day. It will not hunt while the sun is on it "
                                + "either, so a sunlit blood-drinker is doing nothing but looking for "
                                + "shade.",
                        "Both of these are genes rather than breeds. The dhampir is where you will "
                                + "meet them first, but they turn up on their own, and a horse you bred "
                                + "yourself can inherit either one."),
                List.of(new ItemStack(Items.TORCH), new ItemStack(Items.WATER_BUCKET),
                        new ItemStack(Items.BEEF)), Art.NONE,
                List.of(ProgressTask.SUN_SENSITIVE_SEEN, ProgressTask.BLOOD_BITE_SEEN)));

        out.add(new Chapter("Magical horses", List.copyOf(magic)));

        // ------------------------------------------------------------------
        // 6. Breeding projects - doing it on purpose.
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
        // 7. The villagers.
        // ------------------------------------------------------------------
        List<Step> people = new ArrayList<>();

        people.add(new Step("The four equestrians",
                List.of("Four villagers with jobs you will not have seen before, one post each. Put a "
                                + "post down near a villager who has no work and they will take it.",
                        "The Leatherworker sells tack - a saddle and leather horse armour - and dyes "
                                + "it more exotically the higher his tier: plain bone and flower "
                                + "colours at first, lapis blue with diamond fittings at the top. The "
                                + "Metalsmith sells horse armour in metal and crystal and nothing "
                                + "else.",
                        "The Scientist deals in breeding carrots, the vet's kit and breed spawn eggs, "
                                + "and buys filled seed jars off you; his best carrots name a rare "
                                + "gene twice over, so a foal is certain to get it. The Supplier is "
                                + "the general store - carrots, leads, whistles, tickets, papers, an "
                                + "empty seed jar - and buys horse hair from his first day, which "
                                + "makes him the easiest early trade in the mod.",
                        "Buying a paper is not the same as knowing the gene - see above - but it is "
                                + "how you get one for a horse you have never met."),
                List.of(new ItemStack(ModItems.LEATHERWORKERS_POST.get()),
                        new ItemStack(ModItems.SCIENTISTS_POST.get()),
                        new ItemStack(ModItems.METALSMITHS_POST.get()),
                        new ItemStack(ModItems.SUPPLIERS_POST.get()),
                        new ItemStack(Items.EMERALD)), Art.EQUESTRIAN,
                List.of(ProgressTask.TRADE_EQUESTRIAN)));

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
                                + "also where you will find a Tack Dyeing Bench already standing."),
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
                case EQUESTRIAN -> TutorialPortraits.drawEquestrian(g, artX, textTop, ART, mouseX, mouseY);
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
