package org.demonsoft.spatialkappa.parser;

import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_0;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_X;
import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;
import static org.demonsoft.spatialkappa.model.Utils.getList;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.Parser;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.demonsoft.spatialkappa.model.Agent;
import org.demonsoft.spatialkappa.model.AgentDeclaration;
import org.demonsoft.spatialkappa.model.BooleanExpression;
import org.demonsoft.spatialkappa.model.CellIndexExpression;
import org.demonsoft.spatialkappa.model.Channel;
import org.demonsoft.spatialkappa.model.IKappaModel;
import org.demonsoft.spatialkappa.model.Location;
import org.demonsoft.spatialkappa.model.Perturbation;
import org.demonsoft.spatialkappa.model.PerturbationEffect;
import org.demonsoft.spatialkappa.model.VariableExpression;
import org.demonsoft.spatialkappa.model.VariableExpression.Constant;
import org.demonsoft.spatialkappa.model.VariableExpression.Operator;
import org.demonsoft.spatialkappa.model.VariableReference;
import org.demonsoft.spatialkappa.parser.SpatialKappaWalker.link_return;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

public class SpatialKappaWalkerTest {

    private IKappaModel kappaModel = EasyMock.createMock(IKappaModel.class);
    
    // To deal with Agent not having equals()
    private Capture<List<Agent>> lhsAgents = new Capture<List<Agent>>();
    private Capture<List<Agent>> rhsAgents = new Capture<List<Agent>>();

    private Object[] mocks = { kappaModel };
    
    @Test
    public void testRuleDecl_transform() throws Throwable {

        checkRuleDecl("A(s!1),B(x!1)   -> A(s),  B(x) @ 1", 
                null, NOT_LOCATED, "[A(s!1), B(x!1)]", NOT_LOCATED, "[A(s), B(x)]", null, new VariableExpression(1));

        checkRuleDecl("'label' A(s!1),B(x!1)   -> A(s),  B(x) @ 1", 
                "label", NOT_LOCATED, "[A(s!1), B(x!1)]", NOT_LOCATED, "[A(s), B(x)]", null, new VariableExpression(1));

        checkRuleDecl("'label' A(s!1),B(x!1)   -> A(s),  B(x) @ 'x'+[inf]", 
                "label", NOT_LOCATED, "[A(s!1), B(x!1)]", NOT_LOCATED, "[A(s), B(x)]", null, 
                new VariableExpression(new VariableExpression(new VariableReference("x")), Operator.PLUS, 
                        new VariableExpression(Constant.INFINITY)));

        checkRuleDecl("'IPTG addition{77331}'  -> IPTG(laci) @ 0.0", 
                "IPTG addition{77331}", NOT_LOCATED, null, NOT_LOCATED, "[IPTG(laci)]", null, new VariableExpression(0));

        checkRuleDecl("'bin' CRY(clk),EBOX-CLK-BMAL1(cry) -> CRY(clk!2), EBOX-CLK-BMAL1(cry!2) @ 127.2862", 
                "bin", NOT_LOCATED, "[CRY(clk), EBOX-CLK-BMAL1(cry)]", NOT_LOCATED, "[CRY(clk!2), EBOX-CLK-BMAL1(cry!2)]", null, 
                new VariableExpression(127.2862f));

    }

    @Test
    public void testRuleDecl_transform_spatial() throws Throwable {

        Location location = new Location("cytosol");

        checkRuleDecl("'label' :cytosol A(s!1),B(x!1)   -> A(s),  B(x) @ 1", 
                "label", location, "[A(s!1), B(x!1)]", NOT_LOCATED, "[A(s), B(x)]", null, new VariableExpression(1));

        checkRuleDecl(":cytosol A(s!1),B(x!1)   -> A(s),  B(x) @ 1", 
                null, new Location("cytosol"), "[A(s!1), B(x!1)]", NOT_LOCATED, "[A(s), B(x)]", null, new VariableExpression(1));

        location = new Location("cytosol", new CellIndexExpression("0"), new CellIndexExpression("1"));

        checkRuleDecl("'label' :cytosol[0][1] A(s!1),B(x!1)   -> A(s),  B(x) @ 1", 
                "label", new Location("cytosol", INDEX_0, INDEX_1), "[A(s!1), B(x!1)]", NOT_LOCATED, "[A(s), B(x)]", null, new VariableExpression(1));
    }
    
