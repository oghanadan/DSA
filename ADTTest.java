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
        // Test Driver availability HashMap
        Map<LocalDate, Boolean> driverAvailability = new HashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        
        System.out.println("Adding driver availability...");
        driverAvailability.put(today, true);
        driverAvailability.put(tomorrow, false);
        
        System.out.println("Driver available today: " + driverAvailability.get(today));
        System.out.println("Driver available tomorrow: " + driverAvailability.get(tomorrow));
        
        // Test delivery requests HashMap
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
        
        // Sort by urgency level (highest to lowest)
        requests.sort((r1, r2) -> Integer.compare(r2.getUrgencyLevel(), r1.getUrgencyLevel()));
        
        System.out.println("\nAfter sorting by urgency (highest to lowest):");
        for (DeliveryRequest req : requests) {
            System.out.println("Request " + req.getId() + " - Urgency: " + req.getUrgencyLevel());
        }
    }

    public static void testPriorityQueue() {
        System.out.println("\n=== Testing PriorityQueue ADT ===");
        // Create a priority queue of delivery requests sorted by urgency level
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
}
