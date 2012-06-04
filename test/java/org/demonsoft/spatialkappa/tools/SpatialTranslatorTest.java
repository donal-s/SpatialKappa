package org.demonsoft.spatialkappa.tools;

import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_0;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_2;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_X;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_X_PLUS_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpressionTest.INDEX_Y;
import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;
import static org.demonsoft.spatialkappa.model.TestUtils.getList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.apache.commons.io.FileUtils;
import org.demonsoft.spatialkappa.model.Agent;
import org.demonsoft.spatialkappa.model.AgentLink;
import org.demonsoft.spatialkappa.model.AgentSite;
import org.demonsoft.spatialkappa.model.CellIndexExpression;
import org.demonsoft.spatialkappa.model.Channel;
import org.demonsoft.spatialkappa.model.Compartment;
import org.demonsoft.spatialkappa.model.Complex;
import org.demonsoft.spatialkappa.model.IKappaModel;
import org.demonsoft.spatialkappa.model.KappaModel;
import org.demonsoft.spatialkappa.model.Location;
import org.demonsoft.spatialkappa.model.Transform;
import org.demonsoft.spatialkappa.model.VariableExpression.Operator;
import org.demonsoft.spatialkappa.model.VariableReference;
import org.demonsoft.spatialkappa.parser.SpatialKappaLexer;
import org.demonsoft.spatialkappa.parser.SpatialKappaParser;
import org.demonsoft.spatialkappa.parser.SpatialKappaWalker;
import org.demonsoft.spatialkappa.tools.SpatialTranslator.MappingInstance;
import org.junit.Before;
import org.junit.Test;


public class SpatialTranslatorTest {

	private VariableReference refX = new VariableReference("x");
    private VariableReference refY = new VariableReference("y");
    
    private SpatialTranslator translator;
    