    @Test
    public void testRuleDecl_transport() throws Throwable {
        checkRuleDecl("->:intra-cytosol @ 1", null,
                NOT_LOCATED, null, NOT_LOCATED, null, "intra-cytosol", new VariableExpression(1));

        checkRuleDecl("'label' ->:intra-cytosol @ 1", 
                "label", NOT_LOCATED, null, NOT_LOCATED, null, "intra-cytosol", new VariableExpression(1));

        checkRuleDecl("A(s),B(x) ->:intra-cytosol A(s),B(x) @ 1", 
                null, NOT_LOCATED, "[A(s), B(x)]", NOT_LOCATED, "[A(s), B(x)]", "intra-cytosol", new VariableExpression(1));

        checkRuleDecl("'label' A(s),B(x) ->:intra-cytosol A(s),B(x) @ 1", 
                "label", NOT_LOCATED, "[A(s), B(x)]", NOT_LOCATED, "[A(s), B(x)]", "intra-cytosol", new VariableExpression(1));
        
        // with rule locations
        checkRuleDecl(":source ->:intra-cytosol @ 1", 
                null, new Location("source"), null, NOT_LOCATED, null, "intra-cytosol", new VariableExpression(1));

        checkRuleDecl("'label' ->:intra-cytosol :target @ 1", 
                "label", NOT_LOCATED, null, new Location("target"), null, "intra-cytosol", new VariableExpression(1));

        checkRuleDecl("A(s),B(x) ->:intra-cytosol :target A(s),B(x) @ 1", 
                null, NOT_LOCATED, "[A(s), B(x)]", new Location("target"), "[A(s), B(x)]", "intra-cytosol", new VariableExpression(1));

        checkRuleDecl("'label' :source A(s),B(x) ->:intra-cytosol A(s),B(x) @ 1", 
                "label", new Location("source"), "[A(s), B(x)]", NOT_LOCATED, "[A(s), B(x)]", "intra-cytosol", new VariableExpression(1));

        // With variable rate
        checkRuleDecl("->:intra-cytosol @ |A()|", null,
                NOT_LOCATED, null, NOT_LOCATED, null, "intra-cytosol", 
                new VariableExpression(getList(new Agent("A")), NOT_LOCATED));
        checkRuleDecl("->:intra-cytosol @ |:loc1 A()|", null,
                NOT_LOCATED, null, NOT_LOCATED, null, "intra-cytosol", 
                new VariableExpression(getList(new Agent("A")), new Location("loc1")));
    }


    private void checkRuleDecl(String inputText, String label, Location leftLocation, String leftSideAgents, 
            Location rightLocation, String rightSideAgents, String channelName, VariableExpression rate) throws Throwable {
        lhsAgents.reset();
        rhsAgents.reset();
        reset(mocks);
        kappaModel.addTransition(eq(label), eq(leftLocation), capture(lhsAgents), eq(channelName), eq(rightLocation), 
                capture(rhsAgents), eq(rate));
        replay(mocks);
        runParserRule("ruleDecl", inputText);
        verify(mocks);
        assertEquals(leftSideAgents, lhsAgents.getValue() == null ? null : lhsAgents.getValue().toString());
        assertEquals(rightSideAgents, rhsAgents.getValue() == null ? null : rhsAgents.getValue().toString());
    }

    @Test
    public void testInitDecl() throws Throwable {
        checkInitDecl_value("%init: 5 A(x~a,a!1),B(y~d,a!1)", "[A(a!1,x~a), B(a!1,y~d)]", 5, NOT_LOCATED);
        
        checkInitDecl_reference("%init: 'label' A(x~a,a!1),B(y~d,a!1)", "[A(a!1,x~a), B(a!1,y~d)]", "label", NOT_LOCATED);
    }

    @Test
    public void testInitDecl_spatial() throws Throwable {

        checkInitDecl_value("%init: 5 :cytosol A(x~a,a!1),B(y~d,a!1)", "[A(a!1,x~a), B(a!1,y~d)]", 5, new Location("cytosol"));
        checkInitDecl_value("%init: 5 :cytosol[0][1] A(x~a,a!1),B(y~d,a!1)", "[A(a!1,x~a), B(a!1,y~d)]", 5, 
                new Location("cytosol", 0, 1));
        checkInitDecl_value("%init: 5 :cytosol[x][1] A(x~a,a!1),B(y~d,a!1)", "[A(a!1,x~a), B(a!1,y~d)]", 5, 
                new Location("cytosol", INDEX_X, INDEX_1));
        
        checkInitDecl_reference("%init: 'label' :cytosol A(x~a,a!1),B(y~d,a!1)", "[A(a!1,x~a), B(a!1,y~d)]", "label", new Location("cytosol"));
        checkInitDecl_reference("%init: 'label' :cytosol[0][1] A(x~a,a!1),B(y~d,a!1)", "[A(a!1,x~a), B(a!1,y~d)]", "label", 
                new Location("cytosol", 0, 1));
    }
    
