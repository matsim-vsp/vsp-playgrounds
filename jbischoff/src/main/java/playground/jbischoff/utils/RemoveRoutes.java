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

package playground.jbischoff.utils;/*
 * created by jbischoff, 06.07.2018
 */

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.population.io.PopulationReader;

import java.util.Arrays;
import java.util.List;

import static org.matsim.core.config.ConfigUtils.createConfig;
import static org.matsim.core.scenario.ScenarioUtils.createScenario;

public class RemoveRoutes {
    public static void main(String[] args) {

        List<String> files = Arrays.asList(new String[]{"C:/Users/Joschka/git/matsim/contribs/drt/src/main/resources/mielec_2014_02/plans_only_drt_4.0.xml.gz",
                "C:/Users/Joschka/git/matsim/contribs/drt/src/main/resources/mielec_2014_02/plans_only_drt_1.0.xml.gz",
                "C:/Users/Joschka/git/matsim/contribs/drt/src/main/resources/mielec_2014_02/plans_only_drt_1.5.xml.gz",
                "C:/Users/Joschka/git/matsim/contribs/drt/src/main/resources/mielec_2014_02/plans_only_drt_2.0.xml.gz",
                "C:/Users/Joschka/git/matsim/contribs/drt/src/main/resources/mielec_2014_02/plans_only_drt_2.5.xml.gz",
                "C:/Users/Joschka/git/matsim/contribs/drt/src/main/resources/mielec_2014_02/plans_only_drt_3.0.xml.gz",
                "C:/Users/Joschka/git/matsim/contribs/drt/src/main/resources/mielec_2014_02/plans_only_drt_3.5.xml.gz"});

        for (String f : files) {
            Config config = createConfig();
            Scenario scenario = createScenario(config);
            new PopulationReader(scenario).readFile(f);
            for (Person p : scenario.getPopulation().getPersons().values()) {
                Leg l = (Leg) p.getSelectedPlan().getPlanElements().get(1);
                l.setRoute(null);
            }
            new PopulationWriter(scenario.getPopulation()).write(f);
        }
    }
}
