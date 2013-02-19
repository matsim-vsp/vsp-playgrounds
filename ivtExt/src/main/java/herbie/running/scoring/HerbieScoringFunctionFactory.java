/* *********************************************************************** *
 * project: org.matsim.*
 * KTIYear3ScoringFunctionFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package herbie.running.scoring;

import herbie.running.config.HerbieConfigGroup;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalty;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;


public class HerbieScoringFunctionFactory extends org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory {

	private final Config config;
	private final HerbieConfigGroup ktiConfigGroup;
	private final TreeMap<Id, FacilityPenalty> facilityPenalties;
	private final ActivityFacilities facilities;
	private Network network;
	
	public HerbieScoringFunctionFactory(
			final Config config, 
			final HerbieConfigGroup ktiConfigGroup,
			final TreeMap<Id, FacilityPenalty> facilityPenalties,
			final ActivityFacilities facilities, 
			final Network network) {
		super(config.planCalcScore(), network);
		this.config = config;
		this.ktiConfigGroup = ktiConfigGroup;
		this.facilityPenalties = facilityPenalties;
		this.facilities = facilities;
		this.network = network;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Plan plan) {
		
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		
//		scoringFunctionAccumulator.addScoringFunction(new ActivityScoringFunction(plan, super.getParams()));
		
		scoringFunctionAccumulator.addScoringFunction(new ActivityScoringFunction(
				plan, 
				new CharyparNagelScoringParameters(config.planCalcScore()), 
				this.facilityPenalties,
				this.facilities,
				this.config));
		scoringFunctionAccumulator.addScoringFunction(new LegScoringFunction(
				plan, 
				new CharyparNagelScoringParameters(config.planCalcScore()),
				config,
				this.network,
				this.ktiConfigGroup));
		scoringFunctionAccumulator.addScoringFunction(new org.matsim.core.scoring.functions.CharyparNagelMoneyScoring(new CharyparNagelScoringParameters(config.planCalcScore())));
		scoringFunctionAccumulator.addScoringFunction(new org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring(new CharyparNagelScoringParameters(config.planCalcScore())));
		
		return scoringFunctionAccumulator;
	}

}
