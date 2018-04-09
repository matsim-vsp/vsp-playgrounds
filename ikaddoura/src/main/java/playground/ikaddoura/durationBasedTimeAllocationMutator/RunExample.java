/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.ikaddoura.durationBasedTimeAllocationMutator;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;

public class RunExample {
	
	private static final String STRATEGY_NAME = "durationBasedTimeMutator";

	public static void main(final String[] args) {
		OutputDirectoryLogging.catchLogEntries();
		
		Config config;
		if ( args.length==0 ) {
			config = ConfigUtils.loadConfig( "/Users/ihab/Documents/workspace/runs-svn/test-scenario/equil/config-test.xml" ) ;
		} else {
			config = ConfigUtils.loadConfig(args[0]);
		}
		
		//add a strategy to the config
		StrategySettings stratSets = new StrategySettings();
		stratSets.setStrategyName(STRATEGY_NAME);
		stratSets.setWeight(0.1);
		config.strategy().addStrategySettings(stratSets);

		final Controler controler = new Controler(config);
		
		//add the binding strategy 
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding(STRATEGY_NAME).toProvider(DurationBasedTimeAllocationPlanStrategyProvider.class);
			}
		});
				
		controler.run();

	}

}
