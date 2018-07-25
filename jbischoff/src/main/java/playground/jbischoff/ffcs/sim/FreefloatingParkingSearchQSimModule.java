package playground.jbischoff.ffcs.sim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.contrib.parking.parkingsearch.sim.ParkingPopulationAgentSource;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.ActivityEnginePlugin;
import org.matsim.core.mobsim.qsim.QSimProvider;
import org.matsim.core.mobsim.qsim.TeleportationPlugin;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsPlugin;
import org.matsim.core.mobsim.qsim.components.QSimComponents;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentsConfigurator;
import org.matsim.core.mobsim.qsim.messagequeueengine.MessageQueuePlugin;
import org.matsim.core.mobsim.qsim.pt.TransitEnginePlugin;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEnginePlugin;
import org.matsim.pt.config.TransitConfigGroup;

import com.google.inject.Module;
import com.google.inject.Provides;


class FreefloatingParkingSearchQSimModule extends com.google.inject.AbstractModule {
	@Override 
	protected void configure() {
		bind(Mobsim.class).toProvider(QSimProvider.class);
	}
	@SuppressWarnings("static-method")
	@Provides 
	Collection<AbstractQSimPlugin> provideQSimPlugins(TransitConfigGroup transitConfigGroup, NetworkConfigGroup networkConfigGroup, Config config) {
		final Collection<AbstractQSimPlugin> plugins = new ArrayList<>();
		plugins.add(new MessageQueuePlugin(config));
		plugins.add(new ActivityEnginePlugin(config));
		plugins.add(new QNetsimEnginePlugin(config));
		if (networkConfigGroup.isTimeVariantNetwork()) {
			plugins.add(new NetworkChangeEventsPlugin(config));
		}
		if (transitConfigGroup.isUseTransit()) {
			plugins.add(new TransitEnginePlugin(config));
		}
		plugins.add(new TeleportationPlugin(config));
		plugins.add(new ParkingSearchPopulationPlugin(config));
		return plugins;
	}
	
	@Provides
	QSimComponents provideQSimComponents(Config config) {
		QSimComponents components = new QSimComponents();
		new StandardQSimComponentsConfigurator(config).configure(components);
		
		components.activeAgentSources.add(FREEFLOATING_PARKING_POPULATION_AGENT_SOURCE);
		components.activeAgentSources.add(FFCS_VEHICLE_AGENT_SOURCE);
		
		return components;
	}
	
	static public String FREEFLOATING_PARKING_POPULATION_AGENT_SOURCE = "FreefloatingParkingPopulationAgentSource";
	static public String FFCS_VEHICLE_AGENT_SOURCE = "FFCSVehicleAgentSource";
	
	private static class ParkingSearchPopulationPlugin extends AbstractQSimPlugin {
		public ParkingSearchPopulationPlugin(Config config) { super(config); }
		@Override 
		public Collection<? extends Module> modules() {
			Collection<Module> result = new ArrayList<>();
			result.add(new com.google.inject.AbstractModule() {
				@Override
				protected void configure() {
					if (getConfig().transit().isUseTransit()) {
						throw new RuntimeException("parking search together with transit is not implemented (should not be difficult)") ;
					} else {
						bind(AgentFactory.class).to(FreefloatingParkingAgentFactory.class).asEagerSingleton(); // (**)
					}
					bind(FFCSVehicleAgentSource.class).asEagerSingleton();
					bind(FreefloatingParkingPopulationAgentSource.class).asEagerSingleton();
				}
			});
			return result;
		}
		@Override 
		public Map<String, Class<? extends AgentSource>> agentSources() {
			Map<String, Class<? extends AgentSource>> result = new HashMap<>();
			result.put(FREEFLOATING_PARKING_POPULATION_AGENT_SOURCE, FreefloatingParkingPopulationAgentSource.class);
			result.put(FFCS_VEHICLE_AGENT_SOURCE, FFCSVehicleAgentSource.class);
			return result;
		}
	}
	
}