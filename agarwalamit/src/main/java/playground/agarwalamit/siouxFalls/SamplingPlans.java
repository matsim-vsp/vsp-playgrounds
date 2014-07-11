/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.siouxFalls;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * @author amit
 */
public class SamplingPlans {
	private final Logger log = Logger.getLogger(SamplingPlans.class);
	final CoordinateReferenceSystem targetCRS =	 MGC.getCRS("EPSG:3459");
	private final double xMin =	673506.73;
	private final double xMax = 689857.13;
	private	final double yMin = 4814378.34;
	private final double yMax = 4857392.75;
	private final int noOfXbins = 30;
	private final int noOfYbins = 30;
	private final double samplingRatio = 0.1;
	private SortedMap<String, Population>  binToPopulation;
	private final String clusterPath = "/Users/aagarwal/Desktop/ils4/agarwal/siouxFalls/";
	private final String inputPlans  = clusterPath+"/outputMC/selectedPlansOnly_plans.xml";
	//	private final String networkFile = clusterPath+"/input/SiouxFalls_networkWithRoadType.xml.gz";
	private final String outputPlans = "./input/plans10Pct.xml";
	private double totalNoOfPersons ;
	private Population initialPopulation;
	private Population samplePopulation;

	public static void main(String[] args) {
		SamplingPlans sp = new SamplingPlans();
		sp.readInputPlansAndCreateSubPopulation();
		sp.getAndWriteRandomPlansFromSubPopulation();
		sp.compareModeShare();
	}

	private void getAndWriteRandomPlansFromSubPopulation(){
		log.info("Extracting "+this.samplingRatio*100+"% persons from each sub population randomly...");
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.samplePopulation = sc.getPopulation();
		for(String str:this.binToPopulation.keySet()){
			double noOfPersons = this.binToPopulation.get(str).getPersons().size();
			double requiredPersons = Math.ceil(this.samplingRatio*noOfPersons);

			for(int i=0;i<requiredPersons;i++){

				int index =(int) (MatsimRandom.getRandom().nextDouble()*requiredPersons);
				Iterator<Id> personIds = this.binToPopulation.get(str).getPersons().keySet().iterator();
				for(int j=0 ;j<this.binToPopulation.get(str).getPersons().size();j++){
					Id id = personIds.next();
					if(index==j){
						Person p = this.binToPopulation.get(str).getPersons().get(id);
						if(this.samplePopulation.getPersons().get(p.getId())!=null) 
							requiredPersons++; // to exclude the adding same person again without decreasing required number of persons.
						else this.samplePopulation.addPerson(p);
						break;
					}
				}
			}
		}
		double finalSamplingRatio = this.samplePopulation.getPersons().size()/this.totalNoOfPersons;
		log.info("Total number of persons in sample population are "+this.samplePopulation.getPersons().size()+". Thus final sample population ratio is "+finalSamplingRatio);
		new PopulationWriter(this.samplePopulation,sc.getNetwork()).write(this.outputPlans);
	}

	private void readInputPlansAndCreateSubPopulation(){
		log.info("Reading input plans and creating sub populations...");
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(this.inputPlans);
		Scenario sc= ScenarioUtils.loadScenario(config);
		this.initialPopulation = sc.getPopulation();
		totalNoOfPersons = this.initialPopulation.getPersons().size();
		initiateBinsPopulationMap();

		for(Person p : this.initialPopulation.getPersons().values()){
			Plan selectedPlan = p.getSelectedPlan();
			Coord hCoord = ((Activity) selectedPlan.getPlanElements().get(0)).getCoord();
			String binId = getBinIdentificationFromCoord(hCoord);
			Population pop = this.binToPopulation.get(binId);

			PopulationFactory factory = pop.getFactory();
			//create a new person and new plan; new plan is same as selected plan.
			Person newP = factory.createPerson(p.getId());
			pop.addPerson(newP);
			newP.addPlan(selectedPlan);
			this.binToPopulation.put(binId, pop);
		}
		checkForNumberOfPersonInSubPopulation();
	}

