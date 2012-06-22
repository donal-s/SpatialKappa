package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;
import static org.demonsoft.spatialkappa.model.Utils.getList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;


public class TransformPrimitiveTest {

    @Test
    public void testCreation() {
        Agent sourceAgent = new Agent("source");
        Agent targetAgent = new Agent("target");
        AgentSite sourceSite = new AgentSite("sourceSite", "s", "1");
        AgentSite targetSite = new AgentSite("targetSite", "t", "1");
        Complex complex = new Complex(sourceAgent);
        AgentLink agentLink = new AgentLink(sourceSite, targetSite);
        String state = "u";

        assertEquals("CHANGE_STATE(source, sourceSite~s!1, u)", TransformPrimitive.getChangeState(sourceAgent, sourceSite, state).toString());
        assertEquals("CREATE_AGENT(source, target)", TransformPrimitive.getCreateAgent(sourceAgent, targetAgent).toString());
        assertEquals("CREATE_COMPLEX([source])", TransformPrimitive.getCreateComplex(complex).toString());
        assertEquals("CREATE_LINK(null [sourceSite~s!1] -> null [targetSite~t!1])", TransformPrimitive.getCreateLink(sourceSite, targetSite, null).toString());
        assertEquals("DELETE_AGENT(source)", TransformPrimitive.getDeleteAgent(sourceAgent).toString());
        assertEquals("DELETE_LINK(sourceSite~s!1->targetSite~t!1)", TransformPrimitive.getDeleteLink(agentLink).toString());
        assertEquals("MERGE_COMPLEXES(source, target)", TransformPrimitive.getMergeComplexes(sourceAgent, targetAgent).toString());
        assertEquals("MOVE_COMPLEX(source, target, channel)", TransformPrimitive.getMoveComplex(new Location("source"), new Location("target"), "channel").toString());
    }

    @SuppressWarnings("unused")
    @Test
    public void testApply_MergeComplexes() {

        Agent templateSourceAgent = new Agent("source");
        Agent templateTargetAgent = new Agent("target");
        new Complex(templateSourceAgent);
        new Complex(templateTargetAgent);

        // Test merging distinct linked complexes, removing empty source complex

        Agent realSourceAgent = new Agent("source", new AgentSite("a", null, "1"));
        Agent realTargetAgent = new Agent("target", new AgentSite("c", null, "1"));
        new Complex(new Agent("other1", new AgentSite("b", null, "1")), realSourceAgent);
        new Complex(new Agent("other2", new AgentSite("d", null, "1")), realTargetAgent);

        Map<Agent, Agent> transformMap = new HashMap<Agent, Agent>();
        transformMap.put(templateSourceAgent, realSourceAgent);
        transformMap.put(templateTargetAgent, realTargetAgent);

        checkApplyMergeComplexes(templateSourceAgent, templateTargetAgent, transformMap,
                new String[] { "[other1(b!2), other2(d!1), source(a!2), target(c!1)]" });

        // Test merging same complexes

        realSourceAgent = new Agent("source", new AgentSite("a", null, "2"));
        realTargetAgent = new Agent("target", new AgentSite("c", null, "1"));
        new Complex(new Agent("other1", new AgentSite("b", null, "2")), realSourceAgent, new Agent("other2", new AgentSite("d", null, "1")), realTargetAgent);

        transformMap.clear();
        transformMap.put(templateSourceAgent, realSourceAgent);
        transformMap.put(templateTargetAgent, realTargetAgent);

        checkApplyMergeComplexes(templateSourceAgent, templateTargetAgent, transformMap,
                new String[] { "[other1(b!2), other2(d!1), source(a!2), target(c!1)]" });

    }

    @Test
    public void testApply_MoveComplex_locationOnly() {

        Complex realSourceComplex = new Complex(
                new Agent("agent", new Location("source"), new AgentSite("a", null, "1")),
                new Agent("other", new Location("source"), new AgentSite("b", null, "1")));

        List<Channel> channels = getList(new Channel("channel", new Location("source"), new Location("target")));
        
        List<Compartment> compartments = getList(new Compartment("source"), new Compartment("target"));
        
        checkApplyMoveComplex(new Location("source"), new Location("target"), "channel", realSourceComplex, channels, compartments,
                new String[] { "[agent:target(a!1), other:target(b!1)]" });
    }

