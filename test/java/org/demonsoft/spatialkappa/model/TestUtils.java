package org.demonsoft.spatialkappa.model;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.demonsoft.spatialkappa.parser.SpatialKappaLexer;
import org.demonsoft.spatialkappa.parser.SpatialKappaParser;
import org.demonsoft.spatialkappa.parser.SpatialKappaWalker;

public class TestUtils {

    public static List<Complex> getComplexes(Agent... agents) {
        return Utils.getComplexes(Arrays.asList(agents));
    }

    public static final IKappaModel createKappaModel(String inputText) throws Exception {
        ANTLRInputStream input = new ANTLRInputStream(new ByteArrayInputStream(inputText.getBytes()));
        SpatialKappaLexer lexer = new SpatialKappaLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SpatialKappaParser parser = new SpatialKappaParser(tokens);
        SpatialKappaParser.prog_return r = parser.prog();

        List<String> parseErrors = lexer.getErrors();
        parseErrors.addAll(parser.getErrors());
        if (parseErrors.size() > 0) {
            fail("Problems parsing model file: " + parseErrors);
        }

        CommonTree t = (CommonTree) r.getTree();
        CommonTreeNodeStream nodes = new CommonTreeNodeStream(t);
        nodes.setTokenStream(tokens);
        SpatialKappaWalker walker = new SpatialKappaWalker(nodes);
        
        return walker.prog();
    }
    

}
