package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_0;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_1;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_2;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_X;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_X_PLUS_1;
import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;
import static org.demonsoft.spatialkappa.model.Transition.DELETED;
import static org.demonsoft.spatialkappa.model.Transition.UNMAPPED;
import static org.demonsoft.spatialkappa.model.Utils.getComplexes;
import static org.demonsoft.spatialkappa.model.Utils.getList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.demonsoft.spatialkappa.model.VariableExpression.Constant;
import org.junit.Test;

public class TransitionTest {


    @SuppressWarnings("unused")
    @Test
    public void testTransition_full() {
        VariableExpression rate = new VariableExpression("0.01");
        List<Agent> leftAgents = Utils.getList(new Agent("agent1"));
        List<Agent> rightAgents = Utils.getList(new Agent("agent2"));

        try {
            new Transition("label", leftAgents, "channelName", rightAgents, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            new Transition("label", (List<Agent>) null, null, null, rate);
            fail("null agents and channel should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            new Transition("label", new ArrayList<Agent>(), null, new ArrayList<Agent>(), rate);
            fail("lhs and rhs empty and null channel should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        leftAgents = Utils.getList(
                new Agent("agent1", new AgentSite("interface1", "state1", "link1"), 
                        new AgentSite("interface2", "state2", "link1")),
                new Agent("agent3", new Location("left")));
        rightAgents = Utils.getList(
                new Agent("agent2"), new Agent("agent4", new Location("right")));
        
        // All parameters
        
        Transition transition = new Transition("label", leftAgents, "channel", rightAgents, rate);

        List<Agent> expectedLeftAgents = Utils.getList(
                new Agent("agent1", NOT_LOCATED, new AgentSite("interface1", "state1", "link1"), 
                        new AgentSite("interface2", "state2", "link1")),
                new Agent("agent3", new Location("left")));
        List<Agent> expectedRightAgents = Utils.getList(
                new Agent("agent2", NOT_LOCATED), 
                new Agent("agent4", new Location("right")));
        List<Complex> expectedLeftComplexes = Utils.getComplexes(expectedLeftAgents);
        List<Complex> expectedRightComplexes = Utils.getComplexes(expectedRightAgents);

        checkComplexes(expectedLeftComplexes, transition.sourceComplexes);
        checkComplexes(expectedRightComplexes, transition.targetComplexes);
        checkAgents(expectedLeftAgents, transition.leftAgents);
        checkAgents(expectedRightAgents, transition.rightAgents);
        assertEquals("label", transition.label);
        assertEquals("channel", transition.channelName);
        assertSame(rate, transition.getRate());
        assertFalse(transition.isInfiniteRate(new HashMap<String, Variable>()));
        assertEquals("'label' : [[agent1(interface1~state1!link1,interface2~state2!link1)], [agent3:left()]]" +
                " ->:channel [[agent2()], [agent4:right()]] @ 0.01", transition.toString());
        assertNull(transition.leftLocation);
        assertNull(transition.rightLocation);

        // No channel
        
        transition = new Transition("label", leftAgents, null, rightAgents, rate);

        expectedLeftAgents = Utils.getList(
                new Agent("agent1", NOT_LOCATED, new AgentSite("interface1", "state1", "link1"), 
                        new AgentSite("interface2", "state2", "link1")),
                new Agent("agent3", new Location("left")));
        expectedRightAgents = Utils.getList(
                new Agent("agent2", NOT_LOCATED), 
                new Agent("agent4", new Location("right")));
        expectedLeftComplexes = Utils.getComplexes(expectedLeftAgents);
        expectedRightComplexes = Utils.getComplexes(expectedRightAgents);

        checkComplexes(expectedLeftComplexes, transition.sourceComplexes);
        checkComplexes(expectedRightComplexes, transition.targetComplexes);
        checkAgents(expectedLeftAgents, transition.leftAgents);
        checkAgents(expectedRightAgents, transition.rightAgents);
        assertEquals("label", transition.label);
        assertEquals(null, transition.channelName);
        assertSame(rate, transition.getRate());
        assertFalse(transition.isInfiniteRate(new HashMap<String, Variable>()));
        assertEquals("'label' : [[agent1(interface1~state1!link1,interface2~state2!link1)], [agent3:left()]]" +
                " -> [[agent2()], [agent4:right()]] @ 0.01", transition.toString());
        assertNull(transition.leftLocation);
        assertNull(transition.rightLocation);

        // No agents, infinite rate
        rate = new VariableExpression(Constant.INFINITY);
        transition = new Transition(null, (List<Agent>) null, "channel", null, rate);

        expectedLeftAgents.clear();
        expectedRightAgents.clear();
        expectedLeftComplexes.clear();
        expectedRightComplexes.clear();

        checkComplexes(expectedLeftComplexes, transition.sourceComplexes);
        checkComplexes(expectedRightComplexes, transition.targetComplexes);
        checkAgents(expectedLeftAgents, transition.leftAgents);
        checkAgents(expectedRightAgents, transition.rightAgents);
        assertEquals(null, transition.label);
        assertEquals("channel", transition.channelName);
        assertSame(rate, transition.getRate());
        assertTrue(transition.isInfiniteRate(new HashMap<String, Variable>()));
        assertEquals("[] ->:channel [] @ [inf]", transition.toString());
        assertNull(transition.leftLocation);
        assertNull(transition.rightLocation);
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testTransition_noAgents_transportOnly() {
        VariableExpression rate = new VariableExpression("0.01");
        Location leftLocation = new Location("left");
        Location rightLocation = new Location("right");

        try {
            new Transition("label", leftLocation, "channelName", rightLocation, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            new Transition("label", leftLocation, null, rightLocation, null);
            fail("null channel should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        // All parameters
        
        Transition transition = new Transition("label", leftLocation, "channel", rightLocation, rate);

        List<Agent> emptyAgents = new ArrayList<Agent>();
        List<Complex> emptyComplexes = new ArrayList<Complex>();

        checkComplexes(emptyComplexes, transition.sourceComplexes);
        checkComplexes(emptyComplexes, transition.targetComplexes);
        checkAgents(emptyAgents, transition.leftAgents);
        checkAgents(emptyAgents, transition.rightAgents);
        assertEquals("label", transition.label);
        assertEquals("channel", transition.channelName);
        assertSame(rate, transition.getRate());
        assertFalse(transition.isInfiniteRate(new HashMap<String, Variable>()));
        assertEquals("'label' : left ->:channel right @ 0.01", transition.toString());
        assertEquals(leftLocation, transition.leftLocation);
        assertEquals(rightLocation, transition.rightLocation);

        // No right location
        
        transition = new Transition("label", leftLocation, "channel", null, rate);

        checkComplexes(emptyComplexes, transition.sourceComplexes);
        checkComplexes(emptyComplexes, transition.targetComplexes);
        checkAgents(emptyAgents, transition.leftAgents);
        checkAgents(emptyAgents, transition.rightAgents);
        assertEquals("label", transition.label);
        assertEquals("channel", transition.channelName);
        assertSame(rate, transition.getRate());
        assertFalse(transition.isInfiniteRate(new HashMap<String, Variable>()));
        assertEquals("'label' : left ->:channel  @ 0.01", transition.toString());
        assertEquals(leftLocation, transition.leftLocation);
        assertNull(transition.rightLocation);

        // No left location
        
        transition = new Transition("label", null, "channel", rightLocation, rate);

        checkComplexes(emptyComplexes, transition.sourceComplexes);
        checkComplexes(emptyComplexes, transition.targetComplexes);
        checkAgents(emptyAgents, transition.leftAgents);
        checkAgents(emptyAgents, transition.rightAgents);
        assertEquals("label", transition.label);
        assertEquals("channel", transition.channelName);
        assertSame(rate, transition.getRate());
        assertFalse(transition.isInfiniteRate(new HashMap<String, Variable>()));
        assertEquals("'label' :  ->:channel right @ 0.01", transition.toString());
        assertNull(transition.leftLocation);
        assertEquals(rightLocation, transition.rightLocation);

        // No left or right location
        
        transition = new Transition("label", (Location) null, "channel", (Location) null, rate);

        checkComplexes(emptyComplexes, transition.sourceComplexes);
        checkComplexes(emptyComplexes, transition.targetComplexes);
        checkAgents(emptyAgents, transition.leftAgents);
        checkAgents(emptyAgents, transition.rightAgents);
        assertEquals("label", transition.label);
        assertEquals("channel", transition.channelName);
        assertSame(rate, transition.getRate());
        assertFalse(transition.isInfiniteRate(new HashMap<String, Variable>()));
        assertEquals("'label' : [] ->:channel [] @ 0.01", transition.toString());
        assertNull(transition.leftLocation);
        assertNull(transition.rightLocation);
    }
    
    
    private void checkAgents(List<Agent> expected, List<Agent> actual) {
        assertEquals(expected.size(), actual.size());

        Set<String> actualStrings = new HashSet<String>();
        for (Agent agent : actual) {
            actualStrings.add(agent.toString());
        }
        for (Agent agent : expected) {
            String agentString = agent.toString();
            if (!actualStrings.contains(agentString)) {
                fail("agent not found: " + agentString + "\nin:" + actualStrings);
            }
        }
    }

    
    private void checkComplexes(List<Complex> expected, List<Complex> actual) {
        assertEquals(expected.size(), actual.size());

        Set<String> actualStrings = new HashSet<String>();
        for (Complex complex : actual) {
            actualStrings.add(complex.toString());
        }
        for (Complex complex : expected) {
            String complexString = complex.toString();
            if (!actualStrings.contains(complexString)) {
                fail("Complex not found: " + complexString + "\nin:" + actualStrings);
            }
        }
    }

    @Test
    public void testTransition_checkPrimitives_noChannel() {
        Agent leftAgent1 = new Agent("DNA", new AgentSite("downstream", null, "2"), new AgentSite("type~BBaB0000", null, null), new AgentSite("binding", null, "1")); 
        Agent leftAgent2 = new Agent("DNA", new AgentSite("upstream", null, "2"), new AgentSite("binding", null, null));
        Agent leftAgent3 = new Agent("RNA", new AgentSite("downstream", null, "3"));
        Agent leftAgent4 = new Agent("RNAP", new AgentSite("dna", null, "1"), new AgentSite("rna", null, "3"));
        
        List<Agent> leftAgents = Utils.getList(leftAgent1, leftAgent2, leftAgent3, leftAgent4);
        
        Agent rightAgent1 = new Agent("DNA", new AgentSite("downstream", null, "2"), new AgentSite("type~BBaB0000", null, null), new AgentSite("binding", null, null));
        Agent rightAgent2 = new Agent("DNA", new AgentSite("upstream", null, "2"), new AgentSite("binding", null, "1"));
        Agent rightAgent3 = new Agent("RNA", new AgentSite("downstream", null, "3"), new AgentSite("type~BBaB0000", null, null), new AgentSite("upstream", null, "4"), new AgentSite("binding", null, null));
        Agent rightAgent4 = new Agent("RNA", new AgentSite("downstream", null, "4"));
        Agent rightAgent5 = new Agent("RNAP", new AgentSite("dna", null, "1"), new AgentSite("rna", null, "3"));
        
        List<Agent> rightAgents = Utils.getList(rightAgent1, rightAgent2, rightAgent3, rightAgent4, rightAgent5);
                
        Transition transition = new Transition("test", leftAgents, null, rightAgents, 10.0f);
                
        TransitionPrimitive[] expected = new TransitionPrimitive[] {
                TransitionPrimitive.getDeleteLink(getAgentLink(leftAgents, "1")),
                TransitionPrimitive.getDeleteLink(leftAgent2.getComplex().getAgentLink(leftAgent2, "binding")),
                TransitionPrimitive.getDeleteLink(getAgentLink(rightAgents, "3")),
                TransitionPrimitive.getCreateAgent(rightAgent3, leftAgent2),
                TransitionPrimitive.getCreateLink(leftAgent2.getSite("binding"), leftAgent4.getSite("dna"), null),
                TransitionPrimitive.getCreateLink(leftAgent3.getSite("downstream"), rightAgent3.getSite("upstream"), null),
                TransitionPrimitive.getCreateLink(leftAgent4.getSite("rna"), rightAgent3.getSite("downstream"), null),
        };
        checkPrimitives(Arrays.asList(expected), transition.bestPrimitives);
        
        Map<Agent, Agent> expectedAgentMap = new HashMap<Agent, Agent>();
        expectedAgentMap.put(leftAgent1, rightAgent1);
        expectedAgentMap.put(leftAgent2, rightAgent2);
        expectedAgentMap.put(leftAgent3, rightAgent4);
        expectedAgentMap.put(leftAgent4, rightAgent5);
        
        assertEquals(expectedAgentMap, transition.getLeftRightAgentMap());
    }

    @Test
    public void testTransition_checkPrimitives_channel_singleAgentRule() {
        Agent leftAgent = new Agent("DNA"); 
        Agent rightAgent = new Agent("DNA");
        Transition transition = new Transition("test", Utils.getList(leftAgent), "channel", Utils.getList(rightAgent), 10.0f);
                
        TransitionPrimitive[] expected = new TransitionPrimitive[] {
                TransitionPrimitive.getMoveAgents(getList(leftAgent), getList(NOT_LOCATED), "channel"),
        };
        checkPrimitives(Arrays.asList(expected), transition.bestPrimitives);
        
        Map<Agent, Agent> expectedAgentMap = new HashMap<Agent, Agent>();
        expectedAgentMap.put(leftAgent, rightAgent);
        assertEquals(expectedAgentMap, transition.getLeftRightAgentMap());

        // Located agents
        
        leftAgent = new Agent("DNA", new Location("source")); 
        rightAgent = new Agent("DNA", new Location("target"));
        transition = new Transition("test", Utils.getList(leftAgent), "channel", Utils.getList(rightAgent), 10.0f);
                
        expected = new TransitionPrimitive[] {
                TransitionPrimitive.getMoveAgents(getList(leftAgent), getList(new Location("target")), "channel"),
        };
        checkPrimitives(Arrays.asList(expected), transition.bestPrimitives);
        
        expectedAgentMap.clear();
        expectedAgentMap.put(leftAgent, rightAgent);
        assertEquals(expectedAgentMap, transition.getLeftRightAgentMap());
    }

    @Test
    public void testTransition_checkPrimitives_channel_multipleAgentRule() {
        List<Agent> leftAgents = getList(new Agent("A", new Location("locA")), new Agent("B", new Location("locA")), new Agent("C", new Location("locA"))); 
        List<Agent> rightAgents = getList(new Agent("A", new Location("locB")), new Agent("B", new Location("locB")), new Agent("C", new Location("locA"))); 
        Transition transition = new Transition("test", leftAgents, "channel", rightAgents, 10.0f);
                
        TransitionPrimitive[] expected = new TransitionPrimitive[] {
                TransitionPrimitive.getMoveAgents(leftAgents, 
                        getList(new Location("locB"), new Location("locB"), new Location("locA")), "channel"),
        };
        checkPrimitives(Arrays.asList(expected), transition.bestPrimitives);
        
        Map<Agent, Agent> expectedAgentMap = new HashMap<Agent, Agent>();
        expectedAgentMap.put(leftAgents.get(0), rightAgents.get(0));
        expectedAgentMap.put(leftAgents.get(1), rightAgents.get(1));
        expectedAgentMap.put(leftAgents.get(2), rightAgents.get(2));
        assertEquals(expectedAgentMap, transition.getLeftRightAgentMap());
    }

    @Test
    public void testApply_channel_singleAgentRule() {
        List<Compartment> compartments = getList(new Compartment("A"), new Compartment("B"));
        List<Channel> channels = getList(new Channel("channel", new Location("A"), new Location("B")));

        Agent leftTemplateAgent = new Agent("DNA"); 
        Agent rightTemplateAgent = new Agent("DNA");
        Transition transition = new Transition("test", Utils.getList(leftTemplateAgent), "channel", Utils.getList(rightTemplateAgent), 10.0f);
        Complex leftTemplateComplex = transition.sourceComplexes.get(0);        
        
        TransitionPrimitive[] expected = new TransitionPrimitive[] {
                TransitionPrimitive.getMoveAgents(getList(leftTemplateAgent), getList(NOT_LOCATED), "channel"),
        };
        checkPrimitives(Arrays.asList(expected), transition.bestPrimitives);
        
        Map<Agent, Agent> expectedAgentMap = new HashMap<Agent, Agent>();
        expectedAgentMap.put(leftTemplateAgent, rightTemplateAgent);
        assertEquals(expectedAgentMap, transition.getLeftRightAgentMap());


        Agent leftRealAgent = new Agent("DNA", new Location("A"));
        Complex leftRealComplex = new Complex(leftRealAgent);
        Map<Agent, Agent> mapping = new HashMap<Agent, Agent>();
        mapping.put(leftTemplateAgent, leftRealAgent);
        List<ComplexMapping> sourceComplexMappings = getList(new ComplexMapping(leftTemplateComplex, leftRealComplex, mapping));
        TransitionInstance transitionInstance = new TransitionInstance(sourceComplexMappings, 1);
        
        List<Complex> result = transition.apply(transitionInstance, channels, compartments);
        assertEquals("[[DNA:B()]]", result.toString());
        // TODO test predefined channel types

    }

    @Test
    public void testApply_channel_multipleAgentRule() {
        List<Compartment> compartments = getList(new Compartment("locA"), new Compartment("locB"), new Compartment("locC"), new Compartment("locD"), new Compartment("locE"));
        Channel channel = new Channel("channel");
        channel.addChannelComponent(null, getList(new Location("locA"), new Location("locB")), 
                getList(new Location("locD"), new Location("locE")));
        
        List<Channel> channels = getList(channel);

        List<Agent> leftTemplateAgents = getList(new Agent("A", new Location("locA")), new Agent("B", new Location("locB")), new Agent("C", new Location("locC"))); 
        List<Agent> rightTemplateAgents = getList(new Agent("A", new Location("locD")), new Agent("B", new Location("locE")), new Agent("C", new Location("locC"))); 
        Transition transition = new Transition("test", leftTemplateAgents, "channel", rightTemplateAgents, 10.0f);

        TransitionPrimitive[] expected = new TransitionPrimitive[] {
                TransitionPrimitive.getMoveAgents(leftTemplateAgents, getList(new Location("locD"), new Location("locE"), new Location("locC")), "channel"),
        };
        checkPrimitives(Arrays.asList(expected), transition.bestPrimitives);
        
        Map<Agent, Agent> expectedAgentMap = new HashMap<Agent, Agent>();
        expectedAgentMap.put(leftTemplateAgents.get(0), rightTemplateAgents.get(0));
        expectedAgentMap.put(leftTemplateAgents.get(1), rightTemplateAgents.get(1));
        expectedAgentMap.put(leftTemplateAgents.get(2), rightTemplateAgents.get(2));
        assertEquals(expectedAgentMap, transition.getLeftRightAgentMap());


        List<Agent> leftRealAgents = getList(new Agent("A", new Location("locA")), new Agent("B", new Location("locB")), new Agent("C", new Location("locC"))); 
        getComplexes(leftRealAgents);
        Map<Agent, Agent> mapping1 = new HashMap<Agent, Agent>();
        Map<Agent, Agent> mapping2 = new HashMap<Agent, Agent>();
        Map<Agent, Agent> mapping3 = new HashMap<Agent, Agent>();
        mapping1.put(leftTemplateAgents.get(0), leftRealAgents.get(0));
        mapping2.put(leftTemplateAgents.get(1), leftRealAgents.get(1));
        mapping3.put(leftTemplateAgents.get(2), leftRealAgents.get(2));
        List<ComplexMapping> sourceComplexMappings = getList(
                new ComplexMapping(leftTemplateAgents.get(0).getComplex(), leftRealAgents.get(0).getComplex(), mapping1),
                new ComplexMapping(leftTemplateAgents.get(1).getComplex(), leftRealAgents.get(1).getComplex(), mapping2),
                new ComplexMapping(leftTemplateAgents.get(2).getComplex(), leftRealAgents.get(2).getComplex(), mapping3));
        TransitionInstance transitionInstance = new TransitionInstance(sourceComplexMappings, 1);
        
        List<Complex> result = transition.apply(transitionInstance, channels, compartments);
        Collections.sort(result, new Comparator<Complex>() {
            public int compare(Complex o1, Complex o2) {
                return o1.toString().compareTo(o2.toString());
            }
        });
        assertEquals("[[A:locD()], [B:locE()], [C:locC()]]", result.toString());
        // TODO test predefined channel types

    }

    @Test
    public void testApply_singleAgentOfComplexMovement() {
        List<Compartment> compartments = getList(new Compartment("A"), new Compartment("B"));
        List<Channel> channels = getList(new Channel("channel", new Location("A"), new Location("B")));

        Agent leftTemplateAgent1 = new Agent("agent1", new Location("A"), new AgentSite("s", null, "1")); 
        Agent leftTemplateAgent2 = new Agent("agent2", new Location("A"), new AgentSite("s", null, "1")); 
        Agent rightTemplateAgent1 = new Agent("agent1", new Location("B"), new AgentSite("s", null, "1", "channel")); 
        Agent rightTemplateAgent2 = new Agent("agent2", new Location("A"), new AgentSite("s", null, "1")); 
        Transition transition = new Transition("test", getList(leftTemplateAgent1, leftTemplateAgent2), 
                "channel", getList(rightTemplateAgent1, rightTemplateAgent2), 10.0f);
        Complex leftTemplateComplex = transition.sourceComplexes.get(0);        
        
        List<TransitionPrimitive> expected = getList(
                TransitionPrimitive.getDeleteLink(leftTemplateComplex.agentLinks.get(0)),
                TransitionPrimitive.getMoveAgents(getList(leftTemplateAgent1, leftTemplateAgent2), 
                        getList(new Location("B"), new Location("A")), "channel"),
                TransitionPrimitive.getCreateLink(leftTemplateAgent1.getSite("s"), leftTemplateAgent2.getSite("s"), "channel")
        );
        checkPrimitives(expected, transition.bestPrimitives);
        
        Map<Agent, Agent> expectedAgentMap = new HashMap<Agent, Agent>();
        expectedAgentMap.put(leftTemplateAgent1, rightTemplateAgent1);
        expectedAgentMap.put(leftTemplateAgent2, rightTemplateAgent2);
        assertEquals(expectedAgentMap, transition.getLeftRightAgentMap());


        Agent leftRealAgent1 = new Agent("agent1", new Location("A"), new AgentSite("s", null, "1")); 
        Agent leftRealAgent2 = new Agent("agent2", new Location("A"), new AgentSite("s", null, "1")); 
        Complex leftRealComplex = new Complex(leftRealAgent1, leftRealAgent2);
        Map<Agent, Agent> mapping = new HashMap<Agent, Agent>();
        mapping.put(leftTemplateAgent1, leftRealAgent1);
        mapping.put(leftTemplateAgent2, leftRealAgent2);
        List<ComplexMapping> sourceComplexMappings = getList(new ComplexMapping(leftTemplateComplex, leftRealComplex, mapping));
        TransitionInstance transitionInstance = new TransitionInstance(sourceComplexMappings, 1);
        
        List<Complex> result = transition.apply(transitionInstance, channels, compartments);
        assertEquals("[[agent1:B(s!1:channel), agent2:A(s!1:channel)]]", result.toString());
        // TODO test predefined channel types

    }

    @Test
    public void testTransition_checkPrimitives_locationsOnly() {
        Transition transition = new Transition("test", (Location) null, "channel", null, 10.0f);
        TransitionPrimitive[] expected = new TransitionPrimitive[] {
                TransitionPrimitive.getMoveComplex(null, null, "channel"),
        };
        checkPrimitives(Arrays.asList(expected), transition.bestPrimitives);
        assertEquals(0, transition.getLeftRightAgentMap().size());
        
        transition = new Transition("test", null, "channel", new Location("B"), 10.0f);
        expected = new TransitionPrimitive[] {
                TransitionPrimitive.getMoveComplex(null, new Location("B"), "channel"),
        };
        checkPrimitives(Arrays.asList(expected), transition.bestPrimitives);
        assertEquals(0, transition.getLeftRightAgentMap().size());
        
        transition = new Transition("test", new Location("A"), "channel", null, 10.0f);
        expected = new TransitionPrimitive[] {
                TransitionPrimitive.getMoveComplex(new Location("A"), null, "channel"),
        };
        checkPrimitives(Arrays.asList(expected), transition.bestPrimitives);
        assertEquals(0, transition.getLeftRightAgentMap().size());
        
        transition = new Transition("test", new Location("A"), "channel", new Location("B"), 10.0f);
        expected = new TransitionPrimitive[] {
                TransitionPrimitive.getMoveComplex(new Location("A"), new Location("B"), "channel"),
        };
        checkPrimitives(Arrays.asList(expected), transition.bestPrimitives);
        assertEquals(0, transition.getLeftRightAgentMap().size());
    }

    private void checkPrimitives(List<TransitionPrimitive> expected, List<TransitionPrimitive> actual) {
        assertEquals(expected.size(), actual.size());

        Set<String> actualStrings = new HashSet<String>();
        for (TransitionPrimitive primitive : actual) {
            actualStrings.add(primitive.toString());
        }
        for (TransitionPrimitive primitive : expected) {
            String primitiveString = primitive.toString();
            if (!actualStrings.contains(primitiveString)) {
                fail("Primitive not found: " + primitiveString + "\nin:" + actualStrings);
            }
        }
    }

    private AgentLink getAgentLink(List<Agent> agents, String linkName) {
        List<Complex> complexes = Utils.getComplexes(agents);
        for (Complex complex : complexes) {
            for (AgentLink agentLink : complex.agentLinks) {
                if (linkName.equals(agentLink.sourceSite.getLinkName())) {
                    return agentLink;
                }
            }
        }
        fail("link not found: " + linkName);
        return null;
    }

    @Test
    public void testCreateTransitionMap() {
        List<Agent> leftAgents = new ArrayList<Agent>();
        List<Agent> rightAgents = new ArrayList<Agent>();
        leftAgents.add(new Agent("agent1", new AgentSite("x", "s", null)));
        rightAgents.add(new Agent("agent1", new AgentSite("x", "t", null)));
        Transition transition = new Transition(null, leftAgents, null, rightAgents, 0.1f);

        transition.bestPrimitives = null;
        leftAgents.clear();
        rightAgents.clear();
        leftAgents.add(new Agent("agent1", new AgentSite("x", "s", null)));
        leftAgents.add(new Agent("agent2"));
        rightAgents.add(new Agent("agent1", new AgentSite("x", "t", null)));
        rightAgents.add(new Agent("agent3", new AgentSite("x", "t", null)));
        Utils.getComplexes(leftAgents);
        Utils.getComplexes(rightAgents);
        transition.createTransitionMap(leftAgents, rightAgents);

        transition.bestPrimitives = null;
        leftAgents.clear();
        rightAgents.clear();
        leftAgents.add(new Agent("agent1", new AgentSite("x", "s", "1")));
        leftAgents.add(new Agent("agent2", new AgentSite("y", "t", "1")));
        rightAgents.add(new Agent("agent1", new AgentSite("x", "s", null)));
        rightAgents.add(new Agent("agent2", new AgentSite("y", "t", null)));
        Utils.getComplexes(leftAgents);
        Utils.getComplexes(rightAgents);
        transition.createTransitionMap(leftAgents, rightAgents);

        transition.bestPrimitives = null;
        leftAgents.clear();
        rightAgents.clear();
        leftAgents.add(new Agent("agent1", new AgentSite("x", "s", "1"), new AgentSite("y", "t", "1")));
        rightAgents.add(new Agent("agent1", new AgentSite("x", "s", null), new AgentSite("y", "t", null)));
        Utils.getComplexes(leftAgents);
        Utils.getComplexes(rightAgents);
        transition.createTransitionMap(leftAgents, rightAgents);

        transition.bestPrimitives = null;
        leftAgents.clear();
        rightAgents.clear();
        leftAgents.add(new Agent("agent1", new AgentSite("x", "s", null)));
        leftAgents.add(new Agent("agent2", new AgentSite("y", "t", null)));
        rightAgents.add(new Agent("agent1", new AgentSite("x", "s", "1")));
        rightAgents.add(new Agent("agent2", new AgentSite("y", "t", "1")));
        Utils.getComplexes(leftAgents);
        Utils.getComplexes(rightAgents);
        transition.createTransitionMap(leftAgents, rightAgents);

        transition.bestPrimitives = null;
        leftAgents.clear();
        rightAgents.clear();
        leftAgents.add(new Agent("agent1", new AgentSite("x", "s", null)));
        rightAgents.add(new Agent("agent1", new AgentSite("x", "s", "1")));
        rightAgents.add(new Agent("agent2", new AgentSite("y", "t", "1")));
        Utils.getComplexes(leftAgents);
        Utils.getComplexes(rightAgents);
        transition.createTransitionMap(leftAgents, rightAgents);
    }

    @Test
    public void testCreateTransitionMap_largerCase() {
        List<Agent> leftAgents = Arrays.asList(new Agent[] {
                new Agent("DNA", new AgentSite("downstream", null, "2"), new AgentSite("type", "BBaR0051p3", null), new AgentSite("upstream", null,
                        "3"), new AgentSite("binding", null, null)),
                new Agent("DNA", new AgentSite("downstream", null, "3"), new AgentSite("type", "BBaR0051p2", null),
                        new AgentSite("upstream", null, "4"), new AgentSite("binding", null, null)),
                new Agent("DNA", new AgentSite("downstream", null, "4"), new AgentSite("type", "BBaR0051p1", null), new AgentSite("binding", null, "1")),
                new Agent("DNA", new AgentSite("downstream", null, "6"), new AgentSite("type", "BBaR0051p4", null),
                        new AgentSite("upstream", null, "2"), new AgentSite("binding", null, null)),
                new Agent("DNA", new AgentSite("upstream", null, "6"), new AgentSite("binding", null, null)),

                new Agent("RNA", new AgentSite("downstream", null, "5")),
                new Agent("RNAP", new AgentSite("dna", null, "1"), new AgentSite("rna", null, "5")) 
                });

        List<Agent> rightAgents = Arrays.asList(new Agent[] {
                new Agent("DNA", new AgentSite("downstream", null, "3"), new AgentSite("type", "BBaR0051p3", null), new AgentSite("upstream", null, "5"),
                        new AgentSite("binding", null, null)),
                new Agent("DNA", new AgentSite("downstream", null, "4"), new AgentSite("type", "BBaR0051p1", null), new AgentSite("binding", null, null)),
                new Agent("DNA", new AgentSite("downstream", null, "5"), new AgentSite("type", "BBaR0051p2", null), new AgentSite("upstream", null, "4"),
                        new AgentSite("binding", null, null)),
                new Agent("DNA", new AgentSite("downstream", null, "7"), new AgentSite("type", "BBaR0051p4", null), new AgentSite("upstream", null, "3"),
                        new AgentSite("binding", null, null)),
                new Agent("DNA", new AgentSite("upstream", null, "7"), new AgentSite("binding", null, "1")),

                new Agent("RNA", new AgentSite("downstream", null, "2")),
                new Agent("RNA", new AgentSite("downstream", null, "6"), new AgentSite("type~BBaR0051", null, null), new AgentSite("upstream", null, "2"),
                        new AgentSite("binding", null, null)), 
                new Agent("RNAP", new AgentSite("dna", null, "1"), new AgentSite("rna", null, "6")) 
                });

        Transition transition = new Transition(null, leftAgents, null, rightAgents, 0.1f);
        
        transition.bestPrimitives = null;
        transition.createTransitionMap(leftAgents, rightAgents);
    }
    @Test
    public void testGetApplicationCount() {
        List<Compartment> compartments = getList(new Compartment("A"), new Compartment("B"), new Compartment("C"));
        Channel channel = new Channel("channel");
        channel.addChannelComponent(null, getList(new Location("A")), getList(new Location("B")));
        channel.addChannelComponent(null, getList(new Location("A")), getList(new Location("C")));
        channel.addChannelComponent(null, getList(new Location("B")), getList(new Location("C")));
        List<Channel> channels = getList(channel);

        // Test compartments - no source constraint - no target constraint
        Agent leftTemplateAgent = new Agent("DNA"); 
        Agent rightTemplateAgent = new Agent("DNA");
        Transition transition = new Transition("test", Utils.getList(leftTemplateAgent), "channel", Utils.getList(rightTemplateAgent), 10.0f);
        
        List<ComplexMapping> complexMappings = getTestComplexMappings(new Agent("DNA", new Location("A")), leftTemplateAgent);
        assertEquals(2, transition.getApplicationCount(complexMappings, channels, compartments));
        
        complexMappings = getTestComplexMappings(new Agent("DNA", new Location("B")), leftTemplateAgent);
        assertEquals(1, transition.getApplicationCount(complexMappings, channels, compartments));
       
        complexMappings = getTestComplexMappings(new Agent("DNA", new Location("C")), leftTemplateAgent);
        assertEquals(0, transition.getApplicationCount(complexMappings, channels, compartments));

        // Test compartments - no source constraint - target constraint
        leftTemplateAgent = new Agent("DNA"); 
        rightTemplateAgent = new Agent("DNA", new Location("B"));
        transition = new Transition("test", Utils.getList(leftTemplateAgent), "channel", Utils.getList(rightTemplateAgent), 10.0f);
        
        complexMappings = getTestComplexMappings(new Agent("DNA", new Location("A")), leftTemplateAgent);
        assertEquals(1, transition.getApplicationCount(complexMappings, channels, compartments));
        
        complexMappings = getTestComplexMappings(new Agent("DNA", new Location("B")), leftTemplateAgent);
        assertEquals(0, transition.getApplicationCount(complexMappings, channels, compartments));
       
        complexMappings = getTestComplexMappings(new Agent("DNA", new Location("C")), leftTemplateAgent);
        assertEquals(0, transition.getApplicationCount(complexMappings, channels, compartments));

        // Test compartments - source constraint - no target constraint
        leftTemplateAgent = new Agent("DNA", new Location("B")); 
        rightTemplateAgent = new Agent("DNA");
        transition = new Transition("test", Utils.getList(leftTemplateAgent), "channel", Utils.getList(rightTemplateAgent), 10.0f);
        
        complexMappings = getTestComplexMappings(new Agent("DNA", new Location("A")), leftTemplateAgent);
        assertEquals(0, transition.getApplicationCount(complexMappings, channels, compartments));
        
        complexMappings = getTestComplexMappings(new Agent("DNA", new Location("B")), leftTemplateAgent);
        assertEquals(1, transition.getApplicationCount(complexMappings, channels, compartments));
       
        complexMappings = getTestComplexMappings(new Agent("DNA", new Location("C")), leftTemplateAgent);
        assertEquals(0, transition.getApplicationCount(complexMappings, channels, compartments));

        // Test compartments - source constraint - target constraint
        leftTemplateAgent = new Agent("DNA", new Location("C")); 
        rightTemplateAgent = new Agent("DNA", new Location("B"));
        transition = new Transition("test", Utils.getList(leftTemplateAgent), "channel", Utils.getList(rightTemplateAgent), 10.0f);
        
        complexMappings = getTestComplexMappings(new Agent("DNA", new Location("A")), leftTemplateAgent);
        assertEquals(0, transition.getApplicationCount(complexMappings, channels, compartments));
        
        complexMappings = getTestComplexMappings(new Agent("DNA", new Location("B")), leftTemplateAgent);
        assertEquals(0, transition.getApplicationCount(complexMappings, channels, compartments));
       
        complexMappings = getTestComplexMappings(new Agent("DNA", new Location("C")), leftTemplateAgent);
        assertEquals(0, transition.getApplicationCount(complexMappings, channels, compartments));

        
        // Test voxels
        compartments = getList(new Compartment("A", 3));
        channels = getList(new Channel("channel", new Location("A", INDEX_X), new Location("A", INDEX_X_PLUS_1)));

        leftTemplateAgent = new Agent("DNA"); 
        rightTemplateAgent = new Agent("DNA");
        transition = new Transition("test", Utils.getList(leftTemplateAgent), "channel", Utils.getList(rightTemplateAgent), 10.0f);
        
        complexMappings = getTestComplexMappings(new Agent("DNA", new Location("A", INDEX_0)), leftTemplateAgent);
        assertEquals(1, transition.getApplicationCount(complexMappings, channels, compartments));
        
        complexMappings = getTestComplexMappings(new Agent("DNA", new Location("A", INDEX_1)), leftTemplateAgent);
        assertEquals(1, transition.getApplicationCount(complexMappings, channels, compartments));
       
        complexMappings = getTestComplexMappings(new Agent("DNA", new Location("A", INDEX_2)), leftTemplateAgent);
        assertEquals(0, transition.getApplicationCount(complexMappings, channels, compartments));
        
        // TODO test predefined channel types

    }
    
    private List<ComplexMapping> getTestComplexMappings(Agent realAgent, Agent templateAgent) {
        Complex realComplex = new Complex(realAgent);
        Map<Agent, Agent> mapping = new HashMap<Agent, Agent>();
        mapping.put(templateAgent, realAgent);
        return getList(new ComplexMapping(templateAgent.getComplex(), realComplex, mapping));
    }
    
    @Test
    public void testCreatePrimitivesDeleteAgents() {
        List<Agent> leftAgents = getList(
                new Agent("A", new AgentSite("s1", null, "_")), 
                new Agent("B", new AgentSite("s5", null, "_")), 
                new Agent("C", new AgentSite("s2", null, "?"), new AgentSite("s3", null, "1")),
                new Agent("D", new AgentSite("s4", null, "1")));
        getComplexes(leftAgents);
        int[] indexMapLeftRight = new int[] { DELETED, 1, DELETED, 3  };
        List<TransitionPrimitive> primitives = new ArrayList<TransitionPrimitive>();
        Set<AgentLink> deletedLinks = new HashSet<AgentLink>();
        Set<Agent> deletedAgents = new HashSet<Agent>();

        Transition transition = new Transition(null, getList(new Agent("a1")), null, getList(new Agent("b1")), 0.1f);

        transition.createPrimitivesDeleteAgents(leftAgents, indexMapLeftRight, primitives, deletedLinks, deletedAgents);
        
        checkSetByString("[DELETE_AGENT(A(s1!_)), DELETE_AGENT(C(s2?,s3!1))]", primitives);
        checkSetByString("[A(s1!_), C(s2?,s3!1)]", deletedAgents);
        checkSetByString("[s1!_->occupied, s2?->any, s3!1->s4!1]", deletedLinks);
    }
    
    @Test
    public void testCreatePrimitivesMoveAgents() {
        // Agent1(d!1:domainLink),Agent2(d!1) ->:diffusion Agent1(d!1:domainLink),Agent2(d!1) @ 1.0
        List<Agent> leftAgents = getList(
                new Agent("Agent1", new AgentSite("d", null, "1", "domainLink")),
                new Agent("Agent2", new AgentSite("d", null, "1")));
        
        List<Agent> rightAgents = getList(
                new Agent("Agent1", new AgentSite("d", null, "1", "domainLink")),
                new Agent("Agent2", new AgentSite("d", null, "1")));
        
        getComplexes(leftAgents);
        int[] indexMapLeftRight = new int[] { 0, 1 };
        List<TransitionPrimitive> primitives = new ArrayList<TransitionPrimitive>();

        Transition transition = new Transition(null, getList(new Agent("a1")), "diffusion", getList(new Agent("b1")), 0.1f);

        transition.createPrimitivesMoveAgents(leftAgents, rightAgents, indexMapLeftRight, primitives);
        
        checkSetByString("[MOVE_AGENTS([Agent1(d!1:domainLink), Agent2(d!1)], [##NOT LOCATED##, ##NOT LOCATED##], diffusion)]", primitives);
        
        // Agent1:domain1(d!1:domainLink),Agent2(d!1) ->:diffusion Agent1:domain1(d!1:domainLink),Agent2(d!1) @ 1.0
        Location domain1Location = new Location("domain1");
        leftAgents = getList(
                new Agent("Agent1", domain1Location, new AgentSite("d", null, "1", "domainLink")),
                new Agent("Agent2", new AgentSite("d", null, "1")));
        
        rightAgents = getList(
                new Agent("Agent1", domain1Location, new AgentSite("d", null, "1", "domainLink")),
                new Agent("Agent2", new AgentSite("d", null, "1")));
        
        primitives.clear();
        transition.createPrimitivesMoveAgents(leftAgents, rightAgents, indexMapLeftRight, primitives);
        
        checkSetByString("[MOVE_AGENTS([Agent1:domain1(d!1:domainLink), Agent2(d!1)], [domain1, ##NOT LOCATED##], diffusion)]", primitives);
    }
    
    private <T> void checkSetByString(String expected, Collection<T> actual) {
        List<T> actualList = new ArrayList<T>(actual);
        Collections.sort(actualList, new Comparator<T>() {
            public int compare(T o1, T o2) {
                return o1.toString().compareTo(o2.toString());
            }
        });
        assertEquals(expected, actualList.toString());
    }
    
    @Test
    public void testCreatePrimitivesDeleteLinks() {
        List<Agent> leftAgents = getList(
                new Agent("A", new AgentSite("s1", null, "_"), new AgentSite("s2", null, "2")), 
                new Agent("B", new AgentSite("s3", null, "_"), new AgentSite("s4", null, "2")), 
                new Agent("C", new AgentSite("s5", null, "?"), new AgentSite("s6", null, "1")),
                new Agent("D", new AgentSite("s7", null, "1")),
                new Agent("E", new AgentSite("s8", null, "?"))
                );
        List<Agent> rightAgents = getList(
                new Agent("A", new AgentSite("s2", null, "2")), 
                new Agent("B", new AgentSite("s3", null, "_")), 
                new Agent("C", new AgentSite("s5", null, "?"), new AgentSite("s6", null, "2")),
                new Agent("D"));
        getComplexes(leftAgents);
        getComplexes(rightAgents);
        int[] indexMapLeftRight = new int[] { 0, 1, 2, 3, DELETED };
        List<TransitionPrimitive> primitives = new ArrayList<TransitionPrimitive>();
        Set<AgentLink> deletedLinks = new HashSet<AgentLink>();

        Transition transition = new Transition(null, getList(new Agent("a1")), null, getList(new Agent("b1")), 0.1f);
        transition.createPrimitivesDeleteLinks(leftAgents, rightAgents, indexMapLeftRight, primitives, deletedLinks);
        
        checkSetByString("[DELETE_LINK(s1!_->occupied), DELETE_LINK(s2!2->s4!2), DELETE_LINK(s6!1->s7!1)]", primitives);
        checkSetByString("[s1!_->occupied, s2!2->s4!2, s6!1->s7!1]", deletedLinks);
        
        // Check channels
        leftAgents = getList(
                new Agent("A", new AgentSite("s1", null, "_"), new AgentSite("s2", null, "2", "ch1")), 
                new Agent("B", new AgentSite("s3", null, "_", "ch2"), new AgentSite("s4", null, "2")), 
                new Agent("C", new AgentSite("s5", null, "?"), new AgentSite("s6", null, "1")),
                new Agent("D", new AgentSite("s7", null, "1")),
                new Agent("E", new AgentSite("s8", null, "?"))
                );
        rightAgents = getList(
                new Agent("A", new AgentSite("s1", null, "_", "ch3"), new AgentSite("s2", null, "2", "ch4")), 
                new Agent("B", new AgentSite("s3", null, "_", "ch2"), new AgentSite("s4", null, "2")), 
                new Agent("C", new AgentSite("s5", null, "?"), new AgentSite("s6", null, "1")),
                new Agent("D", new AgentSite("s7", null, "1"))
                );

        getComplexes(leftAgents);
        getComplexes(rightAgents);
        indexMapLeftRight = new int[] { 0, 1, 2, 3, DELETED };
        primitives.clear();
        deletedLinks.clear();

        transition = new Transition(null, getList(new Agent("a1")), null, getList(new Agent("b1")), 0.1f);
        transition.createPrimitivesDeleteLinks(leftAgents, rightAgents, indexMapLeftRight, primitives, deletedLinks);
        
        checkSetByString("[DELETE_LINK(s1!_->occupied), DELETE_LINK(s2!2:ch1->s4!2)]", primitives);
        checkSetByString("[s1!_->occupied, s2!2:ch1->s4!2]", deletedLinks);
        
    }

    @Test
    public void testCreatePrimitivesAddLinks() {
        List<Agent> leftAgents = getList(
                new Agent("A", new AgentSite("s1", null, null), new AgentSite("s2", null, "2")), 
                new Agent("B", new AgentSite("s3", null, "_"), new AgentSite("s4", null, null)), 
                new Agent("C", new AgentSite("s5", null, "?"), new AgentSite("s6", null, "2")),
                new Agent("D", new AgentSite("s7", null, null), new AgentSite("s8", null, null)));
        List<Agent> rightAgents = getList(
                new Agent("A", new AgentSite("s1", null, null), new AgentSite("s2", null, "2")), 
                new Agent("B", new AgentSite("s3", null, "_"), new AgentSite("s4", null, "2")), 
                new Agent("C", new AgentSite("s5", null, "?"), new AgentSite("s6", null, "1")),
                new Agent("D", new AgentSite("s7", null, "1"), new AgentSite("s8", null, "4")),
                new Agent("E", new AgentSite("s9", null, "?"), new AgentSite("s10", null, "3"), new AgentSite("s11", null, "4")),
                new Agent("F", new AgentSite("s12", null, null), new AgentSite("s13", null, "3"))
                );
        getComplexes(leftAgents);
        getComplexes(rightAgents);
        int[] indexMapRightLeft = new int[] { 0, 1, 2, 3, UNMAPPED, UNMAPPED };
        List<TransitionPrimitive> primitives = new ArrayList<TransitionPrimitive>();

        Transition transition = new Transition(null, getList(new Agent("a1")), null, getList(new Agent("b1")), 0.1f);
        transition.createPrimitivesAddLinks(leftAgents, rightAgents, indexMapRightLeft, primitives);
        
        checkSetByString("[CREATE_LINK(A(s1,s2!2) [s2!2] -> B(s3!_,s4) [s4]), " +
        		"CREATE_LINK(C(s5?,s6!2) [s6!2] -> D(s7,s8) [s7]), " +
        		"CREATE_LINK(D(s7,s8) [s8] -> E(s10!3,s11!4,s9?) [s11!4])]", primitives); //TODO UNMAPPED - UNMAPPED links
        
        // Check channels
        leftAgents = getList(
                new Agent("A", new AgentSite("s1", null, "_"), new AgentSite("s2", null, "2", "ch1")), 
                new Agent("B", new AgentSite("s3", null, "_", "ch2"), new AgentSite("s4", null, "2")), 
                new Agent("C", new AgentSite("s5", null, "?"), new AgentSite("s6", null, "1")),
                new Agent("D", new AgentSite("s7", null, "1")),
                new Agent("E", new AgentSite("s8", null, "?"))
                );
        rightAgents = getList(
                new Agent("A", new AgentSite("s1", null, "_"), new AgentSite("s2", null, "2", "ch4")), 
                new Agent("B", new AgentSite("s3", null, "_", "ch2"), new AgentSite("s4", null, "2")), 
                new Agent("C", new AgentSite("s5", null, "?"), new AgentSite("s6", null, "1")),
                new Agent("D", new AgentSite("s7", null, "1", "ch3"))
                );

        getComplexes(leftAgents);
        getComplexes(rightAgents);
        indexMapRightLeft = new int[] { 0, 1, 2, 3 };
        primitives.clear();

        transition = new Transition(null, getList(new Agent("a1")), null, getList(new Agent("b1")), 0.1f);
        transition.createPrimitivesAddLinks(leftAgents, rightAgents, indexMapRightLeft, primitives);
        
        checkSetByString("[CREATE_LINK(A(s1!_,s2!2:ch1) [s2!2:ch1] -> B(s3!_:ch2,s4!2) [s4!2] ch4), " +
        		"CREATE_LINK(C(s5?,s6!1) [s6!1] -> D(s7!1) [s7!1] ch3)]", primitives);
    }
}
