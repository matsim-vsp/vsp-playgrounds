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

package playground.agarwalamit.opdyts.equil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.google.common.io.Files;
import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.contrib.opdyts.MATSimSimulator2;
import org.matsim.contrib.opdyts.MATSimStateFactoryImpl;
import org.matsim.contrib.opdyts.useCases.modeChoice.EveryIterationScoringParameters;
import org.matsim.contrib.opdyts.utils.MATSimOpdytsControler;
import org.matsim.contrib.opdyts.utils.OpdytsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.analysis.modalShare.ModalShareControlerListener;
import playground.agarwalamit.analysis.modalShare.ModalShareEventHandler;
import playground.agarwalamit.opdyts.*;
import playground.agarwalamit.opdyts.analysis.OpdytsModalStatsControlerListener;
import playground.agarwalamit.opdyts.plots.BestSolutionVsDecisionVariableChart;
import playground.agarwalamit.opdyts.plots.OpdytsConvergenceChart;
import playground.agarwalamit.utils.FileUtils;

/**
 * @author amit
 */

public class MatsimOpdytsEquilMixedTrafficIntegration {

	private static String EQUIL_DIR = "../matsim-git/examples/scenarios/equil-mixedTraffic/";
	private static final OpdytsScenario EQUIL_MIXEDTRAFFIC = OpdytsScenario.EQUIL_MIXEDTRAFFIC;

	public static void main(String[] args) {
		String configFile;
		String OUT_DIR ;
		String relaxedPlans;
		ModeChoiceRandomizer.ASCRandomizerStyle ascRandomizeStyle;

		if (args.length > 0) {
			EQUIL_DIR = args[0];
			OUT_DIR = args[1];
			relaxedPlans = args[2];
			ascRandomizeStyle = ModeChoiceRandomizer.ASCRandomizerStyle.valueOf(args[3]);
		} else {
			OUT_DIR = FileUtils.RUNS_SVN+"/opdyts/equil/car,bicycle/testCalib/";
			relaxedPlans = FileUtils.RUNS_SVN+"/opdyts/equil/car,bicycle/relaxedPlans/output_plans.xml.gz";
			ascRandomizeStyle = ModeChoiceRandomizer.ASCRandomizerStyle.axial_randomVariation;
		}

		configFile = EQUIL_DIR+"/config-with-mode-vehicles.xml";
		if (! new File(configFile).exists()) configFile = EQUIL_DIR+"/config.xml";

		OUT_DIR += ascRandomizeStyle+"/";

		Config config = ConfigUtils.loadConfig(configFile, new OpdytsConfigGroup());
		config.plans().setInputFile(relaxedPlans);
		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn); // must be warn, since opdyts override few things
		config.controler().setOutputDirectory(OUT_DIR);

		OpdytsConfigGroup opdytsConfigGroup = ConfigUtils.addOrGetModule(config, OpdytsConfigGroup.GROUP_NAME, OpdytsConfigGroup.class ) ;
		opdytsConfigGroup.setOutputDirectory(OUT_DIR);

		List<String> modes2consider = Arrays.asList("car","bicycle");

		//== default config has limited inputs
		StrategyConfigGroup strategies = config.strategy();
		strategies.clearStrategySettings();

