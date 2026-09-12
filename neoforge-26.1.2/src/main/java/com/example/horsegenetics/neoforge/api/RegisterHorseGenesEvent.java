package com.example.horsegenetics.neoforge.api;

import com.example.horsegenetics.common.genetics.Gene;
import com.example.horsegenetics.common.genetics.Genes;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/**
 * <b>Add your own genes.</b> Fired once on every mod's own event bus, at the one
 * moment when it is safe: every mod has been constructed, and nothing has parsed
 * a genotype code yet.
 *
 * <pre>{@code
 * @Mod("yourmod")
 * public final class YourMod {
 *     public YourMod(IEventBus modEventBus) {
 *         modEventBus.addListener(this::addGenes);
 *     }
 *
 *     private void addGenes(RegisterHorseGenesEvent event) {
 *         event.register(new YourGene());
 *     }
 * }
 * }</pre>
 *
 * <h2>Why an event and not a static call</h2>
 * {@link Genes#register} is public and works from a mod constructor. What it
 * cannot do is guarantee <i>when</i>: mod constructors run in dependency order,
 * so a gene registered from one is registered before or after this mod's own
 * genes depending on whether that mod declared a dependency - and the registry
 * decides the genotype code's layout, so "sometimes earlier, sometimes later"
 * is not a thing a genotype code can survive. This event has exactly one firing
 * point, so every mod's genes land in the same place every launch.
 *
 * <h2>What a gene has to honour</h2>
 * <ul>
 *   <li>A key of {@code <yourmodid>.<gene>}. Refused otherwise, loudly - the
 *       namespace is the only thing keeping two mods' "dun" apart.</li>
 *   <li>A {@link Gene#priority()} in its phase's band: {@code 0}-{@code 99}
 *       natural, {@code 100}+ magical. Outside it is a warning, not a refusal.
 *       <b>Order never depends on registration order</b> - everything is sorted
 *       on {@code (priority, key)} - so two players with the same mods get the
 *       same horses whatever order the mods loaded in.</li>
 *   <li>The rest of the contract is on
 *       {@code wiki/making-a-gene.html#java-when}, and the short path to a gene
 *       is {@code common}'s {@code AbstractNaturalGene} /
 *       {@code AbstractMagicalGene} / {@code AbstractAbilityGene}.</li>
 * </ul>
 *
 * <p><b>Adding a gene changes the genotype code</b>, and this mod has no save
 * compatibility: horses saved before your mod was added will not load with it,
 * and will not load again once it is removed. That is expected while the mod is
 * in development, and it is the reason this event is the only way in rather
 * than one of several.
 */
public final class RegisterHorseGenesEvent extends Event implements IModBusEvent {

    RegisterHorseGenesEvent() {}

    /**
     * Register one gene.
     *
     * @throws IllegalArgumentException if the key is malformed or already taken
     */
    public void register(Gene gene) {
        Genes.register(gene);
    }

    /** How many genes are registered so far - the length of the genotype code. */
    public int count() {
        return Genes.codeOrder().size();
    }
}
