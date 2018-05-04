package playground.agarwalamit.fundamentalDiagrams.dynamicPCU.headwayMethod;

import java.util.Map;
import java.util.stream.Collectors;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import playground.agarwalamit.fundamentalDiagrams.core.FDNetworkGenerator;
import playground.agarwalamit.fundamentalDiagrams.core.FDTrackMobsimAgent;
import playground.agarwalamit.fundamentalDiagrams.core.FundamentalDiagramDataGenerator;
import playground.agarwalamit.fundamentalDiagrams.core.GlobalFlowDynamicsUpdator;

public class DynamicPCUFDQSimProvider implements Provider<Mobsim> {

	public static final String PERSON_MODE_ATTRIBUTE_KEY = "travelMode";

	private final Scenario scenario;
	private final EventsManager events;
	private final QNetworkFactory qnetworkFactory;

	private final Map<String, VehicleType> modeToVehicleTypes ;
	private final FDNetworkGenerator fdNetworkGenerator;
	private final GlobalFlowDynamicsUpdator globalFlowDynamicsUpdator;

	@Inject
	private DynamicPCUFDQSimProvider(Scenario scenario, EventsManager events, QNetworkFactory qnetworkFactory,
                                     FDNetworkGenerator fdNetworkGenerator, GlobalFlowDynamicsUpdator globalFlowDynamicsUpdator) {
		this.scenario = scenario;
		this.events = events;
		this.qnetworkFactory = qnetworkFactory;
		this.modeToVehicleTypes = this.scenario.getVehicles()
											   .getVehicleTypes()
											   .entrySet()
											   .stream()
											   .collect(Collectors.toMap(e -> e.getKey().toString(),
													   Map.Entry::getValue));
		this.fdNetworkGenerator = fdNetworkGenerator;
		this.globalFlowDynamicsUpdator = globalFlowDynamicsUpdator;
	}
	
	@Override
	public Mobsim get() {
		final QSim qSim = new QSim(scenario, events);
		ActivityEngine activityEngine = new ActivityEngine(events, qSim.getAgentCounter());
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);

		QNetsimEngine netsimEngine  = new QNetsimEngine(qSim, qnetworkFactory);

		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());

		FundamentalDiagramDataGenerator.LOG.info("=======================");
		FundamentalDiagramDataGenerator.LOG.info("Mobsim agents' are directly added to AgentSource.");
		FundamentalDiagramDataGenerator.LOG.info("=======================");

		if (this.scenario.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(NetworkChangeEventsEngine.createNetworkChangeEventsEngine());
		}

		AgentSource agentSource = new AgentSource() {
			@Override
			public void insertAgentsIntoMobsim() {

				for (Person person : scenario.getPopulation().getPersons().values()) {
					String travelMode = (String) scenario.getPopulation().getPersonAttributes().getAttribute(person.getId().toString(), PERSON_MODE_ATTRIBUTE_KEY);
					double randDouble = MatsimRandom.getRandom().nextDouble();
					double actEndTime = randDouble * FundamentalDiagramDataGenerator.MAX_ACT_END_TIME;

					FDTrackMobsimAgent agent = new FDTrackMobsimAgent(person.getId(), actEndTime, travelMode, fdNetworkGenerator);
					agent.setGlobalFlowDynamicsUpdator(globalFlowDynamicsUpdator);
					qSim.insertAgentIntoMobsim(agent);

					final AmitQVehicle vehicle = new AmitQVehicle(VehicleUtils.getFactory().createVehicle(Id.create(agent.getId(), Vehicle.class), modeToVehicleTypes.get(travelMode)));
					agent.setVehicle(vehicle);
					final Id<Link> linkId4VehicleInsertion = fdNetworkGenerator.getTripDepartureLinkId();
					qSim.createAndParkVehicleOnLink(vehicle.getVehicle(), linkId4VehicleInsertion);
				}
			}
		};

		qSim.addAgentSource(agentSource);
		return qSim;
	}
	
}
