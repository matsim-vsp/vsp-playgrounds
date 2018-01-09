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

package playground.ikaddoura.analysis.money;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.noise.personLinkMoneyEvents.CombinedPersonLinkMoneyEventsReader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author ikaddoura
 *
 */
public class MoneyExtCostAnalysisMain {

	private String outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/optAV_new/output/output_v5000_SAVuserOpCostPricingT_SAVuserExtCostPricingT_SAVdriverExtCostPricingT_CCuserExtCostPricingT/";
	private String runId = "run5b";
	
	public static void main(String[] args) {
		MoneyExtCostAnalysisMain anaMain = new MoneyExtCostAnalysisMain();
		anaMain.run();
		
	}

	private void run() {
		
		if (!outputDirectory.endsWith("/")) {
			outputDirectory = outputDirectory + "/";
		}
			
		EventsManager eventsManager = EventsUtils.createEventsManager();
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(outputDirectory + runId + ".output_network.xml.gz");
		Network network = ScenarioUtils.loadScenario(config).getNetwork();
		
		MoneyExtCostHandler handler = new MoneyExtCostHandler(network);
		eventsManager.addHandler(handler);
		
		CombinedPersonLinkMoneyEventsReader reader = new CombinedPersonLinkMoneyEventsReader(eventsManager);
		reader.readFile(outputDirectory + runId + ".output_events.xml.gz");
		
		handler.writeInfo(outputDirectory);
	} 
}
		

