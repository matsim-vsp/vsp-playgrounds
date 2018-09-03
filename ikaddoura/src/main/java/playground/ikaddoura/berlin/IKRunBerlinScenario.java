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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.run.RunBerlinScenario;

/**
* @author ikaddoura
*/

public class IKRunBerlinScenario {

	private static final Logger log = Logger.getLogger(IKRunBerlinScenario.class);

	public static void main(String[] args) {
		String configFileName ;
		String overridingConfigFileName = null;
		if ( args.length==0 || args[0].equals("")) {
			configFileName = "scenarios/berlin-v5.1-10pct/input/berlin-v5.2-10pct.config.xml";
//			configFileName = "scenarios/berlin-v5.1-10pct/input/berlin-v5.1-10pct.config.xml";
			overridingConfigFileName = "overridingConfig.xml";
		} else {
			configFileName = args[0];
			if ( args.length>1 ) overridingConfigFileName = args[1];
		}
		log.info( "config file: " + configFileName );
		RunBerlinScenario berlin = new RunBerlinScenario( configFileName, overridingConfigFileName );
				
		final Random random = MatsimRandom.getRandom();
		List<Id<Person>> deletePersons = new ArrayList<>();
		
		final Scenario scenario = berlin.prepareScenario();
		final double sampleSize = 0.1;
		int counterPersons = 0;
		int counterPersonsDelete = 0;
		for (Person person : scenario.getPopulation().getPersons().values()) {
			counterPersons++;
			if (random.nextDouble() >= sampleSize) {
				deletePersons.add(person.getId());
				counterPersonsDelete++;
			}
		}
		
		for (Id<Person> id : deletePersons) {
			scenario.getPopulation().getPersons().remove(id);
		}
		
		log.info("original persons: " + counterPersons);
		log.info("deleted persons: " + counterPersonsDelete);
		log.info("number of persons in output population: " + scenario.getPopulation().getPersons().size());
		
		new PopulationWriter(scenario.getPopulation()).write("/Users/ihab/Desktop/plans1pct.xml.gz");
		
		berlin.run();
	}

}

