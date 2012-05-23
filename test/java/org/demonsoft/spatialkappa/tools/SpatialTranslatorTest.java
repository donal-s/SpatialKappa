package org.demonsoft.spatialkappa.tools;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
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
import org.demonsoft.spatialkappa.model.CellIndexExpression;
import org.demonsoft.spatialkappa.model.Compartment;
import org.demonsoft.spatialkappa.model.CompartmentLink;
import org.demonsoft.spatialkappa.model.Direction;
import org.demonsoft.spatialkappa.model.IKappaModel;
import org.demonsoft.spatialkappa.model.KappaModel;
import org.demonsoft.spatialkappa.model.Location;
import org.demonsoft.spatialkappa.model.VariableExpression.Operator;
import org.demonsoft.spatialkappa.model.VariableReference;
import org.demonsoft.spatialkappa.parser.SpatialKappaLexer;
import org.demonsoft.spatialkappa.parser.SpatialKappaParser;
import org.demonsoft.spatialkappa.parser.SpatialKappaWalker;
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
    public void testInitOnly() throws Exception {
        translator = new SpatialTranslator(INIT_ONLY_INPUT);
        assertEquals(INIT_ONLY_OUTPUT, translator.translateToKappa());
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
        CompartmentLink compartmentLink = new CompartmentLink("label", new Location("a"), new Location("b"), Direction.FORWARD);
        List<Compartment> compartments = new ArrayList<Compartment>();
        
        try {
            translator.getLinkStateSuffixPairs(compartmentLink, null);
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
            translator.getLinkStateSuffixPairs(compartmentLink, compartments);
            fail("missing compartment should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        compartments.clear();
        compartments.add(new Compartment("b"));

        try {
            translator.getLinkStateSuffixPairs(compartmentLink, compartments);
            fail("missing compartment should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        compartments.clear();
        compartments.add(new Compartment("a", 3));
        compartments.add(new Compartment("b"));

        try {
            translator.getLinkStateSuffixPairs(compartmentLink, compartments);
            fail("dimension mismatch should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        // Single cell compartments
        compartments.clear();
        compartmentLink = new CompartmentLink("label", new Location("a"), new Location("b"), Direction.FORWARD);
        compartments.add(new Compartment("a"));
        compartments.add(new Compartment("b"));
        
        assertArrayEquals(new String[][] {{"loc~a", "loc~b"}}, translator.getLinkStateSuffixPairs(compartmentLink, compartments));
        
        // Single cell-cell link
        compartments.clear();
        compartmentLink = new CompartmentLink("label", 
                new Location("a", new CellIndexExpression("1")), new Location("a", new CellIndexExpression("2")), Direction.FORWARD);
        compartments.add(new Compartment("a", 4));
        
        assertArrayEquals(new String[][] {{"loc~a,loc_index_1~1", "loc~a,loc_index_1~2"}}, translator.getLinkStateSuffixPairs(compartmentLink, compartments));

        // Linear array
        compartments.clear();
        compartmentLink = new CompartmentLink("label",
                new Location("a", new CellIndexExpression(refX)), 
                new Location("a", new CellIndexExpression(new CellIndexExpression(refX), Operator.PLUS, new CellIndexExpression("1"))), Direction.FORWARD);
        compartments.add(new Compartment("a", 4));
        
        assertArrayEquals(new String[][] {
                {"loc~a,loc_index_1~0", "loc~a,loc_index_1~1"},
                {"loc~a,loc_index_1~1", "loc~a,loc_index_1~2"},
                {"loc~a,loc_index_1~2", "loc~a,loc_index_1~3"}}, translator.getLinkStateSuffixPairs(compartmentLink, compartments));

        // 2D array - square mesh - dimensions treated independently
        compartments.clear();
        compartmentLink = new CompartmentLink("label", 
                new Location("a", new CellIndexExpression(refX), new CellIndexExpression(refY)), 
                new Location("a", new CellIndexExpression(new CellIndexExpression(refX), Operator.PLUS, new CellIndexExpression("1")), new CellIndexExpression(refY)), 
                Direction.FORWARD);
        compartmentLink = new CompartmentLink("label", 
                new Location("a", new CellIndexExpression(refX), new CellIndexExpression(refY)), 
                new Location("a", new CellIndexExpression(refX), new CellIndexExpression(new CellIndexExpression(refY), Operator.PLUS, new CellIndexExpression("1"))), 
                Direction.FORWARD);
        compartments.add(new Compartment("a", 4, 4));
        
        /* TODO known issue here
        assertArrayEquals(new String[][] {
                {"loc~a,loc_index_1~0", "loc~a,loc_index_1~1"},
                {"loc~a,loc_index_1~1", "loc~a,loc_index_1~2"},
                {"loc~a,loc_index_1~2", "loc~a,loc_index_1~3"},
                {"loc~a,loc_index_2~0", "loc~a,loc_index_2~1"},
                {"loc~a,loc_index_2~1", "loc~a,loc_index_2~2"},
                {"loc~a,loc_index_2~2", "loc~a,loc_index_2~3"}
                }, translator.getLinkStateSuffixPairs(compartmentLink, compartments));
        
        // 2D array - diagonal linked mesh - dimensions treated together
        compartments.clear();
        compartmentLink = new CompartmentLink("label", 
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
                }, translator.getLinkStateSuffixPairs(compartmentLink, compartments));
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
    	assertEquals(0, translator.getPartitionedLocations(null).size());
    	
    	IKappaModel model = new KappaModel();
    	Compartment cytosol = new Compartment("cytosol", 2, 2);
    	Compartment membrane = new Compartment("membrane", 3);
    	model.addCompartment(cytosol);
    	model.addCompartment(membrane);
    	translator = new SpatialTranslator(model);
    	
    	List<Agent> agents = new ArrayList<Agent>();
    	Set<Location> expected = new HashSet<Location>();

    	agents.add(new Agent("A", new Location("cytosol")));
    	expected.add(new Location("cytosol"));
    	assertEquals(expected, translator.getPartitionedLocations(agents));

    	agents.clear();
    	expected.clear();
    	
    	agents.add(new Agent("A", new Location("cytosol", new CellIndexExpression("1"))));
    	expected.add(new Location("cytosol", new CellIndexExpression("1")));
    	assertEquals(expected, translator.getPartitionedLocations(agents));

    	agents.clear();
    	expected.clear();
    	
    	agents.add(new Agent("A", new Location("cytosol", new CellIndexExpression("1"), new CellIndexExpression("1"))));
    	assertEquals(expected, translator.getPartitionedLocations(agents));

    	agents.clear();
    	expected.clear();
    	
    	agents.add(new Agent("A", new Location("cytosol")));
    	agents.add(new Agent("B", new Location("cytosol")));
    	expected.add(new Location("cytosol"));
    	assertEquals(expected, translator.getPartitionedLocations(agents));

    	agents.clear();
    	expected.clear();
    	
    	agents.add(new Agent("A", new Location("cytosol")));
    	agents.add(new Agent("B", new Location("membrane")));
    	expected.add(new Location("cytosol"));
    	expected.add(new Location("membrane"));
    	assertEquals(expected, translator.getPartitionedLocations(agents));
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
    
    private static final String SIMPLE_TEST_INPUT = 
        "%agent: A(state~blue~red~green)\n" +
        "%compartment: cytosol [4]\n" + 
        "%link: 'intra-cytosol' cytosol ['x'] <-> cytosol ['x'+1]\n" + 
        "\n" + 
        "%transport: 'diffusion-all' 'intra-cytosol' @ 0.1\n" + 
        "%transport: 'diffusion-red' 'intra-cytosol' A(state~red) @ 0.1\n" + 
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
        "'diffusion-all-2' A(loc~cytosol,loc_index_1~1) -> A(loc~cytosol,loc_index_1~0) @ 0.1\n" + 
        "'diffusion-all-3' A(loc~cytosol,loc_index_1~1) -> A(loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-all-4' A(loc~cytosol,loc_index_1~2) -> A(loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-all-5' A(loc~cytosol,loc_index_1~2) -> A(loc~cytosol,loc_index_1~3) @ 0.1\n" + 
        "'diffusion-all-6' A(loc~cytosol,loc_index_1~3) -> A(loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-red-1' A(state~red,loc~cytosol,loc_index_1~0) -> A(state~red,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-red-2' A(state~red,loc~cytosol,loc_index_1~1) -> A(state~red,loc~cytosol,loc_index_1~0) @ 0.1\n" + 
        "'diffusion-red-3' A(state~red,loc~cytosol,loc_index_1~1) -> A(state~red,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-red-4' A(state~red,loc~cytosol,loc_index_1~2) -> A(state~red,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-red-5' A(state~red,loc~cytosol,loc_index_1~2) -> A(state~red,loc~cytosol,loc_index_1~3) @ 0.1\n" + 
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
        "%var: 'cytosol blue :loc~cytosol,loc_index_1~0' A(state~blue,loc~cytosol,loc_index_1~0)\n" + 
        "%plot: 'cytosol blue :loc~cytosol,loc_index_1~0'\n" + 
        "%var: 'cytosol blue :loc~cytosol,loc_index_1~1' A(state~blue,loc~cytosol,loc_index_1~1)\n" + 
        "%plot: 'cytosol blue :loc~cytosol,loc_index_1~1'\n" + 
        "%var: 'cytosol blue :loc~cytosol,loc_index_1~2' A(state~blue,loc~cytosol,loc_index_1~2)\n" + 
        "%plot: 'cytosol blue :loc~cytosol,loc_index_1~2'\n" + 
        "%var: 'cytosol blue :loc~cytosol,loc_index_1~3' A(state~blue,loc~cytosol,loc_index_1~3)\n" + 
        "%plot: 'cytosol blue :loc~cytosol,loc_index_1~3'\n" + 
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
        "%link: 'intra-cytosol' cytosol ['x'] <-> cytosol ['x'+1]\n" + 
        "\n" + 
        "%transport: 'diffusion-all' 'intra-cytosol' @ [inf]\n" + 
        "%transport: 'diffusion-red' 'intra-cytosol' A(state~red) @ [inf]\n" + 
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
        "'diffusion-all-2' A(loc~cytosol,loc_index_1~1) -> A(loc~cytosol,loc_index_1~0) @ [inf]\n" + 
        "'diffusion-all-3' A(loc~cytosol,loc_index_1~1) -> A(loc~cytosol,loc_index_1~2) @ [inf]\n" + 
        "'diffusion-all-4' A(loc~cytosol,loc_index_1~2) -> A(loc~cytosol,loc_index_1~1) @ [inf]\n" + 
        "'diffusion-all-5' A(loc~cytosol,loc_index_1~2) -> A(loc~cytosol,loc_index_1~3) @ [inf]\n" + 
        "'diffusion-all-6' A(loc~cytosol,loc_index_1~3) -> A(loc~cytosol,loc_index_1~2) @ [inf]\n" + 
        "'diffusion-red-1' A(state~red,loc~cytosol,loc_index_1~0) -> A(state~red,loc~cytosol,loc_index_1~1) @ [inf]\n" + 
        "'diffusion-red-2' A(state~red,loc~cytosol,loc_index_1~1) -> A(state~red,loc~cytosol,loc_index_1~0) @ [inf]\n" + 
        "'diffusion-red-3' A(state~red,loc~cytosol,loc_index_1~1) -> A(state~red,loc~cytosol,loc_index_1~2) @ [inf]\n" + 
        "'diffusion-red-4' A(state~red,loc~cytosol,loc_index_1~2) -> A(state~red,loc~cytosol,loc_index_1~1) @ [inf]\n" + 
        "'diffusion-red-5' A(state~red,loc~cytosol,loc_index_1~2) -> A(state~red,loc~cytosol,loc_index_1~3) @ [inf]\n" + 
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
        "%var: 'cytosol blue :loc~cytosol,loc_index_1~0' A(state~blue,loc~cytosol,loc_index_1~0)\n" + 
        "%plot: 'cytosol blue :loc~cytosol,loc_index_1~0'\n" + 
        "%var: 'cytosol blue :loc~cytosol,loc_index_1~1' A(state~blue,loc~cytosol,loc_index_1~1)\n" + 
        "%plot: 'cytosol blue :loc~cytosol,loc_index_1~1'\n" + 
        "%var: 'cytosol blue :loc~cytosol,loc_index_1~2' A(state~blue,loc~cytosol,loc_index_1~2)\n" + 
        "%plot: 'cytosol blue :loc~cytosol,loc_index_1~2'\n" + 
        "%var: 'cytosol blue :loc~cytosol,loc_index_1~3' A(state~blue,loc~cytosol,loc_index_1~3)\n" + 
        "%plot: 'cytosol blue :loc~cytosol,loc_index_1~3'\n" + 
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
        "%link: 'intra-cytosol' cytosol ['x'] <-> cytosol ['x'+1]\n" + 
        "%link: 'interface1' cytosol ['x'] -> membrane ['x'+2]\n" +
        "%link: 'interface2' membrane ['x'] <- cytosol ['x'+2]\n" +
        "\n" + 
        "%transport: 'diffusion-all' 'intra-cytosol' @ 0.1\n" + // TODO - remember only works for single agents
        "%transport: 'diffusion-red' 'intra-cytosol' A(state~red) @ 0.1\n" + 
        "%transport: 'diffusion-other' 'intra-cytosol' B() @ 0.1\n" + 
        "%transport: 'cell exit1' 'interface1' B() @ 0.2\n" + 
        "%transport: 'cell exit2' 'interface2' B() @ 0.3\n" + 
        "%init: 800 A(state~red),B()\n" + 
        "\n" + 
        "";
    
    private static final String TRANSPORT_ONLY_OUTPUT = 
        "%agent: A(state~red,loc~cytosol~membrane,loc_index_1~0~1~2~3)\n" + 
        "%agent: B(loc~cytosol~membrane,loc_index_1~0~1~2~3)\n" +
        "\n" + 
        "'diffusion-all-1' A(loc~cytosol,loc_index_1~0) -> A(loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-all-2' A(loc~cytosol,loc_index_1~1) -> A(loc~cytosol,loc_index_1~0) @ 0.1\n" + 
        "'diffusion-all-3' A(loc~cytosol,loc_index_1~1) -> A(loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-all-4' A(loc~cytosol,loc_index_1~2) -> A(loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-all-5' A(loc~cytosol,loc_index_1~2) -> A(loc~cytosol,loc_index_1~3) @ 0.1\n" + 
        "'diffusion-all-6' A(loc~cytosol,loc_index_1~3) -> A(loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-all-7' B(loc~cytosol,loc_index_1~0) -> B(loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-all-8' B(loc~cytosol,loc_index_1~1) -> B(loc~cytosol,loc_index_1~0) @ 0.1\n" + 
        "'diffusion-all-9' B(loc~cytosol,loc_index_1~1) -> B(loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-all-10' B(loc~cytosol,loc_index_1~2) -> B(loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-all-11' B(loc~cytosol,loc_index_1~2) -> B(loc~cytosol,loc_index_1~3) @ 0.1\n" + 
        "'diffusion-all-12' B(loc~cytosol,loc_index_1~3) -> B(loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-red-1' A(state~red,loc~cytosol,loc_index_1~0) -> A(state~red,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-red-2' A(state~red,loc~cytosol,loc_index_1~1) -> A(state~red,loc~cytosol,loc_index_1~0) @ 0.1\n" + 
        "'diffusion-red-3' A(state~red,loc~cytosol,loc_index_1~1) -> A(state~red,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-red-4' A(state~red,loc~cytosol,loc_index_1~2) -> A(state~red,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-red-5' A(state~red,loc~cytosol,loc_index_1~2) -> A(state~red,loc~cytosol,loc_index_1~3) @ 0.1\n" + 
        "'diffusion-red-6' A(state~red,loc~cytosol,loc_index_1~3) -> A(state~red,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-other-1' B(loc~cytosol,loc_index_1~0) -> B(loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-other-2' B(loc~cytosol,loc_index_1~1) -> B(loc~cytosol,loc_index_1~0) @ 0.1\n" + 
        "'diffusion-other-3' B(loc~cytosol,loc_index_1~1) -> B(loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-other-4' B(loc~cytosol,loc_index_1~2) -> B(loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-other-5' B(loc~cytosol,loc_index_1~2) -> B(loc~cytosol,loc_index_1~3) @ 0.1\n" + 
        "'diffusion-other-6' B(loc~cytosol,loc_index_1~3) -> B(loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'cell exit1-1' B(loc~cytosol,loc_index_1~0) -> B(loc~membrane,loc_index_1~2) @ 0.2\n" + 
        "'cell exit1-2' B(loc~cytosol,loc_index_1~1) -> B(loc~membrane,loc_index_1~3) @ 0.2\n" + 
        "'cell exit2-1' B(loc~cytosol,loc_index_1~2) -> B(loc~membrane,loc_index_1~0) @ 0.3\n" + 
        "'cell exit2-2' B(loc~cytosol,loc_index_1~3) -> B(loc~membrane,loc_index_1~1) @ 0.3\n" + 
        "\n" + 
        "\n" + 
        "%init: 800 A(state~red),B\n" + 
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
        "%link: 'interface1' nucleus <-> membrane [2]\n" +
        "%link: 'interface2' nucleus <-> cytosol [1][2]\n" +
        "%link: 'interface3' membrane ['x'] <-> cytosol ['x']['x'+2]\n" +
        "\n" + 
        "%transport: 'diffusion-blue' 'interface1'  A(state~blue) @ 0.1\n" + 
        "%transport: 'diffusion-red' 'interface2' A(state~red) @ 0.2\n" + 
        "%transport: 'diffusion-other' 'interface3' B() @ 0.3\n" + 
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
        "'diffusion-other-2' B(loc~cytosol,loc_index_1~0,loc_index_2~2) -> B(loc~membrane,loc_index_1~0,loc_index_2~0) @ 0.3\n" + 
        "'diffusion-other-3' B(loc~membrane,loc_index_1~1,loc_index_2~0) -> B(loc~cytosol,loc_index_1~1,loc_index_2~3) @ 0.3\n" + 
        "'diffusion-other-4' B(loc~cytosol,loc_index_1~1,loc_index_2~3) -> B(loc~membrane,loc_index_1~1,loc_index_2~0) @ 0.3\n" + 
        "'diffusion-other-5' B(loc~membrane,loc_index_1~2,loc_index_2~0) -> B(loc~cytosol,loc_index_1~2,loc_index_2~4) @ 0.3\n" + 
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
        "%link: '6way' cytosol ['x']['y'] <-> cytosol ['x'+1]['y'] \n" + 
        "%link: '6way' cytosol ['x']['y'] <-> cytosol ['x']['y'+1] \n" + 
        "%link: '6way' cytosol ['x']['y'] <-> cytosol ['x'+1][('y'+1)-(2*('x'%2))] \n" + 
        "\n" + 
        "%transport: 'diffusion RED' '6way' RED() @ 0.05 \n" + 
        "%init: 40 RED()\n" + 
        "";
    
    private static final String TRANSPORT_6WAY_OUTPUT = 
		"%agent: RED(loc~cytosol,loc_index_1~0~1~2,loc_index_2~0~1~2)\n" + 
        "\n" + 
        "'diffusion RED-1' RED(loc~cytosol,loc_index_1~0,loc_index_2~0) -> RED(loc~cytosol,loc_index_1~1,loc_index_2~0) @ 0.05\n" + 
        "'diffusion RED-2' RED(loc~cytosol,loc_index_1~1,loc_index_2~0) -> RED(loc~cytosol,loc_index_1~0,loc_index_2~0) @ 0.05\n" + 
        "'diffusion RED-3' RED(loc~cytosol,loc_index_1~0,loc_index_2~1) -> RED(loc~cytosol,loc_index_1~1,loc_index_2~1) @ 0.05\n" + 
        "'diffusion RED-4' RED(loc~cytosol,loc_index_1~1,loc_index_2~1) -> RED(loc~cytosol,loc_index_1~0,loc_index_2~1) @ 0.05\n" + 
        "'diffusion RED-5' RED(loc~cytosol,loc_index_1~0,loc_index_2~2) -> RED(loc~cytosol,loc_index_1~1,loc_index_2~2) @ 0.05\n" + 
        "'diffusion RED-6' RED(loc~cytosol,loc_index_1~1,loc_index_2~2) -> RED(loc~cytosol,loc_index_1~0,loc_index_2~2) @ 0.05\n" + 
        "'diffusion RED-7' RED(loc~cytosol,loc_index_1~1,loc_index_2~0) -> RED(loc~cytosol,loc_index_1~2,loc_index_2~0) @ 0.05\n" + 
        "'diffusion RED-8' RED(loc~cytosol,loc_index_1~2,loc_index_2~0) -> RED(loc~cytosol,loc_index_1~1,loc_index_2~0) @ 0.05\n" + 
        "'diffusion RED-9' RED(loc~cytosol,loc_index_1~1,loc_index_2~1) -> RED(loc~cytosol,loc_index_1~2,loc_index_2~1) @ 0.05\n" + 
        "'diffusion RED-10' RED(loc~cytosol,loc_index_1~2,loc_index_2~1) -> RED(loc~cytosol,loc_index_1~1,loc_index_2~1) @ 0.05\n" + 
        "'diffusion RED-11' RED(loc~cytosol,loc_index_1~1,loc_index_2~2) -> RED(loc~cytosol,loc_index_1~2,loc_index_2~2) @ 0.05\n" + 
        "'diffusion RED-12' RED(loc~cytosol,loc_index_1~2,loc_index_2~2) -> RED(loc~cytosol,loc_index_1~1,loc_index_2~2) @ 0.05\n" + 
        "'diffusion RED-13' RED(loc~cytosol,loc_index_1~0,loc_index_2~0) -> RED(loc~cytosol,loc_index_1~0,loc_index_2~1) @ 0.05\n" + 
        "'diffusion RED-14' RED(loc~cytosol,loc_index_1~0,loc_index_2~1) -> RED(loc~cytosol,loc_index_1~0,loc_index_2~0) @ 0.05\n" + 
        "'diffusion RED-15' RED(loc~cytosol,loc_index_1~0,loc_index_2~1) -> RED(loc~cytosol,loc_index_1~0,loc_index_2~2) @ 0.05\n" + 
        "'diffusion RED-16' RED(loc~cytosol,loc_index_1~0,loc_index_2~2) -> RED(loc~cytosol,loc_index_1~0,loc_index_2~1) @ 0.05\n" + 
        "'diffusion RED-17' RED(loc~cytosol,loc_index_1~1,loc_index_2~0) -> RED(loc~cytosol,loc_index_1~1,loc_index_2~1) @ 0.05\n" + 
        "'diffusion RED-18' RED(loc~cytosol,loc_index_1~1,loc_index_2~1) -> RED(loc~cytosol,loc_index_1~1,loc_index_2~0) @ 0.05\n" + 
        "'diffusion RED-19' RED(loc~cytosol,loc_index_1~1,loc_index_2~1) -> RED(loc~cytosol,loc_index_1~1,loc_index_2~2) @ 0.05\n" + 
        "'diffusion RED-20' RED(loc~cytosol,loc_index_1~1,loc_index_2~2) -> RED(loc~cytosol,loc_index_1~1,loc_index_2~1) @ 0.05\n" + 
        "'diffusion RED-21' RED(loc~cytosol,loc_index_1~2,loc_index_2~0) -> RED(loc~cytosol,loc_index_1~2,loc_index_2~1) @ 0.05\n" + 
        "'diffusion RED-22' RED(loc~cytosol,loc_index_1~2,loc_index_2~1) -> RED(loc~cytosol,loc_index_1~2,loc_index_2~0) @ 0.05\n" + 
        "'diffusion RED-23' RED(loc~cytosol,loc_index_1~2,loc_index_2~1) -> RED(loc~cytosol,loc_index_1~2,loc_index_2~2) @ 0.05\n" + 
        "'diffusion RED-24' RED(loc~cytosol,loc_index_1~2,loc_index_2~2) -> RED(loc~cytosol,loc_index_1~2,loc_index_2~1) @ 0.05\n" + 
        "'diffusion RED-25' RED(loc~cytosol,loc_index_1~0,loc_index_2~0) -> RED(loc~cytosol,loc_index_1~1,loc_index_2~1) @ 0.05\n" + 
        "'diffusion RED-26' RED(loc~cytosol,loc_index_1~1,loc_index_2~1) -> RED(loc~cytosol,loc_index_1~0,loc_index_2~0) @ 0.05\n" + 
        "'diffusion RED-27' RED(loc~cytosol,loc_index_1~0,loc_index_2~1) -> RED(loc~cytosol,loc_index_1~1,loc_index_2~2) @ 0.05\n" + 
        "'diffusion RED-28' RED(loc~cytosol,loc_index_1~1,loc_index_2~2) -> RED(loc~cytosol,loc_index_1~0,loc_index_2~1) @ 0.05\n" + 
        "'diffusion RED-29' RED(loc~cytosol,loc_index_1~1,loc_index_2~1) -> RED(loc~cytosol,loc_index_1~2,loc_index_2~0) @ 0.05\n" + 
        "'diffusion RED-30' RED(loc~cytosol,loc_index_1~2,loc_index_2~0) -> RED(loc~cytosol,loc_index_1~1,loc_index_2~1) @ 0.05\n" + 
        "'diffusion RED-31' RED(loc~cytosol,loc_index_1~1,loc_index_2~2) -> RED(loc~cytosol,loc_index_1~2,loc_index_2~1) @ 0.05\n" + 
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
        "%link: 'intra-cytosol' cytosol ['x'] <-> cytosol ['x'+1]\n" + 
        "\n" + 
        "%transport: 'diffusion-all' 'intra-cytosol' @ 0.1\n" +
        "%transport: 'diffusion-complex' 'intra-cytosol' A(l1!1),B(l1!1) @ 0.1\n" + 
        "%transport: 'diffusion-agent' 'intra-cytosol' C() @ 0.1\n" + 
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
        "'diffusion-all-2' A(l1,l2,loc~cytosol,loc_index_1~1) -> A(l1,l2,loc~cytosol,loc_index_1~0) @ 0.1\n" + 
        "'diffusion-all-3' A(l1,l2,loc~cytosol,loc_index_1~1) -> A(l1,l2,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-all-4' A(l1,l2,loc~cytosol,loc_index_1~2) -> A(l1,l2,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-all-5' A(l1,l2,loc~cytosol,loc_index_1~2) -> A(l1,l2,loc~cytosol,loc_index_1~3) @ 0.1\n" + 
        "'diffusion-all-6' A(l1,l2,loc~cytosol,loc_index_1~3) -> A(l1,l2,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-all-7' B(l1,loc~cytosol,loc_index_1~0) -> B(l1,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-all-8' B(l1,loc~cytosol,loc_index_1~1) -> B(l1,loc~cytosol,loc_index_1~0) @ 0.1\n" + 
        "'diffusion-all-9' B(l1,loc~cytosol,loc_index_1~1) -> B(l1,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-all-10' B(l1,loc~cytosol,loc_index_1~2) -> B(l1,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-all-11' B(l1,loc~cytosol,loc_index_1~2) -> B(l1,loc~cytosol,loc_index_1~3) @ 0.1\n" + 
        "'diffusion-all-12' B(l1,loc~cytosol,loc_index_1~3) -> B(l1,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-all-13' C(l1,l2,loc~cytosol,loc_index_1~0) -> C(l1,l2,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-all-14' C(l1,l2,loc~cytosol,loc_index_1~1) -> C(l1,l2,loc~cytosol,loc_index_1~0) @ 0.1\n" + 
        "'diffusion-all-15' C(l1,l2,loc~cytosol,loc_index_1~1) -> C(l1,l2,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-all-16' C(l1,l2,loc~cytosol,loc_index_1~2) -> C(l1,l2,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-all-17' C(l1,l2,loc~cytosol,loc_index_1~2) -> C(l1,l2,loc~cytosol,loc_index_1~3) @ 0.1\n" + 
        "'diffusion-all-18' C(l1,l2,loc~cytosol,loc_index_1~3) -> C(l1,l2,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-all-19' D(l1,loc~cytosol,loc_index_1~0) -> D(l1,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-all-20' D(l1,loc~cytosol,loc_index_1~1) -> D(l1,loc~cytosol,loc_index_1~0) @ 0.1\n" + 
        "'diffusion-all-21' D(l1,loc~cytosol,loc_index_1~1) -> D(l1,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-all-22' D(l1,loc~cytosol,loc_index_1~2) -> D(l1,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-all-23' D(l1,loc~cytosol,loc_index_1~2) -> D(l1,loc~cytosol,loc_index_1~3) @ 0.1\n" + 
        "'diffusion-all-24' D(l1,loc~cytosol,loc_index_1~3) -> D(l1,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-complex-1' A(l1!1,l2,loc~cytosol,loc_index_1~0),B(l1!1,loc~cytosol,loc_index_1~0) -> A(l1!1,l2,loc~cytosol,loc_index_1~1),B(l1!1,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-complex-2' A(l1!1,l2,loc~cytosol,loc_index_1~1),B(l1!1,loc~cytosol,loc_index_1~1) -> A(l1!1,l2,loc~cytosol,loc_index_1~0),B(l1!1,loc~cytosol,loc_index_1~0) @ 0.1\n" + 
        "'diffusion-complex-3' A(l1!1,l2,loc~cytosol,loc_index_1~1),B(l1!1,loc~cytosol,loc_index_1~1) -> A(l1!1,l2,loc~cytosol,loc_index_1~2),B(l1!1,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-complex-4' A(l1!1,l2,loc~cytosol,loc_index_1~2),B(l1!1,loc~cytosol,loc_index_1~2) -> A(l1!1,l2,loc~cytosol,loc_index_1~1),B(l1!1,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-complex-5' A(l1!1,l2,loc~cytosol,loc_index_1~2),B(l1!1,loc~cytosol,loc_index_1~2) -> A(l1!1,l2,loc~cytosol,loc_index_1~3),B(l1!1,loc~cytosol,loc_index_1~3) @ 0.1\n" + 
        "'diffusion-complex-6' A(l1!1,l2,loc~cytosol,loc_index_1~3),B(l1!1,loc~cytosol,loc_index_1~3) -> A(l1!1,l2,loc~cytosol,loc_index_1~2),B(l1!1,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-agent-1' C(l1,l2,loc~cytosol,loc_index_1~0) -> C(l1,l2,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-agent-2' C(l1,l2,loc~cytosol,loc_index_1~1) -> C(l1,l2,loc~cytosol,loc_index_1~0) @ 0.1\n" + 
        "'diffusion-agent-3' C(l1,l2,loc~cytosol,loc_index_1~1) -> C(l1,l2,loc~cytosol,loc_index_1~2) @ 0.1\n" + 
        "'diffusion-agent-4' C(l1,l2,loc~cytosol,loc_index_1~2) -> C(l1,l2,loc~cytosol,loc_index_1~1) @ 0.1\n" + 
        "'diffusion-agent-5' C(l1,l2,loc~cytosol,loc_index_1~2) -> C(l1,l2,loc~cytosol,loc_index_1~3) @ 0.1\n" + 
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
        "'dimerisation' cytosol A(),A() -> B() @ 0.05\n" + 
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
        "'dimerisation-1' A(loc~cytosol,loc_index_1~0),A(loc~cytosol,loc_index_1~0) -> B(loc~cytosol,loc_index_1~0) @ 0.05\n" + 
        "'dimerisation-2' A(loc~cytosol,loc_index_1~1),A(loc~cytosol,loc_index_1~1) -> B(loc~cytosol,loc_index_1~1) @ 0.05\n" + 
        "'dimerisation-3' A(loc~cytosol,loc_index_1~2),A(loc~cytosol,loc_index_1~2) -> B(loc~cytosol,loc_index_1~2) @ 0.05\n" + 
        "'dimerisation-4' A(loc~cytosol,loc_index_1~3),A(loc~cytosol,loc_index_1~3) -> B(loc~cytosol,loc_index_1~3) @ 0.05\n" + 
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
        "%var: 'cytosol blue :loc~cytosol,loc_index_1~0' A(state~blue,loc~cytosol,loc_index_1~0)\n" + 
        "%plot: 'cytosol blue :loc~cytosol,loc_index_1~0'\n" + 
        "%var: 'cytosol blue :loc~cytosol,loc_index_1~1' A(state~blue,loc~cytosol,loc_index_1~1)\n" + 
        "%plot: 'cytosol blue :loc~cytosol,loc_index_1~1'\n" + 
        "%var: 'cytosol blue :loc~cytosol,loc_index_1~2' A(state~blue,loc~cytosol,loc_index_1~2)\n" + 
        "%plot: 'cytosol blue :loc~cytosol,loc_index_1~2'\n" + 
        "%var: 'cytosol blue :loc~cytosol,loc_index_1~3' A(state~blue,loc~cytosol,loc_index_1~3)\n" + 
        "%plot: 'cytosol blue :loc~cytosol,loc_index_1~3'\n" + 
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
        "%init: 800 A(state~green),B(s~right)\n" + 
		"%init: 200 A(state~purple),B(s~centre,loc~cytosol,loc_index_1~0)\n" + 
		"%init: 200 A(state~purple),B(s~centre,loc~cytosol,loc_index_1~1)\n" + 
		"%init: 200 A(state~purple),B(s~centre,loc~cytosol,loc_index_1~2)\n" + 
		"%init: 200 A(state~purple),B(s~centre,loc~cytosol,loc_index_1~3)\n" + 
		"%init: 800 A(state~orange),B(s~left,loc~cytosol,loc_index_1~3)\n" +
		"%init: 200 A(state~blue,loc~cytosol,loc_index_1~0),B(s~outside,loc~cytosol,loc_index_1~0)\n" + 
		"%init: 200 A(state~blue,loc~cytosol,loc_index_1~1),B(s~outside,loc~cytosol,loc_index_1~1)\n" + 
		"%init: 200 A(state~blue,loc~cytosol,loc_index_1~2),B(s~outside,loc~cytosol,loc_index_1~2)\n" + 
		"%init: 200 A(state~blue,loc~cytosol,loc_index_1~3),B(s~outside,loc~cytosol,loc_index_1~3)\n" +
		"%init: 200 A(state~yellow,loc~cytosol,loc_index_1~0),B(s~bottom,loc~cytosol,loc_index_1~0)\n" + 
		"%init: 200 A(state~yellow,loc~cytosol,loc_index_1~1),B(s~bottom,loc~cytosol,loc_index_1~1)\n" + 
		"%init: 200 A(state~yellow,loc~cytosol,loc_index_1~2),B(s~bottom,loc~cytosol,loc_index_1~2)\n" + 
		"%init: 200 A(state~yellow,loc~cytosol,loc_index_1~3),B(s~bottom,loc~cytosol,loc_index_1~3)\n" + 
		"%init: 200 A(state~brown,loc~cytosol,loc_index_1~0),B(s~middle,loc~cytosol,loc_index_1~3)\n" + 
		"%init: 200 A(state~brown,loc~cytosol,loc_index_1~1),B(s~middle,loc~cytosol,loc_index_1~3)\n" + 
		"%init: 200 A(state~brown,loc~cytosol,loc_index_1~2),B(s~middle,loc~cytosol,loc_index_1~3)\n" + 
		"%init: 200 A(state~brown,loc~cytosol,loc_index_1~3),B(s~middle,loc~cytosol,loc_index_1~3)\n" + 
		"%init: 800 A(state~white,loc~cytosol,loc_index_1~0),B(s~top,loc~cytosol,loc_index_1~0)\n" +
		"%init: 200 A(state~black,loc~cytosol,loc_index_1~0),B(s~inside,loc~cytosol,loc_index_1~0)\n" + 
		"%init: 200 A(state~black,loc~cytosol,loc_index_1~0),B(s~inside,loc~cytosol,loc_index_1~1)\n" + 
		"%init: 200 A(state~black,loc~cytosol,loc_index_1~0),B(s~inside,loc~cytosol,loc_index_1~2)\n" + 
		"%init: 200 A(state~black,loc~cytosol,loc_index_1~0),B(s~inside,loc~cytosol,loc_index_1~3)\n" + 
		"%init: 800 A(state~red,loc~cytosol,loc_index_1~0),B(s~back,loc~cytosol,loc_index_1~0)\n" + 
		"%init: 800 A(state~cyan,loc~cytosol,loc_index_1~0),B(s~front,loc~cytosol,loc_index_1~3)\n" + 
        "\n" + 
        "\n" + 
        "";
    // TODO aggregate inits when possible ?
    
    
    private static final String TEST_INPUT_FILENAME = "test/data/spatial-1D-array.ka";
    private static final String TEST_OUTPUT_FILENAME = "test/data/spatial-1D-array-target.ka";
    
}
