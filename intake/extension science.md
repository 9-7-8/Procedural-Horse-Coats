The horse **Extension locus** is the *MC1R* gene—also called *Red Factor*. Its primary, well-established role is to determine whether coat melanocytes can make substantial **eumelanin** (black/brown pigment) or are effectively limited to **pheomelanin** (red/yellow pigment). It is one half of the classical base-color system; the other is Agouti (*ASIP*), which determines where black pigment appears *if MC1R permits black pigment at all*. [vgl.ucdavis](https://vgl.ucdavis.edu/resources/horse-coat-color)

## Core biology

*MC1R* encodes melanocortin 1 receptor, a receptor on melanocytes. In simplified functional terms:

- When MC1R is functional and activated by melanocyte-stimulating hormone signaling, melanocytes can switch toward producing eumelanin.
- Agouti signaling protein (ASIP) can antagonize that receptor in particular body regions. In a genetically bay horse, this makes the body predominantly red while allowing eumelanin at the points.
- When both *MC1R* copies are loss-of-function red alleles, the melanocyte cannot make the normal coat-hair eumelanin response. The horse is chestnut/red-based, and its *ASIP* genotype is visually hidden. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9498372/)

“Extension” is a historical genetic name, not a literal claim that the gene simply “extends black.” “Red factor” is useful in testing contexts, but can be misleading: the ordinary dominant allele does not itself make red pigment; it permits black pigment production. Red pigment is still part of normal mammalian pigmentation, including in bay horses.

A practical way to frame the hierarchy is:

\[
\textit{MC1R} = \text{can the coat make black pigment?}
\]

\[
\textit{ASIP} = \text{if yes, where is black pigment restricted?}
\]

That makes Extension epistatic to Agouti: a horse that cannot produce visible eumelanin in its coat cannot display an Agouti-controlled bay-versus-black difference.

## Established allele series

At the molecular level, three equine *MC1R* alleles are established and recognized by UC Davis Veterinary Genetics Laboratory: **E**, **e**, and **eᵃ**. The last two are recessive loss-of-function red alleles. [vgl.ucdavis](https://vgl.ucdavis.edu/resources/horse-coat-color)

| Allele | Molecular/functional status | Phenotypic consequence |
|---|---|---|
| **E** | Functional, wild-type *MC1R* allele | Permits eumelanin production. It does **not** decide whether the horse looks black or bay; that depends chiefly on *ASIP* and additional modifiers |
| **e** | Common recessive red allele; missense change p.Ser83Phe (S83F) | Loss of normal MC1R function. A horse with two red-function alleles—typically *e/e*—is chestnut/sorrel/red, with red body and red points |
| **eᵃ** | Rare recessive red allele; missense change p.Asp84Asn (D84N) | Also loss-of-function for practical coat-color purposes. It combines with *e* or another *eᵃ* to produce a chestnut phenotype |

The usual dominance relationship is:

\[
E > e \quad \text{and} \quad E > e^a
\]

So these genotypes are interpreted as follows:

| *MC1R* genotype | What it establishes | Likely undiluted base phenotype when Agouti is known |
|---|---|---|
| *E/E* | Black pigment can be produced; cannot pass ordinary *e* | *A/_* → bay; *a/a* → black |
| *E/e* | Black pigment can be produced; carries ordinary red | *A/_* → bay; *a/a* → black |
| *E/eᵃ* | Black pigment can be produced; carries rare red | *A/_* → bay; *a/a* → black |
| *e/e* | No normal coat eumelanin production | Chestnut, regardless of Agouti |
| *e/eᵃ* | Two loss-of-function red alleles | Chestnut |
| *eᵃ/eᵃ* | Two rare loss-of-function red alleles | Chestnut; reported, but extremely uncommon in most tested populations |

The most familiar red allele, *e*, is the p.S83F variant. The rare *eᵃ* allele is p.D84N, immediately adjacent in the protein sequence. Both have been demonstrated as recessive variants associated with chestnut coloring. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9498372/)

## Pigment and phenotype

### Red-based horses: *e/e*, *e/eᵃ*, or *eᵃ/eᵃ*

A horse with no functional **E** allele produces predominantly pheomelanin in the coat:

