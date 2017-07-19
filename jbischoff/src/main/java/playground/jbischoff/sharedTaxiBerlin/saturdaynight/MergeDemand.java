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
package playground.jbischoff.sharedTaxiBerlin.saturdaynight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Geometry;

import playground.jbischoff.utils.JbUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class MergeDemand {
	Scenario scenario;
public static void main(String[] args) {
	new MergeDemand().run();
}
private void run(){
	Random rnd = MatsimRandom.getLocalInstance();
	double startTime = 18*3600;
	double endTime = 4*3600;
	double scale = 0.1;
	scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	new PopulationReader(scenario).readFile("C:/Users/Joschka/Documents/shared-svn/projects/sustainability-w-michal-and-dlr/data/taxi_berlin/2013/OD/20130420/OD_20130420_SCALE_2.0_plans.xml.gz");
	
	Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	new PopulationReader(scenario2).readFile("C:/Users/Joschka/Documents/shared-svn/projects/sustainability-w-michal-and-dlr/data/taxi_berlin/2013/OD/20130421/OD_20130421_SCALE_2.0_plans.xml.gz");
	
	new MatsimNetworkReader(scenario.getNetwork()).readFile("C:/Users/Joschka/Documents/shared-svn/projects/sustainability-w-michal-and-dlr/data/network/berlin_brb.xml.gz");
	boolean addDummyPlans = true;
	Geometry geo = JbUtils.readShapeFileAndExtractGeometry("C:/Users/Joschka/Documents/shared-svn/projects/sustainability-w-michal-and-dlr/data/scenarios/drt_saturdaynight/shp/berlin_all.shp").get("010113");
	Random r = MatsimRandom.getRandom();
	Population mergedPop = PopulationUtils.createPopulation(ConfigUtils.createConfig());
	for (Person p : scenario.getPopulation().getPersons().values()){
		Plan plan = p.getSelectedPlan();
		Activity act1 = (Activity) plan.getPlanElements().get(0);
		if (act1.getEndTime()>=startTime){
			Coord startCoord = ParkingUtils.getRandomPointAlongLink(rnd, scenario.getNetwork().getLinks().get(act1.getLinkId()));
			Activity act2 = (Activity) plan.getPlanElements().get(2);
			Coord endCoord = ParkingUtils.getRandomPointAlongLink(rnd, scenario.getNetwork().getLinks().get(act2.getLinkId()));
			if (geo.contains(MGC.coord2Point(startCoord))&&geo.contains(MGC.coord2Point(endCoord))){
			Person np = mergedPop.getFactory().createPerson(p.getId());
//			if (r.nextDouble()<scale){
			mergedPop.addPerson(np);
//			}
			Plan nplan = PopulationUtils.createPlan();
			np.addPlan(nplan);
			Activity a0 = PopulationUtils.createActivityFromCoord("dummy", startCoord);
			a0.setEndTime(act1.getEndTime());
			nplan.addActivity(a0);
			nplan.addLeg(PopulationUtils.createLeg("taxi"));
			Activity a1 = PopulationUtils.createActivityFromCoord("dummy", endCoord);
			nplan.addActivity(a1);
			if (addDummyPlans){
				for (int i = 0; i<9; i++){
					np.addPlan(createDummyPlan(startCoord));
				}
			}
			ArrayList<Plan> plans = new ArrayList<>();
			plans.addAll(np.getPlans());
			Collections.shuffle(plans);
			np.setSelectedPlan(plans.get(0));
			
			}
		}
	}
	
	for (Person p : scenario.getPopulation().getPersons().values()){
		Plan plan = p.getSelectedPlan();
		Activity act1 = (Activity) plan.getPlanElements().get(0);
		if (act1.getEndTime()<endTime){
			Coord startCoord = ParkingUtils.getRandomPointAlongLink(rnd, scenario.getNetwork().getLinks().get(act1.getLinkId()));
			Activity act2 = (Activity) plan.getPlanElements().get(2);
			Coord endCoord = ParkingUtils.getRandomPointAlongLink(rnd, scenario.getNetwork().getLinks().get(act2.getLinkId()));
			if (geo.contains(MGC.coord2Point(startCoord))&&geo.contains(MGC.coord2Point(endCoord))){
				
			Person np = mergedPop.getFactory().createPerson(p.getId());
//			if (r.nextDouble()<scale){
				mergedPop.addPerson(np);
//				}
			Plan nplan = PopulationUtils.createPlan();
			np.addPlan(nplan);
			Activity a0 = PopulationUtils.createActivityFromCoord("dummy", startCoord);
			a0.setEndTime(act1.getEndTime()+24*3600);
			nplan.addActivity(a0);
			nplan.addLeg(PopulationUtils.createLeg("taxi"));
			Activity a1 = PopulationUtils.createActivityFromCoord("dummy", endCoord);
			nplan.addActivity(a1);
			if (addDummyPlans){
				for (int i = 0; i<9; i++){
					np.addPlan(createDummyPlan(startCoord));
				}
			}
			ArrayList<Plan> plans = new ArrayList<>();
			plans.addAll(np.getPlans());
			Collections.shuffle(plans);
			np.setSelectedPlan(plans.get(0));
			
		}}
		
	}
	new PopulationWriter(mergedPop).write("C:/Users/Joschka/Documents/shared-svn/projects/sustainability-w-michal-and-dlr/data/scenarios/drt_saturdaynight/population_night_bln_dummy_"+scale+".xml");
	
}
	Plan createDummyPlan(Coord coord){
		Plan plan = scenario.getPopulation().getFactory().createPlan();
		Activity act = scenario.getPopulation().getFactory().createActivityFromCoord("dummy", coord);
		plan.addActivity(act);
		return plan;
	
	}
}
