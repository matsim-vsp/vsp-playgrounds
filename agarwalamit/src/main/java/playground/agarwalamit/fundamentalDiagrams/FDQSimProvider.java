package playground.agarwalamit.fundamentalDiagrams;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OnTheFlyServer;

import com.google.inject.Inject;
import com.google.inject.Provider;

import playground.agarwalamit.fundamentalDiagrams.FundamentalDiagramDataGenerator.MySimplifiedRoundAndRoundAgent;

public class FDQSimProvider implements Provider<Mobsim> {
	
	public static final String PERSON_MODE_ATTRIBUTE_KEY = "travelMode";
	
	private Scenario scenario;
	private EventsManager events;
	private QNetworkFactory qnetworkFactory;
	
	private Map<String, VehicleType> modeToVehicleTypes = new HashMap<>();
	
	@Inject
	private FDQSimProvider(Scenario scenario, EventsManager events, QNetworkFactory qnetworkFactory) {
		this.scenario = scenario;
		this.events = events;
		this.qnetworkFactory = qnetworkFactory;
		init();
	}
	
	private void init() {
		this.modeToVehicleTypes = this.scenario.getVehicles().getVehicleTypes().entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue() ));
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

					MobsimAgent agent = new MySimplifiedRoundAndRoundAgent(person.getId(), actEndTime, travelMode);
					qSim.insertAgentIntoMobsim(agent);

					final Vehicle vehicle = VehicleUtils.getFactory().createVehicle(Id.create(agent.getId(), Vehicle.class), modeToVehicleTypes.get(travelMode));
					final Id<Link> linkId4VehicleInsertion = FundamentalDiagramDataGenerator.fdNetworkGenerator.getTripDepartureLinkId();
					qSim.createAndParkVehicleOnLink(vehicle, linkId4VehicleInsertion);
				}
			}
		};

		qSim.addAgentSource(agentSource);

		if ( FundamentalDiagramDataGenerator.isUsingLiveOTFVis ) {
			// otfvis configuration.  There is more you can do here than via file!
			final OTFVisConfigGroup otfVisConfig = ConfigUtils.addOrGetModule(qSim.getScenario().getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
			otfVisConfig.setDrawTransitFacilities(false) ; // this DOES work
			OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, qSim);
			OTFClientLive.run(scenario.getConfig(), server);
		}
		return qSim;
	}
	
}
