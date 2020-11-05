/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package playground.gleich.misc;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class PlansFilter {

    public static void main (String[] args) {
//        String inputPopulationPath = "/home/gregor/git/shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v1/u19-pt-0.35-raptor/Vu-DRT-9.output_plans_u19-pt-0.35.xml.gz";
//        String outputPopulationPath = "/home/gregor/git/shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v1/u19-pt-0.35-raptor/Vu-DRT-9.output_plans_u19-pt-0.35_u19withschool-in-Vu-selected-plans.xml.gz";

        String inputPopulationPath = "/home/gregor/git/runs-svn/avoev/snz-vulkaneifel/u19-runs/output_Vu-DRT-u19-pt-6/ITERS/it.0/Vu-DRT-u19-pt-6.0.plans.xml";
        String outputPopulationPath = "/home/gregor/git/runs-svn/avoev/snz-vulkaneifel/u19-runs/output_Vu-DRT-u19-pt-6/ITERS/it.0/Vu-DRT-u19-pt-6.0.plans_u19withschool-in-Vu-selected-plans.xml.gz";


//        String inputPopulationPath = "/home/gregor/git/runs-svn/avoev/snz-vulkaneifel/u19-runs/output_Vu-DRT-u19-pt-1/Vu-DRT-u19-pt-1.output_plans.xml.gz";
//        String outputPopulationPath = "/home/gregor/git/runs-svn/avoev/snz-vulkaneifel/u19-runs/output_Vu-DRT-u19-pt-1/Vu-DRT-u19-pt-1.output_plans_u19withschool-in-Vu-selected-plans.xml.gz";

        String personFilterAttribute = "subpopulation";
        String personFilterValue = "u19-with-school-act-in-Vu";

        Scenario inputScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        Scenario scOutput = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Population popOutput = scOutput.getPopulation();
        popOutput.getAttributes().putAttribute("coordinateReferenceSystem", "EPSG:25832");

        StreamingPopulationWriter popWriter = new StreamingPopulationWriter();
        popWriter.writeStartPlans(outputPopulationPath);

        StreamingPopulationReader spr = new StreamingPopulationReader(inputScenario);
        spr.addAlgorithm(person -> {
            Object attr = person.getAttributes().getAttribute(personFilterAttribute);
            String attrValue = attr == null ? null : attr.toString();
            if (personFilterValue.equals(attrValue)) {
                Person personNew = popOutput.getFactory().createPerson(person.getId());

                for (String attribute : person.getAttributes().getAsMap().keySet()) {
                    personNew.getAttributes().putAttribute(attribute, person.getAttributes().getAttribute(attribute));
                }
                personNew.addPlan(person.getSelectedPlan());

                popWriter.writePerson(personNew);
            }
        }
        );
        spr.readFile(inputPopulationPath);
        popWriter.writeEndPlans();
        System.out.println("PlansFilter done");
    }
}
