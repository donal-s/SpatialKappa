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

//TODO - cross compartment complexes - find all matching links between compartments - distribute complexes evenly
//TODO within compartment complexes - isolate to each cell and spread evenly

public class SpatialKappaParserTest {

    @Test
    public void testRuleDecl_transform() throws Exception {
        runParserRule("ruleDecl", "A(s!1),B(x!1)   -> A(s),  B(x) @ 1", 
                "(RULE (TRANSITION (LHS (AGENTS (AGENT A (INTERFACE s (LINK 1))) (AGENT B (INTERFACE x (LINK 1))))) " +
                "(RHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x))))) " +
                "(RATE (VAR_EXPR 1)))");
        runParserRule("ruleDecl", "'label' A(s!1),B(x!1)   -> A(s),  B(x) @ 1", 
                "(RULE (TRANSITION (LHS (AGENTS (AGENT A (INTERFACE s (LINK 1))) (AGENT B (INTERFACE x (LINK 1))))) " +
                "(RHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x))))) " +
                "(RATE (VAR_EXPR 1)) label)");
        runParserRule("ruleDecl", "'IPTG addition{77331}'  -> IPTG(laci) @ 0.0", 
                "(RULE (TRANSITION LHS (RHS (AGENTS (AGENT IPTG (INTERFACE laci))))) (RATE (VAR_EXPR 0.0)) IPTG addition{77331})");
        
        runParserRule("ruleDecl", "'bin' CRY(clk),EBOX-CLK-BMAL1(cry) -> CRY(clk!2), EBOX-CLK-BMAL1(cry!2) @ 127.286", 
                "(RULE (TRANSITION (LHS (AGENTS (AGENT CRY (INTERFACE clk)) (AGENT EBOX-CLK-BMAL1 (INTERFACE cry)))) " +
                "(RHS (AGENTS (AGENT CRY (INTERFACE clk (LINK 2))) (AGENT EBOX-CLK-BMAL1 (INTERFACE cry (LINK 2)))))) " +
                "(RATE (VAR_EXPR 127.286)) bin)");

        runParserRule("ruleDecl", "'AB embed'    A:cytosol[0](s!1),B:cytosol[0](s!1) ->:intra A:cytosol[1](s!1:intra),B:cytosol[0](s!1) @ 1.0", 
                "(RULE (TRANSITION (LHS (AGENTS (AGENT A (LOCATION cytosol (INDEX (CELL_INDEX_EXPR 0))) (INTERFACE s (LINK 1))) " +
                "(AGENT B (LOCATION cytosol (INDEX (CELL_INDEX_EXPR 0))) (INTERFACE s (LINK 1))))) " +
                "(RHS (AGENTS (AGENT A (LOCATION cytosol (INDEX (CELL_INDEX_EXPR 1))) (INTERFACE s (LINK (CHANNEL intra) 1))) " +
                "(AGENT B (LOCATION cytosol (INDEX (CELL_INDEX_EXPR 0))) (INTERFACE s (LINK 1))))) (CHANNEL intra)) " +
                "(RATE (VAR_EXPR 1.0)) AB embed)");

        // <-> rules no longer valid
//        runParserRuleFail("ruleDecl", "A(s!1),B(x!1)   <-> A(s),  B(x) @ 1,2");
//        runParserRuleFail("ruleDecl", "'label' A(s!1),B(x!1)   <-> A(s),  B(x) @ 1,2");

    }

    @Test
    public void testTransition_transform() throws Exception {
        runParserRule("transition", "A(s),B(x) -> @", 
                "(TRANSITION (LHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x)))) RHS)");
        runParserRule("transition", "-> A(s!1),B(x!1)", 
                "(TRANSITION LHS (RHS (AGENTS (AGENT A (INTERFACE s (LINK 1))) (AGENT B (INTERFACE x (LINK 1))))))");
        runParserRule("transition", "A(s),B(x) -> A(s!1),B(x!1)", 
                "(TRANSITION (LHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x)))) (RHS (AGENTS (AGENT A (INTERFACE s (LINK 1))) (AGENT B (INTERFACE x (LINK 1))))))");

        // <-> rules no longer valid
//        runParserRuleFail("transition", "A(s),B(x) <-> @");
        runParserRuleFail("transition", "<-> A(s!1),B(x!1)");
//        runParserRuleFail("transition", "A(s),B(x) <-> A(s!1),B(x!1)");
    }

    @Test
    public void testTransition_transport() throws Exception {
        runParserRule("transition", "->:intra-cytosol @", 
                "(TRANSITION LHS RHS (CHANNEL intra-cytosol))");
        runParserRule("transition", "A(s),B(x) ->:intra-cytosol A(s),B(x)", 
                "(TRANSITION (LHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x)))) " +
                "(RHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x)))) " +
                "(CHANNEL intra-cytosol))");
        runParserRule("transition", "A:cytosol[0](s!1),B:cytosol[0](s!1) ->:intra A:cytosol[1](s!1:intra),B:cytosol[0](s!1)", 
                "(TRANSITION (LHS (AGENTS (AGENT A (LOCATION cytosol (INDEX (CELL_INDEX_EXPR 0))) (INTERFACE s (LINK 1))) " +
                "(AGENT B (LOCATION cytosol (INDEX (CELL_INDEX_EXPR 0))) (INTERFACE s (LINK 1))))) " +
                "(RHS (AGENTS (AGENT A (LOCATION cytosol (INDEX (CELL_INDEX_EXPR 1))) (INTERFACE s (LINK (CHANNEL intra) 1))) " +
                "(AGENT B (LOCATION cytosol (INDEX (CELL_INDEX_EXPR 0))) (INTERFACE s (LINK 1))))) (CHANNEL intra))");
    }

