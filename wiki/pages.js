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
                { href: "eye-colour.html", text: "Eye colour & heterochromia", kind: "core", views: ["gameplay","coding","science"] }
            ]
        },
        {
            title: "For modders",
            items: [
                { href: "modding.html", text: "Writing a gene", kind: "core", views: ["coding"] },
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
                { href: "creating-a-gene.html", text: "Creating a gene", kind: "core", views: ["gameplay","coding"] },
                { href: "gene-format.html", text: "Gene file format", kind: "tool", views: ["coding"] },
                { href: "breed-format.html", text: "Breed file format", kind: "tool", views: ["coding"] },
                { href: "gene-effects.html", text: "Gene effects", kind: "tool", views: ["coding"] }
            ]
        },
        {
            title: "Project",
            items: [
                { href: "architecture.html", text: "Architecture & the build", kind: "core", views: ["coding"] },
                { href: "api-notes.html", text: "NeoForge 26.1.2 API notes", kind: "core", views: ["coding"] },
                { href: "verification.html", text: "To be verified", kind: "core", views: ["coding"] },
                { href: "known-gaps.html", text: "Known gaps & lessons", kind: "core", views: ["coding"] },
                { href: "compatibility.html", text: "Mod compatibility", kind: "core", views: ["coding"] },
                { href: "roadmap.html", text: "Roadmap / backlog", kind: "core", views: ["coding"] },
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
                { href: "gene-mushroom.html", text: "Mushroom", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-dun.html", text: "Dun", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-matp.html", text: "MATP", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-champagne.html", text: "Champagne", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-grey.html", text: "Grey", kind: "natural", views: ["gameplay","coding","science"] }
            ]
        },
        {
            title: "Natural white genes",
            items: [
                { href: "gene-natural-zebra.html", text: "Zebra striping", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-roan.html", text: "Roan", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-rabicano.html", text: "Rabicano", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-tobiano.html", text: "Tobiano", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-leopard.html", text: "The leopard complex (appaloosa)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-ednrb.html", text: "EDNRB (frame overo)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-kit.html", text: "KIT (white spotting)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-manchado.html", text: "Manchado", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-mitf.html", text: "MITF (splash white)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-pax3.html", text: "PAX3 (splash white)", kind: "natural", views: ["gameplay","coding"] }
            ]
        },
        {
            title: "Natural health genes",
            items: [
                { href: "gene-mstn.html", text: "MSTN (myostatin)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-pdk4.html", text: "PDK4", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-ckm.html", text: "CKM", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-ryr2.html", text: "RYR2 (jumping)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-lcorl.html", text: "LCORL / NCAPG (height)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-hmga2.html", text: "HMGA2 (pony)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-acan.html", text: "ACAN (chondrodysplastic dwarfism)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-b4galt7.html", text: "B4GALT7 (Friesian dwarfism)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-plod1.html", text: "PLOD1 (fragile foal syndrome)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-rapgef5.html", text: "RAPGEF5 (EFIH)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-st14.html", text: "ST14 (naked foal syndrome)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-shox.html", text: "SHOX (skeletal atavism)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-met.html", text: "MET (embryonic lethal)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-scn4a.html", text: "SCN4A (HYPP)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-gys1.html", text: "GYS1 (PSSM1)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-ppib.html", text: "PPIB (HERDA)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-prkdc.html", text: "PRKDC (SCID)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-myo5a.html", text: "MYO5A (lavender foal syndrome)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-toe1.html", text: "TOE1 (cerebellar abiotrophy)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-cvm.html", text: "CVM (cervical vertebral malformation)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-gbe1.html", text: "GBE1 (GBED)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-megaesophagus.html", text: "Megaesophagus", kind: "natural", views: ["gameplay","coding","science"] }
            ]
        },
        {
            title: "Other natural genes",
            items: [
                { href: "gene-sex.html", text: "Sex", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-diet.html", text: "Diet", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-tiger-eye.html", text: "Tiger eye (SLC24A5)", kind: "natural", views: ["gameplay","coding"] }
            ]
        },
        {
            title: "Magical genes",
            items: [
                { href: "gene-cutie-mark.html", text: "Cutie mark", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-dhampir.html", text: "Dhampir", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-healer.html", text: "Healer", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-hood.html", text: "Hood", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-light.html", text: "Light", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-lut.html", text: "LUT", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-lycan.html", text: "LYCAN", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-magic-sectoral-heterochromia.html", text: "Magic sectoral heterochromia", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-magic-zebra.html", text: "Magic zebra", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-mane-color.html", text: "Mane colour", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-milk.html", text: "Milk", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-particle.html", text: "Particle", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-rainbow-dust.html", text: "Rainbow dust", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-suit.html", text: "Suit", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-tail-color.html", text: "Tail colour", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-verdant.html", text: "Verdant", kind: "magical", views: ["gameplay","coding"] }
            ]
        },
        {
            title: "Magical body-stat genes",
            items: [
                { href: "gene-body-size.html", text: "Magic body size", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-magic-health.html", text: "Magic health", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-magic-jump.html", text: "Magic jump", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-magic-speed.html", text: "Magic speed", kind: "magical", views: ["gameplay","coding"] }
            ]
        },
        {
            title: "Magic: ground and strong white",
            items: [
                { href: "gene-black-holes.html", text: "Black Holes", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-burn.html", text: "Burn", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-cleave.html", text: "Cleave", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-fracture.html", text: "Fracture", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-galaxy.html", text: "Galaxy", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-imperial.html", text: "Imperial", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-laciano.html", text: "Laciano", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-marble-tobiano.html", text: "Marble Tobiano", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-panda.html", text: "Panda", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-qular.html", text: "Qular", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-suntouched.html", text: "Suntouched", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-valentine-appaloosa.html", text: "Valentine Appaloosa", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-waterborn.html", text: "Waterborn", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-winged.html", text: "Winged", kind: "magical", views: ["gameplay","coding"] }
            ]
        },
        {
            title: "Magic: fields and regions",
            items: [
                { href: "gene-accretion.html", text: "Accretion", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-cosmic.html", text: "Cosmic", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-dutch.html", text: "Dutch", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-emitola.html", text: "Emitola", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-flametouched.html", text: "Flametouched", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-goth.html", text: "Goth", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-hued-pangare.html", text: "Hued Pangare", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-iuari.html", text: "Iuari", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-masked.html", text: "Masked", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-nimbus.html", text: "Nimbus", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-opossum.html", text: "Opossum", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-oscar.html", text: "Oscar", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-papillon.html", text: "Papillon", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-patina.html", text: "Patina", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-quarter.html", text: "Quarter", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-quazars.html", text: "Quazars", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-shark.html", text: "Shark", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-sky-shadow.html", text: "Sky Shadow", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-stardust.html", text: "Stardust", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-synort.html", text: "Synort", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-war-mask.html", text: "War Mask", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-xuke.html", text: "Xuke", kind: "magical", views: ["gameplay","coding"] }
            ]
        },
        {
            title: "Magic: spots and rings",
            items: [
                { href: "gene-angler.html", text: "Angler", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-aurorae.html", text: "Aurorae", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-crescents.html", text: "Crescents", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-fawn.html", text: "Fawn", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-glow.html", text: "Glow", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-integration.html", text: "Integration", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-lantern.html", text: "Lantern", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-lasertae.html", text: "Lasertae", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-pavonem.html", text: "Pavonem", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-polymoon.html", text: "Polymoon", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-rex.html", text: "Rex", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-rosette.html", text: "Rosette", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-wormholes.html", text: "Wormholes", kind: "magical", views: ["gameplay","coding"] }
            ]
        },
        {
            title: "Magic: speckle and dust",
            items: [
                { href: "gene-dust.html", text: "Dust", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-peafowl.html", text: "Peafowl", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-speckling.html", text: "Speckling", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-stars.html", text: "Stars", kind: "magical", views: ["gameplay","coding"] }
            ]
        },
        {
            title: "Magic: lines and strokes",
            items: [
                { href: "gene-bracket.html", text: "Bracket", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-brindlelace.html", text: "Brindlelace", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-circuit.html", text: "Circuit", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-filigree.html", text: "Filigree", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-gudial.html", text: "Gudial", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-internal-flow.html", text: "Internal Flow", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-kintsugi.html", text: "Kintsugi", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-panark.html", text: "Panark", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-qhada.html", text: "Qhada", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-riblines.html", text: "Riblines", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-scratches.html", text: "Scratches", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-stellar.html", text: "Stellar", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-vortex.html", text: "Vortex", kind: "magical", views: ["gameplay","coding"] },
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
                { href: "gene-extreme-white-dominant.html", text: "Extreme white dominant", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-fielded.html", text: "Fielded", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-gamma.html", text: "Gamma", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-invert.html", text: "Invert", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-nebula-light.html", text: "Nebula Light", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-opalized.html", text: "Opalized", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-spectrum-light.html", text: "Spectrum Light", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-voided.html", text: "Voided", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-yalia.html", text: "Yalia", kind: "magical", views: ["gameplay","coding"] }
            ]
        },
        /* END generated by :common:bakeGeneWikiPages */
    ]
};
