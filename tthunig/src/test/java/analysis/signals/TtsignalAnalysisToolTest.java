package analysis.signals;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.analysis.SignalAnalysisTool;
import org.matsim.contrib.signals.binder.SignalsModule;
import org.matsim.contrib.signals.controller.fixedTime.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataFactoryImpl;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlDataFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactoryImpl;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.utils.SignalUtils;

/**
 * @author sbraun
 */


public class TtsignalAnalysisToolTest {

	private static Id<Link> LINK_ID12 = Id.create("1_2", Link.class);
	private static Id<Link> LINK_ID23 = Id.create("2_3", Link.class);
	private static Id<Link> LINK_ID34 = Id.create("3_4", Link.class);
	private static Id<Link> LINK_ID45 = Id.create("4_5", Link.class);
	private static Id<SignalGroup> SIGNALGROUP = Id.create("SignalGroup-2_3", SignalGroup.class);
	private static Id<SignalSystem> SIGNALSYSTEM = Id.create("SignalSystem-3", SignalSystem.class);

	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
		
	@Test
	public void testTotalSignalGreenTime() {
		ScenarioForTest testscenario = new ScenarioForTest(0.0,10060.);
		SignalAnalysisTool signalAnalysishandler = testscenario.prepareTest();		
		double totalgreentime = ((Double)signalAnalysishandler.getTotalSignalGreenTime().get(SIGNALGROUP)).doubleValue();
			
		Assert.assertEquals("The total green time of SignalGroup 2-3",1560.,totalgreentime, MatsimTestUtils.EPSILON); 		
	}

	@Test
	public void testSumBygoneGreenTime() {
		ScenarioForTest testscenario = new ScenarioForTest(0.0,10000.);
		SignalAnalysisTool signalAnalysishandler = testscenario.prepareTest();		
		
		double greenAt51 = signalAnalysishandler.getSumOfBygoneSignalGreenTime().get(new Double(51)).get(SIGNALGROUP).doubleValue();
		double greenAt61 = signalAnalysishandler.getSumOfBygoneSignalGreenTime().get(new Double(61)).get(SIGNALGROUP).doubleValue();
		double greenAt121 = signalAnalysishandler.getSumOfBygoneSignalGreenTime().get(new Double(121)).get(SIGNALGROUP).doubleValue();
			
		Assert.assertEquals("The at second 51", 51.,greenAt51, MatsimTestUtils.EPSILON);
		Assert.assertEquals("The at second 61", 60.,greenAt61, MatsimTestUtils.EPSILON);
		Assert.assertEquals("The at second 121", 61.,greenAt121, MatsimTestUtils.EPSILON);		
	}	

	@Test
	public void testAvgGreenCycleperSignalGroup() {
		ScenarioForTest testscenario = new ScenarioForTest(0.0,0.0);
		SignalAnalysisTool signalAnalysishandler = testscenario.prepareTest();		
		
		double avgGreenCycle = signalAnalysishandler.calculateAvgFlexibleCycleTimePerSignalSystem().get(SIGNALSYSTEM).doubleValue();
		
		Assert.assertEquals("The average Greencycle should be 115.55555", 115.5555555555555,avgGreenCycle, MatsimTestUtils.EPSILON);		
	}
	
//TODO Is this doing what it suppose to do?? 
	@Test
	public void testSignalGreenTimeRatios() {
		ScenarioForTest testscenario = new ScenarioForTest(0.0,120);
		SignalAnalysisTool signalAnalysishandler = testscenario.prepareTest();		
		
		double greenRatio = signalAnalysishandler.calculateSignalGreenTimeRatios().get(SIGNALGROUP).doubleValue();
		Assert.assertEquals("Ratio of Greentime to relevant simulation time", 0.018939393939, greenRatio, MatsimTestUtils.EPSILON);		
	}
	