    //TODO test agent specific locations

    private void checkInitDecl_reference(String inputText, String leftSideAgents, String label, Location location) throws Throwable {
        lhsAgents.reset();
        reset(mocks);
        kappaModel.addInitialValue(capture(lhsAgents), eq(new VariableReference(label)), eq(location));
        replay(mocks);
        runParserRule("initDecl", inputText);
        verify(mocks);
        assertEquals(leftSideAgents, lhsAgents.getValue().toString());
    }

    private void checkInitDecl_value(String inputText, String leftSideAgents, int value, Location location) throws Throwable {
        lhsAgents.reset();
        reset(mocks);
        kappaModel.addInitialValue(capture(lhsAgents), eq("" + value), eq(location));
        replay(mocks);
        runParserRule("initDecl", inputText);
        verify(mocks);
        assertEquals(leftSideAgents, lhsAgents.getValue().toString());
    }

    @Test
    public void testCompartmentDecl() throws Throwable {
        checkCompartmentDecl("%compartment: label", "label", null, new Integer[0]);
        checkCompartmentDecl("%compartment: label[1]", "label", null, new Integer[] {1});
        checkCompartmentDecl("%compartment: label[1][20]", "label", null, new Integer[] {1, 20});
        checkCompartmentDecl("%compartment: complex1Label-with-Stuff", "complex1Label-with-Stuff", null, new Integer[0]);

        checkCompartmentDecl("%compartment: label type", "label", "type", new Integer[0]);
        checkCompartmentDecl("%compartment: label type [10]", "label", "type", new Integer[] {10});
        checkCompartmentDecl("%compartment: label type [10][5] [2][3]", "label", "type", new Integer[] {10, 5, 2, 3});
    }
    
    private void checkCompartmentDecl(String inputText, String name, String type, Integer[] dimensions) throws Throwable {
        reset(mocks);
        kappaModel.addCompartment(eq(name), eq(type), eq(Arrays.asList(dimensions)));
        replay(mocks);
        runParserRule("compartmentDecl", inputText);
        verify(mocks);
    }

    @Test
    public void testChannelDecl() throws Throwable {
        checkChannelDecl("%channel: label :compartment1 -> :compartment2", "label: [[compartment1 -> compartment2]]");
        checkChannelDecl("%channel: label :compartment1[x] -> :compartment2[2][x + 1]", "label: [[compartment1[x] -> compartment2[2][(x + 1)]]]");
        checkChannelDecl("%channel: label (:compartment1[x] -> :compartment2[x + 1]) + " +
                "(:compartment1[x] -> :compartment2[x - 1])", 
                "label: ([[compartment1[x] -> compartment2[(x + 1)]]]) + ([[compartment1[x] -> compartment2[(x - 1)]]])");

        // Multi agent channels
        checkChannelDecl("%channel: diffusion" +
                "        (:membrane [x][y], :cytosol [u][v][0] -> :membrane [x + 1][y], :cytosol [u +1][v][0]) + \\\n" + 
                "        (:membrane [x][y], :cytosol [u][v][0] -> :membrane [x -1][y], :cytosol [u -1][v][0])", 
                "diffusion: ([[membrane[x][y] -> membrane[(x + 1)][y]], [cytosol[u][v][0] -> cytosol[(u + 1)][v][0]]]) + " +
                "([[membrane[x][y] -> membrane[(x - 1)][y]], [cytosol[u][v][0] -> cytosol[(u - 1)][v][0]]])");

        // Predefined channel types
        checkChannelDecl("%channel: label Hexagonal :compartment1 -> :compartment2", 
                "label: (Hexagonal) [[compartment1 -> compartment2]]");
        checkChannelDecl("%channel: label (Hexagonal :compartment1 -> :compartment2) + " +
                "(:compartment1[x] -> :compartment2[x - 1])", 
                "label: ((Hexagonal) [[compartment1 -> compartment2]]) + ([[compartment1[x] -> compartment2[(x - 1)]]])");
        checkChannelDecl("%channel: diffusion (Hexagonal :membrane, :cytosol -> :membrane, :cytosol)", 
                "diffusion: (Hexagonal) [[membrane -> membrane], [cytosol -> cytosol]]");

    }
    
