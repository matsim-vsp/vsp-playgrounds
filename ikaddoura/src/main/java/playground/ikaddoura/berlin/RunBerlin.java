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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.run.RunBerlinScenario;

import playground.ikaddoura.analysis.IKAnalysisRun;
import playground.ikaddoura.analysis.modalSplitUserType.AgentAnalysisFilter;
import playground.ikaddoura.analysis.modalSplitUserType.ModalSplitUserTypeControlerListener;

/**
* @author ikaddoura
*/

public class RunBerlin {
	private static final Logger log = Logger.getLogger(RunBerlin.class);

	private static String configFileName;
	private static String overridingConfigFileName;
	private static String runId;
	private static String outputDirectory;
	private static String visualizationScriptDirectory;
	private static Integer scaleFactor;
		
	public static void main(String[] args) {

		if (args.length > 0) {		
			configFileName = args[0];
			overridingConfigFileName = args[1];
			runId = args[2];
			outputDirectory = args[3];
			visualizationScriptDirectory = args[4];
			scaleFactor = Integer.parseInt(args[5]);
			
		} else {		
			String baseDirectory = "/Users/ihab/Documents/workspace/matsim-berlin/";	
			configFileName = baseDirectory + "scenarios/berlin-v5.2-1pct/input/berlin-v5.2-1pct.config.xml";
			overridingConfigFileName = null;
			runId = "berlin-test-1";
			outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/savPricing/output/output-local-run_" + runId + "/";
			visualizationScriptDirectory = "./visualization-scripts/";
			scaleFactor = 100;
		}
			
		log.info("run Id: " + runId);
		log.info("output directory: " + outputDirectory);
		
		RunBerlin drtPricing = new RunBerlin();
		drtPricing.run();
	}
	
	private void run() {
		RunBerlinScenario berlin = new RunBerlinScenario(configFileName, overridingConfigFileName);
		
		Config config = berlin.prepareConfig();
		config.controler().setRunId(runId);
		config.controler().setOutputDirectory(outputDirectory);
						
		Controler controler = berlin.prepareControler();
		
		// modal split analysis
		controler.addOverridingModule(new org.matsim.core.controler.AbstractModule() {	
			@Override
			public void install() {
				this.addControlerListenerBinding().to(ModalSplitUserTypeControlerListener.class);
			}
		});
				
		controler.run();
		
		log.info("Done.");
		
		log.info("Running offline analysis...");
		
		final String scenarioCRS = TransformationFactory.DHDN_GK4;	
		final String shapeFileZones = null;
		final String zonesCRS = null;
		final String homeActivity = "home";
		final int scalingFactor = scaleFactor;
		
		List<AgentAnalysisFilter> filters = new ArrayList<>();

		AgentAnalysisFilter filter1 = new AgentAnalysisFilter(controler.getScenario());
		filter1.setPersonAttribute("berlin");
		filter1.setPersonAttributeName("home-activity-zone");
		filter1.preProcess(controler.getScenario());
		filters.add(filter1);
		
		AgentAnalysisFilter filter2 = new AgentAnalysisFilter(controler.getScenario());
		filter2.preProcess(controler.getScenario());
		filters.add(filter2);
		
		AgentAnalysisFilter filter3 = new AgentAnalysisFilter(controler.getScenario());
		filter3.setPersonAttribute("brandenburg");
		filter3.setPersonAttributeName("home-activity-zone");
		filter3.preProcess(controler.getScenario());
		filters.add(filter3);
		
		List<String> modes = new ArrayList<>();
		modes.add(TransportMode.car);

		IKAnalysisRun analysis = new IKAnalysisRun(
				controler.getScenario(),
				null,
				visualizationScriptDirectory,
				scenarioCRS,
				shapeFileZones,
				zonesCRS,
				homeActivity,
				scalingFactor,
				filters,
				null,
				modes,
				null,
				null,
				0.);
		analysis.run();
				
		log.info("Done.");
	}

}

