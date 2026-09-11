package com.example.horsegenetics.common.genetics.eye;

import com.example.horsegenetics.common.CommonMaps;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>"A horse like this has eyes like that"</b> - one gene's claim on the eye
 * loci, expressed as alleles rather than as paint.
 *
 * <h2>Why a request and not a colour</h2>
 * Seven natural genes used to paint an iris themselves - cream and pearl, champagne,
 * tiger eye, and the four white loci - each declaring its own hex value and
 * competing with the others through a rank. That made an eye colour something a
 * <i>gene</i> owned, which is exactly backwards: a horse's eye colour is a
 * heritable trait of the horse, and it now lives at
 * {@link EyeLocus#IRIS_LEFT the eye loci} like any other.
 *
 * <p>So a gene that has something to say about an eye says it here, in the eye
 * loci's own vocabulary, and {@link Eyes#force} <b>writes the allele onto the
 * horse</b> when the horse is made. A splashed white foal really is
 * {@code MBl/MBl} at both iris loci: it shows blue eyes, and it passes blue
 * eyes on. That is the owner's call and it is the whole difference between this
 * and an override - the eye a white horse has is the eye its foals can inherit,
 * splash or no splash.
 *
 * <h2>Writing one</h2>
 * A request is a small map from locus to allele token. Build it with the
 * fluent {@link #iris}, {@link #sector} and friends:
 *
 * <pre>
 *   EyeRequest.none()
 *       .iris(CoatRegions.RIGHT_EYE, EyeHue.MID_BLUE)
 *       .iris(CoatRegions.LEFT_EYE,  EyeHue.MID_BLUE)
 * </pre>
 *
 * <h2>When two genes both ask</h2>
 * Requests are collected in {@link com.example.horsegenetics.common.genetics.Genes#codeOrder()}
 * and a later gene's claim on a locus <b>replaces</b> an earlier one, the same
 * last-writer-wins rule the overlay phase runs on. That is why the white loci
 * sort after the dilutions: a cremello that is also splashed has blue eyes
 * because {@code KIT} and {@code MITF} come after {@code SLC45A2}, not because
 * anything ranks them. The old {@code EyeColor.rank()} argument about melanin is
 * now simply the gene order, which is where every other ordering in this mod
 * already lives.
 */
public final class EyeRequest {

    private static final EyeRequest NONE = new EyeRequest(CommonMaps.empty());

    private final Map<EyeLocus, String> forced;

    private EyeRequest(Map<EyeLocus, String> forced) {
        this.forced = forced;
    }

    /** The empty request - what a gene with nothing to say about an eye returns. */
    public static EyeRequest none() {
        return NONE;
    }

    public boolean isEmpty() {
        return forced.isEmpty();
    }

    /** The loci this request forces, and the allele token each is forced to. */
    public Map<EyeLocus, String> forced() {
        return forced;
    }

    /** This request with one more locus forced. */
    public EyeRequest with(EyeLocus locus, String alleleToken) {
        Map<EyeLocus, String> m = new LinkedHashMap<>(forced);
        m.put(locus, alleleToken);
        return new EyeRequest(CommonMaps.copyOf(m));
    }

    /** Force one eye's iris to {@code hue}. */
    public EyeRequest iris(int eye, EyeHue hue) {
        return with(EyeLocus.iris(eye), hue.token());
    }

    /** Force both irises to {@code hue} - much the commonest request. */
    public EyeRequest bothIrises(EyeHue hue) {
        return iris(EyeLocus.EyeSideRef.RIGHT, hue).iris(EyeLocus.EyeSideRef.LEFT, hue);
    }

    /** Force one eye's sector, and the colour that sector is painted. */
    public EyeRequest sector(int eye, EyeSector sector, EyeHue hue) {
        return with(EyeLocus.sector(eye), sector.token())
                .with(EyeLocus.sectorColour(eye), hue.token());
    }

    /** Force one eye's sclera to {@code hue}. */
    public EyeRequest sclera(int eye, EyeHue hue) {
        return with(EyeLocus.sclera(eye), hue.token());
    }

    /**
     * Every request on this horse, in {@link com.example.horsegenetics.common.genetics.Genes#codeOrder()},
     * folded into one - later claims replacing earlier ones on the same locus.
     */
    public static EyeRequest merge(List<EyeRequest> requests) {
        Map<EyeLocus, String> m = new LinkedHashMap<>();
        List<EyeRequest> ordered = new ArrayList<>(requests);
        for (EyeRequest r : ordered) {
            m.putAll(r.forced);
        }
        return m.isEmpty() ? NONE : new EyeRequest(CommonMaps.copyOf(m));
    }

    @Override
    public String toString() {
        return "EyeRequest" + forced;
    }
}
