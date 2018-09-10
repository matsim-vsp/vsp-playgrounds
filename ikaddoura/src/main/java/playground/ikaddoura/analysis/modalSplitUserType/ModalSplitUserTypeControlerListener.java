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
	
	private final Map<String, double []> mode2numberofLegs1 = new HashMap<>();
	private final Set<String> modeHistory1 = new HashSet<>();
	
	private final Map<String, double []> mode2numberofLegs2 = new HashMap<>();
	private final Set<String> modeHistory2 = new HashSet<>();

	@Inject
	private Scenario scenario;

	@Override
	public void notifyStartup(StartupEvent event) {
		this.firstIteration = event.getServices().getConfig().controler().getFirstIteration();
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		AgentAnalysisFilter filter1 = new AgentAnalysisFilter(scenario);
		filter1.setPersonAttribute("berlin");
		filter1.setPersonAttributeName("home-activity-zone");
		filter1.preProcess(scenario);
		
		AgentAnalysisFilter filter2 = new AgentAnalysisFilter(scenario);
		filter2.preProcess(scenario);
		
		String outputDir = scenario.getConfig().controler().getOutputDirectory();
		String outputDirIteration = scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/";

		ModeAnalysis analysis1 = new ModeAnalysis(scenario, filter1);
		analysis1.run();
		
		ModeAnalysis analysis2 = new ModeAnalysis(scenario, filter2);
		analysis2.run();
		
		analysis1.writeModeShares(outputDirIteration);
		analysis2.writeModeShares(outputDirIteration);
		
		final List<Tuple<Double, Double>> distanceGroups = new ArrayList<>();
		distanceGroups.add(new Tuple<>(0., 1000.));
		distanceGroups.add(new Tuple<>(1000., 3000.));
		distanceGroups.add(new Tuple<>(3000., 5000.));
		distanceGroups.add(new Tuple<>(5000., 10000.));
		distanceGroups.add(new Tuple<>(10000., 20000.));
		distanceGroups.add(new Tuple<>(20000., 100000.));
		analysis1.writeTripEuclideanDistances(outputDirIteration, distanceGroups);
		analysis2.writeTripEuclideanDistances(outputDirIteration, distanceGroups);
				
		Map<String, Integer > mode2legs1 = analysis1.getMode2TripCounterFiltered();
		modeHistory1.addAll(mode2legs1.keySet());
		modeHistory1.stream().filter(e -> ! mode2legs1.containsKey(e)).forEach(e -> mode2legs1.put(e, 0));
		
		Map<String, Integer > mode2legs2 = analysis2.getMode2TripCounterFiltered();
		modeHistory2.addAll(mode2legs2.keySet());
		modeHistory2.stream().filter(e -> ! mode2legs2.containsKey(e)).forEach(e -> mode2legs2.put(e, 0));

		int itNrIndex = event.getIteration() - this.firstIteration;

		for(String mode : mode2legs1.keySet()) {
			if ( ! mode2numberofLegs1.containsKey(mode)){ // initialize
				double [] legs = new double [itNrIndex+1];
				legs[itNrIndex] = mode2legs1.get(mode);
				mode2numberofLegs1.put(mode, legs);
			} else {
				double [] legsSoFar = mode2numberofLegs1.get(mode);
				double [] legsNew = new double[legsSoFar.length+1];
				System.arraycopy(legsSoFar,0,legsNew,0,legsSoFar.length);
				legsNew[itNrIndex] = mode2legs1.get(mode);
				mode2numberofLegs1.put(mode,legsNew);
			}
		}
		
		for(String mode : mode2legs2.keySet()) {
			if ( ! mode2numberofLegs2.containsKey(mode)){ // initialize
				double [] legs = new double [itNrIndex+1];
				legs[itNrIndex] = mode2legs2.get(mode);
				mode2numberofLegs2.put(mode, legs);
			} else {
				double [] legsSoFar = mode2numberofLegs2.get(mode);
				double [] legsNew = new double[legsSoFar.length+1];
				System.arraycopy(legsSoFar,0,legsNew,0,legsSoFar.length);
				legsNew[itNrIndex] = mode2legs2.get(mode);
				mode2numberofLegs2.put(mode,legsNew);
			}
		}

		if(itNrIndex == 0) return;

		//plot data here...
		XYLineChart chart1 = new XYLineChart("Modal Split", "iteration", "Number of legs");
		XYLineChart chart2 = new XYLineChart("Modal Split", "iteration", "Number of legs");

		// x-series
		double[] iterations = new double[itNrIndex + 1];
		for (int i = 0; i <= itNrIndex; i++) {
			iterations[i] = this.firstIteration + i;
		}
		
		//y series
		for(String mode : this.mode2numberofLegs1.keySet()){
			double [] values = new double [itNrIndex+1]; // array of only available data
			System.arraycopy(this.mode2numberofLegs1.get(mode), 0, values, 0, itNrIndex + 1);
			chart1.addSeries(mode, iterations, values);
		}
		
		for(String mode : this.mode2numberofLegs2.keySet()){
			double [] values = new double [itNrIndex+1]; // array of only available data
			System.arraycopy(this.mode2numberofLegs2.get(mode), 0, values, 0, itNrIndex + 1);
			chart2.addSeries(mode, iterations, values);
		}
		
		// others--
		chart1.addMatsimLogo();
        chart1.saveAsPng(outputDir+"/modalSplit_berlin.png", 800, 600);
        
        chart2.addMatsimLogo();
        chart2.saveAsPng(outputDir+"/modalSplit_all.png", 800, 600);
        
        
	}
}