    private void checkChannelDecl(String inputText, String linkText) throws Throwable {
        Capture<Channel> channel = new Capture<Channel>();
        reset(mocks);
        kappaModel.addChannel(capture(channel));
        replay(mocks);
        runParserRule("channelDecl", inputText);
        verify(mocks);
        assertEquals(linkText, channel.toString());
    }

    
    @Test
    public void testTransition_transform() throws Throwable {
        checkTransition("[A(s!1), B(x!1)]", "[A(s), B(x)]", null, runParserRule("transition", "A(s!1),B(x!1)   -> A(s),  B(x)"));
        checkTransition(null, "[A(s), B(x)]", null, runParserRule("transition", "-> A(s),  B(x)"));
    }

    @Test
    public void testTransition_transport() throws Throwable {
        checkTransition(null, null, "intra-cytosol", 
                runParserRule("transition", "->:intra-cytosol @"));
        checkTransition("[A(s), B(x)]", "[A(s), B(x)]", "intra-cytosol", 
                runParserRule("transition", "A(s),B(x) ->:intra-cytosol A(s),B(x)"));
    }

    private void checkTransition(String leftAgents, String rightAgents, String channel, Object actual) throws Exception {
        if (leftAgents == null) {
            assertNull(actual.getClass().getDeclaredField("lhs").get(actual));
        }
        else {
            assertEquals(leftAgents, actual.getClass().getDeclaredField("lhs").get(actual).toString());
        }
        if (rightAgents == null) {
            assertNull(actual.getClass().getDeclaredField("rhs").get(actual));
        }
        else {
            assertEquals(rightAgents, actual.getClass().getDeclaredField("rhs").get(actual).toString());
        }
        if (channel == null) {
            assertNull(actual.getClass().getDeclaredField("channel").get(actual));
        }
        else {
            assertEquals(channel, actual.getClass().getDeclaredField("channel").get(actual).toString());
        }
    }

    @Test
    public void testLink() throws Throwable {
        checkLink("! 0", "0", null, null);
        checkLink("! 1", "1", null, null);
        checkLink("!1", "1", null, null);
        checkLink("?", "?", null, null);
        checkLink("!_", "_", null, null);
        checkLink("! _", "_", null, null);
        // TODO - implement the !x.y syntax
        try {
            Object result = runParserRule("link", "!x.y");
            fail("occupied site syntax should have failed: " + result);
        }
        catch (RuntimeException ex) {
            // Expected exception
        }
//         checkLink("!x.y", "_", null, "y(x)");
//        checkLink("! x . y", "_", null, "y(x)");
    }

    @Test
    public void testLink_withNamedChannel() throws Throwable {
        checkLink("! 0:channel", "0", "channel", null);
        checkLink("! 1:channel", "1", "channel", null);
        checkLink("!1:channel", "1", "channel", null);
        checkLink("!_:channel", "_", "channel", null);
        checkLink("! _:channel", "_", "channel", null);
        // TODO - implement the !x.y syntax
        try {
            Object result = runParserRule("link", "!x.y:channel");
            fail("occupied site syntax should have failed: " + result);
        }
        catch (RuntimeException ex) {
            // Expected exception
        }
//        checkLink("!x.y:channel", "_", "channel", "y(x)");
//        checkLink("! x . y:channel", "_", "channel", "y(x)");

        // Suffix will be caught by next invoked rule
        checkLink("?:channel", "?", null, null);
        checkLink("?channel", "?", null, null);
    }

    private void checkLink(String input, String expectedLink, String expectedChannel, String expectedTargetSite) throws Throwable {
    	link_return result = (link_return) runParserRule("link", input);
    	assertNotNull(result);
    	assertEquals(expectedLink, result.linkName);
        assertEquals(expectedChannel, result.channelName);
        if (expectedTargetSite == null) {
            assertNull(result.targetSite);
        }
        else {
            assertEquals(expectedTargetSite, result.targetSite.toString());
        }
	}

	@Test
    public void testObsDecl() throws Throwable {
        checkObsDecl("%obs: 'label' A(x~a),B(y~d)\n", "label", "[A(x~a), B(y~d)]", NOT_LOCATED, false);
        checkObsDecl("%obs: voxel 'label' A(x~a),B(y~d)", "label", "[A(x~a), B(y~d)]", NOT_LOCATED, true);
        checkObsDecl("%obs: 'label' 800", "800.0", "label");
        checkObsDecl("%obs: 'label' [pi] ^ 2", "([pi] ^ 2.0)", "label");
        checkObsDecl("%obs: 'label' [log] 'n'", "[log] ('n')", "label");
    }

