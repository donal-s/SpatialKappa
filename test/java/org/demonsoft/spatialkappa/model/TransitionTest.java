package org.demonsoft.spatialkappa.model;
import static junit.framework.Assert.assertEquals;

import org.demonsoft.spatialkappa.model.Transition;
import org.junit.Test;

public abstract class TransitionTest {

    @Test
    public void testGetRateText() {
        Transition transition = new TestTransition("label", "0.01");
        assertEquals(0.01f, transition.getRate(), 0.0001);
        assertEquals("0.01", transition.getRateText());

        transition = new TestTransition("label", "$INF");
        assertEquals(0.01f, transition.getRate(), Transition.INFINITE_RATE);
        assertEquals("$INF", transition.getRateText());
    }
    
    
    class TestTransition extends Transition {

        public TestTransition(String label, String rate) {
            super(label, rate);
        }
    }
    
}
