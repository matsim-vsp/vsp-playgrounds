/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package playground.vsp.modalCadyts;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ChangeModeConfigGroup.Behavior;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import playground.vsp.cadyts.marginals.DistanceDistribution;
import playground.vsp.cadyts.marginals.ModalDistanceCadytsContext;

import javax.inject.Inject;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

@RunWith(Parameterized.class)
public class CountsAndModalDistanceCadytsIT {
	
	private double cadytsWt ;
    private double cadytsMgnWt;

    private static final URL EQUIL_DIR = ExamplesUtils.getTestScenarioURL("equil-mixedTraffic");

    public CountsAndModalDistanceCadytsIT(double cadytsWt, double mgnWt) {
    	  this.cadytsWt = cadytsWt;
          this.cadytsMgnWt = mgnWt;
    }
    
    @Parameterized.Parameters(name = "{index}: cadytsCountsWeight == {0}; cadytsMarginalWeight == {1};")
    public static Collection<Object[]> parameterObjects () {
        return Arrays.asList(new Object[][] {
        		//{150.0, 0.0} ,
				{0.0, 15000.0},
				//{150.0, 150.0}
        });
    }
    
    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void test() {

		Config config = createConfig();
		CadytsConfigGroup cadytsConfigGroup = new CadytsConfigGroup();
		cadytsConfigGroup.setWriteAnalysisFile(true);
		config.addModule(cadytsConfigGroup);

		Scenario scenario = cretaeScenario(config);

		Controler controler = new Controler(scenario);

		DistanceDistribution inputDistanceDistribution = createDistanceDistribution();
		//controler.addOverridingModule(new ModalDistanceCadytsModule(inputDistanceDistribution));

		// we need to also set the scoring function to see an effect
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {

			@Inject
			private ScoringParametersForPerson parameters;

			@Inject
			private ModalDistanceCadytsContext modalDistanceCadytsContext;

			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				SumScoringFunction sumScoringFunction = new SumScoringFunction();

				final ScoringParameters params = parameters.getScoringParameters(person);
				sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params,
						controler.getScenario().getNetwork()));
				sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params));
				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				final CadytsScoring<Id<DistanceDistribution.DistanceBin>> scoringFunctionMarginals = new CadytsScoring<>(person.getSelectedPlan(),
						config,
						modalDistanceCadytsContext);

				// in this case where all the other factors are equal, a small weight is appropriate and leads to more
				// stable results. If combined with other things impacting the agents's score. A much higher value in the
				// range of 100 - 200 is probably appropriate
				scoringFunctionMarginals.setWeightOfCadytsCorrection(1);
				sumScoringFunction.addScoringFunction(scoringFunctionMarginals);

				return sumScoringFunction;
			}
		});

		controler.run();
	}

	private static Scenario cretaeScenario(Config config) {

		Scenario scenario = ScenarioUtils.createScenario(config);
		createNetwork(scenario.getNetwork());
		createPopulation(scenario.getPopulation(), scenario.getNetwork());

		VehicleType car = scenario.getVehicles().getFactory().createVehicleType(Id.create("car", VehicleType.class));
		car.setMaximumVelocity(100 / 3.6);
		car.setLength(7.5);

		// make bike and car equally fast for now
		VehicleType bike = scenario.getVehicles().getFactory().createVehicleType(Id.create("bike", VehicleType.class));
		bike.setMaximumVelocity(100 / 3.6);
		bike.setLength(7.5);
		scenario.getVehicles().addVehicleType(car);
		scenario.getVehicles().addVehicleType(bike);
		return scenario;
	}

	private static void createNetwork(Network network) {

		Node node1 = network.getFactory().createNode(Id.createNodeId("node-1"), new Coord(0, 0));
		Node node2 = network.getFactory().createNode(Id.createNodeId("node-2"), new Coord(50, 0));
		Node node3 = network.getFactory().createNode(Id.createNodeId("node-3"), new Coord(2050, 0));
		Node node4 = network.getFactory().createNode(Id.createNodeId("node-4"), new Coord(2100, 0));

		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);

		Link link1 = createLink(node1, node2, "start-link", 50, network.getFactory());
		Link link2 = createLink(node2, node3, "long-link", 2500, network.getFactory());
		Link link3 = createLink(node2, node3, "short-link", 2000, network.getFactory());
		Link link4 = createLink(node3, node4, "end-link", 50, network.getFactory());

		network.addLink(link1);
		network.addLink(link2);
		network.addLink(link3);
		network.addLink(link4);
	}

	private static Link createLink(Node from, Node to, String id, double length, NetworkFactory factory) {
		Link link = factory.createLink(Id.createLinkId(id), from, to);
		link.setLength(length);
		link.setCapacity(10000);
		link.setAllowedModes(new HashSet<>(Arrays.asList(TransportMode.car, TransportMode.bike)));
		link.setFreespeed(100 / 3.6);
		return link;
	}

	private static DistanceDistribution createDistanceDistribution() {

		DistanceDistribution result = new DistanceDistribution(1.0);
		result.add(TransportMode.car, 2500, 2700, 100, 0);
		result.add(TransportMode.car, 1900, 2150, 100, 500);
		result.add(TransportMode.bike, 1900, 2150, 100, 500);
		result.add(TransportMode.bike, 2500, 2700, 100, 0);
		return result;
	}

	private static void createPopulation(Population population, Network network) {

		for (int i = 0; i < 1000; i++) {
			Id<Person> personId = Id.createPersonId(population.getPersons().size() + 1);
			Person person = population.getFactory().createPerson(personId);

			person.addPlan(createPlan(TransportMode.bike, network, population.getFactory()));
			person.addPlan(createPlan(TransportMode.car, network, population.getFactory()));
			population.addPerson(person);
		}
	}

	private static Plan createPlan(String mode, Network network, PopulationFactory factory) {

		Plan plan = factory.createPlan();

		//home
		Activity h = factory
				.createActivityFromCoord("home",
						network.getLinks().get(Id.createLinkId("start-link")).getFromNode().getCoord());
		h.setEndTime(6. * 3600. /*+ MatsimRandom.getRandom().nextInt(3600)*/);
		plan.addActivity(h);

		plan.addLeg(factory.createLeg(mode));

		//work
		Activity w = factory
				.createActivityFromLinkId("work",
						Id.createLinkId("end-link"));
		w.setEndTime(16. * 3600. + MatsimRandom.getRandom().nextInt(3600));
		plan.addActivity(w);

		return plan;
	}


	// TODO: Think about a better integration test. E.g. synthetic network with three routes
	@Test
    public final void simultaneousMarginalsAndCountCalibrationTest() {
        double beelineDistanceFactorForNetworkModes = 1.0;

        String outPut = "test/output/playground/vsp/modalCadyts/CountsAndModalDistanceCadytsIT/nemoModeLocationChoiceTest/" + 
        				"cadytsWt" + this.cadytsWt + "_cadytsMgnWt" + this.cadytsMgnWt;
        
//        Config config = ConfigUtils.loadConfig(IOUtils.newUrl(EQUIL_DIR,"config-with-mode-vehicles.xml"));
//        config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExis	tingFiles);
        
        Config config = createAndPrepareConfig(outPut);
        
        Scenario scenario = ScenarioUtils.loadScenario(config);
        modifyNetworkCapacityByFactor(scenario, 0.1);
        createPopulation(scenario);
        
        DistanceDistribution inputDistanceDistribution = getInputDistanceDistribution(beelineDistanceFactorForNetworkModes);
       
        Controler controler = new Controler(scenario);
		// controler.addOverridingModule(new ModalDistanceCadytsModule(inputDistanceDistribution));


        Counts<Link> counts = createCounts();
        scenario.addScenarioElement(Counts.ELEMENT_NAME, counts);
        installCadyts(config, controler, counts);
        
    	controler.run();
 
    	TabularFileParserConfig tbfConfig = new TabularFileParserConfig();
    	tbfConfig.setDelimiterRegex("\t");
    	TabularFileParser tbfParser = new TabularFileParser();
    	
    	TbfHandlerMarginals mgnHandler = new TbfHandlerMarginals();
    	TbfHandlerCounts countsHandler = new TbfHandlerCounts();
    	
    	double mgnAbsoluteErrorSum = Double.NaN;
    	double mgnRelativeAbsoluteErrorSum  = Double.NaN;
    	double countsAbsoluteErrorSum  = Double.NaN;
    	double countsRelativeAbsoluteErrorSum  = Double.NaN;
    	
    	for(int iteration = 5; iteration <=20; iteration +=5) {
    		tbfConfig.setFileName(outPut + "/ITERS/it." + iteration + "/" + iteration + ".flowAnalysis.txt");
    		tbfParser.parse(tbfConfig, countsHandler);

    		tbfConfig.setFileName(outPut + "/ITERS/it." + iteration + "/" + iteration + ".flowAnalysis_marginals.txt");
    		tbfParser.parse(tbfConfig, mgnHandler);
    		
    		
    		switch(iteration) {
    		case 5:
    			
    				// after 5 iterations, the run's output data shows no differences at all !?
    			
    				Assert.assertEquals("wrong amount of bikes in short distance range in iteration " + iteration, 172, mgnHandler.bikesOnShortDistance, 0.1);	
    				Assert.assertEquals("wrong amount of bikes in medium distance range in iteration " + iteration, 134, mgnHandler.bikesOnMediumDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of bikes in long distance range in iteration " + iteration, 142, mgnHandler.bikesOnLongDistance, 0.1 );
    				
    				Assert.assertEquals("wrong amount of cars in short distance range in iteration " + iteration, 828, mgnHandler.carsOnShortDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of cars in medium distance range in iteration " + iteration, 866, mgnHandler.carsOnMediumDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of cars in long distance range in iteration " + iteration, 858, mgnHandler.carsOnLongDistance, 0.1 );	
    				
    				Assert.assertEquals("wrong volume for 6am to 7am on link 2 in iteration " + iteration, 145, countsHandler.linkVolumes[0], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 3 in iteration " + iteration, 124, countsHandler.linkVolumes[1], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 4 in iteration " + iteration, 166, countsHandler.linkVolumes[2], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 5 in iteration " + iteration, 154, countsHandler.linkVolumes[3], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 6 in iteration " + iteration, 21, countsHandler.linkVolumes[4], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 7 in iteration " + iteration, 300, countsHandler.linkVolumes[5], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 8 in iteration " + iteration, 0, countsHandler.linkVolumes[6], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 9 in iteration " + iteration, 0, countsHandler.linkVolumes[7], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 10 in iteration " + iteration, 0, countsHandler.linkVolumes[8], 0.1); 
    				
    			mgnAbsoluteErrorSum = mgnHandler.absoluteErrorSum;
    			mgnRelativeAbsoluteErrorSum = mgnHandler.relativeAbsoluteErrorSum;
    			countsAbsoluteErrorSum = countsHandler.absoluteErrorSum;
    			countsRelativeAbsoluteErrorSum = countsHandler.relativeAbsoluteErrorSum;
    			break;
    		case 10:
    			
    			Assert.assertTrue("the sum of relative absolute erros for marginals calibration should be less in iteration 10 than in iteration 5. old sum = " + mgnRelativeAbsoluteErrorSum + " new sum = " + mgnHandler.relativeAbsoluteErrorSum
    					, mgnHandler.relativeAbsoluteErrorSum < mgnRelativeAbsoluteErrorSum);	
    			Assert.assertTrue("the sum of absolute erros for marginals calibration should be less in iteration 10 than in iteration 5. old sum = " + mgnAbsoluteErrorSum + " new sum = " + mgnHandler.absoluteErrorSum,
    					mgnHandler.absoluteErrorSum < mgnAbsoluteErrorSum);
    			Assert.assertTrue("the sum of relative absolute erros for counts calibration should be less in iteration 10 than in iteration 5. old sum = " + countsRelativeAbsoluteErrorSum + " new sum = " + countsHandler.relativeAbsoluteErrorSum,
    					countsHandler.relativeAbsoluteErrorSum < countsRelativeAbsoluteErrorSum);	
    			Assert.assertTrue("the sum of absolute erros for counts calibration should be less in iteration 10 than in iteration 5.  old sum = " + countsAbsoluteErrorSum + " new sum = " + countsHandler.absoluteErrorSum,
    					countsHandler.absoluteErrorSum < countsAbsoluteErrorSum);
    			
    			if(this.cadytsWt == 150.0 && cadytsMgnWt == 0.) {

    				Assert.assertEquals("wrong amount of bikes in short distance range in iteration " + iteration, 216, mgnHandler.bikesOnShortDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of bikes in medium distance range in iteration " + iteration, 175, mgnHandler.bikesOnMediumDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of bikes in long distance range in iteration " + iteration, 127, mgnHandler.bikesOnLongDistance, 0.1 );
    				
    				Assert.assertEquals("wrong amount of cars in short distance range in iteration " + iteration, 784, mgnHandler.carsOnShortDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of cars in medium distance range in iteration " + iteration, 825, mgnHandler.carsOnMediumDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of cars in long distance range in iteration " + iteration, 873, mgnHandler.carsOnLongDistance, 0.1 );
    				
    				Assert.assertEquals("wrong volume for 6am to 7am on link 2 in iteration " + iteration, 117, countsHandler.linkVolumes[0], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 3 in iteration " + iteration, 110, countsHandler.linkVolumes[1], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 4 in iteration " + iteration, 101, countsHandler.linkVolumes[2], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 5 in iteration " + iteration, 65, countsHandler.linkVolumes[3], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 6 in iteration " + iteration, 21, countsHandler.linkVolumes[4], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 7 in iteration " + iteration, 43, countsHandler.linkVolumes[5], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 8 in iteration " + iteration, 20, countsHandler.linkVolumes[6], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 9 in iteration " + iteration, 300, countsHandler.linkVolumes[7], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 10 in iteration " + iteration, 129, countsHandler.linkVolumes[8], 0.1); 
    				
    			} else if(this.cadytsWt == 0.0 && cadytsMgnWt == 150.){

    				Assert.assertEquals("wrong amount of bikes in short distance range in iteration " + iteration, 239, mgnHandler.bikesOnShortDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of bikes in medium distance range in iteration " + iteration, 215, mgnHandler.bikesOnMediumDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of bikes in long distance range in iteration " + iteration, 202, mgnHandler.bikesOnLongDistance, 0.1 );
    				
    				Assert.assertEquals("wrong amount of cars in short distance range in iteration " + iteration, 761, mgnHandler.carsOnShortDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of cars in medium distance range in iteration " + iteration, 785, mgnHandler.carsOnMediumDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of cars in long distance range in iteration " + iteration, 798, mgnHandler.carsOnLongDistance, 0.1 );
    				
    				Assert.assertEquals("wrong volume for 6am to 7am on link 2 in iteration " + iteration, 47, countsHandler.linkVolumes[0], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 3 in iteration " + iteration, 40, countsHandler.linkVolumes[1], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 4 in iteration " + iteration, 60, countsHandler.linkVolumes[2], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 5 in iteration " + iteration, 300, countsHandler.linkVolumes[3], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 6 in iteration " + iteration, 61, countsHandler.linkVolumes[4], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 7 in iteration " + iteration, 95, countsHandler.linkVolumes[5], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 8 in iteration " + iteration, 67, countsHandler.linkVolumes[6], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 9 in iteration " + iteration, 64, countsHandler.linkVolumes[7], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 10 in iteration " + iteration, 162, countsHandler.linkVolumes[8], 0.1); 
    				
    			} else if(this.cadytsWt == 150.0 && cadytsMgnWt == 150.0){

    				Assert.assertEquals("wrong amount of bikes in short distance range in iteration " + iteration, 243, mgnHandler.bikesOnShortDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of bikes in medium distance range in iteration " + iteration, 217, mgnHandler.bikesOnMediumDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of bikes in long distance range in iteration " + iteration, 199, mgnHandler.bikesOnLongDistance, 0.1 );
    				
    				Assert.assertEquals("wrong amount of cars in short distance range in iteration " + iteration, 757, mgnHandler.carsOnShortDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of cars in medium distance range in iteration " + iteration, 783, mgnHandler.carsOnMediumDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of cars in long distance range in iteration " + iteration, 801, mgnHandler.carsOnLongDistance, 0.1 );
    				
    				Assert.assertEquals("wrong volume for 6am to 7am on link 2 in iteration " + iteration, 45, countsHandler.linkVolumes[0], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 3 in iteration " + iteration, 37, countsHandler.linkVolumes[1], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 4 in iteration " + iteration, 53, countsHandler.linkVolumes[2], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 5 in iteration " + iteration, 300, countsHandler.linkVolumes[3], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 6 in iteration " + iteration, 62, countsHandler.linkVolumes[4], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 7 in iteration " + iteration, 103, countsHandler.linkVolumes[5], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 8 in iteration " + iteration, 63, countsHandler.linkVolumes[6], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 9 in iteration " + iteration, 70, countsHandler.linkVolumes[7], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 10 in iteration " + iteration, 161, countsHandler.linkVolumes[8], 0.1); 
    				
    			}
    			
    			mgnAbsoluteErrorSum = mgnHandler.absoluteErrorSum;
    			mgnRelativeAbsoluteErrorSum = mgnHandler.relativeAbsoluteErrorSum;
    			countsAbsoluteErrorSum = countsHandler.absoluteErrorSum;
    			countsRelativeAbsoluteErrorSum = countsHandler.relativeAbsoluteErrorSum;
    			break;
    			
    		case 15:
    			
    			Assert.assertTrue("the sum of relative absolute erros for marginals calibration should be less in iteration 15 than in iteration 10. old sum = " + mgnRelativeAbsoluteErrorSum + " new sum = " + mgnHandler.relativeAbsoluteErrorSum
    					, mgnHandler.relativeAbsoluteErrorSum < mgnRelativeAbsoluteErrorSum);	
    			Assert.assertTrue("the sum of absolute erros for marginals calibration should be less in iteration 15 than in iteration 10. old sum = " + mgnAbsoluteErrorSum + " new sum = " + mgnHandler.absoluteErrorSum,
    					mgnHandler.absoluteErrorSum < mgnAbsoluteErrorSum);
    			
    			if(this.cadytsWt == 150.0 && cadytsMgnWt == 0.0){
    				
    				Assert.assertEquals("wrong amount of bikes in short distance range in iteration " + iteration, 250, mgnHandler.bikesOnShortDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of bikes in medium distance range in iteration " + iteration, 236, mgnHandler.bikesOnMediumDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of bikes in long distance range in iteration " + iteration, 144, mgnHandler.bikesOnLongDistance, 0.1 );
    				
    				Assert.assertEquals("wrong amount of cars in short distance range in iteration " + iteration, 750, mgnHandler.carsOnShortDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of cars in medium distance range in iteration " + iteration, 764, mgnHandler.carsOnMediumDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of cars in long distance range in iteration " + iteration, 856, mgnHandler.carsOnLongDistance, 0.1 );
    				
    				Assert.assertEquals("wrong volume for 6am to 7am on link 2 in iteration " + iteration, 74, countsHandler.linkVolumes[0], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 3 in iteration " + iteration, 300, countsHandler.linkVolumes[1], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 4 in iteration " + iteration, 61, countsHandler.linkVolumes[2], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 5 in iteration " + iteration, 57, countsHandler.linkVolumes[3], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 6 in iteration " + iteration, 71, countsHandler.linkVolumes[4], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 7 in iteration " + iteration, 43, countsHandler.linkVolumes[5], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 8 in iteration " + iteration, 38, countsHandler.linkVolumes[6], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 9 in iteration " + iteration, 60, countsHandler.linkVolumes[7], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 10 in iteration " + iteration, 174, countsHandler.linkVolumes[8], 0.1); 
    				
    			} else if(this.cadytsWt == 0.0 && cadytsMgnWt == 150.0){ 
    				
    				Assert.assertEquals("wrong amount of bikes in short distance range in iteration " + iteration, 299, mgnHandler.bikesOnShortDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of bikes in medium distance range in iteration " + iteration, 251, mgnHandler.bikesOnMediumDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of bikes in long distance range in iteration " + iteration, 245, mgnHandler.bikesOnLongDistance, 0.1 );
    				
    				Assert.assertEquals("wrong amount of cars in short distance range in iteration " + iteration, 701, mgnHandler.carsOnShortDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of cars in medium distance range in iteration " + iteration, 749, mgnHandler.carsOnMediumDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of cars in long distance range in iteration " + iteration, 755, mgnHandler.carsOnLongDistance, 0.1 );
    				
    				Assert.assertEquals("wrong volume for 6am to 7am on link 2 in iteration " + iteration, 82, countsHandler.linkVolumes[0], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 3 in iteration " + iteration, 300, countsHandler.linkVolumes[1], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 4 in iteration " + iteration, 31, countsHandler.linkVolumes[2], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 5 in iteration " + iteration, 62, countsHandler.linkVolumes[3], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 6 in iteration " + iteration, 70, countsHandler.linkVolumes[4], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 7 in iteration " + iteration, 192, countsHandler.linkVolumes[5], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 8 in iteration " + iteration, 48, countsHandler.linkVolumes[6], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 9 in iteration " + iteration, 73, countsHandler.linkVolumes[7], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 10 in iteration " + iteration, 45, countsHandler.linkVolumes[8], 0.1); 
    				
    				
    			} else if(this.cadytsWt == 150.0 && cadytsMgnWt == 150.0){
    				
    				Assert.assertEquals("wrong amount of bikes in short distance range in iteration " + iteration, 303, mgnHandler.bikesOnShortDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of bikes in medium distance range in iteration " + iteration, 262, mgnHandler.bikesOnMediumDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of bikes in long distance range in iteration " + iteration, 240, mgnHandler.bikesOnLongDistance, 0.1 );
    				
    				Assert.assertEquals("wrong amount of cars in short distance range in iteration " + iteration, 697, mgnHandler.carsOnShortDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of cars in medium distance range in iteration " + iteration, 738, mgnHandler.carsOnMediumDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of cars in long distance range in iteration " + iteration, 760, mgnHandler.carsOnLongDistance, 0.1 );
    				
    				Assert.assertEquals("wrong volume for 6am to 7am on link 2 in iteration " + iteration, 77, countsHandler.linkVolumes[0], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 3 in iteration " + iteration, 68, countsHandler.linkVolumes[1], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 4 in iteration " + iteration, 40, countsHandler.linkVolumes[2], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 5 in iteration " + iteration, 207, countsHandler.linkVolumes[3], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 6 in iteration " + iteration, 300, countsHandler.linkVolumes[4], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 7 in iteration " + iteration, 53, countsHandler.linkVolumes[5], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 8 in iteration " + iteration, 70, countsHandler.linkVolumes[6], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 9 in iteration " + iteration, 50, countsHandler.linkVolumes[7], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 10 in iteration " + iteration, 51, countsHandler.linkVolumes[8], 0.1); 
    			}
    			
    			mgnAbsoluteErrorSum = mgnHandler.absoluteErrorSum;
    			mgnRelativeAbsoluteErrorSum = mgnHandler.relativeAbsoluteErrorSum;
    			countsAbsoluteErrorSum = countsHandler.absoluteErrorSum;
    			countsRelativeAbsoluteErrorSum = countsHandler.relativeAbsoluteErrorSum;
    			break;
    			
    		case 20:
    			
    			if(this.cadytsWt == 150.0 && cadytsMgnWt == 0.0){
    				Assert.assertEquals("wrong amount of bikes in short distance range in iteration " + iteration, 179, mgnHandler.bikesOnShortDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of bikes in medium distance range in iteration " + iteration, 166, mgnHandler.bikesOnMediumDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of bikes in long distance range in iteration " + iteration, 21, mgnHandler.bikesOnLongDistance, 0.1 );
    				
    				Assert.assertEquals("wrong amount of cars in short distance range in iteration " + iteration, 821, mgnHandler.carsOnShortDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of cars in medium distance range in iteration " + iteration, 834, mgnHandler.carsOnMediumDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of cars in long distance range in iteration " + iteration, 979, mgnHandler.carsOnLongDistance, 0.1 );
    				
    				Assert.assertEquals("wrong volume for 6am to 7am on link 2 in iteration " + iteration, 151, countsHandler.linkVolumes[0], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 3 in iteration " + iteration, 129, countsHandler.linkVolumes[1], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 4 in iteration " + iteration, 59, countsHandler.linkVolumes[2], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 5 in iteration " + iteration, 131, countsHandler.linkVolumes[3], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 6 in iteration " + iteration, 116, countsHandler.linkVolumes[4], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 7 in iteration " + iteration, 107, countsHandler.linkVolumes[5], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 8 in iteration " + iteration, 104, countsHandler.linkVolumes[6], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 9 in iteration " + iteration, 95, countsHandler.linkVolumes[7], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 10 in iteration " + iteration, 108, countsHandler.linkVolumes[8], 0.1); 
    				
    			} else if(this.cadytsWt == 0.0 && cadytsMgnWt == 150.0){
    				
    				Assert.assertEquals("wrong amount of bikes in short distance range in iteration " + iteration, 299, mgnHandler.bikesOnShortDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of bikes in medium distance range in iteration " + iteration, 247, mgnHandler.bikesOnMediumDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of bikes in long distance range in iteration " + iteration, 185, mgnHandler.bikesOnLongDistance, 0.1 );
    				
    				Assert.assertEquals("wrong amount of cars in short distance range in iteration " + iteration, 701, mgnHandler.carsOnShortDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of cars in medium distance range in iteration " + iteration, 753, mgnHandler.carsOnMediumDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of cars in long distance range in iteration " + iteration, 815, mgnHandler.carsOnLongDistance, 0.1 );
    				
    				
    				Assert.assertEquals("wrong volume for 6am to 7am on link 2 in iteration " + iteration, 137, countsHandler.linkVolumes[0], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 3 in iteration " + iteration, 137, countsHandler.linkVolumes[1], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 4 in iteration " + iteration, 58, countsHandler.linkVolumes[2], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 5 in iteration " + iteration, 120, countsHandler.linkVolumes[3], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 6 in iteration " + iteration, 158, countsHandler.linkVolumes[4], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 7 in iteration " + iteration, 93, countsHandler.linkVolumes[5], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 8 in iteration " + iteration, 61, countsHandler.linkVolumes[6], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 9 in iteration " + iteration, 148, countsHandler.linkVolumes[7], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 10 in iteration " + iteration, 88, countsHandler.linkVolumes[8], 0.1); 
    			
    			} else if(this.cadytsWt == 150.0 && cadytsMgnWt == 150.0){
    				
    				Assert.assertEquals("wrong amount of bikes in short distance range in iteration " + iteration, 281, mgnHandler.bikesOnShortDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of bikes in medium distance range in iteration " + iteration, 252, mgnHandler.bikesOnMediumDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of bikes in long distance range in iteration " + iteration, 192, mgnHandler.bikesOnLongDistance, 0.1 );
    				
    				Assert.assertEquals("wrong amount of cars in short distance range in iteration " + iteration, 719, mgnHandler.carsOnShortDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of cars in medium distance range in iteration " + iteration, 748, mgnHandler.carsOnMediumDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of cars in long distance range in iteration " + iteration, 808, mgnHandler.carsOnLongDistance, 0.1 );
    				
    				
    				Assert.assertEquals("wrong volume for 6am to 7am on link 2 in iteration " + iteration, 162, countsHandler.linkVolumes[0], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 3 in iteration " + iteration, 115, countsHandler.linkVolumes[1], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 4 in iteration " + iteration, 90, countsHandler.linkVolumes[2], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 5 in iteration " + iteration, 87, countsHandler.linkVolumes[3], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 6 in iteration " + iteration, 118, countsHandler.linkVolumes[4], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 7 in iteration " + iteration, 90, countsHandler.linkVolumes[5], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 8 in iteration " + iteration, 135, countsHandler.linkVolumes[6], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 9 in iteration " + iteration, 116, countsHandler.linkVolumes[7], 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 10 in iteration " + iteration, 87, countsHandler.linkVolumes[8], 0.1); 
    			}
    			
    			break;
    		}
    		countsHandler.reset();
    		mgnHandler.reset();
    	}
    }

	/**
	 * @param config
	 * @param controler
	 * @param counts
	 */
	private void installCadyts(Config config, Controler controler, Counts<Link> counts) {
		//controler.addOverridingModule(new CadytsCarModule(counts));
        final double cadytsCountsScoringWeight = this.cadytsWt * config.planCalcScore().getBrainExpBeta();

        final double cadytsMarginalsScoringWeight = this.cadytsMgnWt * config.planCalcScore().getBrainExpBeta();
        controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
           // @Inject
           // private CadytsContext cadytsContext;
            @Inject
            ScoringParametersForPerson parameters;

            @Inject private ModalDistanceCadytsContext marginalCadytsContext;

            @Override
            public ScoringFunction createNewScoringFunction(Person person) {
                SumScoringFunction sumScoringFunction = new SumScoringFunction();

                final ScoringParameters params = parameters.getScoringParameters(person);
                sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params,
                        controler.getScenario().getNetwork()));
                sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params));
                sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

             /*   final CadytsScoring<Link> scoringFunctionCounts = new CadytsScoring<Link>(person.getSelectedPlan(),
                        config,
                        cadytsContext);
                scoringFunctionCounts.setWeightOfCadytsCorrection(cadytsCountsScoringWeight);
*/
				final CadytsScoring<Id<DistanceDistribution.DistanceBin>> scoringFunctionMarginals = new CadytsScoring<>(person.getSelectedPlan(),
                            config,
                            marginalCadytsContext);

                scoringFunctionMarginals.setWeightOfCadytsCorrection(cadytsMarginalsScoringWeight);
                sumScoringFunction.addScoringFunction(scoringFunctionMarginals);

              //  sumScoringFunction.addScoringFunction(scoringFunctionCounts);

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

	private Config createConfig() {

		Config config = ConfigUtils.createConfig();
		String[] modes = new String[]{TransportMode.car, TransportMode.bike};

		config.controler().setOutputDirectory(this.utils.getOutputDirectory());
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(20);

		config.counts().setWriteCountsInterval(1);
		config.counts().setAverageCountsOverIterations(1);

		ActivityParams home = new ActivityParams("home");
		home.setMinimalDuration(6 * 3600);
		home.setTypicalDuration(6 * 3600);
		home.setEarliestEndTime(6 * 3600);
		config.planCalcScore().addActivityParams(home);

		ActivityParams work = new ActivityParams("work");
		work.setMinimalDuration(8 * 3600);
		work.setTypicalDuration(8 * 3600);
		work.setEarliestEndTime(14 * 3600);
		work.setOpeningTime(6 * 3600);
		work.setClosingTime(18 * 3600);
		config.planCalcScore().addActivityParams(work);

		// add mode choice to it
		/*StrategyConfigGroup.StrategySettings modeChoice = new StrategyConfigGroup.StrategySettings();
		modeChoice.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice);
		modeChoice.setWeight(0.25);
		config.strategy().addStrategySettings(modeChoice);
		config.subtourModeChoice().setModes(modes);

		 */

		// add Reroute choice to it
		/*StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings();
		reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
		reRoute.setWeight(0.25);
		config.strategy().addStrategySettings(reRoute);
		*/

		{
			StrategyConfigGroup.StrategySettings bestScore = new StrategyConfigGroup.StrategySettings();
			bestScore.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
			bestScore.setWeight(0.5);
			config.strategy().addStrategySettings(bestScore);
		}
		{
			StrategyConfigGroup.StrategySettings bestScore = new StrategyConfigGroup.StrategySettings();
			bestScore.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.SelectRandom);
			bestScore.setDisableAfter(17);
			bestScore.setWeight(0.5);
			config.strategy().addStrategySettings(bestScore);
		}


		// remove teleported bike
		config.plansCalcRoute().removeModeRoutingParams(TransportMode.bike);
		config.plansCalcRoute().setNetworkModes(Arrays.asList(modes));

		config.qsim().setMainModes(Arrays.asList(modes));
		config.qsim().setLinkDynamics(LinkDynamics.PassingQ);
		config.qsim().setTrafficDynamics(TrafficDynamics.kinematicWaves);

		config.qsim().setVehiclesSource(VehiclesSource.modeVehicleTypesFromVehiclesData);

		config.travelTimeCalculator().setAnalyzedModes(new HashSet<>((Arrays.asList(modes))));
		config.travelTimeCalculator().setSeparateModes(true);
		config.travelTimeCalculator().setFilterModes(true);
		config.changeMode().setModes(modes);
		config.changeMode().setBehavior(Behavior.fromSpecifiedModesToSpecifiedModes);
		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);

		return config;
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
        
        
        ActivityParams home = new ActivityParams("h");
        home.setMinimalDuration(6*3600);
        home.setTypicalDuration(6*3600);
        home.setEarliestEndTime(6*3600);
        config.planCalcScore().addActivityParams(home);


        ActivityParams work = new ActivityParams("w");
        work.setMinimalDuration(8*3600);
        work.setTypicalDuration(8*3600);
        work.setEarliestEndTime(14*3600);
        work.setOpeningTime(6*3600);
        work.setClosingTime(18*3600);
        config.planCalcScore().addActivityParams(work);
        
        ActivityParams leisure = new ActivityParams("l");
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
        config.qsim().setLinkDynamics(LinkDynamics.PassingQ);
        config.qsim().setTrafficDynamics(TrafficDynamics.kinematicWaves);
        
        config.qsim().setVehiclesSource(VehiclesSource.modeVehicleTypesFromVehiclesData);
        config.vehicles().setVehiclesFile(EQUIL_DIR + "mode-vehicles.xml");
		config.travelTimeCalculator().setAnalyzedModes(new HashSet<>((Arrays.asList(modes))));
        config.travelTimeCalculator().setSeparateModes(true);
        config.travelTimeCalculator().setFilterModes(true);
        config.changeMode().setModes(modes);
        config.changeMode().setBehavior(Behavior.fromSpecifiedModesToSpecifiedModes);
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
    

    private DistanceDistribution getInputDistanceDistribution(double beelineDistanceFactorForNetworkModes){
		DistanceDistribution inputDistanceDistribution = new DistanceDistribution(beelineDistanceFactorForNetworkModes);

        //leg leisure to h = 10.000km 
		inputDistanceDistribution.add("car", 0.0, 10000.1, 10000, 200);
		inputDistanceDistribution.add("bicycle", 0.0, 10000.1, 10000, 800);

        //h1 to work = 20.000km
		inputDistanceDistribution.add("car", 10000.1, 20000.1, 10000, 500);
		inputDistanceDistribution.add("bicycle", 10000.1, 20000.1, 10000, 500);

        //work to leisure = ~ 27.000 km
		inputDistanceDistribution.add("car", 20000.1, 50000.0, 10000, 800);
		inputDistanceDistribution.add("bicycle", 20000.1, 50000.0, 10000, 200);

//        //anything above
//        inputDistanceDistribution.addToDistribution("car", new DistanceBin.DistanceRange(50000.0,90000.),80);
//        inputDistanceDistribution.addToDistribution("bicycle", new DistanceBin.DistanceRange(50000.0,90000.),20);

        return inputDistanceDistribution;
    }
    
    	
}