    @Test
    public void testObsDecl_spatial() throws Throwable {
        checkObsDecl("%obs: 'label' :cytosol A(x~a),B(y~d)\n", "label", "[A(x~a), B(y~d)]", new Location("cytosol"), false);
        checkObsDecl("%obs: voxel 'label' :cytosol A(x~a),B(y~d)\n", "label", "[A(x~a), B(y~d)]", new Location("cytosol"), true);
        checkObsDecl("%obs: 'label' :cytosol[0][1] A(x~a),B(y~d)\n", "label", "[A(x~a), B(y~d)]", new Location("cytosol", new CellIndexExpression("0"), new CellIndexExpression("1")), false);
        checkObsDecl("%obs: 'label' A:cytosol[0][1](x~a),B:cytosol[0][1](y~d)\n", "label", "[A:cytosol[0][1](x~a), B:cytosol[0][1](y~d)]", NOT_LOCATED, false);
    }

    private void checkObsDecl(String inputText, String label, String leftSideAgents, Location location, boolean recordVoxels) throws Throwable {
        checkObsVarDecl("obsDecl", inputText, label, leftSideAgents, location, true, recordVoxels);
    }
    private void checkVarDecl(String inputText, String label, String leftSideAgents, Location location, boolean recordVoxels) throws Throwable {
        checkObsVarDecl("varDecl", inputText, label, leftSideAgents, location, false, recordVoxels);
    }
    private void checkObsVarDecl(String ruleName, String inputText, String label, String leftSideAgents, Location location, boolean inObservations, boolean recordVoxels) throws Throwable {
        lhsAgents.reset();
        reset(mocks);
        kappaModel.addVariable(capture(lhsAgents), eq(label), eq(location), eq(recordVoxels));
        if (inObservations) {
            kappaModel.addPlot(label);
        }
        replay(mocks);
        runParserRule(ruleName, inputText);
        verify(mocks);
        assertEquals(leftSideAgents, lhsAgents.getValue().toString());
    }
    
    private void checkObsDecl(String inputText, String expressionText, String label) throws Throwable {
        checkObsVarDecl("obsDecl", inputText, expressionText, label, true);
    }
    private void checkVarDecl(String inputText, String expressionText, String label) throws Throwable {
        checkObsVarDecl("varDecl", inputText, expressionText, label, false);
    }
    private void checkObsVarDecl(String ruleName, String inputText, String expressionText, String label, boolean inObservations) throws Throwable {
        Capture<VariableExpression> expression = new Capture<VariableExpression>();
        reset(mocks);
        kappaModel.addVariable(capture(expression), eq(label));
        if (inObservations) {
            kappaModel.addPlot(label);
        }
        replay(mocks);
        runParserRule(ruleName, inputText);
        verify(mocks);
        assertEquals(expressionText, expression.getValue().toString());
    }


    @Test
    public void testPlotDecl() throws Throwable {
        checkPlotDecl("%plot: 'label'\n", "label");
    }

    @Test
    public void testProg() throws Throwable {
        reset(mocks);
        kappaModel.addCompartment("label", null, new ArrayList<Integer>());
        kappaModel.validate();
        replay(mocks);
        runParserRule("prog", "%compartment: label\n");
        verify(mocks);
    }
    
    @Test
    public void testProg_emptyInput() throws Throwable {
        reset(mocks);
        kappaModel.validate();
        replay(mocks);
        runParserRule("prog", "");
        verify(mocks);

        reset(mocks);
        kappaModel.validate();
        replay(mocks);
        runParserRule("prog", "\n");
        verify(mocks);

        reset(mocks);
        kappaModel.validate();
        replay(mocks);
        runParserRule("prog", "# comment");
        verify(mocks);

        reset(mocks);
        kappaModel.validate();
        replay(mocks);
        runParserRule("prog", "# comment\n");
        verify(mocks);
    }
    
    private void checkPlotDecl(String inputText, String label) throws Throwable {
        reset(mocks);
        kappaModel.addPlot(label);
        replay(mocks);
        runParserRule("plotDecl", inputText);
        verify(mocks);
    }