    @Test
    public void testRuleDecl_transform_spatial() throws Exception {
        runParserRule("ruleDecl", "'label' :cytosol A:membrane(s!1),B(x!1)   -> A(s),  B(x) @ 1", 
                "(RULE (TRANSITION (LHS (AGENTS (LOCATION cytosol) (AGENT A (LOCATION membrane) (INTERFACE s (LINK 1))) (AGENT B (INTERFACE x (LINK 1))))) " +
                "(RHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x))))) " +
                "(RATE (VAR_EXPR 1)) label)");
        runParserRule("ruleDecl", ":cytosol A(s!1),B(x!1)   -> A(s),  B(x) @ 1", 
                "(RULE (TRANSITION (LHS (AGENTS (LOCATION cytosol) (AGENT A (INTERFACE s (LINK 1))) (AGENT B (INTERFACE x (LINK 1))))) " +
                "(RHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x))))) " +
                "(RATE (VAR_EXPR 1)))");
        runParserRule("ruleDecl", "'label' :cytosol[0][1] A(s!1),B(x!1)   -> A(s),  B(x) @ 1", 
                "(RULE (TRANSITION (LHS (AGENTS (LOCATION cytosol (INDEX (CELL_INDEX_EXPR 0)) (INDEX (CELL_INDEX_EXPR 1))) " +
                "(AGENT A (INTERFACE s (LINK 1))) (AGENT B (INTERFACE x (LINK 1))))) " +
                "(RHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x))))) " +
                "(RATE (VAR_EXPR 1)) label)");
 
        runParserRule("ruleDecl", "'unbinRv{200960}' REV-ERBa(rore!1,loc~nuc), RORE(rev-erba!1,gene!_) -> REV-ERBa(rore,loc~nuc), RORE(rev-erba,gene!_) @ 21.8", 
                    "(RULE (TRANSITION (LHS (AGENTS (AGENT REV-ERBa (INTERFACE rore (LINK 1)) (INTERFACE loc (STATE nuc))) " +
                    "(AGENT RORE (INTERFACE rev-erba (LINK 1)) (INTERFACE gene (LINK OCCUPIED))))) " +
                    "(RHS (AGENTS (AGENT REV-ERBa (INTERFACE rore) (INTERFACE loc (STATE nuc))) " +
                    "(AGENT RORE (INTERFACE rev-erba) (INTERFACE gene (LINK OCCUPIED)))))) " +
                    "(RATE (VAR_EXPR 21.8)) unbinRv{200960})");

        // <-> rules no longer valid
        runParserRuleFail("ruleDecl", "'label' cytosol A(s!1),B(x!1)   <-> A(s),  B(x) @ 1,2");
        runParserRuleFail("ruleDecl", "'label' cytosol[0][1] A(s!1),B(x!1)   <-> A(s),  B(x) @ 1,2");
    }

    @Test
    public void testRuleDecl_transport() throws Exception {
        runParserRule("ruleDecl", "->:intra-cytosol @ 0.1", 
                "(RULE (TRANSITION LHS RHS " +
                "(CHANNEL intra-cytosol)) " +
                "(RATE (VAR_EXPR 0.1)))");
        runParserRule("ruleDecl", "'transport-all' ->:intra-cytosol @ 0.1", 
                "(RULE (TRANSITION LHS RHS " +
                "(CHANNEL intra-cytosol)) " +
                "(RATE (VAR_EXPR 0.1)) transport-all)");
        runParserRule("ruleDecl", "A(s),B(x) ->:intra-cytosol A(s),B(x) @ 0.1", 
                "(RULE (TRANSITION (LHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x)))) " +
                "(RHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x)))) " +
                "(CHANNEL intra-cytosol)) " +
                "(RATE (VAR_EXPR 0.1)))");
        runParserRule("ruleDecl", "'transport-all' A(s),B(x) ->:intra-cytosol A(s),B(x) @ 0.1", 
                "(RULE (TRANSITION (LHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x)))) " +
                "(RHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x)))) " +
                "(CHANNEL intra-cytosol)) " +
                "(RATE (VAR_EXPR 0.1)) transport-all)");

        // With rule locations
        runParserRule("ruleDecl", ":source ->:intra-cytosol @ 0.1", 
                "(RULE (TRANSITION (LHS (LOCATION source)) RHS " +
                "(CHANNEL intra-cytosol)) " +
                "(RATE (VAR_EXPR 0.1)))");
        runParserRule("ruleDecl", "'transport-all' ->:intra-cytosol :target @ 0.1", 
                "(RULE (TRANSITION LHS (RHS (LOCATION target)) " +
                "(CHANNEL intra-cytosol)) " +
                "(RATE (VAR_EXPR 0.1)) transport-all)");
        runParserRule("ruleDecl", "A(s),B(x) ->:intra-cytosol :target A(s),B(x) @ 0.1", 
                "(RULE (TRANSITION (LHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x)))) " +
                "(RHS (AGENTS (LOCATION target) (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x)))) " +
                "(CHANNEL intra-cytosol)) " +
                "(RATE (VAR_EXPR 0.1)))");
        runParserRule("ruleDecl", "'transport-all' :source A(s),B(x) ->:intra-cytosol A(s),B(x) @ 0.1", 
                "(RULE (TRANSITION (LHS (AGENTS (LOCATION source) (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x)))) " +
                "(RHS (AGENTS (AGENT A (INTERFACE s)) (AGENT B (INTERFACE x)))) " +
                "(CHANNEL intra-cytosol)) " +
                "(RATE (VAR_EXPR 0.1)) transport-all)");

        // Show difference between rule channel and target location syntax
        runParserRuleFail("ruleDecl", "'transport-all' -> :target @ 0.1");
        
        // With variable rate
        runParserRule("ruleDecl", "->:intra-cytosol @ 3 / |A,B|", 
                "(RULE (TRANSITION LHS RHS " +
                "(CHANNEL intra-cytosol)) " +
                "(RATE (VAR_EXPR / (VAR_EXPR 3) (VAR_EXPR (AGENTS (AGENT A) (AGENT B))))))");
    }

