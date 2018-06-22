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

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.cadyts.car.CadytsCarModule;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ChangeModeConfigGroup.Behavior;
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
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import org.junit.Assert;
import playground.vsp.cadyts.marginals.ModalDistanceCadytsContext;
import playground.vsp.cadyts.marginals.ModalDistanceCadytsModule;
import playground.vsp.cadyts.marginals.RunExample;
import playground.vsp.cadyts.marginals.prep.DistanceBin;
import playground.vsp.cadyts.marginals.prep.DistanceDistribution;
import playground.vsp.cadyts.marginals.prep.ModalDistanceBinIdentifier;

@RunWith(Parameterized.class)
public class CountsAndModalDistanceCadytsIT {
	
	private double cadytsWt ;
    private double cadytsMgnWt;

    private static final URL EQUIL_DIR = ExamplesUtils.getTestScenarioURL("equil-mixedTraffic");

    public CountsAndModalDistanceCadytsIT(double cadytsWt, double mgnWt) {
    	  this.cadytsWt = cadytsWt;
          this.cadytsMgnWt = mgnWt;
    }
    
    @Parameterized.Parameters(name = "{index}: cadytsWeight == {0};")
    public static Collection<Object[]> parameterObjects () {
        return Arrays.asList(new Object[][] { {150.0, 0.0} ,  {0.0, 150.0}});
    }
    
    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    
    /*
     * as a start i'm naming this nemoModeLocationChoiceTest
     * since this is where functionality comes from
     * tschlenther june '18 
     */
    @Test
    public final void nemoModeLocationChoiceTest() {
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
        controler.addOverridingModule(new ModalDistanceCadytsModule(inputDistanceDistribution));


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
    			if(this.cadytsWt == 150.0) {
    				
    				Assert.assertEquals("wrong amount of bikes in short distance range in iteration " + iteration, 171, mgnHandler.bikesOnShortDistance, 0.1);	
    				Assert.assertEquals("wrong amount of bikes in medium distance range in iteration " + iteration, 134, mgnHandler.bikesOnMediumDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of bikes in long distance range in iteration " + iteration, 142, mgnHandler.bikesOnLongDistance, 0.1 );
    				
    				Assert.assertEquals("wrong amount of cars in short distance range in iteration " + iteration, 829, mgnHandler.carsOnShortDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of cars in medium distance range in iteration " + iteration, 866, mgnHandler.carsOnMediumDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of cars in long distance range in iteration " + iteration, 858, mgnHandler.carsOnLongDistance, 0.1 );	
    				
    				Assert.assertEquals("wrong volume for 6am to 7am on link 2 in iteration " + iteration, 145, countsHandler.volumeOnLink2, 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 10 in iteration " + iteration, 0, countsHandler.volumeOnLink10, 0.1); 
    			} else {
    				
//    				Assert.assertEquals("wrong amount of bikes in short distance range in iteration " + iteration, 171, mgnHandler.bikesOnShortDistance );	
//    				Assert.assertEquals("wrong amount of bikes in medium distance range in iteration " + iteration, 134, mgnHandler.bikesOnMediumDistance );	
//    				Assert.assertEquals("wrong amount of bikes in long distance range in iteration " + iteration, 142, mgnHandler.bikesOnLongDistance );
//    				
//    				Assert.assertEquals("wrong amount of cars in short distance range in iteration " + iteration, 829, mgnHandler.bikesOnShortDistance );	
//    				Assert.assertEquals("wrong amount of cars in medium distance range in iteration " + iteration, 866, mgnHandler.bikesOnMediumDistance );	
//    				Assert.assertEquals("wrong amount of cars in long distance range in iteration " + iteration, 858, mgnHandler.bikesOnLongDistance );
    			}
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
    			if(this.cadytsWt == 150.0) {
    				
    				Assert.assertEquals("wrong amount of bikes in short distance range in iteration " + iteration, 211, mgnHandler.bikesOnShortDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of bikes in medium distance range in iteration " + iteration, 181, mgnHandler.bikesOnMediumDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of bikes in long distance range in iteration " + iteration, 130, mgnHandler.bikesOnLongDistance, 0.1 );
    				
    				Assert.assertEquals("wrong amount of cars in short distance range in iteration " + iteration, 789, mgnHandler.carsOnShortDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of cars in medium distance range in iteration " + iteration, 819, mgnHandler.carsOnMediumDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of cars in long distance range in iteration " + iteration, 870, mgnHandler.carsOnLongDistance, 0.1 );
    				
    				Assert.assertEquals("wrong volume for 6am to 7am on link 2 in iteration " + iteration, 103, countsHandler.volumeOnLink2, 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 10 in iteration " + iteration, 133, countsHandler.volumeOnLink10, 0.1); 
    			} else {
    				
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
//    			Assert.assertTrue("the sum of relative absolute erros for counts calibration should be less in iteration 15 than in iteration 10. old sum = " + countsRelativeAbsoluteErrorSum + " new sum = " + countsHandler.relativeAbsoluteErrorSum,
//    					countsHandler.relativeAbsoluteErrorSum < countsRelativeAbsoluteErrorSum);	
//    			Assert.assertTrue("the sum of absolute erros for counts calibration should be less in iteration 15 than in iteration 10.  old sum = " + countsAbsoluteErrorSum + " new sum = " + countsHandler.absoluteErrorSum,
//    					countsHandler.absoluteErrorSum < countsAbsoluteErrorSum);
    			if(this.cadytsWt == 150.0) {
    				Assert.assertEquals("wrong amount of bikes in short distance range in iteration " + iteration, 248, mgnHandler.bikesOnShortDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of bikes in medium distance range in iteration " + iteration, 251, mgnHandler.bikesOnMediumDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of bikes in long distance range in iteration " + iteration, 181, mgnHandler.bikesOnLongDistance, 0.1 );
    				
    				Assert.assertEquals("wrong amount of cars in short distance range in iteration " + iteration, 752, mgnHandler.carsOnShortDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of cars in medium distance range in iteration " + iteration, 749, mgnHandler.carsOnMediumDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of cars in long distance range in iteration " + iteration, 819, mgnHandler.carsOnLongDistance, 0.1 );
    				
    				Assert.assertEquals("wrong volume for 6am to 7am on link 2 in iteration " + iteration, 37, countsHandler.volumeOnLink2, 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 10 in iteration " + iteration, 89, countsHandler.volumeOnLink10, 0.1); 
    			} else {
    				
    			}
    			mgnAbsoluteErrorSum = mgnHandler.absoluteErrorSum;
    			mgnRelativeAbsoluteErrorSum = mgnHandler.relativeAbsoluteErrorSum;
    			countsAbsoluteErrorSum = countsHandler.absoluteErrorSum;
    			countsRelativeAbsoluteErrorSum = countsHandler.relativeAbsoluteErrorSum;
    			break;
    		case 20:
    			if(this.cadytsWt == 150.0) {
    				Assert.assertEquals("wrong amount of bikes in short distance range in iteration " + iteration, 170, mgnHandler.bikesOnShortDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of bikes in medium distance range in iteration " + iteration, 169, mgnHandler.bikesOnMediumDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of bikes in long distance range in iteration " + iteration, 24, mgnHandler.bikesOnLongDistance, 0.1 );
    				
    				Assert.assertEquals("wrong amount of cars in short distance range in iteration " + iteration, 830, mgnHandler.carsOnShortDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of cars in medium distance range in iteration " + iteration, 831, mgnHandler.carsOnMediumDistance, 0.1 );	
    				Assert.assertEquals("wrong amount of cars in long distance range in iteration " + iteration, 976, mgnHandler.carsOnLongDistance, 0.1 );
    				
    				Assert.assertEquals("wrong volume for 6am to 7am on link 2 in iteration " + iteration, 114, countsHandler.volumeOnLink2, 0.1);
    				Assert.assertEquals("wrong volume for 6am to 7am on link 10 in iteration " + iteration, 151, countsHandler.volumeOnLink10, 0.1);
    				
    			} else {
    				
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
		controler.addOverridingModule(new CadytsCarModule(counts));
        final double cadytsCountsScoringWeight = this.cadytsWt * config.planCalcScore().getBrainExpBeta();

        final double cadytsMarginalsScoringWeight = this.cadytsMgnWt * config.planCalcScore().getBrainExpBeta();
        controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
            @Inject
            private CadytsContext cadytsContext;
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

                final CadytsScoring<Link> scoringFunctionCounts = new CadytsScoring<Link>(person.getSelectedPlan(),
                        config,
                        cadytsContext);
                scoringFunctionCounts.setWeightOfCadytsCorrection(cadytsCountsScoringWeight);

                final CadytsScoring<ModalDistanceBinIdentifier> scoringFunctionMarginals = new CadytsScoring<>(person.getSelectedPlan(),
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
        
        config.strategy().addParam("ModuleProbability_1", "0.5");
        config.strategy().addParam("Module_1", "BestScore");
        
        SubtourModeChoiceConfigGroup sbtModeChoiceConfgGroup = config.subtourModeChoice();
        String[] modes = new String[]{"car", "bicycle"};
       
        sbtModeChoiceConfgGroup.setModes(modes);
        config.plansCalcRoute().setNetworkModes(Arrays.asList(modes));
        config.qsim().setMainModes(Arrays.asList(modes));
        config.qsim().setLinkDynamics(LinkDynamics.PassingQ);
        config.qsim().setTrafficDynamics(TrafficDynamics.kinematicWaves);
        
        config.qsim().setVehiclesSource(VehiclesSource.modeVehicleTypesFromVehiclesData);
        config.vehicles().setVehiclesFile(EQUIL_DIR + "mode-vehicles.xml");
        
        config.travelTimeCalculator().setAnalyzedModes("car,bicycle");
        config.travelTimeCalculator().setSeparateModes(true);
        config.travelTimeCalculator().setFilterModes(true);
        config.changeMode().setModes(modes);
        config.changeMode().setBehavior(Behavior.fromSpecifiedModesToSpecifiedModes);
        config.strategy().setFractionOfIterationsToDisableInnovation(0.8);

        config.planCalcScore().getOrCreateModeParams("car").setConstant(-2);
        config.planCalcScore().getOrCreateModeParams("bicycle");
        
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
    	c8.createVolume(7, 500);
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
        DistanceDistribution inputDistanceDistribution = new DistanceDistribution();
                
        inputDistanceDistribution.setBeelineDistanceFactorForNetworkModes("car",beelineDistanceFactorForNetworkModes);
        inputDistanceDistribution.setBeelineDistanceFactorForNetworkModes("bicycle",beelineDistanceFactorForNetworkModes);
        
        inputDistanceDistribution.setModeToScalingFactor("car",1);
        inputDistanceDistribution.setModeToScalingFactor("bicycle",1);

        //leg leisure to h = 10.000km 
        inputDistanceDistribution.addToDistribution("car", new DistanceBin.DistanceRange(0.0,10000.1),200);
        inputDistanceDistribution.addToDistribution("bicycle", new DistanceBin.DistanceRange(0.0,10000.1),800);

        //h1 to work = 20.000km
        inputDistanceDistribution.addToDistribution("car", new DistanceBin.DistanceRange(10000.1,20000.1),500);
        inputDistanceDistribution.addToDistribution("bicycle", new DistanceBin.DistanceRange(10000.1,20000.1),500);

        //work to leisure = ~ 27.000 km
        inputDistanceDistribution.addToDistribution("car", new DistanceBin.DistanceRange(20000.1,50000.0),800);
        inputDistanceDistribution.addToDistribution("bicycle", new DistanceBin.DistanceRange(20000.1,50000.0),200);

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
	
	double volumeOnLink2;
	double volumeOnLink10;
	
	@Override
	public void startRow(String[] row) {
		if(!header) {
			absoluteErrorSum += Double.parseDouble(row[10]);
			double relativeAbsoluteError = Double.parseDouble(row[12]);
			if(!Double.isNaN(relativeAbsoluteError) && Double.isFinite(relativeAbsoluteError))	relativeAbsoluteErrorSum += relativeAbsoluteError;
			
			if(row[1].equals("06:00:00")) {
				if(row[0].contains("[id=10]")) {
					volumeOnLink10 = Double.parseDouble(row[6]);
				}
				else if(row[0].contains("[id=2]")){
					volumeOnLink2 = Double.parseDouble(row[6]);
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
		volumeOnLink2 = Integer.MAX_VALUE;
		volumeOnLink10 = Integer.MAX_VALUE;
	}
	
}
