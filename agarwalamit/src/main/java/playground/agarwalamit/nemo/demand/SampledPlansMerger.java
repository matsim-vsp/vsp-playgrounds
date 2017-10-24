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

package playground.agarwalamit.nemo.demand;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 *
 * Every person in sampled plans file has exactly one plan. This class reads rest plans files (100% scenario) and
 * extract the plans for sampled plans. Eventually, this writes out a sampled plans file with full choice set for each person.
 *
 * Created by amit on 24.10.17.
 */

public class SampledPlansMerger {

    public static void main(String[] args) {

        int numberOfFirstCemdapOutputFile = 100;
        int numberOfPlans = 5;

        String plansBaseDir = "/Users/amit/Documents/gitlab/nemo/data/input/matsim_initial_plans/";

        String sampledPlans = plansBaseDir+"/"+numberOfFirstCemdapOutputFile+"/sampling/plans_1pct.xml.gz";
        Population sampledPop = LoadMyScenarios.loadScenarioFromPlans(sampledPlans).getPopulation();

        String outPlans = plansBaseDir+"/plans_1pct_fullChoiceSet.xml.gz";

        for (int planNumber = 1; planNumber < numberOfPlans; planNumber++) {
            int planDir = 100+planNumber;
            String unsampledPlans = plansBaseDir+"/"+planDir+"/plans.xml.gz";
            Population unsampledPop = LoadMyScenarios.loadScenarioFromPlans(unsampledPlans).getPopulation();
            for (Person sampledPerson : sampledPop.getPersons().values()) {
                Person person = unsampledPop.getPersons().get(sampledPerson.getId());
                if (person==null) throw new RuntimeException("Sampled person "+ sampledPerson.getId() + " is not found in unsample plans "+unsampledPlans+".");
                else if (person.getPlans().size()!=0) throw new RuntimeException("Unsampled person "+ sampledPerson.getId() + " should have exactly one plan in choice set. It has "+person.getPlans().size()+" in his choice set.");

                sampledPerson.addPlan(person.getPlans().get(0));
            }
        }

        new PopulationWriter(sampledPop).write(outPlans);
    }
}
