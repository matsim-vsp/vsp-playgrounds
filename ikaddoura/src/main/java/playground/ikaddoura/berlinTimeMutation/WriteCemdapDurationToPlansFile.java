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

package playground.ikaddoura.berlinTimeMutation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
* @author ikaddoura
*/

public class WriteCemdapDurationToPlansFile {

	private static final String inputPlansFile = "/Users/ihab/Documents/workspace/runs-svn/cemdap-berlin-timeMutation/input/be_251.output_plans_selected.xml.gz";
	private static String cemdapStopsFile = "/Users/ihab/Documents/workspace/runs-svn/cemdap-berlin-timeMutation/input/Stops.out.txt";
	private static final String outputPlansFile = "/Users/ihab/Documents/workspace/runs-svn/cemdap-berlin-timeMutation/input/be_251.output_plans_selected_cemdap-durations.xml.gz";

	public static void main(String[] args) {
		WriteCemdapDurationToPlansFile cemdapDurationToPlansFile = new WriteCemdapDurationToPlansFile();
		cemdapDurationToPlansFile.write(inputPlansFile, cemdapStopsFile );
	}

	private void write(String plansfile, String cemdapStopsFile) {
		
		// load population
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(plansfile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Population population = scenario.getPopulation();
		
		CemdapStopsDurationParser parser = new CemdapStopsDurationParser();
		parser.parse(population, cemdapStopsFile, outputPlansFile);
		
	}
}

