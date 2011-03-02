package org.demonsoft.spatialkappa.scenarios;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.demonsoft.spatialkappa.model.KappaModel;
import org.demonsoft.spatialkappa.model.LocatedObservable;
import org.demonsoft.spatialkappa.model.Observation;
import org.demonsoft.spatialkappa.model.ObservationListener;
import org.demonsoft.spatialkappa.parser.SpatialKappaLexer;
import org.demonsoft.spatialkappa.parser.SpatialKappaParser;
import org.demonsoft.spatialkappa.parser.SpatialKappaWalker;
import org.demonsoft.spatialkappa.tools.AbstractSimulation;
import org.junit.Assert;
import org.junit.Test;


public abstract class AbstractSpatialTest {


    Observation currentObservation;
    
    @Test
    public void testSimpleStateCase() throws Exception {
        checkEventSimulation(SIMPLE_STATE_INPUT, new String[] {"[A(x~s)]", "[A(x~t)]"}, 200, 50, new int[][] {
                {1000, 0}, {800, 200}, {600, 400}, {400, 600}, {200, 800}, {0, 1000}, {0, 1000}
        });
    }
    
    @Test
    public void testSimpleTimedStateCase() throws Exception {
        checkTimedSimulation(SIMPLE_STATE_INPUT, new String[] {"[A(x~s)]", "[A(x~t)]"}, 2.5f, 100, new int[][] {
                {1000, 0}, {800, 200}, {600, 400}, {480, 520}, {380, 620}, {280, 720}, {220, 780}
        });
    }
    
    @Test
    public void testSimpleInfiniteRateCase() throws Exception {
        checkEventSimulation(SIMPLE_INFINITE_RATE_INPUT, new String[] {"[A(x~s)]", "[A(x~t)]"}, 1, 50, new int[][] {
                {1000, 0}, {0, 1000}, {0, 1000}
        });
    }
    
    @Test
    public void testInfiniteRateGradualSubstrateCase() throws Exception {
        checkEventSimulation(INFINITE_RATE_GRADUAL_SUBSTRATE_INPUT, new String[] {"[A(x~s)]", "[A(x~t)]", "[A(x~u)]"}, 1, 1, new int[][] {
                {1000, 0, 0}, {999, 0, 1}, {998, 0, 2}, {997, 0, 3}, {996, 0, 4}
        });
    }
    
    @Test
    public void testErrorCase1() throws Exception {
        checkEventSimulation(ERROR_CASE1_INPUT, new String[] {"[A(a)]", "result"}, 200, 50, new int[][] {
                {1000, 0}, {800, 200}, {600, 400}, {400, 600}, {200, 800}, {0, 1000}, {0, 1000}
        });
    }
    
    @Test
    public void testSimpleLinkCase() throws Exception {
        checkEventSimulation(SIMPLE_LINK_INPUT, new String[] {"[A(x)]", "[A(x!_)]"}, 2000, 50, new int[][] {
                {10000, 0}, {8000, 2000}, {6000, 4000}, {4000, 6000}, {2000, 8000}, {0, 10000}
        });
    }
    
    @Test
    public void testTimedPerturbationCase() throws Exception {
        checkEventSimulation(TIMED_PERTURBATION_INPUT, new String[] {"[A(x~a)]", "[C(z~a)]"}, 2000, 200, new int[][] {
                {10000, 10000}, {8000, 10000}, {6000, 10000}, {4650, 9300}, {4000, 8000}, {3300, 6700}
        });
    }
    
    @Test
    public void testTimedInfiniteRatePerturbationCase() throws Exception {
        checkTimedSimulation(TIMED_INFINITE_RATE_PERTURBATION_INPUT, new String[] {"[A(x~a)]", "[C(z~a)]"}, 1, 500, new int[][] {
                {10000, 10000}, {9000, 10000}, {8250, 10000}, {7350, 10000}, {6700, 0}, {6120, 0}
        });
    }
    
