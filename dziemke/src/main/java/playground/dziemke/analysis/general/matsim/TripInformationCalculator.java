package playground.dziemke.analysis.general.matsim;

import org.matsim.api.core.v01.network.Network;

import java.util.Collection;
import java.util.List;

/**
 * @author gthunig on 06.04.2017.
 */
class TripInformationCalculator {
    private Network network;
    private Collection<String> networkModes;

    public TripInformationCalculator(Network network, Collection<String> networkModes) {

        this.network = network;
        this.networkModes = networkModes;
    }

    public void calculateInformation(List<MatsimTrip> trips) {
        for (MatsimTrip trip : trips) {
            calculateInformation(trip);
        }
    }

    public void calculateInformation(MatsimTrip trip) {
        trip.setDuration_s(MatsimTripUtils.getDurationByCalculation_s(trip));
        trip.setDistanceBeeline_m(MatsimTripUtils.calculateBeelineDistance_m(trip, network));
        trip.setDistanceRouted_m(MatsimTripUtils.getDistanceRoutedByCalculation_m(trip, network, networkModes));
    }
}
