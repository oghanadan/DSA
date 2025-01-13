import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

class Driver {
    private String id;
    private String name;
    private Map<LocalDate, Boolean> availability;
    private LocalTime lastDeliveryTime;

    public Driver(String id, String name) {
        this.id = id;
        this.name = name;
        this.availability = new HashMap<>();
        this.lastDeliveryTime = null;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void addAvailability(LocalDate date) {
        availability.put(date, true);
    }

    public boolean isAvailable(LocalDate date) {
        return availability.getOrDefault(date, false);
    }

    public LocalTime getLastDeliveryTime() {
        return lastDeliveryTime;
    }

    public void setLastDeliveryTime(LocalTime time) {
        this.lastDeliveryTime = time;
    }
}
