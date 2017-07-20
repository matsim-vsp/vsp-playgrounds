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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareConfigGroup;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareHandler;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.noise.NoiseComputationModule;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider;
import org.matsim.contrib.taxi.run.TaxiConfigConsistencyChecker;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.taxi.run.TaxiOutputModule;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.PersonTripAnalysisModule;
import playground.ikaddoura.decongestion.DecongestionConfigGroup;
import playground.ikaddoura.decongestion.DecongestionModule;
import playground.ikaddoura.moneyTravelDisutility.MoneyTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.moneyTravelDisutility.MoneyTravelDisutilityModule;
import playground.ikaddoura.moneyTravelDisutility.data.AgentFilter;
import playground.ikaddoura.optAV.OptAVConfigGroup.SAVTollingApproach;
import playground.ikaddoura.optAV.congestionAV.DecongestionModuleSAV;
import playground.ikaddoura.optAV.noiseAV.NoiseComputationModuleSAV;

/**
 * Idea:
 * (1) Adjust the SAV's (routing- and dispatch-relevant) cost function (mode = 'taxi_optimizer')
 * (2) Add the SAV's external costs to the fare paid by the passenger traveling with the SAV (mode = 'taxi'), i.e. waiting for or sitting inside the SAV.
 * 
 * Marginal operating costs are charged from the passengers via the 'TaxiFareHandler'
 * Marginal external costs are charged from the passengers via the 'SAVTolls2FareHandler'
 * 
 * 
 * 
* @author ikaddoura
*/

public class OptAVModule extends AbstractModule {
	private static final Logger log = Logger.getLogger(OptAVModule.class);

	private final Scenario scenario;

	public OptAVModule(Scenario scenario) {
		this.scenario = scenario;
	}
	
