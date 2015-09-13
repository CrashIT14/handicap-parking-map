package se.creotec.ctkhandicapmap;

/**
 * @author Alexander Håkansson
 * @since 2015-09-13
 */
public class HandicapParking implements IHandicapParking {

    private final String id, name, maxParkingTime;
    private final double lat, lng;
    private final long parkingCount;

    public HandicapParking(String id, String name, double lat, double lng,
                           long parkingCount, String maxParkingTime) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.parkingCount = parkingCount;
        this.maxParkingTime = maxParkingTime;
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double getLatitude() {
        return lat;
    }

    @Override
    public double getLongitude() {
        return lng;
    }

    @Override
    public long getTotalParkingCount() {
        return parkingCount;
    }

    @Override
    public String getMaxParkingTime() {
        return maxParkingTime;
    }
}
