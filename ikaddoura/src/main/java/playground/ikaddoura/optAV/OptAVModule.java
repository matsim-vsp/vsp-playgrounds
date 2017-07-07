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

package playground.ikaddoura.optAV;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.noise.NoiseCalculationOnline;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.NoiseModule;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider;
import org.matsim.contrib.taxi.run.TaxiConfigConsistencyChecker;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.taxi.run.TaxiOutputModule;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.PersonTripAnalysisModule;
import playground.ikaddoura.decongestion.DecongestionConfigGroup;
import playground.ikaddoura.decongestion.DecongestionConfigGroup.DecongestionApproach;
import playground.ikaddoura.decongestion.DecongestionModule;
import playground.ikaddoura.moneyTravelDisutility.MoneyTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.moneyTravelDisutility.MoneyTravelDisutilityModule;
import playground.ikaddoura.optAV.OptAVConfigGroup.OptAVApproach;

/**
* @author ikaddoura
*/

public class OptAVModule extends AbstractModule {

	private final Scenario scenario;

	public OptAVModule(Scenario scenario) {
		this.scenario = scenario;
	}
	
	@Override
	public void install() {
		
		// #############################
		// pricing
		// #############################
		
		NoiseConfigGroup noiseParams = ConfigUtils.addOrGetModule(this.getConfig(), NoiseConfigGroup.class);
		OptAVConfigGroup optAVParams = ConfigUtils.addOrGetModule(this.getConfig(), OptAVConfigGroup.class);
		DecongestionConfigGroup decongestionParams = ConfigUtils.addOrGetModule(this.getConfig(), DecongestionConfigGroup.class);

		if (optAVParams.isAccountForNoise()) {
			install(new NoiseModule(scenario));
		}
				
		if (optAVParams.isAccountForCongestion()) {
			install(new DecongestionModule(scenario));
		}
		
		if (optAVParams.getOptAVApproach().toString().equals(OptAVApproach.ExternalCost.toString()) ||
				optAVParams.getOptAVApproach().toString().equals(OptAVApproach.PrivateAndExternalCost.toString())) {
			
			// internalize
			noiseParams.setInternalizeNoiseDamages(true);
			decongestionParams.setDecongestionApproach(DecongestionApproach.P_MC);
			
		} else {
			noiseParams.setInternalizeNoiseDamages(false);
			decongestionParams.setDecongestionApproach(DecongestionApproach.NoPricing);
		}
		
		// #############################
		// dvrp / taxi
		// #############################

		DvrpConfigGroup.get(this.getConfig()).setMode(TaxiModule.TAXI_MODE);
		this.getConfig().addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
		this.getConfig().checkConsistency();
        
		install(new TaxiOutputModule());
		install(new TaxiModule());
		
        // #############################
        // travel disutility
        // #############################
       
        TravelDisutilityFactory dvrpTravelDisutilityFactory = null;
        
		if (optAVParams.getOptAVApproach().toString().equals(OptAVApproach.ExternalCost.toString())) {
        	dvrpTravelDisutilityFactory = new MoneyTimeDistanceTravelDisutilityFactory(null);        	
        	
        } else if (optAVParams.getOptAVApproach().toString().equals(OptAVApproach.PrivateAndExternalCost.toString())) {
        	dvrpTravelDisutilityFactory = new MoneyTimeDistanceTravelDisutilityFactory(
				new RandomizingTimeDistanceTravelDisutilityFactory(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER, this.getConfig().planCalcScore()));
       
        } else if (optAVParams.getOptAVApproach().toString().equals(OptAVApproach.NoPricing.toString())) {
        	dvrpTravelDisutilityFactory = new RandomizingTimeDistanceTravelDisutilityFactory(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER, this.getConfig().planCalcScore()); 
        	// TODO: acccount for operating costs...
        }
		
    	this.addTravelDisutilityFactoryBinding(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER).toInstance(dvrpTravelDisutilityFactory);

		// #############################
		// welfare analysis
		// #############################

		install(new PersonTripAnalysisModule());
	}

}