	@Test
	public void testcalculateAvgSignalGreenTimePerCycle() {
		ScenarioForTest testscenario = new ScenarioForTest(0.0,240.);
		SignalAnalysisTool signalAnalysishandler = testscenario.prepareTest();		
		
		double avggreentime = signalAnalysishandler.calculateAvgSignalGreenTimePerFlexibleCycle().get(SIGNALGROUP).doubleValue();
		Assert.assertEquals("The average Green time per flexible cycle should be ", 60. , avggreentime , MatsimTestUtils.EPSILON);		
	}
	
	
//Preparation for tests		
	private class ScenarioForTest{
		private Double planStartTime;
		private Double planEndTime;		
		private Scenario scenario;
		private int populationsize = 4;
		private int offsets = 1000;
		
		ScenarioForTest(double planStartTime, double planEndTime){
			this.planStartTime = new Double(planStartTime);
			this.planEndTime = new Double(planEndTime);
		}
			
		SignalAnalysisTool prepareTest() {
			Config config = createConfig();
			createScenarioElem(config);
			
			EventsManager eventsManager = EventsUtils.createEventsManager();		
			SignalAnalysisTool signalAnalysishandler = new SignalAnalysisTool();
						
			eventsManager.addHandler(signalAnalysishandler);
			
			Controler controler = new Controler(scenario);
			controler.addOverridingModule(new SignalsModule());
			
			// add signal analysis handler
			controler.addOverridingModule(new AbstractModule() {			
				@Override
				public void install() {
					this.addEventHandlerBinding().toInstance(signalAnalysishandler);
				}
			});
			
			
			controler.run();
			return signalAnalysishandler;
		}
			
		private Config createConfig() {
			Config config = ConfigUtils.createConfig();
			config.controler().setOutputDirectory(testUtils.getOutputDirectory());
			
			config.controler().setLastIteration(0);
			
			config.qsim().setStartTime(0);
	        config.qsim().setUsingFastCapacityUpdate(false);
		
			// able or enable signals and lanes
			SignalSystemsConfigGroup signalConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
			signalConfigGroup.setUseSignalSystems( true );
			
			
			ActivityParams dummyAct = new ActivityParams("dummy");
			dummyAct.setTypicalDuration(10);
			config.planCalcScore().addActivityParams(dummyAct);
			
			
			
			config.qsim().setStuckTime( 3600 );
			config.qsim().setRemoveStuckVehicles(false);
		
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
			config.vspExperimental().setWritingOutputEvents(true);
			config.planCalcScore().setWriteExperiencedPlans(false);
			config.controler().setCreateGraphs(false);
			config.controler().setDumpDataAtEnd(false);
			config.controler().setWriteEventsInterval(config.controler().getLastIteration());
			config.controler().setWritePlansInterval(config.controler().getLastIteration());
		
			
			return config;
		}
		
		private void createScenarioElem(Config config) {
			scenario = ScenarioUtils.loadScenario(config);
			scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
			createNetwork();
			createPopulation(populationsize,offsets);
			createSignals();
		}
		
