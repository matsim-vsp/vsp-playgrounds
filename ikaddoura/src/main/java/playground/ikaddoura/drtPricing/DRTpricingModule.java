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

package playground.ikaddoura.drtPricing;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareConfigGroup;
import org.matsim.contrib.decongestion.DecongestionConfigGroup;
import org.matsim.contrib.drt.optimizer.DefaultDrtOptimizer;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;

import playground.ikaddoura.drtPricing.congestionAV.DecongestionModuleSAV;
import playground.ikaddoura.drtPricing.disutility.DvrpMoneyTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.drtPricing.disutility.DvrpMoneyTravelDisutilityModule;
import playground.ikaddoura.drtPricing.noiseAV.NoiseComputationModuleSAV;
import playground.ikaddoura.moneyTravelDisutility.MoneyTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.moneyTravelDisutility.MoneyTravelDisutilityModule;
import playground.ikaddoura.moneyTravelDisutility.data.AgentFilter;

/**
 * Idea:
 * (1) Adjusts the DRT's (routing- and dispatch-relevant) cost function (mode = 'drt_optimizer')
 * (2) Adds the SAV's external costs to the fare paid by the passenger traveling with the SAV (mode = 'drt'), i.e. waiting for or traveling with DRT.
 * 
 * 
* @author ikaddoura
*/

public class DRTpricingModule extends AbstractModule {
	private static final Logger log = Logger.getLogger(DRTpricingModule.class);

	private final Scenario scenario;
	private final String privateCarMode;
	
	public DRTpricingModule(Scenario scenario, String privateCarMode) {
		this.scenario = scenario;
		this.privateCarMode = privateCarMode;
	}
		
	private final boolean useDefaultTravelDisutilityInTheCaseWithoutPricing = true;
	
