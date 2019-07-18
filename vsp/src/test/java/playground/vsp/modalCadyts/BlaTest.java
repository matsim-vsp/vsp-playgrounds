package playground.vsp.modalCadyts;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.cadyts.car.CadytsCarModule;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import playground.vsp.cadyts.marginals.ModalDistanceCadytsContext;
import playground.vsp.cadyts.marginals.ModalDistanceCadytsContext2;
import playground.vsp.cadyts.marginals.ModalDistanceCadytsModule;
import playground.vsp.cadyts.marginals.ModalDistanceCadytsModule2;
import playground.vsp.cadyts.marginals.prep.DistanceBin;
import playground.vsp.cadyts.marginals.prep.DistanceDistribution;
import playground.vsp.cadyts.marginals.prep.DistanceDistribution2;
import playground.vsp.cadyts.marginals.prep.ModalDistanceBinIdentifier;

import javax.inject.Inject;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

@RunWith(Parameterized.class)
public class BlaTest {

	private double cadytsWt ;
	private double cadytsMgnWt;

	private static final URL EQUIL_DIR = ExamplesUtils.getTestScenarioURL("equil-mixedTraffic");

	public BlaTest(double cadytsWt, double mgnWt) {
		this.cadytsWt = cadytsWt;
		this.cadytsMgnWt = mgnWt;
	}

	@Parameterized.Parameters(name = "{index}: cadytsCountsWeight == {0}; cadytsMarginalWeight == {1};")
	public static Collection<Object[]> parameterObjects () {
		return Arrays.asList(new Object[][] {
				//{150.0, 0.0} ,
				{0.0, 150.0},
				//{150.0, 150.0}
		});
	}

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();


	// TODO: Think about a better integration test. E.g. synthetic network with three routes
	@Test
	public final void simultaneousMarginalsAndCountCalibrationTest() {
		double beelineDistanceFactorForNetworkModes = 1.0;

		String outPut = "test/output/playground/vsp/modalCadyts/CountsAndModalDistanceCadytsIT/blaTest/" +
				"cadytsWt" + this.cadytsWt + "_cadytsMgnWt" + this.cadytsMgnWt;

//        Config config = ConfigUtils.loadConfig(IOUtils.newUrl(EQUIL_DIR,"config-with-mode-vehicles.xml"));
//        config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExis	tingFiles);

		Config config = createAndPrepareConfig(outPut);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		modifyNetworkCapacityByFactor(scenario, 0.1);
		createPopulation(scenario);

		DistanceDistribution2 inputDistanceDistribution = getInputDistanceDistribution(beelineDistanceFactorForNetworkModes);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new ModalDistanceCadytsModule2(inputDistanceDistribution));


		Counts<Link> counts = createCounts();
		scenario.addScenarioElement(Counts.ELEMENT_NAME, counts);
		installCadyts(config, controler, counts);