class TbfHandlerMarginals implements TabularFileHandler{
	boolean header = true;
	
	double bikesOnShortDistance;
	double bikesOnMediumDistance;
	double bikesOnLongDistance;
	double carsOnShortDistance;
	double carsOnMediumDistance;
	double carsOnLongDistance;
	
	double absoluteErrorSum;
	double relativeAbsoluteErrorSum;
	
	@Override
	public void startRow(String[] row) {
		if(!header) {
			absoluteErrorSum += Double.parseDouble(row[10]);
			double relativeAbsoluteError = Double.parseDouble(row[12]);
			if(relativeAbsoluteError != Double.NaN)	relativeAbsoluteErrorSum += relativeAbsoluteError;
			
			if(row[0].contains("mode='bicycle', distanceRange=DistanceRange[lowerLimit=0.0, upperLimit=10000.1]")) {
				bikesOnShortDistance = Double.parseDouble(row[6]);
			} else if(row[0].contains("mode='bicycle', distanceRange=DistanceRange[lowerLimit=10000.1, upperLimit=20000.1]")){
				bikesOnMediumDistance = Double.parseDouble(row[6]);
			} else if(row[0].contains("mode='bicycle', distanceRange=DistanceRange[lowerLimit=20000.1, upperLimit=50000.0]")){
				bikesOnLongDistance = Double.parseDouble(row[6]);
			} else if(row[0].contains("mode='car', distanceRange=DistanceRange[lowerLimit=0.0, upperLimit=10000.1]")) {
				carsOnShortDistance = Double.parseDouble(row[6]);
			} else if(row[0].contains("mode='car', distanceRange=DistanceRange[lowerLimit=10000.1, upperLimit=20000.1]")){
				carsOnMediumDistance= Double.parseDouble(row[6]);
			} else if(row[0].contains("mode='car', distanceRange=DistanceRange[lowerLimit=20000.1, upperLimit=50000.0]")){
				carsOnLongDistance = Double.parseDouble(row[6]);
			}
			
		} else {
			header = false;
		}
		
	}
	
