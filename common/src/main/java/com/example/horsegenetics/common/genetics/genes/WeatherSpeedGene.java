package com.example.horsegenetics.common.genetics.genes;

/**
 * <b>Weather sensitive (speed)</b> ({@code horsegenetics.weather_speed}) - how
 * the sky changes how fast the horse moves.
 *
 * <p>Unlinked to {@link WeatherJumpGene}, deliberately. A single weather locus
 * moving both stats would make "good in the rain" one thing to breed for, and
 * one selection pressure produces one kind of horse. Two independent loci give
 * four corners - fast and springy in a storm, fast and flat, and so on - which
 * is four lines a breeder can keep apart.
 *
 * <p>See {@link AbstractWeatherGene} for the shape, and for why this is not the
 * copy of {@link MagicSwimSpeedGene} it was first specified as.
 */
public final class WeatherSpeedGene extends AbstractWeatherGene {

    public static final String KEY = "horsegenetics.weather_speed";
    public static final int PRIORITY = 175;

    public WeatherSpeedGene() {
        super(KEY, PRIORITY, "Weather sensitive (speed)", "movement_speed", "moving speed",
                "The horse moves at the same pace whatever the weather does.");
    }

    @Override
    protected String idPrefix() {
        return "wspeed";
    }
}
