package playground.dziemke.analysis.general.matsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
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

    public Population2TripsParser(Population population, Network network, Collection<String> networkModes) {

        this.population = population;
        this.tripInformationCalculator = new TripInformationCalculator(network, networkModes);
    }

    public List<MatsimTrip> parse() {

        List<MatsimTrip> trips = new ArrayList<>();

        log.warn("Assuming a experiencedPlans file was read so that there should only be a selectedPLan available.");
        for (Person currentPerson : population.getPersons().values()) {

            Plan selectedPlan = currentPerson.getSelectedPlan();
            for (int i = 0; i < selectedPlan.getPlanElements().size(); i++) {

                if (selectedPlan.getPlanElements().get(i) instanceof Leg) {

                    Leg leg = (Leg)selectedPlan.getPlanElements().get(i);
                    MatsimTrip trip = new MatsimTrip();
                    trip.setPersonId(currentPerson.getId());
                    trip.setTripId(getNewTripId(currentPerson.getId()));
                    trip.setActivityTypeBeforeTrip(getActivityTypeBeforeTrip(selectedPlan, i));
                    trip.setDepartureTime_s(leg.getDepartureTime());
                    trip.setArrivalTime_s(leg.getDepartureTime()+leg.getTravelTime());
                    trip.setLegMode(leg.getMode());
                    trip.setDuration_s(leg.getTravelTime());
                    trip.setActivityTypeAfterTrip(getActivityTypeAfterTrip(selectedPlan, i));

                    trip.setDepartureLinkId(leg.getRoute().getStartLinkId());
                    trip.setLinks(getRoute(leg));
                    trip.setArrivalLinkId(leg.getRoute().getEndLinkId());

                    tripInformationCalculator.calculateInformation(trip);

                    trips.add(trip);
                }

            }

        }
        return trips;
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
        return ((Activity) precedingElement).getType();
    }

    private String getActivityTypeAfterTrip(Plan selectedPlan, int i) {

        PlanElement followingElement = selectedPlan.getPlanElements().get(i+1);
        return ((Activity) followingElement).getType();
    }

}
