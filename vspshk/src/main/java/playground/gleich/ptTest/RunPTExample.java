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

/**
 * 
 */
package playground.gleich.ptTest;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.av.intermodal.router.VariableAccessTransitRouterModule;
import org.matsim.contrib.av.intermodal.router.config.*;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.run.*;
import org.matsim.core.config.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import com.google.inject.Provider;


/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class RunPTExample {
	public static void main(String[] args) {
		new RunPTExample().run();
	}

	public void run() {
		Config config = ConfigUtils.loadConfig(
				"intermodal-example/config_without_taxi.xml");

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Controler controler = new Controler(scenario);
		
		controler.run();    
		
		TripRouter tripRouter = controler.getTripRouterProvider().get();
		String mode = TransportMode.pt;
		
		ActivityFacilitiesFactoryImpl actFacilFacImpl = new ActivityFacilitiesFactoryImpl();
		ActivityFacility originFacility = actFacilFacImpl.createActivityFacility(Id.create("homeXCoord2050", ActivityFacility.class), CoordUtils.createCoord(2050.0, 1050.0));
		
		ActivityFacility destinationFacility = actFacilFacImpl.createActivityFacility(Id.create("workAtPtSideOfDecisionPoint", ActivityFacility.class), CoordUtils.createCoord(3873.0, 1050.0));

		double TIME_OF_DAY = 8*60*60;
		List<? extends PlanElement> route = tripRouter.calcRoute(mode, originFacility, destinationFacility, TIME_OF_DAY, null);
		
//		controler.run();
	}
}
