/* The wiki's page manifest - the ONE place a page is registered.
 *
 * wiki/nav.js builds the sidebar from this, and the landing page filters its
 * cards by it, so a new page is added here and nowhere else.
 *
 * views: which of the three index views list this page. For a page split into
 * Gameplay / Coding / Science tabs that is the tabs it actually has; for a page
 * that is one continuous document it is the audiences it is written for - a
 * backend note has no business on the gameplay view. wiki/tabs.js is what
 * renders the tabs themselves; this only decides who is shown the page.
 *
 * Works from file://: it declares data and fetches nothing.
 */
window.HG = window.HG || {};
window.HG.pages = {
    SECTIONS: [
        {
            title: "Start here",
            items: [
                { href: "../index.html", text: "Wiki home", kind: "core", views: ["gameplay","coding","science"] },
                { href: "philosophy.html", text: "Philosophy", kind: "core", views: ["coding","science"] },
                { href: "genetics-model.html", text: "The genetics model", kind: "core", views: ["gameplay","coding","science"] },
                { href: "breeding.html", text: "Breeding & pedigree", kind: "core", views: ["gameplay","coding"] },
                { href: "breeds.html", text: "Breeds", kind: "core", views: ["gameplay","coding"] },
                { href: "breed-book.html", text: "The breed book", kind: "core", views: ["gameplay"] },
                { href: "horse-body.html", text: "The horse's body", kind: "core", views: ["gameplay","coding"] }
            ]
        },
        {
            title: "Gameplay",
            items: [
                { href: "horse-care.html", text: "Horse care: healing, bond, herds", kind: "core", views: ["gameplay"] },
                { href: "carrots.html", text: "Breeding carrots & the gene database", kind: "magical", views: ["gameplay","coding"] },
                { href: "items.html", text: "Items & recipes", kind: "core", views: ["gameplay","coding"] },
                { href: "villagers.html", text: "Villagers & transfer papers", kind: "core", views: ["gameplay","coding"] },
                { href: "horse-dimension.html", text: "Hay portals & the horse dimension", kind: "core", views: ["gameplay","coding"] },
                { href: "stables.html", text: "Generated stables", kind: "core", views: ["gameplay","coding"] }
            ]
        },
        {
            title: "Items",
            items: [
                { href: "item-horse-hair.html", text: "Horse hair & the material chain", kind: "core", views: ["gameplay","coding"] },
                { href: "item-breeding-carrots.html", text: "Breeding carrots", kind: "magical", views: ["gameplay","coding"] },
                { href: "item-splice-carrots.html", text: "Splice carrots", kind: "magical", views: ["gameplay","coding"] },
                { href: "item-research-papers.html", text: "Research papers", kind: "core", views: ["gameplay","coding"] },
                { href: "item-research-shelf.html", text: "Equine Research Shelf", kind: "core", views: ["gameplay","coding"] },
                { href: "progression.html", text: "The checklist \u0026 the collection", kind: "core", views: ["gameplay","coding"] },
                { href: "item-seed-jars.html", text: "Seed jars", kind: "core", views: ["gameplay","coding"] },
                { href: "item-whistles.html", text: "Whistles", kind: "core", views: ["gameplay","coding"] },
                { href: "item-stall-signs.html", text: "Stall signs", kind: "core", views: ["gameplay","coding"] },
                { href: "item-tickets.html", text: "Tickets", kind: "core", views: ["gameplay","coding"] },
                { href: "item-transfer-papers.html", text: "Transfer papers", kind: "core", views: ["gameplay","coding"] },
                { href: "item-horsemans-table.html", text: "Horseman's Table", kind: "core", views: ["gameplay","coding"] },
                { href: "item-spawn-eggs.html", text: "Spawn eggs", kind: "tool", views: ["gameplay","coding"] }
            ]
        },
        {
            title: "The coat engine",
            items: [
                { href: "pipeline.html", text: "Three-phase pipeline", kind: "core", views: ["coding"] },
                { href: "body-space.html", text: "Body space & regions", kind: "core", views: ["coding"] },
                { href: "paint-order.html", text: "The paint order", kind: "core", views: ["gameplay","coding"] },
                { href: "eye-colour.html", text: "Eye colour & heterochromia", kind: "core", views: ["gameplay","coding","science"] },
                { href: "texture-resolution.html", text: "Texture resolution", kind: "core", views: ["coding"] }
            ]
        },
        {
            title: "For modders",
            items: [
                { href: "making-a-gene.html", text: "Making a gene", kind: "core", views: ["coding"] },
                { href: "api-reference.html", text: "Class abstractions", kind: "core", views: ["coding"] },
                { href: "horse-traits.html", text: "Trait & effect architecture", kind: "magical", views: ["coding"] }
            ]
        },
        {
            title: "Tools",
            items: [
                { href: "gene-creator/index.html", text: "Gene creator", kind: "tool", views: ["coding"] },
                { href: "horse-designer/index.html", text: "Horse designer", kind: "tool", views: ["gameplay","coding"] },
                { href: "breed-designer/index.html", text: "Breed designer", kind: "tool", views: ["coding"] },
                { href: "gene-lut.html?view=lab", text: "LUT lab", kind: "tool", views: ["gameplay","coding"] },
                { href: "breed-format.html", text: "Breed file format", kind: "tool", views: ["coding"] }
            ]
        },
        {
            title: "Project",
            items: [
                { href: "releases.html", text: "Releases", kind: "core", views: ["gameplay","coding"] },
                { href: "architecture.html", text: "Architecture & the build", kind: "core", views: ["coding"] },
                { href: "api-notes.html", text: "NeoForge 26.1.2 API notes", kind: "core", views: ["coding"] },
                { href: "coding-notes.html", text: "Coding notes", kind: "core", views: ["coding"] },
                { href: "verification.html", text: "To be verified", kind: "core", views: ["coding"] },
                { href: "known-gaps.html", text: "Known gaps & lessons", kind: "core", views: ["coding"] },
                { href: "compatibility.html", text: "Mod compatibility", kind: "core", views: ["coding"] },
                { href: "roadmap.html", text: "Roadmap / backlog", kind: "core", views: ["coding"] },
                { href: "timeline-of-genes.html", text: "Timeline of genes", kind: "core", views: ["coding"] },
                { href: "session-log.html", text: "Session log", kind: "core", views: ["coding"] }
            ]
        },
        /* BEGIN generated by :common:bakeGeneWikiPages - do not edit between the markers.
           One section per magical gene family, each led by its index page.
           Hand-written entries go OUTSIDE this span and are never touched. */
        {
            title: "Natural coat genes",
            items: [
                { href: "gene-extension.html", text: "Extension", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-agouti.html", text: "Agouti", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-brindle.html", text: "Brindle (MBTPS2)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-sooty.html", text: "Sooty", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-pangare.html", text: "Pangaré", kind: "natural", views: ["gameplay","coding","science"] }
            ]
        },
        {
            title: "Natural dilution genes",
            items: [
                { href: "gene-silver.html", text: "Silver dapple", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-flaxen.html", text: "Flaxen", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-mushroom.html", text: "Mushroom", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-dun.html", text: "Dun", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-matp.html", text: "MATP", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-champagne.html", text: "Champagne", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-grey.html", text: "Grey", kind: "natural", views: ["gameplay","coding","science"] }
            ]
        },
        {
            title: "Natural white genes",
            items: [
                { href: "gene-natural-zebra.html", text: "Zebra striping", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-roan.html", text: "Roan", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-rabicano.html", text: "Rabicano", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-tobiano.html", text: "Tobiano", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-leopard.html", text: "The leopard complex (appaloosa)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-ednrb.html", text: "EDNRB (frame overo)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-kit.html", text: "KIT (white spotting)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-manchado.html", text: "Manchado", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-mitf.html", text: "MITF (splash white)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-pax3.html", text: "PAX3 (splash white)", kind: "natural", views: ["gameplay","coding","science"] }
            ]
        },
        {
            title: "Natural health genes",
            items: [
                { href: "gene-mstn.html", text: "MSTN (myostatin)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-pdk4.html", text: "PDK4", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-ckm.html", text: "CKM", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-ryr2.html", text: "RYR2 (jumping)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-lcorl.html", text: "LCORL / NCAPG (height)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-hmga2.html", text: "HMGA2 (pony)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-acan.html", text: "ACAN (chondrodysplastic dwarfism)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-b4galt7.html", text: "B4GALT7 (Friesian dwarfism)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-plod1.html", text: "PLOD1 (fragile foal syndrome)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-rapgef5.html", text: "RAPGEF5 (EFIH)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-st14.html", text: "ST14 (naked foal syndrome)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-shox.html", text: "SHOX (skeletal atavism)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-met.html", text: "MET (embryonic lethal)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-scn4a.html", text: "SCN4A (HYPP)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-gys1.html", text: "GYS1 (PSSM1)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-ppib.html", text: "PPIB (HERDA)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-prkdc.html", text: "PRKDC (SCID)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-myo5a.html", text: "MYO5A (lavender foal syndrome)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-toe1.html", text: "TOE1 (cerebellar abiotrophy)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-cvm.html", text: "CVM (cervical vertebral malformation)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-gbe1.html", text: "GBE1 (GBED)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-megaesophagus.html", text: "Megaesophagus", kind: "natural", views: ["gameplay","coding","science"] }
            ]
        },
        {
            title: "Other natural genes",
            items: [
                { href: "gene-sex.html", text: "Sex", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-diet.html", text: "Diet", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-tiger-eye.html", text: "Tiger eye (SLC24A5)", kind: "natural", views: ["gameplay","coding","science"] }
            ]
        },
        {
            title: "Magical coat genes",
            items: [
                { href: "gene-bargello.html", text: "Bargello", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-blackwork.html", text: "Blackwork", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-coastline.html", text: "Coastline", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-contour.html", text: "Contour", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-crazework.html", text: "Crazework", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-cutie-mark.html", text: "Cutie mark", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-dhampir.html", text: "Dhampir", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-dorsal-wing.html", text: "Dorsal Wing", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-dripwork.html", text: "Dripwork", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-dustfall.html", text: "Dustfall", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-healer.html", text: "Healer", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-hood.html", text: "Hood", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-light.html", text: "Light", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-lut.html", text: "LUT", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-magic-sectoral-heterochromia.html", text: "Magic sectoral heterochromia", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-magic-zebra.html", text: "Magic zebra", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-mane-color.html", text: "Mane colour", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-nacre-saddle.html", text: "Nacre Saddle", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-nymphaline.html", text: "Nymphaline", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-overcast.html", text: "Overcast", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-ringwork.html", text: "Ringwork", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-serpentine.html", text: "Serpentine", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-shallows.html", text: "Shallows", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-squiggle.html", text: "Squiggle", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-starburst.html", text: "Starburst", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-suit.html", text: "Suit", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-synort.html", text: "Synort", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-tail-color.html", text: "Tail colour", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-teardrop.html", text: "Teardrop", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-wingmargin.html", text: "Wing Margin", kind: "magical", views: ["gameplay","coding"] }
            ]
        },
        {
            title: "Magical yield genes",
            items: [
                { href: "gene-egg-layer.html", text: "Egg layer", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-magic-item-drop.html", text: "Magic item drop", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-magic-meat.html", text: "Magic meat", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-magic-milk-volume.html", text: "Magic milk volume", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-magic-on-death.html", text: "Magic on death", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-milk.html", text: "Milk", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-potion-milk.html", text: "Potion milk", kind: "magical", views: ["gameplay","coding","science"] }
            ]
        },
        {
            title: "Magical behaviour genes",
            items: [
                { href: "gene-base-alarm.html", text: "Base alarm", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-bird-boned.html", text: "Bird boned", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-cleansing-light.html", text: "Cleansing light", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-dryad.html", text: "Dryad", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-echolocate.html", text: "Echolocate", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-ender-echo.html", text: "Ender echo", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-fireproof.html", text: "Fireproof", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-food-preference.html", text: "Food preference", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-gladiator.html", text: "Gladiator", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-guardian.html", text: "Guardian", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-holy-ward.html", text: "Holy ward", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-hot-blooded.html", text: "Hot blooded", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-hydrophobic.html", text: "Hydrophobic", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-intimidating.html", text: "Intimidating", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-pack-leader.html", text: "Leader of the pack", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-lycan.html", text: "LYCAN", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-magic-mob-aura.html", text: "Magic mob aura", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-magic-night-temper.html", text: "Magic night temper", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-magic-night-watch.html", text: "Magic night watch", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-meowing.html", text: "Meowing", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-music-enjoyer.html", text: "Music enjoyer", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-ocean-born.html", text: "Ocean-born", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-spawner.html", text: "Spawner", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-spontaneous-breeding.html", text: "Spontaneous breeding", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-verdant.html", text: "Verdant", kind: "magical", views: ["gameplay","coding"] }
            ]
        },
        {
            title: "Magical emission genes",
            items: [
                { href: "gene-molten-hooves.html", text: "Molten hooves", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-particle.html", text: "Particle", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-rainbow-dust.html", text: "Rainbow dust", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-singer.html", text: "Singer", kind: "magical", views: ["gameplay","coding","science"] }
            ]
        },
        {
            title: "Magical body-stat genes",
            items: [
                { href: "gene-agatebound.html", text: "Agatebound", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-bracketed.html", text: "Bracketed", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-coronal.html", text: "Coronal", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-eyesight.html", text: "Eyesight locus", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-geode.html", text: "Geode", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-maelstrom.html", text: "Maelstrom", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-body-size.html", text: "Magic body size", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-magic-fighter.html", text: "Magic fighter", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-magic-health.html", text: "Magic health", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-magic-jump.html", text: "Magic jump", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-magic-speed.html", text: "Magic speed", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-magic-swim-speed.html", text: "Magic swim speed", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-magic-water-breathing.html", text: "Magic water breathing", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-sunspiral.html", text: "Sunspiral", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-weather-jump.html", text: "Weather sensitive (jump)", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-weather-speed.html", text: "Weather sensitive (speed)", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-witchfire.html", text: "Witchfire", kind: "magical", views: ["gameplay","coding"] }
            ]
        },
        {
            title: "Magic: ground and strong white",
            items: [
                { href: "gene-beetle-pearl.html", text: "Beetle Pearl", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-black-holes.html", text: "Black Holes", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-burn.html", text: "Burn", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-carapace-sheen.html", text: "Carapace Sheen", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-cleave.html", text: "Cleave", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-coccinella.html", text: "Coccinella Marking", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-cracked-porcelain.html", text: "Cracked Porcelain", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-dragon-shroud.html", text: "Dragon Shroud", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-fracture.html", text: "Fracture", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-galaxy.html", text: "Galaxy", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-imperial.html", text: "Imperial", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-iridescent-jewel.html", text: "Iridescent Jewel Beetle Coat", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-koi.html", text: "Koi", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-laciano.html", text: "Laciano", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-marble-tobiano.html", text: "Marble Tobiano", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-moth-mantle.html", text: "Moth Mantle", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-opal-wing-veins.html", text: "Opal Wing-Vein Marking", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-panda.html", text: "Panda", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-qular.html", text: "Qular", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-snow-cloud.html", text: "Snow Cloud", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-suntouched.html", text: "Suntouched", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-tribal-ward.html", text: "Tribal Ward", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-valentine-appaloosa.html", text: "Valentine Appaloosa", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-waterborn.html", text: "Waterborn", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-winged.html", text: "Winged", kind: "magical", views: ["gameplay","coding"] }
            ]
        },
        {
            title: "Magic: fields and regions",
            items: [
                { href: "gene-accretion.html", text: "Accretion", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-agate.html", text: "Agate", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-banded-socks.html", text: "Banded Socks", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-barred-wing.html", text: "Barred Wing", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-candelabra.html", text: "Candelabra", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-coral-bloom.html", text: "Coral Bloom", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-cosmic.html", text: "Cosmic", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-dorsal-shield.html", text: "Dorsal Shield", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-dutch.html", text: "Dutch", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-emitola.html", text: "Emitola", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-flametouched.html", text: "Flametouched", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-foxglove.html", text: "Foxglove", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-gill-bloom.html", text: "Gill Bloom", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-goth.html", text: "Goth", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-hued-pangare.html", text: "Hued Pangare", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-inkcoil.html", text: "Inkcoil", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-iris-bloom-diamonds.html", text: "Iris Bloom Diamonds", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-iuari.html", text: "Iuari", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-kite-bloom.html", text: "Kite Bloom", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-ladybug-saddle.html", text: "Ladybug Saddle", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-masked.html", text: "Masked", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-moonwing.html", text: "Moonwing", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-morpho-wing.html", text: "Morpho Wing", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-nebula-points.html", text: "Nebula Points", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-nightbloom-shroud.html", text: "Nightbloom Shroud", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-nimbus.html", text: "Nimbus", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-opal-crackle.html", text: "Opal Crackle", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-opal-fire.html", text: "Opal Fire", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-opaline.html", text: "Opaline", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-opossum.html", text: "Opossum", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-orchid-frost.html", text: "Orchid Frost", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-oscar.html", text: "Oscar", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-papillon.html", text: "Papillon", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-patina.html", text: "Patina", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-petaled.html", text: "Petaled", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-quarter.html", text: "Quarter", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-quazars.html", text: "Quazars", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-shark.html", text: "Shark", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-sky-shadow.html", text: "Sky Shadow", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-stardust.html", text: "Stardust", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-taper-flame.html", text: "Taper Flame", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-tidefoam-sash.html", text: "Tidefoam Sash", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-tidemark.html", text: "Tidemark", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-tidewave.html", text: "Tidewave", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-trillium.html", text: "Trillium", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-uraniid.html", text: "Uraniid", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-war-mask.html", text: "War Mask", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-xuke.html", text: "Xuke", kind: "magical", views: ["gameplay","coding"] }
            ]
        },
        {
            title: "Magic: spots and rings",
            items: [
                { href: "gene-agate-eye.html", text: "Agate Eye", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-angler.html", text: "Angler", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-aurorae.html", text: "Aurorae", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-beadscale.html", text: "Beadscale", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-concentric-eyespots.html", text: "Concentric Eyespots", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-contour-cells.html", text: "Contour Cells", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-corolla.html", text: "Corolla", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-crescents.html", text: "Crescents", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-duskwing-bands.html", text: "Duskwing Bands", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-ehretia.html", text: "Ehretia", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-fawn.html", text: "Fawn", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-fish-scales.html", text: "Fish Scales", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-glow.html", text: "Glow", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-harlequin-shield.html", text: "Harlequin Shield", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-integration.html", text: "Integration", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-iridescent-rosette.html", text: "Iridescent Rosette", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-lantern.html", text: "Lantern", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-lasertae.html", text: "Lasertae", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-moth-eyes.html", text: "Moth Eyes", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-ocular.html", text: "Ocular", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-painted-lady-wing.html", text: "Painted Lady Wingmark", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-pavonem.html", text: "Pavonem", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-picasso-marking.html", text: "Picasso Marking", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-polymoon.html", text: "Polymoon", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-rex.html", text: "Rex", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-rosette.html", text: "Rosette", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-scarab-dapple.html", text: "Scarab Dapple", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-scuted.html", text: "Scuted", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-shieldback.html", text: "Shieldback", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-wormholes.html", text: "Wormholes", kind: "magical", views: ["gameplay","coding"] }
            ]
        },
        {
            title: "Magic: speckle and dust",
            items: [
                { href: "gene-duskfall-speckle.html", text: "Duskfall Speckle", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-dust.html", text: "Dust", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-peafowl.html", text: "Peafowl", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-speckling.html", text: "Speckling", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-sporefall.html", text: "Sporefall", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-stars.html", text: "Stars", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-wishstar.html", text: "Wishstar", kind: "magical", views: ["gameplay","coding"] }
            ]
        },
        {
            title: "Magic: lines and strokes",
            items: [
                { href: "gene-bracket.html", text: "Bracket", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-brindlelace.html", text: "Brindlelace", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-circuit.html", text: "Circuit", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-datarain.html", text: "Datarain", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-diamond-scutes.html", text: "Diamond Scutes", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-elytra-veins.html", text: "Elytra Veins", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-emberveins.html", text: "Emberveins", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-filigree.html", text: "Filigree", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-foamed.html", text: "Foamed", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-gilded-crackle.html", text: "Gilded Crackle", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-gudial.html", text: "Gudial", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-ink-scroll.html", text: "Ink Scroll", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-internal-flow.html", text: "Internal Flow", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-kintsugi.html", text: "Kintsugi", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-lace.html", text: "Lace", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-lacevein.html", text: "Lacevein", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-moth-wings.html", text: "Moth Wings", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-ooze-drip.html", text: "Ooze Drip", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-opaline-zebra.html", text: "Opaline Zebra", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-panark.html", text: "Panark", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-qhada.html", text: "Qhada", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-rainbow-drip.html", text: "Rainbow Drip", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-riblines.html", text: "Riblines", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-rime.html", text: "Rime", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-scratches.html", text: "Scratches", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-stained-glass.html", text: "Stained Glass", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-stellar.html", text: "Stellar", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-tribal-claw.html", text: "Tribal Claw", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-vortex.html", text: "Vortex", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-webbed.html", text: "Webbed", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-wyrmwood-sigils.html", text: "Wyrmwood Sigils", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-zebras-coat.html", text: "Zebra's Coat", kind: "magical", views: ["gameplay","coding"] }
            ]
        },
        {
            title: "Magic: mane and tail",
            items: [
                { href: "gene-auroraband.html", text: "Auroraband", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-corvid.html", text: "Corvid", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-element-dusted.html", text: "Element Dusted", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-fade.html", text: "Fade", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-frostfall.html", text: "Frostfall", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-guppy.html", text: "Guppy", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-prismtail.html", text: "Prismtail", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-striped-mane-tail.html", text: "Striped Mane and Tail", kind: "magical", views: ["gameplay","coding"] }
            ]
        },
        {
            title: "Magic: colour modifiers",
            items: [
                { href: "gene-bloodstained.html", text: "Bloodstained", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-extreme-white-dominant.html", text: "Extreme white dominant", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-fielded.html", text: "Fielded", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-gamma.html", text: "Gamma", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-holo-flake.html", text: "Holo Flake", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-invert.html", text: "Invert", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-nacre.html", text: "Nacre", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-nebula-light.html", text: "Nebula Light", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-nyxborn.html", text: "Nyxborn", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-oil-sheen.html", text: "Oil Sheen", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-opalized.html", text: "Opalized", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-rising-sun.html", text: "Rising Sun", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-shadowcreature.html", text: "Shadowcreature", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-spectrum-light.html", text: "Spectrum Light", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-tron.html", text: "Tron", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-voided.html", text: "Voided", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-yalia.html", text: "Yalia", kind: "magical", views: ["gameplay","coding"] }
            ]
        },
        /* END generated by :common:bakeGeneWikiPages */
    ]
};
