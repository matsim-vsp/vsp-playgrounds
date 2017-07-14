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

import java.util.Map;

import org.matsim.contrib.drt.data.validator.DrtRequestValidator;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import com.google.inject.name.Names;
import com.vividsolutions.jts.geom.Geometry;

import playground.jbischoff.sharedTaxiBerlin.saturdaynight.ZonalSystem.OptimizationCriterion;
import playground.jbischoff.utils.JbUtils;


/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class RunSharedTaxiWithDynamicZonesServer {
	public static void main(String[] args) {
		
		Config config = ConfigUtils.loadConfig(args[0], new DrtConfigGroup(), new DvrpConfigGroup(), new OTFVisConfigGroup());
		config.controler().setWriteEventsInterval(10);
		config.controler().setOutputDirectory(config.controler().getOutputDirectory()+"/"+args[1]+"/");
		OptimizationCriterion criterion = OptimizationCriterion.valueOf(args[1]);
		
		ZonalSystem zones = new ZonalSystem(JbUtils.readShapeFileAndExtractGeometry(config.getContext().getFile()+"/shp/berlin_grid_1500.shp", "ID"),criterion);
		Controler controler = DrtControlerCreator.createControler(config, false);
		ZonalBasedRequestValidator validator = new ZonalBasedRequestValidator(controler.getScenario().getNetwork(), zones);

		controler.addOverridingModule(new AbstractModule() {
						
			@Override
			public void install() {
				addControlerListenerBinding().to(TaxiZoneManager.class).asEagerSingleton();
				bind(ZonalOccupancyAggregator.class).asEagerSingleton();
				bind(SharedTaxiFareCalculator.class).asEagerSingleton();
				bind(DrtRequestValidator.class).toInstance(validator);
				bind(ZonalBasedRequestValidator.class).toInstance(validator);
				bind(ZonalSystem.class).toInstance(zones);

			}
		});
		controler.run();
		
		
}
}
