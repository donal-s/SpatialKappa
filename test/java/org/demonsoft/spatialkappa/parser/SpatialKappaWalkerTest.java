package org.demonsoft.spatialkappa.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.Parser;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.antlr.runtime.tree.TreeParser;
import org.demonsoft.spatialkappa.model.Agent;
import org.demonsoft.spatialkappa.model.AgentSite;
import org.demonsoft.spatialkappa.model.Compartment;
import org.demonsoft.spatialkappa.model.CompartmentLink;
import org.demonsoft.spatialkappa.model.Complex;
import org.demonsoft.spatialkappa.model.Direction;
import org.demonsoft.spatialkappa.model.KappaModel;
import org.demonsoft.spatialkappa.model.KappaModelTest;
import org.demonsoft.spatialkappa.model.Location;
import org.demonsoft.spatialkappa.model.MathExpression;
import org.demonsoft.spatialkappa.model.Transform;
import org.demonsoft.spatialkappa.model.TransformTest;
import org.demonsoft.spatialkappa.model.Transport;
import org.junit.Test;

public class SpatialKappaWalkerTest {

    @Test
    public void testRuleExpr() throws Exception {
        List<Complex> leftComplexes = TransformTest.getComplexes(new Agent("A", new AgentSite("s", null, "1")), new Agent("B", new AgentSite("x", null, "1")));
        List<Complex> rightComplexes = TransformTest.getComplexes(new Agent("A", new AgentSite("s", null, null)), new Agent("B", new AgentSite("x", null, null)));

        checkTransforms(new Transform[] {
            new Transform(null, leftComplexes, rightComplexes, "1")
        }, (KappaModel) runParserRule("ruleExpr", "A(s!1),B(x!1)   -> A(s),  B(x) @ 1"));
        checkTransforms(new Transform[] {
                new Transform(null, leftComplexes, rightComplexes, "1"), new Transform(null, rightComplexes, leftComplexes, "2")
        }, (KappaModel) runParserRule("ruleExpr", "A(s!1),B(x!1)   <-> A(s),  B(x) @ 1,2"));
        checkTransforms(new Transform[] {
            new Transform("label", leftComplexes, rightComplexes, "1")
        }, (KappaModel) runParserRule("ruleExpr", "'label' A(s!1),B(x!1)   -> A(s),  B(x) @ 1"));
        checkTransforms(new Transform[] {
                new Transform("label", leftComplexes, rightComplexes, "1"), new Transform("label", rightComplexes, leftComplexes, "2")
        }, (KappaModel) runParserRule("ruleExpr", "'label' A(s!1),B(x!1)   <-> A(s),  B(x) @ 1,2"));

        leftComplexes.clear();
        rightComplexes = TransformTest.getComplexes(new Agent("IPTG", new AgentSite("laci", null, null)));
        checkTransforms(new Transform[] {
                new Transform("IPTG addition{77331}", leftComplexes, rightComplexes, "0.0")
            }, (KappaModel) runParserRule("ruleExpr", "'IPTG addition{77331}'  -> IPTG(laci) @ 0.0"));

        leftComplexes = TransformTest.getComplexes(new Agent("CRY", new AgentSite("clk", null, null)), new Agent("EBOX-CLK-BMAL1", new AgentSite("cry", null, null)));
        rightComplexes = TransformTest.getComplexes(new Agent("CRY", new AgentSite("clk", null, "2")), new Agent("EBOX-CLK-BMAL1", new AgentSite("cry", null, "2")));
        checkTransforms(new Transform[] {
                new Transform("bin", leftComplexes, rightComplexes, "127.2862")
            }, (KappaModel) runParserRule("ruleExpr", "'bin' CRY(clk),EBOX-CLK-BMAL1(cry) -> CRY(clk!2), EBOX-CLK-BMAL1(cry!2) @ 127.2862"));
        
    }

    private void checkTransforms(Transform[] expected, KappaModel kappaModel) {
        KappaModelTest.checkTransforms(kappaModel, expected);
    }

    @Test
    public void testTransformExpr() throws Exception {
        List<Agent> leftAgents = new ArrayList<Agent>();
        leftAgents.add(new Agent("A", new AgentSite("s", null, "1")));
        leftAgents.add(new Agent("B", new AgentSite("x", null, "1")));
        List<Agent> rightAgents = new ArrayList<Agent>();
        rightAgents.add(new Agent("A", new AgentSite("s", null, null)));
        rightAgents.add(new Agent("B", new AgentSite("x", null, null)));

        checkTransform(Direction.FORWARD, "[A(s!1), B(x!1)]", "[A(s), B(x)]", runParserRule("transformExpr", "A(s!1),B(x!1)   -> A(s),  B(x)"));
        checkTransform(Direction.BIDIRECTIONAL, "[A(s!1), B(x!1)]", "[A(s), B(x)]", runParserRule("transformExpr", "A(s!1),B(x!1)   <-> A(s),  B(x)"));
    }

