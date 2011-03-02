package org.demonsoft.spatialkappa.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LocatedComplexMap<E> {

    Map<Location, Map<Complex, E>> locationComplexMap = new HashMap<Location, Map<Complex, E>>();
    Map<Complex, Map<Location, E>> complexLocationMap = new HashMap<Complex, Map<Location, E>>();

    public boolean contains(Complex complex, Location location) {
        Map<Location, E> locationMap = complexLocationMap.get(complex);
        if (locationMap != null) {
            return locationMap.containsKey(location);
        }
        return false;
    }

    public E get(Complex complex, Location location) {
        Map<Location, E> locationMap = complexLocationMap.get(complex);
        if (locationMap != null) {
            return locationMap.get(location);
        }
        return null;
    }

    public void put(Complex complex, Location location, E value) {
        Map<Location, E> locationMap = complexLocationMap.get(complex);
        if (locationMap == null) {
            locationMap = new HashMap<Location, E>();
            complexLocationMap.put(complex, locationMap);
        }
        locationMap.put(location, value);

        Map<Complex, E> complexMap = locationComplexMap.get(location);
        if (complexMap == null) {
            complexMap = new HashMap<Complex, E>();
            locationComplexMap.put(location, complexMap);
        }
        complexMap.put(complex, value);
    }

    public void remove(Complex complex) {
        Map<Location, E> locationMap = complexLocationMap.get(complex);
        if (locationMap != null) {
            Set<Location> locations = locationMap.keySet();
            complexLocationMap.remove(complex);

            for (Location current : locations) {
                locationComplexMap.get(current).remove(complex);
            }
        }
    }

    public void remove(Complex complex, Location location) {
        Map<Location, E> locationMap = complexLocationMap.get(complex);
        if (locationMap != null) {
            locationMap.remove(location);
            if (locationMap.isEmpty()) {
                complexLocationMap.remove(complex);
            }
        }

        Map<Complex, E> complexMap = locationComplexMap.get(location);
        if (complexMap != null) {
            complexMap.remove(complex);
            if (complexMap.isEmpty()) {
                locationComplexMap.remove(location);
            }
        }
    }

    public void clear() {
        locationComplexMap.clear();
        complexLocationMap.clear();
    }

    public Map<Location, E> getLocatedValues(Complex complex, Location location, boolean matchCompartmentNameOnly) {
        Map<Location, E> result = new HashMap<Location, E>();
        Map<Location, E> locationMap = complexLocationMap.get(complex);
        if (locationMap != null) {
            for (Map.Entry<Location, E> entry : locationMap.entrySet()) {
                if (doLocationsMatch(location, entry.getKey(), matchCompartmentNameOnly)) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return result;
    }

    private boolean doLocationsMatch(Location location1, Location location2, boolean matchNameOnly) {
        if (matchNameOnly) {
            return (location1 == null && location2 == null) || (location1 != null && location1.getName().equals(location2.getName()));
        }
        return Utils.equal(location1, location2);
    }

    public String getDebugOutput() {
        StringBuilder builder = new StringBuilder();

        for (Map.Entry<Location, Map<Complex, E>> locationComplexEntry : locationComplexMap.entrySet()) {
            for (Map.Entry<Complex, E> complexEntry : locationComplexEntry.getValue().entrySet()) {
                builder.append(locationComplexEntry.getKey() + "\t" + complexEntry.getKey() + "\t" + complexEntry.getValue() + "\n");
            }

        }
        return builder.toString();
    }

}
