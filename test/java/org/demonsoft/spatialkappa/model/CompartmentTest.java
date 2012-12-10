package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_0;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_2;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_X;
import static org.demonsoft.spatialkappa.model.Utils.getList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;

import org.demonsoft.spatialkappa.model.Compartment.OpenCircle;
import org.demonsoft.spatialkappa.model.Compartment.OpenCuboid;
import org.demonsoft.spatialkappa.model.Compartment.OpenCylinder;
import org.demonsoft.spatialkappa.model.Compartment.OpenRectangle;
import org.demonsoft.spatialkappa.model.Compartment.OpenSphere;
import org.demonsoft.spatialkappa.model.Compartment.OpenSpine;
import org.demonsoft.spatialkappa.model.Compartment.SolidCircle;
import org.demonsoft.spatialkappa.model.Compartment.SolidCylinder;
import org.demonsoft.spatialkappa.model.Compartment.SolidSphere;
import org.demonsoft.spatialkappa.model.Compartment.SolidSpine;
import org.junit.Test;

public class CompartmentTest {

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
        assertEquals(0, compartment.getThickness());

        compartment = new Compartment("label", 2, 3, 5);
        assertEquals("label", compartment.getName());
        assertArrayEquals(new int[] {2, 3, 5}, compartment.getDimensions());
        assertEquals("label[2][3][5]", compartment.toString());
        assertEquals(0, compartment.getThickness());
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
            assertEquals(2, location.getFixedIndices().length);
            actual[location.getFixedIndices()[0]][location.getFixedIndices()[1]] = true;
        }
        String[] actualStrings = boolean2DArrayToString(actual);
        assertArrayEquals(expected, actualStrings);
        
        actual = new boolean[compartment.dimensions[0]][compartment.dimensions[1]];
        for (int y=0; y<compartment.dimensions[0]; y++) {
            for (int x=0; x<compartment.dimensions[1]; x++) {
                actual[y][x] = compartment.isValidVoxel(y, x);
            }
        }
        actualStrings = boolean2DArrayToString(actual);
        assertArrayEquals(expected, actualStrings);
    }

    private String[] boolean2DArrayToString(boolean[][] actual) {
        String[] actualStrings = new String[actual.length];
        for (int rowIndex=0; rowIndex<actual.length; rowIndex++) {
            StringBuilder builder = new StringBuilder();
            boolean[] row = actual[rowIndex];
            for (int index=0; index<row.length; index++) {
                builder.append(row[index] ? 'x' : ' ');
            }
            actualStrings[rowIndex] = builder.toString();
        }
        return actualStrings;
    }

    private void checkCompartmentReferences3D(Location[] distributedCellReferences, Compartment compartment, String[][] expected) {
        assertEquals(3, compartment.dimensions.length);
        
        boolean[][][] actual = new boolean[compartment.dimensions[0]][compartment.dimensions[1]][compartment.dimensions[2]];
        for (Location location : distributedCellReferences) {
            assertEquals(compartment.name, location.getName());
            assertEquals(3, location.getFixedIndices().length);
            actual[location.getFixedIndices()[0]][location.getFixedIndices()[1]][location.getFixedIndices()[2]] = true;
        }
        String[][] actualStrings = boolean3DArrayToString(actual);
        assertArrayEquals(expected, actualStrings);
        
        actual = new boolean[compartment.dimensions[0]][compartment.dimensions[1]][compartment.dimensions[2]];
        for (int y=0; y<compartment.dimensions[0]; y++) {
            for (int x=0; x<compartment.dimensions[1]; x++) {
                for (int z=0; z<compartment.dimensions[2]; z++) {
                    actual[y][x][z] = compartment.isValidVoxel(y, x, z);
                }
            }
        }
        actualStrings = boolean3DArrayToString(actual);
        assertArrayEquals(expected, actualStrings);
    }

    private String[][] boolean3DArrayToString(boolean[][][] actual) {
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
        return actualStrings;
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
    public void testSolidSpine() {
        SolidSpine compartment = new SolidSpine("name", new int[] {4, 4, 3});
        checkCompartmentReferences3D(compartment.getDistributedCellReferences(), compartment,
                new String[][] {
                    {"       ", " xxxxxx", " xxxxxx", "       "}, 
                    {" xxxxxx", "xxxxxxx", "xxxxxxx", " xxxxxx"}, 
                    {" xxxxxx", "xxxxxxx", "xxxxxxx", " xxxxxx"},
                    {"       ", " xxxxxx", " xxxxxx", "       "}, 
                }
        );
        
        compartment = new SolidSpine("name", new int[] {7, 5, 4});
        checkCompartmentReferences3D(compartment.getDistributedCellReferences(), compartment,
                new String[][] {
                    {"           ", "           ", "  xxx      ", "  xxx      ", "  xxx      ", "           ", "           "}, 
                    {"           ", " xxxxx     ", " xxxxxxxxxx", " xxxxxxxxxx", " xxxxxxxxxx", " xxxxx     ", "           "}, 
                    {"  xxx      ", " xxxxxxxxxx", "xxxxxxxxxxx", "xxxxxxxxxxx", "xxxxxxxxxxx", " xxxxxxxxxx", "  xxx      "}, 
                    {"  xxx      ", " xxxxxxxxxx", "xxxxxxxxxxx", "xxxxxxxxxxx", "xxxxxxxxxxx", " xxxxxxxxxx", "  xxx      "}, 
                    {"  xxx      ", " xxxxxxxxxx", "xxxxxxxxxxx", "xxxxxxxxxxx", "xxxxxxxxxxx", " xxxxxxxxxx", "  xxx      "}, 
                    {"           ", " xxxxx     ", " xxxxxxxxxx", " xxxxxxxxxx", " xxxxxxxxxx", " xxxxx     ", "           "}, 
                    {"           ", "           ", "  xxx      ", "  xxx      ", "  xxx      ", "           ", "           "}, 
                }
        );
    }
    
    @Test
    public void testOpenSpine() {
        OpenSpine compartment = new OpenSpine("name", new int[] {4, 4, 3, 1});
        checkCompartmentReferences3D(compartment.getDistributedCellReferences(), compartment,
                new String[][] {
                    {"       ", " xxxxxx", " xxxxxx", "       "}, 
                    {" xxxxxx", "x      ", "x      ", " xxxxxx"}, 
                    {" xxxxxx", "x      ", "x      ", " xxxxxx"},
                    {"       ", " xxxxxx", " xxxxxx", "       "}, 
                }
        );
        
        compartment = new OpenSpine("name", new int[] {7, 5, 4, 2});
        checkCompartmentReferences3D(compartment.getDistributedCellReferences(), compartment,
                new String[][] {
                    {"           ", "           ", "  xxx      ", "  xxx      ", "  xxx      ", "           ", "           "}, 
                    {"           ", " xxxxx     ", " xxxxxxxxxx", " xxxxxxxxxx", " xxxxxxxxxx", " xxxxx     ", "           "}, 
                    {"  xxx      ", " xxxxxxxxxx", "xxx xxxxxxx", "xx   xxxxxx", "xxx xxxxxxx", " xxxxxxxxxx", "  xxx      "}, 
                    {"  xxx      ", " xxxxxxxxxx", "xx   xxxxxx", "xx         ", "xx   xxxxxx", " xxxxxxxxxx", "  xxx      "}, 
                    {"  xxx      ", " xxxxxxxxxx", "xxx xxxxxxx", "xx   xxxxxx", "xxx xxxxxxx", " xxxxxxxxxx", "  xxx      "}, 
                    {"           ", " xxxxx     ", " xxxxxxxxxx", " xxxxxxxxxx", " xxxxxxxxxx", " xxxxx     ", "           "}, 
                    {"           ", "           ", "  xxx      ", "  xxx      ", "  xxx      ", "           ", "           "}, 
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
    
    @Test
    public void testIsValidVoxel() {
        Compartment compartment = new Compartment("single");
        
        try {
            compartment.isValidVoxel((Location) null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        assertFalse(compartment.isValidVoxel(new Location("other")));
        assertFalse(compartment.isValidVoxel(new Location("single", INDEX_0)));
        assertFalse(compartment.isValidVoxel(new Location("single", INDEX_X)));
        assertTrue(compartment.isValidVoxel(new Location("single")));
        
        compartment = new Compartment("rectangle", 3, 4);
        assertFalse(compartment.isValidVoxel(new Location("other", INDEX_0, INDEX_1)));
        assertFalse(compartment.isValidVoxel(new Location("rectangle")));
        assertFalse(compartment.isValidVoxel(new Location("rectangle", INDEX_0)));
        assertFalse(compartment.isValidVoxel(new Location("rectangle", INDEX_X, INDEX_0, INDEX_1)));
        assertFalse(compartment.isValidVoxel(new Location("rectangle", INDEX_0, new CellIndexExpression("4"))));
        assertTrue(compartment.isValidVoxel(new Location("rectangle", INDEX_X, INDEX_0)));
        assertTrue(compartment.isValidVoxel(new Location("rectangle", INDEX_1, INDEX_0)));
    }

    @Test
    public void testIsValidVoxel_voxelIndices() {
        Compartment compartment = new Compartment("single");
        
        try {
            compartment.isValidVoxel((int[]) null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        assertTrue(compartment.isValidVoxel());
        assertFalse(compartment.isValidVoxel(0));
        
        compartment = new Compartment("rectangle", 3, 4);
        assertFalse(compartment.isValidVoxel());
        assertFalse(compartment.isValidVoxel(0));
        assertFalse(compartment.isValidVoxel(0, 0, 1));
        assertFalse(compartment.isValidVoxel(0, 4));
        assertTrue(compartment.isValidVoxel(1, 0));
    }

    @Test
    public void testIsValidVoxel_solidSpine() {
        SolidSpine compartment = new SolidSpine("name", new int[] {7, 5, 4});
        
        assertFalse(compartment.isValidVoxel(0, 0, 0));
        assertFalse(compartment.isValidVoxel(3, 0, 0));
        assertTrue(compartment.isValidVoxel(3, 3, 0));
        assertTrue(compartment.isValidVoxel(3, 3, 10));
        assertFalse(compartment.isValidVoxel(3, 3, 11));
    }

    @Test
    public void testIsValidVoxel_openSpine() {
        OpenSpine compartment = new OpenSpine("name", new int[] {7, 5, 4, 2});
        
        assertFalse(compartment.isValidVoxel(0, 0, 0));
        assertFalse(compartment.isValidVoxel(3, 0, 0));
        assertTrue(compartment.isValidVoxel(3, 3, 0));
        assertTrue(compartment.isValidVoxel(3, 3, 1));
        assertFalse(compartment.isValidVoxel(3, 3, 2));
        assertFalse(compartment.isValidVoxel(3, 3, 8));
        assertFalse(compartment.isValidVoxel(3, 3, 9));
        assertFalse(compartment.isValidVoxel(3, 3, 10));
        assertFalse(compartment.isValidVoxel(3, 3, 11));
        
        assertTrue(compartment.isValidVoxel(3, 2, 8));
        assertTrue(compartment.isValidVoxel(3, 2, 9));
        assertTrue(compartment.isValidVoxel(3, 2, 10));
        assertFalse(compartment.isValidVoxel(3, 2, 11));
    }


    @Test
    public void testTranslate() {
        Compartment sourceCompartment = new Compartment("source", 3, 4);
        Compartment targetCompartment = new Compartment("target", 9, 8);
        
        int[] indices = new int[] {0, 0};
        Compartment.translate(sourceCompartment, targetCompartment, indices);
        assertArrayEquals(new int[] {3, 2}, indices);
    }
    
    @Test
    public void testTranslate_spines() {
        Compartment inner = new Compartment.SolidSpine("inner", 7, 5, 6);
        Compartment outer = new Compartment.OpenSpine("outer", 11, 9, 4, 2);
        
        int[] indices = new int[] {0, 0, 0};
        Compartment.translate(inner, outer, indices);
        assertArrayEquals(new int[] {2, 2, 2}, indices);
        
        indices = new int[] {0, 0, 0};
        Compartment.translate(outer, inner, indices);
        assertArrayEquals(new int[] {-2, -2, -2}, indices);

        inner = new Compartment.OpenSpine("inner", 6, 4, 6, 1);
        outer = new Compartment.OpenSpine("outer", 10, 8, 4, 2);
        
        indices = new int[] {0, 0, 0};
        Compartment.translate(inner, outer, indices);
        assertArrayEquals(new int[] {2, 2, 2}, indices);
        
        indices = new int[] {0, 0, 0};
        Compartment.translate(outer, inner, indices);
        assertArrayEquals(new int[] {-2, -2, -2}, indices);
    }
    
    @Test
    public void testCreateCompartment() {
        try {
            Compartment.createCompartment(null, "type", new ArrayList<Integer>());
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            Compartment.createCompartment("label", "type", null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            Compartment.createCompartment("label", "unknownShape", new ArrayList<Integer>());
            fail("unknown shape should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        Compartment compartment = Compartment.createCompartment("compartment1", null, new ArrayList<Integer>());
        assertEquals("compartment1", compartment.toString());
        compartment = Compartment.createCompartment("compartment2", null, getList(2, 3));
        assertEquals("compartment2[2][3]", compartment.toString());
    }

    @Test
    public void testCreateCompartment_openRectangle() {
        try {
            Compartment.createCompartment("label", "OpenRectangle", getList(1, 2));
            fail("wrong dimension count should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            Compartment.createCompartment("label", "OpenRectangle", getList(1, 2, 3, 4));
            fail("wrong dimension count should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            Compartment.createCompartment("label", "OpenRectangle", getList(10, 8, 5));
            fail("too thick should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        Compartment compartment = Compartment.createCompartment("compartment1", "OpenRectangle", getList(10, 8, 3));
        assertEquals(Compartment.OpenRectangle.class, compartment.getClass());
        assertEquals("compartment1 (OpenRectangle) [10][8] [3]", compartment.toString());
        assertEquals(3, compartment.getThickness());
    }

    @Test
    public void testCreateCompartment_solidCircle() {
        try {
            Compartment.createCompartment("label", "SolidCircle", new ArrayList<Integer>());
            fail("wrong dimension count should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            Compartment.createCompartment("label", "SolidCircle", getList(1, 2));
            fail("wrong dimension count should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        Compartment compartment = Compartment.createCompartment("compartment1", "SolidCircle", getList(10));
        assertEquals(Compartment.SolidCircle.class, compartment.getClass());
        assertEquals("compartment1 (SolidCircle) [10]", compartment.toString());
        assertEquals(0, compartment.getThickness());
    }


    @Test
    public void testCreateCompartment_openCircle() {
        try {
            Compartment.createCompartment("label", "OpenCircle", getList(1));
            fail("wrong dimension count should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            Compartment.createCompartment("label", "OpenCircle", getList(1, 2, 3));
            fail("wrong dimension count should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            Compartment.createCompartment("label", "OpenCircle", getList(10, 8));
            fail("too thick should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        Compartment compartment = Compartment.createCompartment("compartment1", "OpenCircle", getList(10, 3));
        assertEquals(Compartment.OpenCircle.class, compartment.getClass());
        assertEquals("compartment1 (OpenCircle) [10] [3]", compartment.toString());
        assertEquals(3, compartment.getThickness());
    }

    @Test
    public void testCreateCompartment_openCuboid() {
        try {
            Compartment.createCompartment("label", "OpenCuboid", getList(1, 2, 2));
            fail("wrong dimension count should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            Compartment.createCompartment("label", "OpenCuboid", getList(10, 20, 30, 40, 4));
            fail("wrong dimension count should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            Compartment.createCompartment("label", "OpenCuboid", getList(10, 5, 8, 3));
            fail("too thick should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        Compartment compartment = Compartment.createCompartment("compartment1", "OpenCuboid", getList(10, 5, 8, 2));
        assertEquals(Compartment.OpenCuboid.class, compartment.getClass());
        assertEquals("compartment1 (OpenCuboid) [10][5][8] [2]", compartment.toString());
        assertEquals(2, compartment.getThickness());
    }

    @Test
    public void testCreateCompartment_solidSphere() {
        try {
            Compartment.createCompartment("label", "SolidSphere", new ArrayList<Integer>());
            fail("wrong dimension count should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            Compartment.createCompartment("label", "SolidSphere", getList(1, 2));
            fail("wrong dimension count should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        Compartment compartment = Compartment.createCompartment("compartment1", "SolidSphere", getList(10));
        assertEquals(Compartment.SolidSphere.class, compartment.getClass());
        assertEquals("compartment1 (SolidSphere) [10]", compartment.toString());
        assertEquals(0, compartment.getThickness());
    }

    @Test
    public void testCreateCompartment_openSphere() {
        try {
            Compartment.createCompartment("label", "OpenSphere", getList(2));
            fail("wrong dimension count should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            Compartment.createCompartment("label", "OpenSphere", getList(10, 20, 4));
            fail("wrong dimension count should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            Compartment.createCompartment("label", "OpenSphere", getList(10, 8));
            fail("too thick should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        Compartment compartment = Compartment.createCompartment("compartment1", "OpenSphere", getList(10, 2));
        assertEquals(Compartment.OpenSphere.class, compartment.getClass());
        assertEquals("compartment1 (OpenSphere) [10] [2]", compartment.toString());
        assertEquals(2, compartment.getThickness());
    }

    @Test
    public void testCreateCompartment_solidCylinder() {
        try {
            Compartment.createCompartment("label", "SolidCylinder", getList(10));
            fail("wrong dimension count should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            Compartment.createCompartment("label", "SolidCylinder", getList(1, 2, 3));
            fail("wrong dimension count should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        Compartment compartment = Compartment.createCompartment("compartment1", "SolidCylinder", getList(10, 20));
        assertEquals(Compartment.SolidCylinder.class, compartment.getClass());
        assertEquals("compartment1 (SolidCylinder) [10][20]", compartment.toString());
        assertEquals(0, compartment.getThickness());
    }

    @Test
    public void testCreateCompartment_openCylinder() {
        try {
            Compartment.createCompartment("label", "OpenCylinder", getList(2, 20));
            fail("wrong dimension count should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            Compartment.createCompartment("label", "OpenCylinder", getList(10, 20, 20, 4));
            fail("wrong dimension count should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            Compartment.createCompartment("label", "OpenCylinder", getList(10, 20, 8));
            fail("too thick should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        Compartment compartment = Compartment.createCompartment("compartment1", "OpenCylinder", getList(10, 20, 2));
        assertEquals(Compartment.OpenCylinder.class, compartment.getClass());
        assertEquals("compartment1 (OpenCylinder) [10][20] [2]", compartment.toString());
        assertEquals(2, compartment.getThickness());
    }

    @Test
    public void testCreateCompartment_solidSpine() {
        try {
            Compartment.createCompartment("label", "SolidSpine", getList(10, 10));
            fail("wrong dimension count should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            Compartment.createCompartment("label", "SolidSpine", getList(10, 10, 10, 10));
            fail("wrong dimension count should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            Compartment.createCompartment("label", "SolidSpine", getList(10, 9, 10));
            fail("mismatched diameters should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            Compartment.createCompartment("label", "SolidSpine", getList(10, 12, 10));
            fail("too wide cylinder should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        Compartment compartment = Compartment.createCompartment("compartment1", "SolidSpine", getList(10, 8, 5));
        assertEquals(Compartment.SolidSpine.class, compartment.getClass());
        assertEquals("compartment1 (SolidSpine) [10][8][5]", compartment.toString());
        assertEquals(0, compartment.getThickness());
    }

    @Test
    public void testCreateCompartment_openSpine() {
        try {
            Compartment.createCompartment("label", "OpenSpine", getList(10, 10, 2));
            fail("wrong dimension count should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            Compartment.createCompartment("label", "OpenSpine", getList(10, 10, 10, 10, 2));
            fail("wrong dimension count should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            Compartment.createCompartment("label", "OpenSpine", getList(10, 9, 10, 2));
            fail("mismatched diameters should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            Compartment.createCompartment("label", "OpenSpine", getList(10, 12, 10, 2));
            fail("too wide cylinder should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            Compartment.createCompartment("label", "OpenSpine", getList(12, 8, 5, 5));
            fail("too thick should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        Compartment compartment = Compartment.createCompartment("compartment1", "OpenSpine", getList(10, 8, 5, 2));
        assertEquals(Compartment.OpenSpine.class, compartment.getClass());
        assertEquals("compartment1 (OpenSpine) [10][8][5] [2]", compartment.toString());
        assertEquals(2, compartment.getThickness());
    }

    @Test
    public void testCreateVoxelArray() {
        Compartment compartment = new Compartment("label");
        assertNull(compartment.createVoxelArray());

        compartment = new Compartment("label", 2, 3, 5);
        assertEquals("[[[0, 0, 0, 0, 0], [0, 0, 0, 0, 0], [0, 0, 0, 0, 0]], " +
        		"[[0, 0, 0, 0, 0], [0, 0, 0, 0, 0], [0, 0, 0, 0, 0]]]", 
                Arrays.deepToString(compartment.createVoxelArray()));

        compartment = new Compartment.OpenCylinder("label", 3, 4, 1);
        assertEquals("[[[0, 0, 0, 0], [0, 0, 0, 0], [0, 0, 0, 0]], " +
        		"[[0, 0, 0, 0], [0, 0, 0, 0], [0, 0, 0, 0]], " +
        		"[[0, 0, 0, 0], [0, 0, 0, 0], [0, 0, 0, 0]]]", 
                Arrays.deepToString(compartment.createVoxelArray()));

    }

    
    @Test
    public void testValidate() {
        Compartment compartment = new Compartment("known");
        compartment.validate();

        try {
            compartment = new Compartment("fixed");
            compartment.validate();
            fail("validation should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
            assertEquals("Compartment 'fixed' uses a reserved compartment name", ex.getMessage());
        }
    }


    
}
