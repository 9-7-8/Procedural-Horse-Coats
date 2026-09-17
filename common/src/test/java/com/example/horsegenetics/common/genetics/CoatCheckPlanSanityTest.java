package com.example.horsegenetics.common.genetics;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>Every pen the horse dimension's right-hand column will build, walked once.</b>
 *
 * <p>The corridor asks each {@link CoatCheckPlan.Pen} for four genotype codes, an
 * expression name and an expression description, and it does that for the first
 * thirty segments <i>while the player is being teleported in</i>. So anything in
 * here that throws or hands back a null is not a bad pen - it is a crash on
 * entering the dimension, before there is anything to look at.
 *
 * <p>Written after testers reported exactly that against 0.5.004. The plan is
 * built from a baked resource and the registry, so it changes without this file
 * changing, which is the reason it is a walk rather than a list.
 */
final class CoatCheckPlanSanityTest {

    @Test
    void everyPenCanBeBuilt() {
        List<CoatCheckPlan.Pen> pens = CoatCheckPlan.pens();
        assertFalse(pens.isEmpty(), "the coat-check column is empty - is unverified-genes.txt baked?");

        List<String> problems = new ArrayList<>();
        for (int i = 0; i < pens.size(); i++) {
            CoatCheckPlan.Pen pen = pens.get(i);
            String where = "pen #" + (i + 1) + " " + pen.gene().key() + " " + pen.pair().toTokens();
            try {
                assertNotNull(pen.sex(), where + ": null sex");
                Expression expression = pen.expression();
                assertNotNull(expression, where + ": null expression");
                assertNotNull(expression.name(), where + ": null expression name");
                // The sign wraps this one; wrapText(null) is a crash, not a blank board.
                assertNotNull(expression.description(), where + ": null expression description");
                for (CoatCheckPlan.Base base : CoatCheckPlan.Base.values()) {
                    String code = pen.genotype(base).toCode();
                    assertNotNull(code, where + " on " + base.label() + ": null genotype code");
                    assertFalse(code.isEmpty(), where + " on " + base.label() + ": empty genotype code");
                }
            } catch (RuntimeException | AssertionError e) {
                problems.add(where + " -> " + e);
            }
        }
        assertTrue(problems.isEmpty(),
                problems.size() + " of " + pens.size() + " pens would crash the corridor:\n"
                        + String.join("\n", problems));
    }
}