    @Test
    public void testApply_MoveAgent() {
        List<Compartment> compartments = getList(new Compartment("source"), new Compartment("target"));
        List<Channel> channels = getList(new Channel("channel", new Location("source"), new Location("target")));

        // Single agent - source location specified - no target constraint
        Agent realSourceAgent = new Agent("agent", new Location("source"));
        @SuppressWarnings("unused")
        Complex realSourceComplex = new Complex(realSourceAgent);

        Agent leftAgent = new Agent("agent", new Location("source"));
        checkApplyMoveAgent(leftAgent, NOT_LOCATED, "channel",  realSourceAgent, channels, compartments,
                new String[] { "[agent:target]" });

        // Single agent - source location specified - target constraint
        realSourceAgent = new Agent("agent", new Location("source"));
        realSourceComplex = new Complex(realSourceAgent);
        checkApplyMoveAgent(leftAgent, new Location("target"), "channel",  realSourceAgent, channels, compartments,
                new String[] { "[agent:target]" });
        
        // Single agent - source location inferred from channel - no target constraint
        realSourceAgent = new Agent("agent", new Location("source"));
        realSourceComplex = new Complex(realSourceAgent);
        leftAgent = new Agent("agent");
        checkApplyMoveAgent(leftAgent, NOT_LOCATED, "channel",  realSourceAgent, channels, compartments,
                new String[] { "[agent:target]" });

        // Single agent - source location inferred from channel - target constraint
        realSourceAgent = new Agent("agent", new Location("source"));
        realSourceComplex = new Complex(realSourceAgent);
        checkApplyMoveAgent(leftAgent, new Location("target"), "channel",  realSourceAgent, channels, compartments,
                new String[] { "[agent:target]" });

        
        // Co-located complex - source location inferred from channel - no target constraint
        realSourceAgent = new Agent("agent", new Location("source"), new AgentSite("a", null, "1"));
        realSourceComplex = new Complex(
                realSourceAgent,
                new Agent("other", new Location("source"), new AgentSite("b", null, "1")));

        checkApplyMoveAgent(leftAgent, NOT_LOCATED, "channel",  realSourceAgent, channels, compartments,
                new String[] { "[agent:target(a!1), other:target(b!1)]" });

        
        // Mixed location complex - should do nothing
//        realSourceAgent = new Agent("agent", new Location("source"), new AgentSite("a", null, "1"));
//        realSourceComplex = new Complex(
//                new Agent("agent", new Location("source"), new AgentSite("a", null, "1")),
//                new Agent("other", new Location("source"), new AgentSite("b", null, "1")));
//
//        checkApplyMoveAgent(leftAgent, NOT_LOCATED, "channel",  realSourceAgent, channels, compartments,
//                new String[] { "[agent:target(a!1), other:target(b!1)]" });

    }

    @SuppressWarnings("unused")
    @Test
    public void testApply_DeleteAgent() {

        Agent templateAgent = new Agent("target");
        new Complex(templateAgent);

        // Test removing lone agent, removing empty source complex

        Agent realAgent = new Agent("source", new AgentSite("a", null, null));
        new Complex(realAgent);

        Map<Agent, Agent> transformMap = new HashMap<Agent, Agent>();
        transformMap.put(templateAgent, realAgent);

        checkApplyDeleteAgent(templateAgent, transformMap, new String[] {});

        // Test removing terminal agent

        realAgent = new Agent("source", new AgentSite("a", null, "1"));
        new Complex(new Agent("other1", new AgentSite("b", null, "1"), new AgentSite("c", null, "2")), new Agent("other2", new AgentSite("y", null, "2")),
                realAgent);

        transformMap.clear();
        transformMap.put(templateAgent, realAgent);

        checkApplyDeleteAgent(templateAgent, transformMap, new String[] { "[other1(b,c!2), other2(y!2)]" });

        // Test removing non-terminal agent, does not split complex

        realAgent = new Agent("source", new AgentSite("a", null, "1"), new AgentSite("b", null, "2"));
        new Complex(new Agent("other1", new AgentSite("x", null, "1")), new Agent("other2", new AgentSite("y", null, "2")), realAgent);

        transformMap.clear();
        transformMap.put(templateAgent, realAgent);

        checkApplyDeleteAgent(templateAgent, transformMap, new String[] { "[other1(x), other2(y)]" });
    }

