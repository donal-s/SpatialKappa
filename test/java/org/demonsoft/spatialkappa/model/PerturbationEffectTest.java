package org.demonsoft.spatialkappa.model;

import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.fail;

import java.util.List;

import org.demonsoft.spatialkappa.model.PerturbationEffect.Type;
import org.easymock.EasyMock;
import org.junit.Test;

public class PerturbationEffectTest {

    @SuppressWarnings("unused")
    @Test
    public void testPerturbationEffect() {
        VariableExpression expression = new VariableExpression("2");
        List<Agent> agents = Utils.getList(new Agent("A"));
        
        try {
            new PerturbationEffect(null, expression, agents);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        
        try {
            new PerturbationEffect(Type.ADD, null, agents);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        
        try {
            new PerturbationEffect(Type.ADD, expression, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        
        try {
            new PerturbationEffect(null, expression);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }

        try {
            new PerturbationEffect("label", null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }

        try {
            new PerturbationEffect(Type.FIXED, expression, agents);
            fail("invalid type should have failed");
        }
        catch (IllegalArgumentException ex) {
            // expected exception
        }
        
        try {
            new PerturbationEffect(Type.SET, expression, agents);
            fail("invalid type should have failed");
        }
        catch (IllegalArgumentException ex) {
            // expected exception
        }
        
        PerturbationEffect effect = new PerturbationEffect(Type.ADD, expression, agents);
        assertEquals(Type.ADD, effect.type);
        assertEquals("$ADD 2.0 [A()]", effect.toString());
        
        effect = new PerturbationEffect(Type.REMOVE, expression, agents);
        assertEquals(Type.REMOVE, effect.type);
        assertEquals("$DEL 2.0 [A()]", effect.toString());
        
        effect = new PerturbationEffect("label", expression);
        assertEquals(Type.SET, effect.type);
        assertEquals("'label' := 2.0", effect.toString());
        
        assertEquals(Type.FIXED, PerturbationEffect.SNAPSHOT.type);
        assertEquals("$SNAPSHOT", PerturbationEffect.SNAPSHOT.toString());
        
        assertEquals(Type.FIXED, PerturbationEffect.STOP.type);
        assertEquals("$STOP", PerturbationEffect.STOP.toString());
    }
    

    @Test
    public void testApply() {
        SimulationState state = EasyMock.createMock(SimulationState.class);
        VariableExpression expression = new VariableExpression("2");
        List<Agent> agents = Utils.getList(new Agent("A"));
        try {
            new PerturbationEffect(Type.ADD, expression, agents).apply(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }

        state.addComplexInstances(agents, 2);
        
        replay(state);
        new PerturbationEffect(Type.ADD, expression, agents).apply(state);
        verify(state);

        reset(state);
        state.addComplexInstances(agents, -2);
        
        replay(state);
        new PerturbationEffect(Type.REMOVE, expression, agents).apply(state);
        verify(state);

        reset(state);
        state.setTransitionRate("label", expression);
        
        replay(state);
        new PerturbationEffect("label", expression).apply(state);
        verify(state);

        reset(state);
        state.stop();
        
        replay(state);
        PerturbationEffect.STOP.apply(state);
        verify(state);

        reset(state);
        state.snapshot();
        
        replay(state);
        PerturbationEffect.SNAPSHOT.apply(state);
        verify(state);
    }

}