		//Network which looks like this:
		//  1/5---->2
		//   ^      |
		//   |      V
		//   4<-----3	
		private void createNetwork() {
			Network network = scenario.getNetwork();
			NetworkFactory nfac = network.getFactory();
			
			double traveltime = 1000; 
			double freespeed = 10;
			double length = traveltime*freespeed;
		
			Node node1 = nfac.createNode(Id.createNodeId("1"), CoordUtils.createCoord(0., 100.));
			Node node2 = nfac.createNode(Id.createNodeId("2"), CoordUtils.createCoord(100., 100.));
			Node node3 = nfac.createNode(Id.createNodeId("3"), CoordUtils.createCoord(100., 0.));
			Node node4 = nfac.createNode(Id.createNodeId("4"), CoordUtils.createCoord(0., 0.));
			Node node5 = nfac.createNode(Id.createNodeId("5"), CoordUtils.createCoord(0., 100.));
		
			network.addNode(node1);
			network.addNode(node2);
			network.addNode(node3);
			network.addNode(node4);
			network.addNode(node5);
		
			Link link12 = nfac.createLink((LINK_ID12), node1, node2);
			link12.setCapacity(100.);
			link12.setLength(length);
			link12.setFreespeed(freespeed);
			network.addLink(link12);
		
			Link link23 = nfac.createLink((LINK_ID23), node2, node3);
			link23.setCapacity(100.);
			link23.setLength(length);
			link23.setFreespeed(50*3.6);
			network.addLink(link23);
		
			Link link34 = nfac.createLink((LINK_ID34), node3, node4);
			link34.setCapacity(100.);
			link34.setLength(length);
			link34.setFreespeed(50*3.6);
			network.addLink(link34);
		
			Link link45 = nfac.createLink((LINK_ID45), node4, node5);
			link45.setCapacity(100.);
			link45.setLength(length);
			link45.setFreespeed(50*3.6);
			network.addLink(link45);
		}
	
		private void createPopulation(int persons, int offsets) {
			Population pop = scenario.getPopulation();
			PopulationFactory pfac = pop.getFactory();
		
		
			for (int i=0;i<persons;i++) {
				Activity start = pfac.createActivityFromLinkId("dummy", LINK_ID12);
				Activity end = pfac.createActivityFromLinkId("dummy", LINK_ID45);			
				Leg leg = pfac.createLeg(TransportMode.car);

		
				Person person = pfac.createPerson(Id.createPersonId("1-2-3-4-5_"+(i+1)));
				Plan plan = pfac.createPlan();			
				start.setEndTime((double) i*offsets);			
				plan.addActivity(start);
				plan.addLeg(leg);
				plan.addActivity(end);			
				person.addPlan(plan);
				pop.addPerson(person);
			}
		
		}	
	
		private void createSignals() {
			SignalsData signalsdata = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
			SignalSystemsData signalSystems = signalsdata.getSignalSystemsData();
			SignalSystemsDataFactory sysfac = new SignalSystemsDataFactoryImpl();
			SignalGroupsData signalGroups = signalsdata.getSignalGroupsData();
			SignalControlData signalControl = signalsdata.getSignalControlData();
			SignalControlDataFactory conFac = new SignalControlDataFactoryImpl();
			
			//Signal at Node 3
			Id<SignalSystem> signalSystemId = SIGNALSYSTEM;
			SignalSystemData signalSystem = sysfac.createSignalSystemData(signalSystemId);
			signalSystems.addSignalSystemData(signalSystem);
			
			// create a signal for Link 2_3
			SignalData signal = sysfac.createSignalData(Id.create("Signal-2_3", Signal.class));
			signalSystem.addSignalData(signal);
			signal.setLinkId(LINK_ID23);
			
			// create an one element group for the signal
			Id<SignalGroup> signalGroupId1 = SIGNALGROUP;
			SignalGroupData signalGroup1 = signalGroups.getFactory().createSignalGroupData(signalSystemId, signalGroupId1);
			signalGroup1.addSignalId(Id.create("Signal-2_3", Signal.class));
			signalGroups.addSignalGroupData(signalGroup1);
			
			// create the signal control
			SignalSystemControllerData signalSystemControl = conFac.createSignalSystemControllerData(signalSystemId);
			signalSystemControl.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
			signalControl.addSignalSystemControllerData(signalSystemControl);
	
			// create a first plan for the signal system (with cycle time 120)
			SignalPlanData signalPlan = SignalUtils.createSignalPlan(conFac, 120, 0, Id.create("SignalPlan", SignalPlan.class));
			if (planStartTime != null) signalPlan.setStartTime(planStartTime);
			if (planEndTime != null) signalPlan.setEndTime(planEndTime);
			signalSystemControl.addSignalPlanData(signalPlan);
			signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, signalGroupId1, 0, 60));
			signalPlan.setOffset(0);
		}
	
	}
}
