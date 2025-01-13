import java.util.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;

public class DeliverySystem {
    public Graph roadNetwork;
    public Map<LocalDate, List<DeliveryRequest>> deliveryRequests;
    public List<Driver> drivers;
    public Map<LocalDate, List<Route>> deliveryPlans;
    private TrafficManager trafficManager;
    public List<POI> hubs;
    public List<POI> lockers;
    private static final LocalTime WORKDAY_START = LocalTime.of(8, 0);
    private static final LocalTime WORKDAY_END = LocalTime.of(20, 0);  // 8 PM is the end of shit
    private static final double AVERAGE_SPEED = 50.0; // km/h
    private static final int MAX_DELIVERIES_PER_DRIVER = 25;
    private static final int MAX_DELIVERIES_PER_SESSION = 15;

    public DeliverySystem() {
        this.roadNetwork = new Graph();
        this.deliveryRequests = new HashMap<>();
        this.drivers = new ArrayList<>();
        this.deliveryPlans = new HashMap<>();
        this.trafficManager = new TrafficManager();
        this.hubs = new ArrayList<>();
        this.lockers = new ArrayList<>();
    }

    // hub and Locker Management
    public void addHub(POI hub) {
        hubs.add(hub);
    }

    public void addLocker(POI locker) {
        lockers.add(locker);
    }

    // driver part
    public void addDriver(Driver driver) {
        drivers.add(driver);
    }

    public void removeDriver(String driverId) {
        drivers.removeIf(d -> d.getId().equals(driverId));
        // re do affected delivery plans
        deliveryPlans.keySet().forEach(this::generateOptimizedDeliveryPlan);
    }

    // requests
    public void addDeliveryRequest(DeliveryRequest request) {
        if (request.urgencyLevel >= 4) {
            LocalTime currentTime = LocalTime.now();
            if (request.preferredTime == null || request.preferredTime.isBefore(currentTime)) {
                request.preferredTime = currentTime;
            }
        }
        
        // adds request to the list
        deliveryRequests.computeIfAbsent(request.deliveryDate, k -> new ArrayList<>())
                       .add(request);
        
        // sort every request in order of urgency
        deliveryRequests.get(request.deliveryDate).sort((r1, r2) -> 
            Integer.compare(r2.urgencyLevel, r1.urgencyLevel));
    }


