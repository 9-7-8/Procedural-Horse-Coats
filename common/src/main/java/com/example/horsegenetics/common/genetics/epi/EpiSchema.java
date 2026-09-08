package com.example.horsegenetics.common.genetics.epi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>Every number one gene writes on an allele copy</b>, declared in order -
 * the gene's half of the epigenetics contract, and the thing that makes drift,
 * storage and the in-game inspector generic instead of per-gene.
 *
 * <p>A gene returns one from {@code Gene.epiSchema()}. Nothing else has to know
 * what the numbers <i>mean</i>: {@link EpiRoll} can roll a founder's,
 * {@link EpiDrift} can nudge them, {@code Epigenome} can write and read them,
 * and the inspector can list them, all by walking this.
 *
 * <p>The ~39 genes whose behaviour is fixed by their alleles - a {@code p/p}
 * pony is one particular pony and always the same one - return {@link #EMPTY}
 * and store nothing at all. That is most of the registry, and it is why the
 * stored epigenome shrank from a segment per registered gene to a segment per
 * <i>varying</i> gene even as each segment grew.
 *
 * <h2>Order is the storage layout, not the meaning</h2>
 * Unlike the draw orders this replaced, <b>re-ordering a schema does not
 * rewrite every horse</b> - values are stored and read <i>by name</i>, so the
 * order here only decides how the code string is laid out. Renaming a value
 * does orphan it (it reads as a fresh roll on the next parse); adding one in
 * the middle is free. That is the main practical win of literal storage while
 * a gene is being tuned.
 */
public final class EpiSchema {

    /** For a gene whose behaviour is a pure function of its alleles. */
    public static final EpiSchema EMPTY = new EpiSchema(List.of());

    private final List<EpiValue> values;
    private final Map<String, Integer> indexByName;
    /** Where each value's scalars start in {@link EpiValues}' flat array. */
    private final int[] scalarOffset;
    private final int scalarWidth;

    private EpiSchema(List<EpiValue> values) {
        this.values = List.copyOf(values);
        Map<String, Integer> names = new LinkedHashMap<>();
        this.scalarOffset = new int[values.size()];
        int offset = 0;
        for (int i = 0; i < values.size(); i++) {
            EpiValue v = values.get(i);
            if (names.put(v.name(), i) != null) {
                throw new IllegalArgumentException("duplicate epigenetic value name: " + v.name());
            }
            scalarOffset[i] = offset;
            if (v.kind() != EpiValue.Kind.SEED) {
                offset += v.arity();
            }
        }
        this.scalarWidth = offset;
        this.indexByName = Collections.unmodifiableMap(names);
    }

    public static EpiSchema of(EpiValue... values) {
        return values.length == 0 ? EMPTY : new EpiSchema(List.of(values));
    }

    public static EpiSchema of(List<EpiValue> values) {
        return values.isEmpty() ? EMPTY : new EpiSchema(values);
    }

    /** This schema plus {@code extra} appended - for a gene built on a shared base. */
    public EpiSchema and(EpiValue... extra) {
        List<EpiValue> all = new ArrayList<>(values);
        Collections.addAll(all, extra);
        return new EpiSchema(all);
    }

    public List<EpiValue> values() {
        return values;
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public int size() {
        return values.size();
    }

    public EpiValue get(int index) {
        return values.get(index);
    }

    /** {@code -1} if this schema declares no value by that name. */
    public int indexOf(String name) {
        Integer i = indexByName.get(name);
        return i == null ? -1 : i;
    }

    /**
     * The index of {@code name}, or a hard failure. A gene asking for a value it
     * did not declare is a bug in the gene, not a runtime condition to absorb -
     * absorbing it would paint a silently wrong horse.
     */
    public int require(String name) {
        int i = indexOf(name);
        if (i < 0) {
            throw new IllegalArgumentException("no epigenetic value named '" + name
                    + "'; this schema declares " + indexByName.keySet());
        }
        return i;
    }

    int scalarOffset(int index) {
        return scalarOffset[index];
    }

    int scalarWidth() {
        return scalarWidth;
    }

    /**
     * Every value at its {@link EpiValue#midpoint()} - what a genotype with no
     * horse behind it reports. See {@code HorseTraits.resolve(Genotype)}.
     */
    public EpiValues midpoint() {
        double[] scalars = new double[scalarWidth];
        long[] seeds = new long[values.size()];
        for (int i = 0; i < values.size(); i++) {
            EpiValue v = values.get(i);
            if (v.kind() == EpiValue.Kind.SEED) {
                continue;
            }
            double mid = v.midpoint();
            for (int k = 0; k < v.arity(); k++) {
                scalars[scalarOffset[i] + k] = mid;
            }
        }
        return new EpiValues(this, scalars, seeds);
    }
}
