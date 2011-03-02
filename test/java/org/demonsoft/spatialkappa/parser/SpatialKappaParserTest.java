package org.demonsoft.spatialkappa.parser;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.Parser;
import org.antlr.runtime.tree.CommonErrorNode;
import org.antlr.runtime.tree.CommonTree;
import org.junit.Test;

public class SpatialKappaParserTest {

    @Test
    public void testRuleExpr() throws Exception {
        runParserRule("ruleExpr", "A(s!1),B(x!1)   -> A(s),  B(x) @ 1", 
                "(TRANSFORM (-> (LHS (AGENTS (AGENT A (INTERFACE s (LINK 1))) (AGENT B (INTERFACE x (LINK 1))))) " +
                "(RHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x))))) " +
                "(RATE (RATEVALUE 1)))");
        runParserRule("ruleExpr", "A(s!1),B(x!1)   <-> A(s),  B(x) @ 1,2", 
                "(TRANSFORM (<-> (LHS (AGENTS (AGENT A (INTERFACE s (LINK 1))) (AGENT B (INTERFACE x (LINK 1))))) " +
                "(RHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x))))) " +
                "(RATE (RATEVALUE 1) (RATEVALUE 2)))");
        runParserRule("ruleExpr", "'label' A(s!1),B(x!1)   -> A(s),  B(x) @ 1", 
                "(TRANSFORM (-> (LHS (AGENTS (AGENT A (INTERFACE s (LINK 1))) (AGENT B (INTERFACE x (LINK 1))))) " +
                "(RHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x))))) " +
                "(RATE (RATEVALUE 1)) label)");
        runParserRule("ruleExpr", "'label' A(s!1),B(x!1)   <-> A(s),  B(x) @ 1,2", 
                "(TRANSFORM (<-> (LHS (AGENTS (AGENT A (INTERFACE s (LINK 1))) (AGENT B (INTERFACE x (LINK 1))))) " +
                "(RHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x))))) " +
                "(RATE (RATEVALUE 1) (RATEVALUE 2)) label)");
        runParserRule("ruleExpr", "'IPTG addition{77331}'  -> IPTG(laci) @ 0.0", 
                "(TRANSFORM (-> LHS (RHS (AGENTS (AGENT IPTG (INTERFACE laci))))) (RATE (RATEVALUE 0.0)) IPTG addition{77331})");
        
        runParserRule("ruleExpr", "'bin' CRY(clk),EBOX-CLK-BMAL1(cry) -> CRY(clk!2), EBOX-CLK-BMAL1(cry!2) @ 127.286", 
                "(TRANSFORM (-> (LHS (AGENTS (AGENT CRY (INTERFACE clk)) (AGENT EBOX-CLK-BMAL1 (INTERFACE cry)))) " +
                "(RHS (AGENTS (AGENT CRY (INTERFACE clk (LINK 2))) (AGENT EBOX-CLK-BMAL1 (INTERFACE cry (LINK 2)))))) " +
                "(RATE (RATEVALUE 127.286)) bin)");
    }

    @Test
    public void testTransformExpr() throws Exception {
        runParserRule("transformExpr", "A(s),B(x) <-> @", 
                "(<-> (LHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x)))) RHS)");
        runParserRule("transformExpr", "-> A(s!1),B(x!1)", 
                "(-> LHS (RHS (AGENTS (AGENT A (INTERFACE s (LINK 1))) (AGENT B (INTERFACE x (LINK 1))))))");
        runParserRule("transformExpr", "A(s),B(x) <-> A(s!1),B(x!1)", 
                "(<-> (LHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x)))) (RHS (AGENTS (AGENT A (INTERFACE s (LINK 1))) (AGENT B (INTERFACE x (LINK 1))))))");
    }

    @Test
    public void testAgentGroup() throws Exception {
        runParserRule("agentGroup", "A()", "(AGENTS (AGENT A))");
        runParserRule("agentGroup", "A(x~a,l!1),B(y~b,m!1)", 
                "(AGENTS (AGENT A (INTERFACE x (STATE a)) (INTERFACE l (LINK 1))) (AGENT B (INTERFACE y (STATE b)) (INTERFACE m (LINK 1))))");
    }

