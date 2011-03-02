package org.demonsoft.spatialkappa.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.demonsoft.spatialkappa.model.Agent;
import org.demonsoft.spatialkappa.model.Complex;
import org.demonsoft.spatialkappa.model.Observable;
import org.junit.Test;

public class ObservableTest {

    @Test
    public void testObservable() {
        Complex complex = new Complex(new Agent("agent1"));

        try {
            new Observable(null, "label", true);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        Observable observable = new Observable(complex, "label", true);
        assertEquals("label", observable.label);
        assertSame(complex, observable.complex);
        assertFalse(observable.isGeneratedLabel());

        observable = new Observable(complex, null, true);
        assertEquals(complex.toString(), observable.label);
        assertSame(complex, observable.complex);
        assertTrue(observable.isInObservations());
        assertTrue(observable.isGeneratedLabel());

        complex = new Complex(new Agent("agent1"), new Agent("agent2"));

        observable = new Observable(complex, "label", false);
        assertEquals("label", observable.label);
        assertSame(complex, observable.complex);
        assertFalse(observable.isInObservations());
        assertEquals("'label' ([agent1(), agent2()])", observable.toString());
        assertFalse(observable.isGeneratedLabel());

        observable = new Observable(complex, null, true);
        assertEquals("[agent1(), agent2()]", observable.label);
        assertSame(complex, observable.complex);
        assertEquals("'[agent1(), agent2()]' ([agent1(), agent2()])", observable.toString());
        assertTrue(observable.isGeneratedLabel());
    }

    private void checkObservable(Observable observable, String expectedLabel, Complex complex, boolean inObservations, String string) {
        
        assertEquals(expectedLabel, observable.label);
        assertSame(complex, observable.complex);
        assertEquals(inObservations, observable.isInObservations());
        assertEquals(string, observable.toString());
    }

    @Test
    public void testObservable_labelOnly() {
        try {
            new Observable(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        Observable observable = new Observable("label");
        checkObservable(observable, "label", null, true, "'label'");
    }

}
