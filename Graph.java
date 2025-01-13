import java.time.LocalTime;
import java.util.*;


class Graph {
    // maps containing all roads. outer map is starting point,
    // inner map holds possible destinations and their roads
    private Map<String, Map<String, Road>> adjacencyList;
    private TrafficManager trafficManager;

    static class Road {
        // base distance without traffic
        double baseDistance;
        // multiplier for current traffic conditions
        double currentTrafficMultiplier;

        Road(double baseDistance) {
            this.baseDistance = baseDistance;
            this.currentTrafficMultiplier = 1.0;
        }

        double getCurrentDistance(LocalTime time, TrafficManager trafficManager) {
            return baseDistance * trafficManager.getTrafficMultiplier(time);
        }

        double getDistance() {
            return baseDistance;
        }

        double getTrafficMultiplier(LocalTime time) {
            return currentTrafficMultiplier;
        }
    }

    // constructor for the graph
    public Graph() {
        this.adjacencyList = new HashMap<>();
        this.trafficManager = new TrafficManager();
    }

    // adds a 2-way road between locations
    public void addRoad(String from, String to, double distance) {
        adjacencyList.computeIfAbsent(from, k -> new HashMap<>())
                    .put(to, new Road(distance));
        adjacencyList.computeIfAbsent(to, k -> new HashMap<>())
                    .put(from, new Road(distance));
    }

    // gets road between 2 POIs if it exists
    public Road getRoad(POI from, POI to) {
        if (from == null || to == null) return null;
        return getRoad(from.getName(), to.getName());
    }

    // same as above but with location names
    public Road getRoad(String from, String to) {
        if (from == null || to == null) return null;
        Map<String, Road> neighbors = adjacencyList.get(from);
        return neighbors != null ? neighbors.get(to) : null;
    }

    // finds shortest path using dijkstra, accounts for traffic
    public List<String> findOptimalPath(String start, String end, LocalTime time) {
        if (start == null || end == null) {
            return new ArrayList<>();
        }

        // track shortest distance to each location
        Map<String, Double> distances = new HashMap<>();
        // store previous node to rebuild path later
        Map<String, String> previousNodes = new HashMap<>();
        // priority queue to process closest node first
        PriorityQueue<Map.Entry<String, Double>> queue = 
            new PriorityQueue<>(Map.Entry.comparingByValue());
        // track visited nodes
        Set<String> visited = new HashSet<>();

        // Initialize distances
        for (String node : adjacencyList.keySet()) {
            distances.put(node, Double.POSITIVE_INFINITY);
        }
        distances.put(start, 0.0);
        queue.offer(new AbstractMap.SimpleEntry<>(start, 0.0));

        while (!queue.isEmpty()) {
            String current = queue.poll().getKey();
            if (current.equals(end)) {
                break;
            }

            if (visited.contains(current)) {
                continue;
            }
            visited.add(current);

            for (Map.Entry<String, Road> neighbor : adjacencyList.get(current).entrySet()) {
                if (visited.contains(neighbor.getKey())) {
                    continue;
                }

                double newDist = distances.get(current) + 
                    neighbor.getValue().getCurrentDistance(time, trafficManager);

                if (newDist < distances.get(neighbor.getKey())) {
                    distances.put(neighbor.getKey(), newDist);
                    previousNodes.put(neighbor.getKey(), current);
                    queue.offer(new AbstractMap.SimpleEntry<>(neighbor.getKey(), newDist));
                }
            }
        }

        // Reconstruct path
        List<String> path = new ArrayList<>();
        String current = end;
        while (current != null) {
            path.add(0, current);
            current = previousNodes.get(current);
        }

        return path;
    }

    public TrafficManager getTrafficManager() {
        return trafficManager;
    }

    // makes sure a POI exists in the graph even without connections
    public void initializePOI(String poiName) {
        adjacencyList.putIfAbsent(poiName, new HashMap<>());
    }
}
