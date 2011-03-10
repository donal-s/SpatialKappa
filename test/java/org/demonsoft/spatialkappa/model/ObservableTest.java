package org.demonsoft.spatialkappa.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.junit.Test;

public class ObservableTest {

    @Test
    public void testObservable() {
        Complex complex = new Complex(new Agent("agent1"), new Agent("agent2"));

        try {
            new Observable(null, "label");
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new Observable(complex, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        Observable observable = new Observable(complex, "label");
        assertEquals("label", observable.label);
        assertSame(complex, observable.complex);
        assertEquals("'label' ([agent1(), agent2()])", observable.toString());
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
        assertEquals("label", observable.label);
        assertNull(observable.complex);
        assertEquals("'label'", observable.toString());
    }

}
