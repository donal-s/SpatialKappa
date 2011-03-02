package org.demonsoft.spatialkappa.tools;

import static org.demonsoft.spatialkappa.model.TestUtils.getList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.demonsoft.spatialkappa.model.Agent;
import org.demonsoft.spatialkappa.model.KappaModel;
import org.demonsoft.spatialkappa.model.LocatedTransform;
import org.demonsoft.spatialkappa.model.Perturbation;
import org.demonsoft.spatialkappa.model.Transform;
import org.demonsoft.spatialkappa.model.TransformTest;
import org.demonsoft.spatialkappa.model.Perturbation.Assignment;
import org.demonsoft.spatialkappa.model.Perturbation.ConcentrationExpression;
import org.demonsoft.spatialkappa.model.Perturbation.Condition;
import org.demonsoft.spatialkappa.model.Perturbation.Inequality;
import org.demonsoft.spatialkappa.tools.ComplexMatchingSimulation;
import org.junit.Test;

public class PerturbationTest {

    @Test
    public void testConstructorTime() {
        Assignment assignment = new Assignment("transform", new ConcentrationExpression(2f));
        
        try {
            new Perturbation((String) null, assignment);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new Perturbation("15", null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new Perturbation("", assignment);
            fail("invalid should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        try {
            new Perturbation("invalid", assignment);
            fail("invalid should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        try {
            new Perturbation("-15", assignment);
            fail("invalid should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        Perturbation perturbation = new Perturbation("15", assignment);
        assertEquals(15f, perturbation.getTime(), 0.01f);
        assertSame(assignment, perturbation.getAssignment());
        assertNull(perturbation.getCondition());
    }

    @Test
    public void testConstructorConcentration() {
        ConcentrationExpression expression = new ConcentrationExpression(2f);
        Assignment assignment = new Assignment("transform", expression);
        Condition condition = new Condition(expression, Inequality.GREATER_THAN, expression);
        
        try {
            new Perturbation((Condition) null, assignment);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        try {
            new Perturbation(condition, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        Perturbation perturbation = new Perturbation(condition, assignment);
        assertSame(condition, perturbation.getCondition());
        assertSame(assignment, perturbation.getAssignment());
        assertEquals(0f, perturbation.getTime(), 0.01f);
    }

    @Test
    public void testIsConditionMet_basic() {
        Assignment assignment = new Assignment("transform", new ConcentrationExpression(2f));
        Condition condition = new Condition(new ConcentrationExpression("label"), Inequality.GREATER_THAN, new ConcentrationExpression(0.5f));
        Perturbation perturbation = new Perturbation(condition, assignment);
        
        try {
            perturbation.isConditionMet(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        KappaModel kappaModel = new KappaModel();
        ComplexMatchingSimulation simulation = new ComplexMatchingSimulation(kappaModel);
        // Unknown label
        assertFalse(perturbation.isConditionMet(simulation));
        
        // Time based condition
        perturbation = new Perturbation("5", assignment);
        simulation = new ComplexMatchingSimulation();
        assertFalse(perturbation.isConditionMet(simulation));
        
        simulation.setTime(6f);
        assertTrue(perturbation.isConditionMet(simulation));
    }

    @Test
    public void testIsConditionMet() {
        Assignment assignment = new Assignment("transform", new ConcentrationExpression(2f));
        Condition condition = new Condition(new ConcentrationExpression("label"), Inequality.GREATER_THAN, new ConcentrationExpression(0.5f));
        Perturbation perturbation = new Perturbation(condition, assignment);
        
        KappaModel kappaModel = new KappaModel();
        ComplexMatchingSimulation simulation = new ComplexMatchingSimulation(kappaModel);
        
        // Transform based expression
        Transform transform = new Transform("label", TransformTest.getComplexes(new Agent("A")), null, "0.6");
        kappaModel.addTransform(new LocatedTransform(transform, null));
        simulation.initialise();
        
        assertTrue(perturbation.isConditionMet(simulation));
        simulation.getTransition("label").setRate(0.4f);
        assertFalse(perturbation.isConditionMet(simulation));
        
        // Observation based expression
        condition = new Condition(new ConcentrationExpression("label"), Inequality.GREATER_THAN, new ConcentrationExpression(50f));
        perturbation = new Perturbation(condition, assignment);

        kappaModel = new KappaModel();
        simulation = new ComplexMatchingSimulation(kappaModel);
        kappaModel.addObservable(getList(new Agent("A")), "label", null, false);
        kappaModel.addInitialValue(getList(new Agent("A")), "100", null);
        simulation.initialise();
        
        assertTrue(perturbation.isConditionMet(simulation));

        kappaModel = new KappaModel();
        simulation = new ComplexMatchingSimulation(kappaModel);
        kappaModel.addObservable(getList(new Agent("A")), "label", null, false);
        kappaModel.addInitialValue(getList(new Agent("A")), "10", null);
        simulation.initialise();
        
        assertFalse(perturbation.isConditionMet(simulation));
    }

    @Test
    public void testApply() {
        
        Assignment assignment = new Assignment("transform", new ConcentrationExpression(2f));
        Condition condition = new Condition(new ConcentrationExpression(1f), Inequality.GREATER_THAN, new ConcentrationExpression(0.5f));
        Perturbation perturbation = new Perturbation(condition, assignment);
        
        try {
            perturbation.apply(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }

        KappaModel kappaModel = new KappaModel();
        ComplexMatchingSimulation simulation = new ComplexMatchingSimulation(kappaModel);
        kappaModel.addTransform(new LocatedTransform(new Transform("transform", TransformTest.getComplexes(new Agent("A")), null, "0.4"), null));
        simulation.initialise();
        
        assertTrue(perturbation.isConditionMet(simulation));
        
        perturbation.apply(simulation);
        
        assertTrue(perturbation.isConditionMet(simulation));
        assertEquals(2f, simulation.getTransition("transform").getRate(), 0.01f);

        // TODO assignments from %var & %obs
        // TODO assignments to reversible reactions
    }

}
