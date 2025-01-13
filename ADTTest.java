import java.time.LocalTime;
import java.time.LocalDate;
import java.util.*;

public class ADTTest {

    public static void main(String[] args) {
        testGraph();
        testHashMap();
        testArrayList();
        testComparator();
        testPriorityQueue();
        testDynamicRouteAdjustments();
        testTrafficManager();
    }

    public static void testGraph() {
        System.out.println("\n=== Testing Graph ADT ===");
        Graph graph = new Graph();
        graph.addRoad("A", "B", 10);
        graph.addRoad("B", "C", 20);
        graph.addRoad("C", "D", 30);
        System.out.println("Graph created with roads: A-B, B-C, C-D");
        System.out.println("Road between A and B: " + graph.getRoad("A", "B"));
        System.out.println("Road between B and C: " + graph.getRoad("B", "C"));
        System.out.println("Road between C and D: " + graph.getRoad("C", "D"));

        System.out.println("\nCalculating shortest path from A to D");
        List<String> path = graph.findOptimalPath("A", "D", LocalTime.of(10, 0));
        System.out.println("Shortest path: " + path);

        System.out.println("\nCalculating shortest path from B to C");
        path = graph.findOptimalPath("B", "C", LocalTime.of(10, 0));
        System.out.println("Shortest path: " + path);
    }

    public static void testHashMap() {
        System.out.println("\n=== Testing HashMap ADT ===");

        Map<LocalDate, Boolean> driverAvailability = new HashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        
        System.out.println("Adding driver availability...");
        driverAvailability.put(today, true);
        driverAvailability.put(tomorrow, false);
        
        System.out.println("Driver available today: " + driverAvailability.get(today));
        System.out.println("Driver available tomorrow: " + driverAvailability.get(tomorrow));
        

        Map<LocalDate, List<DeliveryRequest>> deliveryRequests = new HashMap<>();
        POI hub = new POI("Test Hub", "123 Test St", true);
        POI destination = new POI("Test Dest", "456 Test Ave", false);
        
        DeliveryRequest request1 = new DeliveryRequest("REQ1", hub, destination, 3, today, LocalTime.of(10, 0), 5.0, "Medium");
        DeliveryRequest request2 = new DeliveryRequest("REQ2", hub, destination, 4, today, LocalTime.of(14, 0), 3.0, "Small");
        
        List<DeliveryRequest> todayRequests = new ArrayList<>();
        todayRequests.add(request1);
        todayRequests.add(request2);
        
        deliveryRequests.put(today, todayRequests);
        System.out.println("\nNumber of delivery requests for today: " + deliveryRequests.get(today).size());
    }

    public static void testArrayList() {
        System.out.println("\n=== Testing ArrayList ADT ===");
        List<Driver> drivers = new ArrayList<>();
        
        System.out.println("Adding drivers...");
        drivers.add(new Driver("D1", "John Doe"));
        drivers.add(new Driver("D2", "Jane Smith"));
        drivers.add(new Driver("D3", "Bob Wilson"));
        
        System.out.println("Number of drivers: " + drivers.size());
        System.out.println("First driver: " + drivers.get(0).getName());
        
        System.out.println("\nRemoving driver D2...");
        drivers.removeIf(d -> d.getId().equals("D2"));
        System.out.println("Number of drivers after removal: " + drivers.size());
        
        System.out.println("\nIterating through remaining drivers:");
        for (Driver driver : drivers) {
            System.out.println("Driver: " + driver.getName() + " (ID: " + driver.getId() + ")");
        }
    }

    public static void testComparator() {
        System.out.println("\n=== Testing Comparator ADT ===");
        List<DeliveryRequest> requests = new ArrayList<>();
        POI hub = new POI("Test Hub", "123 Test St", true);
        POI destination = new POI("Test Dest", "456 Test Ave", false);
        LocalDate today = LocalDate.now();
        
        requests.add(new DeliveryRequest("REQ1", hub, destination, 3, today, LocalTime.of(10, 0), 5.0, "Medium"));
        requests.add(new DeliveryRequest("REQ2", hub, destination, 5, today, LocalTime.of(14, 0), 3.0, "Small"));
        requests.add(new DeliveryRequest("REQ3", hub, destination, 1, today, LocalTime.of(12, 0), 7.0, "Large"));
        
        System.out.println("Before sorting by urgency:");
        for (DeliveryRequest req : requests) {
            System.out.println("Request " + req.getId() + " - Urgency: " + req.getUrgencyLevel());
        }
        

        requests.sort((r1, r2) -> Integer.compare(r2.getUrgencyLevel(), r1.getUrgencyLevel()));
        
        System.out.println("\nAfter sorting by urgency (highest to lowest):");
        for (DeliveryRequest req : requests) {
            System.out.println("Request " + req.getId() + " - Urgency: " + req.getUrgencyLevel());
        }
    }

