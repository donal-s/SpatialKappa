package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.TestUtils.getList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.demonsoft.spatialkappa.model.Agent;
import org.demonsoft.spatialkappa.model.Compartment;
import org.demonsoft.spatialkappa.model.CompartmentLink;
import org.demonsoft.spatialkappa.model.Direction;
import org.demonsoft.spatialkappa.model.KappaModel;
import org.demonsoft.spatialkappa.model.LocatedTransform;
import org.demonsoft.spatialkappa.model.Location;
import org.demonsoft.spatialkappa.model.MathExpression;
import org.demonsoft.spatialkappa.model.Transform;
import org.demonsoft.spatialkappa.model.Transport;
import org.demonsoft.spatialkappa.model.MathExpression.Operator;
import org.demonsoft.spatialkappa.model.Perturbation.ConcentrationExpression;
import org.demonsoft.spatialkappa.tools.ComplexMatchingSimulation;
import org.junit.Test;

public class ConcentrationExpressionTest {

    @Test
    public void testConstructors() {
        ConcentrationExpression otherExpression = new ConcentrationExpression(0.1f);
        
        try {
            new ConcentrationExpression(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        try {
            new ConcentrationExpression(null, "+", otherExpression);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        try {
            new ConcentrationExpression("label", null, otherExpression);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        try {
            new ConcentrationExpression("label", "+", null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        try {
            new ConcentrationExpression(0.1f, null, otherExpression);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }
        try {
            new ConcentrationExpression(0.1f, "+", null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }

        try {
            new ConcentrationExpression(0.1f, "?", otherExpression);
            fail("illegal operator should have failed");
        }
        catch (IllegalArgumentException ex) {
            // expected exception
        }

        try {
            new ConcentrationExpression("label", "?", otherExpression);
            fail("illegal operator should have failed");
        }
        catch (IllegalArgumentException ex) {
            // expected exception
        }

        ConcentrationExpression expression = new ConcentrationExpression(0.1f);
        assertEquals(0.1f, expression.getValue(), 0.01f);
        assertNull(expression.getLabel());
        assertNull(expression.getOperator());
        assertNull(expression.getExpression());
        
        expression = new ConcentrationExpression("label");
        assertEquals(0, expression.getValue(), 0.01f);
        assertEquals("label", expression.getLabel());
        assertNull(expression.getOperator());
        assertNull(expression.getExpression());
        
        expression = new ConcentrationExpression(0.1f, "+", otherExpression);
        assertEquals(0.1f, expression.getValue(), 0.01f);
        assertNull(expression.getLabel());
        assertEquals(Operator.PLUS, expression.getOperator());
        assertSame(otherExpression, expression.getExpression());
        
        expression = new ConcentrationExpression("label", "+", otherExpression);
        assertEquals(0, expression.getValue(), 0.01f);
        assertEquals("label", expression.getLabel());
        assertEquals(Operator.PLUS, expression.getOperator());
        assertSame(otherExpression, expression.getExpression());
        
        assertEquals(Operator.MINUS, new ConcentrationExpression(0.1f, "-", otherExpression).getOperator());
        assertEquals(Operator.DIVIDE, new ConcentrationExpression(0.1f, "/", otherExpression).getOperator());
        assertEquals(Operator.MULTIPLY, new ConcentrationExpression(0.1f, "*", otherExpression).getOperator());
    }

    @Test
    public void testEvaluate_noCompartments() {
        ConcentrationExpression otherExpression = new ConcentrationExpression(0.1f);
        KappaModel kappaModel = new KappaModel();
        ComplexMatchingSimulation simulation = new ComplexMatchingSimulation(kappaModel);
        
        try {
            new ConcentrationExpression(0.1f).evaluate(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // expected exception
        }

        ConcentrationExpression expression = new ConcentrationExpression(0.1f);
        assertEquals(0.1f, expression.evaluate(simulation), 0.01f);
        
        expression = new ConcentrationExpression(0.1f, "+", otherExpression);
        assertEquals(0.2f, expression.evaluate(simulation), 0.01f);

        expression = new ConcentrationExpression(0.4f, "-", otherExpression);
        assertEquals(0.3f, expression.evaluate(simulation), 0.01f);

        expression = new ConcentrationExpression(2f, "*", otherExpression);
        assertEquals(0.2f, expression.evaluate(simulation), 0.01f);

        expression = new ConcentrationExpression(1f, "/", otherExpression);
        assertEquals(10f, expression.evaluate(simulation), 0.01f);

        kappaModel.addTransform(new LocatedTransform(new Transform("label", TransformTest.getComplexes(new Agent("A")), null, "0.5"), null));
        simulation.initialise();
        
        expression = new ConcentrationExpression("label");
        assertEquals(0.5f, expression.evaluate(simulation), 0.01f);
        
        expression = new ConcentrationExpression("label", "+", otherExpression);
        assertEquals(0.6f, expression.evaluate(simulation), 0.01f);

        // TODO Guard against division by 0 ?
    }

    @Test
    public void testEvaluate_compartments_transitions() {
        Location compartmentLocation = new Location("cytosol");
        Location cellLocation = new Location("cytosol", new MathExpression("1"));
        Compartment compartment = new Compartment("cytosol", 2);
        CompartmentLink compartmentLink = new CompartmentLink("link", new Location("cytosol", new MathExpression("0")), new Location("cytosol", new MathExpression("1")), Direction.BIDIRECTIONAL);

        ConcentrationExpression expression = new ConcentrationExpression("label");
        
        // Transform based expression - compartment irrelevant
        Transform transform = new Transform("label", TransformTest.getComplexes(new Agent("A")), null, "0.6");
        KappaModel kappaModel = new KappaModel();
        ComplexMatchingSimulation simulation = new ComplexMatchingSimulation(kappaModel);
        kappaModel.addCompartment(compartment);
        kappaModel.addTransform(new LocatedTransform(transform, compartmentLocation));
        simulation.initialise();
        assertEquals(0.6f, expression.evaluate(simulation), 0.01f);

        // Transform based expression - cell irrelevant
        transform = new Transform("label", TransformTest.getComplexes(new Agent("A")), null, "0.6");
        kappaModel = new KappaModel();
        simulation = new ComplexMatchingSimulation(kappaModel);
        kappaModel.addCompartment(compartment);
        kappaModel.addTransform(new LocatedTransform(transform, cellLocation));
        simulation.initialise();
        assertEquals(0.6f, expression.evaluate(simulation), 0.01f);
        
        // Transport based expression - location irrelevant
        Transport transport = new Transport("label", "link", getList(new Agent("A")), "0.6");
        kappaModel = new KappaModel();
        simulation = new ComplexMatchingSimulation(kappaModel);
        kappaModel.addCompartment(compartment);
        kappaModel.addCompartmentLink(compartmentLink);
        kappaModel.addTransport(transport);
        simulation.initialise();
        assertEquals(0.6f, expression.evaluate(simulation), 0.01f);
    }

    @Test
    public void testEvaluate_compartments_observables() {
        Compartment compartment = new Compartment("cytosol", 2);
        Location compartmentLocation = new Location("cytosol");
        Location cellLocation = new Location("cytosol", new MathExpression("1"));
        ConcentrationExpression expression = new ConcentrationExpression("label");

        // Compartment irrelevant
        KappaModel kappaModel = new KappaModel();
        ComplexMatchingSimulation simulation = new ComplexMatchingSimulation(kappaModel);
        kappaModel.addCompartment(compartment);
        kappaModel.addObservable(getList(new Agent("A")), "label", compartmentLocation, false);
        kappaModel.addInitialValue(getList(new Agent("A")), "100", compartmentLocation);
        simulation.initialise();
        assertEquals(100, expression.evaluate(simulation), 0.01f);
        
        // Cell irrelevant
        kappaModel = new KappaModel();
        simulation = new ComplexMatchingSimulation(kappaModel);
        kappaModel.addCompartment(compartment);
        kappaModel.addObservable(getList(new Agent("A")), "label", cellLocation, false);
        kappaModel.addInitialValue(getList(new Agent("A")), "100", cellLocation);
        simulation.initialise();
        assertEquals(100, expression.evaluate(simulation), 0.01f);
    }

}
