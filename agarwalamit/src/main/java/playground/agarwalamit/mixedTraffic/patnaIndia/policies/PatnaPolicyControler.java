/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.mixedTraffic.patnaIndia.policies;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ScoringParameterSet;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;
import playground.agarwalamit.analysis.StatsWriter;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareEventHandler;
import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;
import playground.agarwalamit.analysis.tripTime.ModalTravelTimeAnalyzer;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTravelTimeControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTripTravelTimeHandler;
import playground.vsp.cadyts.multiModeCadyts.MultiModeCountsControlerListener;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForBike;
import playground.agarwalamit.mixedTraffic.patnaIndia.scoring.PtFareEventHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter.PatnaUserGroup;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.VehicleUtils;

/**
 * @author amit
 */

public class PatnaPolicyControler {

	private static String dir = FileUtils.RUNS_SVN + "/patnaIndia/run108/jointDemand/policies/0.15pcu/";
	private static String configFile = dir + "/input/configBaseCaseCtd.xml";
	private static boolean addBikeTrack = true;
	private static boolean isAllwoingMotorbikeOnBikeTrack = false;

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig();
		String outputDir ;
		double bike_track_length_reductionFactor_sensitivity = PatnaUtils.BIKE_TRACK_LEGNTH_REDUCTION_FACTOR; // same as before

		if(args.length>0){
			dir = args[0];
			configFile = args[1];
			addBikeTrack = Boolean.valueOf(args[2]);
			isAllwoingMotorbikeOnBikeTrack = Boolean.valueOf(args[3]);
			bike_track_length_reductionFactor_sensitivity = Double.valueOf(args[4]);

		}  else {
			//nothing to do
		}

		if (addBikeTrack  && isAllwoingMotorbikeOnBikeTrack) outputDir = dir+"/BT-mb/";
		else if(addBikeTrack ) outputDir = dir+"/BT-b/";
		else if (! addBikeTrack ) outputDir = dir + "/bau/";
		else throw new RuntimeException("not implemented yet.");

		String inputDir = dir + "/input/";
		String configFile = inputDir + "configBaseCaseCtd.xml";

		ConfigUtils.loadConfig(config, configFile);
		config.controler().setOutputDirectory(outputDir);

		//==
		// after calibration;  departure time is fixed for urban; check if time choice is not present
		Collection<StrategySettings> strategySettings = config.strategy().getStrategySettings();
		for(StrategySettings ss : strategySettings){ // departure time is fixed now.
			if ( ss.getStrategyName().equals(DefaultStrategy.TimeAllocationMutator.toString()) ) {
				throw new RuntimeException("Time mutation should not be used; fixed departure time must be used after cadyts calibration.");
			}
		}
		//==

		//==
		// take only selected plans so that time for urban and location for external traffic is fixed.
		// not anymore, there is now second calibration after cadyts and before baseCaseCtd; thus all plans in the choice set.
		String inPlans = "baseCaseOutput_plans.xml.gz";
		config.plans().setInputFile(inputDir + inPlans);
		config.plans().setInputPersonAttributeFile(inputDir+"output_personAttributes.xml.gz");

		config.vehicles().setVehiclesFile(null); // vehicle types are added from vehicle file later.
		//==

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setWriteEventsInterval(50);

		config.travelTimeCalculator().setFilterModes(true);
		config.travelTimeCalculator().setAnalyzedModes(String.join(",", PatnaUtils.ALL_MAIN_MODES));

		if( addBikeTrack) config.network().setInputFile(inputDir + "/networkWithOptimizedConnectors_halfLength.xml.gz"); // must be after getting optimum number of connectors
		else config.network().setInputFile(inputDir+"/network.xml.gz");

		Scenario scenario = ScenarioUtils.loadScenario(config);

		if ( addBikeTrack ) {
			// remove the connectors on which freespeed is only 0.01 m/s
			scenario.getNetwork().getLinks().values().stream().filter(
					link -> link.getId().toString().startsWith(PatnaUtils.BIKE_TRACK_CONNECTOR_PREFIX) && link.getFreespeed() == 0.01
			).collect(Collectors.toList()).forEach(link -> scenario.getNetwork().removeLink(link.getId()));

			// sensitivity
			double factor = PatnaUtils.BIKE_TRACK_LEGNTH_REDUCTION_FACTOR / bike_track_length_reductionFactor_sensitivity;
			if (factor==1.0) {
				// this is same as before i.e. track lengths are halved.
			} else {
				scenario.getNetwork()
						.getLinks()
						.values()
						.stream()
						.filter(l -> l.getId().toString().startsWith(PatnaUtils.BIKE_TRACK_CONNECTOR_PREFIX) ||
									 l.getId().toString().startsWith(PatnaUtils.BIKE_TRACK_PREFIX))
						.forEach(l -> l.setLength(l.getLength() * factor));
			}
		}