    private void checkTransform(Direction direction, String leftAgents, String rightAgents, Object actual) throws Exception {
        assertEquals(direction, actual.getClass().getDeclaredField("direction").get(actual));
        assertEquals(leftAgents, actual.getClass().getDeclaredField("lhs").get(actual).toString());
        assertEquals(rightAgents, actual.getClass().getDeclaredField("rhs").get(actual).toString());
    }

    @Test
    public void testLinkExpr() throws Exception {
        checkParserRule("linkExpr", "! 0", "0");
        checkParserRule("linkExpr", "! 1", "1");
        checkParserRule("linkExpr", "!1", "1");
        checkParserRule("linkExpr", "?", "?");
        checkParserRule("linkExpr", "!_", "_");
        checkParserRule("linkExpr", "! _", "_");
    }

    @Test
    public void testInitExpr() throws Exception {
        KappaModelTest.checkInitialValues((KappaModel) runParserRule("initExpr", "%init: 5 * (A(x~a,a!1),B(y~d,a!1))"), new Object[][] {
            {
                    "[A(a!1,x~a), B(a!1,y~d)]", 5, null
            }
        });
    }

    @Test
    public void testObsExpr() throws Exception {
        checkObservation("'label'", (KappaModel) runParserRule("obsExpr", "%obs: 'label'\n"));
        checkObservation("'label' ([A(x~a), B(y~d)])", (KappaModel) runParserRule("obsExpr", "%obs: 'label' A(x~a),B(y~d)"));
        checkObservation("'[A(x~a), B(y~d)]' ([A(x~a), B(y~d)])", (KappaModel) runParserRule("obsExpr", "%obs: A(x~a),B(y~d)"));
    }

    private void checkObservation(String expected, KappaModel kappaModel) {
        KappaModelTest.checkObservables(kappaModel, new String[] {
            expected
        });
    }

    private final void checkParserRule(String rulename, String inputText, String expectedOutput) throws Exception {
        assertEquals(expectedOutput, runParserRule(rulename, inputText).toString());
    }

    private final Object runParserRule(String rulename, String inputText) throws Exception {
        ANTLRInputStream input = new ANTLRInputStream(new ByteArrayInputStream(inputText.getBytes()));
        CommonTokenStream tokens = new CommonTokenStream(new SpatialKappaLexer(input));
        Parser parser = new SpatialKappaParser(tokens);

        Method ruleMethod = SpatialKappaParser.class.getMethod(rulename, (Class[]) null);
        Object ruleOutput = ruleMethod.invoke(parser, (Object[]) null);

        Method getTreeMethod = ruleOutput.getClass().getMethod("getTree", (Class[]) null);
        CommonTree tree = (CommonTree) getTreeMethod.invoke(ruleOutput, (Object[]) null);

        CommonTreeNodeStream nodes = new CommonTreeNodeStream(tree);
        nodes.setTokenStream(tokens);
        TreeParser walker = new SpatialKappaWalker(nodes);
        ruleMethod = SpatialKappaWalker.class.getMethod(rulename, (Class[]) null);
        if (!"void".equals(ruleMethod.getReturnType().getName())) {
            return ruleMethod.invoke(walker, (Object[]) null);
        }
        ruleMethod.invoke(walker, (Object[]) null);
        return ((SpatialKappaWalker) walker).kappaModel;
    }

    @Test
    public void testMathExpr() throws Exception {
        checkMathExpression("1", (MathExpression) runParserRule("mathExpr", "1"));
        checkMathExpression("x", (MathExpression) runParserRule("mathExpr", "x"));
        checkMathExpression("(2 + 3)", (MathExpression) runParserRule("mathExpr", "2 + 3"));
        checkMathExpression("(2 - 3)", (MathExpression) runParserRule("mathExpr", "2 - 3"));
        checkMathExpression("(2 / 3)", (MathExpression) runParserRule("mathExpr", "2 / 3"));
        checkMathExpression("(2 * 3)", (MathExpression) runParserRule("mathExpr", "2 * 3"));
        checkMathExpression("(2 % 3)", (MathExpression) runParserRule("mathExpr", "2 % 3"));
        checkMathExpression("(2 * 3)", (MathExpression) runParserRule("mathExpr", "2*3"));
        checkMathExpression("(2 * 3)", (MathExpression) runParserRule("mathExpr", "(2 * 3)"));
        checkMathExpression("(x + (y * 2))", (MathExpression) runParserRule("mathExpr", "x + (y * 2)"));
    }

