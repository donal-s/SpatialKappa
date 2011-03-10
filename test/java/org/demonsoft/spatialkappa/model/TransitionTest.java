package org.demonsoft.spatialkappa.model;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;

import java.util.HashMap;

import org.demonsoft.spatialkappa.model.VariableExpression.Constant;
import org.junit.Test;

public abstract class TransitionTest {

    @Test
    public void testTransition() {
        VariableExpression rate = new VariableExpression("0.01");
        Transition transition = new TestTransition("label", rate);
        assertEquals("label", transition.label);
        assertSame(rate, transition.getRate());
        assertFalse(transition.isInfiniteRate(new HashMap<String, Variable>()));

        rate = new VariableExpression(Constant.INFINITY);
        transition = new TestTransition(null, rate);
        assertEquals(null, transition.label);
        assertSame(rate, transition.getRate());
        assertTrue(transition.isInfiniteRate(new HashMap<String, Variable>()));
    }
    
    
    class TestTransition extends Transition {

        public TestTransition(String label, VariableExpression rate) {
            super(label, rate);
        }
    }
    
}