    @Test
    public void testRuleObservationCase() throws Exception {
        checkEventSimulation(SIMPLE_LINK_OBSERVED_RULE_INPUT, new String[] {"link"}, 2000, 50, new int[][] {
                {0}, {2000}, {2000}, {2000}, {2000}, {2000}
        });
    }

    @Test
    public void testCreateAgentLinkCase() throws Exception {
        checkEventSimulation(CREATE_AGENT_LINK_INPUT, new String[] {"[A(x)]", "[A(x!1), B(y!1)]"}, 2000, 50, new int[][] {
                {10000, 0}, {8000, 2000}, {6000, 4000}, {4000, 6000}, {2000, 8000}, {0, 10000}
        });
    }
    
    @Test
    public void testCreateObservableCase() throws Exception {
        checkEventSimulation(CREATE_COMPLEX_INPUT, new String[] {"[A(x)]", "[B(y!1), C(z!1)]"}, 2000, 50, new int[][] {
                {10000, 0}, {10000, 2000}, {10000, 4000}, {10000, 6000}, {10000, 8000}, {10000, 10000}
        });
    }
    
    @Test
    public void testDeleteLinkCase() throws Exception {
        checkEventSimulation(DELETE_LINK_INPUT, new String[] {"[A(x!_)]", "[B(y)]"}, 2000, 50, new int[][] {
                {10000, 0}, {8000, 2000}, {6000, 4000}, {4000, 6000}, {2000, 8000}, {0, 10000}
        });
    }
    
    @Test
    public void testDeleteAgentLinkCase() throws Exception {
        checkEventSimulation(DELETE_AGENT_LINK_INPUT, new String[] {"[B(y!?)]"}, 2000, 50, new int[][] {
                {10000}, {8000}, {6000}, {4000}, {2000}, {0}
        });
    }
    
    @Test
    public void testDeleteObservableCase() throws Exception {
        checkEventSimulation(DELETE_COMPLEX_INPUT, new String[] {"[A(x)]"}, 2000, 50, new int[][] {
                {10000}, {8000}, {6000}, {4000}, {2000}, {0}
        });
    }
    
    @Test
    public void testDeleteObservableNoSitesCase() throws Exception {
        checkEventSimulation(DELETE_COMPLEX_INPUT_NO_SITES, new String[] {"[A()]"}, 2000, 50, new int[][] {
                {10000}, {8000}, {6000}, {4000}, {2000}, {0}
        });
    }
    
    @Test
    public void testSimpleLinkEquilibriumCase() throws Exception {
        checkEventSimulation(SIMPLE_LINK_EQUILIBRIUM_INPUT, new String[] {"[A(x)]", "[A(x!_)]"}, 2000, 50, new int[][] {
                {10000, 0}, {8000, 2000}, {6000, 4000}, {4000, 6000}, {2004, 7996}, 
        });
    }
    
    @Test
    public void testAdditionalStateCase() throws Exception {
        checkEventSimulation(ADDITIONAL_STATE_INPUT, new String[] {"[A(x~s)]", "[A(x~t,y~a)]"}, 200, 50, new int[][] {
                {1000, 0}, {800, 200}, {600, 400}, {400, 600}, {200, 800}, {0, 1000}, {0, 1000}
        });
    }
    
    public static LocatedObservable getSimulationObservable(AbstractSimulation simulation, String label, boolean isLocated) {
        for (LocatedObservable observable : simulation.getLocatedObservables()) {
            if (label.equals(isLocated ? observable.toString() : observable.observable.label)) {
                return observable;
            }
        }
        Assert.fail("Observable not found: " + label);
        return null;
    }

    public static LocatedObservable getSimulationObservable(AbstractSimulation simulation, String label) {
        return getSimulationObservable(simulation, label, false);
    }

    public static LocatedObservable[] getSimulationObservables(AbstractSimulation simulation, String... labels) {
        LocatedObservable[] result = new LocatedObservable[labels.length];
        for (int index = 0; index < result.length; index++) {
            result[index] = getSimulationObservable(simulation, labels[index]);
        }
        return result;
    }

