package org.demonsoft.spatialkappa.model;

import static org.junit.Assert.*;

import org.demonsoft.spatialkappa.model.ObservationElement;
import org.junit.Test;

public class ObservationElementTest {

    @Test
    public void testObservationElement_nonCompartment() {
        ObservationElement element = new ObservationElement(5);
        assertEquals(5f, element.value, 0.01f);
        assertFalse(element.isCompartment);
        assertNull(element.dimensions);
        assertNull(element.cellValues);
    }

    @Test
    public void testObservationElement_compartment() {
        int[] dimensions = new int[] {3};
        int[] cellValues = new int[] {5, 6, 7};
        
        try {
            new ObservationElement(4, null, cellValues);
            fail("Null should have failed");
        }
        catch (NullPointerException e) {
            // expected exception
        }
        
        try {
            new ObservationElement(4, dimensions, null);
            fail("Null should have failed");
        }
        catch (NullPointerException e) {
            // expected exception
        }
        
        try {
            new ObservationElement(4, new int[] {3, 2}, cellValues);
            fail("Dimension mismatch should have failed");
        }
        catch (Exception e) {
            // expected exception
        }
        
        try {
            new ObservationElement(4, dimensions, new int[][] {{5,5}, {6,6}, {7,7}});
            fail("Dimension mismatch should have failed");
        }
        catch (Exception e) {
            // expected exception
        }
        
        try {
            new ObservationElement(4, dimensions, new int[] {5, 6});
            fail("Length should have failed");
        }
        catch (Exception e) {
            // expected exception
        }
        
        ObservationElement element = new ObservationElement(5, dimensions, cellValues);
        assertEquals(5f, element.value, 0.01f);
        assertTrue(element.isCompartment);
        assertEquals(dimensions, element.dimensions);
        assertEquals(cellValues, element.cellValues);
    }

}