    @Test
    public void testStateExpr() throws Exception {
        runParserRule("stateExpr", "~a", "(STATE a)");
        runParserRule("stateExpr", "~3a", "(STATE 3a)");
    }

    @Test
    public void testLinkExpr() throws Exception {
        runParserRule("linkExpr", "! 0", "(LINK 0)");
        runParserRule("linkExpr", "! 1", "(LINK 1)");
        runParserRule("linkExpr", "!1", "(LINK 1)");
        runParserRule("linkExpr", "?", "(LINK ANY)");
        runParserRule("linkExpr", "!_", "(LINK OCCUPIED)");
        runParserRule("linkExpr", "! _", "(LINK OCCUPIED)");
        
        runParserRuleFail("linkExpr", "! -1");
        runParserRuleFail("linkExpr", "!");
        runParserRuleFail("linkExpr", "! 0.1");
        runParserRuleFail("linkExpr", "! a");
    }

    @Test
    public void testTransformKineticExpr() throws Exception {
        runParserRule("transformKineticExpr", "@ 0.1", "(RATE (RATEVALUE 0.1))");
        runParserRule("transformKineticExpr", "@ 0.1,0.2", "(RATE (RATEVALUE 0.1) (RATEVALUE 0.2))");
    }

    @Test
    public void testTransportKineticExpr() throws Exception {
        runParserRule("transportKineticExpr", "@ 0.1", "(RATE (RATEVALUE 0.1))");
    }

    @Test
    public void testInitExpr() throws Exception {
        runParserRule("initExpr", "%init: 5 * (A(x~a),B(y~d))", 
            "(INIT (AGENTS (AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) 5)");
    }

    @Test
    public void testObsExpr() throws Exception {
        runParserRule("obsExpr", "%obs: 'label'\n", "(OBSERVATION label)");
        runParserRule("obsExpr", "%obs: 'label' A(x~a),B(y~d)", 
            "(OBSERVATION (AGENTS (AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) label)");
        runParserRule("obsExpr", "%obs: A(x~a),B(y~d)", 
            "(OBSERVATION (AGENTS (AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))))");
        
        runParserRuleFail("obsExpr", " \"%obs: \n");
    }

    @Test
    public void testMarker() throws Exception {
        runParserRule("marker", "100", "100");
        runParserRule("marker", "a100", "a100");
        runParserRule("marker", "100a", "100a");
    }

    @Test
    public void testNumber() throws Exception {
        runParserRule("number", "1", "1");
        runParserRule("number", "0", "0");
        runParserRule("number", "101", "101");
        runParserRule("number", "2e-10", "2e-10");
        runParserRule("number", "2E+10", "2E+10");
        runParserRule("number", "2.55", "2.55");
        runParserRule("number", "2.55e4", "2.55e4");
        runParserRule("number", ".55", ".55");
        runParserRule("number", ".55e4", ".55e4");
    }
    
    @Test
    public void testCompartmentExpr() throws Exception {
        runParserRule("compartmentExpr", "%compartment: 'label'", "(COMPARTMENT label)");
        runParserRule("compartmentExpr", "%compartment: 'label'[1]", "(COMPARTMENT label (DIMENSION 1))");
        runParserRule("compartmentExpr", "%compartment: 'label'[0]", "(COMPARTMENT label (DIMENSION 0))");
        runParserRule("compartmentExpr", "%compartment: 'label' [1][20]", "(COMPARTMENT label (DIMENSION 1) (DIMENSION 20))");
        runParserRule("compartmentExpr", "%compartment: 'complex label-with-!�$%'", "(COMPARTMENT complex label-with-!�$%)");
        
        // Error cases - TODO have them throw exceptions
        runParserRule("compartmentExpr", "%compartment:", "(COMPARTMENT <missing LABEL>)");
        runParserRule("compartmentExpr", "%compartment: \"label\"[10]", "(COMPARTMENT <missing LABEL>)");
        runParserRule("compartmentExpr", "%compartment: 'label'[-1]", "(COMPARTMENT label (DIMENSION 1))");
        runParserRuleFail("compartmentExpr", "%compartment: 'label'[1.0]");
    }