	@Override
	public void install() {
		
		// #############################
		// consistency check
		// #############################
		
		ModeParams taxiOptimizerModeParams = null;
		if (this.getConfig().planCalcScore().getModes().get(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER) != null) {
			taxiOptimizerModeParams = this.getConfig().planCalcScore().getModes().get(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER);
		} else {
			throw new RuntimeException("There is no 'taxi_optimizer' mode in the planCalcScore config group.");
		}
		
		if (taxiOptimizerModeParams.getMonetaryDistanceRate() == 0.) {
			log.warn("The monetary distance rate for 'taxi_optimizer' is zero. Are you sure, the operating costs are zero?");
		}
		
		if (taxiOptimizerModeParams.getMonetaryDistanceRate() > 0.) {
			throw new RuntimeException("The monetary distance rate for 'taxi_optimizer' should be negative.");
		}
		
		if (taxiOptimizerModeParams.getMarginalUtilityOfDistance() != 0.) {
			throw new RuntimeException("The marginal utility of distance for 'taxi_optimizer' should be zero.");
		}
		
		ModeParams taxiModeParams = null;
		if (this.getConfig().planCalcScore().getModes().get(TaxiModule.TAXI_MODE) != null) {
			taxiModeParams = this.getConfig().planCalcScore().getModes().get(TaxiModule.TAXI_MODE);
		} else {
			throw new RuntimeException("There is no 'taxi' mode in the planCalcScore config group.");
		}
		
		if (taxiModeParams.getMonetaryDistanceRate() != 0.) {
			throw new RuntimeException("The monetary distance rate for 'taxi' should be zero. The fare is considered somewhere else.");
		}
		
		if (taxiModeParams.getMarginalUtilityOfDistance() != 0.) {
			throw new RuntimeException("The marginal utility of distance for 'taxi' should be zero.");
		}
		
		if (taxiOptimizerModeParams.getMarginalUtilityOfTraveling() != taxiModeParams.getMarginalUtilityOfTraveling()) {
			throw new RuntimeException("The marginal utility of traveling for 'taxi' and 'taxi_optimizer' should be the same..."
					+ "Assumption: There is either a passenger in the SAV or there is a passenger waiting for the SAV.");	
		}
		
		TaxiFareConfigGroup taxiFareParams = ConfigUtils.addOrGetModule(this.getConfig(), TaxiFareConfigGroup.class);
		
		if (taxiOptimizerModeParams.getMonetaryDistanceRate() != (taxiFareParams.getDistanceFare_m() * (-1) )) {
			throw new RuntimeException("Distance-based cost in plansCalcScore config group and taxiFareConfigGroup for 'taxi_optimizer' should be the same..."
					+ "Assumption: A competitive market where the fare is equivalent to the marginal operating costs.");
		}
		
		OptAVConfigGroup optAVParams = ConfigUtils.addOrGetModule(this.getConfig(), OptAVConfigGroup.class);

		if (optAVParams.getFixCostsSAVinsteadOfCar() > optAVParams.getFixCostSAV()) {
			throw new RuntimeException("SAV capital costs (per user) should be lower than car capital costs (per user). Aborting... "); 
		}
		
		NoiseConfigGroup noiseParams = ConfigUtils.addOrGetModule(this.getConfig(), NoiseConfigGroup.class);
		DecongestionConfigGroup decongestionParams = ConfigUtils.addOrGetModule(this.getConfig(), DecongestionConfigGroup.class);
		
		// #############################
		// tag car owners
		// #############################
		
		if (optAVParams.isTagInitialCarUsers()) {
			for (Person person : this.scenario.getPopulation().getPersons().values()) {
				Plan selectedPlan = person.getSelectedPlan();
				if (selectedPlan == null) {
					throw new RuntimeException("No selected plan. Aborting...");
				}
				
				boolean personHasCarTrip = false;
				
				for (PlanElement pE : selectedPlan.getPlanElements()) {
					
					if (pE instanceof Leg) {
						Leg leg = (Leg) pE;
						if (leg.getMode().equals(TransportMode.car)) {
							personHasCarTrip = true;
						}	
					}	
				}
				person.getAttributes().putAttribute("CarOwnerInBaseCase", personHasCarTrip);					
			}	
		}		
		
		// #############################
		// pricing
		// #############################
		
		if (optAVParams.isChargeSAVTollsFromPassengers()) {
			this.bind(SAVPassengerTracker.class).asEagerSingleton();
			addEventHandlerBinding().to(SAVPassengerTracker.class);
		}
		
		if (optAVParams.isAccountForNoise()) {
			
			if (optAVParams.isChargeSAVTollsFromPassengers()) {
				install(new NoiseComputationModuleSAV(scenario));
			} else {
				install(new NoiseComputationModule(scenario));
			}
		}
				
		if (optAVParams.isAccountForCongestion()) {
			
			if (optAVParams.isChargeSAVTollsFromPassengers()) {
				install(new DecongestionModuleSAV(scenario));
			} else {
				install(new DecongestionModule(scenario));
			}
		}
		
		if (optAVParams.getOptAVApproach().toString().equals(SAVTollingApproach.ExternalCost.toString()) ||
				optAVParams.getOptAVApproach().toString().equals(SAVTollingApproach.PrivateAndExternalCost.toString())) {
			
			noiseParams.setInternalizeNoiseDamages(true);
			decongestionParams.setEnableDecongestionPricing(true);
			
		} else {
			noiseParams.setInternalizeNoiseDamages(false);
			decongestionParams.setEnableDecongestionPricing(false);
		}
		
		// #############################
		// dvrp / taxi
		// #############################

		DvrpConfigGroup.get(this.getConfig()).setMode(TaxiModule.TAXI_MODE);
		this.getConfig().addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
		this.getConfig().checkConsistency();
        
		install(new TaxiOutputModule());
		install(new TaxiModule());
		
		if (optAVParams.isChargeOperatingCostsFromPassengers()) addEventHandlerBinding().to(TaxiFareHandler.class).asEagerSingleton();
		
        // #############################
        // travel disutility
        // #############################
		
		this.bind(AgentFilter.class).toInstance(new AVAgentFilter());
               
		if (optAVParams.getOptAVApproach().toString().equals(SAVTollingApproach.ExternalCost.toString())) {
			MoneyTimeDistanceTravelDisutilityFactory dvrpTravelDisutilityFactory = new MoneyTimeDistanceTravelDisutilityFactory(null);     
        	
    		install(new MoneyTravelDisutilityModule(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER, dvrpTravelDisutilityFactory));
        	
        } else if (optAVParams.getOptAVApproach().toString().equals(SAVTollingApproach.PrivateAndExternalCost.toString())) {
        	MoneyTimeDistanceTravelDisutilityFactory dvrpTravelDisutilityFactory = new MoneyTimeDistanceTravelDisutilityFactory(
				new RandomizingTimeDistanceTravelDisutilityFactory(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER, this.getConfig().planCalcScore()));
       
    		install(new MoneyTravelDisutilityModule(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER, dvrpTravelDisutilityFactory));
        	
        } else if (optAVParams.getOptAVApproach().toString().equals(SAVTollingApproach.NoPricing.toString())) {
        	RandomizingTimeDistanceTravelDisutilityFactory defaultTravelDisutilityFactory = new RandomizingTimeDistanceTravelDisutilityFactory(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER, this.getConfig().planCalcScore()); 
        	
        	this.addTravelDisutilityFactoryBinding(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER).toInstance(defaultTravelDisutilityFactory);        	
        }
		
		// #############################
        // scoring
        // #############################
		
		addEventHandlerBinding().to(SAVFixCostHandler.class).asEagerSingleton();
		
		// #############################
		// welfare analysis
		// #############################

		install(new PersonTripAnalysisModule());
		
	}

}

