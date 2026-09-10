package com.example.horsegenetics.common.genetics.genes;

/**
 * <b>Weather sensitive (jump)</b> ({@code horsegenetics.weather_jump}) - how the
 * sky changes how high the horse jumps.
 *
 * <p>The same class as {@link WeatherSpeedGene} with a different attribute and a
 * different key, and resisting the urge to make one parameterised locus with two
 * outputs is the point: one locus would inherit as one unit, and the whole
 * reason there are two is that they do not.
 */
public final class WeatherJumpGene extends AbstractWeatherGene {

    public static final String KEY = "horsegenetics.weather_jump";
    public static final int PRIORITY = 176;

    public WeatherJumpGene() {
        super(KEY, PRIORITY, "Weather sensitive (jump)", "jump_strength", "jump",
                "The horse jumps the same height whatever the weather does.");
    }

    @Override
    protected String idPrefix() {
        return "wjump";
    }
}
