/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import com.google.inject.Inject;

import playground.vsp.analysis.utils.GnuplotUtils;

/**
 * @author tthunig
 */
public class TtSubnetworkAnalysisWriter implements IterationEndsListener{

	private static final Logger log = Logger.getLogger(TtSubnetworkAnalysisWriter.class);
	
	private Scenario scenario;
	private TtSubnetworkAnalyzer subNetAnalyzer;
	private String outputDirBase;
	private PrintStream overallItWritingStream;
	private int lastIteration;
	
	@Inject
	public TtSubnetworkAnalysisWriter(Scenario scenario, TtSubnetworkAnalyzer subNetAnalyzer) {
		this.scenario = scenario;
		this.subNetAnalyzer = subNetAnalyzer;
		this.outputDirBase = scenario.getConfig().controler().getOutputDirectory();
		this.lastIteration = scenario.getConfig().controler().getLastIteration();
		
		// prepare file for the results of all iterations
		prepareOverallItWriting();
	}
	
	private void prepareOverallItWriting() {
		// create output dir for overall iteration analysis
		String lastItDir = this.outputDirBase + "/ITERS/it." + this.lastIteration + "/";
		new File(lastItDir).mkdir();
		String lastItOutputDir = lastItDir + "analysis/";
		new File(lastItOutputDir).mkdir();

		// create writing stream
		try {
			this.overallItWritingStream = new PrintStream(new File(lastItOutputDir + "subNetworkAnalysis.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		// write header
		String header = "it \ttotal tt inner city[s] \ttotal delay inner city[s] \ttotal dist inner city[m] \tnumber of trips inner city \trel number of trips inner city";
		this.overallItWritingStream.println(header);
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// write analyzed data
		log.info("Starting to write analysis of iteration " + event.getIteration() + "...");
		// write results
		StringBuffer line = new StringBuffer();
		line.append(event.getIteration());
		line.append("\t" + subNetAnalyzer.getTotalTtSubnetwork());
		line.append("\t" + subNetAnalyzer.getTotalDelaySubnetwork());
		line.append("\t" + subNetAnalyzer.getTotalDistanceSubnetwork());
		line.append("\t" + subNetAnalyzer.getNumberOfTripsInSubnetwork());
		line.append("\t" + subNetAnalyzer.getRelativeNumberOfTripsInSubnetwork());
		this.overallItWritingStream.println(line.toString());

		// handle last iteration
		if (event.getIteration() == lastIteration) {			
			// close overall writing stream
			this.overallItWritingStream.close();
			
			log.info("plot overall iteration results");
			// plot overall iteration results
			runGnuplotScript("plot_subNetAnalysis", event.getIteration());
		}
	}
	
	/**
	 * starts the gnuplot script from the specific iteration directory
	 * 
	 * @param gnuplotScriptName
	 * @param iteration
	 */
	private void runGnuplotScript(String gnuplotScriptName, int iteration){
		String pathToSpecificAnalysisDir = scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + iteration + "/analysis";		
		String relativePathToGnuplotScript = "../../../../../../../shared-svn/studies/tthunig/gnuplotScripts/" + gnuplotScriptName  + ".p";
		
		GnuplotUtils.runGnuplotScript(pathToSpecificAnalysisDir, relativePathToGnuplotScript);
	}

}