		config.changeMode().setModes( modes2consider.toArray(new String [modes2consider.size()]));
		StrategySettings modeChoice = new StrategySettings();
		modeChoice.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode.name());
		modeChoice.setWeight(0.1);
		config.strategy().addStrategySettings(modeChoice);

		StrategySettings expChangeBeta = new StrategySettings();
		expChangeBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
		expChangeBeta.setWeight(0.9);
		config.strategy().addStrategySettings(expChangeBeta);
		//==

		//== planCalcScore params (initialize will all defaults).
		for ( PlanCalcScoreConfigGroup.ActivityParams params : config.planCalcScore().getActivityParams() ) {
			params.setTypicalDurationScoreComputation( PlanCalcScoreConfigGroup.TypicalDurationScoreComputation.relative );
		}

		// remove other mode params
		PlanCalcScoreConfigGroup planCalcScoreConfigGroup = config.planCalcScore();
		for ( PlanCalcScoreConfigGroup.ModeParams params : planCalcScoreConfigGroup.getModes().values() ) {
			planCalcScoreConfigGroup.removeParameterSet(params);
		}

		PlanCalcScoreConfigGroup.ModeParams mpCar = new PlanCalcScoreConfigGroup.ModeParams("car");
		PlanCalcScoreConfigGroup.ModeParams mpBike = new PlanCalcScoreConfigGroup.ModeParams("bicycle");
		mpBike.setMarginalUtilityOfTraveling(0.);
		mpBike.setConstant(0.);

		planCalcScoreConfigGroup.addModeParams(mpCar);
		planCalcScoreConfigGroup.addModeParams(mpBike);
		//==

		//==
		config.qsim().setTrafficDynamics( QSimConfigGroup.TrafficDynamics.withHoles );
		config.qsim().setUsingFastCapacityUpdate(true);

		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		//==

//		if(! isPlansRelaxed) {
//			config.controler().setLastIteration(50);
//			config.strategy().setFractionOfIterationsToDisableInnovation(1.0);
//
//			Scenario scenarioPlansRelaxor = ScenarioUtils.loadScenario(config);
//			// following is taken from KNBerlinControler.prepareScenario(...);
//			// modify equil plans:
//			double time = 6*3600. ;
//			for ( Person person : scenarioPlansRelaxor.getPopulation().getPersons().values() ) {
//				Plan plan = person.getSelectedPlan() ;
//				Activity activity = (Activity) plan.getPlanElements().get(0) ;
//				activity.setEndTime(time);
//				time++ ;
//			}
//
//			Controler controler = new Controler(scenarioPlansRelaxor);
//			controler.addOverridingModule(new AbstractModule() {
//				@Override
//				public void install() {
//					addControlerListenerBinding().toInstance(new OpdytsModalStatsControlerListener(modes2consider, new EquilDistanceDistribution(EQUIL_MIXEDTRAFFIC)));
//				}
//			});
//			controler.run();
//
//			FileUtils.deleteIntermediateIterations(config.controler().getOutputDirectory(),controler.getConfig().controler().getFirstIteration(), controler.getConfig().controler().getLastIteration());
//		}

