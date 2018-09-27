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

package playground.ikaddoura.savPricing;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareConfigGroup;
import org.matsim.contrib.decongestion.DecongestionConfigGroup;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;

import playground.ikaddoura.moneyTravelDisutility.MoneyTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.moneyTravelDisutility.MoneyTravelDisutilityModule;
import playground.ikaddoura.moneyTravelDisutility.data.AgentFilter;
import playground.ikaddoura.savPricing.congestionAV.DecongestionModuleSAV;
import playground.ikaddoura.savPricing.disutility.DvrpMoneyTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.savPricing.disutility.DvrpMoneyTravelDisutilityModule;
import playground.ikaddoura.savPricing.noiseAV.NoiseComputationModuleSAV;

/**
 * Idea:
 * (1) Adjusts the taxi (routing- and dispatch-relevant) cost function (mode = 'taxi_optimizer')
 * (2) Adds the SAV's external costs to the fare paid by the passenger traveling with the SAV (mode = 'taxi'), i.e. waiting for or traveling with taxi.
 * 
 * 
* @author ikaddoura
*/

public class TaxiPricingModule extends AbstractModule {
	private static final Logger log = Logger.getLogger(TaxiPricingModule.class);

	private final Scenario scenario;
	private final String privateCarMode;
	
	public TaxiPricingModule(Scenario scenario, String privateCarMode) {
		this.scenario = scenario;
		this.privateCarMode = privateCarMode;
	}
		
	private final boolean useDefaultTravelDisutilityInTheCaseWithoutPricing = true;
	