    @Test
    public void testCompartmentLinkExpr() throws Exception {
        //Forward
        runParserRule("compartmentLinkExpr", "%link: 'label' 'compartment1' -> 'compartment2'", 
            "(COMPARTMENT_LINK label (LOCATION compartment1) -> (LOCATION compartment2))");
        runParserRule("compartmentLinkExpr", "%link: 'label' 'compartment1'[x] -> 'compartment2'[x+1]", 
            "(COMPARTMENT_LINK label (LOCATION compartment1 (INDEX (MATH_EXPR x))) -> " +
            "(LOCATION compartment2 (INDEX (MATH_EXPR + (MATH_EXPR x) (MATH_EXPR 1)))))");
        
        // Back
        runParserRule("compartmentLinkExpr", "%link: 'label' 'compartment1' <- 'compartment2'", 
            "(COMPARTMENT_LINK label (LOCATION compartment1) <- (LOCATION compartment2))");
        runParserRule("compartmentLinkExpr", "%link: 'label' 'compartment1'[x] <- 'compartment2'[x+1]", 
            "(COMPARTMENT_LINK label (LOCATION compartment1 (INDEX (MATH_EXPR x))) <- " +
            "(LOCATION compartment2 (INDEX (MATH_EXPR + (MATH_EXPR x) (MATH_EXPR 1)))))");
        
        // Both
        runParserRule("compartmentLinkExpr", "%link: 'label' 'compartment1' <-> 'compartment2'", 
            "(COMPARTMENT_LINK label (LOCATION compartment1) <-> (LOCATION compartment2))");
        runParserRule("compartmentLinkExpr", "%link: 'label' 'compartment1'[x] <-> 'compartment2'[x+1]", 
            "(COMPARTMENT_LINK label (LOCATION compartment1 (INDEX (MATH_EXPR x))) <-> " +
            "(LOCATION compartment2 (INDEX (MATH_EXPR + (MATH_EXPR x) (MATH_EXPR 1)))))");
        
        //Forward negative
        runParserRule("compartmentLinkExpr", "%link: 'label' 'compartment1'[x] -> 'compartment2'[x-1]", 
            "(COMPARTMENT_LINK label (LOCATION compartment1 (INDEX (MATH_EXPR x))) -> " +
            "(LOCATION compartment2 (INDEX (MATH_EXPR - (MATH_EXPR x) (MATH_EXPR 1)))))");

    }

    @Test
    public void testTransportExpr() throws Exception {
        runParserRule("transportExpr", "%transport: 'intra-cytosol' @ 0.1", 
                "(TRANSPORT intra-cytosol (RATE (RATEVALUE 0.1)))");
        runParserRule("transportExpr", "%transport: 'transport-all' 'intra-cytosol' @ 0.1", 
                "(TRANSPORT intra-cytosol (RATE (RATEVALUE 0.1)) transport-all)");
        runParserRule("transportExpr", "%transport: 'intra-cytosol' A(s),B(x) @ 0.1", 
                "(TRANSPORT intra-cytosol (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x))) (RATE (RATEVALUE 0.1)))");
        runParserRule("transportExpr", "%transport: 'transport-all' 'intra-cytosol' A(s),B(x) @ 0.1", 
                "(TRANSPORT intra-cytosol (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x))) (RATE (RATEVALUE 0.1)) transport-all)");
    }

    @Test
    public void testCompartmentReferenceExpr() throws Exception {
        runParserRule("locationExpr", "'compartment1'", "(LOCATION compartment1)");
        runParserRule("locationExpr", "'compartment1'[x][x+1]", 
                "(LOCATION compartment1 (INDEX (MATH_EXPR x)) (INDEX (MATH_EXPR + (MATH_EXPR x) (MATH_EXPR 1))))");
    }

