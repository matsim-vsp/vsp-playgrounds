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
package scenarios.illustrative.singleCrossing;

import org.matsim.contrib.signals.SignalSystemsConfigGroup.IntersectionLogic;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;

import analysis.TtTotalTravelTime;
import playground.dgrether.koehlerstrehlersignal.analysis.TtTotalDelay;

/**
 * @author tthunig
 */
public class RunSingleIntersectionWithUnprotectedLeftTurnLogic {

	private boolean vis = false;
	private boolean stochasticDemand = true;
	private final IntersectionLogic intersectionLogic;

	public RunSingleIntersectionWithUnprotectedLeftTurnLogic(IntersectionLogic intersectionLogic) {
		this.intersectionLogic = intersectionLogic;
	}
	
	/**
	 * Run the scenario with specified parameters and return the total delay of left
	 * turning vehicles.
	 */
	public double runSingleIntersection() {
		ComplexSingleCrossingScenario singleIntersection = new ComplexSingleCrossingScenario(250, 0.0, 1250, 0.2,
				ComplexSingleCrossingScenario.SignalControl.FIXED, null, null, vis, stochasticDemand, true, true,
				true, 5, false);
		singleIntersection.setIntersectionLogic(intersectionLogic);
		singleIntersection.setProtectedLeftTurnForFixedTimeSignals(false);
		Controler controler = singleIntersection.defineControler();
		AnalyzeSingleIntersectionLeftTurnDelays leftTurnDelay = new AnalyzeSingleIntersectionLeftTurnDelays();
		TtTotalDelay totalDelay = new TtTotalDelay(controler.getScenario().getNetwork(), controler.getEvents());
		TtTotalTravelTime totalTt = new TtTotalTravelTime();
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(leftTurnDelay);
			}
		});		
		controler.run();

		System.out.println("left turn delays: " + leftTurnDelay.getLeftTurnDelay() + " (including red light delays)");
		System.out.println("stucked agents: " + leftTurnDelay.getStuckCount());
		System.out.println("total travel time: " + totalTt.getTotalTt());
		System.out.println("total delay: " + totalDelay.getTotalDelay());

		return leftTurnDelay.getLeftTurnDelay();
	}

	public static void main(String[] args) {

		RunSingleIntersectionWithUnprotectedLeftTurnLogic runClass = new RunSingleIntersectionWithUnprotectedLeftTurnLogic(
				IntersectionLogic.CONFLICTING_DIRECTIONS_AND_TURN_RESTRICTIONS);
		runClass.vis = true;
		runClass.runSingleIntersection();
	}

	public void setVis(boolean vis) {
		this.vis = vis;
	}

	public void setStochasticDemand(boolean stochasticDemand) {
		this.stochasticDemand = stochasticDemand;
	}

}
