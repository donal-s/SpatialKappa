package org.demonsoft.spatialkappa.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestUtils {

    public static <T> List<T> getList(T... elements) {
        List<T> result = new ArrayList<T>();
        result.addAll(Arrays.asList(elements));
        return result;
    }

}