    @Test
    public void testCompartmentExpr() throws Exception {
        checkCompartment("label", (KappaModel) runParserRule("compartmentExpr", "%compartment: 'label'"));
        checkCompartment("label[1]", (KappaModel) runParserRule("compartmentExpr", "%compartment: 'label'[1]"));
        checkCompartment("label[1][20]", (KappaModel) runParserRule("compartmentExpr", "%compartment: 'label' [1][20]"));
        checkCompartment("complex label-with-!�$%", (KappaModel) runParserRule("compartmentExpr", "%compartment: 'complex label-with-!�$%'"));

        try {
            runParserRule("compartmentExpr", "%compartment: 'label'[0]");
            fail("invalid should have failed");
        }
        catch (Exception ex) {
            // Expected exception
        }
    }
    
    private void checkCompartment(String expected, KappaModel kappaModel) {
        List<Compartment> compartments = kappaModel.getCompartments();
        assertEquals(1, compartments.size());
        assertEquals(expected, compartments.get(0).toString());
    }

    @Test
    public void testLocationExpr() throws Exception {
        checkCompartmentReference("label", (Location) runParserRule("locationExpr", "'label'"));
        checkCompartmentReference("label[1]", (Location) runParserRule("locationExpr", "'label'[1]"));
        checkCompartmentReference("label[1][(20 + x)]", (Location) runParserRule("locationExpr", "'label'[1][20+x]"));
    }
    
    private void checkCompartmentReference(String expected, Location actual) {
        assertNotNull(actual);
        assertEquals(expected, actual.toString());
    }

    private void checkMathExpression(String expected, MathExpression actual) {
        assertNotNull(actual);
        assertEquals(expected, actual.toString());
    }

    @Test
    public void testCompartmentLinkExpr() throws Exception {
        // Forward
        checkCompartmentLink("label: compartment1 -> compartment2", 
                (KappaModel) runParserRule("compartmentLinkExpr", "%link: 'label' 'compartment1' -> 'compartment2'"));
        checkCompartmentLink("label: compartment1[x] -> compartment2[2][(x + 1)]", 
                (KappaModel) runParserRule("compartmentLinkExpr", "%link: 'label' 'compartment1'[x] -> 'compartment2'[2][x+1]"));
        
        // Back
        checkCompartmentLink("label: compartment1 <- compartment2", 
                (KappaModel) runParserRule("compartmentLinkExpr", "%link: 'label' 'compartment1' <- 'compartment2'"));
        checkCompartmentLink("label: compartment1[x] <- compartment2[2][(x + 1)]", 
                (KappaModel) runParserRule("compartmentLinkExpr", "%link: 'label' 'compartment1'[x] <- 'compartment2'[2][x+1]"));
        
        // Both
        checkCompartmentLink("label: compartment1 <-> compartment2", 
                (KappaModel) runParserRule("compartmentLinkExpr", "%link: 'label' 'compartment1' <-> 'compartment2'"));
        checkCompartmentLink("label: compartment1[x] <-> compartment2[2][(x + 1)]", 
                (KappaModel) runParserRule("compartmentLinkExpr", "%link: 'label' 'compartment1'[x] <-> 'compartment2'[2][x+1]"));
    }
    
    private void checkCompartmentLink(String expected, KappaModel kappaModel) {
        List<CompartmentLink> links = kappaModel.getCompartmentLinks();
        assertEquals(1, links.size());
        assertEquals(expected, links.get(0).toString());
    }

    @Test
    public void testTransportExpr() throws Exception {
        checkTransport("intra-cytosol @ 0.1", 
                (KappaModel) runParserRule("transportExpr", "%transport: 'intra-cytosol' @ 0.1"));
        checkTransport("transport-all: intra-cytosol @ 0.1", 
                (KappaModel) runParserRule("transportExpr", "%transport: 'transport-all' 'intra-cytosol' @ 0.1"));
        checkTransport("intra-cytosol A(s),B(x) @ 0.1", 
                (KappaModel) runParserRule("transportExpr", "%transport: 'intra-cytosol'  A(s),B(x) @ 0.1"));
        checkTransport("transport-A: intra-cytosol A(s),B(x) @ 0.1", 
                (KappaModel) runParserRule("transportExpr", "%transport: 'transport-A' 'intra-cytosol'  A(s),B(x) @ 0.1"));
    }
    
    private void checkTransport(String expected, KappaModel kappaModel) {
        List<Transport> transports = kappaModel.getTransports();
        assertEquals(1, transports.size());
        assertEquals(expected, transports.get(0).toString());
    }