    protected void checkEventSimulation(String simulationText, String[] observableNames, int eventsPerStep, int accuracy, int[][] expectedObservableValues) throws Exception {
        AbstractSimulation simulation = createSimulation(simulationText);
        LocatedObservable[] observables = getSimulationObservables(simulation, observableNames);
        
        // Check time 0
        checkObservations(0, observables, accuracy, expectedObservableValues[0]);
   
        // Check remaining steps
        for (int index = 1; index < expectedObservableValues.length; index++) {
            simulation.runByEvent(1, eventsPerStep);
            checkObservations(index, observables, accuracy, expectedObservableValues[index]);
        }
    }

    private final void checkObservations(int observation, LocatedObservable[] observables, int accuracy, int... values) {
        for (int index = 0; index < observables.length; index++) {
            assertEquals("Observation " + observation + ": " + observables[index], values[index], currentObservation.observables.get(observables[index]).value, accuracy);
        }
    }

    protected final void checkTimedSimulation(String simulationText, String[] observableNames, float timePerStep, int accuracy, int[][] expectedObservableValues) throws Exception {
        AbstractSimulation simulation = createSimulation(simulationText);
        LocatedObservable[] observables = getSimulationObservables(simulation, observableNames);
        
        // Check time 0
        checkObservations(0, observables, accuracy, expectedObservableValues[0]);
   
        // Check remaining steps
        for (int index = 1; index < expectedObservableValues.length; index++) {
            simulation.runByTime(0f, timePerStep);
            checkObservations(index, observables, accuracy, expectedObservableValues[index]);
        }
    }
    


    private static final String SIMPLE_STATE_INPUT = 
        "A(x~s) -> A(x~t) @ 0.1\n" + 
        "%init: 1000 * (A(x~s))\n" + 
        "%obs: A(x~s)\n" + 
        "%obs: A(x~t)\n";
    
    private static final String SIMPLE_INFINITE_RATE_INPUT = 
        "A(x~s) -> A(x~t) @ $INF\n" + 
        "%init: 1000 * (A(x~s))\n" + 
        "%obs: A(x~s)\n" + 
        "%obs: A(x~t)\n";
    
    private static final String INFINITE_RATE_GRADUAL_SUBSTRATE_INPUT = 
        "A(x~s) -> A(x~t) @ 0.1\n" + 
        "A(x~t) -> A(x~u) @ $INF\n" + 
        "%init: 1000 * (A(x~s))\n" + 
        "%obs: A(x~s)\n" + 
        "%obs: A(x~t)\n" +
        "%obs: A(x~u)\n";
    
    private static final String ERROR_CASE1_INPUT = 
        "A(a), B(a) -> A(a!1),B(a!1) @ 1\n" + 
        "%init: 1000 * (A(a,b)) \n" + 
        "%init: 1000 * (A(a!2, b!1), B(a, c!1, b), B(a!2, c, b)) \n" + 
        "%obs: A(a) \n" + 
        "%obs: 'result' A(a!2, b!1), B(a!3, c!1, b), B(a!2, c, b), A(a!3, b)\n";
    
    private static final String ADDITIONAL_STATE_INPUT = 
        "A(x~s) -> A(x~t) @ 0.1\n" + 
        "%init: 1000 * (A(x~s,y~a))\n" + 
        "%obs: A(x~s)\n" + 
        "%obs: A(x~t,y~a)\n";
    
    private static final String SIMPLE_LINK_INPUT = 
        "A(x),B(y) -> A(x!1),B(y!1) @ 0.1\n" + 
        "%init: 10000 * (A(x),B(y))\n" + 
        "%obs: A(x)\n" + 
        "%obs: A(x!_)\n";
    
    private static final String TIMED_PERTURBATION_INPUT = 
        "A(x~a) -> A(x~b) @ 0.1\n" + 
        "'triggered' C(z~a) -> C(z~b) @ 0\n" + 
        "%init: 10000 * (A(x~a),C(z~a))\n" + 
        "%obs: A(x~a)\n" +
        "%obs: C(z~a)\n" +
        "%mod: $T > 7 do 'triggered' := 0.1\n";
    
