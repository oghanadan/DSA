import java.time.LocalTime;
import java.util.*;

class Route {
    private Driver driver;
    private POI startHub;
    private List<DeliveryStop> stops;
    private LocalTime startTime;
    private double totalDistance;
    private Graph roadNetwork;
    // max number of packages a driver can deliver in one shift
    private static final int MAX_DELIVERIES_PER_SESSION = 15;
    // end of delivery window, no deliveries after this
    private static final LocalTime WORKDAY_END = LocalTime.of(20, 0);
    
    static class DeliveryStop {
        POI location;
        DeliveryRequest request;
        LocalTime estimatedArrival;
        
        DeliveryStop(POI location, DeliveryRequest request, LocalTime estimatedArrival) {
            this.location = location;
            this.request = request;
            this.estimatedArrival = estimatedArrival;
        }
    }
    
    // creates new route starting from a hub
    public Route(Driver driver, POI startHub, Graph roadNetwork) {
        this.driver = driver;
        this.startHub = startHub;
        this.stops = new ArrayList<>();
        this.totalDistance = 0.0;
        this.roadNetwork = roadNetwork;
        this.startTime = LocalTime.of(8, 0); // All routes start at 8 AM
    }

    // adds new stop and sorts by priority
    public void addStop(POI location, DeliveryRequest request) {
        // figure out where to insert based on urgency
        int insertIndex = 0;
        for (; insertIndex < stops.size(); insertIndex++) {
            if (request.getUrgencyLevel() > stops.get(insertIndex).request.getUrgencyLevel()) {
                break;
            }
        }
        
        stops.add(insertIndex, new DeliveryStop(location, request, null));
        calculateRouteDetails();
    }

    // updates all times and distances for the route
    private void calculateRouteDetails() {
        totalDistance = 0;
        LocalTime currentTime = startTime;
        POI currentLocation = startHub;
        
        for (DeliveryStop stop : stops) {
            // skip calculation if we're already here
            if (currentLocation.equals(stop.location)) {
                stop.estimatedArrival = currentTime;
                continue;
            }
            
            double distance = calculateActualDistance(currentLocation, stop.location);
            totalDistance += distance;
            
            // 2 min per distance unit for travel
            currentTime = currentTime.plusMinutes((long)(distance * 2));
            
            // add traffic delay during rush hour
            if (roadNetwork.getTrafficManager().isPeakHour(currentTime)) {
                currentTime = currentTime.plusMinutes(15);
            }
            
            // handling time depends on package size
            int handlingTime = 5;
            if (stop.request != null && "Large".equals(stop.request.getSize())) {
                handlingTime = 8;
            }
            currentTime = currentTime.plusMinutes(handlingTime);
            
            stop.estimatedArrival = currentTime;
            currentLocation = stop.location;
        }
    }

    // checks if we can add another stop without breaking constraints
    public boolean canAddStop(POI location, LocalTime preferredTime) {
        // try adding stop temporarily to check time
        List<DeliveryStop> tempStops = new ArrayList<>(stops);
        tempStops.add(new DeliveryStop(location, null, null));
        
        LocalTime currentTime = startTime;
        POI currentLocation = startHub;
        
        for (DeliveryStop stop : tempStops) {
            if (!currentLocation.equals(stop.location)) {
                double distance = calculateActualDistance(currentLocation, stop.location);
                currentTime = currentTime.plusMinutes((long)(distance * 2));
                
                if (roadNetwork.getTrafficManager().isPeakHour(currentTime)) {
                    currentTime = currentTime.plusMinutes(30);
                }
                
                currentTime = currentTime.plusMinutes(10);
                currentLocation = stop.location;
            }
        }
        
        return stops.size() < MAX_DELIVERIES_PER_SESSION && 
               !currentTime.isAfter(WORKDAY_END);
    }

    // gets actual road distance considering traffic and alternate routes
    private double calculateActualDistance(POI from, POI to) {
        // try direct road first
        Graph.Road road = roadNetwork.getRoad(from, to);
        if (road != null) {
            return road.getCurrentDistance(startTime, roadNetwork.getTrafficManager());
        }
        
        // no direct road, look for alternate path
        List<String> path = roadNetwork.findOptimalPath(from.getName(), to.getName(), startTime);
        if (!path.isEmpty()) {
            double totalDistance = 0;
            for (int i = 0; i < path.size() - 1; i++) {
                Graph.Road pathRoad = roadNetwork.getRoad(path.get(i), path.get(i + 1));
                if (pathRoad != null) {
                    totalDistance += pathRoad.getCurrentDistance(startTime, roadNetwork.getTrafficManager());
                }
            }
            return totalDistance;
        }
        
        // fallback to straight line distance * 2 if no path exists
        return Math.sqrt(Math.pow(to.getX() - from.getX(), 2) + 
                       Math.pow(to.getY() - from.getY(), 2)) * 2.0;
    }
    
    // standard getters and setters below
    public Driver getDriver() { return driver; }
    public List<DeliveryStop> getStops() { return new ArrayList<>(stops); }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime time) { 
        this.startTime = time;
        calculateRouteDetails(); 
    }
    public double getTotalDistance() { return totalDistance; }
    public POI getStartHub() { return startHub; }
    public LocalTime getEstimatedCompletionTime() {
        if (stops.isEmpty()) return startTime;
        return stops.get(stops.size() - 1).estimatedArrival;
    }
}

