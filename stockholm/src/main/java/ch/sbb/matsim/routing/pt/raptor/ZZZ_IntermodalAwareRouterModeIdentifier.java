/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.matsim.routing.pt.raptor;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.router.MainModeIdentifier;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;

/**
 * @author mrieser / SBB
 */
public class ZZZ_IntermodalAwareRouterModeIdentifier implements MainModeIdentifier {

    private final Set<String> transitModes;

    @Inject
    public ZZZ_IntermodalAwareRouterModeIdentifier(Config config) {
        this.transitModes = config.transit().getTransitModes();
    }

    /** Intermodal trips can have a number of different legs and interaction activities, e.g.:
     * access_walk | bike-interaction | bike | pt-interaction | transit-walk | pt-interaction | train | pt-interaction | egress_walk
     * Thus, this main mode identifier uses the following heuristic to decide to which router mode a trip belongs:
     * - if there is a leg with a pt mode (based on config.transit().getTransitModes(), it returns that pt mode.
     * - if there is only a leg with mode transit_walk, one of the configured transit modes is returned.
     * - otherwise, the first mode not being an access_walk, egress_walk or transit_walk.
     */
    @Override
    public String identifyMainMode(List<? extends PlanElement> tripElements) {
        String identifiedMode = null;
        for (PlanElement pe : tripElements) {
            if (pe instanceof Leg) {
                String mode = ((Leg) pe).getMode();
                if (transitModes.contains(mode)) {
                    return mode;
                }
                if (TransportMode.transit_walk.equals(mode)) {
                	// Logger.getLogger(IntermodalAwareRouterModeIdentifier.class).warn("replacing pt by walk");
                	identifiedMode = TransportMode.pt;
                	// identifiedMode = TransportMode.walk;
                }
                if (identifiedMode == null
                        && !TransportMode.access_walk.equals(mode)
                        && !TransportMode.egress_walk.equals(mode)
                        && !TransportMode.transit_walk.equals(mode)) {
                    identifiedMode = mode;
                }
            }
        }

        if (TransportMode.pt.equals(identifiedMode)) {
        	System.out.println("found identified main PT mode");
        	System.out.println(tripElements);
        	System.exit(0);
        }
        
        if (identifiedMode != null) {
            return identifiedMode;
        }

        throw new RuntimeException("could not identify main mode: " + tripElements);
    }
}
