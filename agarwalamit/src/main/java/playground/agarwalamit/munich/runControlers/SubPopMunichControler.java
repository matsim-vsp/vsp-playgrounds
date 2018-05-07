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
package playground.agarwalamit.munich.runControlers;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.InternalizationEmissionAndCongestion.EmissionCongestionTravelDisutilityCalculatorFactory;
import playground.agarwalamit.InternalizationEmissionAndCongestion.InternalizeEmissionsCongestionControlerListener;
import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;
import playground.agarwalamit.mixedTraffic.patnaIndia.input.joint.JointCalibrationControler;
import playground.agarwalamit.munich.utils.MunichPersonFilter;
import playground.agarwalamit.munich.utils.MunichPersonFilter.MunichUserGroup;
import playground.agarwalamit.utils.FileUtils;
import playground.benjamin.internalization.EmissionTravelDisutilityCalculatorFactory;
import playground.vsp.airPollution.flatEmissions.EmissionCostModule;
import playground.vsp.airPollution.flatEmissions.InternalizeEmissionsControlerListener;
import playground.vsp.congestion.controler.MarginalCongestionPricingContolerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.TollDisutilityCalculatorFactory;

/**
 * @author amit
 */

public class SubPopMunichControler {

	public static void main(String[] args) {

		boolean isRunningOnCluster = false;
		if ( args.length > 0) isRunningOnCluster = true;

		if(! isRunningOnCluster){
			args = new String [] {
					"false",
					"false",
					"false",
					FileUtils.RUNS_SVN+"/detEval/emissionCongestionInternalization/ijst/input/config_wrappedSubActivities_subPop_baseCaseCtd.xml",
					FileUtils.RUNS_SVN+"/detEval/emissionCongestionInternalization/ijst/outputTest/bau/",
			};
		}

		boolean internalizeEmission = Boolean.valueOf(args [0]); 
		boolean internalizeCongestion = Boolean.valueOf(args [1]);
		boolean internalizeBoth = Boolean.valueOf(args [2]);
		String configFile = args[3];
		String outputDir = args[4];

		Config config = ConfigUtils.loadConfig(configFile, new EmissionsConfigGroup());
		config.controler().setOutputDirectory(outputDir);
		config.planCalcScore().getActivityParams().stream().forEach(act->act.setTypicalDurationScoreComputation(
				PlanCalcScoreConfigGroup.TypicalDurationScoreComputation.uniform));

		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.controler().setCreateGraphs(true);
		config.controler().setDumpDataAtEnd(true);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		final Controler controler = new Controler(scenario);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				String ug = "COMMUTER_REV_COMMUTER";
				addPlanStrategyBinding(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute.name().concat("_").concat(ug)).toProvider(new javax.inject.Provider<PlanStrategy>() {
					@Inject
					Scenario sc;
					@Inject
					Provider<TripRouter> tripRouterProvider;
					@Override
					public PlanStrategy get() {
						final Builder builder = new Builder(new RandomPlanSelector<>());
						builder.addStrategyModule(new ReRoute(sc, tripRouterProvider));
						return builder.build();
					}
				});
			}
		});

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				String ug = "COMMUTER_REV_COMMUTER";
				addPlanStrategyBinding(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice.name().concat("_").concat(ug)).toProvider(new javax.inject.Provider<PlanStrategy>() {
					final String[] availableModes = {"car", "pt_".concat(ug)};
					final String[] chainBasedModes = {"car", "bike"};
					@Inject
					Scenario sc;
					@Inject
					Provider<TripRouter> tripRouterProvider;

					@Override
					public PlanStrategy get() {
						final Builder builder = new Builder(new RandomPlanSelector<>());
						builder.addStrategyModule(new SubtourModeChoice(sc.getConfig().global().getNumberOfThreads(),
								availableModes,
								chainBasedModes,
								false,
								0.0, //prob, 0.0 for backward compatiblity
								tripRouterProvider));
						builder.addStrategyModule(new ReRoute(sc, tripRouterProvider));
						return builder.build();
					}
				});
			}
		});

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(EmissionModule.class).asEagerSingleton(); // need at many places even if not internalizing emissions
				bind(EmissionCostModule.class).asEagerSingleton();
			}
		});

		if(internalizeEmission){
			// this affects the router by overwriting its generalized cost function (TravelDisutility):
			final EmissionTravelDisutilityCalculatorFactory emissionTducf = new EmissionTravelDisutilityCalculatorFactory(
            );
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addControlerListenerBinding().to(InternalizeEmissionsControlerListener.class);
					bindCarTravelDisutilityFactory().toInstance(emissionTducf);
				}
			});

		} else if(internalizeCongestion){

			TollHandler tollHandler = new TollHandler(controler.getScenario());
			final TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler, config.planCalcScore());
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindCarTravelDisutilityFactory().toInstance(tollDisutilityCalculatorFactory);
				}
			});
			controler.addControlerListener(new MarginalCongestionPricingContolerListener(controler.getScenario(),tollHandler, new CongestionHandlerImplV3(controler.getEvents(),
                    controler.getScenario()) ));

		} else if(internalizeBoth) {

			TollHandler tollHandler = new TollHandler(controler.getScenario());
			final EmissionCongestionTravelDisutilityCalculatorFactory emissionCongestionTravelDisutilityCalculatorFactory =
					new EmissionCongestionTravelDisutilityCalculatorFactory(new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car,controler.getConfig().planCalcScore()),
							tollHandler);
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindCarTravelDisutilityFactory().toInstance(emissionCongestionTravelDisutilityCalculatorFactory);
				}
			});
			controler.addControlerListener(new InternalizeEmissionsCongestionControlerListener(tollHandler));
		}

		// ride is one of network mode, thus travel disutility is required and every link must allow rider too
		Set<String> modes = new HashSet<>();
		modes.add("car");modes.add("ride");

		for(Link l : controler.getScenario().getNetwork().getLinks().values()) {
			l.setAllowedModes(modes);
		}

		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				addTravelTimeBinding("ride").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("ride").to(carTravelDisutilityFactoryKey());
			}
		});

		controler.run();

		// delete unnecessary iterations folder here.
		int firstIt = controler.getConfig().controler().getFirstIteration();
		int lastIt = controler.getConfig().controler().getLastIteration();
		String OUTPUT_DIR = config.controler().getOutputDirectory();
		for (int index =firstIt+1; index <lastIt; index ++){
			String dirToDel = OUTPUT_DIR+"/ITERS/it."+index;
			Logger.getLogger(JointCalibrationControler.class).info("Deleting the directory "+dirToDel);
			IOUtils.deleteDirectoryRecursively(new File(dirToDel).toPath());
		}

		new File(OUTPUT_DIR+"/analysis/").mkdir();
		String outputEventsFile = OUTPUT_DIR+"/output_events.xml.gz";
		// write some default analysis
		
		{
			String userGroup = MunichUserGroup.Urban.toString();
			ModalShareFromEvents msc = new ModalShareFromEvents(outputEventsFile, userGroup, new MunichPersonFilter());
			msc.run();
			msc.writeResults(OUTPUT_DIR+"/analysis/modalShareFromEvents_"+userGroup+".txt");	
		}
		{
			String userGroup = MunichUserGroup.Rev_Commuter.toString();
			ModalShareFromEvents msc = new ModalShareFromEvents(outputEventsFile, userGroup, new MunichPersonFilter());
			msc.run();
			msc.writeResults(OUTPUT_DIR+"/analysis/modalShareFromEvents_"+userGroup+".txt");
		}
	}
}