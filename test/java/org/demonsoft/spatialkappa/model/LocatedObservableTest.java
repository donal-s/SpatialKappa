package org.demonsoft.spatialkappa.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.demonsoft.spatialkappa.model.Agent;
import org.demonsoft.spatialkappa.model.Complex;
import org.demonsoft.spatialkappa.model.LocatedObservable;
import org.demonsoft.spatialkappa.model.Location;
import org.demonsoft.spatialkappa.model.MathExpression;
import org.demonsoft.spatialkappa.model.Observable;
import org.junit.Test;

public class LocatedObservableTest {

    @Test
    public void testLocatedObservable() {
        Complex complex = new Complex(new Agent("agent1"));
        Location location = new Location("cytosol", new MathExpression("2"));
        Observable observable = new Observable(complex, "label",  true);

        try {
            new LocatedObservable(null, location);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        LocatedObservable locatedObservable = new LocatedObservable(observable, null);
        assertEquals(observable, locatedObservable.observable);
        assertEquals(null, locatedObservable.location);
        assertEquals("label", locatedObservable.label);
        assertEquals("'label' ([agent1()])", locatedObservable.toString());

        locatedObservable = new LocatedObservable(observable, location);
        assertEquals(observable, locatedObservable.observable);
        assertEquals(location, locatedObservable.location);
        assertEquals("label cytosol[2]", locatedObservable.label);
        assertEquals("'label' cytosol[2] ([agent1()])", locatedObservable.toString());
    }


}
