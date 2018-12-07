package playground.kai.usecases.opdytsintegration.modechoice;

import java.util.Arrays;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.opdyts.MATSimOpdytsRunner;
import org.matsim.contrib.opdyts.OpdytsConfigGroup;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.utils.EveryIterationScoringParameters;
import org.matsim.contrib.opdyts.microstate.MATSimState;
import org.matsim.contrib.opdyts.microstate.MATSimStateFactoryImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;

import playground.kairuns.run.KNBerlinControler;
import playground.kairuns.run.KNBerlinControler.A100;

/**
 * 
 * @author Kai Nagel based on Gunnar Flötteröd
 *
 */
class KNModeChoiceCalibMain {
	public static void main(String[] args) {
		boolean equil = true ;
		boolean calib = true ;
		boolean modeChoice = false ;
		boolean assignment = false ;

		final Config config = KNBerlinControler.prepareConfig(args, assignment, equil, modeChoice, A100.base ) ;
		
		config.transitRouter().setDirectWalkFactor(1.e7);
		String outputDirectory = "" ;
		if ( calib ) {
			if ( equil ) {
				config.plans().setInputFile("/Users/nagel/kairuns/equil/relaxed/output_plans.xml.gz");
				outputDirectory = "/Users/nagel/kairuns/equil/opdyts/" ;
			} else {
				config.plans().setInputFile("/Users/nagel/kairuns/a100/relaxed/output_plans.xml.gz");
				outputDirectory = "/Users/nagel/kairuns/a100/opdyts/" ;
			}
		} else {
			if ( equil ) {
				outputDirectory = "/Users/nagel/kairuns/equil/relaxed/" ;
			} else {
				outputDirectory = "/Users/nagel/kairuns/a100/relaxed/" ;
			}
		}
		config.controler().setOutputDirectory(outputDirectory);

		run(config, equil, calib, assignment, outputDirectory, false) ;
	}
	
