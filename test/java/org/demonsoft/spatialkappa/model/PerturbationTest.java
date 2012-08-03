package org.demonsoft.spatialkappa.model;

import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

import org.easymock.EasyMock;
import org.junit.Test;

public class PerturbationTest {

    private SimulationState state = EasyMock.createMock(SimulationState.class);
    private BooleanExpression conditionTrue = new BooleanExpression(true);
    private PerturbationEffect effect = EasyMock.createMock(PerturbationEffect.class);
    private BooleanExpression conditionFalse = new BooleanExpression(false);

    @SuppressWarnings("unused")
    @Test
    public void testPerturbation() {
        
        try {
            new Perturbation(null, effect, conditionFalse);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new Perturbation(conditionTrue, null, conditionFalse);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        Perturbation perturbation = new Perturbation(conditionTrue, PerturbationEffect.SNAPSHOT, conditionFalse);
        assertEquals("[true] do $SNAPSHOT until [false]", perturbation.toString());
        
        perturbation = new Perturbation(conditionTrue, PerturbationEffect.SNAPSHOT, null);
        assertEquals("[true] do $SNAPSHOT", perturbation.toString());
    }

    @Test
    public void testIsConditionMet() {
        try {
            new Perturbation(conditionTrue, effect, conditionFalse).isConditionMet(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }

        assertTrue(new Perturbation(conditionTrue, effect, conditionFalse).isConditionMet(state));
        assertTrue(new Perturbation(conditionTrue, effect, conditionTrue).isConditionMet(state));
        assertFalse(new Perturbation(conditionFalse, effect, conditionTrue).isConditionMet(state));
        assertFalse(new Perturbation(conditionFalse, effect, conditionFalse).isConditionMet(state));

        assertTrue(new Perturbation(conditionTrue, effect, null).isConditionMet(state));
        assertFalse(new Perturbation(conditionFalse, effect, null).isConditionMet(state));
    }

    @Test
    public void testIsUntilConditionMet() {
        try {
            new Perturbation(conditionTrue, effect, conditionFalse).isUntilConditionMet(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }

        assertFalse(new Perturbation(conditionTrue, effect, conditionFalse).isUntilConditionMet(state));
        assertTrue(new Perturbation(conditionTrue, effect, conditionTrue).isUntilConditionMet(state));
        assertTrue(new Perturbation(conditionFalse, effect, conditionTrue).isUntilConditionMet(state));
        assertFalse(new Perturbation(conditionFalse, effect, conditionFalse).isUntilConditionMet(state));

        assertTrue(new Perturbation(conditionTrue, effect, null).isUntilConditionMet(state));
        assertTrue(new Perturbation(conditionFalse, effect, null).isUntilConditionMet(state));
    }

    @Test
    public void testApply() {
        try {
            new Perturbation(conditionTrue, effect, conditionFalse).apply(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }

        effect.apply(state);
        
        replay(effect);
        new Perturbation(conditionTrue, effect, conditionFalse).apply(state);
        verify(effect);
    }

}