    private static final String TIMED_INFINITE_RATE_PERTURBATION_INPUT = 
        "A(x~a) -> A(x~b) @ 0.1\n" + 
        "'triggered' C(z~a) -> C(z~b) @ 0\n" + 
        "%init: 10000 * (A(x~a),C(z~a))\n" + 
        "%obs: A(x~a)\n" +
        "%obs: C(z~a)\n" +
        "%mod: $T > 3 do 'triggered' := $INF\n";
    
    private static final String SIMPLE_LINK_OBSERVED_RULE_INPUT = 
        "'link' A(x),B(y) -> A(x!1),B(y!1) @ 0.1\n" + 
        "%init: 10000 * (A(x),B(y))\n" + 
        "%obs: 'link'\n";
    
    private static final String CREATE_AGENT_LINK_INPUT = 
        "A(x) -> A(x!1),B(y!1) @ 0.1\n" + 
        "%init: 10000 * (A(x))\n" + 
        "%obs: A(x)\n" + 
        "%obs: A(x!1),B(y!1)\n";
    
    private static final String CREATE_COMPLEX_INPUT = 
        "A(x) -> A(x),B(y!1),C(z!1) @ 0.1\n" + 
        "%init: 10000 * (A(x))\n" + 
        "%obs: A(x)\n" + 
        "%obs: B(y!1),C(z!1)\n";
    
    private static final String DELETE_LINK_INPUT = 
        "A(x!1),B(y!1) -> A(x),B(y)  @ 0.1\n" + 
        "%init: 10000 * (A(x!1),B(y!1))\n" + 
        "%obs: B(y)\n" + 
        "%obs: A(x!_)\n";
    
    private static final String DELETE_AGENT_LINK_INPUT = 
        "A(x!1),B(y!1) -> A(x) @ 0.1\n" + 
        "%init: 10000 * (A(x!1),B(y!1))\n" + 
        "%obs: B(y?)\n";
    
    private static final String DELETE_COMPLEX_INPUT = 
        "A(x) ->  @ 0.1\n" + 
        "%init: 10000 * (A(x))\n" + 
        "%obs: A(x)\n" + 
        "%obs: A(x!_)\n";
    
    private static final String DELETE_COMPLEX_INPUT_NO_SITES = 
        "A() ->  @ 0.1\n" + 
        "%init: 10000 * (A())\n" + 
        "%obs: A()\n";
    
    private static final String SIMPLE_LINK_EQUILIBRIUM_INPUT = 
        "A(x),B(y) <-> A(x!1),B(y!1) @ 1,1\n" + 
        "%init: 10000 * (A(x),B(y))\n" + 
        "%obs: A(x)\n" + 
        "%obs: A(x!_)\n";

    private final AbstractSimulation createSimulation(String inputText) throws Exception {
        ANTLRInputStream input = new ANTLRInputStream(new ByteArrayInputStream(inputText.getBytes()));
        CommonTokenStream tokens = new CommonTokenStream(new SpatialKappaLexer(input));
        SpatialKappaParser.prog_return r = new SpatialKappaParser(tokens).prog();

        CommonTree t = (CommonTree) r.getTree();
        CommonTreeNodeStream nodes = new CommonTreeNodeStream(t);
        nodes.setTokenStream(tokens);
        SpatialKappaWalker walker = new SpatialKappaWalker(nodes);
        AbstractSimulation simulation = createSimulation(walker.prog());
        simulation.reset();
        currentObservation = simulation.getCurrentObservation();
        
        simulation.addObservationListener(new ObservationListener() {
            
            public void observation(Observation observation) {
                currentObservation = observation;
            }
        });
        return simulation;
    }
    
