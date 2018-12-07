/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.agarwalamit.opdyts.teleportationModes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import floetteroed.utilities.TimeDiscretization;
import floetteroed.utilities.math.Vector;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.opdyts.MATSimCountingStateAnalyzer;
import org.matsim.contrib.opdyts.SimulationStateAnalyzerProvider;
import org.matsim.core.events.handler.EventHandler;

/**
 * Created by amit on 15.06.17. Adapted after {@link org.matsim.contrib.opdyts.car.DifferentiatedLinkOccupancyAnalyzer}
 */

public class TeleportationODCoordAnalyzer implements PersonDepartureEventHandler {

    private final Map<String, MATSimCountingStateAnalyzer<Zone>> mode2stateAnalyzer;
    private final Set<Zone> relevantZones;
    private final Map<Id<Person>, Integer> personId2TripIndex = new HashMap<>();
    private final Population population;

    public TeleportationODCoordAnalyzer(final TimeDiscretization timeDiscretization,
                                        final Set<Zone> relevantZones,
                                        final Set<String> relevantModes,
                                        final Population population) {
        this.relevantZones = relevantZones;
        this.mode2stateAnalyzer = new LinkedHashMap<>();
        for (String mode : relevantModes) {
            this.mode2stateAnalyzer.put(mode, new MATSimCountingStateAnalyzer<Zone>(timeDiscretization));
        }
        this.population = population;
    }

    public MATSimCountingStateAnalyzer<Zone> getNetworkModeAnalyzer(final String mode) {
        return this.mode2stateAnalyzer.get(mode);
    }

    public void beforeIteration() {
        this.personId2TripIndex.clear();
        for (MATSimCountingStateAnalyzer<Zone> stateAnalyzer : this.mode2stateAnalyzer.values()) {
            stateAnalyzer.beforeIteration();
        }
    }

    @Override
    public void reset(int iteration) {

    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        final MATSimCountingStateAnalyzer<Zone> stateAnalyzer = this.mode2stateAnalyzer.get(event.getLegMode());
        if (this.mode2stateAnalyzer.containsKey(event.getLegMode())) {

            int tripIndex = 0;
            Id<Person> personId = event.getPersonId();
            tripIndex = this.personId2TripIndex.getOrDefault(personId, 0);

            Person person = this.population.getPersons().get(personId);
            // TODO probably use TripStructureUtils.getActivities(...). Amit July'17
            List<Activity> acts = new ArrayList<>();
            for (PlanElement pe : person.getSelectedPlan().getPlanElements() ) {
                if (pe instanceof Activity) {
                    acts.add( (Activity) pe);
                }
            }

            Coord cord = acts.get(tripIndex).getCoord();
            for (Zone zone : this.relevantZones ) {
                if ( zone.getCoordsInsideZone().contains(cord)) {
                    stateAnalyzer.registerIncrease(zone.getZoneId(), (int)event.getTime());
                } else {
                    //dont do anything.
                }
            }
            this.personId2TripIndex.put(personId, tripIndex+1);
        } else {
            // network modes thus irrelevant here
        }
    }

    public static class Provider implements SimulationStateAnalyzerProvider {

        private final TimeDiscretization timeDiscretization;
        private final Set<String> relevantTeleportationMdoes;
        private final Set<Zone> relevantZones;
        private final Population population;

        private TeleportationODCoordAnalyzer teleportationODAnalyzer;


        public Provider(final TimeDiscretization timeDiscretization,
                        final Set<String> relevantTeleportationMdoes,
                        final Set<Zone> relevantZones,
                        Scenario scenario) {
            this.timeDiscretization = timeDiscretization;
            this.relevantTeleportationMdoes = relevantTeleportationMdoes;
            this.relevantZones = relevantZones;
            this.population = scenario.getPopulation();
        }

        @Override
        public String getStringIdentifier() {
            return "teleportationModes";
        }

        @Override
        public EventHandler newEventHandler() {
            this.teleportationODAnalyzer = new TeleportationODCoordAnalyzer(timeDiscretization, relevantZones, relevantTeleportationMdoes, population);
            return this.teleportationODAnalyzer;
        }

        @Override
        public Vector newStateVectorRepresentation() {
            final Vector result = new Vector(
                    this.teleportationODAnalyzer.mode2stateAnalyzer.size() * this.relevantZones.size()  * this.timeDiscretization.getBinCnt());
            int i = 0;
            for (String mode : this.teleportationODAnalyzer.mode2stateAnalyzer.keySet()) {
                final MATSimCountingStateAnalyzer<Zone> analyzer = this.teleportationODAnalyzer.mode2stateAnalyzer.get(mode);
                for (Zone zone : this.relevantZones) {
                    for (int bin = 0; bin < this.timeDiscretization.getBinCnt(); bin++) {
                        result.set(i++, analyzer.getCount(zone.getZoneId(), bin));
                    }
                }
            }
            return result;
        }

        @Override
        public void beforeIteration() {
            this.teleportationODAnalyzer.beforeIteration();
        }
    }
}