    @Test
    public void testVarDecl() throws Throwable {
        checkVarDecl("%var: 'label' A(x~a),B(y~d)", "label", "[A(x~a), B(y~d)]", NOT_LOCATED, false);
        checkVarDecl("%var: voxel 'label' A(x~a),B(y~d)", "label", "[A(x~a), B(y~d)]", NOT_LOCATED, true);
        checkVarDecl("%var: 'label' A:cytosol(x~a),B:cytosol(y~d)", 
                "label", "[A:cytosol(x~a), B:cytosol(y~d)]", NOT_LOCATED, false);
        checkVarDecl("%var: 'label' 2.55e4", "25500.0", "label");
        checkVarDecl("%var: 'label' ('a' + 'b') * 2", "(('a' + 'b') * 2.0)", "label");
        checkVarDecl("%var: 'label' [inf] * 2", "([inf] * 2.0)", "label");
        checkVarDecl("%var: 'label' [inf] - 2", "([inf] - 2.0)", "label");
        checkVarDecl("%var: 'label' [inf] / 2", "([inf] / 2.0)", "label");
        checkVarDecl("%var: 'label' [pi] ^ 2", "([pi] ^ 2.0)", "label");
        checkVarDecl("%var: 'label' [log] 'n'", "[log] ('n')", "label");
        checkVarDecl("%var: 'label' [sin] 'n'", "[sin] ('n')", "label");
        checkVarDecl("%var: 'label' [cos] [Tsim]", "[cos] ([Tsim])", "label");
        checkVarDecl("%var: 'label' [tan] [Tmax]", "[tan] ([Tmax])", "label");
        checkVarDecl("%var: 'label' [sqrt] [Emax]", "[sqrt] ([Emax])", "label");
        checkVarDecl("%var: 'label' [exp] 'n'", "[exp] ('n')", "label");
        checkVarDecl("%var: 'label' 'n' [mod] 2", "('n' [mod] 2.0)", "label");
        checkVarDecl("%var: 'label' 'n' ^ 2", "('n' ^ 2.0)", "label");
        checkVarDecl("%var: 'label' [int] 'n'", "[int] ('n')", "label");
    }

    @Test
    public void testModDecl() throws Throwable {
        checkModDecl("%mod: ([T]>10) && ('v1'/'v2') < 1 do $ADD 'n' C(x1~p)", 
                "(([T] > 10.0) && (('v1' / 'v2') < 1.0)) do [$ADD 'n' [C(x1~p)]]");
        checkModDecl("%mod: repeat ([log][E]>10) || [true] do $SNAPSHOT until [false]", 
                "(([log] ([E]) > 10.0) || [true]) do [$SNAPSHOT] until [false]");
        checkModDecl("%mod: repeat ([T] [mod] 100)=0 do $DEL [inf] C() until [T]>1000", 
                "(([T] [mod] 100.0) = 0.0) do [$DEL [inf] [C()]] until ([T] > 1000.0)");
        checkModDecl("%mod: [not][false] do $UPDATE 'rule_name' [inf]", 
                "[not] [false] do [$UPDATE 'rule_name' [inf]]");
        checkModDecl("%mod: [T]>10 do $ADD 'n' C();$DEL 'm' D()", 
                "([T] > 10.0) do [$ADD 'n' [C()], $DEL 'm' [D()]]");
    }
    
    private void checkModDecl(String inputText, String perturbationText) throws Throwable {
        Capture<Perturbation> perturbation = new Capture<Perturbation>();
        reset(mocks);
        kappaModel.addPerturbation(capture(perturbation));
        replay(mocks);
        runParserRule("modDecl", inputText);
        verify(mocks);
        assertEquals(perturbationText, perturbation.toString());
    }

    @Test
    public void testBooleanExpression() throws Throwable {
        checkBooleanExpression("[false]", "[false]");
        checkBooleanExpression("[true] && [true] || [false]", "(([true] && [true]) || [false])");
        checkBooleanExpression("[not][true] && [false]", "([not] [true] && [false])");
        checkBooleanExpression("[not]([true] && [false])", "[not] ([true] && [false])");
        checkBooleanExpression("[T]>10", "([T] > 10.0)");
        checkBooleanExpression("'v1'/'v2' < [E]", "(('v1' / 'v2') < [E])");
        checkBooleanExpression("[T]>10 && 'v1'/'v2' < 1", "(([T] > 10.0) && (('v1' / 'v2') < 1.0))");

        checkBooleanExpression("'v1' > 'v2'", "('v1' > 'v2')");
        checkBooleanExpression("'v1' < 'v2'", "('v1' < 'v2')");
        checkBooleanExpression("'v1' = 'v2'", "('v1' = 'v2')");
        checkBooleanExpression("'v1' <> 'v2'", "('v1' <> 'v2')");
    }
    
