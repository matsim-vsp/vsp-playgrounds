/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package playground.tschlenther.generalUtils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class CutPopulation {
			
	public static void main(String[] args) {
		
		Logger logger = Logger.getLogger(CutPopulation.class);

		String inputPopulation = "C:/Users/Work/VSP/urbanAtlasBerlin/uA/be_400_c_10pct_person_freight.selected_plans.xml.gz";
		String outputFile =  "C:/Users/Work/VSP/urbanAtlasBerlin/uA/be_400_c_10pct_person_freight.tempelhofCut.xml.gz";
		
		if(args.length != 0) {
			inputPopulation = args[0];
			outputFile = args[1];
		}
		
		Scenario inputScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population outputPopulation = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		
		
		PopulationReader reader = new PopulationReader(inputScenario);
		logger.info("reading " + inputPopulation);
		reader.readFile(inputPopulation);

		int counter = 0;
		int exponent = 1;
		
		for (Person p : inputScenario.getPopulation().getPersons().values()) {
			counter ++;
			if(counter % Math.pow(2, exponent) == 0) {
				logger.info("person #" + counter);
				exponent++;
			}
			
			for(Plan plan : p.getPlans()) {
				boolean personCopied = false;
				for(PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Activity) {

                        Activity activity = (Activity) pe;
                        Coord coord = activity.getCoord();
                        if(isCoordWithinBoundingBox(coord)) {
                        	outputPopulation.addPerson(p);
                        	personCopied = true;
                        	break;
                        }
					}
				}
				if (personCopied) break;
			}
		}
		
		
		logger.info("writing population to " + outputFile);
		PopulationWriter writer = new PopulationWriter(outputPopulation);
		writer.writeV6(outputFile);
		
		
		logger.info("----DONE----");
		
		
		
	}

	private static boolean isCoordWithinBoundingBox(Coord c) {
		double minX = 4592732.382935;
		double maxX = 4598820.41199;
		double minY = 5816378;
		double maxY = 5819403;
		
		if (c.getX() >= minX && c.getX() <= maxX && c.getY() >= minY && c.getY() <= maxY) return true;
		
		return false;
	}

}
