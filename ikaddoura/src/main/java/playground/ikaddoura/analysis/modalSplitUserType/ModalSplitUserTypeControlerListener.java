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

package playground.ikaddoura.analysis.modalSplitUserType;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.collections.Tuple;

/**
 * @author ikaddoura, amit
 */

public class ModalSplitUserTypeControlerListener implements StartupListener, IterationEndsListener{

	private int firstIteration = Integer.MIN_VALUE;
	private final Map<String, double []> mode2numberofLegs = new HashMap<>();
	private final Set<String> modeHistory = new HashSet<>();

	@Inject
	private Scenario scenario;

	@Override
	public void notifyStartup(StartupEvent event) {
		this.firstIteration = event.getServices().getConfig().controler().getFirstIteration();
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		AgentAnalysisFilter filter1 = new AgentAnalysisFilter(scenario);
		filter1.setSubpopulation("person");
		filter1.setPersonAttribute("berlin");
		filter1.setPersonAttributeName("home-activity-zone");
		filter1.preProcess(scenario);
		filter1.setZoneFile(null);
		filter1.setRelevantActivityType(null);
		filter1.preProcess(scenario);
		
		String outputDir = scenario.getConfig().controler().getOutputDirectory();
		
		ModeAnalysis analysis = new ModeAnalysis(scenario, filter1);
		analysis.run();
		
		File directory = new File(outputDir);
		directory.mkdirs();
		
		analysis.writeModeShares(outputDir);
		analysis.writeTripRouteDistances(outputDir);
		analysis.writeTripEuclideanDistances(outputDir);
		
		final List<Tuple<Double, Double>> distanceGroups = new ArrayList<>();
		distanceGroups.add(new Tuple<>(0., 1000.));
		distanceGroups.add(new Tuple<>(1000., 3000.));
		distanceGroups.add(new Tuple<>(3000., 5000.));
		distanceGroups.add(new Tuple<>(5000., 10000.));
		distanceGroups.add(new Tuple<>(10000., 20000.));
		distanceGroups.add(new Tuple<>(20000., 100000.));
		analysis.writeTripRouteDistances(outputDir, distanceGroups);
		analysis.writeTripEuclideanDistances(outputDir, distanceGroups);
				
		Map<String, Integer > mode2legs = analysis.getMode2TripCounterFiltered();
		modeHistory.addAll(mode2legs.keySet());
		modeHistory.stream().filter(e -> ! mode2legs.containsKey(e)).forEach(e -> mode2legs.put(e, 0));

		int itNrIndex = event.getIteration() - this.firstIteration;

		for(String mode : mode2legs.keySet()) {
			if ( ! mode2numberofLegs.containsKey(mode)){ // initialize
				double [] legs = new double [itNrIndex+1];
				legs[itNrIndex] = mode2legs.get(mode);
				mode2numberofLegs.put(mode, legs);
			} else {
				double [] legsSoFar = mode2numberofLegs.get(mode);
				double [] legsNew = new double[legsSoFar.length+1];
				System.arraycopy(legsSoFar,0,legsNew,0,legsSoFar.length);
				legsNew[itNrIndex] = mode2legs.get(mode);
				mode2numberofLegs.put(mode,legsNew);
			}
		}

		if(itNrIndex == 0) return;

		//plot data here...
		XYLineChart chart = new XYLineChart("Modal Split", "iteration", "Number of legs");

		// x-series
		double[] iterations = new double[itNrIndex + 1];
		for (int i = 0; i <= itNrIndex; i++) {
			iterations[i] = this.firstIteration + i;
		}
		
		//y series
		for(String mode : this.mode2numberofLegs.keySet()){
			double [] values = new double [itNrIndex+1]; // array of only available data
			System.arraycopy(this.mode2numberofLegs.get(mode), 0, values, 0, itNrIndex + 1);
			chart.addSeries(mode, iterations, values);
		}
		
		// others--
		chart.addMatsimLogo();
        chart.saveAsPng(outputDir+"/modalSplit_berlin.png", 800, 600);
	}
}