    public static void testPriorityQueue() {
        System.out.println("\n=== Testing PriorityQueue ADT ===");

        PriorityQueue<DeliveryRequest> requestQueue = new PriorityQueue<>(
            (r1, r2) -> Integer.compare(r2.getUrgencyLevel(), r1.getUrgencyLevel())
        );
        
        POI hub = new POI("Test Hub", "123 Test St", true);
        POI destination = new POI("Test Dest", "456 Test Ave", false);
        LocalDate today = LocalDate.now();
        
        System.out.println("Adding delivery requests to priority queue...");
        requestQueue.add(new DeliveryRequest("REQ1", hub, destination, 3, today, LocalTime.of(10, 0), 5.0, "Medium"));
        requestQueue.add(new DeliveryRequest("REQ2", hub, destination, 5, today, LocalTime.of(14, 0), 3.0, "Small"));
        requestQueue.add(new DeliveryRequest("REQ3", hub, destination, 1, today, LocalTime.of(12, 0), 7.0, "Large"));
        requestQueue.add(new DeliveryRequest("REQ4", hub, destination, 4, today, LocalTime.of(16, 0), 2.0, "Small"));
        
        System.out.println("\nProcessing requests in priority order:");
        while (!requestQueue.isEmpty()) {
            DeliveryRequest request = requestQueue.poll();
            System.out.println("Processing Request " + request.getId() + 
                             " - Urgency Level: " + request.getUrgencyLevel() +
                             " - Preferred Time: " + request.getPreferredTime());
        }
    }

    public static void testDynamicRouteAdjustments() {
        System.out.println("\n=== Testing Dynamic Route Adjustments ===");
        

        Driver driver = new Driver("D1", "Test Driver");
        POI hub = new POI("Test Hub", "123 Test St", true);
        Graph roadNetwork = new Graph();
        Route route = new Route(driver, hub, roadNetwork);
        

        System.out.println("Initial route stops: " + route.getStops().size());
        

        POI destination1 = new POI("Dest1", "456 Test Ave", false);
        POI destination2 = new POI("Dest2", "789 Test Blvd", false);
        

        roadNetwork.addRoad(hub.getName(), destination1.getName(), 10.0);
        roadNetwork.addRoad(destination1.getName(), destination2.getName(), 5.0);
        

        LocalDate today = LocalDate.now();
        DeliveryRequest request1 = new DeliveryRequest(
            "REQ1", hub, destination1, 3, today, 
            LocalTime.of(10, 0), 5.0, "Medium"
        );
        DeliveryRequest request2 = new DeliveryRequest(
            "REQ2", hub, destination2, 4, today,
            LocalTime.of(11, 0), 3.0, "Small"
        );
        

        System.out.println("\nTesting stop additions:");
        route.setStartTime(LocalTime.of(9, 0));
        
        boolean added1 = route.canAddStop(destination1, request1.getPreferredTime());
        if (added1) {
            route.addStop(destination1, request1);
            System.out.println("Successfully added first stop");
        }
        
        boolean added2 = route.canAddStop(destination2, request2.getPreferredTime());
        if (added2) {
            route.addStop(destination2, request2);
            System.out.println("Successfully added second stop");
        }
        

        System.out.println("\nRoute metrics after additions:");
        System.out.println("Number of stops: " + route.getStops().size());
        System.out.println("Total distance: " + route.getTotalDistance() + " km");
        System.out.println("Estimated completion time: " + route.getEstimatedCompletionTime());
    }

    public static void testTrafficManager() {
        System.out.println("\n=== Testing TrafficManager Integration ===");
        
        TrafficManager trafficManager = new TrafficManager();
        

        LocalTime[] testTimes = {
            LocalTime.of(8, 0),   // Start of day
            LocalTime.of(9, 0),   // Morning peak
            LocalTime.of(12, 0),  // Mid-day
            LocalTime.of(17, 0),  // Evening peak
            LocalTime.of(20, 0)   // End of day
        };
        
        System.out.println("\nTesting traffic multipliers at different times:");
        for (LocalTime time : testTimes) {
            double multiplier = trafficManager.getTrafficMultiplier(time);
            boolean isPeak = trafficManager.isPeakHour(time);
            System.out.printf("Time: %s - Multiplier: %.2f - Peak Hour: %s%n", 
                time, multiplier, isPeak ? "Yes" : "No");
        }
        

        Graph roadNetwork = new Graph();
        POI start = new POI("Start", "Start St", true);
        POI end = new POI("End", "End St", false);
        roadNetwork.addRoad(start.getName(), end.getName(), 10.0);
        
        System.out.println("\nTesting impact on route calculations:");
        for (LocalTime time : testTimes) {
            List<String> path = roadNetwork.findOptimalPath(
                start.getName(), end.getName(), time);
            Graph.Road road = roadNetwork.getRoad(start.getName(), end.getName());
            double adjustedDistance = road.getCurrentDistance(time, trafficManager);
            
            System.out.printf("Time: %s - Adjusted Distance: %.2f km%n", 
                time, adjustedDistance);
        }
    }
}