    @Test
    public void testMathExpr() throws Exception {
        runParserRule("mathExpr", "1", "(MATH_EXPR 1)");
        runParserRule("mathExpr", "x", "(MATH_EXPR x)");
        runParserRule("mathExpr", "2 + 3", "(MATH_EXPR + (MATH_EXPR 2) (MATH_EXPR 3))");
        runParserRule("mathExpr", "2 - 3", "(MATH_EXPR - (MATH_EXPR 2) (MATH_EXPR 3))");
        runParserRule("mathExpr", "2 / 3", "(MATH_EXPR / (MATH_EXPR 2) (MATH_EXPR 3))");
        runParserRule("mathExpr", "2 * 3", "(MATH_EXPR * (MATH_EXPR 2) (MATH_EXPR 3))");
        runParserRule("mathExpr", "2 % 3", "(MATH_EXPR % (MATH_EXPR 2) (MATH_EXPR 3))");
        runParserRule("mathExpr", "2-3", "(MATH_EXPR - (MATH_EXPR 2) (MATH_EXPR 3))");
        runParserRule("mathExpr", "2*3", "(MATH_EXPR * (MATH_EXPR 2) (MATH_EXPR 3))");
        runParserRule("mathExpr", "(2 * 3)", "(MATH_EXPR * (MATH_EXPR 2) (MATH_EXPR 3))");
        runParserRule("mathExpr", "x + (y * 2)", "(MATH_EXPR + (MATH_EXPR x) (MATH_EXPR * (MATH_EXPR y) (MATH_EXPR 2)))");
    }

    @Test
    public void testObsExpr_spatial() throws Exception {
        runParserRule("obsExpr", "%obs: 'label' 'cytosol' A(x~a),B(y~d)", 
            "(OBSERVATION (AGENTS (AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) label (LOCATION cytosol))");
        runParserRule("obsExpr", "%obs: 'label' 'cytosol'[0][1] A(x~a),B(y~d)", 
            "(OBSERVATION (AGENTS (AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) label " +
            "(LOCATION cytosol (INDEX (MATH_EXPR 0)) (INDEX (MATH_EXPR 1))))");
        
        runParserRuleFail("obsExpr", "%obs: 'cytosol'[0][1] A(x~a),B(y~d)");
        runParserRuleFail("obsExpr", "%obs: 'cytosol'[0][1]");
    }

    @Test
    public void testInitExpr_spatial() throws Exception {
        runParserRule("initExpr", "%init: 'cytosol' 5 * (A(x~a),B(y~d))", 
            "(INIT (AGENTS (AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) 5 (LOCATION cytosol))");
        runParserRule("initExpr", "%init: 'cytosol'[0][1] 5 * (A(x~a),B(y~d))", 
            "(INIT (AGENTS (AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) 5 " +
            "(LOCATION cytosol (INDEX (MATH_EXPR 0)) (INDEX (MATH_EXPR 1))))");
    }

    @Test
    public void testRuleExpr_spatial() throws Exception {
        runParserRule("ruleExpr", "'label' 'cytosol' A(s!1),B(x!1)   -> A(s),  B(x) @ 1", 
                "(TRANSFORM (-> (LHS (AGENTS (AGENT A (INTERFACE s (LINK 1))) (AGENT B (INTERFACE x (LINK 1))))) " +
                "(RHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x))))) " +
                "(RATE (RATEVALUE 1)) label " +
                "(LOCATION cytosol))");
        runParserRule("ruleExpr", "'label' 'cytosol' A(s!1),B(x!1)   <-> A(s),  B(x) @ 1,2", 
                "(TRANSFORM (<-> (LHS (AGENTS (AGENT A (INTERFACE s (LINK 1))) (AGENT B (INTERFACE x (LINK 1))))) " +
                "(RHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x))))) " +
                "(RATE (RATEVALUE 1) (RATEVALUE 2)) label " +
                "(LOCATION cytosol))");
        runParserRule("ruleExpr", "'label' 'cytosol'[0][1] A(s!1),B(x!1)   -> A(s),  B(x) @ 1", 
                "(TRANSFORM (-> (LHS (AGENTS (AGENT A (INTERFACE s (LINK 1))) (AGENT B (INTERFACE x (LINK 1))))) " +
                "(RHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x))))) " +
                "(RATE (RATEVALUE 1)) label " +
                "(LOCATION cytosol (INDEX (MATH_EXPR 0)) (INDEX (MATH_EXPR 1))))");
        runParserRule("ruleExpr", "'label' 'cytosol'[0][1] A(s!1),B(x!1)   <-> A(s),  B(x) @ 1,2", 
                "(TRANSFORM (<-> (LHS (AGENTS (AGENT A (INTERFACE s (LINK 1))) (AGENT B (INTERFACE x (LINK 1))))) " +
                "(RHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x))))) " +
                "(RATE (RATEVALUE 1) (RATEVALUE 2)) label " +
                "(LOCATION cytosol (INDEX (MATH_EXPR 0)) (INDEX (MATH_EXPR 1))))");
 
        runParserRule("ruleExpr", "'unbinRv{200960}' REV-ERBa(rore!1,loc~nuc), RORE(rev-erba!1,gene!_) -> REV-ERBa(rore,loc~nuc), RORE(rev-erba,gene!_) @ 21.8", 
                    "(TRANSFORM (-> (LHS (AGENTS (AGENT REV-ERBa (INTERFACE rore (LINK 1)) (INTERFACE loc (STATE nuc))) " +
                    "(AGENT RORE (INTERFACE rev-erba (LINK 1)) (INTERFACE gene (LINK OCCUPIED))))) " +
                    "(RHS (AGENTS (AGENT REV-ERBa (INTERFACE rore) (INTERFACE loc (STATE nuc))) " +
                    "(AGENT RORE (INTERFACE rev-erba) (INTERFACE gene (LINK OCCUPIED)))))) " +
                    "(RATE (RATEVALUE 21.8)) unbinRv{200960})");
    }

