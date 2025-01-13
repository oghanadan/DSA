import java.time.LocalDate;
import java.time.LocalTime;
 
 
 // Enhanced DeliveryRequest with urgency levels and package details
class DeliveryRequest {
    String id;
    POI destination;
    int urgencyLevel; // 1-5, 5 being most urgent
    LocalDate deliveryDate;
    LocalTime preferredTime;
    POI pickupHub;
    double weight;
    String size;

    public DeliveryRequest(String id, POI pickupHub, POI destination, 
                          int urgencyLevel, LocalDate deliveryDate, 
                          LocalTime preferredTime, double weight, String size) {
        this.id = id;
        this.pickupHub = pickupHub;
        this.destination = destination;
        this.urgencyLevel = urgencyLevel;
        this.deliveryDate = deliveryDate;
        this.preferredTime = preferredTime;
        this.weight = weight;
        this.size = size;
    }

    // Getters
    public String getId() { return id; }
    public POI getPickupHub() { return pickupHub; }
    public POI getDropoffLocation() { return destination; }
    public LocalTime getPreferredTime() { return preferredTime; }
    public LocalDate getDeliveryDate() { return deliveryDate; }
    public int getUrgencyLevel() { return urgencyLevel; }
    public String getSize() { return size; }
    public double getWeight() { return weight; }
}