    private void checkBooleanExpression(String inputText, String expectedText) throws Throwable {
        BooleanExpression expression = (BooleanExpression) runParserRule("booleanExpression", inputText);
        assertNotNull(expression);
        assertEquals(expectedText, expression.toString());
    }

    @Test
    public void testEffect() throws Throwable {
        checkEffect("$SNAPSHOT", "$SNAPSHOT");
        checkEffect("$STOP", "$STOP");
        checkEffect("$ADD 'n' + 1 C()", "$ADD ('n' + 1.0) [C()]");
        checkEffect("$DEL [inf] C(), C(x1~p)", "$DEL [inf] [C(), C(x1~p)]");
        checkEffect("$UPDATE 'rule' 'n' + 1", "$UPDATE 'rule' ('n' + 1.0)");
    }
    
    private void checkEffect(String inputText, String expectedText) throws Throwable {
        PerturbationEffect effect = (PerturbationEffect) runParserRule("effect", inputText);
        assertNotNull(effect);
        assertEquals(expectedText, effect.toString());
    }


    private final void checkParserRule(String rulename, String inputText, String expectedOutput) throws Throwable {
        assertEquals(expectedOutput, runParserRule(rulename, inputText).toString());
    }

    private final Object runParserRule(String rulename, String inputText) throws Throwable {
        ANTLRInputStream input = new ANTLRInputStream(new ByteArrayInputStream(inputText.getBytes()));
        CommonTokenStream tokens = new CommonTokenStream(new SpatialKappaLexer(input));
        Parser parser = new SpatialKappaParser(tokens);

        Method ruleMethod = SpatialKappaParser.class.getMethod(rulename, (Class[]) null);
        Object ruleOutput = ruleMethod.invoke(parser, (Object[]) null);

        Method getTreeMethod = ruleOutput.getClass().getMethod("getTree", (Class[]) null);
        CommonTree tree = (CommonTree) getTreeMethod.invoke(ruleOutput, (Object[]) null);

        if (tree == null) {
            fail("Unhandled parse problem");
        }
        
        CommonTreeNodeStream nodes = new CommonTreeNodeStream(tree);
        nodes.setTokenStream(tokens);
        SpatialKappaWalker walker = new SpatialKappaWalker(nodes);
        walker.setKappaModel(kappaModel);
        ruleMethod = SpatialKappaWalker.class.getMethod(rulename, (Class[]) null);
        try {
            if (!"void".equals(ruleMethod.getReturnType().getName())) {
                return ruleMethod.invoke(walker, (Object[]) null);
            }
            ruleMethod.invoke(walker, (Object[]) null);
        }
        catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
        return walker.kappaModel;
    }

    @Test
    public void testCompartmentIndexExpr() throws Throwable {
        assertSame(CellIndexExpression.WILDCARD, runParserRule("compartmentIndexExpr", "[?]"));
        checkCellIndexExpression("1", (CellIndexExpression) runParserRule("compartmentIndexExpr", "[1]"));
        checkCellIndexExpression("x", (CellIndexExpression) runParserRule("compartmentIndexExpr", "[x]"));
    }

    @Test
    public void testCellIndexExpr() throws Throwable {
        checkCellIndexExpression("1", (CellIndexExpression) runParserRule("cellIndexExpr", "1"));
        checkCellIndexExpression("x", (CellIndexExpression) runParserRule("cellIndexExpr", "x"));
        checkCellIndexExpression("(2 + 3)", (CellIndexExpression) runParserRule("cellIndexExpr", "2 + 3"));
        checkCellIndexExpression("(2 - 3)", (CellIndexExpression) runParserRule("cellIndexExpr", "2 - 3"));
        checkCellIndexExpression("(2 / 3)", (CellIndexExpression) runParserRule("cellIndexExpr", "2 / 3"));
        checkCellIndexExpression("(2 * 3)", (CellIndexExpression) runParserRule("cellIndexExpr", "2 * 3"));
        checkCellIndexExpression("(2 % 3)", (CellIndexExpression) runParserRule("cellIndexExpr", "2 % 3"));
        checkCellIndexExpression("(2 ^ 3)", (CellIndexExpression) runParserRule("cellIndexExpr", "2 ^ 3"));
        checkCellIndexExpression("(2 * 3)", (CellIndexExpression) runParserRule("cellIndexExpr", "2*3"));
        checkCellIndexExpression("(2 * 3)", (CellIndexExpression) runParserRule("cellIndexExpr", "(2 * 3)"));
        checkCellIndexExpression("(x + (y * 2))", (CellIndexExpression) runParserRule("cellIndexExpr", "x + (y * 2)"));
    }