    @Test
    public void testAgent() throws Exception {
        runParserRule("agent", "A()", "(AGENT A)");
        runParserRule("agent", "A(x~a,l!1)", "(AGENT A (INTERFACE x (STATE a)) (INTERFACE l (LINK 1)))");
        runParserRule("agent", "A_new()", "(AGENT A_new)");
        runParserRule("agent", "A-new()", "(AGENT A-new)");
        runParserRule("agent", "EBOX-CLK-BMAL1()", "(AGENT EBOX-CLK-BMAL1)");
        runParserRule("agent", "Predator(loc~domain,loc_index_1~0,loc_index_2~0)", 
                "(AGENT Predator (INTERFACE loc (STATE domain)) (INTERFACE loc_index_1 (STATE 0)) (INTERFACE loc_index_2 (STATE 0)))");
        
    }
    
    @Test
    public void testIface() throws Exception {
        runParserRule("iface", "l", "(INTERFACE l)");
        runParserRule("iface", "l_new", "(INTERFACE l_new)");
        runParserRule("iface", "l-new", "(INTERFACE l-new)");
        runParserRule("iface", "l!1", "(INTERFACE l (LINK 1))");
        runParserRule("iface", "x~a", "(INTERFACE x (STATE a))");
    }

    @Test
    public void testId() throws Exception {
        runParserRule("id", "A", "A");
        runParserRule("id", "A_new", "A_new");
        runParserRule("id", "loc_index_2", "loc_index_2");
        runParserRule("id", "A-new", "A-new");
        runParserRule("id", "A _ new", "A _ new");
        runParserRule("id", "A - new", "A - new");
    }
    
    private void runParserRule(String rulename, String inputText, String expectedTree) throws Exception {
        runParserRule(rulename, inputText, true, expectedTree);
    }
    
    private void runParserRuleFail(String rulename, String inputText) throws Exception {
        runParserRule(rulename, inputText, false, null);
    }
    
    private final void runParserRule(String rulename, String inputText, boolean success, String expectedTree) throws Exception {
        ANTLRInputStream input = new ANTLRInputStream(new ByteArrayInputStream(inputText.getBytes()));
        CommonTokenStream tokens = new CommonTokenStream(new SpatialKappaLexer(input));
        Parser parser = new SpatialKappaParser(tokens);
        
        Method ruleMethod = SpatialKappaParser.class.getMethod(rulename, (Class[]) null);
        Object ruleOutput =  ruleMethod.invoke(parser, (Object[]) null);
        
        Method getTreeMethod = ruleOutput.getClass().getMethod("getTree", (Class[]) null);
        CommonTree tree = (CommonTree) getTreeMethod.invoke(ruleOutput, (Object[]) null);
        
        if (success) {
            assertEquals(CommonTree.class, tree.getClass());
            assertEquals(expectedTree, tree.toStringTree());
        }
        else {
            assertEquals(CommonErrorNode.class, tree.getClass());
        }
        
    }
    
}