	@Override
	public void install() {
		
		// #############################
		// consistency check
		// #############################
		
		ModeParams drtOptimizerModeParams = null;
		if (this.getConfig().planCalcScore().getModes().get(DefaultDrtOptimizer.DRT_OPTIMIZER) != null) {
			drtOptimizerModeParams = this.getConfig().planCalcScore().getModes().get(DefaultDrtOptimizer.DRT_OPTIMIZER);
		} else {
			throw new RuntimeException("There is no 'drt_optimizer' mode in the planCalcScore config group.");
		}
		
		ModeParams drtModeParams = null;
		if (this.getConfig().planCalcScore().getModes().get(TransportMode.drt) != null) {
			drtModeParams = this.getConfig().planCalcScore().getModes().get(TransportMode.drt);
		} else {
			throw new RuntimeException("There is no 'taxi' mode in the planCalcScore config group.");
		}
		
		if (drtOptimizerModeParams.getMonetaryDistanceRate() == 0.) {
			log.warn("The monetary distance rate for 'taxi_optimizer' is zero. Are you sure, the operating costs are zero?");
		}
		
		if (drtOptimizerModeParams.getMonetaryDistanceRate() > 0.) {
			log.warn("The monetary distance rate for 'taxi_optimizer' should be negative.");
		}
		
		if (drtOptimizerModeParams.getMarginalUtilityOfDistance() != 0.) {
			log.warn("The marginal utility of distance for 'taxi_optimizer' should be zero.");
		}
		
		if (drtModeParams.getMonetaryDistanceRate() != 0.) {
			log.warn("The monetary distance rate for 'taxi' should be zero. The fare is considered somewhere else.");
		}
		
		if (drtModeParams.getMarginalUtilityOfDistance() != 0.) {
			log.warn("The marginal utility of distance for 'taxi' should be zero.");
		}
		
		if (drtOptimizerModeParams.getMarginalUtilityOfTraveling() != drtModeParams.getMarginalUtilityOfTraveling()) {
			log.warn("The marginal utility of traveling for 'taxi' and 'taxi_optimizer' should be the same..."
					+ "Assumption: There is either a passenger in the SAV or there is a passenger waiting for the SAV.");	
		}
		
		TaxiFareConfigGroup taxiFareParams = ConfigUtils.addOrGetModule(this.getConfig(), TaxiFareConfigGroup.class);
		
		if (drtOptimizerModeParams.getMonetaryDistanceRate() != (taxiFareParams.getDistanceFare_m() * (-1) )) {
			log.warn("Distance-based cost in plansCalcScore config group and taxiFareConfigGroup for 'taxi_optimizer' should be (approximately) the same..."
					+ "Assumption: A competitive market where the fare is equivalent to the marginal operating costs."
					+ " It may make sense to charge a slighlty higher fare...");
		}
		
		DrtPricingConfigGroup drtPricingParams = ConfigUtils.addOrGetModule(this.getConfig(), DrtPricingConfigGroup.class);		
		NoiseConfigGroup noiseParams = ConfigUtils.addOrGetModule(this.getConfig(), NoiseConfigGroup.class);
		DecongestionConfigGroup decongestionParams = ConfigUtils.addOrGetModule(this.getConfig(), DecongestionConfigGroup.class);
		
		// #############################
		// passenger-vehicle tracking
		// #############################
				
		this.bind(SAVPassengerTracker.class).asEagerSingleton();
		addEventHandlerBinding().to(SAVPassengerTracker.class);
				
		// #############################
        // noise and congestion pricing
        // #############################
		
		if (drtPricingParams.isAccountForNoise()) {
			
			// TODO: make sure the noise package works for any network mode
						
			if (drtPricingParams.isChargeSAVTollsFromPassengers() || drtPricingParams.isChargeTollsFromCarUsers() || drtPricingParams.isChargeTollsFromSAVDriver()) {
				noiseParams.setInternalizeNoiseDamages(true);
			} else {
				noiseParams.setInternalizeNoiseDamages(false);
			}
			install(new NoiseComputationModuleSAV(this.scenario));
		} else {
			noiseParams.setInternalizeNoiseDamages(false);
		}
				
		if (drtPricingParams.isAccountForCongestion()) {
			
			// TODO: make sure the decongestion package works for any network mode
						
			if (drtPricingParams.isChargeSAVTollsFromPassengers() || drtPricingParams.isChargeTollsFromCarUsers() || drtPricingParams.isChargeTollsFromSAVDriver()) {
				decongestionParams.setEnableDecongestionPricing(true);
			} else {
				decongestionParams.setEnableDecongestionPricing(false);
			}
			install(new DecongestionModuleSAV(this.scenario));
		} else {
			decongestionParams.setEnableDecongestionPricing(false);
		}
		
        // #############################
        // travel disutility
        // #############################
		               
		// drt_optimizer
		
		if (drtPricingParams.isChargeTollsFromSAVDriver()) {
			
			install(new DvrpMoneyTravelDisutilityModule(DefaultDrtOptimizer.DRT_OPTIMIZER, new DvrpMoneyTimeDistanceTravelDisutilityFactory()));
			
//			MoneyTimeDistanceTravelDisutilityFactory dvrpTravelDisutilityFactory =
//			new MoneyTimeDistanceTravelDisutilityFactory(
//				new RandomizingTimeDistanceTravelDisutilityFactory(DefaultDrtOptimizer.DRT_OPTIMIZER, this.getConfig().planCalcScore())
//					);
//			install(new MoneyTravelDisutilityModule(DefaultDrtOptimizer.DRT_OPTIMIZER, dvrpTravelDisutilityFactory));
			
		} else {
			
			if (useDefaultTravelDisutilityInTheCaseWithoutPricing) {
				log.info("Using the default travel disutility for SAV drivers / the drt optimizer (case without pricing).");
			} else {
				install(new DvrpMoneyTravelDisutilityModule(DefaultDrtOptimizer.DRT_OPTIMIZER, new DvrpMoneyTimeDistanceTravelDisutilityFactory()));		
				
//				MoneyTimeDistanceTravelDisutilityFactory dvrpTravelDisutilityFactory =
//	        			new MoneyTimeDistanceTravelDisutilityFactory(
//	        				new RandomizingTimeDistanceTravelDisutilityFactory(DefaultDrtOptimizer.DRT_OPTIMIZER, this.getConfig().planCalcScore())
//					);
//	        		install(new MoneyTravelDisutilityModule(DefaultDrtOptimizer.DRT_OPTIMIZER, dvrpTravelDisutilityFactory));   	
			}
		}
		
		// car user
				
		if (drtPricingParams.isChargeTollsFromCarUsers()) {
						
			MoneyTimeDistanceTravelDisutilityFactory dvrpTravelDisutilityFactory =
        			new MoneyTimeDistanceTravelDisutilityFactory(
        				new RandomizingTimeDistanceTravelDisutilityFactory(privateCarMode, this.getConfig().planCalcScore())
        			);
        		install(new MoneyTravelDisutilityModule(privateCarMode, dvrpTravelDisutilityFactory));
			
		} else {	
			
			if (useDefaultTravelDisutilityInTheCaseWithoutPricing) {
				log.info("Using the default travel disutility for car drivers (case without pricing).");
			} else {
				RandomizingTimeDistanceTravelDisutilityFactory defaultTravelDisutilityFactory =
					new RandomizingTimeDistanceTravelDisutilityFactory(privateCarMode, this.getConfig().planCalcScore()); 
				this.addTravelDisutilityFactoryBinding(privateCarMode).toInstance(defaultTravelDisutilityFactory);
			}
		}
		
		// account for vehicle type (taxi vs. car user)
		if ((drtPricingParams.isChargeTollsFromCarUsers() == false && drtPricingParams.isChargeTollsFromSAVDriver() == true) 
				|| (drtPricingParams.isChargeTollsFromCarUsers() == true && drtPricingParams.isChargeTollsFromSAVDriver() == false))  {
			
			log.info("Using an agent filter which differentiates between taxi and other vehicles...");
			this.bind(AgentFilter.class).toInstance(new AVAgentFilter());
		} else {
			log.info("Not binding any agent filter. "
					+ "The computation of average toll payments does not differentiate between taxi/rt and a normal car.");
		}
				
	}
}

