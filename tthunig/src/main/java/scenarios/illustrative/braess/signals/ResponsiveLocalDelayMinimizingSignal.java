/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
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
package scenarios.illustrative.braess.signals;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import scenarios.illustrative.analysis.TtAbstractAnalysisTool;

/**
 * @author tthunig
 *
 */
@Singleton
public final class ResponsiveLocalDelayMinimizingSignal implements AfterMobsimListener {

	private static final Logger LOG = Logger.getLogger(ResponsiveLocalDelayMinimizingSignal.class);

	private static final int INTERVAL = 100;
	private static final int FIRST_INTERVENTION = 100;
	private static final int LAST_INTERVENTION = 1500;

	private Map<Id<Link>, Double> link2avgDelay = new HashMap<>();
	private Double[] absoluteDelay;
	private Double[] avgDelay;
	private Double[] freeflowRouteTT = {11*60 + 3., 3*60 + 3., 11*60 + 3.};

	@Inject
	Scenario scenario;
	@Inject
	TtAbstractAnalysisTool analyzeTool;

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		// change signal green split every INTERVAL iteration
		if (event.getIteration() >= FIRST_INTERVENTION && event.getIteration() <= LAST_INTERVENTION &&
				event.getIteration() % INTERVAL == 0 && event.getIteration() != scenario.getConfig().controler().getFirstIteration()) {
//			computeAbsoluteDelays();
			computeAverageDelays();
			LOG.info("+++ Iteration " + event.getIteration() + ". Update signal green split...");
			updateSignals();
		}
	}

	// note: this does not consider spill back delays
	@Deprecated
	private void computeAvgDelays(AfterMobsimEvent event) {
		TravelTime travelTime = event.getServices().getLinkTravelTimes();
		int timeBinSize = scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize();

		for (Link link : scenario.getNetwork().getLinks().values()) {
			double freespeedTT = link.getLength() / link.getFreespeed();

			int timeBinCounter = 0;
			double summedDelay = 0.0;
			for (int endTime = timeBinSize; endTime <= scenario.getConfig().travelTimeCalculator().getMaxTime(); endTime = endTime + timeBinSize) {
				double avgDelay = travelTime.getLinkTravelTime(link, (endTime - timeBinSize / 2.), null, null) - freespeedTT;
				summedDelay += avgDelay;
				timeBinCounter++;
			}
			link2avgDelay.put(link.getId(), summedDelay / timeBinCounter);
			LOG.info("Link id: " + link.getId() + ", avg delay: " + summedDelay / timeBinCounter);
		}
	}
	
	private void computeAbsoluteDelays() {
		absoluteDelay = new Double[analyzeTool.getNumberOfRoutes()];
		for (int route=0; route < analyzeTool.getNumberOfRoutes(); route++) {
			absoluteDelay[route] = analyzeTool.getTotalRouteTTs()[route] - analyzeTool.getRouteUsers()[route] * freeflowRouteTT[route];
		}
	}

	private void computeAverageDelays() {
		avgDelay = new Double[analyzeTool.getNumberOfRoutes()];
		for (int route=0; route < analyzeTool.getNumberOfRoutes(); route++) {
			avgDelay[route] = (analyzeTool.getTotalRouteTTs()[route] - analyzeTool.getRouteUsers()[route] * freeflowRouteTT[route]) / analyzeTool.getRouteUsers()[route];
		}
	}
	
	private void updateSignals() {
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalControlData signalControl = signalsData.getSignalControlData();

		SignalSystemControllerData signalControlSystem4 = signalControl.getSignalSystemControllerDataBySystemId().get(Id.create("signalSystem4", SignalSystem.class));
		SignalPlanData signalPlan = signalControlSystem4.getSignalPlanData().get(Id.create("1", SignalPlan.class));
		SortedMap<Id<SignalGroup>, SignalGroupSettingsData> signalGroupSettings = signalPlan.getSignalGroupSettingsDataByGroupId();
		SignalGroupSettingsData group3_4Setting = signalGroupSettings.get(Id.create("signal3_4.1", SignalGroup.class));
		SignalGroupSettingsData group2_4Setting = signalGroupSettings.get(Id.create("signal2_4.1", SignalGroup.class));

		// shift green time by one second depending on which delay is higher
//		int greenTimeShift = (int) Math.signum(link2avgDelay.get(Id.createLinkId("3_4")) - link2avgDelay.get(Id.createLinkId("2_4"))); // the time that 3_4 gets added
//		int greenTimeShift = 5 * (int) Math.signum(absoluteDelay[1] - absoluteDelay[2]); // the time that 3_4 gets added
		int greenTimeShift = 5 * (int) Math.signum(avgDelay[1] - avgDelay[2]); // the time that 3_4 gets added

		// group3_4 onset = 0, group2_4 dropping = 60. signal switch should stay inside this interval
		if (greenTimeShift != 0 && group3_4Setting.getDropping() + greenTimeShift > 0 && group2_4Setting.getOnset() + greenTimeShift < 60) {
			group3_4Setting.setDropping(group3_4Setting.getDropping() + greenTimeShift);
			group2_4Setting.setOnset(group2_4Setting.getOnset() + greenTimeShift);
			LOG.info("signal3_4.1: onset " + group3_4Setting.getOnset() + ", dropping " + group3_4Setting.getDropping());
			LOG.info("signal2_4.1: onset " + group2_4Setting.getOnset() + ", dropping " + group2_4Setting.getDropping());
		} else {
			// do nothing
			LOG.info("Signal control unchanged.");
		}
	}

}
