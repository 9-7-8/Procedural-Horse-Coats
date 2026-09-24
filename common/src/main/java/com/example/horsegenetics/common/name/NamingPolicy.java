package com.example.horsegenetics.common.name;

import com.example.horsegenetics.common.horse.Sex;

import java.util.Locale;

/**
 * <b>Which half of a foal's name it inherits, and from which parent.</b>
 *
 * <p>A foal's name is always one inherited half and one freshly rolled word -
 * see {@link HorseNames#foal}. This says which is which. It is a player's
 * setting rather than a world's: on a server every player names <i>their own</i>
 * horses their own way, so this travels as data next to the player it belongs
 * to and never as a global.
 *
 * <p>The default is {@link #DEFAULT}: a filly carries her dam's last name, a
 * colt his sire's, and the first name is rolled. That is a real surname line -
 * follow the last names and you are reading a family - while the rolled first
 * name is what keeps two full names from colliding, which the old
 * foal-count schedule did not.
 */
public record NamingPolicy(InheritedHalf half, ParentSource source) {

    /** Surnames down the same-sex line, first names rolled. */
    public static final NamingPolicy DEFAULT = new NamingPolicy(InheritedHalf.LAST, ParentSource.BY_SEX);

    /** Which half of the parent's name the foal keeps; the other half is rolled. */
    public enum InheritedHalf {
        /** The foal keeps the parent's last name and rolls a first name. */
        LAST,
        /** The foal keeps the parent's first name and rolls a last name. */
        FIRST;

        /** The name in a config file, lower case. */
        public String configName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** Which parent the inherited half comes from. */
    public enum ParentSource {
        /** A filly takes it from her dam, a colt from his sire. */
        BY_SEX,
        /** Always the dam, whatever the foal is. */
        DAM,
        /** Always the sire, whatever the foal is. */
        SIRE;

        /** The name in a config file, lower case, with {@code _} as {@code -}. */
        public String configName() {
            return name().toLowerCase(Locale.ROOT).replace('_', '-');
        }
    }

    public NamingPolicy {
        if (half == null || source == null) {
            throw new IllegalArgumentException("a naming policy needs both a half and a source");
        }
    }

    /**
     * Which parent supplies the inherited half for a foal of this sex.
     *
     * @param foal the foal's sex, from its own Mendelian draw
     * @return true for the dam, false for the sire
     */
    public boolean inheritsFromDam(Sex foal) {
        return switch (source) {
            case DAM -> true;
            case SIRE -> false;
            case BY_SEX -> foal == Sex.FEMALE;
        };
    }
}
