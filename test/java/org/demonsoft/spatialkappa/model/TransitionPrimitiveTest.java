package org.demonsoft.spatialkappa.model;

import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_0;
import static org.demonsoft.spatialkappa.model.CellIndexExpression.INDEX_1;
import static org.demonsoft.spatialkappa.model.Location.NOT_LOCATED;
import static org.demonsoft.spatialkappa.model.Utils.getComplexes;
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


public class TransitionPrimitiveTest {

    @Test
    public void testCreation() {
        Agent sourceAgent = new Agent("source");
        Agent targetAgent = new Agent("target");
        AgentSite sourceSite = new AgentSite("sourceSite", "s", "1");
        AgentSite targetSite = new AgentSite("targetSite", "t", "1");
        Complex complex = new Complex(sourceAgent);
        AgentLink agentLink = new AgentLink(sourceSite, targetSite);
        String state = "u";

        assertEquals("CHANGE_STATE(source, sourceSite~s!1, u)", TransitionPrimitive.getChangeState(sourceAgent, sourceSite, state).toString());
        assertEquals("CREATE_AGENT(source, target)", TransitionPrimitive.getCreateAgent(sourceAgent, targetAgent).toString());
        assertEquals("CREATE_COMPLEX([source])", TransitionPrimitive.getCreateComplex(complex).toString());
        assertEquals("CREATE_LINK(null [sourceSite~s!1] -> null [targetSite~t!1])", TransitionPrimitive.getCreateLink(sourceSite, targetSite, null).toString());
        assertEquals("DELETE_AGENT(source)", TransitionPrimitive.getDeleteAgent(sourceAgent).toString());
        assertEquals("DELETE_LINK(sourceSite~s!1->targetSite~t!1)", TransitionPrimitive.getDeleteLink(agentLink).toString());
        assertEquals("MERGE_COMPLEXES(source, target)", TransitionPrimitive.getMergeComplexes(sourceAgent, targetAgent).toString());
        assertEquals("MOVE_COMPLEX(source, target, channel)", TransitionPrimitive.getMoveComplex(new Location("source"), new Location("target"), "channel").toString());
        assertEquals("MOVE_AGENTS([source], [target], channel)", TransitionPrimitive.getMoveAgents(getList(sourceAgent), getList(new Location("target")), "channel").toString());
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
    public void testApply_MoveAgents_singleAgentChannel() {
        List<Compartment> compartments = getList(new Compartment("source"), new Compartment("target"));
        List<Channel> channels = getList(new Channel("channel", new Location("source"), new Location("target")));

        // Single agent - source location specified - no target constraint
        Agent realSourceAgent = new Agent("agent", new Location("source"));
        @SuppressWarnings("unused")
        Complex realSourceComplex = new Complex(realSourceAgent);

        Agent leftAgent = new Agent("agent", new Location("source"));
        checkApplyMoveAgents(leftAgent, NOT_LOCATED, "channel",  realSourceAgent, channels, compartments,
                new String[] { "[agent:target]" });

        // Single agent - source location specified - target constraint
        realSourceAgent = new Agent("agent", new Location("source"));
        realSourceComplex = new Complex(realSourceAgent);
        checkApplyMoveAgents(leftAgent, new Location("target"), "channel",  realSourceAgent, channels, compartments,
                new String[] { "[agent:target]" });
        
        // Single agent - source location inferred from channel - no target constraint
        realSourceAgent = new Agent("agent", new Location("source"));
        realSourceComplex = new Complex(realSourceAgent);
        leftAgent = new Agent("agent");
        checkApplyMoveAgents(leftAgent, NOT_LOCATED, "channel",  realSourceAgent, channels, compartments,
                new String[] { "[agent:target]" });

        // Single agent - source location inferred from channel - target constraint
        realSourceAgent = new Agent("agent", new Location("source"));
        realSourceComplex = new Complex(realSourceAgent);
        checkApplyMoveAgents(leftAgent, new Location("target"), "channel",  realSourceAgent, channels, compartments,
                new String[] { "[agent:target]" });

        
        // Co-located complex - source location inferred from channel - no target constraint
        realSourceAgent = new Agent("agent", new Location("source"), new AgentSite("a", null, "1"));
        realSourceComplex = new Complex(
                realSourceAgent,
                new Agent("other", new Location("source"), new AgentSite("b", null, "1")));

        checkApplyMoveAgents(leftAgent, NOT_LOCATED, "channel",  realSourceAgent, channels, compartments,
                new String[] { "[agent:target(a!1), other:target(b!1)]" });

        
//        // Mixed location complex - should do nothing // TODO 
//        realSourceAgent = new Agent("agent", new Location("source"), new AgentSite("a", null, "1"));
//        realSourceComplex = new Complex(
//                new Agent("agent", new Location("source"), new AgentSite("a", null, "1")),
//                new Agent("other", new Location("source"), new AgentSite("b", null, "1")));
//
//        checkApplyMoveAgents(leftAgent, NOT_LOCATED, "channel",  realSourceAgent, channels, compartments,
//                new String[] { "[agent:target(a!1), other:target(b!1)]" });

        compartments = getList(new Compartment("cytosol", 2));
        channels = getList(new Channel("intra", new Location("cytosol", INDEX_0), new Location("cytosol", INDEX_1)));

        // Multiple unlinked agents - source location specified - no target constraint
        List<Agent> realSourceAgents = getList(new Agent("A", new Location("cytosol", INDEX_0), new AgentSite("s", null, "1")),
                new Agent("B", new Location("cytosol", INDEX_0), new AgentSite("s", null, "1")));
        getComplexes(realSourceAgents);

        List<Agent> leftAgents = getList(new Agent("A", new AgentSite("s", null, "1")), new Agent("B", new AgentSite("s", null, "1")));
        getComplexes(leftAgents);
        checkApplyMoveAgents(leftAgents, getList(NOT_LOCATED, NOT_LOCATED), "intra",  realSourceAgents, channels, compartments,
                new String[] { "[A:cytosol[1](s!1), B:cytosol[1](s!1)]" });

    }


    @Test
    public void testApply_MoveAgents_multipleAgentChannel() {
        List<Compartment> compartments = getList(new Compartment("source1"), new Compartment("source2"), new Compartment("target1"), new Compartment("target2"));
        Channel channel = new Channel("channel");
        channel.addChannelComponent(null,
                getList(new Location("source1"), new Location("source2")),
                getList(new Location("target1"), new Location("target2"))               
        );
        List<Channel> channels = getList(channel);

        // Unconnected agents - source location specified - no target constraints
        List<Agent> realSourceAgents = getList(new Agent("agent1", new Location("source1")), new Agent("agent2", new Location("source2")));
        getComplexes(realSourceAgents);

        List<Agent> leftAgents = getList(new Agent("agent1", new Location("source1")), new Agent("agent2", new Location("source2")));
        List<Location> targetConstraints = getList(NOT_LOCATED, NOT_LOCATED);
        getComplexes(leftAgents);
        checkApplyMoveAgents(leftAgents, targetConstraints, "channel",  realSourceAgents, channels, compartments,
                new String[] { "[agent1:target1]", "[agent2:target2]" });

        // TODO test predefined channel types

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
        checkApplyPrimitive(TransitionPrimitive.getMergeComplexes(templateSourceAgent, templateTargetAgent), transformMap, expectedComplexes);
    }

    private void checkApplyMoveComplex(Location templateSourceLocation, Location templateTargetLocation, String channelName, Complex realComplex, List<Channel> channels, List<Compartment> compartments, String[] expectedComplexes) {
        checkApplyPrimitive(TransitionPrimitive.getMoveComplex(templateSourceLocation, templateTargetLocation, channelName), 
                null, realComplex, channels, compartments, expectedComplexes);
    }

    private void checkApplyMoveAgents(Agent templateSourceAgent, Location templateTargetLocation, String channelName, Agent realAgent, List<Channel> channels, List<Compartment> compartments, String[] expectedComplexes) {
        Map<Agent, Agent> transformMap = new HashMap<Agent, Agent>();
        transformMap.put(templateSourceAgent, realAgent);
        checkApplyPrimitive(TransitionPrimitive.getMoveAgents(getList(templateSourceAgent), getList(templateTargetLocation), channelName), 
                transformMap, null, channels, compartments, expectedComplexes);
    }
    
    private void checkApplyMoveAgents(List<Agent> templateSourceAgents, List<Location> targetConstraints, String channelName,
            List<Agent> realSourceAgents, List<Channel> channels, List<Compartment> compartments,
            String[] expectedComplexes) {
        Map<Agent, Agent> transformMap = new HashMap<Agent, Agent>();
        for (int index=0; index<templateSourceAgents.size(); index++) {
            transformMap.put(templateSourceAgents.get(index), realSourceAgents.get(index));
        }
        checkApplyPrimitive(TransitionPrimitive.getMoveAgents(templateSourceAgents, targetConstraints, channelName), 
                transformMap, null, channels, compartments, expectedComplexes);
    }



    private void checkApplyDeleteAgent(Agent templateAgent, Map<Agent, Agent> transformMap, String[] expectedComplexes) {
        checkApplyPrimitive(TransitionPrimitive.getDeleteAgent(templateAgent), transformMap, expectedComplexes);
    }

    private void checkApplyChangeState(Agent templateAgent, AgentSite templateSite, String state, Map<Agent, Agent> transformMap, String[] expectedComplexes) {
        checkApplyPrimitive(TransitionPrimitive.getChangeState(templateAgent, templateSite, state), transformMap, expectedComplexes);
    }

    private void checkApplyPrimitive(TransitionPrimitive primitive, Map<Agent, Agent> transformMap, String[] expectedComplexes) {
        checkApplyPrimitive(primitive, transformMap, null, null, null, expectedComplexes);
    }


    private void checkApplyPrimitive(TransitionPrimitive primitive, Map<Agent, Agent> transformMap, Complex realComplex, List<Channel> channels, List<Compartment> compartments, String[] expectedComplexes) {
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
