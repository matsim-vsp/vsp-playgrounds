package playground.dziemke.analysis.general.matsim;

import org.matsim.api.core.v01.network.Network;
import playground.dziemke.analysis.Trip;
import playground.dziemke.utils.ProgressLogger;

import java.util.Collection;
import java.util.List;

/**
 * @author gthunig on 06.04.2017.
 */
class TripInformationCalculator {
    public static final ProgressLogger log = ProgressLogger.getLogger(Events2TripsParser.class);

    private Network network;
    private Collection<String> networkModes;

    public TripInformationCalculator(Network network, Collection<String> networkModes) {

        this.network = network;
        this.networkModes = networkModes;
    }

    public void calculateInformation(List<MatsimTrip> trips) {
        log.initializeProgress("Trip", trips.size());
        for (MatsimTrip trip : trips) {
            log.processCounter();
            calculateInformation(trip);
        }
    }

    public void calculateInformation(MatsimTrip trip) {
        trip.setDuration_s(MatsimTripUtils.getDurationByCalculation_s(trip));
        trip.setDistanceBeeline_m(MatsimTripUtils.calculateBeelineDistance_m(trip, network));
        trip.setDistanceRouted_m(MatsimTripUtils.getDistanceRoutedByCalculation_m(trip, network, networkModes));
    }
}
