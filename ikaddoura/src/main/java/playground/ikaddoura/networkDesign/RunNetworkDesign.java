/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.networkDesign;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

/**
* @author ikaddoura
* 
*/
public class RunNetworkDesign {

	private static final String configFile = "/Users/ihab/Documents/workspace/runs-svn/networkDesign/input/config.xml";
	
	private static final Logger log = Logger.getLogger(RunNetworkDesign.class);
	
	public static void main(String[] args) {
		
		final Config config = ConfigUtils.loadConfig(configFile);
		config.planCalcScore().setMarginalUtlOfWaitingPt_utils_hr(0.);
		config.controler().setOutputDirectory("/Users/ihab/Documents/workspace/runs-svn/networkDesign/output/run0/");
	
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		final Controler controler = new Controler(scenario);
		
		controler.addOverridingModule( new AbstractModule() {
			
			@Override public void install() {
				this.bind(NetworkDesign.class).asEagerSingleton();
				this.addControlerListenerBinding().to(NetworkDesign.class);
				this.addEventHandlerBinding().to(NetworkDesign.class);
			}

		}) ;
				
		controler.run();
		
		log.info("Run completed.");
		
	}	
}

