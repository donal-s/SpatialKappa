package org.demonsoft.spatialkappa.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.demonsoft.spatialkappa.model.KappaModel.ModelOnlySimulationState;
import org.demonsoft.spatialkappa.model.VariableExpression.Constant;
import org.junit.Test;

public class TransformTest extends TransitionTest {

    @Test
    public void testTransform() {
        List<Complex> leftComplexes = getComplexes(new Agent("agent1"));
        List<Complex> rightComplexes = getComplexes(new Agent("agent2"));

        try {
            new Transform("label", leftComplexes, rightComplexes, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }

        try {
            new Transform("label", (List<Complex>) null, (List<Complex>) null, new VariableExpression(0.1f));
            fail("lhs and rhs null should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        try {
            new Transform("label", new ArrayList<Complex>(), new ArrayList<Complex>(), new VariableExpression(0.1f));
            fail("lhs and rhs empty should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }

        Transform transform = new Transform("label", leftComplexes, rightComplexes, new VariableExpression(0.1f));
        checkTransform("label", leftComplexes, rightComplexes, 0.1f, false, transform);

        transform = new Transform("label", leftComplexes, rightComplexes, new VariableExpression(8.30269391581363e-07f));
        checkTransform("label", leftComplexes, rightComplexes, 0.000000830269391581363f, false, transform);

        transform = new Transform("label", leftComplexes, rightComplexes, new VariableExpression(Constant.INFINITY));
        checkTransform("label", leftComplexes, rightComplexes, 0f, true, transform);

        // Ensure [inf] is still shown in toString output
        assertEquals("'label' : [agent1] -> [agent2] @ [inf]\n" + 
        		"\tDELETE_AGENT(agent1)\n" + 
        		"\tCREATE_COMPLEX([agent2])\n", transform.toString(true));

        leftComplexes = getComplexes(new Agent("A", new AgentSite("state", "red", null), new AgentSite("loc", "cytosol", null), new AgentSite("loc_index", "0",
                null)));
        rightComplexes = getComplexes(new Agent("A", new AgentSite("state", "red", null), new AgentSite("loc", "cytosol", null), new AgentSite("loc_index",
                "1", null)));

        transform = new Transform("diffusion-red-0", leftComplexes, rightComplexes, new VariableExpression(0.1f));
        checkTransform("diffusion-red-0", leftComplexes, rightComplexes, 0.1f, false, transform);

        leftComplexes = getComplexes(new Agent("RNA", new AgentSite("d", null, "5")), new Agent("RNAP", new AgentSite("dna", null, "1"), new AgentSite("rna",
                null, "5")), new Agent("DNA", new AgentSite("d", null, "4"), new AgentSite("t", "a", null), new AgentSite("b", null, "1")), new Agent("DNA",
                new AgentSite("d", null, "3"), new AgentSite("t", "b", null), new AgentSite("u", null, "4"), new AgentSite("b", null, null)), new Agent("DNA",
                new AgentSite("u", null, "3"), new AgentSite("b", null, null)));
        rightComplexes = getComplexes(new Agent("DNA", new AgentSite("d", null, "4"), new AgentSite("t", "a", null), new AgentSite("b", null, null)),
                new Agent("DNA", new AgentSite("d", null, "5"), new AgentSite("t", "b", null), new AgentSite("u", null, "4"), new AgentSite("b", null, null)),
                new Agent("DNA", new AgentSite("u", null, "5"), new AgentSite("b", null, "1")), new Agent("RNA", new AgentSite("d", null, "2")), new Agent(
                        "RNA", new AgentSite("d", null, "6"), new AgentSite("t", "x", null), new AgentSite("u", null, "2"), new AgentSite("b", null, null)),
                new Agent("RNAP", new AgentSite("dna", null, "1"), new AgentSite("rna", null, "6")));

        transform = new Transform("diffusion-red-0", leftComplexes, rightComplexes, new VariableExpression(0.1f));
        checkTransform("diffusion-red-0", leftComplexes, rightComplexes, 0.1f, false, transform);
        
         // TODO validate against this:
        // A(a!_,b!1,bindings~2),B(a!1) -> A(a!_,b,bindings~1), B(c) @ 'unbind rate'

    }
    
    @Test
    public void testTransform_checkPrimitives() {
        Agent leftAgent1 = new Agent("DNA", new AgentSite("downstream", null, "2"), new AgentSite("type~BBaB0000", null, null), new AgentSite("binding", null, "1")); 
        Agent leftAgent2 = new Agent("DNA", new AgentSite("upstream", null, "2"), new AgentSite("binding", null, null));
        Agent leftAgent3 = new Agent("RNA", new AgentSite("downstream", null, "3"));
        Agent leftAgent4 = new Agent("RNAP", new AgentSite("dna", null, "1"), new AgentSite("rna", null, "3"));
        
        List<Complex> leftComplexes = getComplexes(leftAgent1, leftAgent2, leftAgent3, leftAgent4);
        
        Agent rightAgent1 = new Agent("DNA", new AgentSite("downstream", null, "2"), new AgentSite("type~BBaB0000", null, null), new AgentSite("binding", null, null));
        Agent rightAgent2 = new Agent("DNA", new AgentSite("upstream", null, "2"), new AgentSite("binding", null, "1"));
        Agent rightAgent3 = new Agent("RNA", new AgentSite("downstream", null, "3"), new AgentSite("type~BBaB0000", null, null), new AgentSite("upstream", null, "4"), new AgentSite("binding", null, null));
        Agent rightAgent4 = new Agent("RNA", new AgentSite("downstream", null, "4"));
        Agent rightAgent5 = new Agent("RNAP", new AgentSite("dna", null, "1"), new AgentSite("rna", null, "3"));
        
        List<Complex> rightComplexes = getComplexes(rightAgent1, rightAgent2, rightAgent3, rightAgent4, rightAgent5);
                
        Transform transform = new Transform("test", leftComplexes, rightComplexes, 10.0f);
                
        TransformPrimitive[] expected = new TransformPrimitive[] {
                TransformPrimitive.getDeleteLink(getAgentLink(leftComplexes, "1")),
                TransformPrimitive.getDeleteLink(getAgentLink(leftComplexes, "3")),
                TransformPrimitive.getCreateAgent(rightAgent3, leftAgent2),
                TransformPrimitive.getCreateLink(leftAgent2.getSite("binding"), leftAgent4.getSite("dna")),
                TransformPrimitive.getCreateLink(leftAgent3.getSite("downstream"), rightAgent3.getSite("upstream")),
                TransformPrimitive.getCreateLink(leftAgent4.getSite("rna"), rightAgent3.getSite("downstream")),
        };
        assertEquals(Arrays.asList(expected), transform.bestPrimitives); 
    }

    private AgentLink getAgentLink(List<Complex> complexes, String linkName) {
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

    private void checkTransform(String label, List<Complex> leftComplexes, List<Complex> rightComplexes, float rate, boolean isInfiniteRate, Transform transform) {
        SimulationState state = new ModelOnlySimulationState(new HashMap<String, Variable>());
        assertEquals(label, transform.label);
        assertEquals(getComplexStrings(leftComplexes), getComplexStrings(transform.sourceComplexes));
        assertEquals(getComplexStrings(rightComplexes), getComplexStrings(transform.targetComplexes));
        if (!isInfiniteRate) {
            assertEquals(rate, transform.getRate().evaluate(state).value, 0.00000001);
        }
        assertEquals(isInfiniteRate, transform.isInfiniteRate(new HashMap<String, Variable>()));
    }

    private Set<String> getComplexStrings(List<Complex> complexes) {
        Set<String> result = new HashSet<String>();
        for (Complex complex : complexes) {
            result.add(complex.toString());
        }
        return result;
    }

    @Test
    public void testCreateTransformMap() {
        List<Agent> leftAgents = new ArrayList<Agent>();
        List<Agent> rightAgents = new ArrayList<Agent>();
        leftAgents.add(new Agent("agent1", new AgentSite("x", "s", null)));
        rightAgents.add(new Agent("agent1", new AgentSite("x", "t", null)));
        Transform transform = new Transform(null, Utils.getComplexes(leftAgents), Utils.getComplexes(rightAgents), 0.1f);

        transform.bestPrimitives = null;
        leftAgents.clear();
        rightAgents.clear();
        leftAgents.add(new Agent("agent1", new AgentSite("x", "s", null)));
        leftAgents.add(new Agent("agent2"));
        rightAgents.add(new Agent("agent1", new AgentSite("x", "t", null)));
        rightAgents.add(new Agent("agent3", new AgentSite("x", "t", null)));
        Utils.getComplexes(leftAgents);
        Utils.getComplexes(rightAgents);
        transform.createTransformMap(leftAgents, rightAgents);

        transform.bestPrimitives = null;
        leftAgents.clear();
        rightAgents.clear();
        leftAgents.add(new Agent("agent1", new AgentSite("x", "s", "1")));
        leftAgents.add(new Agent("agent2", new AgentSite("y", "t", "1")));
        rightAgents.add(new Agent("agent1", new AgentSite("x", "s", null)));
        rightAgents.add(new Agent("agent2", new AgentSite("y", "t", null)));
        Utils.getComplexes(leftAgents);
        Utils.getComplexes(rightAgents);
        transform.createTransformMap(leftAgents, rightAgents);

        transform.bestPrimitives = null;
        leftAgents.clear();
        rightAgents.clear();
        leftAgents.add(new Agent("agent1", new AgentSite("x", "s", "1"), new AgentSite("y", "t", "1")));
        rightAgents.add(new Agent("agent1", new AgentSite("x", "s", null), new AgentSite("y", "t", null)));
        Utils.getComplexes(leftAgents);
        Utils.getComplexes(rightAgents);
        transform.createTransformMap(leftAgents, rightAgents);

        transform.bestPrimitives = null;
        leftAgents.clear();
        rightAgents.clear();
        leftAgents.add(new Agent("agent1", new AgentSite("x", "s", null)));
        leftAgents.add(new Agent("agent2", new AgentSite("y", "t", null)));
        rightAgents.add(new Agent("agent1", new AgentSite("x", "s", "1")));
        rightAgents.add(new Agent("agent2", new AgentSite("y", "t", "1")));
        Utils.getComplexes(leftAgents);
        Utils.getComplexes(rightAgents);
        transform.createTransformMap(leftAgents, rightAgents);

        transform.bestPrimitives = null;
        leftAgents.clear();
        rightAgents.clear();
        leftAgents.add(new Agent("agent1", new AgentSite("x", "s", null)));
        rightAgents.add(new Agent("agent1", new AgentSite("x", "s", "1")));
        rightAgents.add(new Agent("agent2", new AgentSite("y", "t", "1")));
        Utils.getComplexes(leftAgents);
        Utils.getComplexes(rightAgents);
        transform.createTransformMap(leftAgents, rightAgents);
    }

    @Test
    public void testCreateTransformMap_largerCase() {
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

        Transform transform = new Transform(null, Utils.getComplexes(leftAgents), Utils.getComplexes(rightAgents), 0.1f);
        
        transform.bestPrimitives = null;
        transform.createTransformMap(leftAgents, rightAgents);
    }

    @Test
    public void testCreateCloneMap() {
        List<Agent> leftAgents = new ArrayList<Agent>();
        List<Agent> rightAgents = new ArrayList<Agent>();
        leftAgents.add(new Agent("agent1", new AgentSite("x", "s", null)));
        rightAgents.add(new Agent("agent1", new AgentSite("x", "t", null)));
        Transform transform = new Transform(null, Utils.getComplexes(leftAgents), Utils.getComplexes(rightAgents), 0.1f);

        // Test empty case
        Map<Agent, Agent> originalMap = new HashMap<Agent, Agent>();
        Map<Agent, Agent> resultMap = transform.createCloneMap(originalMap);
        assertEquals(0, resultMap.size());

        // Test different template complexes, original complexes
        Agent templateSourceAgent = new Agent("source");
        Agent templateTargetAgent = new Agent("target");
        new Complex(templateSourceAgent);
        new Complex(templateTargetAgent);

        Agent realSourceAgent = new Agent("source");
        Agent realTargetAgent = new Agent("target");
        new Complex(realSourceAgent);
        new Complex(realTargetAgent);

        originalMap.clear();
        originalMap.put(templateSourceAgent, realSourceAgent);
        originalMap.put(templateTargetAgent, realTargetAgent);

        resultMap = transform.createCloneMap(originalMap);
        checkCloneMap(originalMap, resultMap);

        // Test same template complexes, original complexes
        new Complex(templateSourceAgent, templateTargetAgent);
        realSourceAgent = new Agent("source");
        realTargetAgent = new Agent("target");
        new Complex(realSourceAgent, realTargetAgent);

        originalMap.clear();
        originalMap.put(templateSourceAgent, realSourceAgent);
        originalMap.put(templateTargetAgent, realTargetAgent);

        resultMap = transform.createCloneMap(originalMap);
        checkCloneMap(originalMap, resultMap);

        // Test same template complexes, original complexes, linked agents
        realSourceAgent = new Agent("source", new AgentSite("x", null, "1"));
        realTargetAgent = new Agent("target", new AgentSite("y", null, "1"));
        new Complex(realSourceAgent, realTargetAgent);

        originalMap.clear();
        originalMap.put(templateSourceAgent, realSourceAgent);
        originalMap.put(templateTargetAgent, realTargetAgent);

        resultMap = transform.createCloneMap(originalMap);
        checkCloneMap(originalMap, resultMap);
    }

    private void checkCloneMap(Map<Agent, Agent> originalMap, Map<Agent, Agent> resultMap) {
        assertEquals(originalMap.size(), resultMap.size());

        for (Map.Entry<Agent, Agent> originalEntry : originalMap.entrySet()) {
            Agent templateAgent = originalEntry.getKey();
            Agent originalAgent = originalEntry.getValue();

            assertTrue(resultMap.containsKey(templateAgent));
            Agent cloneAgent = resultMap.get(templateAgent);
            assertEquals(originalAgent.getComplex().toString(), cloneAgent.getComplex().toString());
            assertNotSame(cloneAgent, originalAgent);
            assertNotSame(cloneAgent.getComplex(), originalAgent.getComplex());
        }

        // Unlinked template agents should each have unique cloned complex
        for (Map.Entry<Agent, Agent> originalEntry : originalMap.entrySet()) {
            Agent templateAgent = originalEntry.getKey();
            for (Map.Entry<Agent, Agent> originalOtherEntry : originalMap.entrySet()) {
                Agent templateOtherAgent = originalOtherEntry.getKey();
                if (templateAgent != templateOtherAgent) {
                    if (templateAgent.getComplex() == templateOtherAgent.getComplex()) {
                        assertSame(resultMap.get(templateAgent).getComplex(), resultMap.get(templateOtherAgent).getComplex());
                    }
                    else {
                        assertNotSame(resultMap.get(templateAgent).getComplex(), resultMap.get(templateOtherAgent).getComplex());
                    }
                }
            }
        }
    }

    public static List<Complex> getComplexes(Agent... agents) {
        return Utils.getComplexes(Arrays.asList(agents));
    }
}