package playground.dziemke.analysis.general.matsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.routes.NetworkRoute;
import playground.dziemke.analysis.general.Trip;

import java.util.*;

public class Population2TripsParser {
    public static final Logger log = Logger.getLogger(Population2TripsParser.class);

    private Population population;
    private TripInformationCalculator tripInformationCalculator;
    private Map<Id<Person>, Integer> personToTripCount = new HashMap<>();

    private boolean normalizeActivities = true;

    List<String> MINOR_MODES = Arrays.asList(TransportMode.access_walk, TransportMode.egress_walk, TransportMode.transit_walk);

    public Population2TripsParser(Population population, Network network, Collection<String> networkModes) {

        this.population = population;
        this.tripInformationCalculator = new TripInformationCalculator(network, networkModes);
    }

    public List<MatsimTrip> parse() {

        List<MatsimTrip> trips = new ArrayList<>();

        log.warn("Assuming a experiencedPlans file was read so that there should only be a selectedPLan available.");
        for (Person currentPerson : population.getPersons().values()) {

            Plan selectedPlan = currentPerson.getSelectedPlan();
            boolean accessEgressWalkPatternUsed = false;
            List<MatsimTrip> storedTripsToCombine = new ArrayList<>();
            for (int i = 0; i < selectedPlan.getPlanElements().size(); i++) {



                if (selectedPlan.getPlanElements().get(i) instanceof Leg) {

                    Leg leg = (Leg)selectedPlan.getPlanElements().get(i);
                    MatsimTrip trip = new MatsimTrip();
                    trip.setPersonId(currentPerson.getId());
                    trip.setTripId(getNewTripId(currentPerson.getId()));
                    trip.setActivityTypeBeforeTrip(getActivityTypeBeforeTrip(selectedPlan, i));
					trip.setDepartureTime_s(leg.getDepartureTime().seconds());
					trip.setArrivalTime_s(leg.getDepartureTime().seconds() +leg.getTravelTime());
                    trip.setDuration_s(leg.getTravelTime());
                    trip.setActivityTypeAfterTrip(getActivityTypeAfterTrip(selectedPlan, i));

                    trip.setDepartureLinkId(leg.getRoute().getStartLinkId());
                    trip.setLinks(getRoute(leg));
                    trip.setArrivalLinkId(leg.getRoute().getEndLinkId());

                    // change "transit_walk" that occur without pt to "walk"
                    if (!accessEgressWalkPatternUsed && leg.getMode().equals(TransportMode.transit_walk))
                        trip.setLegMode(TransportMode.walk);
                    else trip.setLegMode(leg.getMode());

                    //decide if leg should be stored, combined and/or added
                    if (leg.getMode().equals(TransportMode.access_walk))
                        accessEgressWalkPatternUsed = true;

                    if (accessEgressWalkPatternUsed) {

                        storedTripsToCombine.add(trip);

                        if (leg.getMode().equals(TransportMode.egress_walk)) {

                            accessEgressWalkPatternUsed = false;
                            trip = combine(storedTripsToCombine);
                            storedTripsToCombine.clear();
                        }
                    }

                    if (!accessEgressWalkPatternUsed) {

                        tripInformationCalculator.calculateInformation(trip);
                        trips.add(trip);
                    }

                }

            }

        }
        return trips;
    }

    private MatsimTrip combine(List<MatsimTrip> tripsToCombine) {

        MatsimTrip combinedTrip = tripsToCombine.get(0);

        if (MINOR_MODES.contains(tripsToCombine.get(0).getLegMode()))
            combinedTrip.setLegMode(null);

        combinedTrip.setArrivalTime_s(tripsToCombine.get(tripsToCombine.size()-1).getArrivalTime_s());
        combinedTrip.setActivityTypeAfterTrip(tripsToCombine.get(tripsToCombine.size()-1).getActivityTypeAfterTrip());
        combinedTrip.setArrivalLinkId(tripsToCombine.get(tripsToCombine.size()-1).getArrivalLinkId());

        for (int i = 1; i < tripsToCombine.size(); i++) {

            //set main trip mode
            String currentLegMode = tripsToCombine.get(i).getLegMode();
            if (!MINOR_MODES.contains(currentLegMode)) {

                if (combinedTrip.getLegMode() != null && !combinedTrip.getLegMode().equals(currentLegMode))
                    log.error("Main mode could not be identified. More than one dominant mode.");
                combinedTrip.setLegMode(currentLegMode);
            }

            //combine duration
            combinedTrip.setDuration_s(combinedTrip.getDuration_s() + tripsToCombine.get(i).getDuration_s());
            //combine links
            List<Id<Link>> links = combinedTrip.getLinks();
            links.addAll(tripsToCombine.get(i).getLinks());
            combinedTrip.setLinks(links);
        }
        return combinedTrip;
    }

    private List<Id<Link>> getRoute(Leg leg) {

        if (leg.getRoute() instanceof NetworkRoute) {
            return ((NetworkRoute) leg.getRoute()).getLinkIds();
        } else
            return new ArrayList<>();
    }

    private Id<Trip> getNewTripId(Id<Person> personId) {

        Integer tripCount = personToTripCount.get(personId);
        if (tripCount == null) tripCount = 0;
        Id<Trip> tripId = Id.create(personId.toString() + "_" + tripCount, Trip.class);
        personToTripCount.put(personId, ++tripCount);
        return tripId;
    }

    private String getActivityTypeBeforeTrip(Plan selectedPlan, int i) {

        PlanElement precedingElement = selectedPlan.getPlanElements().get(i-1);
        String activityType = ((Activity) precedingElement).getType();
        if (normalizeActivities)
            activityType = normalize(activityType);
        return activityType;
    }

    private String getActivityTypeAfterTrip(Plan selectedPlan, int i) {

        PlanElement followingElement = selectedPlan.getPlanElements().get(i+1);

        String activityType = ((Activity) followingElement).getType();
        if (normalizeActivities)
            activityType = normalize(activityType);
        return activityType;
    }

    private String normalize(String activityType) {

        return activityType.split("_")[0];
    }

    public void setNormalizeActivities(boolean normalizeActivities) {
        this.normalizeActivities = normalizeActivities;
    }
}
