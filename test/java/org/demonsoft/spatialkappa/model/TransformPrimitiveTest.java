package org.demonsoft.spatialkappa.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.demonsoft.spatialkappa.model.Agent;
import org.demonsoft.spatialkappa.model.AgentLink;
import org.demonsoft.spatialkappa.model.AgentSite;
import org.demonsoft.spatialkappa.model.Complex;
import org.demonsoft.spatialkappa.model.TransformPrimitive;
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

        assertEquals("CHANGE_STATE(source(), sourceSite~s!1, u)", TransformPrimitive.getChangeState(sourceAgent, sourceSite, state).toString());
        assertEquals("CREATE_AGENT(source(), target())", TransformPrimitive.getCreateAgent(sourceAgent, targetAgent).toString());
        assertEquals("CREATE_COMPLEX([source()])", TransformPrimitive.getCreateComplex(complex).toString());
        assertEquals("CREATE_LINK(null [sourceSite~s!1] <-> null [targetSite~t!1])", TransformPrimitive.getCreateLink(sourceSite, targetSite).toString());
        assertEquals("DELETE_AGENT(source())", TransformPrimitive.getDeleteAgent(sourceAgent).toString());
        assertEquals("DELETE_LINK(sourceSite~s!1<->targetSite~t!1)", TransformPrimitive.getDeleteLink(agentLink).toString());
        assertEquals("MERGE_COMPLEXES(source(), target())", TransformPrimitive.getMergeComplexes(sourceAgent, targetAgent).toString());
    }

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

    private void checkApplyDeleteAgent(Agent templateAgent, Map<Agent, Agent> transformMap, String[] expectedComplexes) {
        checkApplyPrimitive(TransformPrimitive.getDeleteAgent(templateAgent), transformMap, expectedComplexes);
    }

    private void checkApplyChangeState(Agent templateAgent, AgentSite templateSite, String state, Map<Agent, Agent> transformMap, String[] expectedComplexes) {
        checkApplyPrimitive(TransformPrimitive.getChangeState(templateAgent, templateSite, state), transformMap, expectedComplexes);
    }

    private void checkApplyPrimitive(TransformPrimitive primitive, Map<Agent, Agent> transformMap, String[] expectedComplexes) {
        List<Complex> targetComplexes = new ArrayList<Complex>();
        for (Agent agent : transformMap.values()) {
            if (!targetComplexes.contains(agent.getComplex())) {
                targetComplexes.add(agent.getComplex());
            }
        }

        primitive.apply(transformMap, targetComplexes);

        assertEquals(expectedComplexes.length, targetComplexes.size());
        List<String> expected = Arrays.asList(expectedComplexes);
        for (Complex actual : targetComplexes) {
            assertTrue("Missing complex: " + actual.toString(), expected.contains(actual.toString()));
            for (Agent agent : actual.agents) {
                assertSame(actual, agent.getComplex());
            }
        }
    }

}