    public void generateOptimizedDeliveryPlan(LocalDate date) {
        System.out.println("Starting plan generation for " + date);
        List<DeliveryRequest> requests = deliveryRequests.getOrDefault(date, new ArrayList<>());
        
        // calculates how many drives are needed based on total deliveries (it gives more than needed to make sure there are enough)
        int requiredDrivers = (requests.size() / 15) + 5;
        




        // make usre drivers are enough
        while (drivers.size() < requiredDrivers) {
            int newDriverIndex = drivers.size() + 1;
            Driver newDriver = new Driver("D" + newDriverIndex, "Driver " + newDriverIndex);
            newDriver.addAvailability(date);
            drivers.add(newDriver);
        }
        
        List<Driver> availableDrivers = getAvailableDrivers(date);

        System.out.println("Found " + requests.size() + " requests and " + availableDrivers.size() + " available drivers");
        System.out.println("Required drivers: " + requiredDrivers + " (based on total deliveries / 15 + 5 extra)");
        
        if (requests.isEmpty() || availableDrivers.isEmpty()) {
            System.out.println("No requests or drivers available");
            return;
        }

        // reset delivery plans for today
        deliveryPlans.remove(date);
        List<Route> optimizedRoutes = new ArrayList<>();
        Set<DeliveryRequest> assignedRequests = new HashSet<>();

        // group requests based on heir urgency level
        Map<Integer, List<DeliveryRequest>> requestsByUrgency = requests.stream()
            .collect(Collectors.groupingBy(DeliveryRequest::getUrgencyLevel));

        // creates the routes for the drivers
        Map<Driver, List<Route>> driverRoutes = new HashMap<>();
        for (Driver driver : availableDrivers) {
            driverRoutes.put(driver, new ArrayList<>());
        }

        // list of requests that are not assigned yet
        List<DeliveryRequest> unassignedRequests = new ArrayList<>(requests);

        // process urgency levels from most urgent to least
        for (int urgency = 5; urgency >= 1; urgency--) {
            List<DeliveryRequest> urgentRequests = requestsByUrgency.getOrDefault(urgency, new ArrayList<>());
            if (urgentRequests.isEmpty()) continue;

            System.out.println("Processing " + urgentRequests.size() + " requests with urgency level " + urgency);

            // group requests by hub for this urgency level
            Map<POI, List<DeliveryRequest>> requestsByHub = urgentRequests.stream()
                .filter(r -> !assignedRequests.contains(r))
                .collect(Collectors.groupingBy(DeliveryRequest::getPickupHub));

            // process each hub's requests
            for (Map.Entry<POI, List<DeliveryRequest>> hubEntry : requestsByHub.entrySet()) {
                POI hub = hubEntry.getKey();
                List<DeliveryRequest> hubRequests = hubEntry.getValue();

                while (!hubRequests.isEmpty()) {
                    // get available driver with least load who can still work
                    Driver bestDriver = availableDrivers.stream()
                        .filter(d -> {
                            // the the last time for this driver's routes
                            LocalTime lastCompletionTime = driverRoutes.get(d).stream()
                                .map(Route::getEstimatedCompletionTime)
                                .max(LocalTime::compareTo)
                                .orElse(WORKDAY_START);
                            
                            // if driver has less than maximum deliveries and can still deliver packages
                            return driverRoutes.get(d).stream()
                                   .mapToInt(r -> r.getStops().size())
                                   .sum() < MAX_DELIVERIES_PER_DRIVER &&
                                   lastCompletionTime.isBefore(WORKDAY_END.minusMinutes(30));
                        })
                        .min((d1, d2) -> {
                            int load1 = driverRoutes.get(d1).stream().mapToInt(r -> r.getStops().size()).sum();
                            int load2 = driverRoutes.get(d2).stream().mapToInt(r -> r.getStops().size()).sum();
                            return Integer.compare(load1, load2);
                        })
                        .orElse(null);

                    if (bestDriver == null) break;

                    // generate a route for this driver
                    Route currentRoute = new Route(bestDriver, hub, roadNetwork);
                    LocalTime nextStartTime = calculateNextStartTime(driverRoutes.get(bestDriver));
                    if (nextStartTime == null || nextStartTime.isAfter(WORKDAY_END.minusMinutes(30))) continue;
                    
                    currentRoute.setStartTime(nextStartTime);

                    // get the current location for distance calculations
                    final class LocationWrapper {
                        POI location;
                        LocationWrapper(POI location) { this.location = location; }
                    }
                    LocationWrapper currentLocation = new LocationWrapper(hub);

                    // fills in the route with the closest requests
                    while (!hubRequests.isEmpty() && currentRoute.getStops().size() < MAX_DELIVERIES_PER_SESSION) {
                        DeliveryRequest nextRequest = hubRequests.stream()
                            .min((r1, r2) -> Double.compare(
                                calculateDistance(currentLocation.location, r1.getDropoffLocation()),
                                calculateDistance(currentLocation.location, r2.getDropoffLocation())))
                            .orElse(null);

                        if (nextRequest == null) break;

                        // check if adding this stop would violate the end of day limit
                        LocalTime estimatedCompletion = calculateEstimatedArrival(
                            currentLocation.location, 
                            nextRequest.getDropoffLocation(), 
                            currentRoute.getEstimatedCompletionTime());
                            
                        if (estimatedCompletion.isAfter(WORKDAY_END)) {
                            hubRequests.remove(nextRequest);
                            continue;
                        }

                        if (currentRoute.canAddStop(nextRequest.getDropoffLocation(), nextRequest.getPreferredTime())) {
                            currentRoute.addStop(nextRequest.getDropoffLocation(), nextRequest);
                            assignedRequests.add(nextRequest);
                            unassignedRequests.remove(nextRequest);
                            currentLocation.location = nextRequest.getDropoffLocation();
                            hubRequests.remove(nextRequest);
                        } else {
                            hubRequests.remove(nextRequest);
                        }
                    }

                    // add route if it has stops
                    if (!currentRoute.getStops().isEmpty()) {
                        driverRoutes.get(bestDriver).add(currentRoute);
                        optimizedRoutes.add(currentRoute);
                    }
                }
            }
        }

        // tries to assign any left over requests to any available driver until 8 PM
        while (!unassignedRequests.isEmpty()) {
            DeliveryRequest request = unassignedRequests.get(0);
            POI hub = request.getPickupHub();
            
            // find any driver who can take this request
            Driver availableDriver = availableDrivers.stream()
                .filter(d -> {
                    LocalTime lastCompletionTime = driverRoutes.get(d).stream()
                        .map(Route::getEstimatedCompletionTime)
                        .max(LocalTime::compareTo)
                        .orElse(WORKDAY_START);
                    
                    return driverRoutes.get(d).stream()
                           .mapToInt(r -> r.getStops().size())
                           .sum() < MAX_DELIVERIES_PER_DRIVER &&
                           lastCompletionTime.isBefore(WORKDAY_END.minusMinutes(30));
                })
                .min((d1, d2) -> {
                    int load1 = driverRoutes.get(d1).stream().mapToInt(r -> r.getStops().size()).sum();
                    int load2 = driverRoutes.get(d2).stream().mapToInt(r -> r.getStops().size()).sum();
                    return Integer.compare(load1, load2);
                })
                .orElse(null);
                
            if (availableDriver == null) break;
            
            // generate a route for this driver
            Route currentRoute = new Route(availableDriver, hub, roadNetwork);
            LocalTime nextStartTime = calculateNextStartTime(driverRoutes.get(availableDriver));
            
            if (nextStartTime == null || nextStartTime.isAfter(WORKDAY_END.minusMinutes(30))) {
                unassignedRequests.remove(request);
                continue;
            }
            
            currentRoute.setStartTime(nextStartTime);
            
            if (currentRoute.canAddStop(request.getDropoffLocation(), request.getPreferredTime())) {
                currentRoute.addStop(request.getDropoffLocation(), request);
                assignedRequests.add(request);
                unassignedRequests.remove(request);
                driverRoutes.get(availableDriver).add(currentRoute);
                optimizedRoutes.add(currentRoute);
            } else {
                unassignedRequests.remove(request);
            }
        }

        // store the optimized plan
        deliveryPlans.put(date, optimizedRoutes);
        
        // log delivery statistics
        int totalRequests = requests.size();
        int deliveredRequests = assignedRequests.size();
        System.out.println("Plan generation completed:");
        System.out.println("Total requests: " + totalRequests);
        System.out.println("Delivered requests: " + deliveredRequests);
        System.out.println("Success rate: " + String.format("%.1f%%", 
            (deliveredRequests * 100.0 / totalRequests)));
    }

    