	private void initiateBinsPopulationMap(){
		log.info("Initializing binId2Population Map...");
		binToPopulation = new TreeMap<String, Population>();
		for(int x=0;x<this.noOfXbins;x++){
			for(int y =0;y<this.noOfYbins;y++){
				Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
				Population pop = sc.getPopulation();
				String binString = "x".concat(String.valueOf(x)).concat("y").concat(String.valueOf(y));
				if(binToPopulation.containsKey(binString)) throw new RuntimeException("Bin identification "+binString+"already exists. Please check.");
				else binToPopulation.put(binString, pop);
			}
		}
	}

	private String getBinIdentificationFromCoord(Coord coord){
		int relativePositionY = (int) ((coord.getY() - yMin) / (yMax - yMin) * noOfYbins);
		int relativePositionX = (int) ((coord.getX() -xMin)/(xMax - xMin)*noOfXbins);
		return "x".concat(String.valueOf(relativePositionX)).concat("y").concat(String.valueOf(relativePositionY));
	}

	private void checkForNumberOfPersonInSubPopulation(){
		log.info("Checking if all plans are included in sub populations...");
		double sumOfPersonsInSubPop =0;
		for(String str:this.binToPopulation.keySet()){
			//			System.out.println(this.binToPopulation.get(str).getPersons());
			sumOfPersonsInSubPop+=this.binToPopulation.get(str).getPersons().size();
		}
		if(sumOfPersonsInSubPop<this.totalNoOfPersons) throw new RuntimeException("Total Number of pesons in initial plans are "+this.totalNoOfPersons
				+" and sum of persons in sub populations are "+sumOfPersonsInSubPop+". Thus, division is inconsistent.");

		log.info("Checking finished. Total number of sub population groups are "+this.binToPopulation.size()+" and total number of persons are "+sumOfPersonsInSubPop);
	}

	private void compareModeShare(){
		SortedMap<String, Double> initialModalShare = calculateModeShare(calculateMode2LegCount(this.initialPopulation));
		SortedMap< String, Double> sampleModalShare = calculateModeShare(calculateMode2LegCount(this.samplePopulation));
		log.info("The modal shares for initial population and sample population respectively are ...");
		log.info("Travel Mode \t  initial \t sample ");
		for(String mode :initialModalShare.keySet()){
			log.warn(mode+" \t "+initialModalShare.get(mode)+" \t "+sampleModalShare.get(mode));
		}
	}

	private SortedMap<String, Double> calculateModeShare(SortedMap<String, Integer> mode2NoOfLegs) {
		SortedMap<String, Double> mode2Pct = new TreeMap<String, Double>();
		int totalNoOfLegs = 0;
		for(String mode : mode2NoOfLegs.keySet()){
			int modeLegs = mode2NoOfLegs.get(mode);
			totalNoOfLegs += modeLegs;
		}
		for(String mode : mode2NoOfLegs.keySet()){
			double share = 100. * (double) mode2NoOfLegs.get(mode) / totalNoOfLegs;
			mode2Pct.put(mode, share);
		}
		return mode2Pct;
	}

	private SortedMap<String, Integer> calculateMode2LegCount(Population population) {
		SortedMap<String, Integer> mode2NoOfLegs = new TreeMap<String, Integer>();

		for(Person person : population.getPersons().values()){
			Plan plan = person.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()){
				if(pe instanceof Leg){
					String mode = ((Leg) pe).getMode();

					if(mode2NoOfLegs.get(mode) == null){
						mode2NoOfLegs.put(mode, 1);
					} else {
						int legsSoFar = mode2NoOfLegs.get(mode);
						int legsAfter = legsSoFar + 1;
						mode2NoOfLegs.put(mode, legsAfter);
					}
				}
			}
		}
		return mode2NoOfLegs;
	}
}
