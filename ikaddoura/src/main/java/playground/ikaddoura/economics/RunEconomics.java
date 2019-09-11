/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.economics;

import java.io.IOException;
import java.util.Map;

import org.junit.Assert;
import org.matsim.analysis.detailedPersonTripAnalysis.handler.BasicPersonTripAnalysisHandler;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;


/**
 * @author ikaddoura
 *
 */
public class RunEconomics {

	public static void main(String[] args) throws IOException {
		
		RunEconomics economics = new RunEconomics();
		economics.run1();
	}
	
	/**
	 *
	 * Case studies:
	 * 0: equi network
	 * 1: equi network, slightly improved
	 * 
	 * 
	 * 	- The VTTS is set equal for all users (beta_perf = 0; beta_traveling = -6)
	 *  - inelastic demand (same number of users in base case and policy case)
	 *
	 */
	private void run1() {
		
		String basicDirectoryInput = "./test/input/playground/ikaddoura/economics/";
		String basicDirectoryOutput = "./test/output/playground/ikaddoura/economics/";

		final String[] helpLegModes = {TransportMode.transit_walk, TransportMode.access_walk, TransportMode.egress_walk};
		final String stageActivitySubString = "interaction";
		
		BasicPersonTripAnalysisHandler analysis0 = new BasicPersonTripAnalysisHandler(helpLegModes, stageActivitySubString);	
		Scenario scenario0 = null;
		
		{
			String configFile0 = basicDirectoryInput + "config0.xml";
		
			Config config0 = ConfigUtils.loadConfig(configFile0);
			config0.controler().setOutputDirectory(basicDirectoryOutput);
			config0.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			
			scenario0 = ScenarioUtils.loadScenario(config0);
			
			Controler controler0 = new Controler(scenario0);
						
			controler0.addOverridingModule(new AbstractModule() {
				
				@Override
				public void install() {
					this.addEventHandlerBinding().toInstance(analysis0);
				}
			});
			
			controler0.run();
		
		}
		
		BasicPersonTripAnalysisHandler analysis1 = new BasicPersonTripAnalysisHandler(helpLegModes, stageActivitySubString);
		Scenario scenario1 = null;
		
		{
			String configFile1 = basicDirectoryInput + "config1.xml";
			
			Config config1 = ConfigUtils.loadConfig(configFile1);
			config1.controler().setOutputDirectory(basicDirectoryOutput);
			config1.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			
			scenario1 = ScenarioUtils.loadScenario(config1);
			
			Controler controler1 = new Controler(scenario1);
						
			controler1.addOverridingModule(new AbstractModule() {
				
				@Override
				public void install() {
					this.addEventHandlerBinding().toInstance(analysis1);
				}
			});
			
			controler1.run();
		}
		
		// analyze results
		
		double totalTravelTime0 = getTotalTravelTime(analysis0.getPersonId2tripNumber2travelTime());
		double totalTravelTime1 = getTotalTravelTime(analysis1.getPersonId2tripNumber2travelTime());
				
		int carDemand0 = getModeDemand(analysis0.getPersonId2tripNumber2legMode(), TransportMode.car);
		int carDemand1 = getModeDemand(analysis1.getPersonId2tripNumber2legMode(), TransportMode.car);
		
		double avgTravelTime0 = totalTravelTime0 / carDemand0;
		double avgTravelTime1 = totalTravelTime1 / carDemand1;
		
		// KR / PR evaluation approach
		
		System.out.println();
		System.out.println("######################################################");
		
		System.out.println("total travel time (run 0) [sec]: " + totalTravelTime0);
		System.out.println("car trips (run 0): " + carDemand0);
		System.out.println("average travel time (run 0) [sec]: " + avgTravelTime0);

		System.out.println("total travel time (run 1) [sec]: " + totalTravelTime1);
		System.out.println("car trips (run 1): " + carDemand1);
		System.out.println("average travel time (run 1) [sec]: " + avgTravelTime1);

		double vtts = (scenario0.getConfig().planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() - scenario0.getConfig().planCalcScore().getPerforming_utils_hr() ) / scenario0.getConfig().planCalcScore().getMarginalUtilityOfMoney();
		
		System.out.println("VTTS [EUR/h]: " + vtts);
		System.out.println("######################################################");


		Assert.assertEquals("VTTS should be the same for both cases.", vtts - ((scenario1.getConfig().planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() - scenario1.getConfig().planCalcScore().getPerforming_utils_hr() ) / scenario1.getConfig().planCalcScore().getMarginalUtilityOfMoney()), 0., 0.0001);
		
		double csOldUsers = carDemand0 * (avgTravelTime1 - avgTravelTime0) / 3600. * vtts ;
		double csNewUsers = (carDemand1 - carDemand0) * ((avgTravelTime1 - avgTravelTime0) / 3600.) * vtts / 2;	
		double totalChangeInUserBenefitsKR = csOldUsers + csNewUsers;
		
		System.out.println();
		System.out.println("######################################################");
		System.out.println("KR / PR evaluation approach");
		System.out.println("change in consumer surplus for old users [EUR]: " + csOldUsers );
		System.out.println("change in consumer surplus for new users [EUR]: " + csNewUsers );
		System.out.println("total change in consumer surplus [EUR]: " + totalChangeInUserBenefitsKR );
		System.out.println("######################################################");
		
		// person-based evaluation approach
		
		double userBenefits0 = getUserBenefits(scenario0.getPopulation());
		double userBenefits1 = getUserBenefits(scenario1.getPopulation());
		double totalChangeInUserBenefitsPersonBased = ((userBenefits1 - userBenefits0)) / scenario0.getConfig().planCalcScore().getMarginalUtilityOfMoney();

		System.out.println();
		System.out.println("######################################################");
		System.out.println("person-based evaluation approach");
		System.out.println("change in user benefits [EUR]: " + totalChangeInUserBenefitsPersonBased);
		System.out.println("######################################################");
		
		Assert.assertEquals("KR/PR approach and person-based approach should result in the same change in user benefits.", totalChangeInUserBenefitsKR - totalChangeInUserBenefitsPersonBased, 0., 0.0001);
		Assert.assertEquals("Change in user benefits should be larger than zero (improvement).", totalChangeInUserBenefitsKR > 0., true);
		Assert.assertEquals("Wrong change in user benefits.", 1.5666666666664, totalChangeInUserBenefitsKR, 0.0001);
		
	}

	private static double getUserBenefits(Population population) {
		double sum = 0.;
		for (Person person : population.getPersons().values()) {
			double userBenefit = person.getSelectedPlan().getScore();
			sum += userBenefit;
		}
		return sum;
	}

	private static int getModeDemand(Map<Id<Person>, Map<Integer, String>> personId2tripNumber2legMode, String mode) {
		int counter = 0;
		for (Id<Person> personId : personId2tripNumber2legMode.keySet()) {
			for (String tripMode : personId2tripNumber2legMode.get(personId).values()) {
				if (tripMode.equals(mode)) {
					counter++;
				}
			}
		}
		return counter;
	}

	private static double getTotalTravelTime(Map<Id<Person>, Map<Integer, Double>> personId2tripNumber2travelTime) {
		double sum = 0.;
		for (Id<Person> personId : personId2tripNumber2travelTime.keySet()) {
			for (Double travelTime : personId2tripNumber2travelTime.get(personId).values()) {
				sum+=travelTime;
			}
		}
		return sum;
	}
		
}
