/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.jbischoff.drt.cottbus;/*
 * created by jbischoff, 13.09.2018
 */

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.util.PopulationUtils;
import org.matsim.core.config.Config;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.pt.router.TransitActsRemover;

import static org.matsim.api.core.v01.TransportMode.drt;
import static org.matsim.core.config.ConfigUtils.createConfig;
import static org.matsim.core.scenario.ScenarioUtils.createScenario;

public class PreprocessPtPlans {

    public static void main(String[] args) {
        Config config = createConfig();
        Scenario scenario = createScenario(config);
        new PopulationReader(scenario).readFile("D:/runs-svn/cottbus/FuehrerBA/cb04/output_plans.xml.gz");
        System.out.println(scenario.getPopulation().getPersons().size() + " persons before ");
        PopulationUtils.removePersonsNotUsingMode(TransportMode.pt, scenario);
        System.out.println(scenario.getPopulation().getPersons().size() + " persons after removing non pt ");
        int tw = 0;
        for (Person p : scenario.getPopulation().getPersons().values()) {

            PersonUtils.removeUnselectedPlans(p);

            Plan plan = p.getSelectedPlan();

            Leg prevLeg = null;
            Activity prevAct = null;
            for (PlanElement pe : plan.getPlanElements()) {
                //convert pure transit_walks to walk
                if (pe instanceof Activity) {
                    if (prevLeg != null && prevAct != null) {
                        if (!((Activity) pe).getType().equals("pt interaction") && !prevAct.getType().equals("pt interaction") && prevLeg.getMode().equals(TransportMode.transit_walk)) {
                            prevLeg.setMode("walk");
                            tw++;
                        }
                    }
                    prevAct = (Activity) pe;
                } else if (pe instanceof Leg) {
                    prevLeg = (Leg) pe;
                }

            }

            new TransitActsRemover().run(plan);
            plan.getPlanElements().stream().filter(Leg.class::isInstance).forEach(l -> {
                if (((Leg) l).getMode().equals(TransportMode.pt))
                    ((Leg) l).setMode(drt);
            });
        }
        System.out.println(tw + "found");
        new PopulationWriter(scenario.getPopulation()).write("D:/Bachelorarbeit/scenarios/Cottbus_DRT/drtplans.xml");
    }
}