    @Before
    public void setUp() throws Exception {
        translator = new SpatialTranslator("%compartment: cytosol [4]\n");
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testSpatialTranslatorFile() throws Exception {
        try {
            new SpatialTranslator((File) null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        translator = new SpatialTranslator(new File(TEST_INPUT_FILENAME));
        String testOutput = FileUtils.readFileToString(new File(TEST_OUTPUT_FILENAME));
        assertEquals(testOutput, translator.translateToKappa());
    }

    @SuppressWarnings("unused")
    @Test
    public void testSpatialTranslatorString() throws Exception {
        try {
            new SpatialTranslator((String) null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        translator = new SpatialTranslator(SIMPLE_TEST_INPUT);
        assertEquals(SIMPLE_TEST_OUTPUT, translator.translateToKappa());
    }

    @Test
    public void testEmptyInput() throws Exception {
        translator = new SpatialTranslator("");
        assertEquals("", translator.translateToKappa().trim());
    }

    @Test
    public void testInfiniteRates() throws Exception {
        translator = new SpatialTranslator(INFINITE_RATE_INPUT);
        assertEquals(INFINITE_RATE_OUTPUT, translator.translateToKappa());
    }

    @Test
    public void testObservationsOnly() throws Exception {
        translator = new SpatialTranslator(OBSERVATIONS_ONLY_INPUT);
        assertEquals(OBSERVATIONS_ONLY_OUTPUT, translator.translateToKappa());
    }

    @Test
    public void testObservationsWithLinks() throws Exception {
        translator = new SpatialTranslator(OBSERVATIONS_WITH_LINKS_INPUT);
        assertEquals(OBSERVATIONS_WITH_LINKS_OUTPUT, translator.translateToKappa());
    }

    @Test
    public void testInitOnly() throws Exception {
        translator = new SpatialTranslator(INIT_ONLY_INPUT);
        assertEquals(INIT_ONLY_OUTPUT, translator.translateToKappa());
    }

    @Test
    public void testInitWithLinks() throws Exception {
        translator = new SpatialTranslator(INIT_WITH_LINKS_INPUT);
        assertEquals(INIT_WITH_LINKS_OUTPUT, translator.translateToKappa());
    }

    @Test
    public void testReactionsOnly() throws Exception {
        translator = new SpatialTranslator(REACTION_ONLY_INPUT);
        assertEquals(REACTION_ONLY_OUTPUT, translator.translateToKappa());
    }

    @Test
    public void testVariablesOnly() throws Exception {
        translator = new SpatialTranslator(VARIABLES_ONLY_INPUT);
        assertEquals(VARIABLES_ONLY_OUTPUT, translator.translateToKappa());
    }

    @Test
    public void testVariablesWithLinks() throws Exception {
        translator = new SpatialTranslator(VARIABLES_WITH_LINKS_INPUT);
        assertEquals(VARIABLES_WITH_LINKS_OUTPUT, translator.translateToKappa());
    }

    @Test
    public void testTransportOnly() throws Exception {
        translator = new SpatialTranslator(TRANSPORT_ONLY_INPUT);
        assertEquals(TRANSPORT_ONLY_OUTPUT, translator.translateToKappa());
    }

    @Test
    public void testTransportLimitedLinks() throws Exception {
        translator = new SpatialTranslator(TRANSPORT_LIMITED_LINKS_INPUT);
        assertEquals(TRANSPORT_LIMITED_LINKS_OUTPUT, translator.translateToKappa());
    }

    @Test
    public void testTransportDifferentDimensions() throws Exception {
        translator = new SpatialTranslator(TRANSPORT_DIFFERENT_DIMENSIONS_INPUT);
        assertEquals(TRANSPORT_DIFFERENT_DIMENSIONS_OUTPUT, translator.translateToKappa());
    }

    @Test
    public void testTransport6Way() throws Exception {
        translator = new SpatialTranslator(TRANSPORT_6WAY_INPUT);
        assertEquals(TRANSPORT_6WAY_OUTPUT, translator.translateToKappa());
    }

    @Test
    public void testAgentOrdering() throws Exception {
        translator = new SpatialTranslator(AGENT_ORDERING_INPUT);
        assertEquals(AGENT_ORDERING_OUTPUT, translator.translateToKappa());
    }

    @SuppressWarnings("unused")
    @Test
    public void testSpatialTranslatorKappaModel() throws Exception {
        try {
            new SpatialTranslator((KappaModel) null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        ANTLRInputStream input = new ANTLRInputStream(new ByteArrayInputStream(SIMPLE_TEST_INPUT.getBytes()));
        CommonTokenStream tokens = new CommonTokenStream(new SpatialKappaLexer(input));
        SpatialKappaParser.prog_return r = new SpatialKappaParser(tokens).prog();

        CommonTree t = (CommonTree) r.getTree();

        CommonTreeNodeStream nodes = new CommonTreeNodeStream(t);
        nodes.setTokenStream(tokens);
        SpatialKappaWalker walker = new SpatialKappaWalker(nodes);
        IKappaModel kappaModel =  walker.prog();
        
        translator = new SpatialTranslator(kappaModel);
        assertEquals(SIMPLE_TEST_OUTPUT, translator.translateToKappa());
    }
    
    
    @Test
    public void testGetLinkStateSuffixPairs() throws Exception {
        Channel channel = new Channel("label", new Location("a"), new Location("b"));
        List<Compartment> compartments = new ArrayList<Compartment>();
        
        try {
            translator.getLinkStateSuffixPairs(channel, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            translator.getLinkStateSuffixPairs(null, compartments);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        compartments.add(new Compartment("a"));
        
        try {
            translator.getLinkStateSuffixPairs(channel, compartments);
            fail("missing compartment should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        compartments.clear();
        compartments.add(new Compartment("b"));

        try {
            translator.getLinkStateSuffixPairs(channel, compartments);
            fail("missing compartment should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        compartments.clear();
        compartments.add(new Compartment("a", 3));
        compartments.add(new Compartment("b"));

        try {
            translator.getLinkStateSuffixPairs(channel, compartments);
            fail("dimension mismatch should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        // Single cell compartments
        compartments.clear();
        channel = new Channel("label", new Location("a"), new Location("b"));
        compartments.add(new Compartment("a"));
        compartments.add(new Compartment("b"));
        
        assertArrayEquals(new String[][] {{"loc~a", "loc~b"}}, translator.getLinkStateSuffixPairs(channel, compartments));
        
        // Single cell-cell link
        compartments.clear();
        channel = new Channel("label", 
                new Location("a", new CellIndexExpression("1")), new Location("a", new CellIndexExpression("2")));
        compartments.add(new Compartment("a", 4));
        
        assertArrayEquals(new String[][] {{"loc~a,loc_index_1~1", "loc~a,loc_index_1~2"}}, translator.getLinkStateSuffixPairs(channel, compartments));

        // Linear array
        compartments.clear();
        channel = new Channel("label",
                new Location("a", new CellIndexExpression(refX)), 
                new Location("a", new CellIndexExpression(new CellIndexExpression(refX), Operator.PLUS, new CellIndexExpression("1"))));
        compartments.add(new Compartment("a", 4));
        
        assertArrayEquals(new String[][] {
                {"loc~a,loc_index_1~0", "loc~a,loc_index_1~1"},
                {"loc~a,loc_index_1~1", "loc~a,loc_index_1~2"},
                {"loc~a,loc_index_1~2", "loc~a,loc_index_1~3"}}, translator.getLinkStateSuffixPairs(channel, compartments));

        // 2D array - square mesh - dimensions treated independently
        compartments.clear();
        channel = new Channel("label", 
                new Location("a", new CellIndexExpression(refX), new CellIndexExpression(refY)), 
                new Location("a", new CellIndexExpression(new CellIndexExpression(refX), Operator.PLUS, new CellIndexExpression("1")), new CellIndexExpression(refY)));
        channel = new Channel("label", 
                new Location("a", new CellIndexExpression(refX), new CellIndexExpression(refY)), 
                new Location("a", new CellIndexExpression(refX), new CellIndexExpression(new CellIndexExpression(refY), Operator.PLUS, new CellIndexExpression("1"))));
        compartments.add(new Compartment("a", 4, 4));
        
        /* TODO known issue here
        assertArrayEquals(new String[][] {
                {"loc~a,loc_index_1~0", "loc~a,loc_index_1~1"},
                {"loc~a,loc_index_1~1", "loc~a,loc_index_1~2"},
                {"loc~a,loc_index_1~2", "loc~a,loc_index_1~3"},
                {"loc~a,loc_index_2~0", "loc~a,loc_index_2~1"},
                {"loc~a,loc_index_2~1", "loc~a,loc_index_2~2"},
                {"loc~a,loc_index_2~2", "loc~a,loc_index_2~3"}
                }, translator.getLinkStateSuffixPairs(channel, compartments));
        
        // 2D array - diagonal linked mesh - dimensions treated together
        compartments.clear();
        channel = new CompartmentLink("label", 
                new Location("a", new CellIndexExpression("x"), new CellIndexExpression("y")), 
                new Location("a", new CellIndexExpression(new CellIndexExpression("x"), Operator.PLUS, new CellIndexExpression("1")), 
                        new CellIndexExpression(new CellIndexExpression("y"), Operator.PLUS, new CellIndexExpression("1"))), 
                Direction.FORWARD);
        compartments.add(new Compartment("a", 3, 3));
        
        assertArrayEquals(new String[][] {
                {"loc~a,loc_index_1~0,loc_index_2~0", "loc~a,loc_index_1~1,loc_index_2~1"},
                {"loc~a,loc_index_1~0,loc_index_2~1", "loc~a,loc_index_1~1,loc_index_2~2"},
                {"loc~a,loc_index_1~1,loc_index_2~0", "loc~a,loc_index_1~2,loc_index_2~1"},
                {"loc~a,loc_index_1~1,loc_index_2~1", "loc~a,loc_index_1~2,loc_index_2~2"},
                }, translator.getLinkStateSuffixPairs(channel, compartments));
        */
    }
    
    
    @Test
    public void testGetKappaString_location() throws Exception {
        Location location = new Location("label");
        assertEquals("loc~label", translator.getKappaString(location, 0));
        assertEquals("loc~label,loc_index_1~0", translator.getKappaString(location, 1));
        assertEquals("loc~label,loc_index_1~0,loc_index_2~0", translator.getKappaString(location, 2));

        location = new Location("label", new CellIndexExpression("2"));
        assertEquals("loc~label,loc_index_1~2", translator.getKappaString(location, 1));


        location = new Location("label", new CellIndexExpression("2"), new CellIndexExpression(new CellIndexExpression("3"), Operator.PLUS, new CellIndexExpression("1")));
        try {
            translator.getKappaString(location, 1);
            fail("too few dimensions should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        assertEquals("loc~label,loc_index_1~2,loc_index_2~4", translator.getKappaString(location, 2));
        assertEquals("loc~label,loc_index_1~2,loc_index_2~4,loc_index_3~0", translator.getKappaString(location, 3));

        location = new Location("label", new CellIndexExpression(refX));
        try {
            translator.getKappaString(location, 1);
            fail("missing variable should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
    }

    
    @Test
    public void testGetKappaString_location_withVariables() {
        Location location = new Location("label");
        Map<String, Integer> variables = new HashMap<String, Integer>();
        
        try {
            translator.getKappaString(location, null, 0);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        assertEquals("loc~label", translator.getKappaString(location, variables, 0));
        assertEquals("loc~label,loc_index_1~0", translator.getKappaString(location, variables, 1));

        location = new Location("label", new CellIndexExpression("2"));
        assertEquals("loc~label,loc_index_1~2", translator.getKappaString(location, variables, 1));
        assertEquals("loc~label,loc_index_1~2,loc_index_2~0", translator.getKappaString(location, variables, 2));


        location = new Location("label", new CellIndexExpression("2"), new CellIndexExpression(new CellIndexExpression("3"), Operator.PLUS, new CellIndexExpression("1")));
        try {
            translator.getKappaString(location, variables, 1);
            fail("too few dimensions should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        assertEquals("loc~label,loc_index_1~2,loc_index_2~4", translator.getKappaString(location, variables, 2));
        assertEquals("loc~label,loc_index_1~2,loc_index_2~4,loc_index_3~0", translator.getKappaString(location, variables, 3));

        location = new Location("label", new CellIndexExpression(refX));
        try {
            translator.getKappaString(location, variables, 1);
            fail("missing variable should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        variables.put("x", 7);
        assertEquals("loc~label,loc_index_1~7", translator.getKappaString(location, variables, 1));

        location = new Location("label", new CellIndexExpression("2"), new CellIndexExpression(refX));
        variables.clear();
        try {
            translator.getKappaString(location, variables, 1);
            fail("missing variable should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        variables.put("x", 7);
        assertEquals("loc~label,loc_index_1~2,loc_index_2~7", translator.getKappaString(location, variables, 2));
    }

    @Test
    public void testGetPartitionedLocations() throws Exception {
        List<Compartment> compartments = new ArrayList<Compartment>();
        Compartment cytosol = new Compartment("cytosol", 2, 2);
        Compartment membrane = new Compartment("membrane", 3);
        compartments.add(cytosol);
        compartments.add(membrane);
        
        List<Agent> agents = new ArrayList<Agent>();

        try {
            translator.getPartitionedLocations(agents, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
 
        assertEquals(0, translator.getPartitionedLocations(null, compartments).size());
    	
    	Set<Location> expected = new HashSet<Location>();

    	agents.add(new Agent("A", new Location("cytosol")));
    	expected.add(new Location("cytosol"));
    	assertEquals(expected, translator.getPartitionedLocations(agents, compartments));

    	agents.clear();
    	expected.clear();
    	
    	agents.add(new Agent("A", new Location("cytosol", new CellIndexExpression("1"))));
    	expected.add(new Location("cytosol", new CellIndexExpression("1")));
    	assertEquals(expected, translator.getPartitionedLocations(agents, compartments));

    	agents.clear();
    	expected.clear();
    	
    	agents.add(new Agent("A", new Location("cytosol", new CellIndexExpression("1"), new CellIndexExpression("1"))));
    	assertEquals(expected, translator.getPartitionedLocations(agents, compartments));

    	agents.clear();
    	expected.clear();
    	
    	agents.add(new Agent("A", new Location("cytosol")));
    	agents.add(new Agent("B", new Location("cytosol")));
    	expected.add(new Location("cytosol"));
    	assertEquals(expected, translator.getPartitionedLocations(agents, compartments));

    	agents.clear();
    	expected.clear();
    	
    	agents.add(new Agent("A", new Location("cytosol")));
    	agents.add(new Agent("B", new Location("membrane")));
    	expected.add(new Location("cytosol"));
    	expected.add(new Location("membrane"));
    	assertEquals(expected, translator.getPartitionedLocations(agents, compartments));
    }
    
    @Test
    public void testCreatePartitionMaps() throws Exception {
    	
    	IKappaModel model = new KappaModel();
    	Compartment cytosol = new Compartment("cytosol", 2, 2);
    	Compartment membrane = new Compartment("membrane", 3);
    	Location locationCytosol = new Location("cytosol");
    	Location locationMembrane = new Location("membrane");
    	
    	model.addCompartment(cytosol);
    	model.addCompartment(membrane);
    	translator = new SpatialTranslator(model);
    	
    	List<Location> locations = new ArrayList<Location>();
    	List<Map<Location, String>> expected = new ArrayList<Map<Location,String>>();
    	
    	assertEquals(expected, translator.createPartitionMaps(locations));
    	
    	locations.add(new Location("cytosol"));
    	Map<Location, String> rowMap = new HashMap<Location, String>();
    	expected.add(rowMap);
    	rowMap.put(locationCytosol, "loc~cytosol,loc_index_1~0,loc_index_2~0");
    	rowMap = new HashMap<Location, String>();
    	expected.add(rowMap);
    	rowMap.put(locationCytosol, "loc~cytosol,loc_index_1~1,loc_index_2~0");
    	rowMap = new HashMap<Location, String>();
    	expected.add(rowMap);
    	rowMap.put(locationCytosol, "loc~cytosol,loc_index_1~0,loc_index_2~1");
    	rowMap = new HashMap<Location, String>();
    	expected.add(rowMap);
    	rowMap.put(locationCytosol, "loc~cytosol,loc_index_1~1,loc_index_2~1");
    	
    	assertEquals(expected, translator.createPartitionMaps(locations));

    	locations.clear();
    	expected.clear();

    	locations.add(new Location("membrane"));
    	rowMap = new HashMap<Location, String>();
    	expected.add(rowMap);
    	rowMap.put(locationMembrane, "loc~membrane,loc_index_1~0");
    	rowMap = new HashMap<Location, String>();
    	expected.add(rowMap);
    	rowMap.put(locationMembrane, "loc~membrane,loc_index_1~1");
    	rowMap = new HashMap<Location, String>();
    	expected.add(rowMap);
    	rowMap.put(locationMembrane, "loc~membrane,loc_index_1~2");
    	
       	assertEquals(expected, translator.createPartitionMaps(locations));

       	locations.clear();
    	expected.clear();

    	locations.add(new Location("cytosol"));
    	locations.add(new Location("membrane"));
    	rowMap = new HashMap<Location, String>();
    	expected.add(rowMap);
    	rowMap.put(locationCytosol, "loc~cytosol,loc_index_1~0,loc_index_2~0");
    	rowMap.put(locationMembrane, "loc~membrane,loc_index_1~0");
    	rowMap = new HashMap<Location, String>();
    	expected.add(rowMap);
    	rowMap.put(locationCytosol, "loc~cytosol,loc_index_1~0,loc_index_2~0");
    	rowMap.put(locationMembrane, "loc~membrane,loc_index_1~1");
    	rowMap = new HashMap<Location, String>();
    	expected.add(rowMap);
    	rowMap.put(locationCytosol, "loc~cytosol,loc_index_1~0,loc_index_2~0");
    	rowMap.put(locationMembrane, "loc~membrane,loc_index_1~2");
    	rowMap = new HashMap<Location, String>();
    	expected.add(rowMap);
    	rowMap.put(locationCytosol, "loc~cytosol,loc_index_1~1,loc_index_2~0");
    	rowMap.put(locationMembrane, "loc~membrane,loc_index_1~0");
    	rowMap = new HashMap<Location, String>();
    	expected.add(rowMap);
    	rowMap.put(locationCytosol, "loc~cytosol,loc_index_1~1,loc_index_2~0");
    	rowMap.put(locationMembrane, "loc~membrane,loc_index_1~1");
    	rowMap = new HashMap<Location, String>();
    	expected.add(rowMap);
    	rowMap.put(locationCytosol, "loc~cytosol,loc_index_1~1,loc_index_2~0");
    	rowMap.put(locationMembrane, "loc~membrane,loc_index_1~2");
    	rowMap = new HashMap<Location, String>();
    	expected.add(rowMap);
    	rowMap.put(locationCytosol, "loc~cytosol,loc_index_1~0,loc_index_2~1");
    	rowMap.put(locationMembrane, "loc~membrane,loc_index_1~0");
    	rowMap = new HashMap<Location, String>();
    	expected.add(rowMap);
    	rowMap.put(locationCytosol, "loc~cytosol,loc_index_1~0,loc_index_2~1");
    	rowMap.put(locationMembrane, "loc~membrane,loc_index_1~1");
    	rowMap = new HashMap<Location, String>();
    	expected.add(rowMap);
    	rowMap.put(locationCytosol, "loc~cytosol,loc_index_1~0,loc_index_2~1");
    	rowMap.put(locationMembrane, "loc~membrane,loc_index_1~2");
    	rowMap = new HashMap<Location, String>();
    	expected.add(rowMap);
    	rowMap.put(locationCytosol, "loc~cytosol,loc_index_1~1,loc_index_2~1");
    	rowMap.put(locationMembrane, "loc~membrane,loc_index_1~0");
    	rowMap = new HashMap<Location, String>();
    	expected.add(rowMap);
    	rowMap.put(locationCytosol, "loc~cytosol,loc_index_1~1,loc_index_2~1");
    	rowMap.put(locationMembrane, "loc~membrane,loc_index_1~1");
    	rowMap = new HashMap<Location, String>();
    	expected.add(rowMap);
    	rowMap.put(locationCytosol, "loc~cytosol,loc_index_1~1,loc_index_2~1");
    	rowMap.put(locationMembrane, "loc~membrane,loc_index_1~2");
    	
       	assertEquals(expected, translator.createPartitionMaps(locations));
    }
    
    @Test
    public void testGetFixedAgents() throws Exception {
        List<Compartment> compartments = new ArrayList<Compartment>();
        compartments.add(new Compartment("loc1"));
        compartments.add(new Compartment("loc2", 5));
        
        Agent agent1 = new Agent("agent1");
        Agent agent2 = new Agent("agent2", new Location("loc1"));
        Agent agent3 = new Agent("agent3", new Location("loc2"));
        Agent agent4 = new Agent("agent4", new Location("loc2", INDEX_0));
        Agent agent5 = new Agent("agent5", new Location("loc2", INDEX_X));
        Agent agent6 = new Agent("agent6", new Location("loc2", INDEX_X_PLUS_1));
        
        List<Agent> agents = new ArrayList<Agent>();
        agents.add(agent1);
        agents.add(agent2);
        agents.add(agent3);
        agents.add(agent4);
        agents.add(agent5);
        agents.add(agent6);
        Complex complex = new Complex(agents);
        
        try {
            translator.getFixedAgents(null, compartments);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            translator.getFixedAgents(complex, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        List<Agent> expected = new ArrayList<Agent>();
        expected.add(agent2);
        expected.add(agent4);
        assertEquals(expected, translator.getFixedAgents(complex, compartments));
    }
    
    
    @Test
    public void testGetInternalLinks() throws Exception {
        
        Agent agent1 = new Agent("agent1", new AgentSite("s1", null, "1"), new AgentSite("s2", null, "_"));
        Agent agent2 = new Agent("agent2", new AgentSite("s3", null, "1"), new AgentSite("s4", null, "2"));
        Agent agent3 = new Agent("agent3", new AgentSite("s5", null, "2"), new AgentSite("s6", null, "?"));
        Agent agent4 = new Agent("agent4");
        
        List<Agent> agents = new ArrayList<Agent>();
        agents.add(agent1);
        agents.add(agent2);
        agents.add(agent3);
        agents.add(agent4);
        Complex complex = new Complex(agents);
        
        try {
            translator.getInternalLinks(null, agents);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            translator.getInternalLinks(complex.agentLinks, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        List<Agent> input = new ArrayList<Agent>();
        
        List<AgentLink> expected = new ArrayList<AgentLink>();
        assertEquals(expected, translator.getInternalLinks(complex.agentLinks, input));
        
        input.add(agent1);
        expected.add(complex.getAgentLink(agent1, "s2"));
        assertEquals(expected, translator.getInternalLinks(complex.agentLinks, input));
        
        input.add(agent2);
        expected.add(complex.getAgentLink(agent1, "s1"));
        assertEquals(expected, translator.getInternalLinks(complex.agentLinks, input));
    }
    
    
    @Test
    public void testChooseNextAgent() throws Exception {
        
        Agent agent1 = new Agent("agent1", new AgentSite("s1", null, "1"), new AgentSite("s2", null, "_"));
        Agent agent2 = new Agent("agent2", new AgentSite("s3", null, "1"), new AgentSite("s4", null, "2"));
        Agent agent3 = new Agent("agent3", new AgentSite("s5", null, "2"), new AgentSite("s6", null, "?"));
        Agent agent4 = new Agent("agent4");
        
        List<Agent> remainingAgents = new ArrayList<Agent>();
        remainingAgents.add(agent1);
        remainingAgents.add(agent2);
        remainingAgents.add(agent3);
        remainingAgents.add(agent4);
        
        Complex complex = new Complex(remainingAgents);
        
        List<Agent> fixedAgents = new ArrayList<Agent>();
        fixedAgents.add(agent1);
        remainingAgents.remove(agent1);
        
        List<AgentLink> remainingLinks = new ArrayList<AgentLink>(complex.agentLinks);
        remainingLinks.removeAll(translator.getInternalLinks(remainingLinks, fixedAgents));
        
        try {
            translator.chooseNextAgent(null, remainingAgents, remainingLinks);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            translator.chooseNextAgent(fixedAgents, null, remainingLinks);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            translator.chooseNextAgent(fixedAgents, remainingAgents, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            translator.chooseNextAgent(new ArrayList<Agent>(), remainingAgents, remainingLinks);
            fail("no linked agent to choose should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        try {
            translator.chooseNextAgent(fixedAgents, new ArrayList<Agent>(), remainingLinks);
            fail("no linked agent to choose should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        try {
            translator.chooseNextAgent(fixedAgents, remainingAgents, new ArrayList<AgentLink>());
            fail("no linked agent to choose should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        assertEquals(agent2, translator.chooseNextAgent(fixedAgents, remainingAgents, remainingLinks));

        fixedAgents.add(agent2);
        remainingAgents.remove(agent2);
        remainingLinks.removeAll(translator.getInternalLinks(remainingLinks, fixedAgents));
        
        assertEquals(agent3, translator.chooseNextAgent(fixedAgents, remainingAgents, remainingLinks));
    }
    
    
    @Test
    public void testGetPossibleLocations() throws Exception {
        
        List<Compartment> compartments = new ArrayList<Compartment>();
        compartments.add(new Compartment("loc1"));
        compartments.add(new Compartment("loc2", 5));
        
        try {
            translator.getPossibleLocations(null, null, null, compartments);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            translator.getPossibleLocations(new Location("loc2", INDEX_1), null, null, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        // No location or channel - stay in same location
        List<Location> expected = new ArrayList<Location>();
        expected.add(NOT_LOCATED);
        assertEquals(expected, translator.getPossibleLocations(NOT_LOCATED, null, null, compartments));
        
        // No channel - stay in same location
        expected = new ArrayList<Location>();
        expected.add(new Location("loc1"));
        assertEquals(expected, translator.getPossibleLocations(new Location("loc1"), null, null, compartments));

        expected.clear();
        expected.add(new Location("loc2", INDEX_1));
        assertEquals(expected, translator.getPossibleLocations(new Location("loc2", INDEX_1), null, null, compartments));
        
        // No channel, but now constraints
        assertEquals(expected, translator.getPossibleLocations(new Location("loc2", INDEX_1), new Location("loc2", INDEX_1), null, compartments));
        assertEquals(expected, translator.getPossibleLocations(new Location("loc2", INDEX_1), new Location("loc2"), null, compartments));
        
        expected.clear();
        assertEquals(expected, translator.getPossibleLocations(new Location("loc2", INDEX_1), new Location("loc2", INDEX_2), null, compartments));
        assertEquals(expected, translator.getPossibleLocations(new Location("loc2", INDEX_1), new Location("loc1"), null, compartments));

        // Matching channel - no constraints
        Channel channel = new Channel("label", new Location("loc2", INDEX_1), new Location("loc2", INDEX_2));
        expected.clear();
        expected.add(new Location("loc2", INDEX_2));
        assertEquals(expected, translator.getPossibleLocations(new Location("loc2", INDEX_1), null, channel, compartments));
        
        channel = new Channel("label", new Location("loc2", INDEX_X), new Location("loc2", INDEX_X_PLUS_1));
        assertEquals(expected, translator.getPossibleLocations(new Location("loc2", INDEX_1), null, channel, compartments));
        
        channel = new Channel("label", new Location("loc2", INDEX_1), new Location("loc1"));
        expected.clear();
        expected.add(new Location("loc1"));
        assertEquals(expected, translator.getPossibleLocations(new Location("loc2", INDEX_1), null, channel, compartments));
        
        // Matching channel - constraints
        channel = new Channel("label", new Location("loc2", INDEX_1), new Location("loc2", INDEX_2));
        expected.clear();
        expected.add(new Location("loc2", INDEX_2));
        assertEquals(expected, translator.getPossibleLocations(new Location("loc2", INDEX_1), new Location("loc2", INDEX_2), channel, compartments));
        assertEquals(expected, translator.getPossibleLocations(new Location("loc2", INDEX_1), new Location("loc2"), channel, compartments));
        
        expected.clear();
        assertEquals(expected, translator.getPossibleLocations(new Location("loc2", INDEX_1), new Location("loc2", INDEX_1), channel, compartments));
        assertEquals(expected, translator.getPossibleLocations(new Location("loc2", INDEX_1), new Location("loc1"), channel, compartments));
        
        // Non matching channel
        channel = new Channel("label", new Location("loc2", INDEX_X), new Location("loc2", INDEX_X_PLUS_1));
        expected.clear();
        assertEquals(expected, translator.getPossibleLocations(new Location("loc2", new CellIndexExpression("4")), null, channel, compartments));

        assertEquals(expected, translator.getPossibleLocations(new Location("loc1"), null, channel, compartments));
    }
    

    // TODO remember to rename
    @Test
    public void testInitMappingStructure() throws Exception {
        
        List<Agent> agents = getList(new Agent("A", new Location("loc1")));
        Complex complex = new Complex(agents);
        
        List<Channel> channels = new ArrayList<Channel>();
        channels.add(new Channel("intra", new Location("loc2", INDEX_X), new Location("loc2", INDEX_X_PLUS_1)));
        channels.add(new Channel("inter", new Location("loc1"), new Location("loc2", INDEX_2)));
        
        List<Compartment> compartments = new ArrayList<Compartment>();
        compartments.add(new Compartment("loc1"));
        compartments.add(new Compartment("loc2", 5));
        
        try {
            translator.initMappingStructure(null, compartments, channels);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            translator.initMappingStructure(complex, null, channels);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            translator.initMappingStructure(complex, compartments, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        // Simple agent - fixed location
        agents = getList(new Agent("A", new Location("loc1")));
        complex = new Complex(agents);
        
        List<List<Agent>> expected = new ArrayList<List<Agent>>();
        List<Agent> instanceAgents = getList(new Agent("A", new Location("loc1")));
        expected.add(instanceAgents);
        
        checkMappingStructure(complex, compartments, channels, expected);

        agents = getList(new Agent("A", new Location("loc2", INDEX_1)));
        complex = new Complex(agents);
        
        expected.clear();
        instanceAgents = getList(new Agent("A", new Location("loc2", INDEX_1)));
        expected.add(instanceAgents);
        
        checkMappingStructure(complex, compartments, channels, expected);
        

        // Simple agent - no location
        agents = getList(new Agent("A"));
        complex = new Complex(agents);
        
        expected.clear();
        instanceAgents = getList(new Agent("A"));
        expected.add(instanceAgents);
        
        checkMappingStructure(complex, compartments, channels, expected);
        
        
        // Simple agent - partial location

        agents = getList(new Agent("A", new Location("loc2")));
        complex = new Complex(agents);
        
        expected.clear();
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_0))));
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_1))));
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_2))));
        expected.add(getList(new Agent("A", new Location("loc2", new CellIndexExpression("3")))));
        expected.add(getList(new Agent("A", new Location("loc2", new CellIndexExpression("4")))));
               
        checkMappingStructure(complex, compartments, channels, expected);

        
        // Dimer - one agent fixed, other same compartment
        agents = getList(new Agent("A", new Location("loc1"), new AgentSite("s", null, "1")),
                new Agent("B", new Location("loc1"), new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        
        expected.clear();
        instanceAgents = getList(new Agent("A", new Location("loc1"), new AgentSite("s", null, "1")),
                new Agent("B", new Location("loc1"), new AgentSite("s", null, "1")));
        expected.add(instanceAgents);
        
        checkMappingStructure(complex, compartments, channels, expected);

        agents = getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1")),
                new Agent("B", new Location("loc2"), new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        
        expected.clear();
        instanceAgents = getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1")),
                new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, "1")));
        expected.add(instanceAgents);
        
        checkMappingStructure(complex, compartments, channels, expected);
        

        // Dimer - one agent no location, other same compartment
        agents = getList(new Agent("A", new AgentSite("s", null, "1")), new Agent("B", new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        
        expected.clear();
        instanceAgents = getList(new Agent("A", new AgentSite("s", null, "1")), new Agent("B", new AgentSite("s", null, "1")));
        expected.add(instanceAgents);
        
        checkMappingStructure(complex, compartments, channels, expected);
        
        
        // Dimer - one agent partial location, other same compartment

        agents = getList(new Agent("A", new Location("loc2"), new AgentSite("s", null, "1")), 
                new Agent("B", new Location("loc2"), new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        
        expected.clear();
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_0), new AgentSite("s", null, "1")), 
                new Agent("B", new Location("loc2", INDEX_0), new AgentSite("s", null, "1"))));
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1")), 
                new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, "1"))));
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, "1")), 
                new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, "1"))));
        expected.add(getList(new Agent("A", new Location("loc2", new CellIndexExpression("3")), new AgentSite("s", null, "1")), 
                new Agent("B", new Location("loc2", new CellIndexExpression("3")), new AgentSite("s", null, "1"))));
        expected.add(getList(new Agent("A", new Location("loc2", new CellIndexExpression("4")), new AgentSite("s", null, "1")), 
                new Agent("B", new Location("loc2", new CellIndexExpression("4")), new AgentSite("s", null, "1"))));
               
        checkMappingStructure(complex, compartments, channels, expected);

        //TODO complex channels giving multiple results
        
        // Dimer - one agent fixed, other using channel
        agents = getList(new Agent("A", new Location("loc1"), new AgentSite("s", null, "1", "inter")),
                new Agent("B", new Location("loc2"), new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        
        expected.clear();
        instanceAgents = getList(new Agent("A", new Location("loc1"), new AgentSite("s", null, "1", "inter")),
                new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, "1")));
        expected.add(instanceAgents);
        
        checkMappingStructure(complex, compartments, channels, expected);

        agents = getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1", "intra")),
                new Agent("B", new Location("loc2"), new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        
        expected.clear();
        instanceAgents = getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1", "intra")),
                new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, "1")));
        expected.add(instanceAgents);
        
        checkMappingStructure(complex, compartments, channels, expected);
        

        // Dimer - one agent partial location, other using channel

        agents = getList(new Agent("A", new Location("loc2"), new AgentSite("s", null, "1", "intra")), 
                new Agent("B", new Location("loc2"), new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        
        expected.clear();
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_0), new AgentSite("s", null, "1", "intra")), 
                new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, "1"))));
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1", "intra")), 
                new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, "1"))));
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, "1", "intra")), 
                new Agent("B", new Location("loc2", new CellIndexExpression("3")), new AgentSite("s", null, "1"))));
        expected.add(getList(new Agent("A", new Location("loc2", new CellIndexExpression("3")), new AgentSite("s", null, "1", "intra")), 
                new Agent("B", new Location("loc2", new CellIndexExpression("4")), new AgentSite("s", null, "1"))));
               
        checkMappingStructure(complex, compartments, channels, expected);

        // Dimer - one agent partial location, other using channel, 2 dimensions

        channels.clear();
        channels.add(new Channel("horiz", new Location("loc2", INDEX_X, INDEX_Y), new Location("loc2", INDEX_X_PLUS_1, INDEX_Y)));
        
        compartments.clear();
        compartments.add(new Compartment("loc2", 2, 2));

        agents = getList(new Agent("A", new Location("loc2"), new AgentSite("s", null, "1", "horiz")), 
                new Agent("B", new Location("loc2"), new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        
        expected.clear();
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_0, INDEX_0), new AgentSite("s", null, "1", "horiz")), 
                new Agent("B", new Location("loc2", INDEX_1, INDEX_0), new AgentSite("s", null, "1"))));
        expected.add(getList(new Agent("A", new Location("loc2", INDEX_0, INDEX_1), new AgentSite("s", null, "1", "horiz")), 
                new Agent("B", new Location("loc2", INDEX_1, INDEX_1), new AgentSite("s", null, "1"))));
               
        checkMappingStructure(complex, compartments, channels, expected);

        
//        private static final String INIT_ONLY_INPUT = 
//                "%agent: A(state~blue~red~green~purple~orange~yellow~brown~white~black~cyan)\n" +
//                "%agent: B(s~right~centre~left~outside~bottom~middle~top~inside~back~front)\n" +
//                "%compartment: cytosol [4]\n" + 
//                "%init: 800 A(state~green),B(s~right) \n" + 
//                "%init: 800 A(state~purple),B:cytosol(s~centre) \n" + 
//                "%init: 800 A(state~orange),B:cytosol[3](s~left) \n" + 
//                "%init: 800 cytosol A(state~blue),B(s~outside) \n" + 
//                "%init: 800 cytosol A(state~yellow),B:cytosol(s~bottom) \n" +
//                "%init: 800 cytosol A(state~brown),B:cytosol[3](s~middle) \n" +
//                "%init: 800 cytosol[0] A(state~white),B(s~top) \n" +
//                "%init: 800 cytosol[0] A(state~black),B:cytosol(s~inside) \n" +
//                "%init: 800 cytosol[0] A(state~red),B:cytosol[0](s~back) \n" +
//                "%init: 800 cytosol[0] A(state~cyan),B:cytosol[3](s~front) \n" +
//
//    private static final String INIT_WITH_LINKS_INPUT = 
//    "%agent: A(state~blue~red~green~purple~orange~yellow~brown~white~black~cyan)\n" +
//    "%agent: B(s~right~centre~left~outside~bottom~middle~top~inside~back~front)\n" +
//    "%compartment: cytosol [2][2]\n" + 
//    "%compartment: membrane [2]\n" +
//    "%channel: horiz cytosol[x][y] <-> cytosol[x+1][y]\n" + 
//    "%channel: vert cytosol[x][y] <-> cytosol[x][y+1]\n" + 
//    "%channel: diag cytosol[x][y] <-> cytosol[x+1][y+1]\n" + 
//    "%channel: diag cytosol[x][y] <-> cytosol[x+1][y-1]\n" + 
//    "%init: 800 cytosol A(state~green!1:horiz),B(s~right!1) \n" + 
//    "";
//
        
    }
    
    private void checkMappingStructure(Complex complex, List<Compartment> compartments, List<Channel> channels,
            List<List<Agent>> expected) {
        List<MappingInstance> result = translator.initMappingStructure(complex, compartments, channels);
        assertEquals(expected.size(), result.size());
        
        for (int index=0; index < expected.size(); index++) {
            List<Agent> expectedAgents = expected.get(index);
            List<Agent> actualAgents = result.get(index).locatedAgents;
            
            assertEquals(expectedAgents.toString(), actualAgents.toString());
        }
    }


    @Test
    public void testHasLinksWithChannels() throws Exception {
        
        try {
            translator.hasLinksWithChannels((Complex) null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            translator.hasLinksWithChannels((List<Agent>) null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        // Simple agent - fixed location
        List<Agent> agents = getList(new Agent("A", new Location("loc1")));
        Complex complex = new Complex(agents);
        assertFalse(translator.hasLinksWithChannels(complex));
        assertFalse(translator.hasLinksWithChannels(agents));
        
        agents = getList(new Agent("A", new Location("loc1"), new AgentSite("s", null, "1")),
                new Agent("B", new Location("loc1"), new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        assertFalse(translator.hasLinksWithChannels(complex));
        assertFalse(translator.hasLinksWithChannels(agents));

        agents = getList(new Agent("A", new Location("loc1"), new AgentSite("s", null, "1", "channel")),
                new Agent("B", new Location("loc1"), new AgentSite("s", null, "1")));
        complex = new Complex(agents);
        assertTrue(translator.hasLinksWithChannels(complex));
        assertTrue(translator.hasLinksWithChannels(agents));
    }
    
    @Test
    public void testGetValidLocatedTransforms() throws Exception {
        
        Transform transform = new Transform("label", 
                getList(new Agent("A", new AgentSite("s", null, null))), 
                getList(new Agent("A", new AgentSite("s", null, "1")), new Agent("B", new AgentSite("s", null, "1"))), 10f, false);
        
        List<Channel> channels = new ArrayList<Channel>();
        channels.add(new Channel("intra", new Location("loc2", INDEX_X), new Location("loc2", INDEX_X_PLUS_1)));
        channels.add(new Channel("inter", new Location("loc1"), new Location("loc2", INDEX_2)));
        
        List<Compartment> compartments = new ArrayList<Compartment>();
        compartments.add(new Compartment("loc1"));
        compartments.add(new Compartment("loc2", 3));
        compartments.add(new Compartment("loc3", 3));
        

        try {
            translator.getValidLocatedTransforms(null, compartments, channels);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            translator.getValidLocatedTransforms(transform, null, channels);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            translator.getValidLocatedTransforms(transform, compartments, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        // No location
        
        List<Transform> expected = new ArrayList<Transform>();
        expected.add(new Transform("label", getList(new Agent("A", new AgentSite("s", null, null))), 
                getList(new Agent("A", new AgentSite("s", null, "1")), new Agent("B", new AgentSite("s", null, "1"))), 10f, false));
        
        checkTransforms(expected, translator.getValidLocatedTransforms(transform, compartments, channels));
        
        // Full location
        transform = new Transform("label", 
                getList(new Agent("A", new Location("loc1"))), 
                getList(new Agent("A", new Location("loc1"))), 10f, false);

        expected.clear();
        expected.add(new Transform("label", 
                getList(new Agent("A", new Location("loc1"))), 
                getList(new Agent("A", new Location("loc1"))), 10f, false));
        
        checkTransforms(expected, translator.getValidLocatedTransforms(transform, compartments, channels));
        
        transform = new Transform("label", 
                getList(new Agent("A", new Location("loc2", INDEX_1))), 
                getList(new Agent("A", new Location("loc2", INDEX_1))), 10f, false);

        expected.clear();
        expected.add(new Transform("label", 
                getList(new Agent("A", new Location("loc2", INDEX_1))), 
                getList(new Agent("A", new Location("loc2", INDEX_1))), 10f, false));
        
        checkTransforms(expected, translator.getValidLocatedTransforms(transform, compartments, channels));
        
        // Single unlinked partial location
        transform = new Transform("label", 
                getList(new Agent("A", new Location("loc2"))), 
                getList(new Agent("A", new Location("loc2"))), 10f, false);

        expected.clear();
        expected.add(new Transform("label-1", 
                getList(new Agent("A", new Location("loc2", INDEX_0))), 
                getList(new Agent("A", new Location("loc2", INDEX_0))), 10f, false));
        expected.add(new Transform("label-2", 
                getList(new Agent("A", new Location("loc2", INDEX_1))), 
                getList(new Agent("A", new Location("loc2", INDEX_1))), 10f, false));
        expected.add(new Transform("label-3", 
                getList(new Agent("A", new Location("loc2", INDEX_2))), 
                getList(new Agent("A", new Location("loc2", INDEX_2))), 10f, false));
        
        checkTransforms(expected, translator.getValidLocatedTransforms(transform, compartments, channels));
        
        // Multiple unlinked partial locations should fail
        transform = new Transform("label", 
                getList(new Agent("A", new Location("loc2"))), 
                getList(new Agent("B", new Location("loc3"))), 10f, false);

        try {
            translator.getValidLocatedTransforms(transform, compartments, channels);
            fail("invalid transform should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        // Dimer unlinking same full location, no channel
        transform = new Transform("label", 
                getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1")), 
                        new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, "1"))),
                getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, null)), 
                        new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, null))), 10f, false);

        expected.clear();
        expected.add(new Transform("label", 
                getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1")), 
                        new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, "1"))),
                getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, null)), 
                        new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, null))), 10f, false));
        
        checkTransforms(expected, translator.getValidLocatedTransforms(transform, compartments, channels));
        
        // Dimer linked, same partial location, no channel
        transform = new Transform("label", 
                getList(new Agent("A", new Location("loc2"), new AgentSite("s", null, "1")), 
                        new Agent("B", new Location("loc2"), new AgentSite("s", null, "1"))),
                getList(new Agent("A", new Location("loc2"), new AgentSite("s", null, null)), 
                        new Agent("B", new Location("loc2"), new AgentSite("s", null, null))), 10f, false);

        expected.clear();
        expected.add(new Transform("label-1", 
                getList(new Agent("A", new Location("loc2", INDEX_0), new AgentSite("s", null, "1")), 
                        new Agent("B", new Location("loc2", INDEX_0), new AgentSite("s", null, "1"))),
                getList(new Agent("A", new Location("loc2", INDEX_0), new AgentSite("s", null, null)), 
                        new Agent("B", new Location("loc2", INDEX_0), new AgentSite("s", null, null))), 10f, false));
        expected.add(new Transform("label-2", 
                getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1")), 
                        new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, "1"))),
                getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, null)), 
                        new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, null))), 10f, false));
        expected.add(new Transform("label-3", 
                getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, "1")), 
                        new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, "1"))),
                getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, null)), 
                        new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, null))), 10f, false));
        
        checkTransforms(expected, translator.getValidLocatedTransforms(transform, compartments, channels));
        
        // No label
        transform = new Transform(null, 
                getList(new Agent("A", new Location("loc2"), new AgentSite("s", null, "1")), 
                        new Agent("B", new Location("loc2"), new AgentSite("s", null, "1"))),
                getList(new Agent("A", new Location("loc2"), new AgentSite("s", null, null)), 
                        new Agent("B", new Location("loc2"), new AgentSite("s", null, null))), 10f, false);

        expected.clear();
        expected.add(new Transform(null, 
                getList(new Agent("A", new Location("loc2", INDEX_0), new AgentSite("s", null, "1")), 
                        new Agent("B", new Location("loc2", INDEX_0), new AgentSite("s", null, "1"))),
                getList(new Agent("A", new Location("loc2", INDEX_0), new AgentSite("s", null, null)), 
                        new Agent("B", new Location("loc2", INDEX_0), new AgentSite("s", null, null))), 10f, false));
        expected.add(new Transform(null, 
                getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, "1")), 
                        new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, "1"))),
                getList(new Agent("A", new Location("loc2", INDEX_1), new AgentSite("s", null, null)), 
                        new Agent("B", new Location("loc2", INDEX_1), new AgentSite("s", null, null))), 10f, false));
        expected.add(new Transform(null, 
                getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, "1")), 
                        new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, "1"))),
                getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, null)), 
                        new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, null))), 10f, false));
        
        checkTransforms(expected, translator.getValidLocatedTransforms(transform, compartments, channels));
        
        transform = new Transform("label", 
                getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, "1")), 
                        new Agent("B", new Location("loc2"), new AgentSite("s", null, "1"))),
                getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, null)), 
                        new Agent("B", new Location("loc2"), new AgentSite("s", null, null))), 10f, false);

        expected.clear();
        expected.add(new Transform("label", 
                getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, "1")), 
                        new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, "1"))),
                getList(new Agent("A", new Location("loc2", INDEX_2), new AgentSite("s", null, null)), 
                        new Agent("B", new Location("loc2", INDEX_2), new AgentSite("s", null, null))), 10f, false));
        
        checkTransforms(expected, translator.getValidLocatedTransforms(transform, compartments, channels));
        

        
    }
    
    private void checkTransforms(List<Transform> expectedTransforms, List<Transform> actualTransforms) {
        assertEquals(expectedTransforms.size(), actualTransforms.size());
        
        for (int index = 0; index < expectedTransforms.size(); index++) {
            assertEquals(expectedTransforms.get(index).toString(), actualTransforms.get(index).toString());
        }
    }
    
    @Test
    public void testGetMergedLocation() {
        
        try {
            translator.getMergedLocation(null, new Location("A"));
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            translator.getMergedLocation(new Location("A"), null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            translator.getMergedLocation(new Location("A"), new Location("B"));
            fail("mismatch should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        try {
            translator.getMergedLocation(new Location("A", INDEX_1), new Location("A", INDEX_2));
            fail("mismatch should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        assertEquals(NOT_LOCATED, 
                translator.getMergedLocation(NOT_LOCATED, NOT_LOCATED));
        assertEquals(new Location("A", INDEX_1), 
                translator.getMergedLocation(NOT_LOCATED, new Location("A", INDEX_1)));
        assertEquals(new Location("A", INDEX_1), 
                translator.getMergedLocation(new Location("A", INDEX_1), NOT_LOCATED));
        assertEquals(new Location("A", INDEX_1), 
                translator.getMergedLocation(new Location("A"), new Location("A", INDEX_1)));
        assertEquals(new Location("A", INDEX_1), 
                translator.getMergedLocation(new Location("A"), new Location("A", INDEX_1)));
        assertEquals(new Location("A"), 
                translator.getMergedLocation(new Location("A"), new Location("A")));
    }

    
    @Test
    public void testGetUnmergedAgents() {
        
        Agent templateAgent = new Agent("template", new Location("T"), new AgentSite("name", "state", "3", "channel"));
        Agent mergedAgent = new Agent("merged", new Location("M"));
        Agent locatedAgent = new Agent("located", new Location("L"));
        
        List<Agent> templateAgents = getList(templateAgent);
        Map<Agent, Agent> mergedLocatedMap = new HashMap<Agent, Agent>();
        Map<Agent, Agent> templateMergedMap = new HashMap<Agent, Agent>();
        
        mergedLocatedMap.put(mergedAgent, locatedAgent);
        templateMergedMap.put(templateAgent, mergedAgent);
        
        try {
            translator.getUnmergedAgents(null, mergedLocatedMap, templateMergedMap);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            translator.getUnmergedAgents(templateAgents, null, templateMergedMap);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            translator.getUnmergedAgents(templateAgents, mergedLocatedMap, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        mergedLocatedMap.clear();
        
        try {
            translator.getUnmergedAgents(templateAgents, mergedLocatedMap, templateMergedMap);
            fail("missing map entry should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        mergedLocatedMap.put(mergedAgent, locatedAgent);
        templateMergedMap.clear();
        
        try {
            translator.getUnmergedAgents(templateAgents, mergedLocatedMap, templateMergedMap);
            fail("missing map entry should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        mergedLocatedMap.put(mergedAgent, locatedAgent);
        templateMergedMap.put(templateAgent, mergedAgent);
        
        List<Agent> expected = getList(new Agent("template", new Location("L"), new AgentSite("name", "state", "3", "channel")));
        assertEquals(expected.toString(), 
                translator.getUnmergedAgents(templateAgents, mergedLocatedMap, templateMergedMap).toString());
    }
    
    private static final String SIMPLE_TEST_INPUT = 
        "%agent: A(state~blue~red~green)\n" +
        "%compartment: cytosol [4]\n" + 
        "%channel: intra-cytosol (cytosol [x] -> cytosol [x+1]) + (cytosol [x] -> cytosol [x -1])\n" + 
        "\n" + 
        "%transport: 'diffusion-all' intra-cytosol @ 0.1\n" + 
        "%transport: 'diffusion-red' intra-cytosol A(state~red) @ 0.1\n" + 
        "'heating' cytosol[0] A(state~blue) -> A(state~red) @ 1.0\n" + 
        "'cooling' cytosol A(state~red) -> A(state~blue) @ 0.05\n" + 
        "\n" + 
        "%var: 'green count' 800\n" + 
        "%init: 800 cytosol A(state~blue) \n" + 
        "%init: 'green count' A(state~green) \n" + 
        "%init: 800 cytosol[0] A(state~red) \n" + 
        "%var: 'constant' 12\n" + 
        "%var: 'all A not plotted' A()\n" + 
        "%var: 'all A plotted' A()\n" + 
        "%plot: 'all A plotted'\n" + 
        "%obs: 'all red' A(state~red)\n" + 
        "%obs: 'cytosol blue' cytosol A(state~blue)\n" + 
        "%obs: 'red[0]' cytosol[0] A(state~red) \n" + 
        "%obs: 'red[1]' cytosol[1] A(state~red) \n" + 
        "%obs: 'red[2]' cytosol[2] A(state~red) \n" + 
        "%obs: 'red[3]' cytosol[3] A(state~red) \n";
    
    private static final String SIMPLE_TEST_OUTPUT = 
        "%agent: A(state~blue~red~green,loc~cytosol,loc_index_1~0~1~2~3)\n" + 
        "\n" + 
        "'diffusion-all-1' A(loc~cytosol,loc_index_1~0) -> A(loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-all-2' A(loc~cytosol,loc_index_1~1) -> A(loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-all-3' A(loc~cytosol,loc_index_1~2) -> A(loc~cytosol,loc_index_1~3) @ 0.1\n" + 
        "'diffusion-all-4' A(loc~cytosol,loc_index_1~1) -> A(loc~cytosol,loc_index_1~0) @ 0.1\n" + 
        "'diffusion-all-5' A(loc~cytosol,loc_index_1~2) -> A(loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-all-6' A(loc~cytosol,loc_index_1~3) -> A(loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-red-1' A(state~red,loc~cytosol,loc_index_1~0) -> A(state~red,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-red-2' A(state~red,loc~cytosol,loc_index_1~1) -> A(state~red,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-red-3' A(state~red,loc~cytosol,loc_index_1~2) -> A(state~red,loc~cytosol,loc_index_1~3) @ 0.1\n" + 
        "'diffusion-red-4' A(state~red,loc~cytosol,loc_index_1~1) -> A(state~red,loc~cytosol,loc_index_1~0) @ 0.1\n" + 
        "'diffusion-red-5' A(state~red,loc~cytosol,loc_index_1~2) -> A(state~red,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-red-6' A(state~red,loc~cytosol,loc_index_1~3) -> A(state~red,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "\n" + 
        "'heating' A(state~blue,loc~cytosol,loc_index_1~0) -> A(state~red,loc~cytosol,loc_index_1~0) @ 1.0\n" + 
        "'cooling-1' A(state~red,loc~cytosol,loc_index_1~0) -> A(state~blue,loc~cytosol,loc_index_1~0) @ 0.05\n" + 
        "'cooling-2' A(state~red,loc~cytosol,loc_index_1~1) -> A(state~blue,loc~cytosol,loc_index_1~1) @ 0.05\n" + 
        "'cooling-3' A(state~red,loc~cytosol,loc_index_1~2) -> A(state~blue,loc~cytosol,loc_index_1~2) @ 0.05\n" + 
        "'cooling-4' A(state~red,loc~cytosol,loc_index_1~3) -> A(state~blue,loc~cytosol,loc_index_1~3) @ 0.05\n" + 
        "\n" + 
        "%init: 200 A(state~blue,loc~cytosol,loc_index_1~0)\n" + 
        "%init: 200 A(state~blue,loc~cytosol,loc_index_1~1)\n" + 
        "%init: 200 A(state~blue,loc~cytosol,loc_index_1~2)\n" + 
        "%init: 200 A(state~blue,loc~cytosol,loc_index_1~3)\n" + 
        "%init: 800 A(state~green)\n" + 
        "%init: 800 A(state~red,loc~cytosol,loc_index_1~0)\n" + 
        "\n" + 
        "%var: 'green count' 800.0\n" + 
        "%var: 'constant' 12.0\n" + 
        "%var: 'all A not plotted' A\n" + 
        "%var: 'all A plotted' A\n" + 
        "%var: 'all red' A(state~red)\n" + 
        "%var: 'cytosol blue' A(state~blue,loc~cytosol)\n" + 
        "%var: 'red[0]' A(state~red,loc~cytosol,loc_index_1~0)\n" + 
        "%var: 'red[1]' A(state~red,loc~cytosol,loc_index_1~1)\n" + 
        "%var: 'red[2]' A(state~red,loc~cytosol,loc_index_1~2)\n" + 
        "%var: 'red[3]' A(state~red,loc~cytosol,loc_index_1~3)\n" + 
        "%plot: 'all A plotted'\n" + 
        "%plot: 'all red'\n" + 
        "%plot: 'cytosol blue'\n" + 
        "%plot: 'red[0]'\n" + 
        "%plot: 'red[1]'\n" + 
        "%plot: 'red[2]'\n" + 
        "%plot: 'red[3]'\n" + 
        "\n" + 
        "";

    private static final String INFINITE_RATE_INPUT = 
        "%agent: A(state~blue~red~green)\n" +
        "%compartment: cytosol [4]\n" + 
        "%channel: intra-cytosol (cytosol [x] -> cytosol [x+1]) + (cytosol [x] -> cytosol [x -1])\n" + 
        "\n" + 
        "%transport: 'diffusion-all' intra-cytosol @ [inf]\n" + 
        "%transport: 'diffusion-red' intra-cytosol A(state~red) @ [inf]\n" + 
        "'heating' cytosol[0] A(state~blue) -> A(state~red) @ [inf]\n" + 
        "'cooling' cytosol A(state~red) -> A(state~blue) @ [inf]\n" + 
        "\n" + 
        "%init: 800 cytosol A(state~blue) \n" + 
        "%init: 800 A(state~green) \n" + 
        "%init: 800 cytosol[0] A(state~red) \n" + 
        "%obs: 'all red' A(state~red)\n" + 
        "%obs: 'cytosol blue' cytosol A(state~blue)\n" + 
        "%obs: 'red[0]' cytosol[0] A(state~red) \n" + 
        "%obs: 'red[1]' cytosol[1] A(state~red) \n" + 
        "%obs: 'red[2]' cytosol[2] A(state~red) \n" + 
        "%obs: 'red[3]' cytosol[3] A(state~red) \n";
    
    private static final String INFINITE_RATE_OUTPUT = 
        "%agent: A(state~blue~red~green,loc~cytosol,loc_index_1~0~1~2~3)\n" + 
        "\n" + 
        "'diffusion-all-1' A(loc~cytosol,loc_index_1~0) -> A(loc~cytosol,loc_index_1~1) @ [inf]\n" + 
        "'diffusion-all-2' A(loc~cytosol,loc_index_1~1) -> A(loc~cytosol,loc_index_1~2) @ [inf]\n" + 
        "'diffusion-all-3' A(loc~cytosol,loc_index_1~2) -> A(loc~cytosol,loc_index_1~3) @ [inf]\n" + 
        "'diffusion-all-4' A(loc~cytosol,loc_index_1~1) -> A(loc~cytosol,loc_index_1~0) @ [inf]\n" + 
        "'diffusion-all-5' A(loc~cytosol,loc_index_1~2) -> A(loc~cytosol,loc_index_1~1) @ [inf]\n" + 
        "'diffusion-all-6' A(loc~cytosol,loc_index_1~3) -> A(loc~cytosol,loc_index_1~2) @ [inf]\n" + 
        "'diffusion-red-1' A(state~red,loc~cytosol,loc_index_1~0) -> A(state~red,loc~cytosol,loc_index_1~1) @ [inf]\n" + 
        "'diffusion-red-2' A(state~red,loc~cytosol,loc_index_1~1) -> A(state~red,loc~cytosol,loc_index_1~2) @ [inf]\n" + 
        "'diffusion-red-3' A(state~red,loc~cytosol,loc_index_1~2) -> A(state~red,loc~cytosol,loc_index_1~3) @ [inf]\n" + 
        "'diffusion-red-4' A(state~red,loc~cytosol,loc_index_1~1) -> A(state~red,loc~cytosol,loc_index_1~0) @ [inf]\n" + 
        "'diffusion-red-5' A(state~red,loc~cytosol,loc_index_1~2) -> A(state~red,loc~cytosol,loc_index_1~1) @ [inf]\n" + 
        "'diffusion-red-6' A(state~red,loc~cytosol,loc_index_1~3) -> A(state~red,loc~cytosol,loc_index_1~2) @ [inf]\n" + 
        "\n" + 
        "'heating' A(state~blue,loc~cytosol,loc_index_1~0) -> A(state~red,loc~cytosol,loc_index_1~0) @ [inf]\n" + 
        "'cooling-1' A(state~red,loc~cytosol,loc_index_1~0) -> A(state~blue,loc~cytosol,loc_index_1~0) @ [inf]\n" + 
        "'cooling-2' A(state~red,loc~cytosol,loc_index_1~1) -> A(state~blue,loc~cytosol,loc_index_1~1) @ [inf]\n" + 
        "'cooling-3' A(state~red,loc~cytosol,loc_index_1~2) -> A(state~blue,loc~cytosol,loc_index_1~2) @ [inf]\n" + 
        "'cooling-4' A(state~red,loc~cytosol,loc_index_1~3) -> A(state~blue,loc~cytosol,loc_index_1~3) @ [inf]\n" + 
        "\n" + 
        "%init: 200 A(state~blue,loc~cytosol,loc_index_1~0)\n" + 
        "%init: 200 A(state~blue,loc~cytosol,loc_index_1~1)\n" + 
        "%init: 200 A(state~blue,loc~cytosol,loc_index_1~2)\n" + 
        "%init: 200 A(state~blue,loc~cytosol,loc_index_1~3)\n" + 
        "%init: 800 A(state~green)\n" + 
        "%init: 800 A(state~red,loc~cytosol,loc_index_1~0)\n" + 
        "\n" + 
        "%var: 'all red' A(state~red)\n" + 
        "%var: 'cytosol blue' A(state~blue,loc~cytosol)\n" + 
        "%var: 'red[0]' A(state~red,loc~cytosol,loc_index_1~0)\n" + 
        "%var: 'red[1]' A(state~red,loc~cytosol,loc_index_1~1)\n" + 
        "%var: 'red[2]' A(state~red,loc~cytosol,loc_index_1~2)\n" + 
        "%var: 'red[3]' A(state~red,loc~cytosol,loc_index_1~3)\n" + 
        "%plot: 'all red'\n" + 
        "%plot: 'cytosol blue'\n" + 
        "%plot: 'red[0]'\n" + 
        "%plot: 'red[1]'\n" + 
        "%plot: 'red[2]'\n" + 
        "%plot: 'red[3]'\n" + 
        "\n" + 
        "";

    private static final String TRANSPORT_ONLY_INPUT = 
        "%agent: A(state~red)\n" + 
        "%agent: B\n" +
        "%compartment: cytosol [4]\n" + 
        "%compartment: membrane [4]\n" + 
        "%channel: intra-cytosol (cytosol [x] -> cytosol [x+1]) + (cytosol [x] -> cytosol [x -1])\n" + 
        "%channel: interface1 cytosol [x] -> membrane [x+2]\n" +
        "%channel: interface2 cytosol [x] -> membrane [x -2]\n" +
        "\n" + 
        "%transport: 'diffusion-all' intra-cytosol @ 0.1\n" + // TODO - remember only works for single agents
        "%transport: 'diffusion-red' intra-cytosol A(state~red) @ 0.1\n" + 
        "%transport: 'diffusion-other' intra-cytosol B() @ 0.1\n" + 
        "%transport: 'cell exit1' interface1 B() @ 0.2\n" + 
        "%transport: 'cell exit2' interface2 B() @ 0.3\n" + 
        "%init: 800 A(state~red),B()\n" + 
        "\n" + 
        "";
    
    private static final String TRANSPORT_ONLY_OUTPUT = 
        "%agent: A(state~red,loc~cytosol~membrane,loc_index_1~0~1~2~3)\n" + 
        "%agent: B(loc~cytosol~membrane,loc_index_1~0~1~2~3)\n" +
        "\n" + 
        "'diffusion-all-1' A(loc~cytosol,loc_index_1~0) -> A(loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-all-2' A(loc~cytosol,loc_index_1~1) -> A(loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-all-3' A(loc~cytosol,loc_index_1~2) -> A(loc~cytosol,loc_index_1~3) @ 0.1\n" + 
        "'diffusion-all-4' A(loc~cytosol,loc_index_1~1) -> A(loc~cytosol,loc_index_1~0) @ 0.1\n" + 
        "'diffusion-all-5' A(loc~cytosol,loc_index_1~2) -> A(loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-all-6' A(loc~cytosol,loc_index_1~3) -> A(loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-all-7' B(loc~cytosol,loc_index_1~0) -> B(loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-all-8' B(loc~cytosol,loc_index_1~1) -> B(loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-all-9' B(loc~cytosol,loc_index_1~2) -> B(loc~cytosol,loc_index_1~3) @ 0.1\n" + 
        "'diffusion-all-10' B(loc~cytosol,loc_index_1~1) -> B(loc~cytosol,loc_index_1~0) @ 0.1\n" + 
        "'diffusion-all-11' B(loc~cytosol,loc_index_1~2) -> B(loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-all-12' B(loc~cytosol,loc_index_1~3) -> B(loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-red-1' A(state~red,loc~cytosol,loc_index_1~0) -> A(state~red,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-red-2' A(state~red,loc~cytosol,loc_index_1~1) -> A(state~red,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-red-3' A(state~red,loc~cytosol,loc_index_1~2) -> A(state~red,loc~cytosol,loc_index_1~3) @ 0.1\n" + 
        "'diffusion-red-4' A(state~red,loc~cytosol,loc_index_1~1) -> A(state~red,loc~cytosol,loc_index_1~0) @ 0.1\n" + 
        "'diffusion-red-5' A(state~red,loc~cytosol,loc_index_1~2) -> A(state~red,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-red-6' A(state~red,loc~cytosol,loc_index_1~3) -> A(state~red,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-other-1' B(loc~cytosol,loc_index_1~0) -> B(loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-other-2' B(loc~cytosol,loc_index_1~1) -> B(loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-other-3' B(loc~cytosol,loc_index_1~2) -> B(loc~cytosol,loc_index_1~3) @ 0.1\n" + 
        "'diffusion-other-4' B(loc~cytosol,loc_index_1~1) -> B(loc~cytosol,loc_index_1~0) @ 0.1\n" + 
        "'diffusion-other-5' B(loc~cytosol,loc_index_1~2) -> B(loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-other-6' B(loc~cytosol,loc_index_1~3) -> B(loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'cell exit1-1' B(loc~cytosol,loc_index_1~0) -> B(loc~membrane,loc_index_1~2) @ 0.2\n" + 
        "'cell exit1-2' B(loc~cytosol,loc_index_1~1) -> B(loc~membrane,loc_index_1~3) @ 0.2\n" + 
        "'cell exit2-1' B(loc~cytosol,loc_index_1~2) -> B(loc~membrane,loc_index_1~0) @ 0.3\n" + 
        "'cell exit2-2' B(loc~cytosol,loc_index_1~3) -> B(loc~membrane,loc_index_1~1) @ 0.3\n" + 
        "\n" +
        "\n" + 
        "%init: 800 A(state~red)\n" + 
        "%init: 800 B\n" + 
        "\n" + 
        "\n" + 
        "";


    // TODO - test localised agent and complex creation
    
    private static final String TRANSPORT_DIFFERENT_DIMENSIONS_INPUT = 
        "%agent: A(state~blue~red)\n" + 
        "%agent: B\n" + 
        "%compartment: cytosol [4][5]\n" + 
        "%compartment: membrane [3]\n" + 
        "%compartment: nucleus\n" + 
        "%channel: interface1 (nucleus -> membrane [2]) + (membrane [2] -> nucleus)\n" +
        "%channel: interface2 (nucleus -> cytosol [1][2]) + (cytosol [1][2] -> nucleus)\n" +
        "%channel: interface3 (membrane [x] -> cytosol [x][x+2]) + (cytosol [x][y] -> membrane [x + ((x - (y - 2)) * 100)])\n" +
        "\n" + 
        "%transport: 'diffusion-blue' interface1  A(state~blue) @ 0.1\n" + 
        "%transport: 'diffusion-red' interface2 A(state~red) @ 0.2\n" + 
        "%transport: 'diffusion-other' interface3 B() @ 0.3\n" + 
        "\n" +
        "%init: 800 cytosol A(state~blue) \n" + 
        "%init: 900 membrane A(state~red) \n" + 
        "%init: 800 nucleus B() \n" + 
        "";
    
    private static final String TRANSPORT_DIFFERENT_DIMENSIONS_OUTPUT = 
        "%agent: A(state~blue~red,loc~cytosol~membrane~nucleus,loc_index_1~0~1~2~3,loc_index_2~0~1~2~3~4)\n" + 
        "%agent: B(loc~cytosol~membrane~nucleus,loc_index_1~0~1~2~3,loc_index_2~0~1~2~3~4)\n" + 
        "\n" + 
        "'diffusion-blue-1' A(state~blue,loc~nucleus,loc_index_1~0,loc_index_2~0) -> A(state~blue,loc~membrane,loc_index_1~2,loc_index_2~0) @ 0.1\n" + 
        "'diffusion-blue-2' A(state~blue,loc~membrane,loc_index_1~2,loc_index_2~0) -> A(state~blue,loc~nucleus,loc_index_1~0,loc_index_2~0) @ 0.1\n" + 
        "'diffusion-red-1' A(state~red,loc~nucleus,loc_index_1~0,loc_index_2~0) -> A(state~red,loc~cytosol,loc_index_1~1,loc_index_2~2) @ 0.2\n" + 
        "'diffusion-red-2' A(state~red,loc~cytosol,loc_index_1~1,loc_index_2~2) -> A(state~red,loc~nucleus,loc_index_1~0,loc_index_2~0) @ 0.2\n" + 
        "'diffusion-other-1' B(loc~membrane,loc_index_1~0,loc_index_2~0) -> B(loc~cytosol,loc_index_1~0,loc_index_2~2) @ 0.3\n" + 
        "'diffusion-other-2' B(loc~membrane,loc_index_1~1,loc_index_2~0) -> B(loc~cytosol,loc_index_1~1,loc_index_2~3) @ 0.3\n" + 
        "'diffusion-other-3' B(loc~membrane,loc_index_1~2,loc_index_2~0) -> B(loc~cytosol,loc_index_1~2,loc_index_2~4) @ 0.3\n" + 
        "'diffusion-other-4' B(loc~cytosol,loc_index_1~0,loc_index_2~2) -> B(loc~membrane,loc_index_1~0,loc_index_2~0) @ 0.3\n" + 
        "'diffusion-other-5' B(loc~cytosol,loc_index_1~1,loc_index_2~3) -> B(loc~membrane,loc_index_1~1,loc_index_2~0) @ 0.3\n" + 
        "'diffusion-other-6' B(loc~cytosol,loc_index_1~2,loc_index_2~4) -> B(loc~membrane,loc_index_1~2,loc_index_2~0) @ 0.3\n" + 
        "\n" + 
        "\n" + 
        "%init: 40 A(state~blue,loc~cytosol,loc_index_1~0,loc_index_2~0)\n" + 
        "%init: 40 A(state~blue,loc~cytosol,loc_index_1~1,loc_index_2~0)\n" + 
        "%init: 40 A(state~blue,loc~cytosol,loc_index_1~2,loc_index_2~0)\n" + 
        "%init: 40 A(state~blue,loc~cytosol,loc_index_1~3,loc_index_2~0)\n" + 
        "%init: 40 A(state~blue,loc~cytosol,loc_index_1~0,loc_index_2~1)\n" + 
        "%init: 40 A(state~blue,loc~cytosol,loc_index_1~1,loc_index_2~1)\n" + 
        "%init: 40 A(state~blue,loc~cytosol,loc_index_1~2,loc_index_2~1)\n" + 
        "%init: 40 A(state~blue,loc~cytosol,loc_index_1~3,loc_index_2~1)\n" + 
        "%init: 40 A(state~blue,loc~cytosol,loc_index_1~0,loc_index_2~2)\n" + 
        "%init: 40 A(state~blue,loc~cytosol,loc_index_1~1,loc_index_2~2)\n" + 
        "%init: 40 A(state~blue,loc~cytosol,loc_index_1~2,loc_index_2~2)\n" + 
        "%init: 40 A(state~blue,loc~cytosol,loc_index_1~3,loc_index_2~2)\n" + 
        "%init: 40 A(state~blue,loc~cytosol,loc_index_1~0,loc_index_2~3)\n" + 
        "%init: 40 A(state~blue,loc~cytosol,loc_index_1~1,loc_index_2~3)\n" + 
        "%init: 40 A(state~blue,loc~cytosol,loc_index_1~2,loc_index_2~3)\n" + 
        "%init: 40 A(state~blue,loc~cytosol,loc_index_1~3,loc_index_2~3)\n" + 
        "%init: 40 A(state~blue,loc~cytosol,loc_index_1~0,loc_index_2~4)\n" + 
        "%init: 40 A(state~blue,loc~cytosol,loc_index_1~1,loc_index_2~4)\n" + 
        "%init: 40 A(state~blue,loc~cytosol,loc_index_1~2,loc_index_2~4)\n" + 
        "%init: 40 A(state~blue,loc~cytosol,loc_index_1~3,loc_index_2~4)\n" + 
        "%init: 300 A(state~red,loc~membrane,loc_index_1~0)\n" + 
        "%init: 300 A(state~red,loc~membrane,loc_index_1~1)\n" + 
        "%init: 300 A(state~red,loc~membrane,loc_index_1~2)\n" + 
        "%init: 800 B(loc~nucleus)\n" + 
        "\n" + 
        "\n" + 
        "";

    private static final String TRANSPORT_6WAY_INPUT = 
		"%agent: RED\n" + 
        "%compartment: cytosol [3][3]\n" + 
        "%channel: 6way (cytosol [x][y] -> cytosol [x+1][y]) + (cytosol [x][y] -> cytosol [x -1][y]) \n" + 
        "%channel: 6way (cytosol [x][y] -> cytosol [x][y+1]) + (cytosol [x][y] -> cytosol [x][y -1]) \n" + 
        "%channel: 6way (cytosol [x][y] -> cytosol [x+1][(y+1)-(2*(x%2))]) + (cytosol [x][y] -> cytosol [x -1][(y -1)+(2*((x -1)%2))]) \n" + 
        "\n" + 
        "%transport: 'diffusion RED' 6way RED() @ 0.05 \n" + 
        "%init: 40 RED()\n" + 
        "";
    
    private static final String TRANSPORT_6WAY_OUTPUT = 
		"%agent: RED(loc~cytosol,loc_index_1~0~1~2,loc_index_2~0~1~2)\n" + 
        "\n" + 
        "'diffusion RED-1' RED(loc~cytosol,loc_index_1~0,loc_index_2~0) -> RED(loc~cytosol,loc_index_1~1,loc_index_2~0) @ 0.05\n" + 
        "'diffusion RED-2' RED(loc~cytosol,loc_index_1~0,loc_index_2~1) -> RED(loc~cytosol,loc_index_1~1,loc_index_2~1) @ 0.05\n" + 
        "'diffusion RED-3' RED(loc~cytosol,loc_index_1~0,loc_index_2~2) -> RED(loc~cytosol,loc_index_1~1,loc_index_2~2) @ 0.05\n" + 
        "'diffusion RED-4' RED(loc~cytosol,loc_index_1~1,loc_index_2~0) -> RED(loc~cytosol,loc_index_1~2,loc_index_2~0) @ 0.05\n" + 
        "'diffusion RED-5' RED(loc~cytosol,loc_index_1~1,loc_index_2~1) -> RED(loc~cytosol,loc_index_1~2,loc_index_2~1) @ 0.05\n" + 
        "'diffusion RED-6' RED(loc~cytosol,loc_index_1~1,loc_index_2~2) -> RED(loc~cytosol,loc_index_1~2,loc_index_2~2) @ 0.05\n" + 
        "'diffusion RED-7' RED(loc~cytosol,loc_index_1~1,loc_index_2~0) -> RED(loc~cytosol,loc_index_1~0,loc_index_2~0) @ 0.05\n" + 
        "'diffusion RED-8' RED(loc~cytosol,loc_index_1~1,loc_index_2~1) -> RED(loc~cytosol,loc_index_1~0,loc_index_2~1) @ 0.05\n" + 
        "'diffusion RED-9' RED(loc~cytosol,loc_index_1~1,loc_index_2~2) -> RED(loc~cytosol,loc_index_1~0,loc_index_2~2) @ 0.05\n" + 
        "'diffusion RED-10' RED(loc~cytosol,loc_index_1~2,loc_index_2~0) -> RED(loc~cytosol,loc_index_1~1,loc_index_2~0) @ 0.05\n" + 
        "'diffusion RED-11' RED(loc~cytosol,loc_index_1~2,loc_index_2~1) -> RED(loc~cytosol,loc_index_1~1,loc_index_2~1) @ 0.05\n" + 
        "'diffusion RED-12' RED(loc~cytosol,loc_index_1~2,loc_index_2~2) -> RED(loc~cytosol,loc_index_1~1,loc_index_2~2) @ 0.05\n" + 
        "'diffusion RED-13' RED(loc~cytosol,loc_index_1~0,loc_index_2~0) -> RED(loc~cytosol,loc_index_1~0,loc_index_2~1) @ 0.05\n" + 
        "'diffusion RED-14' RED(loc~cytosol,loc_index_1~0,loc_index_2~1) -> RED(loc~cytosol,loc_index_1~0,loc_index_2~2) @ 0.05\n" + 
        "'diffusion RED-15' RED(loc~cytosol,loc_index_1~1,loc_index_2~0) -> RED(loc~cytosol,loc_index_1~1,loc_index_2~1) @ 0.05\n" + 
        "'diffusion RED-16' RED(loc~cytosol,loc_index_1~1,loc_index_2~1) -> RED(loc~cytosol,loc_index_1~1,loc_index_2~2) @ 0.05\n" + 
        "'diffusion RED-17' RED(loc~cytosol,loc_index_1~2,loc_index_2~0) -> RED(loc~cytosol,loc_index_1~2,loc_index_2~1) @ 0.05\n" + 
        "'diffusion RED-18' RED(loc~cytosol,loc_index_1~2,loc_index_2~1) -> RED(loc~cytosol,loc_index_1~2,loc_index_2~2) @ 0.05\n" + 
        "'diffusion RED-19' RED(loc~cytosol,loc_index_1~0,loc_index_2~1) -> RED(loc~cytosol,loc_index_1~0,loc_index_2~0) @ 0.05\n" + 
        "'diffusion RED-20' RED(loc~cytosol,loc_index_1~0,loc_index_2~2) -> RED(loc~cytosol,loc_index_1~0,loc_index_2~1) @ 0.05\n" + 
        "'diffusion RED-21' RED(loc~cytosol,loc_index_1~1,loc_index_2~1) -> RED(loc~cytosol,loc_index_1~1,loc_index_2~0) @ 0.05\n" + 
        "'diffusion RED-22' RED(loc~cytosol,loc_index_1~1,loc_index_2~2) -> RED(loc~cytosol,loc_index_1~1,loc_index_2~1) @ 0.05\n" + 
        "'diffusion RED-23' RED(loc~cytosol,loc_index_1~2,loc_index_2~1) -> RED(loc~cytosol,loc_index_1~2,loc_index_2~0) @ 0.05\n" + 
        "'diffusion RED-24' RED(loc~cytosol,loc_index_1~2,loc_index_2~2) -> RED(loc~cytosol,loc_index_1~2,loc_index_2~1) @ 0.05\n" + 
        "'diffusion RED-25' RED(loc~cytosol,loc_index_1~0,loc_index_2~0) -> RED(loc~cytosol,loc_index_1~1,loc_index_2~1) @ 0.05\n" + 
        "'diffusion RED-26' RED(loc~cytosol,loc_index_1~0,loc_index_2~1) -> RED(loc~cytosol,loc_index_1~1,loc_index_2~2) @ 0.05\n" + 
        "'diffusion RED-27' RED(loc~cytosol,loc_index_1~1,loc_index_2~1) -> RED(loc~cytosol,loc_index_1~2,loc_index_2~0) @ 0.05\n" + 
        "'diffusion RED-28' RED(loc~cytosol,loc_index_1~1,loc_index_2~2) -> RED(loc~cytosol,loc_index_1~2,loc_index_2~1) @ 0.05\n" + 
        "'diffusion RED-29' RED(loc~cytosol,loc_index_1~1,loc_index_2~1) -> RED(loc~cytosol,loc_index_1~0,loc_index_2~0) @ 0.05\n" + 
        "'diffusion RED-30' RED(loc~cytosol,loc_index_1~1,loc_index_2~2) -> RED(loc~cytosol,loc_index_1~0,loc_index_2~1) @ 0.05\n" + 
        "'diffusion RED-31' RED(loc~cytosol,loc_index_1~2,loc_index_2~0) -> RED(loc~cytosol,loc_index_1~1,loc_index_2~1) @ 0.05\n" + 
        "'diffusion RED-32' RED(loc~cytosol,loc_index_1~2,loc_index_2~1) -> RED(loc~cytosol,loc_index_1~1,loc_index_2~2) @ 0.05\n" + 
        "\n" + 
        "\n" + 
        "%init: 40 RED\n" + 
        "\n" + 
        "\n" + 
        "";

    private static final String TRANSPORT_LIMITED_LINKS_INPUT = 
        "%agent: A(l1,l2)\n" +
        "%agent: B(l1)\n" +
        "%agent: C(l1,l2)\n" +
        "%agent: D(l1)\n" +
        "%compartment: cytosol [4]\n" + 
        "%channel: intra-cytosol (cytosol [x] -> cytosol [x+1]) + (cytosol [x] -> cytosol [x -1])\n" + 
        "\n" + 
        "%transport: 'diffusion-all' intra-cytosol @ 0.1\n" +
        "%transport: 'diffusion-complex' intra-cytosol A(l1!1),B(l1!1) @ 0.1\n" + 
        "%transport: 'diffusion-agent' intra-cytosol C() @ 0.1\n" + 
        "%obs: A(l1!1,l2!2),B(l1!1),C(l1!2,l2!3),D(l1!3)\n" + 
        "\n" + 
        "";
    
    private static final String TRANSPORT_LIMITED_LINKS_OUTPUT = 
        "%agent: A(l1,l2,loc~cytosol,loc_index_1~0~1~2~3)\n" +
        "%agent: B(l1,loc~cytosol,loc_index_1~0~1~2~3)\n" +
        "%agent: C(l1,l2,loc~cytosol,loc_index_1~0~1~2~3)\n" +
        "%agent: D(l1,loc~cytosol,loc_index_1~0~1~2~3)\n" +
        "\n" +
        "'diffusion-all-1' A(l1,l2,loc~cytosol,loc_index_1~0) -> A(l1,l2,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-all-2' A(l1,l2,loc~cytosol,loc_index_1~1) -> A(l1,l2,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-all-3' A(l1,l2,loc~cytosol,loc_index_1~2) -> A(l1,l2,loc~cytosol,loc_index_1~3) @ 0.1\n" + 
        "'diffusion-all-4' A(l1,l2,loc~cytosol,loc_index_1~1) -> A(l1,l2,loc~cytosol,loc_index_1~0) @ 0.1\n" + 
        "'diffusion-all-5' A(l1,l2,loc~cytosol,loc_index_1~2) -> A(l1,l2,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-all-6' A(l1,l2,loc~cytosol,loc_index_1~3) -> A(l1,l2,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-all-7' B(l1,loc~cytosol,loc_index_1~0) -> B(l1,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-all-8' B(l1,loc~cytosol,loc_index_1~1) -> B(l1,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-all-9' B(l1,loc~cytosol,loc_index_1~2) -> B(l1,loc~cytosol,loc_index_1~3) @ 0.1\n" + 
        "'diffusion-all-10' B(l1,loc~cytosol,loc_index_1~1) -> B(l1,loc~cytosol,loc_index_1~0) @ 0.1\n" + 
        "'diffusion-all-11' B(l1,loc~cytosol,loc_index_1~2) -> B(l1,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-all-12' B(l1,loc~cytosol,loc_index_1~3) -> B(l1,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-all-13' C(l1,l2,loc~cytosol,loc_index_1~0) -> C(l1,l2,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-all-14' C(l1,l2,loc~cytosol,loc_index_1~1) -> C(l1,l2,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-all-15' C(l1,l2,loc~cytosol,loc_index_1~2) -> C(l1,l2,loc~cytosol,loc_index_1~3) @ 0.1\n" + 
        "'diffusion-all-16' C(l1,l2,loc~cytosol,loc_index_1~1) -> C(l1,l2,loc~cytosol,loc_index_1~0) @ 0.1\n" + 
        "'diffusion-all-17' C(l1,l2,loc~cytosol,loc_index_1~2) -> C(l1,l2,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-all-18' C(l1,l2,loc~cytosol,loc_index_1~3) -> C(l1,l2,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-all-19' D(l1,loc~cytosol,loc_index_1~0) -> D(l1,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-all-20' D(l1,loc~cytosol,loc_index_1~1) -> D(l1,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-all-21' D(l1,loc~cytosol,loc_index_1~2) -> D(l1,loc~cytosol,loc_index_1~3) @ 0.1\n" + 
        "'diffusion-all-22' D(l1,loc~cytosol,loc_index_1~1) -> D(l1,loc~cytosol,loc_index_1~0) @ 0.1\n" + 
        "'diffusion-all-23' D(l1,loc~cytosol,loc_index_1~2) -> D(l1,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-all-24' D(l1,loc~cytosol,loc_index_1~3) -> D(l1,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-complex-1' A(l1!1,l2,loc~cytosol,loc_index_1~0),B(l1!1,loc~cytosol,loc_index_1~0) -> A(l1!1,l2,loc~cytosol,loc_index_1~1),B(l1!1,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-complex-2' A(l1!1,l2,loc~cytosol,loc_index_1~1),B(l1!1,loc~cytosol,loc_index_1~1) -> A(l1!1,l2,loc~cytosol,loc_index_1~2),B(l1!1,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-complex-3' A(l1!1,l2,loc~cytosol,loc_index_1~2),B(l1!1,loc~cytosol,loc_index_1~2) -> A(l1!1,l2,loc~cytosol,loc_index_1~3),B(l1!1,loc~cytosol,loc_index_1~3) @ 0.1\n" + 
        "'diffusion-complex-4' A(l1!1,l2,loc~cytosol,loc_index_1~1),B(l1!1,loc~cytosol,loc_index_1~1) -> A(l1!1,l2,loc~cytosol,loc_index_1~0),B(l1!1,loc~cytosol,loc_index_1~0) @ 0.1\n" + 
        "'diffusion-complex-5' A(l1!1,l2,loc~cytosol,loc_index_1~2),B(l1!1,loc~cytosol,loc_index_1~2) -> A(l1!1,l2,loc~cytosol,loc_index_1~1),B(l1!1,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-complex-6' A(l1!1,l2,loc~cytosol,loc_index_1~3),B(l1!1,loc~cytosol,loc_index_1~3) -> A(l1!1,l2,loc~cytosol,loc_index_1~2),B(l1!1,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-agent-1' C(l1,l2,loc~cytosol,loc_index_1~0) -> C(l1,l2,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-agent-2' C(l1,l2,loc~cytosol,loc_index_1~1) -> C(l1,l2,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-agent-3' C(l1,l2,loc~cytosol,loc_index_1~2) -> C(l1,l2,loc~cytosol,loc_index_1~3) @ 0.1\n" + 
        "'diffusion-agent-4' C(l1,l2,loc~cytosol,loc_index_1~1) -> C(l1,l2,loc~cytosol,loc_index_1~0) @ 0.1\n" + 
        "'diffusion-agent-5' C(l1,l2,loc~cytosol,loc_index_1~2) -> C(l1,l2,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-agent-6' C(l1,l2,loc~cytosol,loc_index_1~3) -> C(l1,l2,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "\n" + 
        "\n" + 
        "\n" + 
        "%var: '[A(l1!1,l2!2), B(l1!1), C(l1!2,l2!3), D(l1!3)]' A(l1!1,l2!2),B(l1!1),C(l1!2,l2!3),D(l1!3)\n" + 
        "%plot: '[A(l1!1,l2!2), B(l1!1), C(l1!2,l2!3), D(l1!3)]'\n" + 
        "\n" + 
        "";

    private static final String REACTION_ONLY_INPUT = 
        "%agent: A(state~blue~red)\n" + 
        "%agent: B\n" + 
        "%compartment: cytosol [4]\n" + 
        "'heating' cytosol[0] A(state~blue) -> A(state~red) @ 1.0\n" + 
        "'cooling' cytosol A(state~red) -> A(state~blue) @ 0.05\n" + 
        "'dimerisation' cytosol A(state),A(state) -> A(state!1),A(state!1) @ 0.05\n" + 
        "";
        
    private static final String REACTION_ONLY_OUTPUT = 
        "%agent: A(state~blue~red,loc~cytosol,loc_index_1~0~1~2~3)\n" + 
        "%agent: B(loc~cytosol,loc_index_1~0~1~2~3)\n" + 
        "\n" + 
        "\n" + 
        "'heating' A(state~blue,loc~cytosol,loc_index_1~0) -> A(state~red,loc~cytosol,loc_index_1~0) @ 1.0\n" + 
        "'cooling-1' A(state~red,loc~cytosol,loc_index_1~0) -> A(state~blue,loc~cytosol,loc_index_1~0) @ 0.05\n" + 
        "'cooling-2' A(state~red,loc~cytosol,loc_index_1~1) -> A(state~blue,loc~cytosol,loc_index_1~1) @ 0.05\n" + 
        "'cooling-3' A(state~red,loc~cytosol,loc_index_1~2) -> A(state~blue,loc~cytosol,loc_index_1~2) @ 0.05\n" + 
        "'cooling-4' A(state~red,loc~cytosol,loc_index_1~3) -> A(state~blue,loc~cytosol,loc_index_1~3) @ 0.05\n" + 
        "'dimerisation-1' A(state,loc~cytosol,loc_index_1~0),A(state,loc~cytosol,loc_index_1~0) -> A(state!1,loc~cytosol,loc_index_1~0),A(state!1,loc~cytosol,loc_index_1~0) @ 0.05\n" + 
        "'dimerisation-2' A(state,loc~cytosol,loc_index_1~1),A(state,loc~cytosol,loc_index_1~1) -> A(state!1,loc~cytosol,loc_index_1~1),A(state!1,loc~cytosol,loc_index_1~1) @ 0.05\n" + 
        "'dimerisation-3' A(state,loc~cytosol,loc_index_1~2),A(state,loc~cytosol,loc_index_1~2) -> A(state!1,loc~cytosol,loc_index_1~2),A(state!1,loc~cytosol,loc_index_1~2) @ 0.05\n" + 
        "'dimerisation-4' A(state,loc~cytosol,loc_index_1~3),A(state,loc~cytosol,loc_index_1~3) -> A(state!1,loc~cytosol,loc_index_1~3),A(state!1,loc~cytosol,loc_index_1~3) @ 0.05\n" + 
        "\n" + 
        "\n" + 
        "\n" + 
        "";

    private static final String VARIABLES_ONLY_INPUT = 
        "%agent: egfr-inside(Y1068~pY)\n" + 
        "%var: 'RP-1' egfr-inside(Y1068~pY?)\n" + 
        "%var: 'C' 300\n" + 
        "%var: 'B' 200\n" + 
        "%var: 'A' 100\n" + 
        "";
    
    private static final String VARIABLES_ONLY_OUTPUT = 
        "%agent: egfr-inside(Y1068~pY)\n" + 
        "\n" + 
        "\n" + 
        "\n" + 
        "\n" + 
        "%var: 'RP-1' egfr-inside(Y1068~pY?)\n" + 
        "%var: 'C' 300.0\n" + 
        "%var: 'B' 200.0\n" + 
        "%var: 'A' 100.0\n" + 
        "\n" + 
        "";

    private static final String AGENT_ORDERING_INPUT = 
        "%agent: DNA(binding,downstream,upstream)\n" + 
        "%agent: RNA(downstream)\n" + 
        "%agent: RNAP(rna,dna)\n" + 
        "DNA(binding!1,downstream!3), RNAP(dna!1,rna!2), RNA(downstream!2), DNA(upstream!3,binding!_) -> DNA(binding,downstream!1), RNAP(dna,rna), RNA(downstream), DNA(upstream!1,binding!_) @ 1.0\n";
    
    private static final String AGENT_ORDERING_OUTPUT = 
        "%agent: DNA(binding,downstream,upstream)\n" + 
        "%agent: RNA(downstream)\n" + 
        "%agent: RNAP(rna,dna)\n" + 
        "\n" + 
        "\n" + 
        "DNA(binding!1,downstream!3),RNAP(dna!1,rna!2),RNA(downstream!2),DNA(binding!_,upstream!3) ->" +
        " DNA(binding,downstream!1),RNAP(dna,rna),RNA(downstream),DNA(binding!_,upstream!1) @ 1.0\n" +
        "\n" + 
        "\n" + 
        "\n" + 
        "";

    private static final String OBSERVATIONS_ONLY_INPUT = 
        "%agent: A(state~blue~red)\n" +
        "%compartment: cytosol [4]\n" + 
        "%obs: A(state~blue)\n" + 
        "%obs: 'all red' A(state~red)\n" + 
        "%obs: 'cytosol blue' cytosol A(state~blue)\n" + 
        "%obs: 'red[0]' cytosol[0] A(state~red) \n" + 
        "%obs: 'red[1]' cytosol[1] A(state~red) \n" + 
        "%obs: 'red[2]' cytosol[2] A(state~red) \n" + 
        "%obs: 'red[3]' cytosol[3] A(state~red) \n";
    
    private static final String OBSERVATIONS_ONLY_OUTPUT = 
        "%agent: A(state~blue~red,loc~cytosol,loc_index_1~0~1~2~3)\n" + 
        "\n" + 
        "\n" + 
        "\n" + 
        "\n" + 
        "%var: '[A(state~blue)]' A(state~blue)\n" + 
        "%var: 'all red' A(state~red)\n" + 
        "%var: 'cytosol blue' A(state~blue,loc~cytosol)\n" + 
        "%var: 'red[0]' A(state~red,loc~cytosol,loc_index_1~0)\n" + 
        "%var: 'red[1]' A(state~red,loc~cytosol,loc_index_1~1)\n" + 
        "%var: 'red[2]' A(state~red,loc~cytosol,loc_index_1~2)\n" + 
        "%var: 'red[3]' A(state~red,loc~cytosol,loc_index_1~3)\n" + 
        "%plot: '[A(state~blue)]'\n" + 
        "%plot: 'all red'\n" + 
        "%plot: 'cytosol blue'\n" + 
        "%plot: 'red[0]'\n" + 
        "%plot: 'red[1]'\n" + 
        "%plot: 'red[2]'\n" + 
        "%plot: 'red[3]'\n" + 
        "\n" + 
        "";
    
    // TODO handle plot of transport and transforms
    
    private static final String INIT_ONLY_INPUT = 
        "%agent: A(state~blue~red~green~purple~orange~yellow~brown~white~black~cyan)\n" +
        "%agent: B(s~right~centre~left~outside~bottom~middle~top~inside~back~front)\n" +
        "%compartment: cytosol [4]\n" + 
		"%init: 800 A(state~green),B(s~right) \n" + 
		"%init: 800 A(state~purple),B:cytosol(s~centre) \n" + 
		"%init: 800 A(state~orange),B:cytosol[3](s~left) \n" + 
        "%init: 800 cytosol A(state~blue),B(s~outside) \n" + 
        "%init: 800 cytosol A(state~yellow),B:cytosol(s~bottom) \n" +
        "%init: 800 cytosol A(state~brown),B:cytosol[3](s~middle) \n" +
        "%init: 800 cytosol[0] A(state~white),B(s~top) \n" +
        "%init: 800 cytosol[0] A(state~black),B:cytosol(s~inside) \n" +
        "%init: 800 cytosol[0] A(state~red),B:cytosol[0](s~back) \n" +
        "%init: 800 cytosol[0] A(state~cyan),B:cytosol[3](s~front) \n" +
        "";
    
    private static final String INIT_ONLY_OUTPUT = 
        "%agent: A(state~blue~red~green~purple~orange~yellow~brown~white~black~cyan,loc~cytosol,loc_index_1~0~1~2~3)\n" + 
        "%agent: B(s~right~centre~left~outside~bottom~middle~top~inside~back~front,loc~cytosol,loc_index_1~0~1~2~3)\n" + 
        "\n" + 
        "\n" + 
        "\n" + 
        "%init: 800 A(state~green)\n" + 
        "%init: 800 B(s~right)\n" + 
		"%init: 800 A(state~purple)\n" + 
        "%init: 200 B(s~centre,loc~cytosol,loc_index_1~0)\n" + 
        "%init: 200 B(s~centre,loc~cytosol,loc_index_1~1)\n" + 
        "%init: 200 B(s~centre,loc~cytosol,loc_index_1~2)\n" + 
        "%init: 200 B(s~centre,loc~cytosol,loc_index_1~3)\n" + 
		"%init: 800 A(state~orange)\n" + 
        "%init: 800 B(s~left,loc~cytosol,loc_index_1~3)\n" +
		"%init: 200 A(state~blue,loc~cytosol,loc_index_1~0)\n" + 
		"%init: 200 A(state~blue,loc~cytosol,loc_index_1~1)\n" + 
		"%init: 200 A(state~blue,loc~cytosol,loc_index_1~2)\n" + 
		"%init: 200 A(state~blue,loc~cytosol,loc_index_1~3)\n" + 
        "%init: 200 B(s~outside,loc~cytosol,loc_index_1~0)\n" + 
        "%init: 200 B(s~outside,loc~cytosol,loc_index_1~1)\n" + 
        "%init: 200 B(s~outside,loc~cytosol,loc_index_1~2)\n" + 
        "%init: 200 B(s~outside,loc~cytosol,loc_index_1~3)\n" +
		"%init: 200 A(state~yellow,loc~cytosol,loc_index_1~0)\n" + 
		"%init: 200 A(state~yellow,loc~cytosol,loc_index_1~1)\n" + 
		"%init: 200 A(state~yellow,loc~cytosol,loc_index_1~2)\n" + 
		"%init: 200 A(state~yellow,loc~cytosol,loc_index_1~3)\n" + 
        "%init: 200 B(s~bottom,loc~cytosol,loc_index_1~0)\n" + 
        "%init: 200 B(s~bottom,loc~cytosol,loc_index_1~1)\n" + 
        "%init: 200 B(s~bottom,loc~cytosol,loc_index_1~2)\n" + 
        "%init: 200 B(s~bottom,loc~cytosol,loc_index_1~3)\n" + 
		"%init: 200 A(state~brown,loc~cytosol,loc_index_1~0)\n" + 
		"%init: 200 A(state~brown,loc~cytosol,loc_index_1~1)\n" + 
		"%init: 200 A(state~brown,loc~cytosol,loc_index_1~2)\n" + 
		"%init: 200 A(state~brown,loc~cytosol,loc_index_1~3)\n" + 
        "%init: 800 B(s~middle,loc~cytosol,loc_index_1~3)\n" + 
		"%init: 800 A(state~white,loc~cytosol,loc_index_1~0)\n" + 
        "%init: 800 B(s~top,loc~cytosol,loc_index_1~0)\n" +
        "%init: 800 A(state~black,loc~cytosol,loc_index_1~0)\n" + 
        "%init: 200 B(s~inside,loc~cytosol,loc_index_1~0)\n" + 
        "%init: 200 B(s~inside,loc~cytosol,loc_index_1~1)\n" + 
        "%init: 200 B(s~inside,loc~cytosol,loc_index_1~2)\n" + 
        "%init: 200 B(s~inside,loc~cytosol,loc_index_1~3)\n" + 
		"%init: 800 A(state~red,loc~cytosol,loc_index_1~0)\n" + 
        "%init: 800 B(s~back,loc~cytosol,loc_index_1~0)\n" + 
		"%init: 800 A(state~cyan,loc~cytosol,loc_index_1~0)\n" + 
        "%init: 800 B(s~front,loc~cytosol,loc_index_1~3)\n" + 
        "\n" + 
        "\n" + 
        "";
    // TODO aggregate inits when possible ?
    
    private static final String INIT_WITH_LINKS_INPUT = 
        "%agent: A(state~blue~red~green~purple~orange~yellow~brown~white~black~cyan)\n" +
        "%agent: B(s~right~centre~left~outside~bottom~middle~top~inside~back~front~horiz~vert)\n" +
        "%compartment: cytosol [2][2]\n" + 
        "%compartment: membrane [2]\n" +
        "%channel: horiz cytosol[x][y] -> cytosol[x+1][y]\n" + 
        "%channel: vert cytosol[x][y] -> cytosol[x][y+1]\n" + 
        "%channel: diag (cytosol[x][y] -> cytosol[x+1][y+1]) + (cytosol[x][y] -> cytosol[x+1][y-1])\n" + 
        "%init: 800 cytosol A(state~green!1:horiz),B(s~horiz!1) \n" + 
        "%init: 800 cytosol A(state~purple!1:vert),B(s~vert!1) \n" + 
        "";
        
    private static final String INIT_WITH_LINKS_OUTPUT = 
        "%agent: A(state~blue~red~green~purple~orange~yellow~brown~white~black~cyan,loc~cytosol~membrane,loc_index_1~0~1,loc_index_2~0~1)\n" + 
        "%agent: B(s~right~centre~left~outside~bottom~middle~top~inside~back~front~horiz~vert,loc~cytosol~membrane,loc_index_1~0~1,loc_index_2~0~1)\n" + 
        "\n" + 
        "\n" + 
        "\n" + 
        "%init: 400 A(state~green!1,loc~cytosol,loc_index_1~0,loc_index_2~0),B(s~horiz!1,loc~cytosol,loc_index_1~1,loc_index_2~0)\n" + 
        "%init: 400 A(state~green!1,loc~cytosol,loc_index_1~0,loc_index_2~1),B(s~horiz!1,loc~cytosol,loc_index_1~1,loc_index_2~1)\n" + 
        "%init: 400 A(state~purple!1,loc~cytosol,loc_index_1~0,loc_index_2~0),B(s~vert!1,loc~cytosol,loc_index_1~0,loc_index_2~1)\n" + 
        "%init: 400 A(state~purple!1,loc~cytosol,loc_index_1~1,loc_index_2~0),B(s~vert!1,loc~cytosol,loc_index_1~1,loc_index_2~1)\n" + 
        "\n" + 
        "\n" + 
        "";
    
    private static final String OBSERVATIONS_WITH_LINKS_INPUT = 
        "%agent: A(state~blue~red~green~purple~orange~yellow~brown~white~black~cyan)\n" +
        "%agent: B(s~right~centre~left~outside~bottom~middle~top~inside~back~front~horiz~vert)\n" +
        "%compartment: cytosol [2][2]\n" + 
        "%compartment: membrane [2]\n" +
        "%channel: horiz cytosol[x][y] <-> cytosol[x+1][y]\n" + 
        "%channel: vert cytosol[x][y] <-> cytosol[x][y+1]\n" + 
        "%channel: diag cytosol[x][y] <-> cytosol[x+1][y+1]\n" + 
        "%channel: diag cytosol[x][y] <-> cytosol[x+1][y-1]\n" + 
        "%obs: 'obs1' cytosol A(state~green!1:horiz),B(s~horiz!1) \n" + 
        "%obs: 'obs2' cytosol A(state~purple!1:vert),B(s~vert!1) \n" + 
        "";
        
    private static final String OBSERVATIONS_WITH_LINKS_OUTPUT = 
        "%agent: A(state~blue~red~green~purple~orange~yellow~brown~white~black~cyan,loc~cytosol~membrane,loc_index_1~0~1,loc_index_2~0~1)\n" + 
        "%agent: B(s~right~centre~left~outside~bottom~middle~top~inside~back~front~horiz~vert,loc~cytosol~membrane,loc_index_1~0~1,loc_index_2~0~1)\n" + 
        "\n" + 
        "\n" + 
        "\n" + 
        "\n" + 
        "%var: 'obs1-1' A(state~green!1,loc~cytosol,loc_index_1~0,loc_index_2~0),B(s~horiz!1,loc~cytosol,loc_index_1~1,loc_index_2~0)\n" + 
        "%var: 'obs1-2' A(state~green!1,loc~cytosol,loc_index_1~0,loc_index_2~1),B(s~horiz!1,loc~cytosol,loc_index_1~1,loc_index_2~1)\n" + 
        "%var: 'obs1' 'obs1-1' + 'obs1-2'\n" + 
        "%var: 'obs2-1' A(state~purple!1,loc~cytosol,loc_index_1~0,loc_index_2~0),B(s~vert!1,loc~cytosol,loc_index_1~0,loc_index_2~1)\n" + 
        "%var: 'obs2-2' A(state~purple!1,loc~cytosol,loc_index_1~1,loc_index_2~0),B(s~vert!1,loc~cytosol,loc_index_1~1,loc_index_2~1)\n" + 
        "%var: 'obs2' 'obs2-1' + 'obs2-2'\n" + 
        "%plot: 'obs1'\n" + 
        "%plot: 'obs2'\n" +
        "\n" + 
        "";
            
    private static final String VARIABLES_WITH_LINKS_INPUT = 
        "%agent: A(state~blue~red~green~purple~orange~yellow~brown~white~black~cyan)\n" +
        "%agent: B(s~right~centre~left~outside~bottom~middle~top~inside~back~front~horiz~vert)\n" +
        "%compartment: cytosol [2][2]\n" + 
        "%compartment: membrane [2]\n" +
        "%channel: horiz cytosol[x][y] <-> cytosol[x+1][y]\n" + 
        "%channel: vert cytosol[x][y] <-> cytosol[x][y+1]\n" + 
        "%channel: diag cytosol[x][y] <-> cytosol[x+1][y+1]\n" + 
        "%channel: diag cytosol[x][y] <-> cytosol[x+1][y-1]\n" + 
        "%var: 'obs1' A:cytosol(state~green!1:horiz),B:cytosol(s~horiz!1) \n" + 
        "%var: 'obs2' A:cytosol(state~purple!1:vert),B:cytosol(s~vert!1) \n" + 
        "";
        
    private static final String VARIABLES_WITH_LINKS_OUTPUT = 
        "%agent: A(state~blue~red~green~purple~orange~yellow~brown~white~black~cyan,loc~cytosol~membrane,loc_index_1~0~1,loc_index_2~0~1)\n" + 
        "%agent: B(s~right~centre~left~outside~bottom~middle~top~inside~back~front~horiz~vert,loc~cytosol~membrane,loc_index_1~0~1,loc_index_2~0~1)\n" + 
        "\n" + 
        "\n" + 
        "\n" +
        "\n" + 
        "%var: 'obs1-1' A(state~green!1,loc~cytosol,loc_index_1~0,loc_index_2~0),B(s~horiz!1,loc~cytosol,loc_index_1~1,loc_index_2~0)\n" + 
        "%var: 'obs1-2' A(state~green!1,loc~cytosol,loc_index_1~0,loc_index_2~1),B(s~horiz!1,loc~cytosol,loc_index_1~1,loc_index_2~1)\n" + 
        "%var: 'obs1' 'obs1-1' + 'obs1-2'\n" + 
        "%var: 'obs2-1' A(state~purple!1,loc~cytosol,loc_index_1~0,loc_index_2~0),B(s~vert!1,loc~cytosol,loc_index_1~0,loc_index_2~1)\n" + 
        "%var: 'obs2-2' A(state~purple!1,loc~cytosol,loc_index_1~1,loc_index_2~0),B(s~vert!1,loc~cytosol,loc_index_1~1,loc_index_2~1)\n" + 
        "%var: 'obs2' 'obs2-1' + 'obs2-2'\n" + 
        "\n" + 
        "";
                    
    private static final String TEST_INPUT_FILENAME = "test/data/spatial-1D-array.ka";
    private static final String TEST_OUTPUT_FILENAME = "test/data/spatial-1D-array-target.ka";
    
}