    @Test
    public void testLocation() throws Exception {
        runParserRule("location", ":label", 
                "(LOCATION label)");
        runParserRule("location", ":fixed", 
                "(LOCATION FIXED)");
        runParserRule("location", ":label[1]", 
                "(LOCATION label (INDEX (CELL_INDEX_EXPR 1)))");
        runParserRule("location", ":label[1][20+x]", 
                "(LOCATION label (INDEX (CELL_INDEX_EXPR 1)) (INDEX (CELL_INDEX_EXPR + (CELL_INDEX_EXPR 20) (CELL_INDEX_EXPR x))))");
    }

    @Test
    public void testAgentGroup() throws Exception {
        runParserRule("agentGroup", "A()", "(AGENTS (AGENT A))");
        runParserRule("agentGroup", "A(x~a,l!1),B(y~b,m!1)", 
                "(AGENTS (AGENT A (INTERFACE x (STATE a)) (INTERFACE l (LINK 1))) (AGENT B (INTERFACE y (STATE b)) (INTERFACE m (LINK 1))))");
        
        runParserRule("agentGroup", ":loc1 A(),B:loc2[1]()", 
                "(AGENTS (LOCATION loc1) (AGENT A) (AGENT B (LOCATION loc2 (INDEX (CELL_INDEX_EXPR 1)))))");

        // With parentheses TODO
//        runParserRule("agentGroup", "(A())", "(AGENTS (AGENT A))");
//        runParserRule("agentGroup", "((A(x~a,l!1),B(y~b,m!1)))", 
//                "(AGENTS (AGENT A (INTERFACE x (STATE a)) (INTERFACE l (LINK 1))) (AGENT B (INTERFACE y (STATE b)) (INTERFACE m (LINK 1))))");
    }

    @Test
    public void testState() throws Exception {
        runParserRule("state", "~a", "(STATE a)");
        runParserRule("state", "~3-_9Za", "(STATE 3-_9Za)");
        
        // Invalid characters
        runParserRule("state", "~3^a", "(STATE 3)");
    }

    @Test
    public void testLink() throws Exception {
        runParserRule("link", "! 0", "(LINK 0)");
        runParserRule("link", "! 1", "(LINK 1)");
        runParserRule("link", "!1", "(LINK 1)");
        runParserRule("link", "?", "(LINK ANY)");
        runParserRule("link", "!_", "(LINK OCCUPIED)");
        runParserRule("link", "! _", "(LINK OCCUPIED)");
        
        runParserRuleFail("link", "! -1");
        runParserRuleFail("link", "!");
        runParserRuleFail("link", "! 0.1");
        runParserRuleFail("link", "! a");
    }

    @Test
    public void testLink_withNamedChannel() throws Exception {
        runParserRule("link", "! 0:channel", "(LINK (CHANNEL channel) 0)");
        runParserRule("link", "! 1:channel", "(LINK (CHANNEL channel) 1)");
        runParserRule("link", "!1:channel", "(LINK (CHANNEL channel) 1)");
        runParserRule("link", "!_:channel", "(LINK (CHANNEL channel) OCCUPIED)");
        runParserRule("link", "! _:channel", "(LINK (CHANNEL channel) OCCUPIED)");

        // Suffix will be caught by next invoked rule
        runParserRule("link", "?:channel", "(LINK ANY)");
        runParserRule("link", "?channel", "(LINK ANY)");

        runParserRuleFail("link", ":");
        runParserRuleFail("link", ":channel");
        runParserRuleFail("link", "!:channel");
        runParserRuleFail("link", "! -1:channel");
        runParserRuleFail("link", "!:channel");
        runParserRuleFail("link", "! 0.1:channel");
        runParserRuleFail("link", "! a:channel");
    }

    @Test
    public void testRate() throws Exception {
        runParserRule("rate", "@ 0.1", "(RATE (VAR_EXPR 0.1))");
        runParserRule("rate", "@ 'x'", "(RATE (VAR_EXPR x))");
        runParserRule("rate", "@ 'x' / 2", "(RATE (VAR_EXPR / (VAR_EXPR x) (VAR_EXPR 2)))");
        runParserRule("rate", "@ 'x'-2", "(RATE (VAR_EXPR - (VAR_EXPR x) (VAR_EXPR 2)))");
        runParserRule("rate", "@ 'x'^'y'", "(RATE (VAR_EXPR ^ (VAR_EXPR x) (VAR_EXPR y)))");
        runParserRule("rate", "@ [inf]", "(RATE (VAR_EXPR VAR_INFINITY))");
    }

    @Test
    public void testInitDecl() throws Exception {
        runParserRule("initDecl", "%init: 5 A(x~a),B(y~d)", 
                "(INIT (AGENTS (AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) 5)");
//        runParserRule("initDecl", "%init: 5 (A(x~a),B(y~d))", TODO
//                "(INIT (AGENTS (AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) 5)");
        runParserRule("initDecl", "%init: 'label' A(x~a),B(y~d)", 
            "(INIT (AGENTS (AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) label)");
    }

    @Test
    public void testAgentDecl() throws Exception {
        runParserRule("agentDecl", "%agent: Agent", "(AGENT_DECL Agent)");
        runParserRule("agentDecl", "%agent: Agent()", "(AGENT_DECL Agent)");
        runParserRule("agentDecl", "%agent: Agent(none,single~value,multiple~val1~val2~val3)", 
        		"(AGENT_DECL Agent (INTERFACE none) (INTERFACE single (STATE value)) " +
        		"(INTERFACE multiple (STATE val1) (STATE val2) (STATE val3)))");
    }
    

