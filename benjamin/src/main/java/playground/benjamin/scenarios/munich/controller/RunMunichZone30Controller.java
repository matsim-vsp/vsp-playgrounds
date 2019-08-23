/* *********************************************************************** *
 * project: kai
 * GautengOwnController.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.benjamin.scenarios.munich.controller;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

/**
 * @author benjamin after nagel
 *
 */
public class RunMunichZone30Controller {
	public static Logger logger = Logger.getLogger(RunMunichZone30Controller.class);

	private final Config config;
	private final Scenario scenario;	

	private EventsManager eventsManager;
	
	private Population population  ;
	
	private TravelTimeCalculator travelTime;
	
	public static final String message = "RunMunichZone30Controller class is written in a dialect that " +
								   "existed for a short time, but was " +
								   "then superseeded by controler injection.  In consequence, this class here would " +
								   "need to be re-written.  Since it is not covered by a test, I can't/don't want to " +
								   "do this.  kai, mar'18";

	public RunMunichZone30Controller(Scenario sc) {
		this.scenario = sc;
		this.config = sc.getConfig();
		throw new RuntimeException(message) ;
	}

//	public void run() {
//		this.config.addConfigConsistencyChecker(new ConfigConsistencyCheckerImpl());
//		ControlerUtils.checkConfigConsistencyAndWriteToLog(this.config, "Complete config dump after reading the config file:");
//		this.setupOutputDirectory(config.controler().getOutputDirectory(), config.controler().getRunId(),
//				OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
//		//FIXME: Line above does not work with MATSIM 0.11 snapshot.
//		Network network = this.scenario.getNetwork();
//		this.population = this.scenario.getPopulation();
//		this.eventsManager = EventsUtils.createEventsManager(config);
//		// add a couple of useful event handlers:
//		this.eventsManager.addHandler(new VolumesAnalyzer(3600, 24 * 3600 - 1, network));
//		CalcLegTimes legTimes = new CalcLegTimes();
//		this.eventsManager.addHandler(legTimes);
//		this.travelTime = TravelTimeCalculator.create(network, this.config.travelTimeCalculator());
//		this.eventsManager.addHandler(travelTime);
//		super.run(config);
//	}
//
//	/**
//	 * The order how the listeners are added may be important! As
//	 * dependencies between different listeners exist or listeners may read
//	 * and write to common variables, the order is important.
//	 * <br>
//	 * The example given in the old Controler was: The
//	 * RoadPricing-Listener modifies the scoringFunctionFactory, which in
//	 * turn is used by the PlansScoring-Listener. I would argue that such dependencies are not necessary with the
//	 * code as designed her: One could first define the scoring function completely, and then add it where needed. kai, jun'12
//	 * <br>
//	 * IMPORTANT: The execution order is reverse to the order the listeners
//	 * are added to the list.
//	 */
//	@Override
//	protected void loadCoreListeners() {
//
////		final DumpDataAtEnd dumpDataAtEnd = new DumpDataAtEndImpl(scenario, getControlerIO());
////		this.addControlerListener(dumpDataAtEnd);
//
//		final PlansScoring plansScoring = buildPlansScoring();
//		this.addControlerListener(plansScoring);
//
//		final StrategyManager strategyManager = buildStrategyManager() ;
////		this.addCoreControlerListener(new PlansReplanningImpl( strategyManager, this.population ));
//
////		final PlansDumping plansDumping = new PlansDumpingImpl( this.scenario, this.config.controler().getFirstIteration(),
////				this.config.controler().getWritePlansInterval(), stopwatch, getControlerIO() );
////		this.addCoreControlerListener(plansDumping);
//
////		this.addCoreControlerListener(new LegTimesControlerListener(legTimes, getControlerIO()));
////		final EventsHandling eventsHandling = new EventsHandlingImpl((EventsManagerImpl) eventsManager,
////				this.config.services().getWriteEventsInterval(), this.config.services().getEventsFileFormats(),
////				getControlerIO() );
////		this.addCoreControlerListener(eventsHandling);
//		// must be last being added (=first being executed)
//		throw new RuntimeException("This doesn't work anymore. Come to MZ, who will gladly help you repair it.");
//	}
//	private PlansScoring buildPlansScoring() {
//		ScoringFunctionFactory scoringFunctionFactory = new CharyparNagelScoringFunctionFactory( scenario );
////		final PlansScoring plansScoring = new PlansScoringImpl( this.scenario, this.eventsManager, getControlerIO(), scoringFunctionFactory );
//		return null;
//
//	}
//
//	private StrategyManager buildStrategyManager() {
//		StrategyManager strategyManager = new StrategyManager() ;
//		{
//			strategyManager.setPlanSelectorForRemoval( new WorstPlanForRemovalSelector() ) ;
//		}
//		{
//			PlanStrategy strategy = new PlanStrategyImpl( new ExpBetaPlanChanger(this.config.planCalcScore().getBrainExpBeta()) ) ;
//			strategyManager.addStrategyForDefaultSubpopulation(strategy, 0.9) ;
//		}
//		{
//			PlanStrategyImpl strategy = new PlanStrategyImpl( new ExpBetaPlanSelector(this.config.planCalcScore())) ;
//			strategy.addStrategyModule( new AbstractMultithreadedModule(this.scenario.getConfig().global().getNumberOfThreads()) {
//
//				@Override
//				public PlanAlgorithm getPlanAlgoInstance() {
//					return createRoutingAlgorithm();
//				}
//
//			}) ;
//			strategyManager.addStrategyForDefaultSubpopulation(strategy, 0.1) ;
//		}
//		return strategyManager ;
//	}
//
//
//	private PlanAlgorithm createRoutingAlgorithm() {
//		//// factory to generate routes:
//		//final ModeRouteFactory routeFactory = ((PopulationFactoryImpl) (this.population.getFactory())).getModeRouteFactory();
//
//
//		// travel disutility (generalized cost)
//		final TravelDisutility travelDisutility = new RandomizingTimeDistanceTravelDisutilityFactory( TransportMode.car, config.planCalcScore() ).createTravelDisutility(this.travelTime.getLinkTravelTimes());
//		//
//		//final FreespeedTravelTimeAndDisutility ptTimeCostCalc = new FreespeedTravelTimeAndDisutility(-1.0, 0.0, 0.0);
//
//		//// define the factory for the "computer science" router.  Needs to be a factory because it might be used multiple
//		//// times (e.g. for car router, pt router, ...)
//		//final LeastCostPathCalculatorFactory leastCostPathFactory = new DijkstraFactory();
//
//		//// plug it together
//		//final ModularPlanRouter plansCalcRoute = new ModularPlanRouter();
//		//
//		//Collection<String> networkModes = this.config.plansCalcRoute().getNetworkModes();
//		//for (String mode : networkModes) {
//		//	plansCalcRoute.addLegHandler(mode, new NetworkLegRouter(this.network, leastCostPathFactory.createPathCalculator(this.network, travelDisutility, this.travelTime.getLinkTravelTimes()), routeFactory));
//		//}
//		//Map<String, Double> teleportedModeSpeeds = this.config.plansCalcRoute().getTeleportedModeSpeeds();
//		//for (Entry<String, Double> entry : teleportedModeSpeeds.entrySet()) {
//		//	plansCalcRoute.addLegHandler(entry.getKey(), new TeleportationLegRouter(routeFactory, entry.getValue(), this.config.plansCalcRoute().getBeelineDistanceFactor()));
//		//}
//		//Map<String, Double> teleportedModeFreespeedFactors = this.config.plansCalcRoute().getTeleportedModeFreespeedFactors();
//		//for (Entry<String, Double> entry : teleportedModeFreespeedFactors.entrySet()) {
//		//	plansCalcRoute.addLegHandler(entry.getKey(), new PseudoTransitLegRouter(this.ptRoutingNetwork, leastCostPathFactory.createPathCalculator(this.ptRoutingNetwork, ptTimeCostCalc, ptTimeCostCalc), entry.getValue(), this.config.plansCalcRoute().getBeelineDistanceFactor(), routeFactory));
//		//}
//		//
//		//// return it:
//		//return plansCalcRoute;
//		final Provider<TripRouter> fact =
//			new TripRouterFactoryBuilderWithDefaults().build(
//					scenario);
//		return new PlanRouter(
//				fact.get(
//				) );
//	}
//
//	@Override
//	protected void prepareForSim() {
//		ControlerUtils.checkConfigConsistencyAndWriteToLog(this.config, "Config dump before doIterations:");
//		ParallelPersonAlgorithmUtils.run(this.population, this.config.global().getNumberOfThreads(),
//				new ParallelPersonAlgorithmUtils.PersonAlgorithmProvider() {
//			@Override
//			public AbstractPersonAlgorithm getPersonAlgorithm() {
//				return new PersonPrepareForSim(createRoutingAlgorithm(), RunMunichZone30Controller.this.scenario);
//			}
//		});
//	}
//
//	@Override
//	protected void runMobSim() {
//		QSim qSim = new QSim( this.scenario, this.eventsManager ) ;
//		ActivityEngine activityEngine = new ActivityEngine(eventsManager, qSim.getAgentCounter());
//		qSim.addMobsimEngine(activityEngine);
//		qSim.addActivityHandler(activityEngine);
//		QNetsimEngine netsimEngine = new QNetsimEngine(qSim);
//		qSim.addMobsimEngine(netsimEngine);
//		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
//		DefaultTeleportationEngine teleportationEngine = new DefaultTeleportationEngine(scenario, eventsManager);
//		qSim.addMobsimEngine(teleportationEngine);
//		AgentFactory agentFactory = new DefaultAgentFactory(qSim);
//        PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), agentFactory, qSim);
//        qSim.addAgentSource(agentSource);
//		if (config.controler().getWriteSnapshotsInterval() != 0 && this.getIterationNumber() % config.controler().getWriteSnapshotsInterval() == 0) {
//			// yyyy would be nice to have the following encapsulated in some way:
//			// === begin ===
//			SnapshotWriterManager manager = new SnapshotWriterManager(config);
//			String fileName = getControlerIO().getIterationFilename(this.getIterationNumber(), "otfvis.mvi");
//			SnapshotWriter snapshotWriter = new OTFFileWriter(this.scenario, fileName);
//			manager.addSnapshotWriter(snapshotWriter);
//			// === end ===
//			qSim.addQueueSimulationListeners(manager);
//		}
//		qSim.run();
//	}
//
//	protected void setPtRoutingNetwork(Network ptRoutingNetwork) {
//		Network ptRoutingNetwork1 = ptRoutingNetwork;
//	}
//
//	@Override
//	protected boolean continueIterations(int iteration) {
//		return ( iteration <= config.controler().getLastIteration() ) ;
//	}

}