    protected abstract AbstractSimulation createSimulation(KappaModel kappaModel) throws Exception;
    

    
    @Test
    public void testSimpleTransport() throws Exception {
        checkEventSimulation(SIMPLE_TRANSPORT_INPUT, new String[] {"val[0]", "val[1]", "val[2]", "val[3]"}, 400, 60, new int[][] {
                {2000, 0, 0, 0}, {1680, 300, 20, 0}, {1500, 450, 50, 0}, {1350, 500, 100, 0},
        });
    }
    
    @Test
    public void testDirectionalTransport() throws Exception {
        checkEventSimulation(DIRECTIONAL_TRANSPORT_INPUT, 
                new String[] {"val[0]A", "val[1]A", "val[2]A", "val[0]B", "val[1]B", "val[2]B", }, 2000, 100, new int[][] {
                {1000, 0, 0, 0, 0, 1000}, {330, 330, 330, 330, 330, 330}, {0, 0, 1000, 1000, 0, 0}, 
        });
    }
    
    @Test
    public void testMultipleNamesDiscreteComplexTransport() throws Exception {
        checkEventSimulation(MULTIPLE_NAMES_DISCRETE_COMPLEX_TRANSPORT_INPUT, 
                new String[] {"val[0]A", "val[1]A", "val[0]B", "val[1]B", }, 1000, 0, new int[][] {
                {1000, 0, 0, 1000}, {0, 1000, 0, 1000}, 
        });
    }
    
    @Test
    public void testVerySimpleTransport() throws Exception {
        checkEventSimulation(VERY_SIMPLE_TRANSPORT_INPUT, new String[] {"val[0]", "val[1]"}, 1, 0, new int[][] {
                {10, 0}, {9, 1}, {8, 2}
        });
    }
    
    @Test
    public void testInitialDistribution() throws Exception {
        checkEventSimulation(INITIAL_DISTRIBUTION_INPUT, new String[] {"nucleus", "val[0]", "val[1]", "val[2]", "val[3]", "val[4]"}, 1, 0, new int[][] {
                {20, 420, 420, 1020, 420, 420}
        });
    }
    
    @Test
    public void testCellLimitedTransform() throws Exception {
        checkEventSimulation(CELL_LIMITED_TRANSFORM_INPUT, new String[] {"cytosol[0]", "cytosol[1]", "membrane[1]"}, 500, 0, new int[][] {
                {1000, 1000, 500}, {1000, 500, 500}, {1000, 0, 500}, 
        });
    }
    
    @Test
    public void testCompartmentLimitedTransform() throws Exception {
        checkEventSimulation(COMPARTMENT_LIMITED_TRANSFORM_INPUT, new String[] {"cytosol[0]", "cytosol[1]", "membrane[1]"}, 1000, 50, new int[][] {
                {1000, 1000, 500}, {500, 500, 500}, {0, 0, 500}, 
        });
    }
    
    @Test
    public void testUnlimitedTransform() throws Exception {
        checkEventSimulation(UNLIMITED_TRANSFORM_INPUT, new String[] {"cytosol[0]", "cytosol[1]", "membrane[1]"}, 1000, 50, new int[][] {
                {1000, 1000, 1000}, {670, 670, 670}, {330, 330, 330}, 
        });
    }
    
    @Test
    public void testSteadyStateConcentrationGradient() throws Exception {
        checkEventSimulation(STEADY_STATE_CONCENTRATION_GRADIENT_INPUT, new String[] {"val[0]", "val[1]", "val[2]", "val[3]"}, 3000, 50, new int[][] {
                {0, 0, 0, 0}, {150, 100, 50, 1}, {150, 100, 50, 1}, 
        });
    }
    
    @Test
    public void testTransitionActivation() throws Exception {
        checkEventSimulation(TRANSITION_ACTIVATION_INPUT, new String[] {"C"}, 3000, 50, new int[][] {
                {0}, {1000}, 
        });
    }
    
