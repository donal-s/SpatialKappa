package org.demonsoft.spatialkappa.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

public class Utils {

    public static String getFlatString(Collection<? extends Object> elements) {
        return getFlatString(",", false, elements.toArray());
    }
    
    public static String getFlatString(String seperator, boolean skipNulls, List<? extends Object> elements) {
        return getFlatString(seperator, skipNulls, elements.toArray());
    }
    
    public static String getFlatString(String seperator, boolean skipNulls, Object... elements) {
        StringBuilder builder = new StringBuilder();
        boolean firstWritten = false;
        for (int index = 0; index < elements.length; index++) {
            if (!skipNulls || elements[index] != null) {
                if (firstWritten) {
                    builder.append(seperator);
                }
                else {
                    firstWritten = true;
                }
                builder.append(elements[index]);
            }
        }
        return builder.toString();
    }

    public static List<Complex> getComplexes(List<Agent> agents) {
        if (agents == null) {
            throw new NullPointerException();
        }
        List<Complex> result = new ArrayList<Complex>();
        List<Agent> remainingAgents = new ArrayList<Agent>(agents);
        while (!remainingAgents.isEmpty()) {
            List<Agent> linkedAgents = new ArrayList<Agent>();
            Stack<String> links = new Stack<String>();

            Agent current = remainingAgents.get(0);
            remainingAgents.remove(current);
            linkedAgents.add(current);

            addLinksToStack(links, current);

            while (links.size() > 0) {
                String currentLink = links.pop();

                ListIterator<Agent> iter = remainingAgents.listIterator();
                while (iter.hasNext()) {
                    current = iter.next();
                    if (current.hasLink(currentLink)) {
                        linkedAgents.add(current);
                        iter.remove();
                        addLinksToStack(links, current);
                    }
                }
            }

            result.add(new Complex(linkedAgents));
        }
        return result;
    }

    private static void addLinksToStack(Stack<String> links, Agent agent) {
        for (AgentSite site : agent.getSites()) {
            if (site.getLinkName() != null) {
                String link = site.getLinkName();
                if (!"_".equals(link) && !"?".equals(link)) {
                    links.push(link);
                }
            }
        }
    }

    public static boolean equal(Object o1, Object o2) {
        return (o1 == null && o2 == null) || (o1 != null && o1.equals(o2));
    }


}
