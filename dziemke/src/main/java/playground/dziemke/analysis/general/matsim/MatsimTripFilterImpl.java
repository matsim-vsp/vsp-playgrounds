package playground.dziemke.analysis.general.matsim;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import playground.dziemke.analysis.general.Trip;
import playground.dziemke.analysis.general.TripFilter;
import playground.dziemke.utils.ShapeFileUtils;

import java.util.*;
import java.util.regex.Pattern;

/**
 * @author gthunig on 04.04.2017.
 */
public class MatsimTripFilterImpl implements TripFilter {
    public static final Logger log = Logger.getLogger(MatsimTripFilterImpl.class);

    // Parameters
    private boolean onlyAnalyzeTripsWithMode;
    private List<String> modes;

    private boolean onlyAnalyzeTripInteriorOfArea; // formerly results labelled as "int"
    private boolean onlyAnalyzeTripsStartingOrEndingInArea; // formerly results labelled as "ber" (Berlin-based) <----------
    private int areaId;

    private boolean onlyAnalyzeTripsInDistanceRange; // "dist"; usually varied for analysis // <----------
    private double minDistance_km = -1;
    private double maxDistance_km = -1;

    private boolean onlyAnalyzeTripsWithActivityTypeBeforeTrip;
    private String activityTypeBeforeTrip;
    private boolean onlyAnalyzeTripsWithActivityTypeAfterTrip;
    private String activityTypeAfterTrip;
    
    private boolean excludeActivityType;
    private String excludedActivityType;

    private boolean onlyAnalyzeTripsDoneByPeopleInAgeRange; // "age"; this requires setting a CEMDAP file
    private int minAge = -1; // typically "x0"
    private int maxAge = -1; // typically "x9"; highest number usually chosen is 119
    
    private boolean onlyAnalyzeTripsInDepartureTimeWindow;
    private double minDepartureTime_s;
    private double maxDepartureTime_s;

    private Network network;
    private Geometry areaGeometry;


    private long tripCounter = 0;
    private long nextCounterMsg = 1;

    public MatsimTripFilterImpl() {
        log.info("Create Matsim Trip Filter");
    }

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
        Collection<SimpleFeature> features = (new ShapeFileReader()).readFileAndInitialize(areaShapeFile);
        areaGeometry = ShapeFileUtils.getGeometryByValueOfAttribute(features, "NR", String.valueOf(areaId));
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