    @Test
    public void testAgentDecl() throws Throwable {
        checkAgentDecl("Agent", "%agent: Agent()");
        checkAgentDecl("Agent(none,single~value,multiple~val1~val2~val3)", 
        		"%agent: Agent(none,single~value,multiple~val1~val2~val3)");
    }
    

    
    private void checkAgentDecl(String expected, String inputText) throws Throwable {
        Capture<AgentDeclaration> agent = new Capture<AgentDeclaration>();
        reset(mocks);
        kappaModel.addAgentDeclaration(capture(agent));
        replay(mocks);
        runParserRule("agentDecl", inputText);
        verify(mocks);
        assertEquals(expected, agent.getValue().toString());
	}

	@Test
    public void testAgent() throws Throwable {
        checkAgent("A()", "A()");
        checkAgent("A:cytosol()", "A:cytosol()");
        checkAgent("A:cytosol[0][1]()", "A:cytosol[0][1]()");
        checkAgent("A(l!1,x~a)", "A(x~a,l!1)");
        checkAgent("A_new()", "A_new()");
        checkAgent("A-new()", "A-new()");
        checkAgent("EBOX-CLK-BMAL1()", "EBOX-CLK-BMAL1()");
        checkAgent("Predator(loc~domain,loc_index_1~0,loc_index_2~0)", "Predator(loc~domain,loc_index_1~0,loc_index_2~0)");
    }
    
   // TODO - test nil file and compartment not declared
    // TODO test naming conflict - Agent/compartment

    private void checkAgent(String expected, String input) throws Throwable {
        checkParserRule("agent", input, expected);
    }

    @Test
    public void testAgentInterface() throws Throwable {
    	checkParserRule("agentInterface", "l",     "l");
        checkParserRule("agentInterface", "l!1",   "l!1");
        checkParserRule("agentInterface", "l!_",   "l!_");
        checkParserRule("agentInterface", "l?",    "l?");
        checkParserRule("agentInterface", "l!1:channel", "l!1:channel");
        checkParserRule("agentInterface", "x~a", "x~a");

        // TODO - implement the !x.y syntax
        try {
            Object result = runParserRule("agentInterface", "l!x.y");
            fail("occupied site syntax should have failed: " + result);
        }
        catch (RuntimeException ex) {
            // Expected exception
        }
        // checkParserRule("agentInterface", "l!x.y", "l!x.y");
    }


    @Test
    public void testLocation() throws Throwable {
        assertSame(Location.FIXED_LOCATION, runParserRule("location", ":fixed"));
        checkLocation("label", (Location) runParserRule("location", ":label"));
        checkLocation("label[1]", (Location) runParserRule("location", ":label[1]"));
        checkLocation("label[1][?]", (Location) runParserRule("location", ":label[1][?]"));
        checkLocation("label[1][(20 + x)]", (Location) runParserRule("location", ":label[1][20 + x]"));
    }
    
    private void checkLocation(String expected, Location actual) {
        assertNotNull(actual);
        assertEquals(expected, actual.toString());
    }

    private void checkCellIndexExpression(String expected, CellIndexExpression actual) {
        assertNotNull(actual);
        assertEquals(expected, actual.toString());
    }


    @Test
    public void testId() throws Throwable {
//        checkId("A", "A");
        checkId("A_new", "A_new");
        checkId("A-new", "A-new");
        checkId("EBOX-CLK-BMAL1", "EBOX-CLK-BMAL1");
    }

    @Test
    public void testLabel() throws Throwable {
        checkLabel("'A'", "A");
        checkLabel("'2'", "2");
        checkLabel("''", "");
        checkLabel("'complicated__- string 234 \"£$'", "complicated__- string 234 \"£$");
    }

    private void checkId(String inputText, String expectedOutput) throws Throwable {
        Object actual = runParserRule("id", inputText);
        assertEquals(expectedOutput, actual.getClass().getDeclaredField("result").get(actual));
    }

    private void checkLabel(String inputText, String expectedOutput) throws Throwable {
        Object actual = runParserRule("label", inputText);
        assertEquals(expectedOutput, actual);
    }

}