    @Test
    public void testPlotDecl() throws Exception {
        runParserRule("plotDecl", "%plot: 'label'\n", "(PLOT label)");
        
        runParserRuleFail("plotDecl", "%plot: '\n");
    }

    @Test
    public void testObsDecl() throws Exception {
        runParserRule("obsDecl", "%obs: 'label' A(x~a),B(y~d)", 
            "(OBSERVATION (AGENTS (AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) label)");
        runParserRule("obsDecl", "%obs: A(x~a),B(y~d)", 
            "(OBSERVATION (AGENTS (AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))))");
        
        runParserRuleFail("obsDecl", "%obs: \n");
        runParserRuleFail("obsDecl", "%obs: 'label'\n");
    }

    @Test
    public void testVarDecl() throws Exception {
        runParserRule("varDecl", "%var: 'label' A(x~a),B(y~d)", 
                "(VARIABLE (AGENTS (AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) label)");
    
        runParserRule("varDecl", "%var: 'label' :cytosol A(x~a),B(y~d)", 
                "(VARIABLE (AGENTS (LOCATION cytosol) " +
                "(AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) label)");
    
        runParserRule("varDecl", "%var: voxel 'label' :cytosol A(x~a),B(y~d)", 
                "(VARIABLE VOXEL (AGENTS (LOCATION cytosol) " +
                "(AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) label)");
    
        runParserRule("varDecl", "%var: 'label' 800", 
                "(VARIABLE (VAR_EXPR 800) label)");
        
        runParserRule("varDecl", "%var: 'label' [pi] ^ 2", 
                "(VARIABLE (VAR_EXPR ^ (VAR_EXPR PI) (VAR_EXPR 2)) label)");

        runParserRule("varDecl", "%var: 'label' [log] 'n'", 
                "(VARIABLE (VAR_EXPR LOG (VAR_EXPR n)) label)");

        runParserRuleFail("varDecl", "%var: ");
//        runParserRuleFail("varDecl", "%var: A(x~a),B(y~d)"); // TODO - handle this
    }
    
    @Test
    public void testVarAlgebraExpr() throws Exception {
        runParserRule("varAlgebraExpr", "800", 
                "(VAR_EXPR 800)");
        
        runParserRule("varAlgebraExpr", "2.55e4", 
                "(VAR_EXPR 2.55e4)");
        runParserRule("varAlgebraExpr", "('a' + 'b') * 2", 
                "(VAR_EXPR * (VAR_EXPR + (VAR_EXPR a) (VAR_EXPR b)) (VAR_EXPR 2))");
        runParserRule("varAlgebraExpr", "'a' + 'b' + 2", 
                "(VAR_EXPR + (VAR_EXPR + (VAR_EXPR a) (VAR_EXPR b)) (VAR_EXPR 2))");
        runParserRule("varAlgebraExpr", "'a' + 'b' * 2", 
                "(VAR_EXPR + (VAR_EXPR a) (VAR_EXPR * (VAR_EXPR b) (VAR_EXPR 2)))");
        runParserRule("varAlgebraExpr", "[inf] * 2", 
                "(VAR_EXPR * (VAR_EXPR VAR_INFINITY) (VAR_EXPR 2))");
        runParserRule("varAlgebraExpr", "[pi] ^ 2", 
                "(VAR_EXPR ^ (VAR_EXPR PI) (VAR_EXPR 2))");

        runParserRule("varAlgebraExpr", "[log] 'n'", 
                "(VAR_EXPR LOG (VAR_EXPR n))");
        runParserRule("varAlgebraExpr", "[sin] 'n'", 
                "(VAR_EXPR SIN (VAR_EXPR n))");
        runParserRule("varAlgebraExpr", "[cos] 'n'", 
                "(VAR_EXPR COS (VAR_EXPR n))");
        runParserRule("varAlgebraExpr", "[tan] 'n'", 
                "(VAR_EXPR TAN (VAR_EXPR n))");
        runParserRule("varAlgebraExpr", "[sqrt] 'n'", 
                "(VAR_EXPR SQRT (VAR_EXPR n))");
        runParserRule("varAlgebraExpr", "[exp] 'n'", 
                "(VAR_EXPR EXP (VAR_EXPR n))");

        runParserRule("varAlgebraExpr", "'n' [mod] 2", 
                "(VAR_EXPR MODULUS (VAR_EXPR n) (VAR_EXPR 2))");

        runParserRule("varAlgebraExpr", "|A|", 
                "(VAR_EXPR (AGENTS (AGENT A)))");

        runParserRule("varAlgebraExpr", "3 / |A,B|", 
                "(VAR_EXPR / (VAR_EXPR 3) (VAR_EXPR (AGENTS (AGENT A) (AGENT B))))");

        runParserRule("varAlgebraExpr", "|:loc1 A,B:loc2[1]()|", 
                "(VAR_EXPR (AGENTS (LOCATION loc1) (AGENT A) (AGENT B (LOCATION loc2 (INDEX (CELL_INDEX_EXPR 1))))))");
    }

    @Test
    public void testProg_withLineComments() throws Exception {
    	
        runParserRule("prog", "%var: 'kp1' 1.667e-06\n" + 
        		"%var: 'km1' 0.06\n", 
                "(VARIABLE (VAR_EXPR 1.667e-06) kp1) (VARIABLE (VAR_EXPR 0.06) km1)");
        
        runParserRule("prog", "%var: 'kp1' 1.667e-06 # ligand-monomer binding (scaled)\n" + 
        		"%var: 'km1' 0.06 # ligand-monomer dissociation\n", 
                "(VARIABLE (VAR_EXPR 1.667e-06) kp1) (VARIABLE (VAR_EXPR 0.06) km1)");
    }
    
    @Test
    public void testProg_emptyInput() throws Exception {
        runParserRule("prog", "", null);
        runParserRule("prog", "\n", null);
        runParserRule("prog", "# comment", null);
        runParserRule("prog", "# comment\n", null);
    }
    
