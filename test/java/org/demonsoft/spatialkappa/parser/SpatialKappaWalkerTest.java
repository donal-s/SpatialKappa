package org.demonsoft.spatialkappa.parser;

import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.Parser;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.demonsoft.spatialkappa.model.Agent;
import org.demonsoft.spatialkappa.model.AggregateAgent;
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
import org.demonsoft.spatialkappa.parser.SpatialKappaWalker.linkExpr_return;
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
    public void testRuleExpr() throws Exception {

        checkRuleExpr("A(s!1),B(x!1)   -> A(s),  B(x) @ 1", 
                null, "[A(s!1), B(x!1)]", "[A(s), B(x)]", new VariableExpression(1), NOT_LOCATED);

        checkRuleExpr("'label' A(s!1),B(x!1)   -> A(s),  B(x) @ 1", 
                "label", "[A(s!1), B(x!1)]", "[A(s), B(x)]", new VariableExpression(1), NOT_LOCATED);

        checkRuleExpr("'label' A(s!1),B(x!1)   -> A(s),  B(x) @ 'x'+[inf]", 
                "label", "[A(s!1), B(x!1)]", "[A(s), B(x)]", 
                new VariableExpression(new VariableExpression(new VariableReference("x")), Operator.PLUS, new VariableExpression(Constant.INFINITY)), NOT_LOCATED);

        checkRuleExpr("'IPTG addition{77331}'  -> IPTG(laci) @ 0.0", 
                "IPTG addition{77331}", null, "[IPTG(laci)]", new VariableExpression(0), NOT_LOCATED);

        checkRuleExpr("'bin' CRY(clk),EBOX-CLK-BMAL1(cry) -> CRY(clk!2), EBOX-CLK-BMAL1(cry!2) @ 127.2862", 
                "bin", "[CRY(clk), EBOX-CLK-BMAL1(cry)]", "[CRY(clk!2), EBOX-CLK-BMAL1(cry!2)]", 
                new VariableExpression(127.2862f), NOT_LOCATED);

    }

    @Test
    public void testRuleExpr_spatial() throws Exception {

        Location location = new Location("cytosol");

        checkRuleExpr("'label' cytosol A(s!1),B(x!1)   -> A(s),  B(x) @ 1", 
                "label", "[A(s!1), B(x!1)]", "[A(s), B(x)]", new VariableExpression(1), location);

        checkRuleExpr("cytosol A(s!1),B(x!1)   -> A(s),  B(x) @ 1", 
                null, "[A(s!1), B(x!1)]", "[A(s), B(x)]", new VariableExpression(1), location);

        location = new Location("cytosol", new CellIndexExpression("0"), new CellIndexExpression("1"));

        checkRuleExpr("'label' cytosol[0][1] A(s!1),B(x!1)   -> A(s),  B(x) @ 1", 
                "label", "[A(s!1), B(x!1)]", "[A(s), B(x)]", new VariableExpression(1), location);
    }
    
    private void checkRuleExpr(String inputText, String label, String leftSideAgents, String rightSideAgents, VariableExpression rate, Location location) throws Exception {
        lhsAgents.reset();
        rhsAgents.reset();
        reset(mocks);
        kappaModel.addTransform(eq(label), capture(lhsAgents), capture(rhsAgents), eq(rate), eq(location));
        replay(mocks);
        runParserRule("ruleExpr", inputText);
        verify(mocks);
        assertEquals(leftSideAgents, lhsAgents.getValue() == null ? null : lhsAgents.getValue().toString());
        assertEquals(rightSideAgents, rhsAgents.getValue() == null ? null : rhsAgents.getValue().toString());
    }

    @Test
    public void testInitExpr() throws Exception {
        checkInitExpr_value("%init: 5 A(x~a,a!1),B(y~d,a!1)", "[A(a!1,x~a), B(a!1,y~d)]", 5, NOT_LOCATED);
        
        checkInitExpr_reference("%init: 'label' A(x~a,a!1),B(y~d,a!1)", "[A(a!1,x~a), B(a!1,y~d)]", "label", NOT_LOCATED);
    }

    @Test
    public void testInitExpr_spatial() throws Exception {

        checkInitExpr_value("%init: 5 cytosol A(x~a,a!1),B(y~d,a!1)", "[A(a!1,x~a), B(a!1,y~d)]", 5, new Location("cytosol"));
        checkInitExpr_value("%init: 5 cytosol[0][1] A(x~a,a!1),B(y~d,a!1)", "[A(a!1,x~a), B(a!1,y~d)]", 5, 
                new Location("cytosol", new CellIndexExpression("0"), new CellIndexExpression("1")));
        
        checkInitExpr_reference("%init: 'label' cytosol A(x~a,a!1),B(y~d,a!1)", "[A(a!1,x~a), B(a!1,y~d)]", "label", new Location("cytosol"));
        checkInitExpr_reference("%init: 'label' cytosol[0][1] A(x~a,a!1),B(y~d,a!1)", "[A(a!1,x~a), B(a!1,y~d)]", "label", 
                new Location("cytosol", new CellIndexExpression("0"), new CellIndexExpression("1")));
    }
    
    //TODO test agent specific locations

    private void checkInitExpr_reference(String inputText, String leftSideAgents, String label, Location location) throws Exception {
        lhsAgents.reset();
        reset(mocks);
        kappaModel.addInitialValue(capture(lhsAgents), eq(new VariableReference(label)), eq(location));
        replay(mocks);
        runParserRule("initExpr", inputText);
        verify(mocks);
        assertEquals(leftSideAgents, lhsAgents.getValue().toString());
    }

    private void checkInitExpr_value(String inputText, String leftSideAgents, int value, Location location) throws Exception {
        lhsAgents.reset();
        reset(mocks);
        kappaModel.addInitialValue(capture(lhsAgents), eq("" + value), eq(location));
        replay(mocks);
        runParserRule("initExpr", inputText);
        verify(mocks);
        assertEquals(leftSideAgents, lhsAgents.getValue().toString());
    }

    @Test
    public void testCompartmentExpr() throws Exception {
        checkCompartmentExpr("%compartment: label", "label", new Integer[0]);
        checkCompartmentExpr("%compartment: label[1]", "label", new Integer[] {1});
        checkCompartmentExpr("%compartment: label[1][20]", "label", new Integer[] {1, 20});
        checkCompartmentExpr("%compartment: 0_complex1Label-with-Stuff", "0_complex1Label-with-Stuff", new Integer[0]);

        try {
            runParserRule("compartmentExpr", "%compartment: label[0]");
            fail("invalid should have failed");
        }
        catch (Exception ex) {
            // Expected exception
        }
    }
    
    private void checkCompartmentExpr(String inputText, String name, Integer[] dimensions) throws Exception {
        reset(mocks);
        kappaModel.addCompartment(eq(name), eq(Arrays.asList(dimensions)));
        replay(mocks);
        runParserRule("compartmentExpr", inputText);
        verify(mocks);
    }

    @Test
    public void testChannelDecl() throws Exception {
        // Forward
        checkChannelDecl("%channel: label compartment1 -> compartment2", "label: compartment1 -> compartment2");
        checkChannelDecl("%channel: label compartment1[x] -> compartment2[2][x+1]", "label: compartment1[x] -> compartment2[2][(x + 1)]");
        checkChannelDecl("%channel: label (compartment1[x] -> compartment2[x+1]) + " +
                "(compartment1[x] -> compartment2[x - 1])", 
                "label: (compartment1[x] -> compartment2[(x + 1)]) + (compartment1[x] -> compartment2[(x - 1)])");
    }
    
    private void checkChannelDecl(String inputText, String linkText) throws Exception {
        Capture<Channel> channel = new Capture<Channel>();
        reset(mocks);
        kappaModel.addChannel(capture(channel));
        replay(mocks);
        runParserRule("channelDecl", inputText);
        verify(mocks);
        assertEquals(linkText, channel.toString());
    }

    
    @Test
    public void testTransformExpr() throws Exception {
        checkTransform("[A(s!1), B(x!1)]", "[A(s), B(x)]", runParserRule("transformExpr", "A(s!1),B(x!1)   -> A(s),  B(x)"));
        checkTransform(null, "[A(s), B(x)]", runParserRule("transformExpr", "-> A(s),  B(x)"));
    }

    private void checkTransform(String leftAgents, String rightAgents, Object actual) throws Exception {
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
    }

    @Test
    public void testLinkExpr() throws Exception {
        checkLinkExpr("! 0", "0", null);
        checkLinkExpr("! 1", "1", null);
        checkLinkExpr("!1", "1", null);
        checkLinkExpr("?", "?", null);
        checkLinkExpr("!_", "_", null);
        checkLinkExpr("! _", "_", null);
    }

    @Test
    public void testLinkExpr_withNamedChannel() throws Exception {
        checkLinkExpr("! 0:channel", "0", "channel");
        checkLinkExpr("! 1:channel", "1", "channel");
        checkLinkExpr("!1:channel", "1", "channel");
        checkLinkExpr("!_:channel", "_", "channel");
        checkLinkExpr("! _:channel", "_", "channel");

        // Suffix will be caught by next invoked rule
        checkLinkExpr("?:channel", "?", null);
        checkLinkExpr("?channel", "?", null);
    }

    private void checkLinkExpr(String input, String expectedLink, String expectedChannel) throws Exception {
    	linkExpr_return result = (linkExpr_return) runParserRule("linkExpr", input);
    	assertNotNull(result);
    	assertEquals(expectedLink, result.linkName);
		assertEquals(expectedChannel, result.channelName);
	}

	@Test
    public void testObsExpr() throws Exception {
        checkObsExpr("obsExpr", "%obs: A(x~a),B(y~d)\n", "[A(x~a), B(y~d)]", "[A(x~a), B(y~d)]", NOT_LOCATED, true);
        checkObsExpr("obsExpr", "%obs: 'label' A(x~a),B(y~d)\n", "label", "[A(x~a), B(y~d)]", NOT_LOCATED, true);
    }

    @Test
    public void testObsExpr_spatial() throws Exception {
        checkObsExpr("obsExpr", "%obs: 'label' cytosol A(x~a),B(y~d)\n", "label", "[A(x~a), B(y~d)]", new Location("cytosol"), true);
        checkObsExpr("obsExpr", "%obs: 'label' cytosol[0][1] A(x~a),B(y~d)\n", "label", "[A(x~a), B(y~d)]", new Location("cytosol", new CellIndexExpression("0"), new CellIndexExpression("1")), true);
        checkObsExpr("obsExpr", "%obs: 'label' A:cytosol[0][1](x~a),B:cytosol[0][1](y~d)\n", "label", "[A:cytosol[0][1](x~a), B:cytosol[0][1](y~d)]", NOT_LOCATED, true);
           }

    private void checkObsExpr(String ruleName, String inputText, String label, String leftSideAgents, Location location, boolean inObservations) throws Exception {
        lhsAgents.reset();
        reset(mocks);
        kappaModel.addVariable(capture(lhsAgents), eq(label), eq(location));
        if (inObservations) {
            kappaModel.addPlot(label);
        }
        replay(mocks);
        runParserRule(ruleName, inputText);
        verify(mocks);
        assertEquals(leftSideAgents, lhsAgents.getValue().toString());
    }

    @Test
    public void testPlotExpr() throws Exception {
        checkPlotExpr("%plot: 'label'\n", "label");
    }

    @Test
    public void testProg_emptyInput() throws Exception {
        reset(mocks);
        replay(mocks);
        runParserRule("prog", "");
        verify(mocks);

        reset(mocks);
        replay(mocks);
        runParserRule("prog", "\n");
        verify(mocks);

        reset(mocks);
        replay(mocks);
        runParserRule("prog", "# comment");
        verify(mocks);

        reset(mocks);
        replay(mocks);
        runParserRule("prog", "# comment\n");
        verify(mocks);
    }
    
    private void checkPlotExpr(String inputText, String label) throws Exception {
        reset(mocks);
        kappaModel.addPlot(label);
        replay(mocks);
        runParserRule("plotExpr", inputText);
        verify(mocks);
    }

    @Test
    public void testTransportExpr() throws Exception {
        checkTransportExpr("%transport: intra-cytosol @ 0.1", 
                null, "intra-cytosol", null, new VariableExpression(0.1f));
        checkTransportExpr("%transport: 'transport-all' intra-cytosol @ 0.1", 
                "transport-all", "intra-cytosol", null, new VariableExpression(0.1f));
        checkTransportExpr("%transport: intra-cytosol  A(s),B(x) @ 0.1", 
                null, "intra-cytosol", "[A(s), B(x)]", new VariableExpression(0.1f));
        checkTransportExpr("%transport: 'transport-all' intra-cytosol  A(s),B(x) @ 0.1", 
                "transport-all", "intra-cytosol", "[A(s), B(x)]", new VariableExpression(0.1f));
    }
    
    private void checkTransportExpr(String inputText, String label, String compartmentLinkName, String leftSideAgents, VariableExpression rate) throws Exception {
        lhsAgents.reset();
        reset(mocks);
        kappaModel.addTransport(eq(label), eq(compartmentLinkName), capture(lhsAgents), eq(rate));
        replay(mocks);
        runParserRule("transportExpr", inputText);
        verify(mocks);
        assertEquals(leftSideAgents, lhsAgents.getValue() == null ? null : lhsAgents.getValue().toString());
    }

    @Test
    public void testVarExpr() throws Exception {
        checkObsExpr("varExpr", "%var: 'label' A(x~a),B(y~d)", "label", "[A(x~a), B(y~d)]", NOT_LOCATED, false);
        checkObsExpr("varExpr", "%var: 'label' A:cytosol(x~a),B:cytosol(y~d)", 
                "label", "[A:cytosol(x~a), B:cytosol(y~d)]", NOT_LOCATED, false);
        checkVarExpr("%var: 'label' 2.55e4", "25500.0", "label");
        checkVarExpr("%var: 'label' ('a' + 'b') * 2", "(('a' + 'b') * 2.0)", "label");
        checkVarExpr("%var: 'label' [inf] * 2", "([inf] * 2.0)", "label");
        checkVarExpr("%var: 'label' [inf] * 2", "([inf] * 2.0)", "label");
        checkVarExpr("%var: 'label' [pi] ^ 2", "([pi] ^ 2.0)", "label");
        checkVarExpr("%var: 'label' [log] 'n'", "[log] ('n')", "label");
        checkVarExpr("%var: 'label' [sin] 'n'", "[sin] ('n')", "label");
        checkVarExpr("%var: 'label' [cos] 'n'", "[cos] ('n')", "label");
        checkVarExpr("%var: 'label' [tan] 'n'", "[tan] ('n')", "label");
        checkVarExpr("%var: 'label' [sqrt] 'n'", "[sqrt] ('n')", "label");
        checkVarExpr("%var: 'label' [exp] 'n'", "[exp] ('n')", "label");
        checkVarExpr("%var: 'label' [mod] 'n' 2", "([mod] 'n' 2.0)", "label");
        checkVarExpr("%var: 'label' 'n' ^ 2", "('n' ^ 2.0)", "label");
    }

    private void checkVarExpr(String inputText, String expressionText, String label) throws Exception {
        Capture<VariableExpression> expression = new Capture<VariableExpression>();
        reset(mocks);
        kappaModel.addVariable(capture(expression), eq(label));
        replay(mocks);
        runParserRule("varExpr", inputText);
        verify(mocks);
        assertEquals(expressionText, expression.getValue().toString());
    }

    @Test
    public void testModExpr() throws Exception {
        checkModExpr("%mod: ([T]>10) && ('v1'/'v2') < 1 do $ADD 'n' C(x1~p)", "(([T] > 10.0) && (('v1' / 'v2') < 1.0)) do $ADD 'n' [C(x1~p)]");
        checkModExpr("%mod: ([log][E]>10) || [true] do $SNAPSHOT until [false]", "(([log] ([E]) > 10.0) || [true]) do $SNAPSHOT until [false]");
        checkModExpr("%mod: ([mod] [T] 100)=0 do $DEL [inf] C() until [T]>1000", "(([mod] [T] 100.0) = 0.0) do $DEL [inf] [C] until ([T] > 1000.0)");
        checkModExpr("%mod: [not][false] do 'rule_name' := [inf]", "[not] [false] do 'rule_name' := [inf]");
    }
    
    private void checkModExpr(String inputText, String perturbationText) throws Exception {
        Capture<Perturbation> perturbation = new Capture<Perturbation>();
        reset(mocks);
        kappaModel.addPerturbation(capture(perturbation));
        replay(mocks);
        runParserRule("modExpr", inputText);
        verify(mocks);
        assertEquals(perturbationText, perturbation.toString());
    }

    @Test
    public void testBooleanExpression() throws Exception {
        checkBooleanExpression("[false]", "[false]");
        checkBooleanExpression("[true] && [true] || [false]", "(([true] && [true]) || [false])");
        checkBooleanExpression("[not][true] && [false]", "([not] [true] && [false])");
        checkBooleanExpression("[not]([true] && [false])", "[not] ([true] && [false])");
        checkBooleanExpression("[T]>10", "([T] > 10.0)");
        checkBooleanExpression("'v1'/'v2' < [E]", "(('v1' / 'v2') < [E])");
        checkBooleanExpression("[T]>10 && 'v1'/'v2' < 1", "(([T] > 10.0) && (('v1' / 'v2') < 1.0))");
    }
    
    private void checkBooleanExpression(String inputText, String expectedText) throws Exception {
        BooleanExpression expression = (BooleanExpression) runParserRule("booleanExpression", inputText);
        assertNotNull(expression);
        assertEquals(expectedText, expression.toString());
    }

    @Test
    public void testEffect() throws Exception {
        checkEffect("$SNAPSHOT", "$SNAPSHOT");
        checkEffect("$STOP", "$STOP");
        checkEffect("$ADD 'n' + 1 C()", "$ADD ('n' + 1.0) [C]");
        checkEffect("$DEL [inf] C(), C(x1~p)", "$DEL [inf] [C, C(x1~p)]");
        checkEffect("'rule' := 'n' + 1", "'rule' := ('n' + 1.0)");
    }
    
    private void checkEffect(String inputText, String expectedText) throws Exception {
        PerturbationEffect effect = (PerturbationEffect) runParserRule("effect", inputText);
        assertNotNull(effect);
        assertEquals(expectedText, effect.toString());
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

        if (tree != null) {
	        CommonTreeNodeStream nodes = new CommonTreeNodeStream(tree);
	        nodes.setTokenStream(tokens);
	        SpatialKappaWalker walker = new SpatialKappaWalker(nodes);
	        walker.setKappaModel(kappaModel);
	        ruleMethod = SpatialKappaWalker.class.getMethod(rulename, (Class[]) null);
	        if (!"void".equals(ruleMethod.getReturnType().getName())) {
	            return ruleMethod.invoke(walker, (Object[]) null);
	        }
	        ruleMethod.invoke(walker, (Object[]) null);
	        return walker.kappaModel;
        }
		return kappaModel;
    }

    @Test
    public void testCellIndexExpr() throws Exception {
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
    public void testAgentExpr() throws Exception {
        checkAgentExpr("Agent", "%agent: Agent");
        checkAgentExpr("Agent", "%agent: Agent()");
        checkAgentExpr("Agent(none,single~value,multiple~val1~val2~val3)", 
        		"%agent: Agent(none,single~value,multiple~val1~val2~val3)");
    }
    

    
    private void checkAgentExpr(String expected, String inputText) throws Exception {
        Capture<AggregateAgent> agent = new Capture<AggregateAgent>();
        reset(mocks);
        kappaModel.addAgentDeclaration(capture(agent));
        replay(mocks);
        runParserRule("agentExpr", inputText);
        verify(mocks);
        assertEquals(expected, agent.getValue().toString());
	}

	@Test
    public void testAgent() throws Exception {
        checkAgent("A", "A");
        checkAgent("A", "A()");
        checkAgent("A:cytosol", "A:cytosol");
        checkAgent("A:cytosol", "A:cytosol()");
        checkAgent("A:cytosol[0][1]", "A:cytosol[0][1]");
        checkAgent("A:cytosol[0][1]", "A:cytosol[0][1]()");
        checkAgent("A(l!1,x~a)", "A(x~a,l!1)");
        checkAgent("A_new", "A_new()");
        checkAgent("A-new", "A-new()");
        checkAgent("EBOX-CLK-BMAL1", "EBOX-CLK-BMAL1()");
        checkAgent("Predator(loc~domain,loc_index_1~0,loc_index_2~0)", "Predator(loc~domain,loc_index_1~0,loc_index_2~0)");
    }
    
   // TODO - test nil file and compartment not declared
    // TODO test naming conflict - Agent/compartment

    private void checkAgent(String expected, String input) throws Exception {
        checkParserRule("agent", input, expected);
    }

    @Test
    public void testIface() throws Exception {
    	checkParserRule("iface", "l", "l");
    	checkParserRule("iface", "l!1", "l!1");
    	checkParserRule("iface", "l!1:channel", "l!1:channel");
    	checkParserRule("iface", "x~a", "x~a");
    }


    @Test
    public void testLocationExpr() throws Exception {
        checkCompartmentReference("label", (Location) runParserRule("locationExpr", "label"));
        checkCompartmentReference("label[1]", (Location) runParserRule("locationExpr", "label[1]"));
        checkCompartmentReference("label[1][(20 + x)]", (Location) runParserRule("locationExpr", "label[1][20+x]"));
    }
    
    private void checkCompartmentReference(String expected, Location actual) {
        assertNotNull(actual);
        assertEquals(expected, actual.toString());
    }

    private void checkCellIndexExpression(String expected, CellIndexExpression actual) {
        assertNotNull(actual);
        assertEquals(expected, actual.toString());
    }


    @Test
    public void testTransformExpr_spatial() throws Exception { //TODO ?
        checkTransform("[A(s!1), B(x!1)]", "[A(s), B(x)]", runParserRule("transformExpr", "A(s!1),B(x!1)   -> A(s),  B(x)"));
    }

    @Test
    public void testId() throws Exception {
        checkId("id", "A", "A");
        checkId("id", "A_new", "A_new");
        checkId("id", "A-new", "A-new");
        checkId("id", "EBOX-CLK-BMAL1", "EBOX-CLK-BMAL1");
    }

    @Test
    public void testLabel() throws Exception {
        checkLabel("label", "'A'", "A");
        checkLabel("label", "'2'", "2");
        checkLabel("label", "''", "");
        checkLabel("label", "'complicated__- string 234 \"£$'", "complicated__- string 234 \"£$");
    }

    private void checkId(String rulename, String inputText, String expectedOutput) throws Exception {
        Object actual = runParserRule(rulename, inputText);
        assertEquals(expectedOutput, actual.getClass().getDeclaredField("result").get(actual));
    }

    private void checkLabel(String rulename, String inputText, String expectedOutput) throws Exception {
        Object actual = runParserRule(rulename, inputText);
        assertEquals(expectedOutput, actual);
    }

}