		controler.run();
	}

	private void installCadyts(Config config, Controler controler, Counts<Link> counts) {
		controler.addOverridingModule(new CadytsCarModule(counts));
		final double cadytsCountsScoringWeight = this.cadytsWt * config.planCalcScore().getBrainExpBeta();

		final double cadytsMarginalsScoringWeight = this.cadytsMgnWt * config.planCalcScore().getBrainExpBeta();
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			@Inject
			private CadytsContext cadytsContext;
			@Inject
			ScoringParametersForPerson parameters;

			@Inject private ModalDistanceCadytsContext2 marginalCadytsContext;

			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				SumScoringFunction sumScoringFunction = new SumScoringFunction();

				final ScoringParameters params = parameters.getScoringParameters(person);
				sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params,
						controler.getScenario().getNetwork()));
				sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params));
				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				final CadytsScoring<Link> scoringFunctionCounts = new CadytsScoring<Link>(person.getSelectedPlan(),
						config,
						cadytsContext);
				scoringFunctionCounts.setWeightOfCadytsCorrection(cadytsCountsScoringWeight);

				final CadytsScoring<Id<DistanceBin>> scoringFunctionMarginals = new CadytsScoring<>(person.getSelectedPlan(),
						config,
						marginalCadytsContext);

				scoringFunctionMarginals.setWeightOfCadytsCorrection(cadytsMarginalsScoringWeight);
				sumScoringFunction.addScoringFunction(scoringFunctionMarginals);

				sumScoringFunction.addScoringFunction(scoringFunctionCounts);

				return sumScoringFunction;
			}
		});
	}

	private void modifyNetworkCapacityByFactor(Scenario scenario, double factor) {
		for(Link l : scenario.getNetwork().getLinks().values()) {
			l.setCapacity(l.getCapacity() * factor);
		}
	}

	private void createPopulation(Scenario scenario) {
		Network network = scenario.getNetwork();
		Population population = scenario.getPopulation();

		for (int i = 0; i<1000; i++){
			Id<Person> personId = Id.createPersonId(population.getPersons().size() + 1);
			Person person = population.getFactory().createPerson(personId);
			Plan plan = population.getFactory().createPlan();
			//link23 to link 1

			//home
			Activity h = population.getFactory()
					.createActivityFromCoord("h",
							network.getLinks().get(Id.createLinkId(1)).getFromNode().getCoord());
			h.setEndTime(6. * 3600. /*+ MatsimRandom.getRandom().nextInt(3600)*/);
			plan.addActivity(h);

//	            25.000m
			plan.addLeg(population.getFactory().createLeg("car"));

			//work
			Activity w = population.getFactory()
					.createActivityFromLinkId("w",
							Id.createLinkId(20));
			w.setEndTime(16. * 3600. + MatsimRandom.getRandom().nextInt(3600));
			plan.addActivity(w);

//	            45.000m
			plan.addLeg(population.getFactory().createLeg("car"));

			//leisure
			Activity l = population.getFactory()
					.createActivityFromLinkId("l",
							Id.createLinkId(22));
			l.setEndTime(18*3600);
			plan.addActivity(l);

			//10.000m
			plan.addLeg(population.getFactory().createLeg("car"));

			//home2
			Activity h2 = population.getFactory()
					.createActivityFromLinkId("h",
							Id.createLinkId(23));
			h2.setEndTime(22 * 3600);
			plan.addActivity(h2);


			person.addPlan(plan);
			population.addPerson(person);
		}
	}

	/**
	 * @param outPut
	 * @return
	 */
	private Config createAndPrepareConfig(String outPut) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(EQUIL_DIR.toString() + "network.xml");

		config.controler().setOutputDirectory(outPut);

		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(20);
		config.counts().setWriteCountsInterval(1);
		config.counts().setAverageCountsOverIterations(1);


		PlanCalcScoreConfigGroup.ActivityParams home = new PlanCalcScoreConfigGroup.ActivityParams("h");
		home.setMinimalDuration(6*3600);
		home.setTypicalDuration(6*3600);
		home.setEarliestEndTime(6*3600);
		config.planCalcScore().addActivityParams(home);


		PlanCalcScoreConfigGroup.ActivityParams work = new PlanCalcScoreConfigGroup.ActivityParams("w");
		work.setMinimalDuration(8*3600);
		work.setTypicalDuration(8*3600);
		work.setEarliestEndTime(14*3600);
		work.setOpeningTime(6*3600);
		work.setClosingTime(18*3600);
		config.planCalcScore().addActivityParams(work);

		PlanCalcScoreConfigGroup.ActivityParams leisure = new PlanCalcScoreConfigGroup.ActivityParams("l");
		leisure.setMinimalDuration(1*3600);
		leisure.setTypicalDuration(1.5*3600);
		leisure.setEarliestEndTime(17*3600);
		config.planCalcScore().addActivityParams(leisure);

		// add mode choice to it
		StrategyConfigGroup.StrategySettings modeChoice = new StrategyConfigGroup.StrategySettings();
		modeChoice.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode);
		modeChoice.setWeight(0.25);

		// add Reroute choice to it
		StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings();
		reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
		reRoute.setWeight(0.25);

		config.strategy().addStrategySettings(reRoute);
		config.strategy().addStrategySettings(modeChoice);

		StrategyConfigGroup.StrategySettings bestScore = new StrategyConfigGroup.StrategySettings();
		bestScore.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.BestScore);
		bestScore.setWeight(0.5);
		config.strategy().addStrategySettings(bestScore);

		SubtourModeChoiceConfigGroup sbtModeChoiceConfgGroup = config.subtourModeChoice();
		String[] modes = new String[]{"car", "bicycle"};

		sbtModeChoiceConfgGroup.setModes(modes);
		config.plansCalcRoute().setNetworkModes(Arrays.asList(modes));
		config.qsim().setMainModes(Arrays.asList(modes));
		config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
		config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);

		config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);
		config.vehicles().setVehiclesFile(EQUIL_DIR + "mode-vehicles.xml");
		config.travelTimeCalculator().setAnalyzedModes(new HashSet<>((Arrays.asList(modes))));
		config.travelTimeCalculator().setSeparateModes(true);
		config.travelTimeCalculator().setFilterModes(true);
		config.changeMode().setModes(modes);
		config.changeMode().setBehavior(ChangeModeConfigGroup.Behavior.fromSpecifiedModesToSpecifiedModes);
		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);

		config.planCalcScore().getOrCreateModeParams("car").setConstant(-2);
		config.planCalcScore().getOrCreateModeParams("bicycle");

		config.controler().setRoutingAlgorithmType(ControlerConfigGroup.RoutingAlgorithmType.Dijkstra);

		return config;
	}

	private Counts<Link> createCounts() {
		Counts<Link> counts = new Counts<Link>();
		counts.setDescription("test");
		counts.setYear(1900);

		Count<Link> c2 = counts.createAndAddCount(Id.createLinkId(2), "c2");
		c2.createVolume(7, 300);
		c2.createVolume(8, 0);

		Count<Link> c3 = counts.createAndAddCount(Id.createLinkId(3), "c3");
		c3.createVolume(7, 100);
		c3.createVolume(8, 0);

		Count<Link> c4= counts.createAndAddCount(Id.createLinkId(4), "c4");
		c4.createVolume(7, 50);
		c4.createVolume(8, 0);

		Count<Link> c5 = counts.createAndAddCount(Id.createLinkId(5), "c5");
		c5.createVolume(7, 50);
		c5.createVolume(8, 0);

		Count<Link> c6 = counts.createAndAddCount(Id.createLinkId(6), "c6");
		c6.createVolume(7, 0);
		c6.createVolume(8, 0);

		Count<Link> c7 = counts.createAndAddCount(Id.createLinkId(7), "c7");
		c7.createVolume(7, 50);
		c7.createVolume(8, 0);

		Count<Link> c8 = counts.createAndAddCount(Id.createLinkId(8), "c8");
		c8.createVolume(7, 50);
		c8.createVolume(8, 0);

		Count<Link> c9 = counts.createAndAddCount(Id.createLinkId(9), "c9");
		c9.createVolume(7, 100);
		c9.createVolume(8, 0);

		Count<Link> c10 = counts.createAndAddCount(Id.createLinkId(10), "c10");
		c10.createVolume(7, 300);
		c10.createVolume(8, 0);


//    	CountsWriter writer = new CountsWriter(counts);
//    	writer.write("C:/Users/Work/testCounts.xml");

//    	scenario.getConfig().counts().setInputFile("C:/Users/Work/testCounts.xml");

		return counts;
	}


	private DistanceDistribution2 getInputDistanceDistribution(double beelineDistanceFactorForNetworkModes){
		DistanceDistribution2 inputDistanceDistribution = new DistanceDistribution2();

		//inputDistanceDistribution.setModeToScalingFactor("car",1);
		//inputDistanceDistribution.setModeToScalingFactor("bicycle",1);

		//leg leisure to h = 10.000km
		inputDistanceDistribution.add("car", new DistanceBin.DistanceRange(0.0, 10000.1), 10000, 200);
		inputDistanceDistribution.add("bicycle", new DistanceBin.DistanceRange(0.0, 10000.1), 10000, 800);

		//h1 to work = 20.000km
		inputDistanceDistribution.add("car", new DistanceBin.DistanceRange(10000.1, 20000.1), 10000, 500);
		inputDistanceDistribution.add("bicycle", new DistanceBin.DistanceRange(10000.1, 20000.1), 10000, 500);

		//work to leisure = ~ 27.000 km
		inputDistanceDistribution.add("car", new DistanceBin.DistanceRange(20000.1, 50000.0), 10000, 800);
		inputDistanceDistribution.add("bicycle", new DistanceBin.DistanceRange(20000.1, 50000.0), 10000, 200);

//        //anything above
//        inputDistanceDistribution.addToDistribution("car", new DistanceBin.DistanceRange(50000.0,90000.),80);
//        inputDistanceDistribution.addToDistribution("bicycle", new DistanceBin.DistanceRange(50000.0,90000.),20);

		return inputDistanceDistribution;
	}
}