	void reset() {
		header = true;
		bikesOnShortDistance = Integer.MAX_VALUE;
		bikesOnMediumDistance = Integer.MAX_VALUE;
		bikesOnLongDistance = Integer.MAX_VALUE;
		carsOnShortDistance = Integer.MAX_VALUE;
		carsOnMediumDistance = Integer.MAX_VALUE;
		carsOnLongDistance = Integer.MAX_VALUE;
		
		absoluteErrorSum = 0;
		relativeAbsoluteErrorSum = 0;
	}
}
	
class TbfHandlerCounts implements TabularFileHandler {
	boolean header = true;
	
	double absoluteErrorSum ;
	double relativeAbsoluteErrorSum ;
	
	double[] linkVolumes = new double[9];
	
	@Override
	public void startRow(String[] row) {
		if(!header) {
			absoluteErrorSum += Double.parseDouble(row[10]);
			double relativeAbsoluteError = Double.parseDouble(row[12]);
			if(!Double.isNaN(relativeAbsoluteError) && Double.isFinite(relativeAbsoluteError))	relativeAbsoluteErrorSum += relativeAbsoluteError;
			
			if(row[1].equals("06:00:00")) {
				if(row[0].contains("[id=10]")) {
					linkVolumes [8] = Double.parseDouble(row[6]);
				} else if(row[0].contains("[id=2]")){
					linkVolumes [0] = Double.parseDouble(row[6]);
				} else if(row[0].contains("[id=3]")){
					linkVolumes [1] = Double.parseDouble(row[6]);
				} else if(row[0].contains("[id=4]")){
					linkVolumes [2] = Double.parseDouble(row[6]);
				} else if(row[0].contains("[id=5]")){
					linkVolumes [3] = Double.parseDouble(row[6]);
				} else if(row[0].contains("[id=6]")){
					linkVolumes [4] = Double.parseDouble(row[6]);
				} else if(row[0].contains("[id=7]")){
					linkVolumes [5] = Double.parseDouble(row[6]);
				} else if(row[0].contains("[id=8]")){
					linkVolumes [6] = Double.parseDouble(row[6]);
				} else if(row[0].contains("[id=9]")){
					linkVolumes [7] = Double.parseDouble(row[6]);
				}	
			}
		} else {
			header = false;
		}
		
	}
	
	void reset() {
		header = true;
		absoluteErrorSum = 0;
		relativeAbsoluteErrorSum = 0;
		linkVolumes = new double[9];		
	}
	
}