	public static void run( Config config, boolean equil, boolean calib, boolean assignment, String outputDirectory, boolean testcase ) {

		OutputDirectoryLogging.catchLogEntries();

		System.out.println("STARTED ...");

		
		config.planCalcScore().setBrainExpBeta(1.);

		config.strategy().clearStrategySettings();
		{
			StrategySettings stratSets = new StrategySettings() ;
			stratSets.setStrategyName( DefaultSelector.ChangeExpBeta.toString() );
			stratSets.setWeight(0.9);
			config.strategy().addStrategySettings(stratSets);
		}
		if ( !equil ) {
			StrategySettings stratSets = new StrategySettings() ;
			stratSets.setStrategyName( DefaultStrategy.ReRoute );
			stratSets.setWeight(0.1);
			config.strategy().addStrategySettings(stratSets);
		}
		{
			StrategySettings stratSets = new StrategySettings() ;
			stratSets.setStrategyName( DefaultStrategy.ChangeSingleTripMode );
			stratSets.setWeight(0.1);
			config.strategy().addStrategySettings(stratSets);
			// note that changeMode _always_ includes ReRoute!
		}
		if ( equil ) {
			config.changeMode().setModes(new String[]{"car","pt"});
		} else {
			config.changeMode().setModes(new String[]{"car","pt","bike","walk"});
		}

		// ===

		boolean unterLindenQuiet = false ;
		final Scenario scenario = KNBerlinControler.prepareScenario(equil, unterLindenQuiet, config) ;

		// ===

		AbstractModule overrides = KNBerlinControler.prepareOverrides(assignment);
		overrides = AbstractModule.override(Arrays.asList(overrides), new AbstractModule(){
			@Override public void install() {
				bind( ScoringParametersForPerson.class ).to( EveryIterationScoringParameters.class ) ;
			}
		}  ) ;

		// ===

		// For some reason, this does not work. Gunnar, 2018-09-27.
		config.planCalcScore().setWriteExperiencedPlans(false);
		
		if ( calib ) {

			OpdytsConfigGroup opdytsConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(), OpdytsConfigGroup.class);
			
			//all of these will go to opdytsConfigGroup.
			int maxIterations = 10 ;
			int maxTransitions = Integer.MAX_VALUE ;
			// int populationSize = 10 ;
			// boolean interpolate = true ;
			// boolean includeCurrentBest = false ;
			int warmupIterations = 1;
			boolean useAllWarmUpIterations = false;

			opdytsConfigGroup.setMaxIteration(maxIterations);
			opdytsConfigGroup.setMaxTransition(maxTransitions);
			// opdytsConfigGroup.setPopulationSize( populationSize );
			// opdytsConfigGroup.setInterpolate(interpolate);
			// opdytsConfigGroup.setIncludeCurrentBest(includeCurrentBest);
			opdytsConfigGroup.setWarmUpIterations(warmupIterations);
			opdytsConfigGroup.setUseAllWarmUpIterations(useAllWarmUpIterations);
			if ( testcase ) {
				opdytsConfigGroup.setNumberOfIterationsForConvergence(2);
				opdytsConfigGroup.setNumberOfIterationsForAveraging(1);
			} else {
				opdytsConfigGroup.setNumberOfIterationsForConvergence(100);
				opdytsConfigGroup.setNumberOfIterationsForAveraging(10);
			}
			opdytsConfigGroup.setStartTime(0);
			opdytsConfigGroup.setBinSize(3600);
			opdytsConfigGroup.setBinCount(24);

			
			// MATSimOpdytsControler<ModeChoiceDecisionVariable> opdytsControler = new MATSimOpdytsControler<>(scenario);
			MATSimOpdytsRunner<ModeChoiceDecisionVariable, MATSimState> opdytsControler = new MATSimOpdytsRunner<>(scenario, new MATSimStateFactoryImpl<>());

			// if not provided created by default using opdytsConfigGroup. Amit July'17
			// final TimeDiscretization timeDiscretization = new TimeDiscretization(0, 3600,
			// 24);
			// opdytsControler.setTimeDiscretization(timeDiscretization);

			// final MATSimSimulator2<ModeChoiceDecisionVariable> simulator = new
			// MATSimSimulator2<>( new MATSimStateFactoryImpl<>(),
			// scenario);
			// simulator.addOverridingModule( overrides ) ;
			opdytsControler.addOverridingModule(overrides);

			// alternatively, opdytsControler.addNetworkModeOccupancyAnalyzr(simulator); could be used.
			// Set<String> relevantNetworkModes = new HashSet<>();
			// relevantNetworkModes.add("car");
			// simulator.addSimulationStateAnalyzer(new
			// DifferentiatedLinkOccupancyAnalyzer.Provider(timeDiscretization,
			// relevantNetworkModes,
			// new LinkedHashSet<>(scenario.getNetwork().getLinks().keySet())));
			// the above is included by default
		
			final ModeChoiceDecisionVariable initialDecisionVariable = new ModeChoiceDecisionVariable( scenario.getConfig().planCalcScore(), scenario );

			// final FixedIterationNumberConvergenceCriterion convergenceCriterion ;
			// if ( testcase ) {
			// opdytsConfigGroup.setNumberOfIterationsForConvergence(2);
			// opdytsConfigGroup.setNumberOfIterationsForAveraging(1);
			// convergenceCriterion= new FixedIterationNumberConvergenceCriterion(2, 1 );
			// } else {
			// opdytsConfigGroup.setNumberOfIterationsForConvergence(2);
			// opdytsConfigGroup.setNumberOfIterationsForAveraging(1);
			// convergenceCriterion= new FixedIterationNumberConvergenceCriterion(100, 10 );
			// }
			// opdytsControler.setFixedIterationNumberConvergenceCriterion(convergenceCriterion);
			// // if not provided created by default using opdytsConfigGroup. Amit July'17

			// opdytsControler.run(simulator, new ModeChoiceRandomizer(scenario), initialDecisionVariable, new ModeChoiceObjectiveFunction(equil));

//			opdytsControler.run(new ModeChoiceRandomizer(scenario), initialDecisionVariable, new ModeChoiceObjectiveFunction(equil));
			// yyyyyy did not compile

		} else {
			config.controler().setLastIteration(1000);
			config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
			config.planCalcScore().setFractionOfIterationsToStartScoreMSA(0.8);

			Controler controler = new Controler( scenario ) ;
			controler.addOverridingModule( overrides );
			controler.run();

		}

		System.out.println("... DONE.");

	}

}
