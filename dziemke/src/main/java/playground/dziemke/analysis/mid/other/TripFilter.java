package playground.dziemke.analysis.mid.other;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.geotools.MGC;
import playground.dziemke.utils.ShapeReader;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TripFilter {

    public static final Logger log = Logger.getLogger(TripFilter.class);

    // Parameters
    private boolean onlyAnalyzeTripsWithMode;
    private List<String> modes;

    private boolean onlyAnalyzeTripInteriorOfArea; // formerly results labelled as "int"
    private boolean onlyAnalyzeTripsStartingOrEndingInArea; // formerly results labelled as "ber" (Berlin-based) <----------
    private int areaId;
    private final String INT_AND_BER_WARNING = "onlyAnalyzeTripInteriorOfArea and onlyAnalyzeTripsStartingOrEndingInArea activated at the same time!";
    private boolean intAndBerWarningPrinted = false;

    private boolean onlyAnalyzeTripsInDistanceRange; // "dist"; usually varied for analysis // <----------
    private double minDistance_km = -1;
    private double maxDistance_km = -1;

    private boolean onlyAnalyzeTripsWithActivityTypeBeforeTrip;
    private String activityTypeBeforeTrip;
    private boolean onlyAnalyzeTripsWithActivityTypeAfterTrip;
    private String activityTypeAfterTrip;
    private final String ACT_BEFORE_AND_AFTER_WARNING = "onlyAnalyzeTripsWithActivityTypeBeforeTrip and " +
            "onlyAnalyzeTripsWithActivityTypeAfterTrip activated at the same time. " +
            "This may lead to results that are hard to interpret: rather not use these options simultaneously.";
    private boolean actBeforeAndAfterWarningPrinted = false;

    private boolean excludeActivityType;
    private String excludedActivityType;

    private boolean onlyAnalyzeTripsInDepartureTimeWindow;
    private double minDepartureTime_s;
    private double maxDepartureTime_s;

    private Network network;
    private Geometry areaGeometry;

    public void activateMode(String... mode) {
        onlyAnalyzeTripsWithMode = true;
        this.modes = Arrays.asList(mode);
    }

    public void activateInt(Network network, String areaShapeFile, int areaId) {
        this.areaId = areaId;
        assignNetwork(network);
        assignAreGeometry(areaShapeFile);
        this.onlyAnalyzeTripInteriorOfArea = true;
    }

    public void activateStartsOrEndsIn(Network network, String areaShapeFile, int areaId) {
        this.areaId = areaId;
        assignNetwork(network);
        assignAreGeometry(areaShapeFile);
        onlyAnalyzeTripsStartingOrEndingInArea = true;
    }

    private void assignNetwork(Network network) {
        this.network = network;
    }

    private void assignAreGeometry(String areaShapeFile) {
        Map<Integer, Geometry> zoneGeometries = ShapeReader.read(areaShapeFile, "NR");
        areaGeometry = zoneGeometries.get(areaId);
    }

    public void activateDist(double minDistance_km, double maxDistance_km) {
        onlyAnalyzeTripsInDistanceRange = true;
        this.minDistance_km = minDistance_km;
        this.maxDistance_km = maxDistance_km;
    }

    public void activateCertainActBefore(String activityTypeBeforeTrip) {
        onlyAnalyzeTripsWithActivityTypeBeforeTrip = true;
        this.activityTypeBeforeTrip = activityTypeBeforeTrip;
    }

    public void activateCertainActAfter(String activityTypeAfterTrip) {
        onlyAnalyzeTripsWithActivityTypeAfterTrip = true;
        this.activityTypeAfterTrip = activityTypeAfterTrip;
    }

    public void activateDepartureTimeRange(double minDepartureTime_s, double maxDepartureTime_s) {
        onlyAnalyzeTripsInDepartureTimeWindow = true;
        this.minDepartureTime_s = minDepartureTime_s;
        this.maxDepartureTime_s = maxDepartureTime_s;
    }

    public void activateExcludeActivityType(String excludedActivityType) {
        excludeActivityType = true;
        this.excludedActivityType = excludedActivityType;
    }

    public boolean isTripValid(Trip trip) {

        if (onlyAnalyzeTripInteriorOfArea || onlyAnalyzeTripsStartingOrEndingInArea) {
            // get coordinates of links
            Id<Link> departureLinkId = trip.getLeg().getRoute().getStartLinkId();
            Id<Link> arrivalLinkId = trip.getLeg().getRoute().getEndLinkId();
            //
            Link departureLink = network.getLinks().get(departureLinkId);
            Link arrivalLink = network.getLinks().get(arrivalLinkId);

            // TODO use coords of activities instead of center coord of link
            double arrivalCoordX = arrivalLink.getCoord().getX();
            double arrivalCoordY = arrivalLink.getCoord().getY();
            double departureCoordX = departureLink.getCoord().getX();
            double departureCoordY = departureLink.getCoord().getY();

            // create points
            Point arrivalLocation = MGC.xy2Point(arrivalCoordX, arrivalCoordY);
            Point departureLocation = MGC.xy2Point(departureCoordX, departureCoordY);

            if (onlyAnalyzeTripsStartingOrEndingInArea) {
                if (!areaGeometry.contains(arrivalLocation) && !areaGeometry.contains(departureLocation)) {
                    return false;
                }
            }
            if (onlyAnalyzeTripInteriorOfArea && onlyAnalyzeTripsStartingOrEndingInArea) {
                printIntAndBerWarnIfFirstTime();
                if (!areaGeometry.contains(arrivalLocation) || !areaGeometry.contains(departureLocation)) {
                    return false;
                }
            }
        }

        if (onlyAnalyzeTripsWithMode) {
            if (!modes.contains(trip.getLeg().getMode())) {
                return false;
            }
        }
        if (onlyAnalyzeTripsInDistanceRange) {
            if (trip.getBeelineDistance_km() > maxDistance_km) return false;
            if (trip.getBeelineDistance_km() < minDistance_km) return false;
        }
        if (onlyAnalyzeTripsWithActivityTypeBeforeTrip && onlyAnalyzeTripsWithActivityTypeAfterTrip) {
            printActBeforeAndAfterWarnIfFirstTime();
        }
        if (onlyAnalyzeTripsWithActivityTypeBeforeTrip) {
            if (!trip.getActivityTypeBeforeTrip().equals(activityTypeBeforeTrip)) {
                return false;
            }
        }
        if (onlyAnalyzeTripsWithActivityTypeAfterTrip) {
            if (!trip.getActivityTypeAfterTrip().equals(activityTypeAfterTrip)) {
                return false;
            }
        }
        if (excludeActivityType) {
            if (trip.getActivityTypeAfterTrip().equals(excludedActivityType) || trip.getActivityTypeBeforeTrip().equals(excludedActivityType)) {
                return false;
            }
        }
        if (onlyAnalyzeTripsInDepartureTimeWindow) {

            if ((trip.getDepartureTime_h() * 3600) > maxDepartureTime_s) return false;
        }
        if (onlyAnalyzeTripsInDepartureTimeWindow) {

            if ((trip.getDepartureTime_h() * 3600) < minDepartureTime_s) return false;
        }

        return true;
    }

    private void printIntAndBerWarnIfFirstTime() {

        if (!intAndBerWarningPrinted) {

            log.warn(INT_AND_BER_WARNING);
            intAndBerWarningPrinted = true;
        }
    }

    private void printActBeforeAndAfterWarnIfFirstTime() {

        if (!actBeforeAndAfterWarningPrinted) {

            log.warn(ACT_BEFORE_AND_AFTER_WARNING);
            actBeforeAndAfterWarningPrinted = true;
        }
    }

}