    @SuppressWarnings("unused")
    @Test
    public void testApply_ChangeState() {

        Agent templateAgent = new Agent("target", new AgentSite("loc_index", "0", null));
        new Complex(templateAgent);

        // Test removing lone agent, removing empty source complex

        Agent realAgent = new Agent("A", new AgentSite("state", "red", null), new AgentSite("loc", "cytosol", null), new AgentSite("loc_index", "0", null));
        new Complex(realAgent);

        Map<Agent, Agent> transformMap = new HashMap<Agent, Agent>();
        transformMap.put(templateAgent, realAgent);

        checkApplyChangeState(templateAgent, new AgentSite("loc_index", null, null), "1", transformMap, new String[] { "[A(loc~cytosol,loc_index~1,state~red)]" });
    }

    private void checkApplyMergeComplexes(Agent templateSourceAgent, Agent templateTargetAgent, Map<Agent, Agent> transformMap, String[] expectedComplexes) {
        checkApplyPrimitive(TransformPrimitive.getMergeComplexes(templateSourceAgent, templateTargetAgent), transformMap, expectedComplexes);
    }

    private void checkApplyMoveComplex(Location templateSourceLocation, Location templateTargetLocation, String channelName, Complex realComplex, List<Channel> channels, List<Compartment> compartments, String[] expectedComplexes) {
        checkApplyPrimitive(TransformPrimitive.getMoveComplex(templateSourceLocation, templateTargetLocation, channelName), 
                null, realComplex, channels, compartments, expectedComplexes);
    }

    private void checkApplyMoveAgent(Agent templateSourceAgent, Location templateTargetLocation, String channelName, Agent realAgent, List<Channel> channels, List<Compartment> compartments, String[] expectedComplexes) {
        Map<Agent, Agent> transformMap = new HashMap<Agent, Agent>();
        transformMap.put(templateSourceAgent, realAgent);
        checkApplyPrimitive(TransformPrimitive.getMoveAgent(templateSourceAgent, templateTargetLocation, channelName), 
                transformMap, realAgent.getComplex(), channels, compartments, expectedComplexes);
    }

    private void checkApplyDeleteAgent(Agent templateAgent, Map<Agent, Agent> transformMap, String[] expectedComplexes) {
        checkApplyPrimitive(TransformPrimitive.getDeleteAgent(templateAgent), transformMap, expectedComplexes);
    }

    private void checkApplyChangeState(Agent templateAgent, AgentSite templateSite, String state, Map<Agent, Agent> transformMap, String[] expectedComplexes) {
        checkApplyPrimitive(TransformPrimitive.getChangeState(templateAgent, templateSite, state), transformMap, expectedComplexes);
    }

    private void checkApplyPrimitive(TransformPrimitive primitive, Map<Agent, Agent> transformMap, String[] expectedComplexes) {
        checkApplyPrimitive(primitive, transformMap, null, null, null, expectedComplexes);
    }


    private void checkApplyPrimitive(TransformPrimitive primitive, Map<Agent, Agent> transformMap, Complex realComplex, List<Channel> channels, List<Compartment> compartments, String[] expectedComplexes) {
        List<Complex> targetComplexes = new ArrayList<Complex>();
        
        if (transformMap != null) {
            for (Agent agent : transformMap.values()) {
                if (!targetComplexes.contains(agent.getComplex())) {
                    targetComplexes.add(agent.getComplex());
                }
            }
        }
        else {
            targetComplexes.add(realComplex);
        }
        
        primitive.apply(transformMap, targetComplexes, channels, compartments);

        assertEquals(expectedComplexes.length, targetComplexes.size());
        List<String> expected = Arrays.asList(expectedComplexes);
        for (Complex actual : targetComplexes) {
            assertTrue("Missing complex: " + actual.toString() + " in " + expected, expected.contains(actual.toString()));
            for (Agent agent : actual.agents) {
                assertSame(actual, agent.getComplex());
            }
        }
    }

}
