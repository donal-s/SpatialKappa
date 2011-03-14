package org.demonsoft.spatialkappa.parser;

import static org.junit.Assert.*;

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
                "(RATE (VAR_EXPR 1)))");
        runParserRule("ruleExpr", "'label' A(s!1),B(x!1)   -> A(s),  B(x) @ 1", 
                "(TRANSFORM (-> (LHS (AGENTS (AGENT A (INTERFACE s (LINK 1))) (AGENT B (INTERFACE x (LINK 1))))) " +
                "(RHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x))))) " +
                "(RATE (VAR_EXPR 1)) label)");
        runParserRule("ruleExpr", "'IPTG addition{77331}'  -> IPTG(laci) @ 0.0", 
                "(TRANSFORM (-> LHS (RHS (AGENTS (AGENT IPTG (INTERFACE laci))))) (RATE (VAR_EXPR 0.0)) IPTG addition{77331})");
        
        runParserRule("ruleExpr", "'bin' CRY(clk),EBOX-CLK-BMAL1(cry) -> CRY(clk!2), EBOX-CLK-BMAL1(cry!2) @ 127.286", 
                "(TRANSFORM (-> (LHS (AGENTS (AGENT CRY (INTERFACE clk)) (AGENT EBOX-CLK-BMAL1 (INTERFACE cry)))) " +
                "(RHS (AGENTS (AGENT CRY (INTERFACE clk (LINK 2))) (AGENT EBOX-CLK-BMAL1 (INTERFACE cry (LINK 2)))))) " +
                "(RATE (VAR_EXPR 127.286)) bin)");

        // <-> rules no longer valid
        runParserRuleFail("ruleExpr", "A(s!1),B(x!1)   <-> A(s),  B(x) @ 1,2");
        runParserRuleFail("ruleExpr", "'label' A(s!1),B(x!1)   <-> A(s),  B(x) @ 1,2");

    }

    @Test
    public void testTransformExpr() throws Exception {
        runParserRule("transformExpr", "A(s),B(x) -> @", 
                "(-> (LHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x)))) RHS)");
        runParserRule("transformExpr", "-> A(s!1),B(x!1)", 
                "(-> LHS (RHS (AGENTS (AGENT A (INTERFACE s (LINK 1))) (AGENT B (INTERFACE x (LINK 1))))))");
        runParserRule("transformExpr", "A(s),B(x) -> A(s!1),B(x!1)", 
                "(-> (LHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x)))) (RHS (AGENTS (AGENT A (INTERFACE s (LINK 1))) (AGENT B (INTERFACE x (LINK 1))))))");

        // <-> rules no longer valid
        runParserRuleFail("transformExpr", "A(s),B(x) <-> @");
        runParserRuleFail("transformExpr", "<-> A(s!1),B(x!1)");
        runParserRuleFail("transformExpr", "A(s),B(x) <-> A(s!1),B(x!1)");
    }

    @Test
    public void testLocationExpr() throws Exception {
        runParserRule("locationExpr", "'label'", 
                "(LOCATION label)");
        runParserRule("locationExpr", "'label'[1]", 
                "(LOCATION label (INDEX (CELL_INDEX_EXPR 1)))");
        runParserRule("locationExpr", "'label'[1][20+'x']", 
                "(LOCATION label (INDEX (CELL_INDEX_EXPR 1)) (INDEX (CELL_INDEX_EXPR + (CELL_INDEX_EXPR 20) (CELL_INDEX_EXPR x))))");
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
        runParserRule("stateExpr", "~3-_9Za", "(STATE 3-_9Za)");
        
        // Invalid characters
        runParserRule("stateExpr", "~3^a", "(STATE 3)");
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
    public void testKineticExpr() throws Exception {
        runParserRule("kineticExpr", "@ 0.1", "(RATE (VAR_EXPR 0.1))");
        runParserRule("kineticExpr", "@ 'x'", "(RATE (VAR_EXPR x))");
        runParserRule("kineticExpr", "@ 'x' / 2", "(RATE (VAR_EXPR / (VAR_EXPR x) (VAR_EXPR 2)))");
        runParserRule("kineticExpr", "@ 'x'-2", "(RATE (VAR_EXPR - (VAR_EXPR x) (VAR_EXPR 2)))");
        runParserRule("kineticExpr", "@ 'x'^'y'", "(RATE (VAR_EXPR ^ (VAR_EXPR x) (VAR_EXPR y)))");
        runParserRule("kineticExpr", "@ [inf]", "(RATE (VAR_EXPR VAR_INFINITY))");
    }

    @Test
    public void testInitExpr() throws Exception {
        runParserRule("initExpr", "%init: 5 (A(x~a),B(y~d))", 
            "(INIT (AGENTS (AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) 5)");
        runParserRule("initExpr", "%init: 'label' (A(x~a),B(y~d))", 
            "(INIT (AGENTS (AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) label)");
    }

    @Test
    public void testPlotExpr() throws Exception {
        runParserRule("plotExpr", "%plot: 'label'\n", "(PLOT label)");
        
        runParserRuleFail("plotExpr", "%plot: '\n");
    }

    @Test
    public void testObsExpr() throws Exception {
        runParserRule("obsExpr", "%obs: 'label' A(x~a),B(y~d)", 
            "(OBSERVATION (AGENTS (AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) label)");
        runParserRule("obsExpr", "%obs: A(x~a),B(y~d)", 
            "(OBSERVATION (AGENTS (AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))))");
        
        runParserRuleFail("obsExpr", "%obs: \n");
        runParserRuleFail("obsExpr", "%obs: 'label'\n");
    }

    @Test
    public void testVarExpr() throws Exception {
        runParserRule("varExpr", "%var: 'label' A(x~a),B(y~d)", 
                "(VARIABLE (AGENTS (AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) label)");
    
        runParserRule("varExpr", "%var: 'label' 2.55e4", 
                "(VARIABLE (VAR_EXPR 2.55e4) label)");
        runParserRule("varExpr", "%var: 'label' ('a' + 'b') * 2", 
                "(VARIABLE (VAR_EXPR * (VAR_EXPR + (VAR_EXPR a) (VAR_EXPR b)) (VAR_EXPR 2)) label)");
        runParserRule("varExpr", "%var: 'label' 'a' + 'b' + 2", 
                "(VARIABLE (VAR_EXPR + (VAR_EXPR + (VAR_EXPR a) (VAR_EXPR b)) (VAR_EXPR 2)) label)");
        runParserRule("varExpr", "%var: 'label' 'a' + 'b' * 2", 
                "(VARIABLE (VAR_EXPR + (VAR_EXPR a) (VAR_EXPR * (VAR_EXPR b) (VAR_EXPR 2))) label)");
        runParserRule("varExpr", "%var: 'label' [inf] * 2", 
                "(VARIABLE (VAR_EXPR * (VAR_EXPR VAR_INFINITY) (VAR_EXPR 2)) label)");
        runParserRule("varExpr", "%var: 'label' [pi] ^ 2", 
                "(VARIABLE (VAR_EXPR ^ (VAR_EXPR PI) (VAR_EXPR 2)) label)");

        runParserRule("varExpr", "%var: 'label' [log] 'n'", 
                "(VARIABLE (VAR_EXPR LOG (VAR_EXPR n)) label)");
        runParserRule("varExpr", "%var: 'label' [sin] 'n'", 
                "(VARIABLE (VAR_EXPR SIN (VAR_EXPR n)) label)");
        runParserRule("varExpr", "%var: 'label' [cos] 'n'", 
                "(VARIABLE (VAR_EXPR COS (VAR_EXPR n)) label)");
        runParserRule("varExpr", "%var: 'label' [tan] 'n'", 
                "(VARIABLE (VAR_EXPR TAN (VAR_EXPR n)) label)");
        runParserRule("varExpr", "%var: 'label' [sqrt] 'n'", 
                "(VARIABLE (VAR_EXPR SQRT (VAR_EXPR n)) label)");
        runParserRule("varExpr", "%var: 'label' [exp] 'n'", 
                "(VARIABLE (VAR_EXPR EXP (VAR_EXPR n)) label)");

        runParserRule("varExpr", "%var: 'label' [mod] 'n' 2", 
                "(VARIABLE (VAR_EXPR MODULUS (VAR_EXPR n) (VAR_EXPR 2)) label)");

        // TODO - handle the rest of the grammar as needed
        
        runParserRuleFail("varExpr", "%var: ");
//        runParserRuleFail("varExpr", "%var: A(x~a),B(y~d)"); // TODO - handle this
    }

    @Test
    public void testModExpr() throws Exception {
        runParserRule("modExpr", "%mod: ([T]>10) && ('v1'/'v2') < 1 do $ADD 'n' C(x1~p)", 
                "(PERTURBATION" +
                " (CONDITION (BOOL_EXPR AND (BOOL_EXPR > (VAR_EXPR TIME) (VAR_EXPR 10)) (BOOL_EXPR < (VAR_EXPR / (VAR_EXPR v1) (VAR_EXPR v2)) (VAR_EXPR 1))))" +
                " (EFFECT ADD (VAR_EXPR n) (AGENTS (AGENT C (INTERFACE x1 (STATE p))))))");
        runParserRule("modExpr", "%mod: ([log][E]>10) || [true] do $SNAPSHOT until [false]", 
                "(PERTURBATION" +
                " (CONDITION (BOOL_EXPR OR (BOOL_EXPR > (VAR_EXPR LOG (VAR_EXPR EVENTS)) (VAR_EXPR 10)) (BOOL_EXPR TRUE)))" +
                " (EFFECT SNAPSHOT)" +
                " (UNTIL (BOOL_EXPR FALSE)))");
        runParserRule("modExpr", "%mod: ([mod] [T] 100)=0 do $DEL [inf] C() until [T]>1000", 
                "(PERTURBATION" +
                " (CONDITION (BOOL_EXPR = (VAR_EXPR MODULUS (VAR_EXPR TIME) (VAR_EXPR 100)) (VAR_EXPR 0)))" +
                " (EFFECT REMOVE (VAR_EXPR VAR_INFINITY) (AGENTS (AGENT C)))" +
                " (UNTIL (BOOL_EXPR > (VAR_EXPR TIME) (VAR_EXPR 1000))))");
        runParserRule("modExpr", "%mod: [not][false] do 'rule_name' := [inf]", 
                "(PERTURBATION" +
                " (CONDITION (BOOL_EXPR NOT (BOOL_EXPR FALSE)))" +
                " (EFFECT SET (TARGET rule_name) (VAR_EXPR VAR_INFINITY)))");
    }
    
    @Test
    public void testBooleanExpression() throws Exception {
        runParserRule("booleanExpression", "([true])", "(BOOL_EXPR TRUE)");
        runParserRule("booleanExpression", "[false]", "(BOOL_EXPR FALSE)");
        
        runParserRule("booleanExpression", "[true] && [true] || [false]", 
            "(BOOL_EXPR OR (BOOL_EXPR AND (BOOL_EXPR TRUE) (BOOL_EXPR TRUE)) (BOOL_EXPR FALSE))");
        
        runParserRule("booleanExpression", "[true] && ([true] || [false])", 
        "(BOOL_EXPR AND (BOOL_EXPR TRUE) (BOOL_EXPR OR (BOOL_EXPR TRUE) (BOOL_EXPR FALSE)))");
        
        runParserRule("booleanExpression", "[not][true] && [false]", 
            "(BOOL_EXPR AND (BOOL_EXPR NOT (BOOL_EXPR TRUE)) (BOOL_EXPR FALSE))");
        
        runParserRule("booleanExpression", "[not]([true] && [false])", 
            "(BOOL_EXPR NOT (BOOL_EXPR AND (BOOL_EXPR TRUE) (BOOL_EXPR FALSE)))");
        
        runParserRule("booleanExpression", "[T]>10", 
            "(BOOL_EXPR > (VAR_EXPR TIME) (VAR_EXPR 10))");
        
        runParserRule("booleanExpression", "'v1'/'v2' < [E]", 
            "(BOOL_EXPR < (VAR_EXPR / (VAR_EXPR v1) (VAR_EXPR v2)) (VAR_EXPR EVENTS))");
       
        runParserRule("booleanExpression", "[T]>10 && 'v1'/'v2' < 1", 
            "(BOOL_EXPR AND (BOOL_EXPR > (VAR_EXPR TIME) (VAR_EXPR 10)) (BOOL_EXPR < (VAR_EXPR / (VAR_EXPR v1) (VAR_EXPR v2)) (VAR_EXPR 1)))");
    }
    
    @Test
    public void testEffect() throws Exception {
        runParserRule("effect", "$SNAPSHOT", "(EFFECT SNAPSHOT)");
        runParserRule("effect", "$STOP", "(EFFECT STOP)");
        
        runParserRule("effect", "$ADD 'n' + 1 C()", 
            "(EFFECT ADD (VAR_EXPR + (VAR_EXPR n) (VAR_EXPR 1)) (AGENTS (AGENT C)))");
    
        runParserRule("effect", "$DEL [inf] C(), C(x1~p)", 
            "(EFFECT REMOVE (VAR_EXPR VAR_INFINITY) (AGENTS (AGENT C) (AGENT C (INTERFACE x1 (STATE p)))))");

        runParserRule("effect", "'rule' := 'n' + 1", 
            "(EFFECT SET (TARGET rule) (VAR_EXPR + (VAR_EXPR n) (VAR_EXPR 1)))");
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
        runParserRuleFail("compartmentExpr", "%compartment:");
        runParserRuleFail("compartmentExpr", "%compartment: \"label\"[10]");
        runParserRule("compartmentExpr", "%compartment: 'label'[-1]", "(COMPARTMENT label (DIMENSION 1))");
        runParserRuleFail("compartmentExpr", "%compartment: 'label'[1.0]");
    }

    @Test
    public void testCompartmentLinkExpr() throws Exception {
        //Forward
        runParserRule("compartmentLinkExpr", "%link: 'label' 'compartment1' -> 'compartment2'", 
            "(COMPARTMENT_LINK label (LOCATION compartment1) -> (LOCATION compartment2))");
        runParserRule("compartmentLinkExpr", "%link: 'label' 'compartment1'['x'] -> 'compartment2'['x'+1]", 
            "(COMPARTMENT_LINK label (LOCATION compartment1 (INDEX (CELL_INDEX_EXPR x))) -> " +
            "(LOCATION compartment2 (INDEX (CELL_INDEX_EXPR + (CELL_INDEX_EXPR x) (CELL_INDEX_EXPR 1)))))");
        
        // Back
        runParserRule("compartmentLinkExpr", "%link: 'label' 'compartment1' <- 'compartment2'", 
            "(COMPARTMENT_LINK label (LOCATION compartment1) <- (LOCATION compartment2))");
        runParserRule("compartmentLinkExpr", "%link: 'label' 'compartment1'['x'] <- 'compartment2'['x'+1]", 
            "(COMPARTMENT_LINK label (LOCATION compartment1 (INDEX (CELL_INDEX_EXPR x))) <- " +
            "(LOCATION compartment2 (INDEX (CELL_INDEX_EXPR + (CELL_INDEX_EXPR x) (CELL_INDEX_EXPR 1)))))");
        
        // Both
        runParserRule("compartmentLinkExpr", "%link: 'label' 'compartment1' <-> 'compartment2'", 
            "(COMPARTMENT_LINK label (LOCATION compartment1) <-> (LOCATION compartment2))");
        runParserRule("compartmentLinkExpr", "%link: 'label' 'compartment1'['x'] <-> 'compartment2'['x'+1]", 
            "(COMPARTMENT_LINK label (LOCATION compartment1 (INDEX (CELL_INDEX_EXPR x))) <-> " +
            "(LOCATION compartment2 (INDEX (CELL_INDEX_EXPR + (CELL_INDEX_EXPR x) (CELL_INDEX_EXPR 1)))))");
        
        //Forward negative
        runParserRule("compartmentLinkExpr", "%link: 'label' 'compartment1'['x'] -> 'compartment2'['x'-1]", 
            "(COMPARTMENT_LINK label (LOCATION compartment1 (INDEX (CELL_INDEX_EXPR x))) -> " +
            "(LOCATION compartment2 (INDEX (CELL_INDEX_EXPR - (CELL_INDEX_EXPR x) (CELL_INDEX_EXPR 1)))))");

    }

    @Test
    public void testTransportExpr() throws Exception {
        runParserRule("transportExpr", "%transport: 'intra-cytosol' @ 0.1", 
                "(TRANSPORT intra-cytosol (RATE (VAR_EXPR 0.1)))");
        runParserRule("transportExpr", "%transport: 'transport-all' 'intra-cytosol' @ 0.1", 
                "(TRANSPORT intra-cytosol (RATE (VAR_EXPR 0.1)) transport-all)");
        runParserRule("transportExpr", "%transport: 'intra-cytosol' A(s),B(x) @ 0.1", 
                "(TRANSPORT intra-cytosol (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x))) (RATE (VAR_EXPR 0.1)))");
        runParserRule("transportExpr", "%transport: 'transport-all' 'intra-cytosol' A(s),B(x) @ 0.1", 
                "(TRANSPORT intra-cytosol (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x))) (RATE (VAR_EXPR 0.1)) transport-all)");
    }

    @Test
    public void testCompartmentReferenceExpr() throws Exception {
        runParserRule("locationExpr", "'compartment1'", "(LOCATION compartment1)");
        runParserRule("locationExpr", "'compartment1'['x']['x'+1]", 
                "(LOCATION compartment1 (INDEX (CELL_INDEX_EXPR x)) (INDEX (CELL_INDEX_EXPR + (CELL_INDEX_EXPR x) (CELL_INDEX_EXPR 1))))");
    }

    @Test
    public void testCellIndexExpr() throws Exception {
        runParserRule("cellIndexExpr", "1", "(CELL_INDEX_EXPR 1)");
        runParserRule("cellIndexExpr", "'x'", "(CELL_INDEX_EXPR x)");
        runParserRule("cellIndexExpr", "2 + 3", "(CELL_INDEX_EXPR + (CELL_INDEX_EXPR 2) (CELL_INDEX_EXPR 3))");
        runParserRule("cellIndexExpr", "2 - 3", "(CELL_INDEX_EXPR - (CELL_INDEX_EXPR 2) (CELL_INDEX_EXPR 3))");
        runParserRule("cellIndexExpr", "2 / 3", "(CELL_INDEX_EXPR / (CELL_INDEX_EXPR 2) (CELL_INDEX_EXPR 3))");
        runParserRule("cellIndexExpr", "2 * 3", "(CELL_INDEX_EXPR * (CELL_INDEX_EXPR 2) (CELL_INDEX_EXPR 3))");
        runParserRule("cellIndexExpr", "2 % 3", "(CELL_INDEX_EXPR % (CELL_INDEX_EXPR 2) (CELL_INDEX_EXPR 3))");
        runParserRule("cellIndexExpr", "2 ^ 3", "(CELL_INDEX_EXPR ^ (CELL_INDEX_EXPR 2) (CELL_INDEX_EXPR 3))");
        runParserRule("cellIndexExpr", "2-3", "(CELL_INDEX_EXPR - (CELL_INDEX_EXPR 2) (CELL_INDEX_EXPR 3))");
        runParserRule("cellIndexExpr", "2*3", "(CELL_INDEX_EXPR * (CELL_INDEX_EXPR 2) (CELL_INDEX_EXPR 3))");
        runParserRule("cellIndexExpr", "(2 * 3)", "(CELL_INDEX_EXPR * (CELL_INDEX_EXPR 2) (CELL_INDEX_EXPR 3))");
        runParserRule("cellIndexExpr", "'x' + ('y-_ is a complicated (string)!!!' * 2)", 
                "(CELL_INDEX_EXPR + (CELL_INDEX_EXPR x) (CELL_INDEX_EXPR * (CELL_INDEX_EXPR y-_ is a complicated (string)!!!) (CELL_INDEX_EXPR 2)))");
        
        // Invalid characters
        runParserRuleFail("cellIndexExpr", "x");
    }

    @Test
    public void testCellIndexAtom() throws Exception {
        runParserRule("cellIndexAtom", "1", "(CELL_INDEX_EXPR 1)");
        runParserRule("cellIndexAtom", "'x'", "(CELL_INDEX_EXPR x)");
        runParserRule("cellIndexAtom", "'y-_ is a complicated (string)!!!'", 
                "(CELL_INDEX_EXPR y-_ is a complicated (string)!!!)");
        
        // Invalid characters
        runParserRuleFail("cellIndexAtom", "x");
    }

    @Test
    public void testObsExpr_spatial() throws Exception {
        runParserRule("obsExpr", "%obs: 'label' 'cytosol' A(x~a),B(y~d)", 
            "(OBSERVATION (AGENTS (AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) label (LOCATION cytosol))");
        runParserRule("obsExpr", "%obs: 'label' 'cytosol'[0][1] A(x~a),B(y~d)", 
            "(OBSERVATION (AGENTS (AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) label " +
            "(LOCATION cytosol (INDEX (CELL_INDEX_EXPR 0)) (INDEX (CELL_INDEX_EXPR 1))))");
        
        runParserRuleFail("obsExpr", "%obs: 'cytosol'[0][1] A(x~a),B(y~d)");
        runParserRuleFail("obsExpr", "%obs: 'cytosol'[0][1]");
    }

    @Test
    public void testInitExpr_spatial() throws Exception {
        runParserRule("initExpr", "%init: 'cytosol' 5 (A(x~a),B(y~d))", 
                "(INIT (AGENTS (AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) 5 (LOCATION cytosol))");
        runParserRule("initExpr", "%init: 'cytosol'[0][1] 5 (A(x~a),B(y~d))", 
                "(INIT (AGENTS (AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) 5 " +
                "(LOCATION cytosol (INDEX (CELL_INDEX_EXPR 0)) (INDEX (CELL_INDEX_EXPR 1))))");
        
        runParserRule("initExpr", "%init: 'cytosol' 'label' (A(x~a),B(y~d))", 
                "(INIT (AGENTS (AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) label (LOCATION cytosol))");
        runParserRule("initExpr", "%init: 'cytosol'[0][1] 'label' (A(x~a),B(y~d))", 
                "(INIT (AGENTS (AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) label " +
                "(LOCATION cytosol (INDEX (CELL_INDEX_EXPR 0)) (INDEX (CELL_INDEX_EXPR 1))))");
    }

    @Test
    public void testRuleExpr_spatial() throws Exception {
        runParserRule("ruleExpr", "'label' 'cytosol' A(s!1),B(x!1)   -> A(s),  B(x) @ 1", 
                "(TRANSFORM (-> (LHS (AGENTS (AGENT A (INTERFACE s (LINK 1))) (AGENT B (INTERFACE x (LINK 1))))) " +
                "(RHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x))))) " +
                "(RATE (VAR_EXPR 1)) label " +
                "(LOCATION cytosol))");
        runParserRule("ruleExpr", "'label' 'cytosol'[0][1] A(s!1),B(x!1)   -> A(s),  B(x) @ 1", 
                "(TRANSFORM (-> (LHS (AGENTS (AGENT A (INTERFACE s (LINK 1))) (AGENT B (INTERFACE x (LINK 1))))) " +
                "(RHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x))))) " +
                "(RATE (VAR_EXPR 1)) label " +
                "(LOCATION cytosol (INDEX (CELL_INDEX_EXPR 0)) (INDEX (CELL_INDEX_EXPR 1))))");
 
        runParserRule("ruleExpr", "'unbinRv{200960}' REV-ERBa(rore!1,loc~nuc), RORE(rev-erba!1,gene!_) -> REV-ERBa(rore,loc~nuc), RORE(rev-erba,gene!_) @ 21.8", 
                    "(TRANSFORM (-> (LHS (AGENTS (AGENT REV-ERBa (INTERFACE rore (LINK 1)) (INTERFACE loc (STATE nuc))) " +
                    "(AGENT RORE (INTERFACE rev-erba (LINK 1)) (INTERFACE gene (LINK OCCUPIED))))) " +
                    "(RHS (AGENTS (AGENT REV-ERBa (INTERFACE rore) (INTERFACE loc (STATE nuc))) " +
                    "(AGENT RORE (INTERFACE rev-erba) (INTERFACE gene (LINK OCCUPIED)))))) " +
                    "(RATE (VAR_EXPR 21.8)) unbinRv{200960})");

        // <-> rules no longer valid
        runParserRuleFail("ruleExpr", "'label' 'cytosol' A(s!1),B(x!1)   <-> A(s),  B(x) @ 1,2");
        runParserRuleFail("ruleExpr", "'label' 'cytosol'[0][1] A(s!1),B(x!1)   <-> A(s),  B(x) @ 1,2");
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
        
        // Invalid characters
        runParserRuleFail("agent", "A^new()");
        runParserRule("agent", "A(x~a^b)", "(AGENT A (INTERFACE x (STATE a)))");
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
        
        // Invalid characters
        runParserRule("id", "A^new", "A");

    }
    
    @Test
    public void testLabel() throws Exception {
        runParserRule("label", "'A'", "A");
        runParserRule("label", "'2'", "2");
        runParserRule("label", "''", "");
        runParserRule("label", "'complicated__- string 234 \"£$'", "complicated__- string 234 \"£$");
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
            if (tree.getClass() == CommonTree.class) {
                assertTrue(tree.toStringTree().contains("mismatched token:") || tree.toStringTree().contains("unexpected"));
            }
            else {
                assertEquals(CommonErrorNode.class, tree.getClass());
            }
        }
        
    }
    
}
