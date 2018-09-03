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

package playground.agarwalamit.opdyts.patna.networkModesOnly;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.opdyts.MATSimSimulator2;
import org.matsim.contrib.opdyts.MATSimStateFactoryImpl;
import org.matsim.contrib.opdyts.useCases.modeChoice.EveryIterationScoringParameters;
import org.matsim.contrib.opdyts.utils.MATSimOpdytsControler;
import org.matsim.contrib.opdyts.utils.OpdytsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareEventHandler;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTravelTimeControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTripTravelTimeHandler;
import playground.agarwalamit.opdyts.DistanceDistribution;
import playground.agarwalamit.opdyts.ModeChoiceDecisionVariable;
import playground.agarwalamit.opdyts.ModeChoiceObjectiveFunction;
import playground.agarwalamit.opdyts.ModeChoiceRandomizer;
import playground.agarwalamit.opdyts.ObjectiveFunctionEvaluator;
import playground.agarwalamit.opdyts.ObjectiveFunctionEvaluator.ObjectiveFunctionType;
import playground.agarwalamit.opdyts.OpdytsModeChoiceUtils;
import playground.agarwalamit.opdyts.OpdytsScenario;
import playground.agarwalamit.opdyts.RandomizedUtilityParametersChoser;
import playground.agarwalamit.opdyts.analysis.OpdytsModalStatsControlerListener;
import playground.agarwalamit.utils.FileUtils;

/**
 * @author amit
 */

public class PatnaNetworkModesOpdytsCalibrator {

	private static final OpdytsScenario PATNA_1_PCT = OpdytsScenario.PATNA_1Pct;

	public static void main(String[] args) {
		String configFile = FileUtils.RUNS_SVN+"/opdyts/patna/networkModes/changeExpBeta/calibration/inputs/config_networkModesOnly.xml";
		String OUT_DIR = FileUtils.RUNS_SVN+"/opdyts/patna/networkModes/changeExpBeta/calibration/output/testCalib/";
		String relaxedPlans = FileUtils.RUNS_SVN+"/opdyts/patna/networkModes/changeExpBeta/relaxedPlans/output_changeExpBeta/output_plans.xml.gz";
		ModeChoiceRandomizer.ASCRandomizerStyle ascRandomizeStyle = ModeChoiceRandomizer.ASCRandomizerStyle.axial_fixedVariation;
		boolean useConfigRandomSeed = false; //false --> can't reproduce results

		double stepSize = 1.0;
		int iterations2Convergence = 10; // 600
		int averagingIterations = 5; // //100+
		double selfTuningWt = 1.0;
		int warmUpItrs = 5;

		ObjectiveFunctionType objFunType = ObjectiveFunctionType.SUM_SCALED_LOG;

		int opdytsIterations = 10;

		boolean updateStepSize = true;

		if ( args.length>0 ) {
			configFile = args[0];
			OUT_DIR = args[1];
			relaxedPlans = args[2];
			ascRandomizeStyle = ModeChoiceRandomizer.ASCRandomizerStyle.valueOf(args[3]);

			// opdyts params
			stepSize = Double.valueOf(args[4]);

			iterations2Convergence = Integer.valueOf(args[5]);
			averagingIterations = Integer.valueOf(args[6]);

			selfTuningWt = Double.valueOf(args[7]);
			warmUpItrs = Integer.valueOf(args[8]);
			useConfigRandomSeed = Boolean.valueOf(args[9]);

			updateStepSize = Boolean.valueOf(args[10]);

			objFunType = ObjectiveFunctionType.valueOf(args[11]);

			opdytsIterations = Integer.valueOf(args[12]);

		}

		Config config = ConfigUtils.loadConfig(configFile, new OpdytsConfigGroup());
		config.plans().setInputFile(relaxedPlans);
		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn); // must be warn, since opdyts override few things
		config.controler().setOutputDirectory(OUT_DIR);