//		// set back settings for opdyts
//		File file = new File(config.controler().getOutputDirectory()+"/output_plans.xml.gz");
//		config.plans().setInputFile(file.getAbsoluteFile().getAbsolutePath());
//		OUT_DIR = OUT_DIR+"/calibration_"+ opdytsConfigGroup.getNumberOfIterationsForAveraging() +"Its_"+opdytsConfigGroup.getSelfTuningWeight()+"weight_"+startingASCforBicycle+"asc/";
//
//		config.controler().setOutputDirectory(OUT_DIR);
//		opdytsConfigGroup.setOutputDirectory(OUT_DIR);
		config.strategy().setFractionOfIterationsToDisableInnovation(Double.POSITIVE_INFINITY);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		//****************************** mainly opdyts settings ******************************

		DistanceDistribution distanceDistribution = new EquilDistanceDistribution(EQUIL_MIXEDTRAFFIC);
		OpdytsModalStatsControlerListener stasControlerListner = new OpdytsModalStatsControlerListener(modes2consider,distanceDistribution);

		// following is the  entry point to start a matsim controler together with opdyts
		MATSimSimulator2<ModeChoiceDecisionVariable> simulator = new MATSimSimulator2<>(new MATSimStateFactoryImpl<>(), scenario);

		String finalOUT_DIR = OUT_DIR;
		simulator.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				// some stats
				addControlerListenerBinding().to(KaiAnalysisListener.class);
				addControlerListenerBinding().toInstance(stasControlerListner);

				this.bind(ModalShareEventHandler.class);
				this.addControlerListenerBinding().to(ModalShareControlerListener.class);

				bind(ScoringParametersForPerson.class).to(EveryIterationScoringParameters.class);

				addControlerListenerBinding().toInstance(new ShutdownListener() {
					@Override
					public void notifyShutdown(ShutdownEvent event) {
						// copy the state vector elements files before removing ITERS dir
						String outDir = event.getServices().getControlerIO().getOutputPath()+"/vectorElementSizeFiles/";
						new File(outDir).mkdirs();

						int firstIt = event.getServices().getConfig().controler().getFirstIteration();
						int lastIt = event.getServices().getConfig().controler().getLastIteration();
						int plotEveryItr = 50;

						for (int itr = firstIt+1; itr <=lastIt; itr++) {
							if ( (itr == firstIt+1 || itr%plotEveryItr ==0) && new File(event.getServices().getControlerIO().getIterationPath(itr)).exists() ) {
								{
									String sourceFile = event.getServices().getControlerIO().getIterationFilename(itr,"stateVector_networkModes.txt");
									String sinkFile =  outDir+"/"+itr+".stateVector_networkModes.txt";
									try {
										Files.copy(new File(sourceFile), new File(sinkFile));
									} catch (IOException e) {
										throw new RuntimeException("Data is not copied. Reason : " + e);
									}
								}
								{
									String sourceFile = event.getServices().getControlerIO().getIterationFilename(itr,"stateVector_teleportationModes.txt");
									String sinkFile =  outDir+"/"+itr+".stateVector_teleportationModes.txt";
									try {
										Files.copy(new File(sourceFile), new File(sinkFile));
									} catch (IOException e) {
										throw new RuntimeException("Data is not copied. Reason : " + e);
									}
								}
							}
						}

						// remove the unused iterations
						String dir2remove = event.getServices().getControlerIO().getOutputPath()+"/ITERS/";
						IOUtils.deleteDirectoryRecursively(new File(dir2remove).toPath());

						// post-process
						String opdytsConvergencefile = finalOUT_DIR +"/opdyts.con";
						if (new File(opdytsConvergencefile).exists()) {
							OpdytsConvergenceChart opdytsConvergencePlotter = new OpdytsConvergenceChart();
							opdytsConvergencePlotter.readFile(finalOUT_DIR +"/opdyts.con");
							opdytsConvergencePlotter.plotData(finalOUT_DIR +"/convergence.png");
						}

						BestSolutionVsDecisionVariableChart bestSolutionVsDecisionVariableChart = new BestSolutionVsDecisionVariableChart(new ArrayList<>(modes2consider));
						bestSolutionVsDecisionVariableChart.readFile(finalOUT_DIR +"/opdyts.log");
						bestSolutionVsDecisionVariableChart.plotData(finalOUT_DIR +"/decisionVariableVsASC.png");
					}
				});
			}
		});

		// this is the objective Function which returns the value for given SimulatorState
		// in my case, this will be the distance based modal split
		ObjectiveFunction objectiveFunction = new ModeChoiceObjectiveFunction(distanceDistribution);

		// randomize the decision variables (for e.g.\ utility parameters for modes)
		DecisionVariableRandomizer<ModeChoiceDecisionVariable> decisionVariableRandomizer = new ModeChoiceRandomizer(scenario,
				RandomizedUtilityParametersChoser.ONLY_ASC, EQUIL_MIXEDTRAFFIC, null, modes2consider);

		// what would be the decision variables to optimize the objective function.
		ModeChoiceDecisionVariable initialDecisionVariable = new ModeChoiceDecisionVariable(scenario.getConfig().planCalcScore(),scenario, modes2consider, EQUIL_MIXEDTRAFFIC);

		MATSimOpdytsControler<ModeChoiceDecisionVariable> opdytsControler = new MATSimOpdytsControler<>(scenario);
		opdytsControler.addNetworkModeOccupancyAnalyzr(simulator);
		opdytsControler.run(simulator, decisionVariableRandomizer,  initialDecisionVariable, objectiveFunction);
	}
}
