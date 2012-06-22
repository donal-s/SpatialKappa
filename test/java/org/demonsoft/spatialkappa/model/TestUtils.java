package org.demonsoft.spatialkappa.model;

import java.util.Arrays;
import java.util.List;

public class TestUtils {

    public static List<Complex> getComplexes(Agent... agents) {
        return Utils.getComplexes(Arrays.asList(agents));
    }

}
