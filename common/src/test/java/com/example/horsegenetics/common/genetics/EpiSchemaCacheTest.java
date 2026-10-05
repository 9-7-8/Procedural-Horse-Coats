package com.example.horsegenetics.common.genetics;

import com.example.horsegenetics.common.coat.pattern.CoatBuildContext;
import com.example.horsegenetics.common.coat.pattern.PigmentField;
import com.example.horsegenetics.common.coat.pattern.PigmentView;
import com.example.horsegenetics.common.genetics.epi.EpiSchema;
import com.example.horsegenetics.common.genetics.epi.EpiValue;
import com.example.horsegenetics.common.genetics.genes.AbstractNaturalGene;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link Epigenome#schemaOf} (#205): one schema per gene, built once, and the same
 * answer the gene itself gives.
 */
class EpiSchemaCacheTest {

    /** A gene whose one number runs from 0 to {@code max} - two of them can share a key. */
    private static final class Knobbed extends AbstractNaturalGene {
        private final double max;

        Knobbed(double max) {
            super(gene("test.knobbed", 45, "Knobbed")
                    .variant("Kn", "Knobbed (Kn)")
                    .recessive()
                    .hardyWeinberg(0.1)
                    .wild("Nothing happens.")
                    .carrier("carried", "Carrier", "One copy, invisible.")
                    .outcome("knobbed", "Knobbed", "Nothing visible either."));
            this.max = max;
        }

        @Override
        protected PigmentField restrict(CoatBuildContext ctx, PigmentView coat) {
            return coat.mutableCopy();
        }

        @Override
        public EpiSchema epiSchema() {
            return EpiSchema.of(EpiValue.uniform("knob", 0, max));
        }
    }

    @Test
    void aGeneIsAskedOnceAndAnsweredWithTheSameSchemaAfterwards() {
        for (Gene g : Genes.codeOrder()) {
            assertSame(Epigenome.schemaOf(g), Epigenome.schemaOf(g), g.key());
        }
    }

    @Test
    void theCachedSchemaIsTheOneTheGeneBuilds() {
        for (Gene g : Genes.codeOrder()) {
            assertEquals(g.epiSchema().values(), Epigenome.schemaOf(g).values(), g.key());
        }
    }

    @Test
    void twoGenesUnderOneKeyEachReadTheirOwnSchema() {
        // What a test re-registering a JSON gene does: a new instance, the same key.
        // A cache keyed by the key string would hand the second one the first's schema.
        Knobbed narrow = new Knobbed(1.0);
        Knobbed wide = new Knobbed(3.0);
        assertEquals(1.0, Epigenome.schemaOf(narrow).values().get(0).max());
        assertEquals(3.0, Epigenome.schemaOf(wide).values().get(0).max());
        // And the midpoints an empty copy reads, cached beside the schemas.
        assertEquals(0.5, Epigenome.readable(narrow, AlleleEpigenetics.NONE).get("knob"), 1e-9);
        assertEquals(1.5, Epigenome.readable(wide, AlleleEpigenetics.NONE).get("knob"), 1e-9);
    }
}