		// from GF, every run should have a different random seed.
		if (!useConfigRandomSeed) {
			int randomSeed = new Random().nextInt(9999);
			config.global().setRandomSeed(randomSeed);
		}

		OpdytsConfigGroup opdytsConfigGroup = ConfigUtils.addOrGetModule(config, OpdytsConfigGroup.GROUP_NAME, OpdytsConfigGroup.class ) ;
		opdytsConfigGroup.setOutputDirectory(OUT_DIR);
		opdytsConfigGroup.setDecisionVariableStepSize(stepSize);
		opdytsConfigGroup.setNumberOfIterationsForConvergence(iterations2Convergence);
		opdytsConfigGroup.setSelfTuningWeight(selfTuningWt);
		opdytsConfigGroup.setWarmUpIterations(warmUpItrs);
		opdytsConfigGroup.setMaxIteration(opdytsIterations);

		List<String> modes2consider = Arrays.asList("car","bike","motorbike");

		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.strategy().setFractionOfIterationsToDisableInnovation(Double.POSITIVE_INFINITY);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		//****************************** mainly opdyts settings ******************************

		MATSimOpdytsControler<ModeChoiceDecisionVariable> opdytsControler = new MATSimOpdytsControler<>(scenario);

		DistanceDistribution referenceStudyDistri = new PatnaNetworkModesOneBinDistanceDistribution(PATNA_1_PCT);
		OpdytsModalStatsControlerListener stasControlerListner = new OpdytsModalStatsControlerListener(modes2consider,referenceStudyDistri);

		// following is the  entry point to start a matsim controler together with opdyts
		MATSimSimulator2<ModeChoiceDecisionVariable> simulator = new MATSimSimulator2<>( new MATSimStateFactoryImpl<>(), scenario);

		final ObjectiveFunctionType finalObjFunType = objFunType;
		simulator.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
//				addControlerListenerBinding().to(KaiAnalysisListener.class);
				addControlerListenerBinding().toInstance(stasControlerListner);

				this.bind(ModalShareEventHandler.class);
				this.addControlerListenerBinding().to(ModalShareControlerListener.class);

				this.bind(ModalTripTravelTimeHandler.class);
				this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);

				this.bind(ScoringParametersForPerson.class).to(EveryIterationScoringParameters.class);

				this.bind(ObjectiveFunctionType.class).toInstance(finalObjFunType);
				this.bind(ObjectiveFunctionEvaluator.class).asEagerSingleton();

				this.addControlerListenerBinding().toInstance(OpdytsModeChoiceUtils.copyVectorFilesToParentDirAndRemoveITERSDir(true));
			}
		});

		// this is the objective Function which returns the value for given SimulatorState
		// in my case, this will be the distance based modal split
		ObjectiveFunction objectiveFunction = new ModeChoiceObjectiveFunction(referenceStudyDistri);

		//search algorithm
		// randomize the decision variables (for e.g.\ utility parameters for modes)
		DecisionVariableRandomizer<ModeChoiceDecisionVariable> decisionVariableRandomizer = new ModeChoiceRandomizer(scenario,
				RandomizedUtilityParametersChoser.ONLY_ASC, PATNA_1_PCT, null, modes2consider, ascRandomizeStyle);
		if (updateStepSize) {
			((ModeChoiceRandomizer)decisionVariableRandomizer).updateStepSizeEveryIteration( simulator.getOpdytsIterationWrapper() );
		}

		// what would be the decision variables to optimize the objective function.
		ModeChoiceDecisionVariable initialDecisionVariable = new ModeChoiceDecisionVariable(scenario.getConfig().planCalcScore(),scenario, modes2consider, PATNA_1_PCT);

		opdytsControler.addNetworkModeOccupancyAnalyzr(simulator);
		opdytsControler.run(simulator, decisionVariableRandomizer, initialDecisionVariable, objectiveFunction);

	}
}