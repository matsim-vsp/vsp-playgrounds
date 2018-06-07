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
package analysis.cten;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import com.google.inject.Inject;

/**
 * @author tthunig
 */
public class TtWriteComAnalysis implements IterationEndsListener {

	private static final Logger log = Logger.getLogger(TtWriteComAnalysis.class);
	
	private TtCommodityTravelTimeAnalyzer handler;
	private String outputDirBase;
	
	@Inject
	public TtWriteComAnalysis(Scenario scenario, TtCommodityTravelTimeAnalyzer handler) {
		this.handler = handler;
		this.outputDirBase = scenario.getConfig().controler().getOutputDirectory();
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		log.info("Starting to write analysis of iteration " + event.getIteration() + "...");
		
		// create output dir for this iteration analysis
		String outputDir = this.outputDirBase + "/ITERS/it." + event.getIteration() + "/analysis/";
		new File(outputDir).mkdir();
		
		PrintStream stream;
		String filename = outputDir + "commodityTT.txt";
		try {
			stream = new PrintStream(new File(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		String header = "commodityId\tavgTt\tindividualTTs";
		stream.println(header);

		for (String comId : handler.getAllTravelTimesPerCommodity().keySet()) {
			StringBuffer line = new StringBuffer();
			line.append(comId + "\t" + handler.getAvgTravelTimePerCommodity().get(comId));
			for (Double individualTt : handler.getAllTravelTimesPerCommodity().get(comId)) {
				line.append("\t" + individualTt);
			}
			stream.println(line.toString());
		}

		stream.close();

		log.info("output written to " + filename);
	}

}
