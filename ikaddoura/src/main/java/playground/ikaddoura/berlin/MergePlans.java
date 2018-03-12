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

package playground.ikaddoura.berlin;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

/**
* @author ikaddoura
*/

public class MergePlans {

	public static void main(String[] args) {
		
		// subpopulation: persons
		final String inputFile1 = "/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/be_3/population/pop_300/plans_10pct_corine-landcover_adjusted-activity-types.xml.gz";
		
		// subpopulation: freight
		final String inputFile2 = "/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/be_3/population/freight/freight-agents-berlin4.1_sampleSize0.1_corine-landcover_1.xml.gz";
		
		final String populationOutputFileName = "/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/be_3/population/population_400_person_freight_10pct.xml.gz";
		final String personAttributesOutputFileName = "/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/be_3/population/personAttributes_400_person_freight_10pct.xml.gz";

		Config config1 = ConfigUtils.createConfig();
		config1.plans().setInputFile(inputFile1);
		Scenario scenario1 = ScenarioUtils.loadScenario(config1);

		Config config2 = ConfigUtils.createConfig();
		config2.plans().setInputFile(inputFile2);
		Scenario scenario2 = ScenarioUtils.loadScenario(config2);
		
		Scenario scenario3 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population population = scenario3.getPopulation();
						
		for (Person person: scenario1.getPopulation().getPersons().values()) {
			population.addPerson(person);
			population.getPersonAttributes().putAttribute(person.getId().toString(), scenario3.getConfig().plans().getSubpopulationAttributeName(), "person");
		}
		for (Person person : scenario2.getPopulation().getPersons().values()) {
			population.addPerson(person);
			population.getPersonAttributes().putAttribute(person.getId().toString(), scenario3.getConfig().plans().getSubpopulationAttributeName(), "freight");
		}
		
		PopulationWriter writer = new PopulationWriter(population);
		writer.write(populationOutputFileName);
		
		ObjectAttributesXmlWriter writer2 = new ObjectAttributesXmlWriter(population.getPersonAttributes());
		writer2.writeFile(personAttributesOutputFileName);
	}

}

