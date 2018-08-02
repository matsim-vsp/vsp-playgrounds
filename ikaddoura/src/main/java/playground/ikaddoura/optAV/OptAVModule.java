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
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareConfigGroup;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareHandler;
import org.matsim.contrib.decongestion.DecongestionConfigGroup;
import org.matsim.contrib.decongestion.handler.DelayAnalysis;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider;
import org.matsim.contrib.taxi.run.TaxiConfigConsistencyChecker;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.AnalysisControlerListener;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.BasicPersonTripAnalysisHandler;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.NoiseAnalysisHandler;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.PersonMoneyLinkHandler;
import playground.ikaddoura.moneyTravelDisutility.MoneyTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.moneyTravelDisutility.MoneyTravelDisutilityModule;
import playground.ikaddoura.moneyTravelDisutility.data.AgentFilter;
import playground.ikaddoura.optAV.OptAVConfigGroup.TollingApproach;
import playground.ikaddoura.optAV.congestionAV.DecongestionModuleSAV;
import playground.ikaddoura.optAV.noiseAV.NoiseComputationModuleSAV;

/**
 * Idea:
 * (1) Adjusts the SAV's (routing- and dispatch-relevant) cost function (mode = 'taxi_optimizer')
 * (2) Adds the SAV's external costs to the fare paid by the passenger traveling with the SAV (mode = 'taxi'), i.e. waiting for or sitting inside the SAV.
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
		
	private final boolean useDefaultTravelDisutilityInTheCaseWithoutPricing = true;
	
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
		
		ModeParams taxiModeParams = null;
		if (this.getConfig().planCalcScore().getModes().get(TransportMode.taxi) != null) {
			taxiModeParams = this.getConfig().planCalcScore().getModes().get(TransportMode.taxi);
		} else {
			throw new RuntimeException("There is no 'taxi' mode in the planCalcScore config group.");
		}
		
		if (taxiOptimizerModeParams.getMonetaryDistanceRate() == 0.) {
			log.warn("The monetary distance rate for 'taxi_optimizer' is zero. Are you sure, the operating costs are zero?");
		}
		
		if (taxiOptimizerModeParams.getMonetaryDistanceRate() > 0.) {
			log.warn("The monetary distance rate for 'taxi_optimizer' should be negative.");
		}
		
		if (taxiOptimizerModeParams.getMarginalUtilityOfDistance() != 0.) {
			log.warn("The marginal utility of distance for 'taxi_optimizer' should be zero.");
		}
		
		if (taxiModeParams.getMonetaryDistanceRate() != 0.) {
			log.warn("The monetary distance rate for 'taxi' should be zero. The fare is considered somewhere else.");
		}
		
		if (taxiModeParams.getMarginalUtilityOfDistance() != 0.) {
			log.warn("The marginal utility of distance for 'taxi' should be zero.");
		}
		
		if (taxiOptimizerModeParams.getMarginalUtilityOfTraveling() != taxiModeParams.getMarginalUtilityOfTraveling()) {
			log.warn("The marginal utility of traveling for 'taxi' and 'taxi_optimizer' should be the same..."
					+ "Assumption: There is either a passenger in the SAV or there is a passenger waiting for the SAV.");	
		}
		
		TaxiFareConfigGroup taxiFareParams = ConfigUtils.addOrGetModule(this.getConfig(), TaxiFareConfigGroup.class);
		
		if (taxiOptimizerModeParams.getMonetaryDistanceRate() != (taxiFareParams.getDistanceFare_m() * (-1) )) {
			log.warn("Distance-based cost in plansCalcScore config group and taxiFareConfigGroup for 'taxi_optimizer' should be (approximately) the same..."
					+ "Assumption: A competitive market where the fare is equivalent to the marginal operating costs."
					+ " It may make sense to charge a slighlty higher fare...");
		}
		
		OptAVConfigGroup optAVParams = ConfigUtils.addOrGetModule(this.getConfig(), OptAVConfigGroup.class);

		if (optAVParams.getFixCostsSAVinsteadOfCar() > 0) {
			log.warn("Daily SAV fix costs (per user) should be lower than 0, meaning SAV users who are no longer private car users should 'earn' something."); 
		}
		
		NoiseConfigGroup noiseParams = ConfigUtils.addOrGetModule(this.getConfig(), NoiseConfigGroup.class);
		DecongestionConfigGroup decongestionParams = ConfigUtils.addOrGetModule(this.getConfig(), DecongestionConfigGroup.class);
		
		// #############################
		// passenger-vehicle tracking
		// #############################
				
		if (optAVParams.getTollingApproach().toString().equals(TollingApproach.ExternalCost.toString()) ||
				optAVParams.getTollingApproach().toString().equals(TollingApproach.PrivateAndExternalCost.toString()) ||
				optAVParams.getFixCostsSAVinsteadOfCar() != 0. || optAVParams.getDailyFixCostAllSAVusers() != 0.) {
			this.bind(SAVPassengerTracker.class).asEagerSingleton();
			addEventHandlerBinding().to(SAVPassengerTracker.class);
		}
		
		// #############################
		// fix cost pricing
		// #############################
		
		if (optAVParams.getFixCostsSAVinsteadOfCar() != 0. || optAVParams.getDailyFixCostAllSAVusers() != 0.) {
			this.bind(SAVFixCostHandler.class).asEagerSingleton();
			addEventHandlerBinding().to(SAVFixCostHandler.class);
		}
				
		// #############################
        // noise and congestion pricing
        // #############################
		
		if (optAVParams.isAccountForNoise()) {
			install(new NoiseComputationModuleSAV(scenario));
		}
				
		if (optAVParams.isAccountForCongestion()) {
			install(new DecongestionModuleSAV(scenario));
		}
		
		if (optAVParams.getTollingApproach().toString().equals(TollingApproach.ExternalCost.toString()) ||
				optAVParams.getTollingApproach().toString().equals(TollingApproach.PrivateAndExternalCost.toString())) {
			
			noiseParams.setInternalizeNoiseDamages(true);
			decongestionParams.setEnableDecongestionPricing(true);
			
		} else {
			noiseParams.setInternalizeNoiseDamages(false);
			decongestionParams.setEnableDecongestionPricing(false);
		}
		
		// #############################
		// dvrp / taxi
		// #############################

		DvrpConfigGroup.get(this.getConfig()).setMode(TransportMode.taxi);
		DvrpConfigGroup.get(this.getConfig()).setNetworkMode(TransportMode.car);
		this.getConfig().addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
		this.getConfig().checkConsistency();
        
		if (optAVParams.isChargeOperatingCostsFromPassengers()) addEventHandlerBinding().to(TaxiFareHandler.class).asEagerSingleton();
		
        // #############################
        // travel disutility
        // #############################
		               
		// taxi_optimizer
		
		if (optAVParams.isChargeTollsFromSAVDriver()) {
			if (optAVParams.getTollingApproach().toString().equals(TollingApproach.ExternalCost.toString())) {
//				MoneyTimeDistanceTravelDisutilityFactory dvrpTravelDisutilityFactory =
//					new MoneyTimeDistanceTravelDisutilityFactory(null);
//	    			install(new MoneyTravelDisutilityModule(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER, dvrpTravelDisutilityFactory));
				
				throw new RuntimeException("Not supported. Aborting...");
	        	
	        } else if (optAVParams.getTollingApproach().toString().equals(TollingApproach.PrivateAndExternalCost.toString())) {
	        	
	        		// old approach
//	        		MoneyTimeDistanceTravelDisutilityFactory dvrpTravelDisutilityFactory =
//	        			new MoneyTimeDistanceTravelDisutilityFactory(
//	        				new RandomizingTimeDistanceTravelDisutilityFactory(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER, this.getConfig().planCalcScore())
//					);
//	        		install(new MoneyTravelDisutilityModule(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER, dvrpTravelDisutilityFactory));
	        	
	        		// new approach
        			install(new DvrpMoneyTravelDisutilityModule(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER, new DvrpMoneyTimeDistanceTravelDisutilityFactory()));

	        		
	        } else if (optAVParams.getTollingApproach().toString().equals(TollingApproach.NoPricing.toString())) {
	        	
	        	if (useDefaultTravelDisutilityInTheCaseWithoutPricing) {
					log.info("Using the default travel disutility for SAV drivers / the taxi optimizer (case without pricing).");
				} else {
					RandomizingTimeDistanceTravelDisutilityFactory defaultTravelDisutilityFactory =
						new RandomizingTimeDistanceTravelDisutilityFactory(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER, this.getConfig().planCalcScore()); 
					this.addTravelDisutilityFactoryBinding(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER).toInstance(defaultTravelDisutilityFactory);				
				}
	        }
			
		} else {
			
			if (useDefaultTravelDisutilityInTheCaseWithoutPricing) {
				log.info("Using the default travel disutility for SAV drivers / the taxi optimizer (case without pricing).");
			} else {
				RandomizingTimeDistanceTravelDisutilityFactory defaultTravelDisutilityFactory =
					new RandomizingTimeDistanceTravelDisutilityFactory(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER, this.getConfig().planCalcScore()); 
				this.addTravelDisutilityFactoryBinding(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER).toInstance(defaultTravelDisutilityFactory);				
			}
		}
		
		// car user
		
		if (optAVParams.isChargeTollsFromCarUsers()) {
			if (optAVParams.getTollingApproach().toString().equals(TollingApproach.ExternalCost.toString())) {
				MoneyTimeDistanceTravelDisutilityFactory dvrpTravelDisutilityFactory =
					new MoneyTimeDistanceTravelDisutilityFactory(null);     
	    			install(new MoneyTravelDisutilityModule(TransportMode.car, dvrpTravelDisutilityFactory));
	        	
	        } else if (optAVParams.getTollingApproach().toString().equals(TollingApproach.PrivateAndExternalCost.toString())) {
	        		MoneyTimeDistanceTravelDisutilityFactory dvrpTravelDisutilityFactory =
	        			new MoneyTimeDistanceTravelDisutilityFactory(
	        				new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, this.getConfig().planCalcScore())
	        			);
	        		install(new MoneyTravelDisutilityModule(TransportMode.car, dvrpTravelDisutilityFactory));
	        	
	        } else if (optAVParams.getTollingApproach().toString().equals(TollingApproach.NoPricing.toString())) {
	        		
	        		if (useDefaultTravelDisutilityInTheCaseWithoutPricing) {
					log.info("Using the default travel disutility for car drivers (case without pricing).");
				} else {
					RandomizingTimeDistanceTravelDisutilityFactory defaultTravelDisutilityFactory =
						new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, this.getConfig().planCalcScore()); 
					this.addTravelDisutilityFactoryBinding(TransportMode.car).toInstance(defaultTravelDisutilityFactory);
				}        	
	        }
			
		} else {	
			
			if (useDefaultTravelDisutilityInTheCaseWithoutPricing) {
				log.info("Using the default travel disutility for car drivers (case without pricing).");
			} else {
				RandomizingTimeDistanceTravelDisutilityFactory defaultTravelDisutilityFactory =
					new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, this.getConfig().planCalcScore()); 
				this.addTravelDisutilityFactoryBinding(TransportMode.car).toInstance(defaultTravelDisutilityFactory);
			}
		}
		
		// account for vehicle type (taxi vs. car user)
		if ((optAVParams.isChargeTollsFromCarUsers() == false && optAVParams.isChargeTollsFromSAVDriver() == true) 
				|| (optAVParams.isChargeTollsFromCarUsers() == true && optAVParams.isChargeTollsFromSAVDriver() == false))  {
			
			log.info("Using an agent filter which differentiates between taxi and other vehicles...");
			this.bind(AgentFilter.class).toInstance(new AVAgentFilter());
		} else {
			log.info("Not binding any agent filter. "
					+ "The computation of average toll payments does not differentiate between taxi/rt and a normal car.");
		}
		
		// #############################
		// analysis
		// #############################
		
		if (optAVParams.isRunDefaultAnalysis()) {
			this.bind(BasicPersonTripAnalysisHandler.class).asEagerSingleton();
			this.addEventHandlerBinding().to(BasicPersonTripAnalysisHandler.class);

			this.bind(NoiseAnalysisHandler.class).asEagerSingleton();
			this.addEventHandlerBinding().to(NoiseAnalysisHandler.class);

			this.bind(PersonMoneyLinkHandler.class).asEagerSingleton();
			this.addEventHandlerBinding().to(PersonMoneyLinkHandler.class);
			
			if (!optAVParams.isAccountForCongestion()) {
				this.bind(DelayAnalysis.class).asEagerSingleton();
				this.addEventHandlerBinding().to(DelayAnalysis.class);
			}
			
			this.addControlerListenerBinding().to(AnalysisControlerListener.class);
		}
				
	}
}

