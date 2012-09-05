package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_0;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_2;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_X;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_X_MINUS_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_X_PLUS_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_Y;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_Y_MINUS_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_Y_PLUS_1;
import static org.demonsoft.spatialkappa.model.ChannelConstraint.FIXED_CONSTRAINT;
import static org.demonsoft.spatialkappa.model.Location.FIXED_LOCATION;
import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;
import static org.demonsoft.spatialkappa.model.Utils.getList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.demonsoft.spatialkappa.model.ChannelComponent.EdgeNeighbourComponent;
import org.demonsoft.spatialkappa.model.ChannelComponent.FaceNeighbourComponent;
import org.demonsoft.spatialkappa.model.ChannelComponent.HexagonalComponent;
import org.demonsoft.spatialkappa.model.ChannelComponent.LateralComponent;
import org.demonsoft.spatialkappa.model.ChannelComponent.NeighbourComponent;
import org.demonsoft.spatialkappa.model.ChannelComponent.PredefinedChannelComponent;
import org.demonsoft.spatialkappa.model.ChannelComponent.RadialComponent;
import org.demonsoft.spatialkappa.model.ChannelComponent.RadialInComponent;
import org.demonsoft.spatialkappa.model.ChannelComponent.RadialOutComponent;
import org.demonsoft.spatialkappa.model.Compartment.OpenCircle;
import org.demonsoft.spatialkappa.model.Compartment.OpenCuboid;
import org.demonsoft.spatialkappa.model.Compartment.OpenCylinder;
import org.demonsoft.spatialkappa.model.Compartment.OpenRectangle;
import org.demonsoft.spatialkappa.model.Compartment.OpenSphere;
import org.demonsoft.spatialkappa.model.Compartment.SolidCircle;
import org.demonsoft.spatialkappa.model.Compartment.SolidCylinder;
import org.demonsoft.spatialkappa.model.Compartment.SolidSphere;
import org.demonsoft.spatialkappa.model.VariableExpression.Operator;
import org.junit.Test;

public class ChannelComponentTest {

    public static CellIndexExpression INDEX_3 = new CellIndexExpression("3");
    public static CellIndexExpression INDEX_4 = new CellIndexExpression("4");
    public static CellIndexExpression INDEX_6 = new CellIndexExpression("6");


