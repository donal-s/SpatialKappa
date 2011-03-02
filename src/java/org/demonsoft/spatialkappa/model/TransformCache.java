package org.demonsoft.spatialkappa.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class TransformCache {

    static class TransformApplication {
        public final List<ComplexMapping> sourceComplexMappings;
        public final List<Complex> targetComplexes;

        public TransformApplication(List<ComplexMapping> sourceComplexMappings, List<Complex> targetComplexes) {
            this.sourceComplexMappings = sourceComplexMappings;
            this.targetComplexes = targetComplexes;
        }

        @Override
        public String toString() {
            return sourceComplexMappings + "==>" + targetComplexes;
        }
    }

    private final Map<Transform, List<TransformApplication>> cacheMap = new HashMap<Transform, List<TransformApplication>>();

    public List<Complex> get(Transform transform, List<ComplexMapping> sourceComplexMappings) {
        if (cacheMap.containsKey(transform)) {
            for (TransformApplication application : cacheMap.get(transform)) {
                if (application.sourceComplexMappings.size() == sourceComplexMappings.size()
                        && application.sourceComplexMappings.containsAll(sourceComplexMappings)) {
                    return application.targetComplexes;
                }
            }
        }
        return null;
    }

    public void put(Transform transform, List<ComplexMapping> sourceComplexMappings, List<Complex> targetComplexes) {
        List<TransformApplication> applications = cacheMap.get(transform);
        if (applications == null) {
            applications = new ArrayList<TransformApplication>();
            cacheMap.put(transform, applications);
        }
        applications.add(new TransformApplication(sourceComplexMappings, targetComplexes));
    }

    public void removeAllWith(Complex complex) {
        for (Map.Entry<Transform, List<TransformApplication>> entry : cacheMap.entrySet()) {
            ListIterator<TransformApplication> iter = entry.getValue().listIterator();
            while (iter.hasNext()) {
                TransformApplication application = iter.next();
                if (application.targetComplexes.contains(complex)) {
                    iter.remove();
                    continue;
                }
                for (ComplexMapping complexMapping : application.sourceComplexMappings) {
                    if (complexMapping.target == complex) {
                        iter.remove();
                        break;
                    }
                }
            }
        }
    }
    
    public void clear() {
        cacheMap.clear();
    }

    public int size() {
        return cacheMap.size();
    }
}