    private static final String VERY_SIMPLE_TRANSPORT_INPUT = 
        "%compartment: 'cytosol' [2]\n" + 
        "%link: 'intra-cytosol' 'cytosol' [x] -> 'cytosol' [x+1]\n" + 
        "%transport: 'diffusion-all' 'intra-cytosol' @ 0.1\n" + 
        "%init: 'cytosol'[0] 10 * (A()) \n" + 
        "%obs: 'val[0]' 'cytosol'[0] A() \n" + 
        "%obs: 'val[1]' 'cytosol'[1] A() \n" + 
        "";
    
    private static final String SIMPLE_TRANSPORT_INPUT = 
        "%compartment: 'cytosol' [4]\n" + 
        "%link: 'intra-cytosol' 'cytosol' [x] <-> 'cytosol' [x+1]\n" + 
        "%transport: 'diffusion-all' 'intra-cytosol' @ 0.1\n" + 
        "%init: 'cytosol'[0] 2000 * (A()) \n" + 
        "%obs: 'val[0]' 'cytosol'[0] A() \n" + 
        "%obs: 'val[1]' 'cytosol'[1] A() \n" + 
        "%obs: 'val[2]' 'cytosol'[2] A() \n" + 
        "%obs: 'val[3]' 'cytosol'[3] A() \n" + 
        "";
    
    private static final String DIRECTIONAL_TRANSPORT_INPUT = 
        "%compartment: 'cytosol' [3]\n" + 
        "%link: 'forward' 'cytosol' [x] -> 'cytosol' [x+1]\n" + 
        "%link: 'backward' 'cytosol' [x] <- 'cytosol' [x+1]\n" + 
        "%transport: 'forward'       'forward'       A() @ 0.1\n" + 
        "%transport: 'backward'      'backward'      B() @ 0.1\n" + 
        "%init: 'cytosol'[0] 1000 * (A()) \n" + 
        "%init: 'cytosol'[2] 1000 * (B()) \n" + 
        "%obs: 'val[0]A' 'cytosol'[0] A() \n" + 
        "%obs: 'val[1]A' 'cytosol'[1] A() \n" + 
        "%obs: 'val[2]A' 'cytosol'[2] A() \n" + 
        "%obs: 'val[0]B' 'cytosol'[0] B() \n" + 
        "%obs: 'val[1]B' 'cytosol'[1] B() \n" + 
        "%obs: 'val[2]B' 'cytosol'[2] B() \n" + 
        "";
    
    private static final String MULTIPLE_NAMES_DISCRETE_COMPLEX_TRANSPORT_INPUT = 
        "%compartment: 'cytosol' [2]\n" + 
        "%link: 'forward' 'cytosol' [x] -> 'cytosol' [x+1]\n" + 
        "%transport: 'forward'       'forward'       B(),A() @ 0.1\n" + 
        "%init: 'cytosol'[0] 1000 * (A()) \n" + 
        "%init: 'cytosol'[1] 1000 * (B()) \n" + 
        "%obs: 'val[0]A' 'cytosol'[0] A() \n" + 
        "%obs: 'val[1]A' 'cytosol'[1] A() \n" + 
        "%obs: 'val[0]B' 'cytosol'[0] B() \n" + 
        "%obs: 'val[1]B' 'cytosol'[1] B() \n" + 
        "";
    
    private static final String INITIAL_DISTRIBUTION_INPUT = 
        "%compartment: 'cytosol' [5]\n" + 
        "%compartment: 'nucleus' \n" + 
        "%init:              120 * (A()) \n" + 
        "%init: 'cytosol'    2000 * (A()) \n" + 
        "%init: 'cytosol'[2] 600 * (A()) \n" + 
        "%obs: 'nucleus' 'nucleus'   A() \n" + 
        "%obs: 'val[0]' 'cytosol'[0] A() \n" + 
        "%obs: 'val[1]' 'cytosol'[1] A() \n" + 
        "%obs: 'val[2]' 'cytosol'[2] A() \n" + 
        "%obs: 'val[3]' 'cytosol'[3] A() \n" + 
        "%obs: 'val[4]' 'cytosol'[4] A() \n" + 
        "";
    
