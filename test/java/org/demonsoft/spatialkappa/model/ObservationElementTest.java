package org.demonsoft.spatialkappa.model;

import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.Arrays;

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

    @SuppressWarnings("unused")
    @Test
    public void testObservationElement_compartment() {
        int[] dimensions = new int[] {3};
        Serializable[] cellValues = new Serializable[] {5, 6, 7};
        
        try {
            new ObservationElement(4, null, "cytosol", cellValues);
            fail("Null should have failed");
        }
        catch (NullPointerException e) {
            // expected exception
        }
        
        try {
            new ObservationElement(4, dimensions, null, cellValues);
            fail("Null should have failed");
        }
        catch (NullPointerException e) {
            // expected exception
        }
        
        try {
            new ObservationElement(4, dimensions, "cytosol", null);
            fail("Null should have failed");
        }
        catch (NullPointerException e) {
            // expected exception
        }
        
        try {
            new ObservationElement(4, new int[] {3, 2}, "cytosol", cellValues);
            fail("Dimension mismatch should have failed");
        }
        catch (Exception e) {
            // expected exception
        }
        
        try {
            new ObservationElement(4, dimensions, "cytosol", new Serializable[][] {{5,5}, {6,6}, {7,7}});
            fail("Dimension mismatch should have failed");
        }
        catch (Exception e) {
            // expected exception
        }
        
        try {
            new ObservationElement(4, dimensions, "cytosol", new Serializable[] {5, 6});
            fail("Length should have failed");
        }
        catch (Exception e) {
            // expected exception
        }
        
        ObservationElement element = new ObservationElement(5, dimensions, "cytosol", cellValues);
        assertEquals(5f, element.value, 0.01f);
        assertTrue(element.isCompartment);
        assertEquals(dimensions, element.dimensions);
        assertTrue(Arrays.deepEquals(cellValues, element.cellValues));
        assertEquals("cytosol", element.compartmentName);
    }

}
