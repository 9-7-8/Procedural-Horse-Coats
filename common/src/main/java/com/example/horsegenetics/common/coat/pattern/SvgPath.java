package com.example.horsegenetics.common.coat.pattern;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>An SVG path, flattened once and then measured per texel</b> - the half of
 * the {@code SVG} mask that has nothing to do with horses.
 *
 * <p>{@code PATH} carries a polyline and a Catmull-Rom flag, which is enough to
 * trace an outline by hand and not much else. Every marking anybody actually
 * <i>has</i> a drawing of arrives as an SVG: a {@code d} string of absolute and
 * relative moves, cubics, quadratics, their smooth continuations and elliptical
 * arcs, split into subpaths, sitting inside a {@code viewBox} and usually under
 * a {@code transform}. Re-typing one of those as sixty-four normalised control
 * points is the step where a drawing stops being the drawing.
 *
 * <p>So this class is deliberately the whole of the grammar rather than the
 * convenient part of it:
 * <ul>
 *   <li><b>Every path command</b> - {@code M m L l H h V v C c S s Q q T t A a
 *       Z z}, including the reflected control point {@code S} and {@code T}
 *       carry and the endpoint-parameterised elliptical arc {@code A}, which is
 *       the one people assume is never used and which every circle exported by
 *       a drawing program is made of.</li>
 *   <li><b>Subpaths.</b> A path is a list of them, each open or closed, and
 *       the hole in a letter O is a second subpath wound the other way - which
 *       is only a hole if the fill rule is consulted, so the fill rule is a
 *       parameter and not an assumption.</li>
 *   <li><b>The transform list</b> - {@code matrix translate scale rotate skewX
 *       skewY}, composed left to right the way a renderer composes them.</li>
 *   <li><b>{@code viewBox} and {@code preserveAspectRatio}</b>, both of which
 *       decide where the drawing lands and neither of which is recoverable
 *       afterwards.</li>
 * </ul>
 *
 * <h2>Where the work happens</h2>
 * <b>Flattening runs once, at load.</b> Curves and arcs become polylines in the
 * file's own user space, the {@code transform} is applied to those points (an
 * affine map takes a polyline to a polyline exactly, so nothing is lost by
 * flattening first), and the result is cached on the {@link Shape}. The painter
 * then does per texel what {@code PATH} does per texel: walk segments, take a
 * distance, and count crossings. Only the {@code viewBox} fit is left to paint
 * time, because the viewport it maps into is written in body units and those
 * may come off a knob.
 *
 * <p><b>The point ceiling is a cost ceiling.</b> Every mapped texel of every
 * skin walks every segment, so {@link #MAX_POINTS} is what stops somebody
 * pasting a traced photograph into a gene file and wondering why the game takes
 * a minute to make a horse. Per-subpath bounding boxes cut most of that walk on
 * a drawing made of several separate marks, which is the shape most of them
 * are.
 *
 * <p>Pure and game-free, like everything else here: same {@code d} in, same
 * numbers out, on every platform this module is compiled for.
 */
public final class SvgPath {

    /**
     * The most points a flattened path may carry. Generous next to
     * {@code PATH}'s sixty-four because a real drawing has several subpaths and
     * every arc in it becomes a polyline - and finite for the reason
     * {@code PATH}'s limit is finite, only more so, since nothing here asks the
     * author to type the points and so nothing here makes them count.
     */
    public static final int MAX_POINTS = 1024;

    /**
     * Straight sub-segments per cubic or quadratic span. Sixteen rather than
     * {@code PATH}'s eight because an SVG curve spans whatever the artist drew
     * rather than the gap between two adjacent control points, so one span can
     * cross the whole drawing.
     */
    public static final int CURVE_SAMPLES = 16;

    /**
     * Straight sub-segments per full turn of an elliptical arc. A quarter-turn
     * - the arc a rounded corner is made of - therefore gets twelve, which puts
     * the chord error under a thousandth of the radius.
     */
    public static final int ARC_SAMPLES_PER_TURN = 48;

    /** Where a {@code preserveAspectRatio} may put the drawing inside its viewport. */
    public static final List<String> ALIGNMENTS = List.of(
            "xMidYMid", "xMinYMin", "xMidYMin", "xMaxYMin",
            "xMinYMid", "xMaxYMid", "xMinYMax", "xMidYMax", "xMaxYMax");

    /**
     * A path after flattening: points in the file's own user space, grouped into
     * subpaths.
     *
     * <p>Arrays rather than objects, and handed back rather than copied, for the
     * reason {@code Params.points} gives - the painter asks for them once per
     * texel and nothing downstream writes to them.
     *
     * @param d        the source string, kept so the creator can write the file back out
     * @param xs       every point's x, all subpaths end to end
     * @param ys       every point's y
     * @param cum      arc length from the start of <i>this point's subpath</i> to it,
     *                 in user units - what the dash pattern is measured along
     * @param starts   index into {@code xs} where each subpath begins; one extra
     *                 entry at the end holding {@code xs.length}, so a subpath's
     *                 extent is always {@code starts[i]} to {@code starts[i + 1]}
     * @param closed   whether each subpath ended in a {@code Z}
     * @param subBox   per subpath, {@code minX minY maxX maxY} - the early-out
     * @param box      the whole path's {@code minX minY maxX maxY}
     */
    public record Shape(String d, double[] xs, double[] ys, double[] cum,
                        int[] starts, boolean[] closed, double[] subBox, double[] box) {

        public int subpaths() {
            return closed.length;
        }

        public double width() {
            return box[2] - box[0];
        }

        public double height() {
            return box[3] - box[1];
        }
    }

    private SvgPath() {}

    // ------------------------------------------------------------------
    // Parsing
    // ------------------------------------------------------------------

    /**
     * Flatten a {@code d} string, optionally under an SVG {@code transform}
     * list.
     *
     * @param transform an SVG transform list, or null / empty for none
     * @throws IllegalArgumentException with the offending character's index for
     *                                  anything the grammar does not allow -
     *                                  this runs at load, so a typo should name
     *                                  itself rather than paint nothing
     */
    public static Shape parse(String d, String transform) {
        List<double[]> subX = new ArrayList<>();
        List<double[]> subY = new ArrayList<>();
        List<Boolean> subClosed = new ArrayList<>();
        flatten(d, subX, subY, subClosed);

        if (subX.isEmpty()) {
            throw new IllegalArgumentException("path data draws nothing - it has no subpath with two points in it");
        }

        double[] m = transform == null || transform.trim().isEmpty()
                ? new double[]{1, 0, 0, 1, 0, 0}
                : transformMatrix(transform);

        int total = 0;
        for (double[] xs : subX) {
            total += xs.length;
        }
        if (total > MAX_POINTS) {
            throw new IllegalArgumentException("flattens to " + total + " points, over the " + MAX_POINTS
                    + "-point ceiling - every texel of every skin walks all of them. Simplify the "
                    + "drawing, or split it across two masks");
        }

        double[] xs = new double[total];
        double[] ys = new double[total];
        double[] cum = new double[total];
        int[] starts = new int[subX.size() + 1];
        boolean[] closedOut = new boolean[subX.size()];
        double[] subBox = new double[subX.size() * 4];
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        int at = 0;
        for (int s = 0; s < subX.size(); s++) {
            starts[s] = at;
            closedOut[s] = subClosed.get(s);
            double[] px = subX.get(s);
            double[] py = subY.get(s);
            double sMinX = Double.MAX_VALUE;
            double sMinY = Double.MAX_VALUE;
            double sMaxX = -Double.MAX_VALUE;
            double sMaxY = -Double.MAX_VALUE;
            double run = 0;
            for (int i = 0; i < px.length; i++) {
                double x = m[0] * px[i] + m[2] * py[i] + m[4];
                double y = m[1] * px[i] + m[3] * py[i] + m[5];
                if (i > 0) {
                    double dx = x - xs[at - 1];
                    double dy = y - ys[at - 1];
                    run += Math.sqrt(dx * dx + dy * dy);
                }
                xs[at] = x;
                ys[at] = y;
                cum[at] = run;
                sMinX = Math.min(sMinX, x);
                sMinY = Math.min(sMinY, y);
                sMaxX = Math.max(sMaxX, x);
                sMaxY = Math.max(sMaxY, y);
                at++;
            }
            subBox[s * 4] = sMinX;
            subBox[s * 4 + 1] = sMinY;
            subBox[s * 4 + 2] = sMaxX;
            subBox[s * 4 + 3] = sMaxY;
            minX = Math.min(minX, sMinX);
            minY = Math.min(minY, sMinY);
            maxX = Math.max(maxX, sMaxX);
            maxY = Math.max(maxY, sMaxY);
        }
        starts[subX.size()] = at;
        return new Shape(d, xs, ys, cum, starts, closedOut, subBox,
                new double[]{minX, minY, maxX, maxY});
    }

    /**
     * Walk the {@code d} grammar, emitting one flattened polyline per subpath.
     *
     * <p>Curves and arcs are sampled here rather than kept as curves, because
     * everything downstream - distance, crossings, arc length, the transform -
     * is exact on a polyline and approximate on anything else.
     */
    private static void flatten(String d, List<double[]> subX, List<double[]> subY, List<Boolean> closed) {
        Reader r = new Reader(d);
        List<Double> xs = new ArrayList<>();
        List<Double> ys = new ArrayList<>();
        double cx = 0;
        double cy = 0;
        double startX = 0;
        double startY = 0;
        // The reflected control point S and T carry. Null-equivalent is "the
        // current point", which is what the spec says to use when the previous
        // command was not the matching curve type.
        double lastCubicCx = 0;
        double lastCubicCy = 0;
        double lastQuadCx = 0;
        double lastQuadCy = 0;
        boolean hadCubic = false;
        boolean hadQuad = false;
        char cmd = 0;

        while (true) {
            r.skip();
            if (r.done()) {
                break;
            }
            char c = r.peek();
            if (isCommand(c)) {
                if (cmd == 0 && c != 'M' && c != 'm') {
                    // Every path begins somewhere, and a file that opens with a
                    // lineto has no current point for it to run from. Saying so
                    // beats "draws nothing", which is what it used to reach.
                    throw new IllegalArgumentException("path data must start with a moveto, got '"
                            + c + "' at index " + r.at());
                }
                cmd = c;
                r.next();
            } else if (cmd == 0) {
                throw new IllegalArgumentException("path data must start with a moveto, got '" + c
                        + "' at index " + r.at());
            } else if (cmd == 'M') {
                cmd = 'L';      // repeated coordinates after a moveto are linetos
            } else if (cmd == 'm') {
                cmd = 'l';
            }

            boolean rel = Character.isLowerCase(cmd);
            switch (Character.toUpperCase(cmd)) {
                case 'M': {
                    double x = r.number();
                    double y = r.number();
                    if (rel) {
                        x += cx;
                        y += cy;
                    }
                    emit(subX, subY, closed, xs, ys, false);
                    xs.clear();
                    ys.clear();
                    xs.add(x);
                    ys.add(y);
                    cx = x;
                    cy = y;
                    startX = x;
                    startY = y;
                    hadCubic = false;
                    hadQuad = false;
                    break;
                }
                case 'L': {
                    double x = r.number();
                    double y = r.number();
                    if (rel) {
                        x += cx;
                        y += cy;
                    }
                    line(xs, ys, x, y);
                    cx = x;
                    cy = y;
                    hadCubic = false;
                    hadQuad = false;
                    break;
                }
                case 'H': {
                    double x = r.number();
                    if (rel) {
                        x += cx;
                    }
                    line(xs, ys, x, cy);
                    cx = x;
                    hadCubic = false;
                    hadQuad = false;
                    break;
                }
                case 'V': {
                    double y = r.number();
                    if (rel) {
                        y += cy;
                    }
                    line(xs, ys, cx, y);
                    cy = y;
                    hadCubic = false;
                    hadQuad = false;
                    break;
                }
                case 'C':
                case 'S': {
                    double x1;
                    double y1;
                    if (Character.toUpperCase(cmd) == 'S') {
                        // The reflection of the previous cubic's second control
                        // point through the current point - which is the whole
                        // of what makes an S curve smooth.
                        x1 = hadCubic ? 2 * cx - lastCubicCx : cx;
                        y1 = hadCubic ? 2 * cy - lastCubicCy : cy;
                    } else {
                        x1 = r.number();
                        y1 = r.number();
                        if (rel) {
                            x1 += cx;
                            y1 += cy;
                        }
                    }
                    double x2 = r.number();
                    double y2 = r.number();
                    double x = r.number();
                    double y = r.number();
                    if (rel) {
                        x2 += cx;
                        y2 += cy;
                        x += cx;
                        y += cy;
                    }
                    cubic(xs, ys, cx, cy, x1, y1, x2, y2, x, y);
                    lastCubicCx = x2;
                    lastCubicCy = y2;
                    hadCubic = true;
                    hadQuad = false;
                    cx = x;
                    cy = y;
                    break;
                }
                case 'Q':
                case 'T': {
                    double x1;
                    double y1;
                    if (Character.toUpperCase(cmd) == 'T') {
                        x1 = hadQuad ? 2 * cx - lastQuadCx : cx;
                        y1 = hadQuad ? 2 * cy - lastQuadCy : cy;
                    } else {
                        x1 = r.number();
                        y1 = r.number();
                        if (rel) {
                            x1 += cx;
                            y1 += cy;
                        }
                    }
                    double x = r.number();
                    double y = r.number();
                    if (rel) {
                        x += cx;
                        y += cy;
                    }
                    quad(xs, ys, cx, cy, x1, y1, x, y);
                    lastQuadCx = x1;
                    lastQuadCy = y1;
                    hadQuad = true;
                    hadCubic = false;
                    cx = x;
                    cy = y;
                    break;
                }
                case 'A': {
                    double rx = r.number();
                    double ry = r.number();
                    double rot = r.number();
                    boolean large = r.flag();
                    boolean sweep = r.flag();
                    double x = r.number();
                    double y = r.number();
                    if (rel) {
                        x += cx;
                        y += cy;
                    }
                    arc(xs, ys, cx, cy, rx, ry, rot, large, sweep, x, y);
                    cx = x;
                    cy = y;
                    hadCubic = false;
                    hadQuad = false;
                    break;
                }
                case 'Z': {
                    emit(subX, subY, closed, xs, ys, true);
                    xs.clear();
                    ys.clear();
                    cx = startX;
                    cy = startY;
                    // A subpath after a Z with no moveto starts at the point the
                    // Z returned to, which is the shape a chain of closed
                    // outlines written without repeating the M has.
                    xs.add(cx);
                    ys.add(cy);
                    hadCubic = false;
                    hadQuad = false;
                    break;
                }
                default:
                    throw new IllegalArgumentException("unknown path command '" + cmd + "' at index " + r.at());
            }
        }
        emit(subX, subY, closed, xs, ys, false);
    }

    private static boolean isCommand(char c) {
        return "MmLlHhVvCcSsQqTtAaZz".indexOf(c) >= 0;
    }

    private static void emit(List<double[]> subX, List<double[]> subY, List<Boolean> closed,
                             List<Double> xs, List<Double> ys, boolean isClosed) {
        if (xs.size() < 2) {
            return;     // a lone moveto, or the point a Z left behind
        }
        int n = xs.size();
        // A closed subpath's last point equal to its first is redundant: the
        // painter joins the ends itself, and a zero-length segment is a
        // division by zero waiting for a texel to land exactly on it.
        if (isClosed && n > 2
                && Math.abs(xs.get(n - 1) - xs.get(0)) < 1e-9
                && Math.abs(ys.get(n - 1) - ys.get(0)) < 1e-9) {
            n--;
        }
        double[] ax = new double[n];
        double[] ay = new double[n];
        for (int i = 0; i < n; i++) {
            ax[i] = xs.get(i);
            ay[i] = ys.get(i);
        }
        subX.add(ax);
        subY.add(ay);
        closed.add(isClosed);
    }

    private static void line(List<Double> xs, List<Double> ys, double x, double y) {
        if (!xs.isEmpty() && Math.abs(xs.get(xs.size() - 1) - x) < 1e-12
                && Math.abs(ys.get(ys.size() - 1) - y) < 1e-12) {
            return;
        }
        xs.add(x);
        ys.add(y);
    }

    private static void cubic(List<Double> xs, List<Double> ys, double x0, double y0,
                              double x1, double y1, double x2, double y2, double x3, double y3) {
        for (int i = 1; i <= CURVE_SAMPLES; i++) {
            double t = i / (double) CURVE_SAMPLES;
            double u = 1 - t;
            double a = u * u * u;
            double b = 3 * u * u * t;
            double c = 3 * u * t * t;
            double e = t * t * t;
            line(xs, ys, a * x0 + b * x1 + c * x2 + e * x3, a * y0 + b * y1 + c * y2 + e * y3);
        }
    }

    private static void quad(List<Double> xs, List<Double> ys, double x0, double y0,
                             double x1, double y1, double x2, double y2) {
        for (int i = 1; i <= CURVE_SAMPLES; i++) {
            double t = i / (double) CURVE_SAMPLES;
            double u = 1 - t;
            double a = u * u;
            double b = 2 * u * t;
            double c = t * t;
            line(xs, ys, a * x0 + b * x1 + c * x2, a * y0 + b * y1 + c * y2);
        }
    }

    /**
     * The endpoint-parameterised elliptical arc, turned into a centre and an
     * angular sweep and then sampled.
     *
     * <p>This is the transcription of the implementation notes in the SVG
     * specification's appendix, including its two corrections: a radius too
     * small to reach the endpoint is scaled up until it does, and a zero radius
     * degenerates to a straight line rather than to a division by zero.
     */
    private static void arc(List<Double> xs, List<Double> ys, double x0, double y0,
                            double rx, double ry, double rotDeg, boolean large, boolean sweep,
                            double x1, double y1) {
        rx = Math.abs(rx);
        ry = Math.abs(ry);
        if (rx < 1e-9 || ry < 1e-9 || (Math.abs(x1 - x0) < 1e-12 && Math.abs(y1 - y0) < 1e-12)) {
            line(xs, ys, x1, y1);
            return;
        }
        double phi = Math.toRadians(rotDeg);
        double cos = Math.cos(phi);
        double sin = Math.sin(phi);

        double dx2 = (x0 - x1) / 2;
        double dy2 = (y0 - y1) / 2;
        double px = cos * dx2 + sin * dy2;
        double py = -sin * dx2 + cos * dy2;

        double lambda = (px * px) / (rx * rx) + (py * py) / (ry * ry);
        if (lambda > 1) {
            double k = Math.sqrt(lambda);
            rx *= k;
            ry *= k;
        }

        double num = rx * rx * ry * ry - rx * rx * py * py - ry * ry * px * px;
        double den = rx * rx * py * py + ry * ry * px * px;
        double factor = Math.sqrt(Math.max(0, num / den)) * (large == sweep ? -1 : 1);
        double cxp = factor * rx * py / ry;
        double cyp = -factor * ry * px / rx;

        double cx = cos * cxp - sin * cyp + (x0 + x1) / 2;
        double cy = sin * cxp + cos * cyp + (y0 + y1) / 2;

        double theta = angle(1, 0, (px - cxp) / rx, (py - cyp) / ry);
        double delta = angle((px - cxp) / rx, (py - cyp) / ry, (-px - cxp) / rx, (-py - cyp) / ry);
        if (!sweep && delta > 0) {
            delta -= 2 * Math.PI;
        } else if (sweep && delta < 0) {
            delta += 2 * Math.PI;
        }

        int steps = Math.max(2, (int) Math.ceil(Math.abs(delta) / (2 * Math.PI) * ARC_SAMPLES_PER_TURN));
        for (int i = 1; i <= steps; i++) {
            double t = theta + delta * i / steps;
            double ex = rx * Math.cos(t);
            double ey = ry * Math.sin(t);
            line(xs, ys, cx + cos * ex - sin * ey, cy + sin * ex + cos * ey);
        }
    }

    private static double angle(double ux, double uy, double vx, double vy) {
        double dot = ux * vx + uy * vy;
        double len = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
        double a = Math.acos(Math.max(-1, Math.min(1, dot / len)));
        return ux * vy - uy * vx < 0 ? -a : a;
    }

    // ------------------------------------------------------------------
    // Transforms
    // ------------------------------------------------------------------

    /**
     * An SVG {@code transform} list as one {@code [a b c d e f]} matrix,
     * composed <b>left to right</b> - which is to say the leftmost transform in
     * the string is the outermost, exactly as a renderer applies them.
     */
    public static double[] transformMatrix(String list) {
        double[] m = {1, 0, 0, 1, 0, 0};
        int i = 0;
        int n = list.length();
        while (i < n) {
            while (i < n && (Character.isWhitespace(list.charAt(i)) || list.charAt(i) == ',')) {
                i++;
            }
            if (i >= n) {
                break;
            }
            int nameEnd = i;
            while (nameEnd < n && Character.isLetter(list.charAt(nameEnd))) {
                nameEnd++;
            }
            String name = list.substring(i, nameEnd);
            int open = list.indexOf('(', nameEnd);
            int close = open < 0 ? -1 : list.indexOf(')', open);
            if (open < 0 || close < 0) {
                throw new IllegalArgumentException("transform '" + name + "' has no bracketed arguments");
            }
            double[] a = numbers(list.substring(open + 1, close));
            m = multiply(m, single(name, a));
            i = close + 1;
        }
        return m;
    }

    private static double[] single(String name, double[] a) {
        switch (name) {
            case "matrix":
                need(name, a, 6);
                return new double[]{a[0], a[1], a[2], a[3], a[4], a[5]};
            case "translate":
                return new double[]{1, 0, 0, 1, a.length > 0 ? a[0] : 0, a.length > 1 ? a[1] : 0};
            case "scale": {
                double sx = a.length > 0 ? a[0] : 1;
                double sy = a.length > 1 ? a[1] : sx;
                return new double[]{sx, 0, 0, sy, 0, 0};
            }
            case "rotate": {
                need(name, a, 1);
                double r = Math.toRadians(a[0]);
                double cos = Math.cos(r);
                double sin = Math.sin(r);
                double[] rot = {cos, sin, -sin, cos, 0, 0};
                if (a.length < 3) {
                    return rot;
                }
                // rotate(a, cx, cy) is translate(cx,cy) rotate(a) translate(-cx,-cy).
                return multiply(multiply(new double[]{1, 0, 0, 1, a[1], a[2]}, rot),
                        new double[]{1, 0, 0, 1, -a[1], -a[2]});
            }
            case "skewX":
                need(name, a, 1);
                return new double[]{1, 0, Math.tan(Math.toRadians(a[0])), 1, 0, 0};
            case "skewY":
                need(name, a, 1);
                return new double[]{1, Math.tan(Math.toRadians(a[0])), 0, 1, 0, 0};
            default:
                throw new IllegalArgumentException("unknown transform '" + name + "' - the list is "
                        + "matrix, translate, scale, rotate, skewX, skewY");
        }
    }

    private static void need(String name, double[] a, int count) {
        if (a.length < count) {
            throw new IllegalArgumentException("transform '" + name + "' needs " + count
                    + " arguments, got " + a.length);
        }
    }

    /** {@code a} then {@code b} - the composition a renderer performs for {@code "a b"}. */
    private static double[] multiply(double[] a, double[] b) {
        return new double[]{
                a[0] * b[0] + a[2] * b[1],
                a[1] * b[0] + a[3] * b[1],
                a[0] * b[2] + a[2] * b[3],
                a[1] * b[2] + a[3] * b[3],
                a[0] * b[4] + a[2] * b[5] + a[4],
                a[1] * b[4] + a[3] * b[5] + a[5]};
    }

    /** A whitespace- or comma-separated number list, as {@code viewBox} and the transforms write one. */
    public static double[] numbers(String s) {
        Reader r = new Reader(s);
        List<Double> out = new ArrayList<>();
        while (true) {
            r.skip();
            if (r.done()) {
                break;
            }
            out.add(r.number());
        }
        double[] a = new double[out.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = out.get(i);
        }
        return a;
    }

    // ------------------------------------------------------------------
    // Fitting
    // ------------------------------------------------------------------

    /**
     * The {@code viewBox} to viewport map, as {@code [scaleX, scaleY, offsetX,
     * offsetY]} - apply as {@code out = in * scale + offset}.
     *
     * <p>This is {@code preserveAspectRatio}, and it is here rather than folded
     * into the flattening because the viewport is written in <b>body units</b>
     * and those can come off a knob: a gene whose mark grows with the horse
     * changes its viewport every time it is painted.
     *
     * @param fit {@code meet} fits the whole drawing inside the viewport and
     *            leaves slack on one axis, {@code slice} fills the viewport and
     *            lets the drawing overflow the other, {@code none} stretches to
     *            fill and does not preserve the ratio at all
     */
    public static double[] fit(double[] viewBox, double vx, double vy, double vw, double vh,
                               String fit, String align) {
        double bw = Math.max(1e-9, viewBox[2]);
        double bh = Math.max(1e-9, viewBox[3]);
        double sx = vw / bw;
        double sy = vh / bh;
        if (!"none".equals(fit)) {
            double s = "slice".equals(fit) ? Math.max(sx, sy) : Math.min(sx, sy);
            sx = s;
            sy = s;
        }
        double slackX = vw - bw * sx;
        double slackY = vh - bh * sy;
        double ax = align.contains("xMax") ? 1.0 : (align.contains("xMin") ? 0.0 : 0.5);
        double ay = align.contains("YMax") ? 1.0 : (align.contains("YMin") ? 0.0 : 0.5);
        return new double[]{sx, sy, vx - viewBox[0] * sx + slackX * ax, vy - viewBox[1] * sy + slackY * ay};
    }

    // ------------------------------------------------------------------
    // Measuring
    // ------------------------------------------------------------------

    /**
     * <b>Is {@code (x, y)} inside the shape?</b> - the fill rule, in the
     * shape's own user space.
     *
     * <p>{@code nonzero} counts which <i>way</i> each crossing went and asks
     * whether the total is anything but zero; {@code evenodd} only counts them.
     * The difference is the whole of what makes the hole in a letter O a hole:
     * two subpaths wound the same way are a solid blob under {@code nonzero}
     * and a ring under {@code evenodd}, and which one the artist meant is
     * recorded in the file rather than guessable from the geometry.
     *
     * <p>Every subpath is treated as closed here whether it was or not, because
     * an unclosed subpath still bounds an area and a renderer fills it the same
     * way.
     */
    public static boolean inside(Shape shape, double x, double y, boolean evenOdd) {
        int winding = 0;
        for (int s = 0; s < shape.closed().length; s++) {
            int from = shape.starts()[s];
            int to = shape.starts()[s + 1];
            if (to - from < 3) {
                continue;
            }
            for (int i = from; i < to; i++) {
                int j = i + 1 < to ? i + 1 : from;
                double ax = shape.xs()[i];
                double ay = shape.ys()[i];
                double bx = shape.xs()[j];
                double by = shape.ys()[j];
                if ((ay > y) == (by > y)) {
                    continue;
                }
                double t = (y - ay) / (by - ay);
                if (x < ax + t * (bx - ax)) {
                    winding += by > ay ? 1 : -1;
                }
            }
        }
        return evenOdd ? (winding & 1) != 0 : winding != 0;
    }

    /**
     * Distance from {@code (x, y)} to the nearest point of the outline, in user
     * units - and, through {@code out}, the arc length along the subpath of the
     * point that won, which is what a dash pattern is measured with.
     *
     * @param out  {@code [distance, arcLength, subpathIndex]}, written in place
     * @param caps how an <b>open</b> subpath's two free ends are finished:
     *             {@code round} is the plain segment distance, {@code butt}
     *             stops square at the endpoint, {@code square} stops square
     *             half a stroke width past it. Interior corners are unaffected
     *             - those are joins, not caps
     * @param half half the stroke width, needed only to know how far a
     *             {@code square} cap projects
     */
    public static void distance(Shape shape, double x, double y, String caps, double half, double[] out) {
        double best = Double.MAX_VALUE;
        double bestArc = 0;
        int bestSub = 0;
        boolean square = "square".equals(caps);
        boolean round = "round".equals(caps);

        for (int s = 0; s < shape.closed().length; s++) {
            // The subpath's own box, grown by the best distance so far: a
            // drawing made of several separate marks spends its time in the one
            // the texel is near, which is what makes a big path affordable.
            double bx0 = shape.subBox()[s * 4];
            double by0 = shape.subBox()[s * 4 + 1];
            double bx1 = shape.subBox()[s * 4 + 2];
            double by1 = shape.subBox()[s * 4 + 3];
            double dx = x < bx0 ? bx0 - x : (x > bx1 ? x - bx1 : 0);
            double dy = y < by0 ? by0 - y : (y > by1 ? y - by1 : 0);
            if (dx * dx + dy * dy > best * best) {
                continue;
            }

            int from = shape.starts()[s];
            int to = shape.starts()[s + 1];
            boolean isClosed = shape.closed()[s];
            int spans = isClosed ? to - from : to - from - 1;
            for (int k = 0; k < spans; k++) {
                int i = from + k;
                int j = i + 1 < to ? i + 1 : from;
                double ax = shape.xs()[i];
                double ay = shape.ys()[i];
                double bx2 = shape.xs()[j];
                double by2 = shape.ys()[j];
                double vx = bx2 - ax;
                double vy = by2 - ay;
                double len2 = vx * vx + vy * vy;
                double raw = len2 <= 1e-18 ? 0 : ((x - ax) * vx + (y - ay) * vy) / len2;
                double t = raw < 0 ? 0 : (raw > 1 ? 1 : raw);
                double px = ax + t * vx;
                double py = ay + t * vy;
                double d = Math.sqrt((x - px) * (x - px) + (y - py) * (y - py));

                // A free end is the start of the first span and the end of the
                // last; every other end is an interior corner, whose shape is
                // the join's business and not the cap's.
                double over = 0;
                if (!isClosed && !round) {
                    double len = Math.sqrt(len2);
                    if (k == 0 && raw < 0) {
                        over = -raw * len;
                    } else if (k == spans - 1 && raw > 1) {
                        over = (raw - 1) * len;
                    }
                    if (square) {
                        over -= half;   // a square cap reaches half a width further first
                    }
                }
                if (over > 0) {
                    // Past a flat cap the stroke is a RECTANGLE, not a capsule,
                    // and the caller only ever thresholds one number at 'half'.
                    // So report a distance that crosses 'half' exactly at the
                    // cap's own plane: sideways it is the perpendicular offset,
                    // lengthways it is half plus the overshoot. That is a soft
                    // square end rather than a round one, which is the whole
                    // visible difference between the two caps.
                    double len = Math.max(1e-9, Math.sqrt(len2));
                    double perp = Math.abs((x - ax) * vy - (y - ay) * vx) / len;
                    d = Math.max(perp, half + over);
                }
                if (d < best) {
                    best = d;
                    bestArc = shape.cum()[i] + t * Math.sqrt(len2);
                    bestSub = s;
                }
            }
        }
        out[0] = best;
        out[1] = bestArc;
        out[2] = bestSub;
    }

    /**
     * How far past the plain round join a {@code miter} reaches at the nearest
     * corner, as an extra piece of covered area - 0 when the join is round or
     * bevelled, or when the miter would exceed {@code miterLimit} and SVG falls
     * back to a bevel.
     *
     * <p>Returned as a <b>distance</b>, in the same units as
     * {@link #distance}, so the caller thresholds one number: it is the
     * distance to the miter wedge, which is {@code MAX_VALUE} everywhere the
     * wedge is not.
     */
    public static double miterDistance(Shape shape, double x, double y, double half, double limit) {
        double best = Double.MAX_VALUE;
        for (int s = 0; s < shape.closed().length; s++) {
            int from = shape.starts()[s];
            int to = shape.starts()[s + 1];
            boolean isClosed = shape.closed()[s];
            int corners = isClosed ? to - from : to - from - 2;
            for (int k = 0; k < corners; k++) {
                int i = isClosed ? from + k : from + k + 1;
                int prev = i - 1 >= from ? i - 1 : to - 1;
                int next = i + 1 < to ? i + 1 : from;
                double ux = shape.xs()[i] - shape.xs()[prev];
                double uy = shape.ys()[i] - shape.ys()[prev];
                double wx = shape.xs()[next] - shape.xs()[i];
                double wy = shape.ys()[next] - shape.ys()[i];
                double ul = Math.sqrt(ux * ux + uy * uy);
                double wl = Math.sqrt(wx * wx + wy * wy);
                if (ul < 1e-12 || wl < 1e-12) {
                    continue;
                }
                ux /= ul;
                uy /= ul;
                wx /= wl;
                wy /= wl;
                // Half the turn angle: the miter reaches half / sin(theta),
                // which is why a near-reversal spikes and why SVG caps it.
                double cos = -(ux * wx + uy * wy);
                double sinHalf = Math.sqrt(Math.max(1e-12, (1 - cos) / 2));
                double ratio = 1 / sinHalf;
                if (ratio > limit) {
                    continue;
                }
                double bx = ux - wx;
                double by = uy - wy;
                double bl = Math.sqrt(bx * bx + by * by);
                if (bl < 1e-12) {
                    continue;
                }
                // The tip sits on the outer bisector, which points away from
                // the turn - the opposite of (u - w) normalised.
                double tipX = shape.xs()[i] - bx / bl * half * ratio;
                double tipY = shape.ys()[i] - by / bl * half * ratio;
                double d = pointInWedge(x, y, shape.xs()[i], shape.ys()[i],
                        shape.xs()[i] - uy * half * sign(ux, uy, wx, wy),
                        shape.ys()[i] + ux * half * sign(ux, uy, wx, wy),
                        tipX, tipY,
                        shape.xs()[i] - wy * half * sign(ux, uy, wx, wy),
                        shape.ys()[i] + wx * half * sign(ux, uy, wx, wy));
                if (d < best) {
                    best = d;
                }
            }
        }
        return best;
    }

    private static double sign(double ux, double uy, double wx, double wy) {
        return ux * wy - uy * wx > 0 ? -1 : 1;
    }

    /**
     * 0 inside the quadrilateral {@code p-a-tip-b}, otherwise the distance to
     * its boundary - the miter wedge, which is the piece a round join covers
     * partly and a bevel does not cover at all.
     */
    private static double pointInWedge(double x, double y, double px, double py,
                                       double ax, double ay, double tx, double ty,
                                       double bx, double by) {
        double[] qx = {px, ax, tx, bx};
        double[] qy = {py, ay, ty, by};
        boolean in = true;
        boolean sign = false;
        boolean first = true;
        double best = Double.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) & 3;
            double cross = (qx[j] - qx[i]) * (y - qy[i]) - (qy[j] - qy[i]) * (x - qx[i]);
            if (first) {
                sign = cross > 0;
                first = false;
            } else if (cross > 0 != sign) {
                in = false;
            }
            double vx = qx[j] - qx[i];
            double vy = qy[j] - qy[i];
            double len2 = vx * vx + vy * vy;
            double t = len2 <= 1e-18 ? 0 : ((x - qx[i]) * vx + (y - qy[i]) * vy) / len2;
            t = t < 0 ? 0 : (t > 1 ? 1 : t);
            double ex = qx[i] + t * vx - x;
            double ey = qy[i] + t * vy - y;
            best = Math.min(best, Math.sqrt(ex * ex + ey * ey));
        }
        return in ? 0 : best;
    }

    // ------------------------------------------------------------------
    // Number reader
    // ------------------------------------------------------------------

    /**
     * The path-data number grammar, which is not Java's. {@code 10-5} is two
     * numbers, {@code 1.5.5} is {@code 1.5} and {@code .5}, and an arc's two
     * flags may be written with no separator at all - all three appear in files
     * a drawing program wrote, so all three are read here rather than rejected.
     */
    private static final class Reader {
        private final String s;
        private int i;

        Reader(String s) {
            this.s = s;
        }

        int at() {
            return i;
        }

        boolean done() {
            return i >= s.length();
        }

        char peek() {
            return s.charAt(i);
        }

        void next() {
            i++;
        }

        void skip() {
            while (i < s.length() && (Character.isWhitespace(s.charAt(i)) || s.charAt(i) == ',')) {
                i++;
            }
        }

        /** An arc's large-arc / sweep flag: exactly one character, {@code 0} or {@code 1}. */
        boolean flag() {
            skip();
            if (done()) {
                throw new IllegalArgumentException("arc command ran out of arguments at index " + i);
            }
            char c = s.charAt(i++);
            if (c != '0' && c != '1') {
                throw new IllegalArgumentException("an arc's large-arc and sweep flags are 0 or 1, got '"
                        + c + "' at index " + (i - 1));
            }
            return c == '1';
        }

        double number() {
            skip();
            int start = i;
            if (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
                i++;
            }
            boolean dot = false;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c >= '0' && c <= '9') {
                    i++;
                } else if (c == '.' && !dot) {
                    dot = true;
                    i++;
                } else {
                    break;
                }
            }
            if (i < s.length() && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
                int save = i;
                i++;
                if (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
                    i++;
                }
                if (i < s.length() && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
                    while (i < s.length() && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
                        i++;
                    }
                } else {
                    i = save;
                }
            }
            if (i == start) {
                throw new IllegalArgumentException("expected a number at index " + i
                        + " of the path data, got "
                        + (i >= s.length() ? "the end of it - a command is missing its arguments"
                                : "'" + s.charAt(i) + "'"));
            }
            return Double.parseDouble(s.substring(start, i));
        }
    }
}