    private double calculateDistance(POI p1, POI p2) {
        return Math.sqrt(Math.pow(p2.getX() - p1.getX(), 2) + 
                        Math.pow(p2.getY() - p1.getY(), 2));
    }

    



    private LocalTime calculateEstimatedArrival(POI from, POI to, LocalTime startTime) {
        if (from == null || to == null) {
            return startTime;
        }

        //get optimal path considering traffic
        List<String> path = roadNetwork.findOptimalPath(from.name, to.name, startTime);
        // calculate total distance with traffic considerations
        double totalDistance = calculatePathDistance(path, startTime);
        //calculate travel time considering traffic
        double trafficMultiplier = trafficManager.getTrafficMultiplier(startTime);
        double travelTimeHours = (totalDistance / AVERAGE_SPEED) * trafficMultiplier;
        //convert to minutes and add to start time
        long minutesToAdd = (long) (travelTimeHours * 60);
        return startTime.plusMinutes(minutesToAdd);
    }

    private double calculatePathDistance(List<String> path, LocalTime time) {
        double totalDistance = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            String from = path.get(i);
            String to = path.get(i + 1);
            Graph.Road road = roadNetwork.getRoad(from, to);
            totalDistance += road.getCurrentDistance(time, trafficManager);
        }
        return totalDistance;
    }

    private LocalTime calculateNextStartTime(List<Route> routes) {
        LocalTime nextTime = routes.stream()
            .map(Route::getEstimatedCompletionTime)
            .max(LocalTime::compareTo)
            .orElse(WORKDAY_START);
        
        // sdd a small delay between routes (15 minutes)
        nextTime = nextTime.plusMinutes(15);
        
        // if the start time is after 8 PM it would return null to indicate that there can be no more routes created
        // but if its before 8 we can still create routes but it might slightly go over the 8 pm limit
        return nextTime.isAfter(WORKDAY_END) ? null : nextTime;
    }

    private List<Driver> getAvailableDrivers(LocalDate date) {
        return drivers.stream()
            .filter(driver -> driver.isAvailable(date))
            .collect(Collectors.toList());
    }

    //Stats and reporting
    public void showDeliveryPlan(LocalDate date) {
        List<Route> routes = deliveryPlans.getOrDefault(date, new ArrayList<>());
        if (routes.isEmpty()) {
            System.out.println("No delivery plans for " + date);
            return;
        }

        //group routes for a driver
        Map<Driver, List<Route>> routesByDriver = routes.stream()
            .collect(Collectors.groupingBy(Route::getDriver));

        System.out.println("\nDelivery Plan for " + date + ":");
        
        routesByDriver.forEach((driver, driverRoutes) -> {
            System.out.println("\nDriver: " + driver.getName());
            LocalTime currentTime = WORKDAY_START;
            
            for (Route route : driverRoutes) {
                if (route.getStartTime().isAfter(currentTime.plusMinutes(30))) {
                    System.out.println("--- Driver starts new session at " + route.getStartTime() + " ---");
                }
                
                route.getStops().forEach(stop -> {
                    String details = stop.request != null ?
                        String.format(" (Urgency: %d, Size: %s, Weight: %.1fkg) [Package ID: %s]",
                            stop.request.urgencyLevel,
                            stop.request.size,
                            stop.request.weight,
                            stop.request.id) :
                        " (Hub)";
                    
                    System.out.printf("  %s - ETA: %s%s%n",
                        stop.location.name,
                        stop.estimatedArrival,
                        details);
                });
                
                currentTime = route.getEstimatedCompletionTime();
            }
            System.out.println("Estimated Completion: " + currentTime);
            //calculate the total distance a driver will travel in total dring all sessions and routes
            double totalDriverDistance = driverRoutes.stream()
                .mapToDouble(Route::getTotalDistance)
                .sum();
            System.out.println("Total Distance: " + 
                String.format("%.2f", totalDriverDistance) + " km");
        });
    }

    public void analyzeTrafficImpact(LocalDate date) {
        List<Route> routes = deliveryPlans.get(date);
        if (routes == null) return;

        //stoe all deliveries for each driver in a map
        Map<Driver, int[]> driverDeliveries = new HashMap<>();

        routes.forEach(route -> {
            Driver driver = route.getDriver();
            driverDeliveries.putIfAbsent(driver, new int[]{0, 0});
            int[] deliveryCounts = driverDeliveries.get(driver);
            
            for (Route.DeliveryStop stop : route.getStops()) {
                if (stop.request != null) { // we only count deliveries/travels to lockers not hubs
                    if (trafficManager.isPeakHour(stop.estimatedArrival)) {
                        deliveryCounts[0]++;
                    } else {
                        deliveryCounts[1]++;
                    }
                }
            }
        });

        System.out.println("\nTraffic Impact Analysis for " + date + ":");
        driverDeliveries.forEach((driver, counts) -> {
            System.out.printf("Driver %s: %d peak hour deliveries, %d off-peak deliveries%n",
                driver.getName(), counts[0], counts[1]);
        });
    }

    public int getDriverCount() {
        return drivers.size();
    }

    //shows a set of metrics/summaries that help see what happened during the "day"
    public void showPerformanceMetrics(long networkBuildTime, long requestCreationTime, long planGenerationTime) {
        System.out.println("\n=== Performance Metrics ===");
        System.out.println("System Scale:");
        System.out.println("  Number of Drivers: " + drivers.size());
        System.out.println("  Number of Hubs: " + hubs.size());
        System.out.println("  Number of Lockers: " + lockers.size());
        System.out.println("  Total Delivery Requests: " + 
            deliveryRequests.values().stream()
                .mapToInt(List::size)
                .sum());
        
        System.out.println("\nTiming Metrics:");
        System.out.println("  Network Build Time: " + networkBuildTime + "ms");
        System.out.println("  Request Creation Time: " + requestCreationTime + "ms");
        System.out.println("  Plan Generation Time: " + planGenerationTime + "ms");
    }

    //shows the delivery plan for a given day
    public void displayDeliveryPlan(LocalDate date) {
        List<Route> routes = deliveryPlans.get(date);
        List<DeliveryRequest> allRequests = deliveryRequests.getOrDefault(date, new ArrayList<>());
        
        if (routes == null || routes.isEmpty()) {
            System.out.println("No delivery plan found for " + date);
            return;
        }

        System.out.println("\nDelivery Plan for " + date + ":\n");
        
        //groups the routes for each driver
        Map<Driver, List<Route>> routesByDriver = new HashMap<>();
        for (Route route : routes) {
            routesByDriver.computeIfAbsent(route.getDriver(), k -> new ArrayList<>()).add(route);
        }

        //tracks the delivered packages
        Set<String> deliveredPackages = new HashSet<>();

        // displays the routes for each driver
        for (Map.Entry<Driver, List<Route>> entry : routesByDriver.entrySet()) {
            Driver driver = entry.getKey();
            List<Route> driverRoutes = entry.getValue();
            
            System.out.println("Driver: " + driver.getName());
            
            LocalTime lastSessionStart = null;
            for (Route route : driverRoutes) {
                //is a new session is started, this is printed
                if (lastSessionStart == null || !lastSessionStart.equals(route.getStartTime())) {
                    System.out.println("--- Driver starts new session at " + 
                        route.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " ---");
                    lastSessionStart = route.getStartTime();
                }
                
                //display data for rach stop
                for (Route.DeliveryStop stop : route.getStops()) {
                    System.out.printf("  %s - ETA: %s (Urgency: %d, Size: %s, Weight: %.1fkg) [Package ID: %s]%n",
                        stop.location.getName(),
                        stop.estimatedArrival.format(DateTimeFormatter.ofPattern("HH:mm")),
                        stop.request.getUrgencyLevel(),
                        stop.request.getSize(),
                        stop.request.getWeight(),
                        stop.request.getId());
                    deliveredPackages.add(stop.request.getId());
                }
            }
            
            //print info about the last route
            Route lastRoute = driverRoutes.get(driverRoutes.size() - 1);
            System.out.println("Estimated Completion: " + 
                lastRoute.getEstimatedCompletionTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            //calculate total distance across all routes for this driver
            double totalDriverDistance = driverRoutes.stream()
                .mapToDouble(Route::getTotalDistance)
                .sum();
            System.out.printf("Total Distance: %.2f km%n%n", totalDriverDistance);
        }

        //display driver info
        int totalPackages = allRequests.size();
        int deliveredCount = deliveredPackages.size();
        double successRate = (totalPackages > 0) ? (deliveredCount * 100.0 / totalPackages) : 0;
        
        System.out.println("=== Delivery Statistics ===");
        System.out.printf("Successfully scheduled: %d out of %d packages (%.1f%%)%n", 
            deliveredCount, totalPackages, successRate);
        
        if (deliveredCount < totalPackages) {
            System.out.println("\nUndelivered Packages:");
            allRequests.stream()
                .filter(req -> !deliveredPackages.contains(req.getId()))
                .forEach(req -> System.out.printf("  - Package %s (Urgency: %d, From: %s, To: %s)%n",
                    req.getId(), req.getUrgencyLevel(), 
                    req.getPickupHub().getName(), 
                    req.getDropoffLocation().getName()));
        }
        System.out.println();
    }

    //update the traffic impact data
    private void displayTrafficImpact(LocalDate date) {
        System.out.println("Traffic Impact Analysis for " + date + ":");
        Map<Driver, int[]> driverDeliveries = new HashMap<>(); // [peak, off-peak]
        
        for (Route route : deliveryPlans.get(date)) {
            Driver driver = route.getDriver();
            driverDeliveries.putIfAbsent(driver, new int[]{0, 0});
            
            for (Route.DeliveryStop stop : route.getStops()) {
                if (roadNetwork.getTrafficManager().isPeakHour(stop.estimatedArrival)) {
                    driverDeliveries.get(driver)[0]++;
                } else {
                    driverDeliveries.get(driver)[1]++;
                }
            }
        }
        
        for (Map.Entry<Driver, int[]> entry : driverDeliveries.entrySet()) {
            System.out.printf("Driver %s: %d peak hour deliveries, %d off-peak deliveries%n",
                entry.getKey().getName(), entry.getValue()[0], entry.getValue()[1]);
        }
        System.out.println();
    }

    public static void generateRandomRequests(DeliverySystem system, LocalDate date, 
                                          List<POI> hubs, List<POI> lockers) {
        Random random = new Random();
        // clears existing requests for this date
        system.deliveryRequests.computeIfAbsent(date, k -> new ArrayList<>()).clear();
        
        // cap the number of requests to 25
        int maxRequestsPerDriver = 25;
        int totalRequests = Math.min(random.nextInt(251), // 0 to 251 requests
                                    system.getDriverCount() * maxRequestsPerDriver);
        
        System.out.println("Generating " + totalRequests + " random delivery requests...");

        String[] sizes = {"Small", "Medium", "Large"};
        double[] weights = {2.0, 5.0, 10.0};

        for (int i = 0; i < totalRequests; i++) {
            POI pickupHub = hubs.get(random.nextInt(hubs.size()));
            POI dropoffLocker = lockers.get(random.nextInt(lockers.size()));
            
            // generate urgency level 1 to 5
            int urgencyLevel = random.nextInt(5) + 1;
            
            // generate preferree time between operating times
            LocalTime preferredTime = LocalTime.of(
                random.nextInt(13) + 8, // 8 AM to 8 PM
                random.nextInt(4) * 15  // 0, 15, 30, or 45 minutes
            );

            DeliveryRequest request = new DeliveryRequest(
                String.format("REQ-%03d", i),
                pickupHub,
                dropoffLocker,
                urgencyLevel,
                date,
                preferredTime,
                weights[random.nextInt(weights.length)],
                sizes[random.nextInt(sizes.length)]
            );

            system.addDeliveryRequest(request);
        }
    }

    private static void testDeliveryPlanGeneration(DeliverySystem system, LocalDate date) {
        System.out.println("\n=== Testing Delivery Plan Generation ===");
        
        // Test plan generation with varying load
        int[] testLoads = {5, 15, 25}; // Light, Medium, Heavy loads
        
        for (int load : testLoads) {
            System.out.printf("\nTesting with %d requests:%n", load);
            
            // Clear existing requests
            system.deliveryRequests.get(date).clear();
            
            // Generate specific number of requests
            generateRandomRequests(system, date, system.hubs, system.lockers);
            while (system.deliveryRequests.get(date).size() > load) {
                system.deliveryRequests.get(date).remove(system.deliveryRequests.get(date).size() - 1);
            }
            
            // Generate and analyze plan
            long startTime = System.currentTimeMillis();
            system.generateOptimizedDeliveryPlan(date);
            long endTime = System.currentTimeMillis();
            
            // Analyze results
            List<Route> routes = system.deliveryPlans.get(date);
            int totalDeliveries = routes.stream()
                .mapToInt(r -> r.getStops().size())
                .sum();
            
            System.out.println("Plan generation time: " + (endTime - startTime) + "ms");
            System.out.println("Number of routes created: " + routes.size());
            System.out.println("Total deliveries scheduled: " + totalDeliveries);
            System.out.println("Average deliveries per route: " + 
                String.format("%.2f", (double)totalDeliveries / routes.size()));
        }
    }
    
    private static void testPlanOptimization(DeliverySystem system, LocalDate date) {
        System.out.println("\n=== Testing Plan Optimization ===");
        
        // Generate a mix of urgent and non-urgent requests
        system.deliveryRequests.get(date).clear();
        
        // Add some urgent requests
        for (int i = 0; i < 5; i++) {
            POI hub = system.hubs.get(0);
            POI locker = system.lockers.get(i % system.lockers.size());
            DeliveryRequest urgentRequest = new DeliveryRequest(
                "URG-" + i,
                hub,
                locker,
                5, // Highest urgency
                date,
                LocalTime.of(9, 0), // Early morning
                5.0,
                "Medium"
            );
            system.addDeliveryRequest(urgentRequest);
        }
        
        // Add some non-urgent requests
        for (int i = 0; i < 10; i++) {
            POI hub = system.hubs.get(system.hubs.size() - 1);
            POI locker = system.lockers.get(i % system.lockers.size());
            DeliveryRequest nonUrgentRequest = new DeliveryRequest(
                "REG-" + i,
                hub,
                locker,
                2, // Lower urgency
                date,
                LocalTime.of(14, 0), // Afternoon
                5.0,
                "Medium"
            );
            system.addDeliveryRequest(nonUrgentRequest);
        }
        
        // Generate plan and analyze priorities
        system.generateOptimizedDeliveryPlan(date);
        List<Route> routes = system.deliveryPlans.get(date);
        
        System.out.println("\nAnalyzing delivery priorities:");
        Map<Integer, List<LocalTime>> deliveryTimesByUrgency = new HashMap<>();
        
        for (Route route : routes) {
            for (Route.DeliveryStop stop : route.getStops()) {
                if (stop.request != null) {
                    deliveryTimesByUrgency
                        .computeIfAbsent(stop.request.getUrgencyLevel(), k -> new ArrayList<>())
                        .add(stop.estimatedArrival);
                }
            }
        }
        
        // Print analysis
        deliveryTimesByUrgency.forEach((urgency, times) -> {
            OptionalDouble avgTime = times.stream()
                .mapToDouble(t -> t.getHour() * 60 + t.getMinute())
                .average();
            
            System.out.printf("Urgency Level %d:%n", urgency);
            System.out.printf("  Number of deliveries: %d%n", times.size());
            System.out.printf("  Average delivery time: %02d:%02d%n", 
                (int)avgTime.orElse(0) / 60,
                (int)avgTime.orElse(0) % 60);
        });
    }

    public static void main(String[] args) {
        System.out.println("Starting Delivery System Tests...\n");
        DeliverySystem system = new DeliverySystem();
        
        long startTime = System.currentTimeMillis();
        long networkBuildTime, requestCreationTime, planGenerationTime;
        
        try {
            System.out.println("Setting up POIs...");
            
            //add hubs
            List<POI> hubs = Arrays.asList(
                new POI("Central Hub", "123 Main St", true),
                new POI("North Hub", "456 North Ave", true),
                new POI("South Hub", "789 South Blvd", true),
                new POI("East Hub", "321 East St", true),
                new POI("West Hub", "654 West Ave", true)
            );
            
            //adds lockers
            List<POI> lockers = Arrays.asList(
                new POI("Agia Triada Beach", "Leoforos Megalou Alexandrou", false),
                new POI("Agia Triada Monastery", "Leoforos Konstantinou Karamanli", false),
                new POI("Taverna O Nikos", "Nikolaou Plastira", false),
                new POI("Café Ammos", "Leoforos Georgikis Scholis", false),
                new POI("Agia Triada Square", "Leoforos Ethnikis Antistaseos", false),
                new POI("Waterfront Promenade", "Leoforos Nikis", false),
                new POI("Local Market", "Aristotelous", false),
                new POI("Saint George Church", "Agiou Dimitriou", false),
                new POI("Fisherman's Wharf", "Leoforos Vasileos Georgiou", false),
                new POI("Sunset Viewpoint", "Tsimiski", false)
            );

            //add POIs to system
            for (POI hub : hubs) system.addHub(hub);
            for (POI locker : lockers) system.addLocker(locker);

            //biulds the road network
            System.out.println("Building road network...");
            startTime = System.currentTimeMillis();
            
            //initialize all POIs in the graph
            for (POI poi : hubs) system.roadNetwork.initializePOI(poi.getName());
            for (POI poi : lockers) system.roadNetwork.initializePOI(poi.getName());
            
            // Connect hubs to lockers with realistic distances
            Random random = new Random(42);
            for (POI hub : hubs) {
                for (POI locker : lockers) {
                    double distance = 5.0 + random.nextDouble() * 20.0; // 5-25km
                    system.roadNetwork.addRoad(hub.getName(), locker.getName(), distance);
                }
            }
            
            //connect hubs to each other
            for (int i = 0; i < hubs.size(); i++) {
                for (int j = i + 1; j < hubs.size(); j++) {
                    double distance = 15.0 + random.nextDouble() * 20.0; // 15-35km
                    system.roadNetwork.addRoad(hubs.get(i).getName(), hubs.get(j).getName(), distance);
                }
            }
            
            // connect nearby lockers
            for (int i = 0; i < lockers.size(); i++) {
                for (int j = i + 1; j < lockers.size(); j++) {
                    if (random.nextDouble() < 0.3) { // 30% chance of connection
                        double distance = 3.0 + random.nextDouble() * 12.0; // 3-15km
                        system.roadNetwork.addRoad(lockers.get(i).getName(), lockers.get(j).getName(), distance);
                    }
                }
            }

            networkBuildTime = System.currentTimeMillis() - startTime;

            // add drivers
            System.out.println("Adding drivers...");
            LocalDate today = LocalDate.now();
            
            String[] driverNames = {
                "John Doe", "Jane Smith", "Bob Wilson", "Alice Brown", "Charlie Davis",
                "Diana Miller", "Edward Jones", "Fiona White", "George Black", "Helen Green"
            };
            
            for (int i = 0; i < driverNames.length; i++) {
                Driver driver = new Driver("D" + (i + 1), driverNames[i]);
                driver.addAvailability(today);
                system.addDriver(driver);
            }

            // Generate and process delivery requests
            System.out.println("\nTesting delivery system with random requests...");
            startTime = System.currentTimeMillis();
            generateRandomRequests(system, today, hubs, lockers);
            requestCreationTime = System.currentTimeMillis() - startTime;
            
            // Generate delivery plan
            startTime = System.currentTimeMillis();
            if (!system.deliveryPlans.containsKey(today)) {
                system.generateOptimizedDeliveryPlan(today);
            }
            planGenerationTime = System.currentTimeMillis() - startTime;

            //show final results
            System.out.println("\n=== Test Results ===");
            system.displayDeliveryPlan(today);
            system.displayTrafficImpact(today);
            system.showPerformanceMetrics(networkBuildTime, requestCreationTime, planGenerationTime);

            //testDeliveryPlanGeneration(system, today); UNCOMMENT FOR TESTING!!!
            //testPlanOptimization(system, today);

        } catch (Exception e) {
            System.err.println("Error during test execution: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

