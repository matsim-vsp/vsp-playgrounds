package playground.agarwalamit.fundamentalDiagrams.headwayMethod;

import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.PopulationModule;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import com.google.inject.Inject;
import com.google.inject.Provider;

import playground.agarwalamit.fundamentalDiagrams.AttributableVehicle;
import playground.agarwalamit.fundamentalDiagrams.core.FDConfigGroup;
import playground.agarwalamit.fundamentalDiagrams.core.FDModule;
import playground.agarwalamit.fundamentalDiagrams.core.FDNetworkGenerator;
import playground.agarwalamit.fundamentalDiagrams.core.FDStabilityTester;
import playground.agarwalamit.fundamentalDiagrams.core.FDTrackMobsimAgent;

public class DynamicHeadwayFDQSimProvider implements Provider<Mobsim> {

	public static final String PERSON_MODE_ATTRIBUTE_KEY = "travelMode";

	private final Scenario scenario;
	private final EventsManager events;
	private final QNetworkFactory qnetworkFactory;

	private final Map<String, VehicleType> modeToVehicleTypes ;
	private final FDNetworkGenerator fdNetworkGenerator;
	private final FDStabilityTester stabilityTester;

	@Inject
	DynamicHeadwayFDQSimProvider(Scenario scenario, EventsManager events, QNetworkFactory qnetworkFactory,
								 FDNetworkGenerator fdNetworkGenerator, FDStabilityTester stabilityTester) {
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
		this.stabilityTester = stabilityTester;
	}
	
	@Override
	public Mobsim get() {
		final QSim qSim = new QSimBuilder(scenario.getConfig()) //
				.useDefaults() //
				.removeModule(PopulationModule.class) //
				.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
						bind(QNetworkFactory.class).toInstance(qnetworkFactory);
					}
				}) //
				.build(scenario, events);

		FDModule.LOG.info("=======================");
		FDModule.LOG.info("Mobsim agents' are directly added to AgentSource.");
		FDModule.LOG.info("=======================");

		FDConfigGroup fdConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(), FDConfigGroup.class);

		AgentSource agentSource = new AgentSource() {
			@Override
			public void insertAgentsIntoMobsim() {

				for (Person person : scenario.getPopulation().getPersons().values()) {
					String travelMode = (String) scenario.getPopulation().getPersonAttributes().getAttribute(person.getId().toString(), PERSON_MODE_ATTRIBUTE_KEY);
					double randDouble = MatsimRandom.getRandom().nextDouble();
					double actEndTime = randDouble * FDModule.MAX_ACT_END_TIME;

					FDTrackMobsimAgent agent = new FDTrackMobsimAgent(person.getId(), actEndTime, travelMode, fdNetworkGenerator);
					agent.setStabilityTester(stabilityTester);
					qSim.insertAgentIntoMobsim(agent);

					AttributableVehicle attributableVehicle = new AttributableVehicle(Id.create(agent.getId(), Vehicle.class), modeToVehicleTypes.get(travelMode));
					attributableVehicle.getAttributes().putAttribute("headway", 3600./fdConfigGroup.getTrackLinkCapacity() ); //initialize
					final QVehicle vehicle = new QVehicle(
//							VehicleUtils.getFactory().createVehicle(Id.create(agent.getId(), Vehicle.class),
							attributableVehicle);
					vehicle.setDriver(agent);
					scenario.getVehicles().removeVehicle(vehicle.getId());
					scenario.getVehicles().addVehicle(vehicle.getVehicle());
					agent.setVehicle(vehicle);
					final Id<Link> linkId4VehicleInsertion = fdNetworkGenerator.getTripDepartureLinkId();
					qSim.addParkedVehicle(vehicle, linkId4VehicleInsertion);
				}
			}
		};

		qSim.addAgentSource(agentSource);
		return qSim;
	}
	
}
