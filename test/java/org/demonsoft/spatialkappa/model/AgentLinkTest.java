package org.demonsoft.spatialkappa.model;

import static org.junit.Assert.*;

import org.demonsoft.spatialkappa.model.AgentLink;
import org.demonsoft.spatialkappa.model.AgentSite;
import org.junit.Test;

public class AgentLinkTest {

    private AgentSite interface1 = new AgentSite("interface1", null, null);
    private AgentSite interface2 = new AgentSite("interface2", null, null);

    @SuppressWarnings("unused")
    @Test
    public void testConstructor() {
        try {
            new AgentLink(null, interface2);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            new AgentLink(interface1, null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            new AgentLink(AgentLink.ANY, interface2);
            fail("incorrect special case should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        try {
            new AgentLink(AgentLink.OCCUPIED, interface2);
            fail("incorrect special case should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        try {
            new AgentLink(new AgentSite("interface1", null, "1", "ch1"), new AgentSite("interface2", null, "1", "ch2"));
            fail("channel mismatch should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        AgentLink agentLink = new AgentLink(interface1, interface2);
        assertEquals(interface1, agentLink.sourceSite);
        assertEquals(interface2, agentLink.targetSite);
        assertFalse(agentLink.isAnyLink());
        assertFalse(agentLink.isNoneLink());
        assertFalse(agentLink.isOccupiedLink());
        assertNull(agentLink.getChannel());
        
        agentLink = new AgentLink(new AgentSite("interface1", null, "1", "ch1"), interface2);
        assertEquals("ch1", agentLink.getChannel());
        
        agentLink = new AgentLink(interface1, new AgentSite("interface2", null, "1", "ch2"));
        assertEquals("ch2", agentLink.getChannel());
        
        agentLink = new AgentLink(new AgentSite("interface1", null, "1", "ch2"), new AgentSite("interface2", null, "1", "ch2"));
        assertEquals("ch2", agentLink.getChannel());
    }

    @Test
    public void testGetAnyLink() {
        try {
            AgentLink.getAnyLink(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            AgentLink.getAnyLink(AgentLink.ANY);
            fail("incorrect special case should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        try {
            AgentLink.getAnyLink(AgentLink.OCCUPIED);
            fail("incorrect special case should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        AgentLink agentLink = AgentLink.getAnyLink(interface1);
        assertEquals(interface1, agentLink.sourceSite);
        assertEquals(AgentLink.ANY, agentLink.targetSite);
        assertTrue(agentLink.isAnyLink());
        assertFalse(agentLink.isNoneLink());
        assertFalse(agentLink.isOccupiedLink());
    }

    @Test
    public void testGetOccupiedLink() {
        try {
            AgentLink.getOccupiedLink(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            AgentLink.getOccupiedLink(AgentLink.ANY);
            fail("incorrect special case should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        try {
            AgentLink.getOccupiedLink(AgentLink.OCCUPIED);
            fail("incorrect special case should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        AgentLink agentLink = AgentLink.getOccupiedLink(interface1);
        assertEquals(interface1, agentLink.sourceSite);
        assertEquals(AgentLink.OCCUPIED, agentLink.targetSite);
        assertFalse(agentLink.isAnyLink());
        assertFalse(agentLink.isNoneLink());
        assertTrue(agentLink.isOccupiedLink());
    }

    @Test
    public void testGetNoneLink() {
        try {
            AgentLink.getNoneLink(null);
            fail("null should have failed");
        }
        catch (NullPointerException ex) {
            // Expected exception
        }
        
        try {
            AgentLink.getNoneLink(AgentLink.ANY);
            fail("incorrect special case should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        try {
            AgentLink.getNoneLink(AgentLink.OCCUPIED);
            fail("incorrect special case should have failed");
        }
        catch (IllegalArgumentException ex) {
            // Expected exception
        }
        
        AgentLink agentLink = AgentLink.getNoneLink(interface1);
        assertEquals(interface1, agentLink.sourceSite);
        assertEquals(AgentLink.NONE, agentLink.targetSite);
        assertFalse(agentLink.isAnyLink());
        assertTrue(agentLink.isNoneLink());
        assertFalse(agentLink.isOccupiedLink());
    }

}