	@Override
	public void install() {
		
		// #############################
		// consistency check
		// #############################
		
		ModeParams taxiModeParams = null;
		if (this.getConfig().planCalcScore().getModes().get(TransportMode.taxi) != null) {
			taxiModeParams = this.getConfig().planCalcScore().getModes().get(TransportMode.taxi);
		} else {
			throw new RuntimeException("There is no 'taxi' mode in the planCalcScore config group.");
		}
		
		ModeParams taxiOptimizerModeParams = null;
		if (this.getConfig().planCalcScore().getModes().get(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER) != null) {
			taxiOptimizerModeParams = this.getConfig().planCalcScore().getModes().get(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER);
		} else {
			log.warn("There is no 'taxi_optimizer' mode in the planCalcScore config group. Probably using some default parameters...");
		}
		
		TaxiFareConfigGroup taxiFareParams = ConfigUtils.addOrGetModule(this.getConfig(), TaxiFareConfigGroup.class);
		
		if (taxiOptimizerModeParams != null) {
			
			if (taxiOptimizerModeParams.getMonetaryDistanceRate() == 0.) {
				log.warn("The monetary distance rate for 'taxi_optimizer' is zero. Are you sure, the operating costs are zero?");
			}
			
			if (taxiOptimizerModeParams.getMonetaryDistanceRate() > 0.) {
				log.warn("The monetary distance rate for 'taxi_optimizer' should be negative.");
			}
			
			if (taxiOptimizerModeParams.getMarginalUtilityOfDistance() != 0.) {
				log.warn("The marginal utility of distance for 'taxi_optimizer' should be zero.");
			}
			
			if (taxiOptimizerModeParams.getMarginalUtilityOfTraveling() != taxiModeParams.getMarginalUtilityOfTraveling()) {
				log.warn("The marginal utility of traveling for 'taxi' and 'taxi_optimizer' should be the same..."
						+ "Assumption: There is either a passenger in the SAV or there is a passenger waiting for the SAV.");	
			}
			
			if (taxiOptimizerModeParams.getMonetaryDistanceRate() != (taxiFareParams.getDistanceFare_m() * (-1) )) {
				log.warn("Distance-based cost in plansCalcScore config group and taxiFareConfigGroup for 'taxi_optimizer' should be (approximately) the same..."
						+ "Assumption: A competitive market where the fare is equivalent to the marginal operating costs."
						+ " It may make sense to charge a slighlty higher fare...");
			}
		}
		
		if (taxiModeParams.getMonetaryDistanceRate() != 0.) {
			log.warn("The monetary distance rate for 'taxi' should be zero. The fare is considered somewhere else.");
		}
		
		if (taxiModeParams.getMarginalUtilityOfDistance() != 0.) {
			log.warn("The marginal utility of distance for 'taxi' should be zero.");
		}
				
		TaxiPricingConfigGroup taxiPricingParams = ConfigUtils.addOrGetModule(this.getConfig(), TaxiPricingConfigGroup.class);		
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
		
		if (taxiPricingParams.isAccountForNoise()) {
						
			if (taxiPricingParams.isChargeSAVTollsFromPassengers() || taxiPricingParams.isChargeTollsFromCarUsers() || taxiPricingParams.isChargeTollsFromSAVDriver()) {
				noiseParams.setInternalizeNoiseDamages(true);
			} else {
				noiseParams.setInternalizeNoiseDamages(false);
			}
			install(new NoiseComputationModuleSAV(this.scenario));
		} else {
			noiseParams.setInternalizeNoiseDamages(false);
		}
				
		if (taxiPricingParams.isAccountForCongestion()) {
									
			if (taxiPricingParams.isChargeSAVTollsFromPassengers() || taxiPricingParams.isChargeTollsFromCarUsers() || taxiPricingParams.isChargeTollsFromSAVDriver()) {
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
		               
		// taxi_optimizer
		
		if (taxiPricingParams.isChargeTollsFromSAVDriver()) {
			
			log.info("Charge tolls from SAV drivers. Modify the default travel disutility of the taxi optimizer.");
			install(new DvrpMoneyTravelDisutilityModule(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER, new DvrpMoneyTimeDistanceTravelDisutilityFactory()));
			
//			MoneyTimeDistanceTravelDisutilityFactory dvrpTravelDisutilityFactory =
//			new MoneyTimeDistanceTravelDisutilityFactory(
//				new RandomizingTimeDistanceTravelDisutilityFactory(DefaultDrtOptimizer.DRT_OPTIMIZER, this.getConfig().planCalcScore())
//					);
//			install(new MoneyTravelDisutilityModule(DefaultDrtOptimizer.DRT_OPTIMIZER, dvrpTravelDisutilityFactory));
			
		} else {
			
			log.info("Tolls are not charged from SAV drivers. No need to modify the default travel disutility of the taxi optimizer.");
			
			if (useDefaultTravelDisutilityInTheCaseWithoutPricing) {
				log.info("Using the default travel disutility for SAV drivers / the taxi optimizer.");
			} else {
				install(new DvrpMoneyTravelDisutilityModule(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER, new DvrpMoneyTimeDistanceTravelDisutilityFactory()));		
				
//				MoneyTimeDistanceTravelDisutilityFactory dvrpTravelDisutilityFactory =
//	        			new MoneyTimeDistanceTravelDisutilityFactory(
//	        				new RandomizingTimeDistanceTravelDisutilityFactory(DefaultDrtOptimizer.DRT_OPTIMIZER, this.getConfig().planCalcScore())
//					);
//	        		install(new MoneyTravelDisutilityModule(DefaultDrtOptimizer.DRT_OPTIMIZER, dvrpTravelDisutilityFactory));   	
			}
		}
		
		// car user
				
		if (taxiPricingParams.isChargeTollsFromCarUsers()) {
						
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
		
		// account for vehicle type (taxi vs. car user)
		if ((taxiPricingParams.isChargeTollsFromCarUsers() == false && taxiPricingParams.isChargeTollsFromSAVDriver() == true) 
				|| (taxiPricingParams.isChargeTollsFromCarUsers() == true && taxiPricingParams.isChargeTollsFromSAVDriver() == false))  {
			
			log.info("Using an agent filter which differentiates between taxi vehicles and other vehicles...");
			this.bind(AgentFilter.class).toInstance(new AVAgentFilter());
		} else {
			log.info("Not binding any agent filter. "
					+ "The computation of average toll payments does not differentiate between taxi/rt and a normal car.");
		}
				
	}
}