    @Test
    public void testObsExpr_spatial() throws Exception {
        checkObservation("'label' cytosol ([A(x~a), B(y~d)])", (KappaModel) runParserRule("obsExpr", "%obs: 'label' 'cytosol' A(x~a),B(y~d)"));
        checkObservation("'label' cytosol[0][1] ([A(x~a), B(y~d)])", (KappaModel) runParserRule("obsExpr", "%obs: 'label' 'cytosol'[0][1] A(x~a),B(y~d)"));
    }

    @Test
    public void testInitExpr_spatial() throws Exception {
        KappaModelTest.checkInitialValues((KappaModel) runParserRule("initExpr", "%init: 'cytosol' 5 * (A(x~a,a!1),B(y~d,a!1))"), 
                new Object[][] {{"[A(a!1,x~a), B(a!1,y~d)]", 5, new Location("cytosol")}});
        KappaModelTest.checkInitialValues((KappaModel) runParserRule("initExpr", "%init: 'cytosol'[0][1] 5 * (A(x~a,a!1),B(y~d,a!1))"), 
                new Object[][] {{"[A(a!1,x~a), B(a!1,y~d)]", 5, new Location("cytosol", new MathExpression("0"), new MathExpression("1"))}});
    }

    @Test
    public void testRuleExpr_spatial() throws Exception {
        Location location = new Location("cytosol");
        
        List<Complex> leftComplexes = TransformTest.getComplexes(
                new Agent("A", new AgentSite("s", null, "1")), new Agent("B", new AgentSite("x", null, "1")));
        List<Complex> rightComplexes = TransformTest.getComplexes(
                new Agent("A", new AgentSite("s", null, null)), new Agent("B", new AgentSite("x", null, null)));
        
        checkTransforms(new Transform[] {new Transform("label", leftComplexes, rightComplexes, "1")}, location, 
                (KappaModel) runParserRule("ruleExpr", "'label' 'cytosol' A(s!1),B(x!1)   -> A(s),  B(x) @ 1"));
        checkTransforms(new Transform[] {new Transform("label", leftComplexes, rightComplexes, "1"), 
                new Transform("label", rightComplexes, leftComplexes, "2")}, location, 
                (KappaModel) runParserRule("ruleExpr", "'label' 'cytosol' A(s!1),B(x!1)   <-> A(s),  B(x) @ 1,2"));
        
        location = new Location("cytosol", new MathExpression("0"), new MathExpression("1"));
        
        checkTransforms(new Transform[] {new Transform("label", leftComplexes, rightComplexes, "1")}, location, 
                (KappaModel) runParserRule("ruleExpr", "'label' 'cytosol'[0][1] A(s!1),B(x!1)   -> A(s),  B(x) @ 1"));
        checkTransforms(new Transform[] {new Transform("label", leftComplexes, rightComplexes, "1"), 
                new Transform("label", rightComplexes, leftComplexes, "2")}, location, 
                (KappaModel) runParserRule("ruleExpr", "'label' 'cytosol'[0][1] A(s!1),B(x!1)   <-> A(s),  B(x) @ 1,2"));
    }
    
    private void checkTransforms(Transform[] expected, Location location, KappaModel kappaModel) {
        KappaModelTest.checkTransforms(kappaModel, location, expected);
    }

    @Test
    public void testTransformExpr_spatial() throws Exception {
        List<Agent> leftAgents = new ArrayList<Agent>();
        leftAgents.add(new Agent("A", new AgentSite("s", null, "1")));
        leftAgents.add(new Agent("B", new AgentSite("x", null, "1")));
        List<Agent> rightAgents = new ArrayList<Agent>();
        rightAgents.add(new Agent("A", new AgentSite("s", null, null)));
        rightAgents.add(new Agent("B", new AgentSite("x", null, null)));
        
        checkTransform(Direction.FORWARD, "[A(s!1), B(x!1)]", "[A(s), B(x)]",
                runParserRule("transformExpr", "A(s!1),B(x!1)   -> A(s),  B(x)"));
        checkTransform(Direction.BIDIRECTIONAL, "[A(s!1), B(x!1)]", "[A(s), B(x)]",
                runParserRule("transformExpr", "A(s!1),B(x!1)   <-> A(s),  B(x)"));
    }

    @Test
    public void testId() throws Exception {
        checkId("id", "A", "A");
        checkId("id", "A_new", "A_new");
        checkId("id", "A-new", "A-new");
        checkId("id", "EBOX-CLK-BMAL1", "EBOX-CLK-BMAL1");
    }

    private void checkId(String rulename, String inputText, String expectedOutput) throws Exception {
        Object actual = runParserRule(rulename, inputText);
        assertEquals(expectedOutput, actual.getClass().getDeclaredField("result").get(actual));
    }


}
