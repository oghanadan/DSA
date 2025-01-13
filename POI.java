// Modified POI class to include locker information
class POI {
    String name;
    String address;
    boolean isHub; // this is used to determine whether a POI is a hub or a locker, locker is false and hub is true
    private double x;
    private double y;

    public POI(String name, String address, boolean isHub) {
        this.name = name;
        this.address = address;
        this.isHub = isHub;
        // Initialize coordinates (can be set later if needed)
        this.x = 0.0;
        this.y = 0.0;
    }

    public String getName() { return name; }
    public double getX() { return x; }
    public double getY() { return y; }
    public void setCoordinates(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return name + (isHub ? " (Hub)" : " (Locker)");
    }
}