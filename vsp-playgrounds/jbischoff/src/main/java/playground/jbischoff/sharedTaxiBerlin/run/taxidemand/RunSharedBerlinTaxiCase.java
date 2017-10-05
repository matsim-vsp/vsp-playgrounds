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
package playground.jbischoff.sharedTaxiBerlin.run.taxidemand;

import org.matsim.contrib.av.robotaxi.scoring.TaxiFareConfigGroup;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalModule;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.optimizer.rebalancing.DemandBasedRebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.run.*;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import com.google.inject.Binder;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class RunSharedBerlinTaxiCase {

	public static void main(String[] args) {

		
			String runId = "testreb";
			String configFile = "../../../shared-svn/projects/sustainability-w-michal-and-dlr/data/scenarios/drt/config0.1.xml";
			Config config = ConfigUtils.loadConfig(configFile, new DvrpConfigGroup(), new DrtConfigGroup(),
					new OTFVisConfigGroup(), new TaxiFareConfigGroup());
			config.controler().setWriteEventsInterval(1);
			DrtConfigGroup drt = (DrtConfigGroup) config.getModules().get(DrtConfigGroup.GROUP_NAME);
		
			drt.setEstimatedBeelineDistanceFactor(1.5);
			drt.setVehiclesFile("new_net.taxis4to4_cap4.xml");
			drt.setNumberOfThreads(7);
			drt.setMaxTravelTimeAlpha(5);
			drt.setMaxTravelTimeBeta(3000);
			drt.setkNearestVehicles(7);
			drt.setRebalancingInterval(1800);
			
			config.controler().setRunId(runId);
			config.controler().setLastIteration(5);
			config.controler().setOutputDirectory("D:/runs-svn/sharedTaxi/testReb");
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			org.matsim.core.controler.Controler controler = DrtControlerCreator.createControler(config, false);
			DrtZonalSystem zones = new DrtZonalSystem(controler.getScenario().getNetwork(), 2000);

			controler.addOverridingModule(new AbstractModule() {
		
				@Override
				public void install() {
					bind(DrtZonalSystem.class).toInstance(zones);
					bind(RebalancingStrategy.class).to(DemandBasedRebalancingStrategy.class).asEagerSingleton();
				}
			});
			controler.addOverridingModule(new DrtZonalModule());
			controler.run();
		
	}
		
		
	
}
