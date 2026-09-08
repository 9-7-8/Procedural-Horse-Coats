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
            title: "The coat engine",
            items: [
                { href: "pipeline.html", text: "Three-phase pipeline", kind: "core", views: ["coding"] },
                { href: "body-space.html", text: "Body space & regions", kind: "core", views: ["coding"] },
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
            title: "Natural genes",
            items: [
                { href: "gene-sex.html", text: "Sex (X / Y)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-diet.html", text: "Diet", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-extension.html", text: "Extension", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-agouti.html", text: "Agouti (bay / seal)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-shade.html", text: "Shade (bay shade)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-matp.html", text: "MATP (cream / pearl)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-champagne.html", text: "Champagne", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-grey.html", text: "Grey (dapple)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-dun.html", text: "Dun", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-silver.html", text: "Silver dapple", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-flaxen.html", text: "Flaxen", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-sooty.html", text: "Sooty", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-pangare.html", text: "Pangare (mealy)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-mushroom.html", text: "Mushroom", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-natural-zebra.html", text: "Zebra striping", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-brindle.html", text: "Brindle (X-linked)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-tiger-eye.html", text: "Tiger eye", kind: "natural", views: ["gameplay","coding"] }
            ]
        },
        {
            title: "White pattern loci",
            items: [
                { href: "gene-kit.html", text: "KIT (dominant white / sabino)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-mitf.html", text: "MITF (splash white)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-pax3.html", text: "PAX3 (splash white)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-ednrb.html", text: "EDNRB (frame overo)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-tobiano.html", text: "Tobiano", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-manchado.html", text: "Manchado", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-roan.html", text: "Roan (classic)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-rabicano.html", text: "Rabicano", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-leopard.html", text: "Leopard complex (appaloosa)", kind: "natural", views: ["gameplay","coding","science"] }
            ]
        },
        {
            title: "Performance & size genes",
            items: [
                { href: "gene-mstn.html", text: "MSTN (myostatin)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-pdk4.html", text: "PDK4", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-ckm.html", text: "CKM", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-ryr2.html", text: "RYR2 (jumping)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-lcorl.html", text: "LCORL / NCAPG (height)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-hmga2.html", text: "HMGA2 (pony)", kind: "natural", views: ["gameplay","coding"] }
            ]
        },
        {
            title: "Health genes",
            items: [
                { href: "gene-acan.html", text: "ACAN (dwarfism)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-b4galt7.html", text: "B4GALT7 (Friesian dwarfism)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-plod1.html", text: "PLOD1 (fragile foal)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-rapgef5.html", text: "RAPGEF5 (EFIH)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-st14.html", text: "ST14 (naked foal)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-shox.html", text: "SHOX (skeletal atavism)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-met.html", text: "MET (embryonic lethal)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-scn4a.html", text: "SCN4A (HYPP)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-gys1.html", text: "GYS1 (PSSM1)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-ppib.html", text: "PPIB (HERDA)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-prkdc.html", text: "PRKDC (SCID)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-myo5a.html", text: "MYO5A (lavender foal)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-toe1.html", text: "TOE1 (cerebellar abiotrophy)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-cvm.html", text: "CVM (cervical malformation)", kind: "natural", views: ["gameplay","coding","science"] },
                { href: "gene-gbe1.html", text: "GBE1 (GBED)", kind: "natural", views: ["gameplay","coding"] },
                { href: "gene-megaesophagus.html", text: "Megaesophagus", kind: "natural", views: ["gameplay","coding","science"] }
            ]
        },
        {
            title: "Magical body-stat genes",
            items: [
                { href: "gene-body-size.html", text: "Magic body size", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-magic-speed.html", text: "Magic speed", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-magic-health.html", text: "Magic health", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-magic-jump.html", text: "Magic jump", kind: "magical", views: ["gameplay","coding"] }
            ]
        },
        {
            title: "Magical genes",
            items: [
                { href: "gene-dhampir.html", text: "Dhampir", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-pink-hair.html", text: "Pink hair", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-mane-color.html", text: "Mane colour", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-tail-color.html", text: "Tail colour", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-healer.html", text: "Healer", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-magic-zebra.html", text: "Magic zebra", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-milk.html", text: "Milk (water / lava)", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-light.html", text: "Light", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-magic-sectoral-heterochromia.html", text: "Magic sectoral heterochromia", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-particle.html", text: "Particle", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-rainbow-dust.html", text: "Rainbow dust", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-lycan.html", text: "LYCAN (werewolf)", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-verdant.html", text: "Verdant", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-lut.html", text: "LUT (palette swap)", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-cutie-mark.html", text: "Cutie mark", kind: "magical", views: ["gameplay","coding"] },
                { href: "gene-suntouched.html", text: "Suntouched", kind: "magical", views: ["gameplay","coding","science"] },
                { href: "gene-waterborn.html", text: "Waterborn", kind: "magical", views: ["gameplay","coding","science"] }
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
        {
            title: "Tools",
            items: [
                { href: "gene-creator/index.html", text: "Gene creator", kind: "tool", views: ["coding"] },
                { href: "horse-designer/index.html", text: "Horse designer", kind: "tool", views: ["gameplay","coding"] },
                { href: "breed-designer/index.html", text: "Breed designer", kind: "tool", views: ["coding"] },
                { href: "lut-lab.html", text: "LUT lab", kind: "tool", views: ["coding"] },
                { href: "gene-format.html", text: "Gene file format", kind: "tool", views: ["coding"] },
                { href: "breed-format.html", text: "Breed file format", kind: "tool", views: ["coding"] },
                { href: "gene-effects.html", text: "Gene effects", kind: "tool", views: ["coding"] }
            ]
        }
    ]
};
