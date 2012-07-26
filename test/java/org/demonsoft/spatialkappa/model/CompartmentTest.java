package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_0;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_2;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.demonsoft.spatialkappa.model.Compartment.OpenCircle;
import org.demonsoft.spatialkappa.model.Compartment.OpenCuboid;
import org.demonsoft.spatialkappa.model.Compartment.OpenCylinder;
import org.demonsoft.spatialkappa.model.Compartment.OpenRectangle;
import org.demonsoft.spatialkappa.model.Compartment.OpenSphere;
import org.demonsoft.spatialkappa.model.Compartment.SolidCircle;
import org.demonsoft.spatialkappa.model.Compartment.SolidCylinder;
import org.demonsoft.spatialkappa.model.Compartment.SolidSphere;
import org.junit.Test;

public class CompartmentTest {

    private static final Map<String, Integer> NO_VARIABLES = new HashMap<String, Integer>();

    @SuppressWarnings("unused")
    @Test
    public void testCompartment() {
        try {
            new Compartment(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new Compartment(null, 2, 3);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            new Compartment("label", 0);
            fail("invalid should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        try {
            new Compartment("label", -1);
            fail("invalid should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        Compartment compartment = new Compartment("label");
        assertEquals("label", compartment.getName());
        assertArrayEquals(new int[0], compartment.getDimensions());
        assertEquals("label", compartment.toString());

        compartment = new Compartment("label", 2, 3, 5);
        assertEquals("label", compartment.getName());
        assertArrayEquals(new int[] {2, 3, 5}, compartment.getDimensions());
        assertEquals("label[2][3][5]", compartment.toString());
    }
    
    @Test
    public void testGetDistributedCellReferences() {
        Compartment compartment = new Compartment("label");
        checkCompartmentReferences(compartment.getDistributedCellReferences(), new Location("label"));
        
        compartment = new Compartment("label", 3);
        checkCompartmentReferences(compartment.getDistributedCellReferences(), 
                new Location("label", INDEX_0),
                new Location("label", INDEX_1),
                new Location("label", INDEX_2)
        );
        
        compartment = new Compartment("label", 3, 2);
        checkCompartmentReferences2D(compartment.getDistributedCellReferences(), compartment,
                new String[] {"xx", "xx", "xx"}
        );
        
        compartment = new Compartment("label", 3, 2, 4);
        checkCompartmentReferences3D(compartment.getDistributedCellReferences(), compartment,
                new String[][] {{"xxxx", "xxxx"}, {"xxxx", "xxxx"}, {"xxxx", "xxxx"}}
        );
    }

    private void checkCompartmentReferences2D(Location[] distributedCellReferences, Compartment compartment, String[] expected) {
        assertEquals(2, compartment.dimensions.length);
        
        boolean[][] actual = new boolean[compartment.dimensions[0]][compartment.dimensions[1]];
        for (Location location : distributedCellReferences) {
            assertEquals(compartment.name, location.getName());
            assertEquals(2, location.getIndices().length);
            actual[location.getIndices()[0].evaluateIndex(NO_VARIABLES)]
                    [location.getIndices()[1].evaluateIndex(NO_VARIABLES)] = true;
        }
        String[] actualStrings = new String[actual.length];
        for (int rowIndex=0; rowIndex<actual.length; rowIndex++) {
            StringBuilder builder = new StringBuilder();
            boolean[] row = actual[rowIndex];
            for (int index=0; index<row.length; index++) {
                builder.append(row[index] ? 'x' : ' ');
            }
            actualStrings[rowIndex] = builder.toString();
        }
        
        assertArrayEquals(expected, actualStrings);
    }

    private void checkCompartmentReferences3D(Location[] distributedCellReferences, Compartment compartment, String[][] expected) {
        assertEquals(3, compartment.dimensions.length);
        
        boolean[][][] actual = new boolean[compartment.dimensions[0]][compartment.dimensions[1]][compartment.dimensions[2]];
        for (Location location : distributedCellReferences) {
            assertEquals(compartment.name, location.getName());
            assertEquals(3, location.getIndices().length);
            actual[location.getIndices()[0].evaluateIndex(NO_VARIABLES)]
                    [location.getIndices()[1].evaluateIndex(NO_VARIABLES)]
                    [location.getIndices()[2].evaluateIndex(NO_VARIABLES)] = true;
        }
        String[][] actualStrings = new String[actual.length][];
        for (int sliceIndex=0; sliceIndex<actual.length; sliceIndex++) {
            boolean[][] slice = actual[sliceIndex];
            actualStrings[sliceIndex] = new String[slice.length];
            for (int rowIndex=0; rowIndex<slice.length; rowIndex++) {
                StringBuilder builder = new StringBuilder();
                boolean[] row = slice[rowIndex];
                for (int index=0; index<row.length; index++) {
                    builder.append(row[index] ? 'x' : ' ');
                }
                actualStrings[sliceIndex][rowIndex] = builder.toString();
            }
        }
        
        assertArrayEquals(expected, actualStrings);
    }

    private void checkCompartmentReferences(Location[] actual, Location... expected) {
        assertEquals(expected.length, actual.length);
        
        for (int index = 0 ; index < expected.length; index++) {
            assertEquals(expected[index], actual[index]);
        }
    }
    
    @Test
    public void testSolidCircle() {
        SolidCircle compartment = new SolidCircle("circle", new int[] {1});
        checkCompartmentReferences2D(compartment.getDistributedCellReferences(), compartment,
                new String[] {"x"}
        );
        
        compartment = new SolidCircle("circle", new int[] {7});
        checkCompartmentReferences2D(compartment.getDistributedCellReferences(), compartment,
                new String[] {
                    "  xxx  ", 
                    " xxxxx ", 
                    "xxxxxxx", 
                    "xxxxxxx", 
                    "xxxxxxx", 
                    " xxxxx ", 
                    "  xxx  "
                }
        );
        
    }
    
    @Test
    public void testOpenCircle() {
        OpenCircle compartment = new OpenCircle("circle", new int[] {3, 1});
        checkCompartmentReferences2D(compartment.getDistributedCellReferences(), compartment,
                new String[] {"xxx", "x x", "xxx"}
        );
        
        compartment = new OpenCircle("circle", new int[] {7, 3});
        checkCompartmentReferences2D(compartment.getDistributedCellReferences(), compartment,
                new String[] {
                    "  xxx  ", 
                    " xxxxx ", 
                    "xxxxxxx", 
                    "xxx xxx", 
                    "xxxxxxx", 
                    " xxxxx ", 
                    "  xxx  "
                }
        );
        
        compartment = new OpenCircle("circle", new int[] {7, 1});
        checkCompartmentReferences2D(compartment.getDistributedCellReferences(), compartment,
                new String[] {
                    "  xxx  ", 
                    " x   x ", 
                    "x     x", 
                    "x     x", 
                    "x     x", 
                    " x   x ", 
                    "  xxx  "
                }
        );
    }
    
    @Test
    public void testOpenRectangle() {
        OpenRectangle compartment = new OpenRectangle("name", new int[] {3, 5, 1});
        checkCompartmentReferences2D(compartment.getDistributedCellReferences(), compartment,
                new String[] {"xxxxx", "x   x", "xxxxx"}
        );
        
        compartment = new OpenRectangle("name", new int[] {3, 4, 1});
        checkCompartmentReferences2D(compartment.getDistributedCellReferences(), compartment,
                new String[] {"xxxx", "x  x", "xxxx"}
        );
        
        compartment = new OpenRectangle("name", new int[] {7, 5, 2});
        checkCompartmentReferences2D(compartment.getDistributedCellReferences(), compartment,
                new String[] {
                    "xxxxx", 
                    "xxxxx", 
                    "xx xx", 
                    "xx xx", 
                    "xx xx", 
                    "xxxxx", 
                    "xxxxx", 
                }
        );
    }
    
    @Test
    public void testOpenCuboid() {
        OpenCuboid compartment = new OpenCuboid("name", new int[] {5, 3, 4, 1});
        checkCompartmentReferences3D(compartment.getDistributedCellReferences(), compartment,
                new String[][] {
                    {"xxxx", "xxxx", "xxxx"}, 
                    {"xxxx", "x  x", "xxxx"}, 
                    {"xxxx", "x  x", "xxxx"}, 
                    {"xxxx", "x  x", "xxxx"},
                    {"xxxx", "xxxx", "xxxx"}, 
                }
        );
        
        compartment = new OpenCuboid("name", new int[] {7, 6, 5, 2});
        checkCompartmentReferences3D(compartment.getDistributedCellReferences(), compartment,
                new String[][] {
                    {"xxxxx", "xxxxx", "xxxxx", "xxxxx", "xxxxx", "xxxxx"}, 
                    {"xxxxx", "xxxxx", "xxxxx", "xxxxx", "xxxxx", "xxxxx"}, 
                    {"xxxxx", "xxxxx", "xx xx", "xx xx", "xxxxx", "xxxxx"}, 
                    {"xxxxx", "xxxxx", "xx xx", "xx xx", "xxxxx", "xxxxx"}, 
                    {"xxxxx", "xxxxx", "xx xx", "xx xx", "xxxxx", "xxxxx"}, 
                    {"xxxxx", "xxxxx", "xxxxx", "xxxxx", "xxxxx", "xxxxx"}, 
                    {"xxxxx", "xxxxx", "xxxxx", "xxxxx", "xxxxx", "xxxxx"}, 
                }
        );
    }
    
    @Test
    public void testSolidCylinder() {
        SolidCylinder compartment = new SolidCylinder("name", new int[] {4, 4});
        checkCompartmentReferences3D(compartment.getDistributedCellReferences(), compartment,
                new String[][] {
                    {"    ", "xxxx", "xxxx", "    "}, 
                    {"xxxx", "xxxx", "xxxx", "xxxx"}, 
                    {"xxxx", "xxxx", "xxxx", "xxxx"},
                    {"    ", "xxxx", "xxxx", "    "}, 
                }
        );
        
        compartment = new SolidCylinder("name", new int[] {7, 5});
        checkCompartmentReferences3D(compartment.getDistributedCellReferences(), compartment,
                new String[][] {
                    {"     ", "     ", "xxxxx", "xxxxx", "xxxxx", "     ", "     "}, 
                    {"     ", "xxxxx", "xxxxx", "xxxxx", "xxxxx", "xxxxx", "     "}, 
                    {"xxxxx", "xxxxx", "xxxxx", "xxxxx", "xxxxx", "xxxxx", "xxxxx"}, 
                    {"xxxxx", "xxxxx", "xxxxx", "xxxxx", "xxxxx", "xxxxx", "xxxxx"}, 
                    {"xxxxx", "xxxxx", "xxxxx", "xxxxx", "xxxxx", "xxxxx", "xxxxx"}, 
                    {"     ", "xxxxx", "xxxxx", "xxxxx", "xxxxx", "xxxxx", "     "}, 
                    {"     ", "     ", "xxxxx", "xxxxx", "xxxxx", "     ", "     "}, 
                }
        );
    }
    
    @Test
    public void testOpenCylinder() {
        OpenCylinder compartment = new OpenCylinder("name", new int[] {5, 4, 1});
        checkCompartmentReferences3D(compartment.getDistributedCellReferences(), compartment,
                new String[][] {
                    {"    ", "xxxx", "xxxx", "xxxx", "    "}, 
                    {"xxxx", "x  x", "x  x", "x  x", "xxxx"}, 
                    {"xxxx", "x  x", "x  x", "x  x", "xxxx"}, 
                    {"xxxx", "x  x", "x  x", "x  x", "xxxx"},
                    {"    ", "xxxx", "xxxx", "xxxx", "    "}, 
                }
        );
        
        compartment = new OpenCylinder("name", new int[] {7, 5, 2});
        checkCompartmentReferences3D(compartment.getDistributedCellReferences(), compartment,
                new String[][] {
                    {"     ", "     ", "xxxxx", "xxxxx", "xxxxx", "     ", "     "}, 
                    {"     ", "xxxxx", "xxxxx", "xxxxx", "xxxxx", "xxxxx", "     "}, 
                    {"xxxxx", "xxxxx", "xx xx", "xx xx", "xx xx", "xxxxx", "xxxxx"}, 
                    {"xxxxx", "xxxxx", "xx xx", "xx xx", "xx xx", "xxxxx", "xxxxx"}, 
                    {"xxxxx", "xxxxx", "xx xx", "xx xx", "xx xx", "xxxxx", "xxxxx"}, 
                    {"     ", "xxxxx", "xxxxx", "xxxxx", "xxxxx", "xxxxx", "     "}, 
                    {"     ", "     ", "xxxxx", "xxxxx", "xxxxx", "     ", "     "}, 
                }
        );
    }
    
    @Test
    public void testSolidSphere() {
        SolidSphere compartment = new SolidSphere("name", new int[] {4});
        checkCompartmentReferences3D(compartment.getDistributedCellReferences(), compartment,
                new String[][] {
                    {"    ", " xx ", " xx ", "    "}, 
                    {" xx ", "xxxx", "xxxx", " xx "}, 
                    {" xx ", "xxxx", "xxxx", " xx "},
                    {"    ", " xx ", " xx ", "    "}, 
                }
        );
        
        compartment = new SolidSphere("name", new int[] {7});
        checkCompartmentReferences3D(compartment.getDistributedCellReferences(), compartment,
                new String[][] {
                    {"       ", "       ", "  xxx  ", "  xxx  ", "  xxx  ", "       ", "       "}, 
                    {"       ", " xxxxx ", " xxxxx ", " xxxxx ", " xxxxx ", " xxxxx ", "       "}, 
                    {"  xxx  ", " xxxxx ", "xxxxxxx", "xxxxxxx", "xxxxxxx", " xxxxx ", "  xxx  "}, 
                    {"  xxx  ", " xxxxx ", "xxxxxxx", "xxxxxxx", "xxxxxxx", " xxxxx ", "  xxx  "}, 
                    {"  xxx  ", " xxxxx ", "xxxxxxx", "xxxxxxx", "xxxxxxx", " xxxxx ", "  xxx  "}, 
                    {"       ", " xxxxx ", " xxxxx ", " xxxxx ", " xxxxx ", " xxxxx ", "       "}, 
                    {"       ", "       ", "  xxx  ", "  xxx  ", "  xxx  ", "       ", "       "}, 
                }
        );
    }
    
    @Test
    public void testOpenSphere() {
        OpenSphere compartment = new OpenSphere("name", new int[] {4, 1});
        checkCompartmentReferences3D(compartment.getDistributedCellReferences(), compartment,
                new String[][] {
                    {"    ", " xx ", " xx ", "    "}, 
                    {" xx ", "x  x", "x  x", " xx "}, 
                    {" xx ", "x  x", "x  x", " xx "},
                    {"    ", " xx ", " xx ", "    "}, 
                }
        );
        
        compartment = new OpenSphere("name", new int[] {7, 2});
        checkCompartmentReferences3D(compartment.getDistributedCellReferences(), compartment,
                new String[][] {
                    {"       ", "       ", "  xxx  ", "  xxx  ", "  xxx  ", "       ", "       "}, 
                    {"       ", " xxxxx ", " xxxxx ", " xxxxx ", " xxxxx ", " xxxxx ", "       "}, 
                    {"  xxx  ", " xxxxx ", "xxx xxx", "xx   xx", "xxx xxx", " xxxxx ", "  xxx  "}, 
                    {"  xxx  ", " xxxxx ", "xx   xx", "xx   xx", "xx   xx", " xxxxx ", "  xxx  "}, 
                    {"  xxx  ", " xxxxx ", "xxx xxx", "xx   xx", "xxx xxx", " xxxxx ", "  xxx  "}, 
                    {"       ", " xxxxx ", " xxxxx ", " xxxxx ", " xxxxx ", " xxxxx ", "       "}, 
                    {"       ", "       ", "  xxx  ", "  xxx  ", "  xxx  ", "       ", "       "}, 
                }
        );
    }
    


}
