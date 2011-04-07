package org.demonsoft.spatialkappa.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ComplexStore {

    private static final Collection<Complex> EMPTY_COMPLEX_LIST = new ArrayList<Complex>();
    
    Map<Location, Map<Complex, Integer>> locationComplexQuantityMap = new HashMap<Location, Map<Complex, Integer>>();
    Map<Complex,  Map<Location, Integer>> complexLocationQuantityMap =  new HashMap<Complex,  Map<Location, Integer>>();
    Map<Complex, Integer> totalComplexQuantities = new HashMap<Complex, Integer>();

    public Complex getExistingComplex(Complex complex) {
        if (totalComplexQuantities.containsKey(complex)) {
            return complex;
        }
        return getMatchingComplex(complex);
    }

    public boolean isExistingLocatedComplex(Complex complex, Location location) {
        Map<Location, Integer> locationQuantities = complexLocationQuantityMap.get(complex);
        if (locationQuantities != null) {
            return locationQuantities.containsKey(location);
        }
        return false;
    }

    public Complex getMatchingComplex(Complex complex) {
        for (Complex existingComplex : totalComplexQuantities.keySet()) {
            if (complex.isExactMatch(existingComplex)) {
                return existingComplex;
            }
        }
        return null;
    }

    public Set<Complex> getComplexes() {
        return totalComplexQuantities.keySet();
    }
    
    public int getComplexQuantity(Complex complex, Location location) {
        Map<Location, Integer> locationQuantities = complexLocationQuantityMap.get(complex);
        if (locationQuantities != null) {
            Integer quantity =  locationQuantities.get(location);
            return quantity == null ? 0 : quantity;
        }
        return 0;
    }

    public int getComplexQuantity(Complex complex) {
        Integer quantity = totalComplexQuantities.get(complex);
        return quantity == null ? 0 : quantity;
    }


    public void increaseComplexQuantity(Complex complex, Location location, int change) {
        int value = change;
        Map<Location, Integer> locationQuantities = complexLocationQuantityMap.get(complex);
        if (locationQuantities != null) {
            Integer quantity = locationQuantities.get(location);
            value += quantity == null ? 0 : quantity;
        }
        else {
            locationQuantities = new HashMap<Location, Integer>();
            complexLocationQuantityMap.put(complex, locationQuantities);
        }
        locationQuantities.put(location, value);
        
        Map<Complex, Integer> complexQuantities = locationComplexQuantityMap.get(location);
        if (complexQuantities == null) {
            complexQuantities = new HashMap<Complex, Integer>();
            locationComplexQuantityMap.put(location, complexQuantities);
        }
        complexQuantities.put(complex, value);
        
        if (totalComplexQuantities.containsKey(complex)) {
            totalComplexQuantities.put(complex, totalComplexQuantities.get(complex) + change);
        }
        else {
            totalComplexQuantities.put(complex, change);
        }

    }

    /**
     * 
     * @param complex
     * @param location
     * @param change
     * @return true if complex no longer in use
     */
    public boolean decreaseComplexQuantity(Complex complex, Location location, int change) {
        int value = -change;
        Map<Location, Integer> locationQuantities = complexLocationQuantityMap.get(complex);
        if (locationQuantities != null) {
            Integer quantity = locationQuantities.get(location);
            value += quantity == null ? 0 : quantity;
            if (value < 0) {
                throw new IllegalStateException();
            }
        }
        else {
            throw new IllegalStateException();
        }
        locationQuantities.put(location, value);
        
        Map<Complex, Integer> complexQuantities = locationComplexQuantityMap.get(location);
        if (complexQuantities == null) {
            throw new IllegalStateException();
        }
        complexQuantities.put(complex, value);

        value = totalComplexQuantities.get(complex) - change;
        totalComplexQuantities.put(complex, value);

        return (value == 0);
    }

    public void removeComplex(Complex complex) {
        Map<Location, Integer> locationQuantities = complexLocationQuantityMap.get(complex);
        if (locationQuantities != null) {
            Set<Location> locations = locationQuantities.keySet();
            complexLocationQuantityMap.remove(complex);
            
            for (Location current : locations) {
                locationComplexQuantityMap.get(current).remove(complex);
            }
        }
        totalComplexQuantities.remove(complex);
    }

    public void clear() {
        locationComplexQuantityMap.clear();
        complexLocationQuantityMap.clear();
        totalComplexQuantities.clear();
    }

    public int getComplexCount(Location location) {
        int result = 0;
        Map<Complex, Integer> complexQuantities = locationComplexQuantityMap.get(location);
        if (complexQuantities != null) {
            for (int current : complexQuantities.values()) {
                result += current;
            }
        }
        return result;
    }

    public Complex pickComplex(Location location) {
        int totalQuantity = getComplexCount(location);
        if (totalQuantity == 0) {
            return null;
        }
        int item = (int) (totalQuantity * Math.random());

        for (Map.Entry<Complex, Integer> entry : locationComplexQuantityMap.get(location).entrySet()) {
            int currentQuantity = entry.getValue();
            if (currentQuantity > 0) {
                if (item <= currentQuantity) {
                    return entry.getKey();
                }
                item -= currentQuantity;
            }
        }
        return null;
    }

    public Set<LocatedComplex> getLocatedComplexes() {
        Set<LocatedComplex> result = new HashSet<LocatedComplex>();
        for (Map.Entry<Location, Map<Complex, Integer>> entry : locationComplexQuantityMap.entrySet()) {
            Location location = entry.getKey();
            for (Complex complex : entry.getValue().keySet()) {
                result.add(new LocatedComplex(complex, location));
            }
        }
        
        return result;
    }
    
    private int getComplexQuantity(LocatedComplex complex) {
        return getComplexQuantity(complex.complex, complex.location);
    }

    public String getDebugOutput() {
        StringBuilder builder = new StringBuilder();
        List<LocatedComplex> complexes = new ArrayList<LocatedComplex>(getLocatedComplexes());
        Collections.sort(complexes, new Comparator<LocatedComplex>() {
            public int compare(LocatedComplex o1, LocatedComplex o2) {
                int result = o1.complex.toString().compareTo(o2.complex.toString());
                if (result == 0 && o1.location != null && o2.location != null) {
                    
                    result = o1.location.toString().compareTo(o2.location.toString());
                }
                return result;
            }
        });
    
        for (LocatedComplex complex : complexes) {
            int quantity = getComplexQuantity(complex);
            if (quantity > 0) {
                builder.append(quantity + "\t" + complex + "\n");
            }
        }
        
        builder.append("\nKappa Init Section Format:\n");
        
        for (LocatedComplex complex : complexes) {
            int quantity = getComplexQuantity(complex);
            if (quantity > 0) {
                builder.append("%init:");
                if (complex.location != null) {
                    builder.append(" '").append(complex.location.getName()).append("'");
                    CellIndexExpression[] indices = complex.location.getIndices();
                    if (indices != null && indices.length > 0) {
                        for (CellIndexExpression expression : indices) {
                            builder.append("[").append(expression).append("]");
                        }
                    }
                }
                builder.append(" ").append(quantity).append(" (");
                builder.append(complex.complex.agents.get(0).toString());
                for (int index = 1; index < complex.complex.agents.size(); index++) {
                    builder.append(",").append(complex.complex.agents.get(index).toString());
                }

                builder.append(")\n");
            }
        }
        
        return builder.toString();
    }

    public void remove(Complex complex, Location location) {
        Map<Location, Integer> locationMap = complexLocationQuantityMap.get(complex);
        if (locationMap != null) {
            locationMap.remove(location);
            if (locationMap.isEmpty()) {
                complexLocationQuantityMap.remove(complex);
            }
        }
        
        Map<Complex, Integer> complexMap = locationComplexQuantityMap.get(location);
        if (complexMap != null) {
            complexMap.remove(complex);
            if (complexMap.isEmpty()) {
                locationComplexQuantityMap.remove(location);
            }
        }
    }

    public Collection<Complex> getComplexes(Location location) {
        Map<Complex, Integer> complexMap = locationComplexQuantityMap.get(location);
        if (complexMap != null) {
            return complexMap.keySet();
        }
        return EMPTY_COMPLEX_LIST;
    }

}
