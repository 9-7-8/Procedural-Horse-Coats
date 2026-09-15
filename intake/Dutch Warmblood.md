Dutch Warmblood (KWPN) is a modern European sport-horse breed developed in the Netherlands for dressage, show jumping, and eventing, combining Gelderlander/Groningen ancestry with Thoroughbred refinement. [en.wikipedia](https://en.wikipedia.org/wiki/Dutch_Warmblood)

- Identity: Also called KWPN or Sport Warmblood, bred to excel in high-level competition; not a pony or draft. [en.wikipedia](https://en.wikipedia.org/wiki/Dutch_Warmblood)
- Origins & use: Established postwar to create athletic, balanced riding horses with athletic gaits, scope, and trainability for international sport. [en.wikipedia](https://en.wikipedia.org/wiki/Dutch_Warmblood)
- Appearance: Medium-to-large, well-muscled, elegant yet powerful build; typically tall with a refined head, arched neck, long shoulder, and strong hindquarters. Coat colors vary, but solid and dark hues are common; patterns are rare in the core breed. [breeds.okstate](https://breeds.okstate.edu/horses/dutch-warmblood-horses)
- Temperament & aptitude: Bright, trainable, and generally stable temperaments; exceptional in dressage, show jumping, and eventing; strong work ethic and good rideability. [breeds.okstate](https://breeds.okstate.edu/horses/dutch-warmblood-horses)
- Height range: Typically around 16.0–17.2 hands, but can vary with individual type and crossbreeding. [breeds.okstate](https://breeds.okstate.edu/horses/dutch-warmblood-horses)
- Why it matters in mods: Offers a well-characterized, internationally recognized sport horse profile with defined performance traits, enabling clear stat tuning (speed, jump, health) and a distinct non-feral, non-pony population. It also provides a useful baseline for pedigreed sport-bred lines without focusing on color patterns.

JSON file outline (conceptual; adapt to your mod’s schema):

- id: "dutch_warmblood"
- name: "Dutch Warmblood"
- type: "natural" or "natural_sport" (as per your schema)
- notes: describe that KWPN represents a modern sport-bred warmblood, not a color registry, and that coat colors are variable but not the defining trait
- biomes: suitable temperate grassland/forest-adjacent biomes; add coastal or continental plains if desired
- spawn_weight: moderate (e.g., 6)
- spawn_time: "day"
- sources: ["wild", "stable", "spawn_egg", "cowboy"]
- price: mid-high for sport-bred stock
- commonness: "MODERATE" or equivalent in your enum
- coat_genes: include baseline warmblood coat pool (modern diverse colors) with dominant European-draft and Thoroughbred-like alleles, but don’t overemphasize rare color patterns
- disorder_genes: include common sport-horse considerations if you want, but only add well-supported loci
- stat_scores: speed 6–7, jump 7–8, health 7–8, size range near baseline riding horse
- epigenetic_bands: leave empty unless you want to bias color expression
- strains: optional, could include sport-leaning lines if your mod supports strains
- lineage: default cross table; clarify cross expectations with other sport-type breeds

If you want, I can draft a concrete JSON snippet aligned to your exact schema once you share the precise field names and allowed locus IDs.