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
import org.matsim.contrib.drt.optimizer.DefaultDrtOptimizer;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;

import playground.ikaddoura.moneyTravelDisutility.MoneyTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.moneyTravelDisutility.MoneyTravelDisutilityModule;
import playground.ikaddoura.moneyTravelDisutility.data.AgentFilter;
import playground.ikaddoura.savPricing.congestionSAV.DecongestionModuleSAV;
import playground.ikaddoura.savPricing.disutility.SAVMoneyTravelDisutilityModule;
import playground.ikaddoura.savPricing.disutility.SAVOptimizerMoneyTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.savPricing.noiseSAV.NoiseComputationModuleSAV;

/**
 * Idea:
 * (1) Adjusts the sav (routing- and dispatch-relevant) cost function (mode = 'taxi_optimizer' / 'drt_optimizer')
 * (2) Adds the SAV's external costs to the fare paid by the passenger traveling with the SAV (mode = 'taxi/drt'), i.e. waiting for or traveling with an SAV.
 * 
 * 
* @author ikaddoura
*/

public class SAVPricingModule extends AbstractModule {
	private static final Logger log = Logger.getLogger(SAVPricingModule.class);

	private final Scenario scenario;
	private final String privateCarMode;
	
	public SAVPricingModule(Scenario scenario, String privateCarMode) {
		this.scenario = scenario;
		this.privateCarMode = privateCarMode;
	}
		
	private final boolean useDefaultTravelDisutilityInTheCaseWithoutPricing = true;
	