		if ( isAllwoingMotorbikeOnBikeTrack ) {
			Set<String> allowedModesOnBikeTrack = new HashSet<>(Arrays.asList(TransportMode.bike, "motorbike"));
			List<Link> bikeLinks = scenario.getNetwork().getLinks().values().stream().filter(
					link -> link.getId().toString().startsWith(PatnaUtils.BIKE_TRACK_PREFIX) || link.getId().toString().startsWith(PatnaUtils.BIKE_TRACK_CONNECTOR_PREFIX)
			).collect(Collectors.toList());
			bikeLinks.forEach(link -> link.setAllowedModes(allowedModesOnBikeTrack));
			bikeLinks.forEach(link -> link.setFreespeed(60./3.6)); // naturally, bikes must also be faster
		}

		String vehiclesFile = inputDir+"/output_vehicles.xml.gz"; // following is required to extract only vehicle types and not vehicle info. Amit Nov 2016
		VehicleUtils.addVehiclesToScenarioFromVehicleFile(vehiclesFile, scenario);

		if (!scenario.getVehicles().getVehicles().isEmpty()) throw new RuntimeException("Only vehicle types should be loaded if vehicle source "+
				QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData +" is assigned.");
		scenario.getConfig().qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

		final Controler controler = new Controler(scenario);

		controler.getConfig().controler().setDumpDataAtEnd(true);
		controler.getConfig().strategy().setMaxAgentPlanMemorySize(10);

		controler.addOverridingModule(new AbstractModule() { // plotting modal share over iterations
			@Override
			public void install() {
				this.bind(ModalShareEventHandler.class);
				this.addControlerListenerBinding().to(ModalShareControlerListener.class);

				this.bind(ModalTripTravelTimeHandler.class);
				this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);

				this.addControlerListenerBinding().to(MultiModeCountsControlerListener.class);
			}
		});

		// adding pt fare system based on distance 
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().to(PtFareEventHandler.class);
			}
		});
		// for above make sure that util_dist and monetary dist rate for pt are zero.
		ModeParams mp = controler.getConfig().planCalcScore().getModes().get("pt");
		mp.setMarginalUtilityOfDistance(0.0);
		mp.setMonetaryDistanceRate(0.0);

		// add income dependent scoring function factory
		addScoringFunction(controler);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding(TransportMode.bike).to(FreeSpeedTravelTimeForBike.class);
			}
		});

		controler.run();

		// delete unnecessary iterations folder here.
		int firstIt = controler.getConfig().controler().getFirstIteration();
		int lastIt = controler.getConfig().controler().getLastIteration();
		FileUtils.deleteIntermediateIterations(outputDir,firstIt,lastIt);

		new File(outputDir+"/analysis/").mkdir();
		String outputEventsFile = outputDir+"/output_events.xml.gz";
		// write some default analysis
		String userGroup = PatnaUserGroup.urban.toString();
		ModalTravelTimeAnalyzer mtta = new ModalTravelTimeAnalyzer(outputEventsFile, userGroup, new PatnaPersonFilter());
		mtta.run();
		mtta.writeResults(outputDir+"/analysis/modalTravelTime_"+userGroup+".txt");

		ModalShareFromEvents msc = new ModalShareFromEvents(outputEventsFile, userGroup, new PatnaPersonFilter());
		msc.run();
		msc.writeResults(outputDir+"/analysis/modalShareFromEvents_"+userGroup+".txt");

		StatsWriter.run(outputDir);
	}

	public static void addScoringFunction(final Controler controler){
		// scoring function
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			final ScoringParametersForPerson parameters = new SubpopulationScoringParameters( controler.getScenario() );
			@Inject
             Network network;
			@Inject
             Population population;
			@Inject
             PlanCalcScoreConfigGroup planCalcScoreConfigGroup; // to modify the util parameters
			@Inject
             ScenarioConfigGroup scenarioConfig;
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				final ScoringParameters params = parameters.getScoringParameters( person );

				SumScoringFunction sumScoringFunction = new SumScoringFunction();
				sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				Double ratioOfInc = 1.0;

				if ( PatnaPersonFilter.isPersonBelongsToUrban(person.getId())) { // inc is not available for commuters and through traffic
					Double monthlyInc = (Double) population.getPersonAttributes().getAttribute(person.getId().toString(), PatnaUtils.INCOME_ATTRIBUTE);
					Double avgInc = PatnaUtils.MEADIAM_INCOME;
					ratioOfInc = avgInc/monthlyInc;
				}

				planCalcScoreConfigGroup.setMarginalUtilityOfMoney(ratioOfInc );				

				ScoringParameterSet scoringParameterSet = planCalcScoreConfigGroup.getScoringParameters( null ); // parameters set is same for all subPopulations 

				ScoringParameters.Builder builder = new ScoringParameters.Builder(
						planCalcScoreConfigGroup, scoringParameterSet, scenarioConfig);
				final ScoringParameters modifiedParams = builder.build();

				sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(modifiedParams, network));
				sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(modifiedParams));
				return sumScoringFunction;
			}
		});
	}
}
