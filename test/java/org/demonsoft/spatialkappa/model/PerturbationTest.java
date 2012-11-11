package org.demonsoft.spatialkappa.model;


import static org.demonsoft.spatialkappa.model.Utils.getList;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;

import org.easymock.EasyMock;
import org.junit.Test;

public class PerturbationTest {

    private SimulationState state = EasyMock.createMock(SimulationState.class);
    private BooleanExpression conditionTrue = new BooleanExpression(true);
    private PerturbationEffect effect1 = EasyMock.createMock(PerturbationEffect.class);
    private PerturbationEffect effect2 = EasyMock.createMock(PerturbationEffect.class);
    private BooleanExpression conditionFalse = new BooleanExpression(false);

    @SuppressWarnings("unused")
    @Test
    public void testPerturbation() {
        
        try {
            new Perturbation(null, getList(effect1), conditionFalse);
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
        try {
            new Perturbation(conditionTrue, new ArrayList<PerturbationEffect>(), conditionFalse);
            fail("empty effects list should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        Perturbation perturbation = new Perturbation(conditionTrue, 
                getList(PerturbationEffect.SNAPSHOT), conditionFalse);
        assertEquals("[true] do [$SNAPSHOT] until [false]", perturbation.toString());
        
        perturbation = new Perturbation(conditionTrue, getList(PerturbationEffect.SNAPSHOT), null);
        assertEquals("[true] do [$SNAPSHOT]", perturbation.toString());
        
        perturbation = new Perturbation(conditionTrue, 
                getList(PerturbationEffect.SNAPSHOT, PerturbationEffect.STOP), conditionFalse);
        assertEquals("[true] do [$SNAPSHOT, $STOP] until [false]", perturbation.toString());
        
        perturbation = new Perturbation(conditionTrue, 
                getList(PerturbationEffect.SNAPSHOT, PerturbationEffect.STOP), null);
        assertEquals("[true] do [$SNAPSHOT, $STOP]", perturbation.toString());
    }

    @Test
    public void testIsConditionMet() {
        try {
            new Perturbation(conditionTrue, getList(effect1), conditionFalse).isConditionMet(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }

        assertTrue(new Perturbation(conditionTrue, getList(effect1), conditionFalse).isConditionMet(state));
        assertTrue(new Perturbation(conditionTrue, getList(effect1), conditionTrue).isConditionMet(state));
        assertFalse(new Perturbation(conditionFalse, getList(effect1), conditionTrue).isConditionMet(state));
        assertFalse(new Perturbation(conditionFalse, getList(effect1), conditionFalse).isConditionMet(state));

        assertTrue(new Perturbation(conditionTrue, getList(effect1), null).isConditionMet(state));
        assertFalse(new Perturbation(conditionFalse, getList(effect1), null).isConditionMet(state));
    }

    @Test
    public void testIsUntilConditionMet() {
        try {
            new Perturbation(conditionTrue, getList(effect1), conditionFalse).isUntilConditionMet(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }

        assertFalse(new Perturbation(conditionTrue, getList(effect1), conditionFalse).isUntilConditionMet(state));
        assertTrue(new Perturbation(conditionTrue, getList(effect1), conditionTrue).isUntilConditionMet(state));
        assertTrue(new Perturbation(conditionFalse, getList(effect1), conditionTrue).isUntilConditionMet(state));
        assertFalse(new Perturbation(conditionFalse, getList(effect1), conditionFalse).isUntilConditionMet(state));

        assertTrue(new Perturbation(conditionTrue, getList(effect1), null).isUntilConditionMet(state));
        assertTrue(new Perturbation(conditionFalse, getList(effect1), null).isUntilConditionMet(state));
    }

    @Test
    public void testApply() {
        try {
            new Perturbation(conditionTrue, getList(effect1), conditionFalse).apply(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }

        effect1.apply(state);
        
        replay(effect1);
        new Perturbation(conditionTrue, getList(effect1), conditionFalse).apply(state);
        verify(effect1);
        
        reset(effect1);

        effect1.apply(state);
        effect2.apply(state);
        
        replay(effect1, effect2);
        new Perturbation(conditionTrue, getList(effect1, effect2), conditionFalse).apply(state);
        verify(effect1, effect2);
    }

}
