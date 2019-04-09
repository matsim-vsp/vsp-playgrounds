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

/**
 * 
 */
package playground.jbischoff.csberlin.plans.preprocess;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

import playground.jbischoff.utils.JbUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class CutAgentsFromShape {
public static void main(String[] args) {
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	Geometry geo = JbUtils.readShapeFileAndExtractGeometry("C:/Users/Joschka/Documents/shared-svn/projects/bmw_carsharing/data/gis/klaus.shp", "id").get("0");
	Population pop2 = PopulationUtils.createPopulation(ConfigUtils.createConfig());
	
	StreamingPopulationReader spr = new StreamingPopulationReader(scenario);
	spr.addAlgorithm(new PersonAlgorithm() {
		
		@Override
		public void run(Person person) {
			for (Plan plan : person.getPlans()){
				for (PlanElement pe : plan.getPlanElements()){
					if (pe instanceof Activity){
						Coord c = ((Activity) pe).getCoord();
						if (geo.contains(MGC.coord2Point(c))){
							pop2.addPerson(person);
							return;
						}
					}
				}
			}
		}
	});
	spr.readFile("C:/Users/Joschka/Documents/shared-svn/projects/bmw_carsharing/data/avparking/untersuchungsraum-plans.xml.gz");
	new PopulationWriter(pop2).write("C:/Users/Joschka/Documents/shared-svn/projects/bmw_carsharing/data/avparking/klaus-population.xml.gz");
}

}
