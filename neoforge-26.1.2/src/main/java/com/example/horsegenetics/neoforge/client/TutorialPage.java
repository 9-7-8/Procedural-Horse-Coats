package com.example.horsegenetics.neoforge.client;

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
 */
public final class TutorialPage {

    /** One illustration beside a step. */
    public enum Art {
        NONE,
        HORSEMAN,
        COWBOY
    }

    /** A step: a heading, some paragraphs, a row of items, and maybe a portrait. */
    public record Step(String heading, List<String> paragraphs, List<ItemStack> icons, Art art) {
    }

    private static final int ART = 48;
    private static final int ICON = 18;

    private static List<Step> steps;

    private TutorialPage() {
    }

    /** Built once per session; item stacks need the registry, so not at class-init. */
    public static List<Step> steps() {
        if (steps == null) {
            steps = build();
        }
        return steps;
    }

    /** Dropped with the rest of the per-world client state. */
    public static void clear() {
        steps = null;
    }

    private static List<Step> build() {
        List<Step> out = new ArrayList<>();

        out.add(new Step("Every horse is one of a kind",
                List.of("Horses in this world are not four colours and a saddle. Each one carries a "
                                + "full set of genes, inherited from its parents the way a real horse's "
                                + "are, and its coat is painted from them - so no two are quite alike "
                                + "and none of them was drawn by hand.",
                        "Some of them are not entirely ordinary, either. There are horses that glow, "
                                + "horses that walk on water, and horses that are perfectly friendly "
                                + "until the sun goes down. You will know one when you meet it."),
                List.of(), Art.NONE));

        out.add(new Step("1. Tame one",
                List.of("Find a wild horse and get on it, with nothing in your hand. It will throw "
                                + "you off. Get back on. Keep getting back on until hearts appear. "
                                + "That is the whole of taming.",
                        "Feeding it first makes it quicker to convince. A saddle lets you steer it "
                                + "once it is yours."),
                List.of(new ItemStack(Items.SADDLE), new ItemStack(Items.WHEAT),
                        new ItemStack(Items.GOLDEN_APPLE)), Art.NONE));

        out.add(new Step("2. Shear one for hair",
                List.of("Right-click a horse with shears. You get one to three horse hair, "
                                + "and it grows back after a while.",
                        "Do this early and often. Horse hair is the base material for "
                                + "nearly everything this mod adds - rope, cloth, the "
                                + "whistles, the stall signs, the seed jars, the transfer "
                                + "papers, the research shelf and every breeding carrot. "
                                + "Almost nothing else can be made without it."),
                List.of(new ItemStack(Items.SHEARS), new ItemStack(ModItems.HORSE_HAIR.get()),
                        new ItemStack(ModItems.BRAIDED_ROPE.get())), Art.NONE));

        out.add(new Step("3. You need one of each",
                List.of("Every horse is a mare or a stallion, and it is a gene like any other - a "
                                + "foal inherits its sex rather than being assigned one. Two mares "
                                + "will not give you a foal however much they like each other, and "
                                + "neither will two stallions.",
                        "You do not have to guess. Look at the name above a horse's head: a pink "
                                + "♀ is a mare and a blue ♂ is a stallion, readable right "
                                + "across a paddock. (If you would rather not see them, there is a "
                                + "switch in the client config.)"),
                List.of(), Art.NONE));

        out.add(new Step("4. Feed them both a golden carrot",
                List.of("Tame a mare and a stallion, put them together, and feed each of them a "
                                + "golden carrot. Hearts, and then a foal.",
                        "The foal is not a copy of either parent. Each parent passes on one of its "
                                + "two copies of every gene, and which one is a coin flip - which is "
                                + "why breeding the same pair twice gives you two different horses, "
                                + "and why breeding is worth doing at all."),
                List.of(new ItemStack(Items.GOLDEN_CARROT)), Art.NONE));

        out.add(new Step("5. Milk a mare",
                List.of("Right-click a tamed adult mare with an empty bucket. Stallions "
                                + "decline, and are not shy about it.",
                        "How often a mare can be milked is itself genetic - most manage it "
                                + "once a day, and there is a locus that makes some of them "
                                + "far more generous. A few horses give something other than "
                                + "milk entirely, which is a thing worth finding out about "
                                + "the hard way."),
                List.of(new ItemStack(Items.BUCKET), new ItemStack(Items.MILK_BUCKET)),
                Art.NONE));

        out.add(new Step("6. Take a gene off a horse, with a book",
                List.of("Hold a book and right-click a horse. The book grabs one of that "
                                + "horse's genes at random and becomes a research paper naming it. "
                                + "You cannot pick which.",
                        "A completely ordinary horse has nothing to give, and will not cost you "
                                + "the book.",
                        "The paper is not something you read. It is a component: file it in a "
                                + "shelf to copy it, or spend it on a gene carrot."),
                List.of(new ItemStack(Items.BOOK), new ItemStack(ModItems.RESEARCH_PAPER.get())),
                Art.NONE));

        out.add(new Step("Your gene database is earned, not read",
                List.of("A gene joins your database when you have owned a living horse that "
                                + "carries it - tamed, bred, bought, or given to you. Not from "
                                + "paper, and not from looking at one in a field. Knowing a "
                                + "gene is also what unlocks its gene carrot.",
                        "So a paper and a horse do different jobs. A paper is a thing you can "
                                + "copy and spend; a horse is the only thing that can teach "
                                + "you."),
                List.of(), Art.NONE));

        out.add(new Step("7. Keep them on a shelf",
                List.of("A research paper is a thing you can lose. File it into an Equine Research "
                                + "Shelf and it stops being one: that shelf will copy the gene onto "
                                + "blank books for as long as the original stays in it.",
                        "Copying is not instant - a common gene takes about ten seconds and a truly "
                                + "rare one about a minute, so a shelf is something you set going. "
                                + "Break the shelf and every paper in it drops, so moving one costs "
                                + "you nothing.",
                        "This is what makes knowledge worth having rather than worth hoarding. A "
                                + "gene on your shelf is a gene you can hand to a friend as many "
                                + "times as they have books."),
                List.of(new ItemStack(ModItems.EQUINE_RESEARCH_SHELF.get()),
                        new ItemStack(Items.BOOK),
                        new ItemStack(ModItems.RESEARCH_PAPER.get())), Art.NONE));

        out.add(new Step("8. The horseman sells papers",
                List.of("The horseman is a villager with a job you will not have seen before. Put a "
                                + "Horseman's Table down near one who has no work and he will take "
                                + "it.",
                        "He deals in research papers, and the rarer the gene the more he wants for "
                                + "one. Buying a paper is not the same as knowing the gene - see "
                                + "above - but it is how you get one for a horse you have never "
                                + "met."),
                List.of(new ItemStack(ModItems.HORSEMANS_TABLE.get()),
                        new ItemStack(Items.EMERALD)), Art.HORSEMAN));

        out.add(new Step("9. The cowboy sells horses",
                List.of("The cowboy is not a villager at all. He keeps his own herd, and he will "
                                + "sell you one outright if you would rather buy a bloodline than "
                                + "breed one. A Cowboy Hitch is what brings him.",
                        "The two of them come as a pair - they built their posts side by side, and "
                                + "the story in the villages is that they are married. You will "
                                + "find him out where the horses are rather than in a village, and "
                                + "he is worth the walk."),
                List.of(new ItemStack(ModItems.COWBOY_HITCH.get()),
                        new ItemStack(Items.EMERALD)), Art.COWBOY));

        out.add(new Step("10. Put a gene into a foal",
                List.of("Once a gene is in your database you can make its gene carrot - a golden "
                                + "carrot, that gene's research paper, some horse hair, and an ingot "
                                + "whose metal depends on how rare the gene is.",
                        "Feed the carrot to one of the parents before they breed, and that parent "
                                + "will pass the gene on instead of leaving it to a coin flip. "
                                + "Whether the foal actually shows it still depends on what the "
                                + "other parent brought - some genes need two copies before they do "
                                + "anything at all.",
                        "The Recipes tab has every recipe the mod adds, with the gene carrots on "
                                + "their own list. You craft them at an ordinary crafting table."),
                List.of(new ItemStack(ModItems.KNOWN_GENE_SPLICE_CARROT.get()),
                        new ItemStack(ModItems.HORSE_HAIR.get()),
                        new ItemStack(Items.GOLD_INGOT),
                        new ItemStack(Items.DIAMOND)), Art.NONE));

        out.add(new Step("11. Breed the herd you wanted",
                List.of("Now it is a loop. Meet horses, take what they will tell you, file it, copy "
                                + "it, splice it, and breed the result into the next generation. "
                                + "Every horse in the paddock gets a little more like the one you "
                                + "had in mind.",
                        "Nothing stops you splicing your way to something spectacular in an "
                                + "afternoon - but anybody can do that, and a spliced horse says so "
                                + "on its own paperwork. Getting there by breeding is the part that "
                                + "makes it yours."),
                List.of(), Art.NONE));

        out.add(new Step("Where the good horses are",
                List.of("Wild herds are not the same everywhere. Breeds belong to the country they "
                                + "came from - the heavy horses to the cold north, the desert breeds "
                                + "to the savanna, the ponies to the hills - so a horse you cannot "
                                + "find at home may be three biomes away.",
                        "There are stables out there too, generated with the world and already full "
                                + "of horses somebody else bred. They are worth finding; the animals "
                                + "in them are better than anything wandering loose."),
                List.of(), Art.NONE));

        out.add(new Step("The shortcut: a door made of hay",
                List.of("Build a frame of hay bales the way you would build a nether portal, light "
                                + "it with a golden carrot, and stand in it for ten seconds.",
                        "On the other side is a corridor of two thousand pens, each holding a mare "
                                + "and a stallion of one rolled genotype, each signed with the genes "
                                + "they carry. It is a showroom rather than a place to live - it is "
                                + "there so you can see what a gene actually looks like on a horse "
                                + "without breeding for a week to find out.",
                        "Bring your own horses through and they come home with you."),
                List.of(new ItemStack(Items.HAY_BLOCK), new ItemStack(Items.GOLDEN_CARROT)),
                Art.NONE));

        out.add(new Step("Where to look next",
                List.of("My horses is every horse you own, sortable and searchable - it will take "
                                + "queries like \"mare gen>2 gene:SB1\".",
                        "Gene database is everything you have learned, with what each gene does. "
                                + "Breeding preview reads a mare and a stallion together and tells "
                                + "you what they could make, locus by locus. Recipes is every recipe "
                                + "in the mod.",
                        "This page will not open first again - the browser reopens wherever you left "
                                + "it. It is always here on this tab if you want it."),
                List.of(), Art.NONE));

        return List.copyOf(out);
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    /**
     * Draw the page into {@code (x, y, w)} scrolled by {@code scroll}, and return
     * the total height the content wants. The caller scissors and scrolls; this
     * only lays out.
     */
    public static int draw(GuiGraphicsExtractor g, Font font, int x, int y, int w,
                           int mouseX, int mouseY, int headingColour, int bodyColour, int dimColour) {
        int cy = y;
        for (Step step : steps()) {
            g.text(font, Component.literal(step.heading()), x, cy, headingColour, false);
            cy += font.lineHeight + 4;

            int textW = step.art() == Art.NONE ? w : w - ART - 8;
            int textTop = cy;
            for (String paragraph : step.paragraphs()) {
                for (String line : GuiText.wrap(font, paragraph, textW)) {
                    g.text(font, Component.literal(line), x, cy, bodyColour, false);
                    cy += font.lineHeight + 1;
                }
                cy += 4;
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
            cy += 8;
        }
        // A last line, so the end of the page does not look like a cut.
        g.text(font, Component.literal("— good luck."), x, cy, dimColour, false);
        cy += font.lineHeight;
        return cy - y;
    }
}
