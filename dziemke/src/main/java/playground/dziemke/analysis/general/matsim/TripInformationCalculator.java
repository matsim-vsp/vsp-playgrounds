package playground.dziemke.analysis.general.matsim;

import org.matsim.api.core.v01.network.Network;

import java.util.Collection;
import java.util.List;

/**
 * @author gthunig on 06.04.2017.
 */
class TripInformationCalculator {

    static void calculateInformation(List<MatsimTrip> trips, Network network, Collection<String> networkModes) {
        for (MatsimTrip trip : trips)
            calculateInformation(trip, network, networkModes);
    }

    private static void calculateInformation(MatsimTrip trip, Network network, Collection<String> networkModes) {
        trip.setDuration_s(MatsimTripUtils.getDurationByCalculation_s(trip));
        trip.setDistanceBeeline_m(MatsimTripUtils.calculateBeelineDistance_m(trip, network));
        trip.setDistanceRouted_m(MatsimTripUtils.getDistanceRoutedByCalculation_m(trip, network, networkModes));
    }
}