    @SuppressWarnings({ "unused" })
    @Test
    public void testConstructor() {
        Location reference1 = new Location("a");
        Location reference2 = new Location("b");
        
        try {
            new ChannelComponent(null, getList(reference1));
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            new ChannelComponent(getList(reference1), null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        ChannelComponent component = new ChannelComponent(getList(reference1), getList(reference2));
        assertEquals("[[a -> b]]", component.toString());
    }
    
    
    
    @Test
    public void testGetCellReferencePairs() {
        Location reference1 = new Location("a");
        Location reference2 = new Location("b");
        ChannelComponent component = new ChannelComponent(reference1, reference2);
        List<Compartment> compartments = new ArrayList<Compartment>();
        
        try {
            component.getCellReferencePairs(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        compartments.add(new Compartment("a"));
        
        try {
            component.getCellReferencePairs(compartments);
            fail("missing compartment should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
        }
        
        compartments.clear();
        compartments.add(new Compartment("b"));

        try {
            component.getCellReferencePairs(compartments);
            fail("missing compartment should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
        }
        
        compartments.clear();
        compartments.add(new Compartment("a", 3));
        compartments.add(new Compartment("b"));

        try {
            component.getCellReferencePairs(compartments);
            fail("dimension mismatch should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        // Single cell compartments
        compartments.clear();
        reference1 = new Location("a");
        reference2 = new Location("b");
        component = new ChannelComponent(reference1, reference2);
        compartments.add(new Compartment("a"));
        compartments.add(new Compartment("b"));
        
        assertEquals(getList(new ChannelConstraint(new Location("a"), new Location("b"))), 
                component.getCellReferencePairs(compartments));
        
        // Single cell-cell link
        compartments.clear();
        reference1 = new Location("a", INDEX_1);
        reference2 = new Location("a", INDEX_2);
        component = new ChannelComponent(reference1, reference2);
        compartments.add(new Compartment("a", 4));
        
        assertEquals(getList(new ChannelConstraint(new Location("a", INDEX_1), new Location("a", INDEX_2))), 
                component.getCellReferencePairs(compartments));

        VariableReference refX = new VariableReference("x");
        
        // Linear array
        compartments.clear();
        reference1 = new Location("a", INDEX_X);
        reference2 = new Location("a", INDEX_X_PLUS_1);
        component = new ChannelComponent(reference1, reference2);
        compartments.add(new Compartment("a", 4));
        
        assertEquals(getList(
                new ChannelConstraint(new Location("a", INDEX_0), new Location("a", INDEX_1)),
                new ChannelConstraint(new Location("a", INDEX_1), new Location("a", INDEX_2)),
                new ChannelConstraint(new Location("a", INDEX_2), new Location("a", new CellIndexExpression("3")))),
                component.getCellReferencePairs(compartments));

        // Linear array negative
        compartments.clear();
        reference1 = new Location("a", new CellIndexExpression(refX));
        reference2 = new Location("a", new CellIndexExpression(INDEX_X, Operator.MINUS, INDEX_1));
        component = new ChannelComponent(reference1, reference2);
        compartments.add(new Compartment("a", 4));
        
        assertEquals(getList(
                new ChannelConstraint(new Location("a", INDEX_1), new Location("a", INDEX_0)),
                new ChannelConstraint(new Location("a", INDEX_2), new Location("a", INDEX_1)),
                new ChannelConstraint(new Location("a", new CellIndexExpression("3")), new Location("a", INDEX_2))),
                component.getCellReferencePairs(compartments));

        // TODO channel type tests
    }
    
    @Test
    public void testApplyChannel_singleCompartment() {
        Location reference1 = new Location("a", INDEX_X);
        Location reference2 = new Location("a", INDEX_X_PLUS_1);
        ChannelComponent component = new ChannelComponent(reference1, reference2);
        List<Compartment> compartments = getList(new Compartment("a", 4), new Compartment("b", 2));
        
        try {
            component.applyChannel(null, compartments);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            component.applyChannel(getList(new ChannelConstraint(new Location("a", INDEX_1), new Location("a"))), null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            // Unknown compartment
            ChannelComponent component2 = new ChannelComponent(new Location("a", INDEX_X), 
                    new Location("c", INDEX_X_PLUS_1));
            component2.applyChannel(getList(new ChannelConstraint(new Location("a", INDEX_1), NOT_LOCATED)), compartments);
            fail("missing compartment should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
        }

        try {
            // Unknown compartment
            ChannelComponent component2 = new ChannelComponent(new Location("c", INDEX_X), 
                    new Location("a", INDEX_X_PLUS_1));
            component2.applyChannel(getList(new ChannelConstraint(new Location("a", INDEX_1), NOT_LOCATED)), compartments);
            fail("missing compartment should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
        }

        List<Location> expected = new ArrayList<Location>();

        assertEquals(expected, component.applyChannel(new Location("b", INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a"), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_X), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", new CellIndexExpression("3")), NOT_LOCATED, compartments));
        
        expected.add(new Location("a", INDEX_2));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1), NOT_LOCATED, compartments));
        
        // Check target location constraints
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1), new Location("a"), compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1), new Location("a", INDEX_2), compartments));
        expected.clear();
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1), new Location("b"), compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1), new Location("a", INDEX_0), compartments));
        
        compartments.clear();
        compartments.add(new Compartment("a", 3));
        
        reference1 = new Location("a", INDEX_X);
        reference2 = new Location("a", new CellIndexExpression(INDEX_X, Operator.MINUS, INDEX_1));
        component = new ChannelComponent(reference1, reference2);

        expected.clear();
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0), NOT_LOCATED, compartments));

        expected.add(new Location("a", INDEX_0));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1), NOT_LOCATED, compartments));
        
        compartments.clear();
        compartments.add(new Compartment("a", 5, 5, 5));
        
        reference1 = new Location("a", INDEX_X, INDEX_Y, INDEX_0);
        reference2 = new Location("a", INDEX_X_PLUS_1, new CellIndexExpression(INDEX_Y, Operator.MULTIPLY, INDEX_2), INDEX_2);
        component = new ChannelComponent(reference1, reference2);

        expected.clear();
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_2, INDEX_1, INDEX_1), NOT_LOCATED, compartments));

        expected.add(new Location("a", new CellIndexExpression("3"), new CellIndexExpression("4"), INDEX_2));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_2, INDEX_2, INDEX_0), NOT_LOCATED, compartments));
    }
    
    
    @Test
    public void testApplyChannel_fixedTarget() {
        Location reference1 = new Location("b", INDEX_0);
        Location reference2 = new Location("b", INDEX_1);
        ChannelComponent component = new ChannelComponent(reference1, reference2);
        List<Compartment> compartments = getList(new Compartment("b", 2));
        
        List<ChannelConstraint> constraints = getList(new ChannelConstraint(new Location("b", INDEX_0), new Location("b", INDEX_1)), 
                new ChannelConstraint(new Location("b", INDEX_0), FIXED_LOCATION));

        List<List<Location>> expected = new ArrayList<List<Location>>();
        expected.add(getList(new Location("b", INDEX_1), new Location("b", INDEX_0)));

        assertEquals(expected, component.applyChannel(constraints, compartments));
    }
    
    @Test
    public void testApplyChannel_edgeNeighbour_intracompartment() {
        Location location = new Location("a");
        ChannelComponent component = new EdgeNeighbourComponent(getList(location), getList(location));
        List<Compartment> compartments = getList(new Compartment("a", 4, 4), new Compartment("b", 4, 4));
        

        List<Location> expected = new ArrayList<Location>();

        // Invalid input locations
        assertEquals(expected, component.applyChannel(new Location("b", INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a"), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_X), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, new CellIndexExpression("4")), NOT_LOCATED, compartments));
        
        // Corner voxel
        expected.add(new Location("a", INDEX_1, INDEX_0));
        expected.add(new Location("a", INDEX_0, INDEX_1));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0), NOT_LOCATED, compartments));
        
        // Check target location constraints
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0), new Location("a"), compartments));
        
        expected.clear();
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0), new Location("b"), compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0), new Location("a", INDEX_0, INDEX_0), compartments));
        
        expected.add(new Location("a", INDEX_0, INDEX_1));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0), new Location("a", INDEX_0, INDEX_1), compartments));

        // Inner voxel
        expected.clear();
        expected.add(new Location("a", INDEX_0, INDEX_1));
        expected.add(new Location("a", INDEX_2, INDEX_1));
        expected.add(new Location("a", INDEX_1, INDEX_0));
        expected.add(new Location("a", INDEX_1, INDEX_2));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        
    }
    



    @Test
    public void testApplyChannel_faceNeighbour_intracompartment() {
        Location location = new Location("a");
        ChannelComponent component = new FaceNeighbourComponent(getList(location), getList(location));
        List<Compartment> compartments = getList(new Compartment("a", 4, 4, 4), new Compartment("b", 4, 4, 4));
        

        List<Location> expected = new ArrayList<Location>();

        // Invalid input locations
        assertEquals(expected, component.applyChannel(new Location("b", INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a"), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_X, INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_1, new CellIndexExpression("4")), NOT_LOCATED, compartments));
        
        // Corner voxel
        expected.add(new Location("a", INDEX_1, INDEX_0, INDEX_0));
        expected.add(new Location("a", INDEX_0, INDEX_1, INDEX_0));
        expected.add(new Location("a", INDEX_0, INDEX_0, INDEX_1));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0, INDEX_0), NOT_LOCATED, compartments));
        
        // Check target location constraints
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0, INDEX_0), new Location("a"), compartments));
        
        expected.clear();
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0, INDEX_0), new Location("b"), compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0, INDEX_0), new Location("a", INDEX_0, INDEX_0, INDEX_0), compartments));
        
        expected.add(new Location("a", INDEX_0, INDEX_0, INDEX_1));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0, INDEX_0), new Location("a", INDEX_0, INDEX_0, INDEX_1), compartments));

        // Inner voxel
        expected.clear();
        expected.add(new Location("a", INDEX_0, INDEX_1, INDEX_1));
        expected.add(new Location("a", INDEX_2, INDEX_1, INDEX_1));
        expected.add(new Location("a", INDEX_1, INDEX_0, INDEX_1));
        expected.add(new Location("a", INDEX_1, INDEX_2, INDEX_1));
        expected.add(new Location("a", INDEX_1, INDEX_1, INDEX_0));
        expected.add(new Location("a", INDEX_1, INDEX_1, INDEX_2));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        
    }
    
    @Test
    public void testApplyChannel_neighbour3d_intracompartment() {
        Location location = new Location("a");
        ChannelComponent component = new NeighbourComponent(getList(location), getList(location));
        List<Compartment> compartments = getList(new Compartment("a", 4, 4, 4), new Compartment("b", 4, 4, 4));
        

        List<Location> expected = new ArrayList<Location>();

        // Invalid input locations
        assertEquals(expected, component.applyChannel(new Location("b", INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a"), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_X, INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_1, new CellIndexExpression("4")), NOT_LOCATED, compartments));
        
        // Corner voxel
        expected.add(new Location("a", INDEX_0, INDEX_0, INDEX_1));
        expected.add(new Location("a", INDEX_0, INDEX_1, INDEX_0));
        expected.add(new Location("a", INDEX_0, INDEX_1, INDEX_1));
        expected.add(new Location("a", INDEX_1, INDEX_0, INDEX_0));
        expected.add(new Location("a", INDEX_1, INDEX_0, INDEX_1));
        expected.add(new Location("a", INDEX_1, INDEX_1, INDEX_0));
        expected.add(new Location("a", INDEX_1, INDEX_1, INDEX_1));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0, INDEX_0), NOT_LOCATED, compartments));
        
        // Check target location constraints
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0, INDEX_0), new Location("a"), compartments));
        
        expected.clear();
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0, INDEX_0), new Location("b"), compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0, INDEX_0), new Location("a", INDEX_0, INDEX_0, INDEX_0), compartments));
        
        expected.add(new Location("a", INDEX_0, INDEX_0, INDEX_1));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0, INDEX_0), new Location("a", INDEX_0, INDEX_0, INDEX_1), compartments));

        // Inner voxel
        expected.clear();
        expected.add(new Location("a", INDEX_0, INDEX_0, INDEX_0));
        expected.add(new Location("a", INDEX_0, INDEX_0, INDEX_1));
        expected.add(new Location("a", INDEX_0, INDEX_0, INDEX_2));
        expected.add(new Location("a", INDEX_0, INDEX_1, INDEX_0));
        expected.add(new Location("a", INDEX_0, INDEX_1, INDEX_1));
        expected.add(new Location("a", INDEX_0, INDEX_1, INDEX_2));
        expected.add(new Location("a", INDEX_0, INDEX_2, INDEX_0));
        expected.add(new Location("a", INDEX_0, INDEX_2, INDEX_1));
        expected.add(new Location("a", INDEX_0, INDEX_2, INDEX_2));
        expected.add(new Location("a", INDEX_1, INDEX_0, INDEX_0));
        expected.add(new Location("a", INDEX_1, INDEX_0, INDEX_1));
        expected.add(new Location("a", INDEX_1, INDEX_0, INDEX_2));
        expected.add(new Location("a", INDEX_1, INDEX_1, INDEX_0));
        expected.add(new Location("a", INDEX_1, INDEX_1, INDEX_2));
        expected.add(new Location("a", INDEX_1, INDEX_2, INDEX_0));
        expected.add(new Location("a", INDEX_1, INDEX_2, INDEX_1));
        expected.add(new Location("a", INDEX_1, INDEX_2, INDEX_2));
        expected.add(new Location("a", INDEX_2, INDEX_0, INDEX_0));
        expected.add(new Location("a", INDEX_2, INDEX_0, INDEX_1));
        expected.add(new Location("a", INDEX_2, INDEX_0, INDEX_2));
        expected.add(new Location("a", INDEX_2, INDEX_1, INDEX_0));
        expected.add(new Location("a", INDEX_2, INDEX_1, INDEX_1));
        expected.add(new Location("a", INDEX_2, INDEX_1, INDEX_2));
        expected.add(new Location("a", INDEX_2, INDEX_2, INDEX_0));
        expected.add(new Location("a", INDEX_2, INDEX_2, INDEX_1));
        expected.add(new Location("a", INDEX_2, INDEX_2, INDEX_2));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        
    }
    

    @Test
    public void testApplyChannel_hexagonal_intracompartment() {
        Location location = new Location("a");
        ChannelComponent component = new HexagonalComponent(getList(location), getList(location));
        List<Compartment> compartments = getList(new Compartment("a", 4, 4), new Compartment("b", 4, 4));
        

        List<Location> expected = new ArrayList<Location>();

        // Invalid input locations
        assertEquals(expected, component.applyChannel(new Location("b", INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a"), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_X), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, new CellIndexExpression("4")), NOT_LOCATED, compartments));
        
        // Corner voxel
        expected.add(new Location("a", INDEX_1, INDEX_0));
        expected.add(new Location("a", INDEX_0, INDEX_1));
        expected.add(new Location("a", INDEX_1, INDEX_1));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0), NOT_LOCATED, compartments));
        
        // Check target location constraints
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0), new Location("a"), compartments));
        
        expected.clear();
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0), new Location("b"), compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0), new Location("a", INDEX_0, INDEX_0), compartments));
        
        expected.add(new Location("a", INDEX_0, INDEX_1));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0), new Location("a", INDEX_0, INDEX_1), compartments));

        // Inner voxel
        expected.clear();
        expected.add(new Location("a", INDEX_0, INDEX_1));
        expected.add(new Location("a", INDEX_2, INDEX_1));
        expected.add(new Location("a", INDEX_1, INDEX_0));
        expected.add(new Location("a", INDEX_1, INDEX_2));
        expected.add(new Location("a", INDEX_0, INDEX_0));
        expected.add(new Location("a", INDEX_2, INDEX_0));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        
    }
    

    @Test
    public void testApplyChannel_neighbour2d_intracompartment() {
        Location location = new Location("a");
        ChannelComponent component = new NeighbourComponent(getList(location), getList(location));
        List<Compartment> compartments = getList(new Compartment("a", 4, 4), new Compartment("b", 4, 4));
        

        List<Location> expected = new ArrayList<Location>();

        // Invalid input locations
        assertEquals(expected, component.applyChannel(new Location("b", INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a"), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_X), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, new CellIndexExpression("4")), NOT_LOCATED, compartments));
        
        // Corner voxel
        expected.add(new Location("a", INDEX_1, INDEX_0));
        expected.add(new Location("a", INDEX_0, INDEX_1));
        expected.add(new Location("a", INDEX_1, INDEX_1));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0), NOT_LOCATED, compartments));
        
        // Check target location constraints
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0), new Location("a"), compartments));
        
        expected.clear();
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0), new Location("b"), compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0), new Location("a", INDEX_0, INDEX_0), compartments));
        
        expected.add(new Location("a", INDEX_0, INDEX_1));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0), new Location("a", INDEX_0, INDEX_1), compartments));

        // Inner voxel
        expected.clear();
        expected.add(new Location("a", INDEX_0, INDEX_1));
        expected.add(new Location("a", INDEX_2, INDEX_1));
        expected.add(new Location("a", INDEX_1, INDEX_0));
        expected.add(new Location("a", INDEX_1, INDEX_2));
        expected.add(new Location("a", INDEX_0, INDEX_0));
        expected.add(new Location("a", INDEX_2, INDEX_2));
        expected.add(new Location("a", INDEX_0, INDEX_2));
        expected.add(new Location("a", INDEX_2, INDEX_0));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        
    }


    @Test
    public void testApplyChannel_neighbour2d_intercompartment_rectangle() {
        Location locationA = new Location("a");
        Location locationB = new Location("b");
        ChannelComponent component = new NeighbourComponent(getList(locationA), getList(locationB));
        List<Compartment> compartments = getList(new Compartment("a", 4, 3), new OpenRectangle("b", 8, 7, 2));
        

        List<Location> expected = new ArrayList<Location>();

        // Invalid input locations
        assertEquals(expected, component.applyChannel(new Location("b", INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a"), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_X), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, new CellIndexExpression("4")), NOT_LOCATED, compartments));
        
        // Inner voxel
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1), NOT_LOCATED, compartments));

        // Edge voxel
        expected.add(new Location("b", INDEX_6, INDEX_3));
        expected.add(new Location("b", INDEX_6, INDEX_4));
        expected.add(new Location("b", INDEX_6, INDEX_2));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_3, INDEX_1), NOT_LOCATED, compartments));
        
        // Corner voxel
        expected.clear();
        expected.add(new Location("b", INDEX_1, INDEX_2));
        expected.add(new Location("b", INDEX_2, INDEX_1));
        expected.add(new Location("b", INDEX_1, INDEX_1));
        expected.add(new Location("b", INDEX_1, INDEX_3));
        expected.add(new Location("b", INDEX_3, INDEX_1));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0), NOT_LOCATED, compartments));
        
        // Check target location constraints
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0), new Location("b"), compartments));
        
        expected.clear();
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0), new Location("a"), compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0), new Location("b", INDEX_0, INDEX_0), compartments));
        
        expected.add(new Location("b", INDEX_1, INDEX_1));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, INDEX_0), new Location("b", INDEX_1, INDEX_1), compartments));
    }

    @Test
    public void testApplyChannel_radial2d_intracompartment() {
        Location location = new Location("a");
        ChannelComponent component = new RadialComponent(getList(location), getList(location));
        List<Compartment> compartments = getList(new Compartment("a", 5, 5), new Compartment("b", 4, 4));
        
        List<Location> expected = new ArrayList<Location>();

        // Invalid input locations
        assertEquals(expected, component.applyChannel(new Location("b", INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a"), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_X), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, new CellIndexExpression("54")), NOT_LOCATED, compartments));
        
        // Central voxel
        expected.add(new Location("a", INDEX_1, INDEX_1));
        expected.add(new Location("a", INDEX_1, INDEX_2));
        expected.add(new Location("a", INDEX_1, INDEX_3));
        expected.add(new Location("a", INDEX_2, INDEX_1));
        expected.add(new Location("a", INDEX_2, INDEX_3));
        expected.add(new Location("a", INDEX_3, INDEX_1));
        expected.add(new Location("a", INDEX_3, INDEX_2));
        expected.add(new Location("a", INDEX_3, INDEX_3));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_2, INDEX_2), NOT_LOCATED, compartments));
        
        // Check target location constraints
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_2, INDEX_2), new Location("a"), compartments));
        
        expected.clear();
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_2, INDEX_2), new Location("b"), compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_2, INDEX_2), new Location("a", INDEX_0, INDEX_0), compartments));
        
        expected.add(new Location("a", INDEX_1, INDEX_1));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_2, INDEX_2), new Location("a", INDEX_1, INDEX_1), compartments));

        // Midway voxel
        expected.clear();
        expected.add(new Location("a", INDEX_0, INDEX_1));
        expected.add(new Location("a", INDEX_1, INDEX_0));
        expected.add(new Location("a", INDEX_0, INDEX_0));
        expected.add(new Location("a", INDEX_2, INDEX_2));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        
        // Outer voxel
        expected.clear();
        expected.add(new Location("a", INDEX_1, INDEX_1));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_0), NOT_LOCATED, compartments));
    }

    @Test
    public void testApplyChannel_radialOut2d_intracompartment() {
        Location location = new Location("a");
        ChannelComponent component = new RadialOutComponent(getList(location), getList(location));
        List<Compartment> compartments = getList(new Compartment("a", 5, 5), new Compartment("b", 4, 4));
        
        List<Location> expected = new ArrayList<Location>();

        // Invalid input locations
        assertEquals(expected, component.applyChannel(new Location("b", INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a"), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_X), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, new CellIndexExpression("54")), NOT_LOCATED, compartments));
        
        // Central voxel
        expected.add(new Location("a", INDEX_1, INDEX_1));
        expected.add(new Location("a", INDEX_1, INDEX_2));
        expected.add(new Location("a", INDEX_1, INDEX_3));
        expected.add(new Location("a", INDEX_2, INDEX_1));
        expected.add(new Location("a", INDEX_2, INDEX_3));
        expected.add(new Location("a", INDEX_3, INDEX_1));
        expected.add(new Location("a", INDEX_3, INDEX_2));
        expected.add(new Location("a", INDEX_3, INDEX_3));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_2, INDEX_2), NOT_LOCATED, compartments));
        
        // Check target location constraints
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_2, INDEX_2), new Location("a"), compartments));
        
        expected.clear();
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_2, INDEX_2), new Location("b"), compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_2, INDEX_2), new Location("a", INDEX_0, INDEX_0), compartments));
        
        expected.add(new Location("a", INDEX_1, INDEX_1));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_2, INDEX_2), new Location("a", INDEX_1, INDEX_1), compartments));

        // Midway voxel
        expected.clear();
        expected.add(new Location("a", INDEX_0, INDEX_1));
        expected.add(new Location("a", INDEX_1, INDEX_0));
        expected.add(new Location("a", INDEX_0, INDEX_0));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        
        // Outer voxel
        expected.clear();
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_0), NOT_LOCATED, compartments));
    }


    @Test
    public void testApplyChannel_lateral2d_intracompartment() {
        Location location = new Location("a");
        ChannelComponent component = new LateralComponent(getList(location), getList(location));
        List<Compartment> compartments = getList(new Compartment("a", 5, 5), new Compartment("b", 4, 4));
        
        List<Location> expected = new ArrayList<Location>();

        // Invalid input locations
        assertEquals(expected, component.applyChannel(new Location("b", INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a"), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_X), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, new CellIndexExpression("54")), NOT_LOCATED, compartments));
        
        // Central voxel
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_2, INDEX_2), NOT_LOCATED, compartments));

        // Midway voxel
        expected.clear();
        expected.add(new Location("a", INDEX_1, INDEX_2));
        expected.add(new Location("a", INDEX_2, INDEX_1));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        
        // Outer voxel
        expected.clear();
        expected.add(new Location("a", INDEX_0, INDEX_1));
        expected.add(new Location("a", INDEX_2, INDEX_0));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_0), NOT_LOCATED, compartments));
        
        // Check target location constraints
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_0), new Location("a"), compartments));
        
        expected.clear();
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_0), new Location("b"), compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_0), new Location("a", INDEX_0, INDEX_0), compartments));
        
        expected.add(new Location("a", INDEX_0, INDEX_1));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_0), new Location("a", INDEX_0, INDEX_1), compartments));
    }

    @Test
    public void testApplyChannel_lateral3d_intracompartment() {
        Location location = new Location("a");
        ChannelComponent component = new LateralComponent(getList(location), getList(location));
        List<Compartment> compartments = getList(new Compartment("a", 5, 5, 5), new Compartment("b", 4, 4, 4));
        
        List<Location> expected = new ArrayList<Location>();

        // Invalid input locations
        assertEquals(expected, component.applyChannel(new Location("b", INDEX_1, INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a"), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_X), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_0, new CellIndexExpression("5")), NOT_LOCATED, compartments));
        
        // Central voxel
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_2, INDEX_2, INDEX_2), NOT_LOCATED, compartments));

        // Midway voxel
        expected.clear();
        expected.add(new Location("a", INDEX_0, INDEX_2, INDEX_2));
        expected.add(new Location("a", INDEX_1, INDEX_1, INDEX_2));
        expected.add(new Location("a", INDEX_1, INDEX_2, INDEX_1));
        expected.add(new Location("a", INDEX_2, INDEX_0, INDEX_2));
        expected.add(new Location("a", INDEX_2, INDEX_1, INDEX_1));
        expected.add(new Location("a", INDEX_2, INDEX_2, INDEX_0));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        
        // Outer voxel
        expected.clear();
        expected.add(new Location("a", INDEX_0, INDEX_1, INDEX_1));
        expected.add(new Location("a", INDEX_0, INDEX_2, INDEX_0));
        expected.add(new Location("a", INDEX_0, INDEX_2, INDEX_1));
        expected.add(new Location("a", INDEX_1, INDEX_0, INDEX_1));
        expected.add(new Location("a", INDEX_1, INDEX_2, INDEX_0));
        expected.add(new Location("a", INDEX_2, INDEX_0, INDEX_0));
        expected.add(new Location("a", INDEX_2, INDEX_0, INDEX_1));
        expected.add(new Location("a", INDEX_2, INDEX_1, INDEX_0));
        expected.add(new Location("a", INDEX_2, INDEX_2, INDEX_0));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_0), NOT_LOCATED, compartments));
        
        // Check target location constraints
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_0), new Location("a"), compartments));
        
        expected.clear();
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_0), new Location("b"), compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_0), new Location("a", INDEX_0, INDEX_0, INDEX_0), compartments));
        
        expected.add(new Location("a", INDEX_1, INDEX_0, INDEX_1));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_0), new Location("a", INDEX_1, INDEX_0, INDEX_1), compartments));
    }

    @Test
    public void testApplyChannel_radialIn3d_intracompartment() {
        Location location = new Location("a");
        ChannelComponent component = new RadialInComponent(getList(location), getList(location));
        List<Compartment> compartments = getList(new Compartment("a", 5, 5, 5), new Compartment("b", 4, 4, 4));
        
        List<Location> expected = new ArrayList<Location>();

        // Invalid input locations
        assertEquals(expected, component.applyChannel(new Location("b", INDEX_1, INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a"), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_X), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_0, new CellIndexExpression("5")), NOT_LOCATED, compartments));
        
        // Central voxel
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_2, INDEX_2, INDEX_2), NOT_LOCATED, compartments));

        // Midway voxel
        expected.clear();
        expected.add(new Location("a", INDEX_2, INDEX_2, INDEX_2));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        
        // Outer voxel
        expected.clear();
        expected.add(new Location("a", INDEX_1, INDEX_1, INDEX_1));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_0), NOT_LOCATED, compartments));
        
        // Check target location constraints
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_0), new Location("a"), compartments));
        
        expected.clear();
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_0), new Location("b"), compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_0), new Location("a", INDEX_0, INDEX_0, INDEX_0), compartments));
        
        expected.add(new Location("a", INDEX_1, INDEX_1, INDEX_1));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_0), new Location("a", INDEX_1, INDEX_1, INDEX_1), compartments));
    }


    @Test
    public void testApplyChannel_radialOut3d_intracompartment() {
        Location location = new Location("a");
        ChannelComponent component = new RadialOutComponent(getList(location), getList(location));
        List<Compartment> compartments = getList(new Compartment("a", 5, 5, 5), new Compartment("b", 4, 4, 4));
        
        List<Location> expected = new ArrayList<Location>();

        // Invalid input locations
        assertEquals(expected, component.applyChannel(new Location("b", INDEX_1, INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a"), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_X), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_0, new CellIndexExpression("5")), NOT_LOCATED, compartments));
        
        // Central voxel
        expected.add(new Location("a", INDEX_1, INDEX_1, INDEX_1));
        expected.add(new Location("a", INDEX_1, INDEX_1, INDEX_2));
        expected.add(new Location("a", INDEX_1, INDEX_1, INDEX_3));
        expected.add(new Location("a", INDEX_1, INDEX_2, INDEX_1));
        expected.add(new Location("a", INDEX_1, INDEX_2, INDEX_2));
        expected.add(new Location("a", INDEX_1, INDEX_2, INDEX_3));
        expected.add(new Location("a", INDEX_1, INDEX_3, INDEX_1));
        expected.add(new Location("a", INDEX_1, INDEX_3, INDEX_2));
        expected.add(new Location("a", INDEX_1, INDEX_3, INDEX_3));
        expected.add(new Location("a", INDEX_2, INDEX_1, INDEX_1));
        expected.add(new Location("a", INDEX_2, INDEX_1, INDEX_2));
        expected.add(new Location("a", INDEX_2, INDEX_1, INDEX_3));
        expected.add(new Location("a", INDEX_2, INDEX_2, INDEX_1));
        expected.add(new Location("a", INDEX_2, INDEX_2, INDEX_3));
        expected.add(new Location("a", INDEX_2, INDEX_3, INDEX_1));
        expected.add(new Location("a", INDEX_2, INDEX_3, INDEX_2));
        expected.add(new Location("a", INDEX_2, INDEX_3, INDEX_3));
        expected.add(new Location("a", INDEX_3, INDEX_1, INDEX_1));
        expected.add(new Location("a", INDEX_3, INDEX_1, INDEX_2));
        expected.add(new Location("a", INDEX_3, INDEX_1, INDEX_3));
        expected.add(new Location("a", INDEX_3, INDEX_2, INDEX_1));
        expected.add(new Location("a", INDEX_3, INDEX_2, INDEX_2));
        expected.add(new Location("a", INDEX_3, INDEX_2, INDEX_3));
        expected.add(new Location("a", INDEX_3, INDEX_3, INDEX_1));
        expected.add(new Location("a", INDEX_3, INDEX_3, INDEX_2));
        expected.add(new Location("a", INDEX_3, INDEX_3, INDEX_3));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_2, INDEX_2, INDEX_2), NOT_LOCATED, compartments));
        
        // Check target location constraints
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_2, INDEX_2, INDEX_2), new Location("a"), compartments));
        
        expected.clear();
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_2, INDEX_2, INDEX_2), new Location("b"), compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_2, INDEX_2, INDEX_2), new Location("a", INDEX_0, INDEX_0, INDEX_0), compartments));
        
        expected.add(new Location("a", INDEX_1, INDEX_1, INDEX_1));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_2, INDEX_2, INDEX_2), new Location("a", INDEX_1, INDEX_1, INDEX_1), compartments));

        // Midway voxel
        expected.clear();
        expected.add(new Location("a", INDEX_0, INDEX_0, INDEX_0));
        expected.add(new Location("a", INDEX_0, INDEX_0, INDEX_1));
        expected.add(new Location("a", INDEX_0, INDEX_1, INDEX_0));
        expected.add(new Location("a", INDEX_0, INDEX_1, INDEX_1));
        expected.add(new Location("a", INDEX_1, INDEX_0, INDEX_0));
        expected.add(new Location("a", INDEX_1, INDEX_0, INDEX_1));
        expected.add(new Location("a", INDEX_1, INDEX_1, INDEX_0));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        
        // Outer voxel
        expected.clear();
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_0), NOT_LOCATED, compartments));
    }

    @Test
    public void testApplyChannel_radial3d_intracompartment() {
        Location location = new Location("a");
        ChannelComponent component = new RadialComponent(getList(location), getList(location));
        List<Compartment> compartments = getList(new Compartment("a", 5, 5, 5), new Compartment("b", 4, 4, 4));
        
        List<Location> expected = new ArrayList<Location>();

        // Invalid input locations
        assertEquals(expected, component.applyChannel(new Location("b", INDEX_1, INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a"), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_X), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_0, new CellIndexExpression("5")), NOT_LOCATED, compartments));
        
        // Central voxel
        expected.add(new Location("a", INDEX_1, INDEX_1, INDEX_1));
        expected.add(new Location("a", INDEX_1, INDEX_1, INDEX_2));
        expected.add(new Location("a", INDEX_1, INDEX_1, INDEX_3));
        expected.add(new Location("a", INDEX_1, INDEX_2, INDEX_1));
        expected.add(new Location("a", INDEX_1, INDEX_2, INDEX_2));
        expected.add(new Location("a", INDEX_1, INDEX_2, INDEX_3));
        expected.add(new Location("a", INDEX_1, INDEX_3, INDEX_1));
        expected.add(new Location("a", INDEX_1, INDEX_3, INDEX_2));
        expected.add(new Location("a", INDEX_1, INDEX_3, INDEX_3));
        expected.add(new Location("a", INDEX_2, INDEX_1, INDEX_1));
        expected.add(new Location("a", INDEX_2, INDEX_1, INDEX_2));
        expected.add(new Location("a", INDEX_2, INDEX_1, INDEX_3));
        expected.add(new Location("a", INDEX_2, INDEX_2, INDEX_1));
        expected.add(new Location("a", INDEX_2, INDEX_2, INDEX_3));
        expected.add(new Location("a", INDEX_2, INDEX_3, INDEX_1));
        expected.add(new Location("a", INDEX_2, INDEX_3, INDEX_2));
        expected.add(new Location("a", INDEX_2, INDEX_3, INDEX_3));
        expected.add(new Location("a", INDEX_3, INDEX_1, INDEX_1));
        expected.add(new Location("a", INDEX_3, INDEX_1, INDEX_2));
        expected.add(new Location("a", INDEX_3, INDEX_1, INDEX_3));
        expected.add(new Location("a", INDEX_3, INDEX_2, INDEX_1));
        expected.add(new Location("a", INDEX_3, INDEX_2, INDEX_2));
        expected.add(new Location("a", INDEX_3, INDEX_2, INDEX_3));
        expected.add(new Location("a", INDEX_3, INDEX_3, INDEX_1));
        expected.add(new Location("a", INDEX_3, INDEX_3, INDEX_2));
        expected.add(new Location("a", INDEX_3, INDEX_3, INDEX_3));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_2, INDEX_2, INDEX_2), NOT_LOCATED, compartments));
        
        // Check target location constraints
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_2, INDEX_2, INDEX_2), new Location("a"), compartments));
        
        expected.clear();
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_2, INDEX_2, INDEX_2), new Location("b"), compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_2, INDEX_2, INDEX_2), new Location("a", INDEX_0, INDEX_0, INDEX_0), compartments));
        
        expected.add(new Location("a", INDEX_1, INDEX_1, INDEX_1));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_2, INDEX_2, INDEX_2), new Location("a", INDEX_1, INDEX_1, INDEX_1), compartments));

        // Midway voxel
        expected.clear();
        expected.add(new Location("a", INDEX_0, INDEX_0, INDEX_0));
        expected.add(new Location("a", INDEX_0, INDEX_0, INDEX_1));
        expected.add(new Location("a", INDEX_0, INDEX_1, INDEX_0));
        expected.add(new Location("a", INDEX_0, INDEX_1, INDEX_1));
        expected.add(new Location("a", INDEX_1, INDEX_0, INDEX_0));
        expected.add(new Location("a", INDEX_1, INDEX_0, INDEX_1));
        expected.add(new Location("a", INDEX_1, INDEX_1, INDEX_0));
        expected.add(new Location("a", INDEX_2, INDEX_2, INDEX_2));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        
        // Outer voxel
        expected.clear();
        expected.add(new Location("a", INDEX_1, INDEX_1, INDEX_1));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_0), NOT_LOCATED, compartments));
    }

    @Test
    public void testApplyChannel_radialIn2d_intracompartment() {
        Location location = new Location("a");
        ChannelComponent component = new RadialInComponent(getList(location), getList(location));
        List<Compartment> compartments = getList(new Compartment("a", 5, 5), new Compartment("b", 4, 4));
        
        List<Location> expected = new ArrayList<Location>();

        // Invalid input locations
        assertEquals(expected, component.applyChannel(new Location("b", INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a"), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_X), NOT_LOCATED, compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_0, new CellIndexExpression("54")), NOT_LOCATED, compartments));
        
        // Central voxel
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_2, INDEX_2), NOT_LOCATED, compartments));

        // Midway voxel
        expected.clear();
        expected.add(new Location("a", INDEX_2, INDEX_2));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_1), NOT_LOCATED, compartments));
        
        // Outer voxel
        expected.clear();
        expected.add(new Location("a", INDEX_1, INDEX_1));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_0), NOT_LOCATED, compartments));
        
        // Check target location constraints
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_0), new Location("a"), compartments));
        
        expected.clear();
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_0), new Location("b"), compartments));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_0), new Location("a", INDEX_0, INDEX_0), compartments));
        
        expected.add(new Location("a", INDEX_1, INDEX_1));
        assertEquals(expected, component.applyChannel(new Location("a", INDEX_1, INDEX_0), new Location("a", INDEX_1, INDEX_1), compartments));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testApplyChannel_multipleCompartments() {
        List<Compartment> compartments = getList(new Compartment("a", 4), new Compartment("b", 3));
        ChannelComponent component = new ChannelComponent(null, getList(new Location("a", INDEX_X), new Location("b", INDEX_X)), 
                getList(new Location("a", INDEX_X_PLUS_1), new Location("b", INDEX_X_PLUS_1)));
        
        // No match
        List<List<Location>> results = component.applyChannel(getList(
                new ChannelConstraint(new Location("a", INDEX_1), NOT_LOCATED)), compartments);
        List<List<Location>> expected = new ArrayList<List<Location>>();
        assertEquals(expected, results);
        
        results = component.applyChannel(getList(
                new ChannelConstraint(new Location("a", INDEX_1), NOT_LOCATED),
                new ChannelConstraint(new Location("b", INDEX_2), NOT_LOCATED)), compartments);
        assertEquals(expected, results);
        
        results = component.applyChannel(getList(
                new ChannelConstraint(new Location("a", INDEX_1), NOT_LOCATED),
                new ChannelConstraint(new Location("a", INDEX_1), NOT_LOCATED)), compartments);
        assertEquals(expected, results);
        
        // No match - geometry mismatch
        results = component.applyChannel(getList(
                new ChannelConstraint(new Location("a", INDEX_2), NOT_LOCATED),
                new ChannelConstraint(new Location("b", INDEX_2), NOT_LOCATED)), compartments);
        assertEquals(expected, results);
        
        // Variable linked
        results = component.applyChannel(getList(
                new ChannelConstraint(new Location("a", INDEX_1), NOT_LOCATED),
                new ChannelConstraint(new Location("b", INDEX_1), NOT_LOCATED)), compartments);
        expected = getList(getList(new Location("a", INDEX_2), new Location("b", INDEX_2)));
        assertEquals(expected, results);

        
        // Variable independent
        component = new ChannelComponent(null, getList(new Location("a", INDEX_X), new Location("b", INDEX_Y)), 
                getList(new Location("a", INDEX_X_PLUS_1), new Location("b", INDEX_Y_PLUS_1)));
        
        results = component.applyChannel(getList(
                new ChannelConstraint(new Location("a", INDEX_1), NOT_LOCATED),
                new ChannelConstraint(new Location("b", INDEX_1), NOT_LOCATED)), compartments);
        expected = getList(getList(new Location("a", INDEX_2), new Location("b", INDEX_2)));
        assertEquals(expected, results);

        results = component.applyChannel(getList(
                new ChannelConstraint(new Location("a", INDEX_1), NOT_LOCATED),
                new ChannelConstraint(new Location("b", INDEX_0), NOT_LOCATED)), compartments);
        expected = getList(getList(new Location("a", INDEX_2), new Location("b", INDEX_1)));
        assertEquals(expected, results);

        
        // Swap locations - allow source reordering
        component = new ChannelComponent(null, getList(new Location("a", INDEX_X), new Location("b", INDEX_Y)), 
                getList(new Location("b", INDEX_X), new Location("a", INDEX_Y)));
        
        results = component.applyChannel(getList(
                new ChannelConstraint(new Location("a", INDEX_1), NOT_LOCATED),
                new ChannelConstraint(new Location("b", INDEX_0), NOT_LOCATED)), compartments);
        expected = getList(getList(new Location("b", INDEX_1), new Location("a", INDEX_0)));
        assertEquals(expected, results);

        results = component.applyChannel(getList(
                new ChannelConstraint(new Location("b", INDEX_1), NOT_LOCATED),
                new ChannelConstraint(new Location("a", INDEX_0), NOT_LOCATED)), compartments);
        expected = getList(getList(new Location("a", INDEX_1), new Location("b", INDEX_0)));
        assertEquals(expected, results);
        
        // Target invalid - no results
        results = component.applyChannel(getList(
                new ChannelConstraint(new Location("a", new CellIndexExpression("3")), NOT_LOCATED),
                new ChannelConstraint(new Location("b", INDEX_0), NOT_LOCATED)), compartments);
        expected.clear();
        assertEquals(expected, results);
                
        // Target constraints
        component = new ChannelComponent(null, getList(new Location("a", INDEX_X), new Location("b", INDEX_Y)), 
                getList(new Location("a", INDEX_X_PLUS_1), new Location("b", INDEX_Y_PLUS_1)));

        results = component.applyChannel(getList(
                new ChannelConstraint(new Location("a", INDEX_1), new Location("a")),
                new ChannelConstraint(new Location("b", INDEX_1), new Location("b", INDEX_2))), compartments);
        expected = getList(getList(new Location("a", INDEX_2), new Location("b", INDEX_2)));
        assertEquals(expected, results);

        results = component.applyChannel(getList(
                new ChannelConstraint(new Location("a", INDEX_1), new Location("a")),
                new ChannelConstraint(new Location("b", INDEX_2), new Location("b", INDEX_2))), compartments);
        expected.clear();
        assertEquals(expected, results);

        results = component.applyChannel(getList(
                new ChannelConstraint(new Location("a", INDEX_1), new Location("a", INDEX_2)),
                new ChannelConstraint(new Location("b", INDEX_1), NOT_LOCATED)), compartments);
        expected = getList(getList(new Location("a", INDEX_2), new Location("b", INDEX_2)));
        assertEquals(expected, results);

        results = component.applyChannel(getList(
                new ChannelConstraint(new Location("a", INDEX_2), new Location("a", INDEX_2)),
                new ChannelConstraint(new Location("b", INDEX_2), NOT_LOCATED)), compartments);
        expected.clear();
        assertEquals(expected, results);

        // TODO channel type tests
    }
    
    // TODO check target locations are unique set
    
    @Test
    public void testIsValidSourceLocations() {
        List<Compartment> compartments = getList(new Compartment("a", 4), new Compartment("b", 2));
        ChannelComponent component = new ChannelComponent(getList(new Location("a", INDEX_X)),
                getList(new Location("c", INDEX_X_PLUS_1)));

        assertTrue(component.isValidSourceLocations(getList(new ChannelConstraint(new Location("a", INDEX_X), NOT_LOCATED)),
                getList(new ChannelConstraint(new Location("a", INDEX_1), NOT_LOCATED)), compartments));

        assertFalse(component.isValidSourceLocations(
                getList(new ChannelConstraint(new Location("a", INDEX_X), NOT_LOCATED),
                        FIXED_CONSTRAINT),
                getList(new ChannelConstraint(new Location("a", INDEX_1), NOT_LOCATED), 
                        new ChannelConstraint(new Location("a", INDEX_2), NOT_LOCATED)), compartments));

        assertFalse(component.isValidSourceLocations(
                getList(new ChannelConstraint(new Location("a", INDEX_X), NOT_LOCATED),
                        FIXED_CONSTRAINT),
                getList(new ChannelConstraint(new Location("a", INDEX_1), NOT_LOCATED), 
                        new ChannelConstraint(new Location("a", INDEX_2), new Location("a"))), compartments));

        assertFalse(component.isValidSourceLocations(
                getList(new ChannelConstraint(new Location("a", INDEX_X), NOT_LOCATED),
                        FIXED_CONSTRAINT),
                getList(new ChannelConstraint(new Location("a", INDEX_1), NOT_LOCATED), 
                        new ChannelConstraint(new Location("a", INDEX_2), new Location("a", INDEX_1))), compartments));

        assertTrue(component.isValidSourceLocations(
                getList(new ChannelConstraint(new Location("a", INDEX_X), NOT_LOCATED),
                        FIXED_CONSTRAINT),
                getList(new ChannelConstraint(new Location("a", INDEX_1), NOT_LOCATED), 
                        new ChannelConstraint(new Location("a", INDEX_2), new Location("a", INDEX_2))), compartments));

        assertTrue(component.isValidSourceLocations(
                getList(new ChannelConstraint(new Location("b", INDEX_0), new Location("b", INDEX_1)),
                        FIXED_CONSTRAINT),
                getList(new ChannelConstraint(new Location("b", INDEX_0), new Location("b", INDEX_1)), 
                        new ChannelConstraint(new Location("b", INDEX_0), FIXED_LOCATION)), compartments));
    }
    
    @Test
    public void testIsValidSourceLocation() {
        List<Compartment> compartments = getList(new Compartment("a", 4), new Compartment("b", 2));
        ChannelComponent component = new ChannelComponent(getList(new Location("a", INDEX_X)), getList(new Location("c", INDEX_X_PLUS_1)));

        assertTrue(component.isValidSourceLocation(new Location("a", INDEX_X), new Location("a", INDEX_1), compartments));
    }
    
    
    @Test
    public void testPermuteChannelConstraints() {
        ChannelComponent component = new ChannelComponent(
                getList(new Location("a", INDEX_X)), getList(new Location("c", INDEX_X_PLUS_1)));

        // Single element
        List<ChannelConstraint> constraints = getList(new ChannelConstraint(new Location("a"), new Location("b")));
        List<List<ChannelConstraint>> constraintPermutations = component.permuteChannelConstraints(constraints, 0);
        List<List<ChannelConstraint>> expectedPermutations = new ArrayList<List<ChannelConstraint>>();
        assertEquals(expectedPermutations, constraintPermutations);
        
        constraintPermutations = component.permuteChannelConstraints(constraints, 1);
        expectedPermutations = new ArrayList<List<ChannelConstraint>>();
        expectedPermutations.add(getList(new ChannelConstraint(new Location("a"), new Location("b"))));
        assertEquals(expectedPermutations, constraintPermutations);
        
        constraintPermutations = component.permuteChannelConstraints(constraints, 3);
        expectedPermutations = new ArrayList<List<ChannelConstraint>>();
        expectedPermutations.add(getList(
                new ChannelConstraint(new Location("a"), new Location("b")),
                FIXED_CONSTRAINT,
                FIXED_CONSTRAINT));
        expectedPermutations.add(getList(
                FIXED_CONSTRAINT,
                new ChannelConstraint(new Location("a"), new Location("b")),
                FIXED_CONSTRAINT));
        expectedPermutations.add(getList(
                FIXED_CONSTRAINT,
                FIXED_CONSTRAINT,
                new ChannelConstraint(new Location("a"), new Location("b"))));
        assertEquals(expectedPermutations, constraintPermutations);
        

        // 3 elements
        constraints = getList(new ChannelConstraint(new Location("a"), new Location("d")),
                new ChannelConstraint(new Location("b"), new Location("e")),
                new ChannelConstraint(new Location("c"), new Location("f")));
        constraintPermutations = component.permuteChannelConstraints(constraints, 2);
        expectedPermutations.clear();
        assertEquals(expectedPermutations, constraintPermutations);
        
        constraints = getList(new ChannelConstraint(new Location("a"), new Location("d")),
                new ChannelConstraint(new Location("b"), new Location("e")),
                new ChannelConstraint(new Location("c"), new Location("f")));
        constraintPermutations = component.permuteChannelConstraints(constraints, 3);
        
        expectedPermutations.clear();
        expectedPermutations.add(getList(
                new ChannelConstraint(new Location("a"), new Location("d")),
                new ChannelConstraint(new Location("b"), new Location("e")),
                new ChannelConstraint(new Location("c"), new Location("f"))));
        expectedPermutations.add(getList(
                new ChannelConstraint(new Location("a"), new Location("d")),
                new ChannelConstraint(new Location("c"), new Location("f")),
                new ChannelConstraint(new Location("b"), new Location("e"))));
        expectedPermutations.add(getList(
                new ChannelConstraint(new Location("b"), new Location("e")),
                new ChannelConstraint(new Location("a"), new Location("d")),
                new ChannelConstraint(new Location("c"), new Location("f"))));
        expectedPermutations.add(getList(
                new ChannelConstraint(new Location("b"), new Location("e")),
                new ChannelConstraint(new Location("c"), new Location("f")),
                new ChannelConstraint(new Location("a"), new Location("d"))));
        expectedPermutations.add(getList(
                new ChannelConstraint(new Location("c"), new Location("f")),
                new ChannelConstraint(new Location("a"), new Location("d")),
                new ChannelConstraint(new Location("b"), new Location("e"))));
        expectedPermutations.add(getList(
                new ChannelConstraint(new Location("c"), new Location("f")),
                new ChannelConstraint(new Location("b"), new Location("e")),
                new ChannelConstraint(new Location("a"), new Location("d"))));
        
        assertEquals(expectedPermutations, constraintPermutations);
        
    }
    
    @Test
    public void testGetChannelSubcomponents_edgeNeighbour() {
        List<Compartment> compartments = getList(new Compartment("a", 4, 4));
        
        Location location = new Location("a");

        EdgeNeighbourComponent component = new EdgeNeighbourComponent(getList(location), getList(location));
        
        List<ChannelComponent> expected = getList(
                new ChannelComponent(ChannelComponent.SUBCOMPONENT, getList(new Location("a", INDEX_X, INDEX_Y)), getList(new Location("a", INDEX_X_MINUS_1, INDEX_Y))),
                new ChannelComponent(ChannelComponent.SUBCOMPONENT, getList(new Location("a", INDEX_X, INDEX_Y)), getList(new Location("a", INDEX_X_PLUS_1, INDEX_Y))),
                new ChannelComponent(ChannelComponent.SUBCOMPONENT, getList(new Location("a", INDEX_X, INDEX_Y)), getList(new Location("a", INDEX_X, INDEX_Y_MINUS_1))),
                new ChannelComponent(ChannelComponent.SUBCOMPONENT, getList(new Location("a", INDEX_X, INDEX_Y)), getList(new Location("a", INDEX_X, INDEX_Y_PLUS_1))));

        assertEquals(expected, component.getChannelSubcomponents(compartments));
    }
    
    @Test
    public void testCreateChannelComponent() {
        Location location1 = new Location("a");
        Location location2 = new Location("b");
        
        try {
            ChannelComponent.createChannelComponent(null, getList(location1), null);
            fail("mismatched locations list should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            ChannelComponent.createChannelComponent(null, null, getList(location1));
            fail("mismatched locations list should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            ChannelComponent.createChannelComponent(null, getList(location1, location1), getList(location2));
            fail("mismatched locations list should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            ChannelComponent.createChannelComponent("Unknown", getList(location1), getList(location2));
            fail("mismatched locations list should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        ChannelComponent component = ChannelComponent.createChannelComponent(null, getList(location1), getList(location2));
        assertEquals("[[a -> b]]", component.toString());

        component = ChannelComponent.createChannelComponent(null, getList(location1), getList(location1));
        assertEquals("[[a -> a]]", component.toString());
        
        // Multi compartment channels
        component = ChannelComponent.createChannelComponent(null, getList(location2, location1), getList(location1, location2));
        assertEquals("[[b -> a], [a -> b]]", component.toString());
    }
    
    @Test
    public void testCreateChannelComponent_channelTypes() {
        checkCreateChannelComponent("Neighbour");
        checkCreateChannelComponent("EdgeNeighbour");
        checkCreateChannelComponent("FaceNeighbour");
        checkCreateChannelComponent("Hexagonal");
        checkCreateChannelComponent("Radial");
        checkCreateChannelComponent("RadialOut");
        checkCreateChannelComponent("RadialIn");
        checkCreateChannelComponent("Lateral");
    }
    
    private void checkCreateChannelComponent(String channelType) {
        Location location1 = new Location("a");
        Location location2 = new Location("b");
        Location location3 = new Location("a", INDEX_0);
        
        try {
            ChannelComponent.createChannelComponent(channelType, getList(location1), null);
            fail("mismatched locations list should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            ChannelComponent.createChannelComponent(channelType, null, getList(location1));
            fail("mismatched locations list should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            ChannelComponent.createChannelComponent(channelType, getList(location1, location1), getList(location2));
            fail("mismatched locations list should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            ChannelComponent.createChannelComponent(channelType, getList(location3), getList(location2));
            fail("non compartment locations should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        ChannelComponent component = ChannelComponent.createChannelComponent(channelType, getList(location1), getList(location2));
        assertEquals("(" + channelType + ") [[a -> b]]", component.toString());

        component = ChannelComponent.createChannelComponent(channelType, getList(location1), getList(location1));
        assertEquals("(" + channelType + ") [[a -> a]]", component.toString());
        
        // Multi compartment channels
        component = ChannelComponent.createChannelComponent(channelType, getList(location2, location1), getList(location1, location2));
        assertEquals("(" + channelType + ") [[b -> a], [a -> b]]", component.toString());
    }
    
    @Test
    public void testValidate() {
        List<Compartment> compartments = Utils.getList(new Compartment("known"));
        
        ChannelComponent component = new ChannelComponent(new Location("known"), new Location("unknown"));

        try {
            component.validate(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        
        // Missing compartment link reference
        try {
            component.validate(compartments);
            fail("validation should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
            assertEquals("Compartment 'unknown' not found", ex.getMessage());
        }
        
        component = new ChannelComponent(new Location("unknown"), new Location("known"));
        try {
            component.validate(compartments);
            fail("validation should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
            assertEquals("Compartment 'unknown' not found", ex.getMessage());
        }
        
        component = new ChannelComponent(new Location("known"), new Location("known"));
        component.validate(compartments);

        component = new ChannelComponent(new Location("known", INDEX_0), new Location("known"));
        try {
            component.validate(compartments);
            fail("validation should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
            assertEquals("Not a valid voxel for compartment 'known'", ex.getMessage());
        }
        
        component = new ChannelComponent(new Location("known"), new Location("known", INDEX_0));
        try {
            component.validate(compartments);
            fail("validation should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
            assertEquals("Not a valid voxel for compartment 'known'", ex.getMessage());
        }
        

        // TODO compartment link range out of bounds ?
        
        // TODO channel type validation
        
    }

    
    @Test
    public void testValidate_nestedCompartments_predefinedChannels() {
        
        Compartment other = new Compartment("other"); // Just to remove compile error

        // Same compartment - should not be nested
        PredefinedChannelComponent component = new NeighbourComponent(getList(new Location("inner")), getList(new Location("inner")));
        component.validate(getList(new Compartment("inner", 2, 3)));
        
        
        component = new NeighbourComponent(getList(new Location("inner")), getList(new Location("outer")));
        
        // 2D rectangles
        component.validate(getList(new Compartment("inner", 2, 3), new OpenRectangle("outer", 4, 5, 1)));
        component.validate(getList(new OpenRectangle("inner", 4, 5, 1), new OpenRectangle("outer", 8, 9, 2), other));
        
        checkValidateFail_nesting(component, getList(new Compartment("inner", 2, 4), new OpenRectangle("outer", 4, 5, 1)));
        checkValidateFail_nesting(component, getList(new Compartment("inner", 2, 2), new OpenRectangle("outer", 4, 5, 1)));
        checkValidateFail_nesting(component, getList(new Compartment("inner", 1, 3), new OpenRectangle("outer", 4, 5, 1)));
        checkValidateFail_nesting(component, getList(new Compartment("inner", 3, 3), new OpenRectangle("outer", 4, 5, 1)));
        checkValidateFail_nesting(component, getList(new OpenRectangle("inner", 4, 6, 1), new OpenRectangle("outer", 8, 9, 2), other));
        checkValidateFail_nesting(component, getList(new OpenRectangle("inner", 4, 4, 1), new OpenRectangle("outer", 8, 9, 2), other));
        checkValidateFail_nesting(component, getList(new OpenRectangle("inner", 5, 5, 1), new OpenRectangle("outer", 8, 9, 2), other));
        checkValidateFail_nesting(component, getList(new OpenRectangle("inner", 3, 5, 1), new OpenRectangle("outer", 8, 9, 2), other));
        
        // 2D circles
        component.validate(getList(new SolidCircle("inner", 5), new OpenCircle("outer", 7, 1)));
        component.validate(getList(new OpenCircle("inner", 5, 2), new OpenCircle("outer", 7, 1), other));

        checkValidateFail_nesting(component, getList(new SolidCircle("inner", 6), new OpenCircle("outer", 7, 1)));
        checkValidateFail_nesting(component, getList(new SolidCircle("inner", 4), new OpenCircle("outer", 7, 1)));
        checkValidateFail_nesting(component, getList(new OpenCircle("inner", 6, 2), new OpenCircle("outer", 7, 1), other));
        checkValidateFail_nesting(component, getList(new OpenCircle("inner", 4, 2), new OpenCircle("outer", 7, 1), other));

        // 3D cuboids
        component.validate(getList(new Compartment("inner", 2, 3, 4), new OpenCuboid("outer", 4, 5, 6, 1)));
        component.validate(getList(new OpenCuboid("inner", 4, 5, 6, 1), new OpenCuboid("outer", 8, 9, 10, 2), other));
        
        checkValidateFail_nesting(component, getList(new Compartment("inner", 2, 3, 5), new OpenCuboid("outer", 4, 5, 6, 1)));
        checkValidateFail_nesting(component, getList(new Compartment("inner", 2, 3, 3), new OpenCuboid("outer", 4, 5, 6, 1)));
        checkValidateFail_nesting(component, getList(new Compartment("inner", 2, 4, 4), new OpenCuboid("outer", 4, 5, 6, 1)));
        checkValidateFail_nesting(component, getList(new Compartment("inner", 2, 2, 4), new OpenCuboid("outer", 4, 5, 6, 1)));
        checkValidateFail_nesting(component, getList(new Compartment("inner", 3, 3, 4), new OpenCuboid("outer", 4, 5, 6, 1)));
        checkValidateFail_nesting(component, getList(new Compartment("inner", 1, 3, 4), new OpenCuboid("outer", 4, 5, 6, 1)));

        checkValidateFail_nesting(component, getList(new OpenCuboid("inner", 4, 5, 7, 1), new OpenCuboid("outer", 8, 9, 10, 2), other));
        checkValidateFail_nesting(component, getList(new OpenCuboid("inner", 4, 5, 5, 1), new OpenCuboid("outer", 8, 9, 10, 2), other));
        checkValidateFail_nesting(component, getList(new OpenCuboid("inner", 4, 6, 6, 1), new OpenCuboid("outer", 8, 9, 10, 2), other));
        checkValidateFail_nesting(component, getList(new OpenCuboid("inner", 4, 4, 6, 1), new OpenCuboid("outer", 8, 9, 10, 2), other));
        checkValidateFail_nesting(component, getList(new OpenCuboid("inner", 5, 5, 6, 1), new OpenCuboid("outer", 8, 9, 10, 2), other));
        checkValidateFail_nesting(component, getList(new OpenCuboid("inner", 3, 5, 6, 1), new OpenCuboid("outer", 8, 9, 10, 2), other));
        
        // 3D spheres
        component.validate(getList(new SolidSphere("inner", 5), new OpenSphere("outer", 7, 1)));
        component.validate(getList(new OpenSphere("inner", 5, 2), new OpenSphere("outer", 7, 1), other));

        checkValidateFail_nesting(component, getList(new SolidSphere("inner", 6), new OpenSphere("outer", 7, 1)));
        checkValidateFail_nesting(component, getList(new SolidSphere("inner", 4), new OpenSphere("outer", 7, 1)));
        checkValidateFail_nesting(component, getList(new OpenSphere("inner", 6, 2), new OpenSphere("outer", 7, 1), other));
        checkValidateFail_nesting(component, getList(new OpenSphere("inner", 4, 2), new OpenSphere("outer", 7, 1), other));
        
        // 3D cylinders
        component.validate(getList(new SolidCylinder("inner", 2, 3), new OpenCylinder("outer", 4, 5, 1)));
        component.validate(getList(new OpenCylinder("inner", 4, 5, 1), new OpenCylinder("outer", 8, 9, 2), other));
        
        checkValidateFail_nesting(component, getList(new SolidCylinder("inner", 2, 4), new OpenCylinder("outer", 4, 5, 1)));
        checkValidateFail_nesting(component, getList(new SolidCylinder("inner", 2, 2), new OpenCylinder("outer", 4, 5, 1)));
        checkValidateFail_nesting(component, getList(new SolidCylinder("inner", 1, 3), new OpenCylinder("outer", 4, 5, 1)));
        checkValidateFail_nesting(component, getList(new SolidCylinder("inner", 3, 3), new OpenCylinder("outer", 4, 5, 1)));
        checkValidateFail_nesting(component, getList(new OpenCylinder("inner", 4, 6, 1), new OpenCylinder("outer", 8, 9, 2), other));
        checkValidateFail_nesting(component, getList(new OpenCylinder("inner", 4, 4, 1), new OpenCylinder("outer", 8, 9, 2), other));
        checkValidateFail_nesting(component, getList(new OpenCylinder("inner", 5, 5, 1), new OpenCylinder("outer", 8, 9, 2), other));
        checkValidateFail_nesting(component, getList(new OpenCylinder("inner", 3, 5, 1), new OpenCylinder("outer", 8, 9, 2), other));
        
        // Shape mismatch
        checkValidateFail_nesting(component, getList(new Compartment("inner", 2, 3, 5), new OpenSphere("outer", 4, 1)));
        checkValidateFail_nesting(component, getList(new Compartment("inner", 2, 3, 5), new Compartment("outer", 4, 4, 1)));
    }



    private void checkValidateFail_nesting(ChannelComponent component, List<Compartment> compartments) {
        try {
            component.validate(compartments);
            fail("validation should have failed");
        }
        catch (IllegalStateException ex) {
            // Expected exception
            assertTrue(ex.getMessage().startsWith("Compartments not compatible for nesting: '"));
        }
    }

}
