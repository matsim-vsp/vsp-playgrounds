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

package playground.ikaddoura.savPricing.drtPricing;

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

import playground.ikaddoura.moneyTravelDisutility.MoneyTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.moneyTravelDisutility.MoneyTravelDisutilityModule;
import playground.ikaddoura.moneyTravelDisutility.data.AgentFilter;
import playground.ikaddoura.savPricing.AVAgentFilter;
import playground.ikaddoura.savPricing.drtPricing.congestionAV.DecongestionModuleSAV;
import playground.ikaddoura.savPricing.drtPricing.disutility.DvrpMoneyTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.savPricing.drtPricing.disutility.DvrpMoneyTravelDisutilityModule;
import playground.ikaddoura.savPricing.drtPricing.noiseAV.NoiseComputationModuleSAV;

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
		
		ModeParams drtModeParams = null;
		if (this.getConfig().planCalcScore().getModes().get(TransportMode.drt) != null) {
			drtModeParams = this.getConfig().planCalcScore().getModes().get(TransportMode.drt);
		} else {
			throw new RuntimeException("There is no 'drt' mode in the planCalcScore config group.");
		}
		
		ModeParams drtOptimizerModeParams = null;
		if (this.getConfig().planCalcScore().getModes().get(DefaultDrtOptimizer.DRT_OPTIMIZER) != null) {
			drtOptimizerModeParams = this.getConfig().planCalcScore().getModes().get(DefaultDrtOptimizer.DRT_OPTIMIZER);
		} else {
			log.warn("There is no 'drt_optimizer' mode in the planCalcScore config group. Probably using some default parameters...");
		}
		
		TaxiFareConfigGroup taxiFareParams = ConfigUtils.addOrGetModule(this.getConfig(), TaxiFareConfigGroup.class);
		
		if (drtOptimizerModeParams != null) {
			
			if (drtOptimizerModeParams.getMonetaryDistanceRate() == 0.) {
				log.warn("The monetary distance rate for 'drt_optimizer' is zero. Are you sure, the operating costs are zero?");
			}
			
			if (drtOptimizerModeParams.getMonetaryDistanceRate() > 0.) {
				log.warn("The monetary distance rate for 'drt_optimizer' should be negative.");
			}
			
			if (drtOptimizerModeParams.getMarginalUtilityOfDistance() != 0.) {
				log.warn("The marginal utility of distance for 'drt_optimizer' should be zero.");
			}
			
			if (drtOptimizerModeParams.getMarginalUtilityOfTraveling() != drtModeParams.getMarginalUtilityOfTraveling()) {
				log.warn("The marginal utility of traveling for 'drt' and 'drt_optimizer' should be the same..."
						+ "Assumption: There is either a passenger in the SAV or there is a passenger waiting for the SAV.");	
			}
			
			if (drtOptimizerModeParams.getMonetaryDistanceRate() != (taxiFareParams.getDistanceFare_m() * (-1) )) {
				log.warn("Distance-based cost in plansCalcScore config group and taxiFareConfigGroup for 'taxi_optimizer' should be (approximately) the same..."
						+ "Assumption: A competitive market where the fare is equivalent to the marginal operating costs."
						+ " It may make sense to charge a slighlty higher fare...");
			}
		}
		
		if (drtModeParams.getMonetaryDistanceRate() != 0.) {
			log.warn("The monetary distance rate for 'drt' should be zero. The fare is considered somewhere else.");
		}
		
		if (drtModeParams.getMarginalUtilityOfDistance() != 0.) {
			log.warn("The marginal utility of distance for 'drt' should be zero.");
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
			
			log.info("Charge tolls from SAV drivers. Modify the default travel disutility of the DRT optimizer.");
			install(new DvrpMoneyTravelDisutilityModule(DefaultDrtOptimizer.DRT_OPTIMIZER, new DvrpMoneyTimeDistanceTravelDisutilityFactory()));
			
//			MoneyTimeDistanceTravelDisutilityFactory dvrpTravelDisutilityFactory =
//			new MoneyTimeDistanceTravelDisutilityFactory(
//				new RandomizingTimeDistanceTravelDisutilityFactory(DefaultDrtOptimizer.DRT_OPTIMIZER, this.getConfig().planCalcScore())
//					);
//			install(new MoneyTravelDisutilityModule(DefaultDrtOptimizer.DRT_OPTIMIZER, dvrpTravelDisutilityFactory));
			
		} else {
			
			log.info("Tolls are not charged from SAV drivers. No need to modify the default travel disutility of the DRT optimizer.");
			
			if (useDefaultTravelDisutilityInTheCaseWithoutPricing) {
				log.info("Using the default travel disutility for SAV drivers / the DRT optimizer.");
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
						
			log.info("Charge tolls from private car users. Modify the default travel disutility of private car users.");

			MoneyTimeDistanceTravelDisutilityFactory dvrpTravelDisutilityFactory =
        			new MoneyTimeDistanceTravelDisutilityFactory(
        				new RandomizingTimeDistanceTravelDisutilityFactory(privateCarMode, this.getConfig().planCalcScore())
        			);
        		install(new MoneyTravelDisutilityModule(privateCarMode, dvrpTravelDisutilityFactory));
			
		} else {	
			
			log.info("Tolls are not charged from private car users. No need to modify the default travel disutility of private car users.");
			
			if (useDefaultTravelDisutilityInTheCaseWithoutPricing) {
				log.info("Using the default travel disutility for private car users.");
			} else {
				RandomizingTimeDistanceTravelDisutilityFactory defaultTravelDisutilityFactory =
					new RandomizingTimeDistanceTravelDisutilityFactory(privateCarMode, this.getConfig().planCalcScore()); 
				this.addTravelDisutilityFactoryBinding(privateCarMode).toInstance(defaultTravelDisutilityFactory);
			}
		}
		
		// account for vehicle type (drt vs. car user)
		if ((drtPricingParams.isChargeTollsFromCarUsers() == false && drtPricingParams.isChargeTollsFromSAVDriver() == true) 
				|| (drtPricingParams.isChargeTollsFromCarUsers() == true && drtPricingParams.isChargeTollsFromSAVDriver() == false))  {
			
			log.info("Using an agent filter which differentiates between DRT vehicles and other vehicles...");
			this.bind(AgentFilter.class).toInstance(new AVAgentFilter());
		} else {
			log.info("Not binding any agent filter. "
					+ "The computation of average toll payments does not differentiate between drt/rt and a normal car.");
		}
				
	}
}

