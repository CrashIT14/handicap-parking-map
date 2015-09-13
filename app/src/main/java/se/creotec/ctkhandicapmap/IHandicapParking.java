package se.creotec.ctkhandicapmap;

/**
 * @author Alexander Håkansson
 * @since 2015-09-13
 */
public interface IHandicapParking {
    String getID();
    String getName();
    double getLatitude();
    double getLongitude();
    long getTotalParkingCount();
    String getMaxParkingTime();
}