    public void activateAge(int minAge, int maxAge) {
        onlyAnalyzeTripsDoneByPeopleInAgeRange = true;
        this.minAge = minAge;
        this.maxAge = maxAge;
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

    public List<? extends Trip> filter(List<? extends Trip> tripMap) {
        List<MatsimTrip> trips = new LinkedList<>();
        boolean printedWarn1 = false;
        boolean printedWarn2 = false;

        log.info("# of trips: " + tripMap.size());

        for (Trip currentTrip : tripMap) {

            processCounter(tripMap.size());

            MatsimTrip trip = (MatsimTrip)currentTrip;
            // Choose if trip will be considered
            if (onlyAnalyzeTripInteriorOfArea || onlyAnalyzeTripsStartingOrEndingInArea) {
                // get coordinates of links
                Id<Link> departureLinkId = trip.getDepartureLinkId();
                Id<Link> arrivalLinkId = trip.getArrivalLinkId();
                //
                Link departureLink = network.getLinks().get(departureLinkId);
                Link arrivalLink = network.getLinks().get(arrivalLinkId);

                // TODO use coords of toNode instead of center coord of link
                double arrivalCoordX = arrivalLink.getCoord().getX();
                double arrivalCoordY = arrivalLink.getCoord().getY();
                double departureCoordX = departureLink.getCoord().getX();
                double departureCoordY = departureLink.getCoord().getY();

                // create points
                Point arrivalLocation = MGC.xy2Point(arrivalCoordX, arrivalCoordY);
                Point departureLocation = MGC.xy2Point(departureCoordX, departureCoordY);

                if (onlyAnalyzeTripsStartingOrEndingInArea) {
                    if (!areaGeometry.contains(arrivalLocation) && !areaGeometry.contains(departureLocation)) {
                        continue;
                    }
                }
                if (onlyAnalyzeTripInteriorOfArea) {
                    if (onlyAnalyzeTripsStartingOrEndingInArea && !printedWarn1) {
                        log.warn("onlyAnalyzeTripInteriorOfArea and onlyAnalyzeTripsStartingOrEndingInArea activated at the same time!");
                        printedWarn1 = true;
                    }
                    if (!areaGeometry.contains(arrivalLocation) || !areaGeometry.contains(departureLocation)) {
                        continue;
                    }
                }
            }

            if (onlyAnalyzeTripsWithMode) {
                if (!modes.contains(trip.getLegMode())) {
                    continue;
                }
            }
            if (onlyAnalyzeTripsInDistanceRange && (trip.getDistanceBeeline_m() / 1000.) > maxDistance_km) {
                continue;
            }
            if (onlyAnalyzeTripsInDistanceRange && (trip.getDistanceBeeline_m() / 1000.) < minDistance_km) {
                continue;
            }
            if (onlyAnalyzeTripsWithActivityTypeBeforeTrip && onlyAnalyzeTripsWithActivityTypeAfterTrip && !printedWarn2) {
                log.warn("onlyAnalyzeTripsWithActivityTypeBeforeTrip and onlyAnalyzeTripsWithActivityTypeAfterTrip activated at the same time."
                        + "This may lead to results that are hard to interpret: rather not use these options simultaneously.");
                printedWarn2 = true;
            }
            if (onlyAnalyzeTripsWithActivityTypeBeforeTrip) {
                if (!trip.getActivityTypeBeforeTrip().equals(activityTypeBeforeTrip)) {
                    continue;
                }
            }
            if (onlyAnalyzeTripsWithActivityTypeAfterTrip) {
                if (!trip.getActivityTypeAfterTrip().equals(activityTypeAfterTrip)) {
                    continue;
                }
            }
            if (excludeActivityType) {
                if (trip.getActivityTypeAfterTrip().equals(excludedActivityType) || trip.getActivityTypeBeforeTrip().equals(excludedActivityType)) {
                    continue;
                }
            }
            if (onlyAnalyzeTripsInDepartureTimeWindow && (trip.getDepartureTime_s()) > maxDepartureTime_s) {
                continue;
            }
            if (onlyAnalyzeTripsInDepartureTimeWindow && (trip.getDepartureTime_s()) < minDepartureTime_s) {
                continue;
            }

			/* Only trips that fullfill all checked criteria are added; otherwise that loop would have been "continued" already */
            trips.add(trip);
        }

        log.info("Number of filtered trips: " + trips.size() + " ~ "
                + (double)Math.round((((double)trips.size()/tripMap.size())*100)*100)/100 + "% ");

        return trips;
    }

    private void processCounter(long tripSize) {
        this.tripCounter++;
        if (this.tripCounter == this.nextCounterMsg) {
            this.nextCounterMsg *= 4;
            Runtime rt = Runtime.getRuntime();
            log.info(" trip # " + this.tripCounter + " ~ "
                    + (double)Math.round((((double)tripCounter/tripSize)*100)*100)/100 + "% "
                    + "Allocated Memory: " + rt.totalMemory() / 1000000 + " MB");
        }
    }

    public String adaptOutputDirectory(String outputDirectory) {
        if (onlyAnalyzeTripsWithMode) {
        	for (String mode : modes) {
        		outputDirectory = outputDirectory + "_" + mode;
        	}
        }
        if (onlyAnalyzeTripInteriorOfArea) {
            outputDirectory = outputDirectory + "_inside-" + areaId;
        }
        if (onlyAnalyzeTripsStartingOrEndingInArea) {
            outputDirectory = outputDirectory + "_soe-in-" + areaId;
        }
        if (onlyAnalyzeTripsInDistanceRange) {
            outputDirectory = outputDirectory + "_dist-" + minDistance_km + "-" + maxDistance_km;
        }
        if (onlyAnalyzeTripsWithActivityTypeBeforeTrip) {
            outputDirectory = outputDirectory + "_act-bef-" + activityTypeBeforeTrip;
        }
        if (onlyAnalyzeTripsWithActivityTypeAfterTrip) {
            outputDirectory = outputDirectory + "_act-aft-" + activityTypeAfterTrip;
        }
        if (excludeActivityType) {
            outputDirectory = outputDirectory + "_no-act-" + excludedActivityType;
        }
        if (onlyAnalyzeTripsDoneByPeopleInAgeRange) {
            outputDirectory = outputDirectory + "_age-" + minAge + "-" + maxAge;
        }
        if (onlyAnalyzeTripsInDepartureTimeWindow) {
            outputDirectory = outputDirectory + "_dep-time-" + (minDepartureTime_s / 3600.) + "-" + (maxDepartureTime_s / 3600.);
        }
        return outputDirectory;
    }

    private boolean isOnlyAnalyzeTripsWithModeActivated(String identifier) {
        return identifier.contains("[") && identifier.contains("]");
    }

    private String[] getModesFrom(String identifier) {
        String modeContainingString = identifier.split(Pattern.quote("["))[1].split(Pattern.quote("]"))[0];
        return modeContainingString.split(", ");
    }

    private boolean isOnlyAnalyzeTripInteriorOfAreaActivated(String identifier) {
        return identifier.contains("_inside-");
    }

    private boolean isOnlyAnalyzeTripsStartingOrEndingInAreaActivated(String identifier) {
        return identifier.contains("_soe-in-");
    }

    private boolean isOnlyAnalyzeTripsInDistanceRangeActivated(String identifier) {
        return identifier.contains("_dist-");
    }

    private boolean isOnlyAnalyzeTripsWithActivityTypeBeforeTripActivated(String identifier) {
        return identifier.contains("_act-bef-");
    }

    private boolean isOnlyAnalyzeTripsWithActivityTypeAfterTripActivated(String identifier) {
        return identifier.contains("_act-aft-");
    }

    private boolean isOnlyAnalyzeTripsDoneByPeopleInAgeRangeActivated(String identifier) {
        return identifier.contains("_age-");
    }
    
    private boolean isExcludedActivityType(String identifier) {
        return identifier.contains("_no-act-");
    }
}