	@Override
	public void install() {
		
		SAVPricingConfigGroup savPricingParams = ConfigUtils.addOrGetModule(this.getConfig(), SAVPricingConfigGroup.class);		

		String savMode = savPricingParams.getSavMode();
		String savOptimizerMode;
		if (savMode.equals(TransportMode.taxi)) {
			savOptimizerMode = DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER;
		} else if (savMode.equals(TransportMode.drt)) {
			savOptimizerMode = DefaultDrtOptimizer.DRT_OPTIMIZER;
		} else {
			throw new RuntimeException("Unknown sav mode. Aborting...");
		}
		
		// #############################
		// consistency check
		// #############################
		
		ModeParams savModeParams = null;
		if (this.getConfig().planCalcScore().getModes().get(savMode) != null) {
			savModeParams = this.getConfig().planCalcScore().getModes().get(savMode);
		} else {
			throw new RuntimeException("There is no 'taxi/sav' mode in the planCalcScore config group.");
		}
		
		if (savModeParams.getMonetaryDistanceRate() != 0.) {
			log.warn("The monetary distance rate for 'taxi/sav' should be zero. The fare is considered somewhere else.");
		}
		
		if (savModeParams.getMarginalUtilityOfDistance() != 0.) {
			log.warn("The marginal utility of distance for 'taxi/sav' should be zero.");
		}
		
		ModeParams savOptimizerModeParams = null;
		if (this.getConfig().planCalcScore().getModes().get(savOptimizerMode) != null) {
			savOptimizerModeParams = this.getConfig().planCalcScore().getModes().get(savOptimizerMode);
		} else {
			log.warn("There is no 'taxi_optimizer/drt_optimizer' mode in the planCalcScore config group. Will only allow the default travel disutility.");
		}
		
		if (savOptimizerModeParams != null) {
			TaxiFareConfigGroup taxiFareParams = ConfigUtils.addOrGetModule(this.getConfig(), TaxiFareConfigGroup.class);
			if (savOptimizerModeParams.getMonetaryDistanceRate() == 0.) {
				log.warn("The monetary distance rate for 'taxi_optimizer/drt_optimizer' is zero. Are you sure, the operating costs are zero?");
			}
			
			if (savOptimizerModeParams.getMonetaryDistanceRate() > 0.) {
				log.warn("The monetary distance rate for 'taxi_optimizer/drt_optimizer' should be negative.");
			}
			
			if (savOptimizerModeParams.getMarginalUtilityOfDistance() != 0.) {
				log.warn("The marginal utility of distance for 'taxi_optimizer/drt_optimizer' should be zero.");
			}
			
			if (savOptimizerModeParams.getMarginalUtilityOfTraveling() != savModeParams.getMarginalUtilityOfTraveling()) {
				log.warn("The marginal utility of traveling for 'taxi/sav' and 'taxi_optimizer/drt_optimizer' should be the same..."
						+ "Assumption: There is either a passenger in the SAV or there is a passenger waiting for the SAV.");	
			}
			
			if (savOptimizerModeParams.getMonetaryDistanceRate() != (taxiFareParams.getDistanceFare_m() * (-1) )) {
				log.warn("Distance-based cost in plansCalcScore config group and taxiFareConfigGroup for 'taxi_optimizer/drt_optimizer' should be (approximately) the same..."
						+ "Assumption: A competitive market where the fare is equivalent to the marginal operating costs."
						+ " It may make sense to charge a slighlty higher fare...");
			}
		}
				
		NoiseConfigGroup noiseParams = ConfigUtils.addOrGetModule(this.getConfig(), NoiseConfigGroup.class);
		DecongestionConfigGroup decongestionParams = ConfigUtils.addOrGetModule(this.getConfig(), DecongestionConfigGroup.class);
		
		// #############################
		// passenger-vehicle tracking
		// #############################
				
		SAVPassengerTrackerImpl tracker = new SAVPassengerTrackerImpl(savMode);
		this.bind(SAVPassengerTracker.class).toInstance(tracker);
		addEventHandlerBinding().toInstance(tracker);
				
		// #############################
        // noise and congestion pricing
        // #############################
		
		if (savPricingParams.isAccountForNoise()) {
						
			if (savPricingParams.isChargeSAVTollsFromPassengers() || savPricingParams.isChargeTollsFromCarUsers() || savPricingParams.isChargeTollsFromSAVDriver()) {
				noiseParams.setInternalizeNoiseDamages(true);
			} else {
				noiseParams.setInternalizeNoiseDamages(false);
			}
			install(new NoiseComputationModuleSAV(this.scenario));
		} else {
			noiseParams.setInternalizeNoiseDamages(false);
		}
				
		if (savPricingParams.isAccountForCongestion()) {
									
			if (savPricingParams.isChargeSAVTollsFromPassengers() || savPricingParams.isChargeTollsFromCarUsers() || savPricingParams.isChargeTollsFromSAVDriver()) {
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
		               
		// sav_optimizer
		
		if (savPricingParams.isChargeTollsFromSAVDriver()) {
			
			log.info("Charge tolls from SAV drivers. Modify the default travel disutility of the sav optimizer.");
			if (savOptimizerModeParams == null) throw new RuntimeException("There is no 'taxi_optimizer/drt_optimizer' mode in the planCalcScore config group. Aborting...");
			install(new SAVMoneyTravelDisutilityModule(savOptimizerMode, new SAVOptimizerMoneyTimeDistanceTravelDisutilityFactory(savOptimizerMode)));
		
		} else {
			
			log.info("Tolls are not charged from SAV drivers. No need to modify the default travel disutility of the sav optimizer.");
			
			if (useDefaultTravelDisutilityInTheCaseWithoutPricing) {
				log.info("Using the default travel disutility for the SAV optimizer.");
			} else {
				if (savOptimizerModeParams == null) throw new RuntimeException("There is no 'taxi_optimizer/drt_optimizer' mode in the planCalcScore config group. Aborting...");
				install(new SAVMoneyTravelDisutilityModule(savOptimizerMode, new SAVOptimizerMoneyTimeDistanceTravelDisutilityFactory(savOptimizerMode)));		  	
			}
		}
		
		// car user
				
		if (savPricingParams.isChargeTollsFromCarUsers()) {
						
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
		
		// account for vehicle type (sav vs. car)
		if ((savPricingParams.isChargeTollsFromCarUsers() == false && savPricingParams.isChargeTollsFromSAVDriver() == true) 
				|| (savPricingParams.isChargeTollsFromCarUsers() == true && savPricingParams.isChargeTollsFromSAVDriver() == false))  {
			
			log.info("Using an agent filter which differentiates between sav vehicles and other vehicles...");
			this.bind(AgentFilter.class).toInstance(new AVAgentFilter());
		} else {
			log.info("Not binding any agent filter. "
					+ "The computation of average toll payments does not differentiate between sav and a normal car.");
		}
				
	}
}