    private static final String CELL_LIMITED_TRANSFORM_INPUT = 
        "%compartment: 'cytosol' [2]\n" + 
        "%compartment: 'membrane'\n" + 
        "'react' 'cytosol'[1] A(S~x) -> A(S~y) @ 0.1\n" + 
        "%init: 'cytosol' 2000 * (A(S~x)) \n" + 
        "%init: 'membrane' 500 * (A(S~x)) \n" + 
        "%obs: 'cytosol[0]' 'cytosol'[0] A(S~x) \n" + 
        "%obs: 'cytosol[1]' 'cytosol'[1] A(S~x) \n" + 
        "%obs: 'membrane[1]' 'membrane' A(S~x) \n" + 
        "";
    
    private static final String COMPARTMENT_LIMITED_TRANSFORM_INPUT = 
        "%compartment: 'cytosol' [2]\n" + 
        "%compartment: 'membrane'\n" + 
        "'react' 'cytosol' A(S~x) -> A(S~y) @ 0.1\n" + 
        "%init: 'cytosol' 2000 * (A(S~x)) \n" + 
        "%init: 'membrane' 500 * (A(S~x)) \n" + 
        "%obs: 'cytosol[0]' 'cytosol'[0] A(S~x) \n" + 
        "%obs: 'cytosol[1]' 'cytosol'[1] A(S~x) \n" + 
        "%obs: 'membrane[1]' 'membrane' A(S~x) \n" + 
        "";
    
    private static final String UNLIMITED_TRANSFORM_INPUT = 
        "%compartment: 'cytosol' [2]\n" + 
        "%compartment: 'membrane'\n" + 
        "'react' A(S~x) -> A(S~y) @ 0.1\n" + 
        "%init: 'cytosol' 2000 * (A(S~x)) \n" + 
        "%init: 'membrane' 1000 * (A(S~x)) \n" + 
        "%obs: 'cytosol[0]' 'cytosol'[0] A(S~x) \n" + 
        "%obs: 'cytosol[1]' 'cytosol'[1] A(S~x) \n" + 
        "%obs: 'membrane[1]' 'membrane' A(S~x) \n" + 
        "";
    
    private static final String TRANSITION_ACTIVATION_INPUT = 
        "%compartment: 'cytosol' [3] \n" + 
        "%link: 'intra-cytosola' 'cytosol' [0] -> 'cytosol' [1] \n" + 
        "%link: 'intra-cytosolb' 'cytosol' [2] -> 'cytosol' [1] \n" + 
        "'react' 'cytosol' A(x),B(x) -> A(x!1),B(x!1) @ 0.1\n" + 
        "%transport: 'diffusion-a' 'intra-cytosola' A(x) @ 0.1 \n" + 
        "%transport: 'diffusion-b' 'intra-cytosolb' B(x) @ 0.1 \n" + 
        "%init: 'cytosol'[0] 1000 * (A(x)) \n" + 
        "%init: 'cytosol'[2] 1000 * (B(x)) \n" + 
        "%obs: 'C' 'cytosol' A(x!1),B(x!1)\n" + 
        "";
    
    private static final String STEADY_STATE_CONCENTRATION_GRADIENT_INPUT = 
        "%compartment: 'cytosol' [4]\n" + 
        "%link: 'intra-cytosol' 'cytosol' [x] <-> 'cytosol' [x+1]\n" + 
        "%transport: 'diffusion-all' 'intra-cytosol' @ 0.1\n" + 
        "'source' 'cytosol'[0] -> A() @ 5\n" + 
        "'sink' 'cytosol'[3] A() -> @ $INF\n" + 
        "%obs: 'val[0]' 'cytosol'[0] A() \n" + 
        "%obs: 'val[1]' 'cytosol'[1] A() \n" + 
        "%obs: 'val[2]' 'cytosol'[2] A() \n" + 
        "%obs: 'val[3]' 'cytosol'[3] A() \n" + 
        "";
    

}