    @Test
    public void testModDecl() throws Exception {
        runParserRule("modDecl", "%mod: ([T]>10) && ('v1'/'v2') < 1 do $ADD 'n' C(x1~p)", 
                "(PERTURBATION" +
                " (CONDITION (BOOL_EXPR AND (BOOL_EXPR > (VAR_EXPR TIME) (VAR_EXPR 10)) (BOOL_EXPR < (VAR_EXPR / (VAR_EXPR v1) (VAR_EXPR v2)) (VAR_EXPR 1))))" +
                " (EFFECT ADD (VAR_EXPR n) (AGENTS (AGENT C (INTERFACE x1 (STATE p))))))");
        runParserRule("modDecl", "%mod: ([log][E]>10) || [true] do $SNAPSHOT until [false]", 
                "(PERTURBATION" +
                " (CONDITION (BOOL_EXPR OR (BOOL_EXPR > (VAR_EXPR LOG (VAR_EXPR EVENTS)) (VAR_EXPR 10)) (BOOL_EXPR TRUE)))" +
                " (EFFECT SNAPSHOT)" +
                " (UNTIL (BOOL_EXPR FALSE)))");
        runParserRule("modDecl", "%mod: ([T] [mod] 100)=0 do $DEL [inf] C() until [T]>1000", 
                "(PERTURBATION" +
                " (CONDITION (BOOL_EXPR = (VAR_EXPR MODULUS (VAR_EXPR TIME) (VAR_EXPR 100)) (VAR_EXPR 0)))" +
                " (EFFECT REMOVE (VAR_EXPR VAR_INFINITY) (AGENTS (AGENT C)))" +
                " (UNTIL (BOOL_EXPR > (VAR_EXPR TIME) (VAR_EXPR 1000))))");
        runParserRule("modDecl", "%mod: [not][false] do 'rule_name' := [inf]", 
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
    public void testCompartmentDecl() throws Exception {
        runParserRule("compartmentDecl", "%compartment: label", "(COMPARTMENT label)");
        runParserRule("compartmentDecl", "%compartment: label[1]", "(COMPARTMENT label (DIMENSION 1))");
        runParserRule("compartmentDecl", "%compartment: label[0]", "(COMPARTMENT label (DIMENSION 0))");
        runParserRule("compartmentDecl", "%compartment: label [1][20]", "(COMPARTMENT label (DIMENSION 1) (DIMENSION 20))");
        runParserRule("compartmentDecl", "%compartment: 0_complex1Label-with-Stuff", "(COMPARTMENT 0_complex1Label-with-Stuff)");
        
        // Shapes
        runParserRule("compartmentDecl", "%compartment: label type", 
                "(COMPARTMENT label (TYPE type))");
        runParserRule("compartmentDecl", "%compartment: label type [10]", 
                "(COMPARTMENT label (TYPE type) (DIMENSION 10))");
        runParserRule("compartmentDecl", "%compartment: label type [10][5] [2][3]", 
                "(COMPARTMENT label (TYPE type) (DIMENSION 10) (DIMENSION 5) (DIMENSION 2) (DIMENSION 3))");
        
        // Error cases - TODO have them throw exceptions
        runParserRuleFail("compartmentDecl", "%compartment:");
        runParserRuleFail("compartmentDecl", "%compartment: \'label\'[10]");
//        runParserRuleFail("compartmentDecl", "%compartment: \"label\"[10]"); //TODO check
        runParserRule("compartmentDecl", "%compartment: label[-1]", "(COMPARTMENT label (DIMENSION 1))");
        runParserRuleFail("compartmentDecl", "%compartment: label[1.0]");
    }

    @Test
    public void testChannelDecl() throws Exception {
        //Forward
        runParserRule("channelDecl", "%channel: label :compartment1 -> :compartment2", 
            "(CHANNEL label (LOCATION_PAIR (LOCATIONS (LOCATION compartment1)) (LOCATIONS (LOCATION compartment2))))");
        runParserRule("channelDecl", "%channel: label :compartment1[x] -> :compartment2[x+1]", 
            "(CHANNEL label (LOCATION_PAIR (LOCATIONS (LOCATION compartment1 (INDEX (CELL_INDEX_EXPR x)))) " +
            "(LOCATIONS (LOCATION compartment2 (INDEX (CELL_INDEX_EXPR + (CELL_INDEX_EXPR x) (CELL_INDEX_EXPR 1)))))))");
            
        runParserRule("channelDecl", "%channel: label (:compartment1[x] -> :compartment2[x+1])", 
            "(CHANNEL label (LOCATION_PAIR (LOCATIONS (LOCATION compartment1 (INDEX (CELL_INDEX_EXPR x)))) " +
            "(LOCATIONS (LOCATION compartment2 (INDEX (CELL_INDEX_EXPR + (CELL_INDEX_EXPR x) (CELL_INDEX_EXPR 1)))))))");
            
        runParserRule("channelDecl", "%channel: label (:compartment1[x] -> :compartment2[x+1]) + " +
                "(:compartment1[x] -> :compartment2[x - 1])", 
                "(CHANNEL label " +
                "(LOCATION_PAIR (LOCATIONS (LOCATION compartment1 (INDEX (CELL_INDEX_EXPR x)))) " +
                "(LOCATIONS (LOCATION compartment2 (INDEX (CELL_INDEX_EXPR + (CELL_INDEX_EXPR x) (CELL_INDEX_EXPR 1)))))) " +
                "(LOCATION_PAIR (LOCATIONS (LOCATION compartment1 (INDEX (CELL_INDEX_EXPR x)))) " +
                "(LOCATIONS (LOCATION compartment2 (INDEX (CELL_INDEX_EXPR - (CELL_INDEX_EXPR x) (CELL_INDEX_EXPR 1))))))" +
                ")");
                
        // Multi agent channels
        runParserRule("channelDecl", "%channel: diffusion" +
        		"        (:membrane [x][y], :cytosol [u][v][0] -> :membrane [x+1][y], :cytosol [u+1][v][0]) + \\\n" + 
        		"        (:membrane [x][y], :cytosol [u][v][0] -> :membrane [x -1][y], :cytosol [u -1][v][0])", 
                "(CHANNEL diffusion " +
                "(LOCATION_PAIR " +
                "(LOCATIONS " +
                "(LOCATION membrane (INDEX (CELL_INDEX_EXPR x)) (INDEX (CELL_INDEX_EXPR y))) " +
                "(LOCATION cytosol (INDEX (CELL_INDEX_EXPR u)) (INDEX (CELL_INDEX_EXPR v)) (INDEX (CELL_INDEX_EXPR 0)))) " +
                "(LOCATIONS " +
                "(LOCATION membrane (INDEX (CELL_INDEX_EXPR + (CELL_INDEX_EXPR x) (CELL_INDEX_EXPR 1))) (INDEX (CELL_INDEX_EXPR y))) " +
                "(LOCATION cytosol (INDEX (CELL_INDEX_EXPR + (CELL_INDEX_EXPR u) (CELL_INDEX_EXPR 1))) (INDEX (CELL_INDEX_EXPR v)) (INDEX (CELL_INDEX_EXPR 0))))) " +
                "(LOCATION_PAIR " +
                "(LOCATIONS " +
                "(LOCATION membrane (INDEX (CELL_INDEX_EXPR x)) (INDEX (CELL_INDEX_EXPR y))) " +
                "(LOCATION cytosol (INDEX (CELL_INDEX_EXPR u)) (INDEX (CELL_INDEX_EXPR v)) (INDEX (CELL_INDEX_EXPR 0)))) " +
                "(LOCATIONS " +
                "(LOCATION membrane (INDEX (CELL_INDEX_EXPR - (CELL_INDEX_EXPR x) (CELL_INDEX_EXPR 1))) (INDEX (CELL_INDEX_EXPR y))) " +
                "(LOCATION cytosol (INDEX (CELL_INDEX_EXPR - (CELL_INDEX_EXPR u) (CELL_INDEX_EXPR 1))) (INDEX (CELL_INDEX_EXPR v)) (INDEX (CELL_INDEX_EXPR 0)))))" +
                ")");
                
        // Predefined types
        runParserRule("channelDecl", "%channel: label type :compartment1 -> :compartment2", 
            "(CHANNEL label (LOCATION_PAIR (TYPE type) (LOCATIONS (LOCATION compartment1)) (LOCATIONS (LOCATION compartment2))))");
        runParserRule("channelDecl", "%channel: label type :compartment1[x] -> :compartment2[x+1]", 
            "(CHANNEL label (LOCATION_PAIR (TYPE type) (LOCATIONS (LOCATION compartment1 (INDEX (CELL_INDEX_EXPR x)))) " +
            "(LOCATIONS (LOCATION compartment2 (INDEX (CELL_INDEX_EXPR + (CELL_INDEX_EXPR x) (CELL_INDEX_EXPR 1)))))))");
            
        runParserRule("channelDecl", "%channel: label (type :compartment1[x] -> :compartment2[x+1])", 
            "(CHANNEL label (LOCATION_PAIR (TYPE type) (LOCATIONS (LOCATION compartment1 (INDEX (CELL_INDEX_EXPR x)))) " +
            "(LOCATIONS (LOCATION compartment2 (INDEX (CELL_INDEX_EXPR + (CELL_INDEX_EXPR x) (CELL_INDEX_EXPR 1)))))))");
            
        runParserRule("channelDecl", "%channel: label (type :compartment1 -> :compartment2) + " +
                "(:compartment1[x] -> :compartment2[x - 1])", 
                "(CHANNEL label " +
                "(LOCATION_PAIR (TYPE type) (LOCATIONS (LOCATION compartment1)) " +
                "(LOCATIONS (LOCATION compartment2))) " +
                "(LOCATION_PAIR (LOCATIONS (LOCATION compartment1 (INDEX (CELL_INDEX_EXPR x)))) " +
                "(LOCATIONS (LOCATION compartment2 (INDEX (CELL_INDEX_EXPR - (CELL_INDEX_EXPR x) (CELL_INDEX_EXPR 1))))))" +
                ")");
                
        runParserRule("channelDecl", "%channel: diffusion" +
                "        (type :membrane [x][y], :cytosol [u][v][0] -> :membrane [x+1][y], :cytosol [u+1][v][0]) + \\\n" + 
                "        (type :membrane [x][y], :cytosol [u][v][0] -> :membrane [x -1][y], :cytosol [u -1][v][0])", 
                "(CHANNEL diffusion " +
                "(LOCATION_PAIR (TYPE type) " +
                "(LOCATIONS " +
                "(LOCATION membrane (INDEX (CELL_INDEX_EXPR x)) (INDEX (CELL_INDEX_EXPR y))) " +
                "(LOCATION cytosol (INDEX (CELL_INDEX_EXPR u)) (INDEX (CELL_INDEX_EXPR v)) (INDEX (CELL_INDEX_EXPR 0)))) " +
                "(LOCATIONS " +
                "(LOCATION membrane (INDEX (CELL_INDEX_EXPR + (CELL_INDEX_EXPR x) (CELL_INDEX_EXPR 1))) (INDEX (CELL_INDEX_EXPR y))) " +
                "(LOCATION cytosol (INDEX (CELL_INDEX_EXPR + (CELL_INDEX_EXPR u) (CELL_INDEX_EXPR 1))) (INDEX (CELL_INDEX_EXPR v)) (INDEX (CELL_INDEX_EXPR 0))))) " +
                "(LOCATION_PAIR (TYPE type) " +
                "(LOCATIONS " +
                "(LOCATION membrane (INDEX (CELL_INDEX_EXPR x)) (INDEX (CELL_INDEX_EXPR y))) " +
                "(LOCATION cytosol (INDEX (CELL_INDEX_EXPR u)) (INDEX (CELL_INDEX_EXPR v)) (INDEX (CELL_INDEX_EXPR 0)))) " +
                "(LOCATIONS " +
                "(LOCATION membrane (INDEX (CELL_INDEX_EXPR - (CELL_INDEX_EXPR x) (CELL_INDEX_EXPR 1))) (INDEX (CELL_INDEX_EXPR y))) " +
                "(LOCATION cytosol (INDEX (CELL_INDEX_EXPR - (CELL_INDEX_EXPR u) (CELL_INDEX_EXPR 1))) (INDEX (CELL_INDEX_EXPR v)) (INDEX (CELL_INDEX_EXPR 0)))))" +
                ")");
        
        
        runParserRuleFail("channelDecl", "%channel: label compartment1 -> compartment2");
        runParserRuleFail("channelDecl", "%channel: label :compartment1 <- :compartment2");
        runParserRuleFail("channelDecl", "%channel: label ()");
                
        runParserRuleFail("channelDecl", "%channel: label (:compartment1[x] -> :compartment2[x+1]) + ()");
        runParserRuleFail("channelDecl", "%channel: label (:compartment1[x] -> :compartment2[x+1]) + ");
        runParserRuleFail("channelDecl", "%channel: label + (:compartment1[x] -> :compartment2[x+1])");
    }

    @Test
    public void testCellIndexExpr() throws Exception {
        runParserRule("cellIndexExpr", "1", "(CELL_INDEX_EXPR 1)");
        runParserRule("cellIndexExpr", "x", "(CELL_INDEX_EXPR x)");
        runParserRule("cellIndexExpr", "2 + 3", "(CELL_INDEX_EXPR + (CELL_INDEX_EXPR 2) (CELL_INDEX_EXPR 3))");
        runParserRule("cellIndexExpr", "2 - 3", "(CELL_INDEX_EXPR - (CELL_INDEX_EXPR 2) (CELL_INDEX_EXPR 3))");
        runParserRule("cellIndexExpr", "2 / 3", "(CELL_INDEX_EXPR / (CELL_INDEX_EXPR 2) (CELL_INDEX_EXPR 3))");
        runParserRule("cellIndexExpr", "2 * 3", "(CELL_INDEX_EXPR * (CELL_INDEX_EXPR 2) (CELL_INDEX_EXPR 3))");
        runParserRule("cellIndexExpr", "2 % 3", "(CELL_INDEX_EXPR % (CELL_INDEX_EXPR 2) (CELL_INDEX_EXPR 3))");
        runParserRule("cellIndexExpr", "2 ^ 3", "(CELL_INDEX_EXPR ^ (CELL_INDEX_EXPR 2) (CELL_INDEX_EXPR 3))");
//        runParserRule("cellIndexExpr", "2-3", "(CELL_INDEX_EXPR - (CELL_INDEX_EXPR 2) (CELL_INDEX_EXPR 3))"); //TODO
        runParserRule("cellIndexExpr", "2*3", "(CELL_INDEX_EXPR * (CELL_INDEX_EXPR 2) (CELL_INDEX_EXPR 3))");
        runParserRule("cellIndexExpr", "(2 * 3)", "(CELL_INDEX_EXPR * (CELL_INDEX_EXPR 2) (CELL_INDEX_EXPR 3))");
        runParserRule("cellIndexExpr", "x + (y-_isacomplicatedstring * 2)", 
                "(CELL_INDEX_EXPR + (CELL_INDEX_EXPR x) (CELL_INDEX_EXPR * (CELL_INDEX_EXPR y-_isacomplicatedstring) (CELL_INDEX_EXPR 2)))");
        
        // Invalid characters
        runParserRuleFail("cellIndexExpr", "'x'");
    }

    @Test
    public void testCellIndexAtom() throws Exception {
        runParserRule("cellIndexAtom", "1", "(CELL_INDEX_EXPR 1)");
        runParserRule("cellIndexAtom", "x", "(CELL_INDEX_EXPR x)");
        runParserRule("cellIndexAtom", "y-_isacomplicatedstring", 
                "(CELL_INDEX_EXPR y-_isacomplicatedstring)");
        
        // Invalid characters
        runParserRuleFail("cellIndexAtom", "'x'");
    }

    @Test
    public void testObsDecl_spatial() throws Exception {
        runParserRule("obsDecl", "%obs: voxel 'label' :cytosol A:membrane(x~a),B(y~d)", 
                "(OBSERVATION VOXEL (AGENTS (LOCATION cytosol) (AGENT A (LOCATION membrane) (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) label)");
        runParserRule("obsDecl", "%obs: 'label' :cytosol A:membrane(x~a),B(y~d)", 
                "(OBSERVATION (AGENTS (LOCATION cytosol) (AGENT A (LOCATION membrane) (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) label)");
        runParserRule("obsDecl", "%obs: 'label' :cytosol[0][1] A(x~a),B(y~d)", 
            "(OBSERVATION (AGENTS (LOCATION cytosol (INDEX (CELL_INDEX_EXPR 0)) (INDEX (CELL_INDEX_EXPR 1))) " +
            "(AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) label)");
        runParserRule("obsDecl", "%obs: :cytosol[0][1] A(x~a),B(y~d)", 
            "(OBSERVATION (AGENTS (LOCATION cytosol (INDEX (CELL_INDEX_EXPR 0)) (INDEX (CELL_INDEX_EXPR 1))) " +
            "(AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))))");
        
        runParserRuleFail("obsDecl", "%obs: :cytosol[0][1]");
    }

    @Test
    public void testInitDecl_spatial() throws Exception {
        runParserRule("initDecl", "%init: 5 :cytosol A(x~a),B(y~d)", 
                "(INIT (AGENTS (LOCATION cytosol) (AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) 5)");
        runParserRule("initDecl", "%init: 5 :cytosol A:membrane(x~a),B(y~d)", 
                "(INIT (AGENTS (LOCATION cytosol) (AGENT A (LOCATION membrane) (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) 5)");
        runParserRule("initDecl", "%init: 5 :cytosol[0][1] A(x~a),B(y~d)", 
                "(INIT (AGENTS (LOCATION cytosol (INDEX (CELL_INDEX_EXPR 0)) (INDEX (CELL_INDEX_EXPR 1))) " +
                "(AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) 5)");
        
        runParserRule("initDecl", "%init: 'label' :cytosol A(x~a),B(y~d)", 
                "(INIT (AGENTS (LOCATION cytosol) (AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) label)");
        runParserRule("initDecl", "%init: 'label' :cytosol[0][1] A(x~a),B(y~d)", 
                "(INIT (AGENTS (LOCATION cytosol (INDEX (CELL_INDEX_EXPR 0)) (INDEX (CELL_INDEX_EXPR 1))) " +
                "(AGENT A (INTERFACE x (STATE a))) (AGENT B (INTERFACE y (STATE d)))) label)");
    }

    @Test
    public void testAgent() throws Exception {
        runParserRule("agent", "A", "(AGENT A)");
        runParserRule("agent", "A()", "(AGENT A)");
        runParserRule("agent", "A(x~a,l!1)", "(AGENT A (INTERFACE x (STATE a)) (INTERFACE l (LINK 1)))");
        runParserRule("agent", "A_new()", "(AGENT A_new)");
        runParserRule("agent", "A-new()", "(AGENT A-new)");
        runParserRule("agent", "EBOX-CLK-BMAL1()", "(AGENT EBOX-CLK-BMAL1)");
        runParserRule("agent", "Predator(loc~domain,loc_index_1~0,loc_index_2~0)", 
                "(AGENT Predator (INTERFACE loc (STATE domain)) (INTERFACE loc_index_1 (STATE 0)) (INTERFACE loc_index_2 (STATE 0)))");
        
        // Invalid characters
        runParserRule("agent", "A^new()", "(AGENT A)");
        runParserRule("agent", "A(x~a^b)", "(AGENT A (INTERFACE x (STATE a)))");
    }
    
    @Test
    public void testAgent_spatial() throws Exception {
        runParserRule("agent", "A:cytosol", "(AGENT A (LOCATION cytosol))");
        runParserRule("agent", "A:cytosol()", "(AGENT A (LOCATION cytosol))");
        runParserRule("agent", "A:cytosol(x~a,l!1)", "(AGENT A (LOCATION cytosol) (INTERFACE x (STATE a)) (INTERFACE l (LINK 1)))");
        runParserRule("agent", "A_0-new:cytosol-0_1A()", "(AGENT A_0-new (LOCATION cytosol-0_1A))");
        
        runParserRule("agent", "A:cytosol[0]", "(AGENT A (LOCATION cytosol (INDEX (CELL_INDEX_EXPR 0))))");
        runParserRule("agent", "A:cytosol[0]()", "(AGENT A (LOCATION cytosol (INDEX (CELL_INDEX_EXPR 0))))");
        runParserRule("agent", "A:cytosol[0][1](x~a,l!1)", "(AGENT A (LOCATION cytosol (INDEX (CELL_INDEX_EXPR 0)) (INDEX (CELL_INDEX_EXPR 1))) (INTERFACE x (STATE a)) (INTERFACE l (LINK 1)))");
        
        // Invalid characters
        runParserRuleFail("agent", "A:()");
    }
    
    @Test
    public void testAgentInterface() throws Exception {
        runParserRule("agentInterface", "l", "(INTERFACE l)");
        runParserRule("agentInterface", "l_new", "(INTERFACE l_new)");
        runParserRule("agentInterface", "l-new", "(INTERFACE l-new)");
        runParserRule("agentInterface", "l!1", "(INTERFACE l (LINK 1))");
        runParserRule("agentInterface", "x~a", "(INTERFACE x (STATE a))");
    }

    @Test
    public void testId() throws Exception {
        runParserRule("id", "A", "A");
        runParserRule("id", "A_new", "A_new");
        runParserRule("id", "loc_index_2", "loc_index_2");
        runParserRule("id", "A-new", "A-new");
//        runParserRule("id", "A _ new", "A _ new"); // TODO check
//        runParserRule("id", "A - new", "A - new");
        
        
        String[] keywords = new String[] {
                "inf", "pi", "T", "E", "Tmax", "Tsim", "Emax", "repeat", "until", "do", 
                "set", "true", "false", "not", "mod", "log", "sin", "cos", "tan", "sqrt", "exp", "int",
                };
        for (String keyword : keywords) {
            runParserRule("id", keyword, keyword); 
        }

        // Invalid characters
        runParserRule("id", "A^new", "A");
        
        // Reserved keywords
        runParserRuleFail("id", "fixed");
        runParserRuleFail("id", "voxel");
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
            if (expectedTree != null) {
                assertEquals(CommonTree.class, tree.getClass());
                assertEquals(expectedTree, tree.toStringTree());
            }
            else {
                assertNull(tree);
            }
        }
        else {
            if (tree.getClass() == CommonTree.class) {
                assertTrue(tree.toStringTree().contains("extraneous") || 
                        tree.toStringTree().contains("mismatched") || 
                        tree.toStringTree().contains("unexpected"));
            }
            else {
                assertEquals(CommonErrorNode.class, tree.getClass());
            }
        }
        
    }
    
}
