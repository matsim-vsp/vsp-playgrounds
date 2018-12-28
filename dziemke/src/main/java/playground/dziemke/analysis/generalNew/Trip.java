package playground.dziemke.analysis.generalNew;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.List;

public class Trip {

    private final Logger log = Logger.getLogger(Trip.class);

    private final Activity activityBeforeTrip;
    private final Leg leg;
    private final Activity activityAfterTrip;

    private final String source;
    private final boolean useWeight;

    private Network network;

    private double beelineDistance_km = -1;
    private double travelTime_h = -1;
    private double speed_km_h = -1;
    private double departureTime_h = -1;

    private double routedDistance_km = -1;
    private double routedSpeed_km_h = -1;

    private String mode;

    private double weight = -1;

    public Trip(Activity activityBeforeTrip, Leg leg, Activity activityAfterTrip, String source, boolean useWeight) {

        this.activityBeforeTrip = activityBeforeTrip;
        this.leg = leg;
        this.activityAfterTrip = activityAfterTrip;
        this.source = source;
        this.useWeight = useWeight;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    private boolean isNetworkNeededButNotProvided() {

        return (source.equals(SurveyAdditionalAttributes.Source.MATSIM.name()) && network == null);
    }

    private void printNetworkNotProvidedWarning() {

        log.warn("The source of the population was identified as MATSim but there is no network provided.\n"
                + " Please provide a network with PopulationAnalyzer.setnetwork(Network network) so the calculated (routed and beeline) distance and speed can be analyzed");
    }

    public Activity getActivityBeforeTrip() {
        return activityBeforeTrip;
    }

    public Leg getLeg() {
        return leg;
    }

    public Activity getActivityAfterTrip() {
        return activityAfterTrip;
    }

    public double getBeelineDistance_km() {

        if (beelineDistance_km == -1) calculateBeelineDistance_km();
        return beelineDistance_km;
    }

    public double getTravelTime_h() {

        if (travelTime_h == -1) calculateTravelTime_h();
        return travelTime_h;
    }

    public double getSpeed_km_h() {

        if (speed_km_h == -1) calculateSpeed_km_h();
        return speed_km_h;
    }

    public double getDepartureTime_h() {

        if (departureTime_h == -1) calculateDepartureTime_h();
        return departureTime_h;
    }

    public double getRoutedDistance_km() {

        if (routedDistance_km == -1 && areLinkIdsPresent()) calculateRoutedDistance_km();
        return routedDistance_km;
    }

    public double getRoutedSpeed_km_h() {

        if (routedSpeed_km_h == -1 && areLinkIdsPresent()) calculateRoutedSpeed_km_h();
        return routedSpeed_km_h;
    }

    public String getActivityTypeBeforeTrip() {

        return activityBeforeTrip.getType();
    }

    public String getActivityTypeAfterTrip() {

        return activityAfterTrip.getType();
    }

    public double getWeight() {

        if (weight == -1) calculateWeight();
        return weight;
    }

    private void calculateWeight() {

        if (useWeight) {
            this.weight = SurveyAdditionalAttributesUtils.getWeight(leg);
        } else {
            this.weight = 1;
        }
    }

    private boolean areLinkIdsPresent() {

        Route route = leg.getRoute();
        return route instanceof NetworkRoute;
    }

    private void calculateRoutedSpeed_km_h() {

        double routedDistance_km = getRoutedDistance_km();
        double travelTime_h = getTravelTime_h();
        this.routedSpeed_km_h = routedDistance_km / travelTime_h;
    }

    private void calculateRoutedDistance_km() {

        Route route = leg.getRoute();
        assert route instanceof NetworkRoute;
        List<Id<Link>> linkIds =((NetworkRoute) route).getLinkIds();
        double tripDistance_m = 0;
        if (linkIds.isEmpty()) {
            log.warn("List of links is empty.");
            return;
        }
        for (Id<Link> linkId : linkIds) {
            Link link = network.getLinks().get(linkId);
            tripDistance_m += link.getLength();
        }

        this.routedDistance_km = tripDistance_m / 1000;
    }

    private void calculateSpeed_km_h() {

        double beelineDistance_km = getBeelineDistance_km();
        double travelTime_h = getTravelTime_h();
        speed_km_h = beelineDistance_km / travelTime_h;
    }

    private void calculateDepartureTime_h() {

        this.departureTime_h = leg.getDepartureTime() / 3600;
    }

    private void calculateTravelTime_h() {

        this.travelTime_h = leg.getTravelTime() / 3600;
    }

    private void calculateBeelineDistance_km() {

        if (source.equals(SurveyAdditionalAttributes.Source.MATSIM.name())) {
            //TODO: for the long term use beeline between activities for MATSim source
            assert network != null;
            this.beelineDistance_km = (calculateBeelineDistance_m(leg.getRoute().getStartLinkId(), leg.getRoute().getEndLinkId()) / 1000);
        } else {
            this.beelineDistance_km = (SurveyAdditionalAttributesUtils.getDistanceBeeline_m(leg) / 1000);
        }
    }

    @Deprecated
    private double calculateBeelineDistance_m(Id<Link> departureLinkId, Id<Link> arrivalLinkId) {

        Link departureLink = network.getLinks().get(departureLinkId);
        Link arrivalLink = network.getLinks().get(arrivalLinkId);

        return CoordUtils.calcEuclideanDistance(departureLink.getCoord(), arrivalLink.getCoord());
    }
}
