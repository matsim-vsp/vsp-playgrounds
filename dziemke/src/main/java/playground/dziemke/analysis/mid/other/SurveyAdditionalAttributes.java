package playground.dziemke.analysis.mid.other;

import org.matsim.api.core.v01.population.Leg;

public class SurveyAdditionalAttributes {

    //population
    static final String SOURCE = "source";
    public enum Source {
        MID, SRV, MATSIM
    }
    static final String AGGREGATED_WEIGHT = "aggregatedWeightOfAllTrips";

    //leg
    static final String DISTANCE_BEELINE_M = "distanceBeeline_m";
    static final String SPEED_M_S = "speed_m_s";
    static final String WEIGHT = "weight";

}
