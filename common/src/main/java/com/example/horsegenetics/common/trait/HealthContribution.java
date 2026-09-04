package com.example.horsegenetics.common.trait;

/**
 * A {@link TraitContribution} that is <b>health genetics</b> - a disorder, not
 * a performance or size trait. The marker exists for exactly one reason: the
 * server config's "off" position has to be able to switch the whole disease
 * layer out of a world without switching out the genes, and this is how
 * {@link HorseTraits#resolve(com.example.horsegenetics.common.genetics.Genotype, boolean)}
 * knows which contributions to skip.
 *
 * <p>The genes themselves are <b>always registered and always inherited</b>,
 * whatever the config says: turning the disorders off must not change the
 * genotype code, the founder draw or a single coat, or two players with
 * different settings would be breeding different animals. All it changes is
 * whether the horse standing in front of you is affected by what it carries.
 *
 * <p>A gene may paint a coat <i>and</i> implement this - overo lethal white is
 * a white pattern and a foal lethal at the same time. Only the trait
 * contribution is gated; the coat never is.
 */
public interface HealthContribution extends TraitContribution {
}