- Body hair is red to red-yellow.
- Mane, tail, lower legs, and ear rims are red rather than genetically black.
- “Chestnut,” “sorrel,” and “liver chestnut” are phenotype or breed-language labels, not separate established *MC1R* alleles.
- The horse may genetically be *A/A*, *A/a*, or *a/a* at Agouti, but that cannot be read from its undiluted chestnut coat because Agouti’s basic job is to distribute eumelanin—and there is no visible coat eumelanin for it to distribute. [vgl.ucdavis](https://vgl.ucdavis.edu/resources/horse-coat-color)

This is why a chestnut can produce a black or bay foal: a chestnut may carry *a* or *A* at Agouti, and it contributes one of its red alleles. If the other parent supplies **E**, the foal can make black pigment, revealing the inherited Agouti pattern.

### Black-pigment-capable horses: *E/_*

At least one **E** says only that black pigment production is available. It does not label the horse “black.”

- *E/_ A/_*: usually bay in the basic model—red body with black mane, tail, lower legs, and ear margins.
- *E/_ a/a*: usually black in the basic model—black pigment broadly distributed in the coat.
- Dilutions, gray, white-pattern alleles, environmental fading, seasonal coat, and shade modifiers can transform or obscure those visual outcomes. A buckskin remains genetically black-pigment-capable, for example; cream has diluted the bay base. A gray horse retains its underlying Extension genotype despite progressive depigmentation. [vgl.ucdavis](https://vgl.ucdavis.edu/resources/horse-coat-color)

### Extension does not explain shade by itself

The locus is foundational, but it is not a “chestnut shade” locus or a full black/bay appearance predictor.

- *e/e* horses can range from pale yellow-red through bright copper to very dark liver chestnut.
- *E/_ A/_* horses can range from bright bay to dark bay or near-black “seal-like” phenotypes.
- *E/E* versus *E/e* may be statistically associated with some bay shade differences in some datasets, but it is not reliable enough to use as a universal visual rule for individual horses.
- Many pigment loci and likely polygenic/regulatory modifiers influence intensity, distribution, timing, and hair-level pigment deposition. UC Davis notes that, although hundreds of pigmentation-related genes are known across mammals, the contributions of many to equine shade remain unresolved. [vgl.ucdavis](https://vgl.ucdavis.edu/resources/horse-coat-color)

For simulation design, treat Extension as a **pigment-permission gate**, then layer Agouti, known dilutions, white/gray patterning, and separately modeled shade modifiers on top. Do not encode “liver,” “flaxen,” “sooty,” “seal brown,” or “blood bay” as direct *MC1R* genotype outcomes.

## The eᵃ issue and testing

The most important practical complication is **eᵃ**.

Older “red factor” tests often assayed only the common *e* variant. An *E/eᵃ* horse could therefore be reported as apparently *E/E* by a limited test, because it lacks the ordinary *e* marker even though it carries a different recessive red allele. Likewise, an *e/eᵃ* chestnut could be misinterpreted if the laboratory does not test for *eᵃ*. Modern full base-color panels should explicitly distinguish **E**, **e**, and **eᵃ**. [vgl.ucdavis](https://vgl.ucdavis.edu/resources/horse-coat-color)

*eᵃ* is uncommon but not merely theoretical. In a UC Davis dataset of 11,281 horses across 28 breeds, it appeared in Knabstruppers, Paint Horses, Percherons, and Quarter Horses; the relatedness-filtered estimate in Knabstruppers was 0.035. Earlier reports had associated it especially with Black Forest, Hungarian Coldblood, and Haflinger populations. That kind of distribution is exactly why a result from a single-breed or single-lab dataset should not be mistaken for a universal breed exclusion list. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9498372/)

A breeding example:

- Chestnut parent: *e/e*
- Black or bay parent: *E/eᵃ*

Possible foals at Extension are 50% *E/e*—black-pigment-capable—and 50% *e/eᵃ*—red-based. A panel that sees only ordinary *e* could fail to communicate the parent’s full red-carrier status.

## Speculation, historic claims, and controversy

### “Dominant black” or Eᴰ

The most persistent disputed concept is a putative **Eᴰ** (“dominant black”) Extension allele. Older coat-color literature and breed discussions sometimes proposed it to explain very dark horses, black-producing crosses, or horses believed to be “true black” in a way that ordinary **E** could not explain.

The critical distinction is:

- **A phenotypic observation is not proof of a new allele.**
- To establish a new *MC1R* allele, researchers need a reproducible molecular variant or haplotype, clear inheritance evidence, functional plausibility or testing, and replication in an adequately characterized population.

No molecularly characterized, broadly accepted equine **Eᴰ** allele is included in the modern routine *MC1R* allele series. Current institutional summaries list **E**, **e**, and **eᵃ**, not Eᴰ. [vgl.ucdavis](https://vgl.ucdavis.edu/resources/horse-coat-color)

Why did the idea seem plausible? In many mammals, different *MC1R* variants can cause dominant black or other eumelanic effects. That makes the hypothesis biologically reasonable in the abstract. But “plausible in other species” does not establish it in horses. In horses, dark phenotypes can arise from ordinary *E/_* plus *a/a*, dark-bay/bay-shade modifiers, sooty-type effects, environmental fading differences, and visual misclassification. The 2020 bay-shade study found a strong associated region near *ASIP/RALY* on chromosome 22, illustrating that major dark-versus-light bay variation need not come from a new *MC1R* allele. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC7349280/)

### “Extension controls black versus red” is true—but incomplete

This shorthand is useful for beginners but creates several misconceptions:

- **E does not guarantee black.** *E/_ A/_* is bay in the classical model.
- **e does not “make chestnut pigment.”** It removes normal MC1R-mediated eumelanin production in the coat, allowing pheomelanin to predominate.
- **e/e does not mean Agouti is absent.** It means Agouti cannot visibly express its usual eumelanin-placement effect.
- **E/e versus E/E is not a dependable coat-color label.** Both allow black pigment; the genotype matters chiefly for inheritance and potentially modest population-level shade associations.
-- **Red points in a chestnut are not evidence of Agouti failure.** They are the expected consequence of loss-of-function *MC1R* alleles in coat pigment production.

### “Seal brown” and Extension

Seal brown, brown, dark bay, and sun-faded black are especially prone to overconfident genetic claims. A horse can look nearly black yet genetically be *E/_ A/_* with an unusually dark bay expression, or be *E/_ a/a* with brownish/faded areas. The older notion that Extension alone includes a distinct established “brown” allele is not supported by current molecular testing.

The best-supported current position is that classical *MC1R* and *ASIP* explain the broad base-color framework, while the exact boundaries among dark bay, brown, and black reflect additional genetic architecture, phenotype classification problems, and environmental effects. The discovered chromosome-22 shade association lies near *ASIP* and *RALY*, but its causal mutation has not been conclusively identified. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC7349280/)

## What science can establish next

The next meaningful advances are likely to come less from discovering a dramatic new “black allele” and more from resolving **regulatory, polygenic, and structural variation**.

### Highest-value research questions

- **Causal variant at the bay-shade locus:** A GWAS identified a strong chromosome-22 signal near *ASIP* and *RALY*, but the lead marker is likely tagging a causal regulatory or structural change rather than being causal itself. The high linkage disequilibrium and assembly complexity in that region make fine-mapping difficult. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC7349280/)
- **MC1R regulatory variation:** Coding changes are easy to test, but regulatory changes that alter when, where, or how much *MC1R* is expressed could create subtle phenotype effects without a clean textbook allele series.
- **True functional validation:** Cell-based receptor-signaling assays, allele-specific expression work, and carefully phenotyped breeding populations are stronger evidence than associations from owner-reported color labels.
- **Better phenotyping:** Color-calibrated photography, spectrophotometry, repeated seasonal measurements, and explicit separation of gray/dilute/white-pattern effects would reduce the noise that plagues “dark bay versus black” studies.
- **Long-read and phased assemblies:** Pigmentation regions often contain regulatory sequence, repeats, and structural variation that short-read association studies do not resolve cleanly. The *ASIP/RALY* study specifically noted technical assembly difficulties around its candidate region. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC7349280/)
- **Broader breed sampling:** Rare alleles such as *eᵃ* can be missed or treated as breed-specific simply because the relevant lines have not been tested at scale. [pmc.ncbi.nlm.nih](https://pmc.ncbi.nlm.nih.gov/articles/PMC9498372/)

## Practical conclusion

The rigorous modern model is simple at its core:

1. **E** is a functional *MC1R* allele that permits black pigment.
2. **e** and rare **eᵃ** are recessive loss-of-function alleles that produce a red/chestnut base when no functional **E** allele is present.
3. Extension determines **whether coat eumelanin is available**; Agouti determines **how that available eumelanin is distributed**.
4. The accepted molecular allele series is **E, e, eᵃ**—not a confirmed Eᴰ/dominant-black allele.
5. Much of what horse people argue about—liver chestnut, flaxen, dark bay, seal brown, black fading, and exact shade—is real phenotype variation, but it should not be casually assigned to new Extension alleles without molecular evidence.

For a genetics simulator, the best implementation is an *MC1R* allele series of **E / e / eᵃ**, with a functional state of “eumelanin enabled” whenever at least one **E** is present. Make the chestnut outcome occur when both alleles are loss-of-function, then let Agouti operate only when eumelanin is enabled, and assign dark-bay/black-adjacent ambiguity to independent shade modifiers rather than an invented *MC1R* Eᴰ allele.
