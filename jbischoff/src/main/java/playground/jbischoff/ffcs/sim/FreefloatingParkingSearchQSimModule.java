package playground.jbischoff.ffcs.sim;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.PopulationModule;
import org.matsim.core.mobsim.qsim.QSimModule;
import org.matsim.core.mobsim.qsim.QSimProvider;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentConfigurator;
import org.matsim.pt.config.TransitConfigGroup;

import com.google.inject.Provides;


class FreefloatingParkingSearchQSimModule extends com.google.inject.AbstractModule {
	@Override 
	protected void configure() {
		bind(Mobsim.class).toProvider(QSimProvider.class);
	}
	@SuppressWarnings("static-method")
	@Provides 
	Collection<AbstractQSimModule> provideQSimModules(TransitConfigGroup transitConfigGroup, NetworkConfigGroup networkConfigGroup, Config config) {
		final Collection<AbstractQSimModule> plugins = new ArrayList<>(QSimModule.getDefaultQSimModules());
		plugins.removeIf(PopulationModule.class::isInstance);
		plugins.add(new ParkingSearchPopulationModule());
		return plugins;
	}
	
	@Provides
	QSimComponentsConfig provideQSimComponentsConfig(Config config) {
		QSimComponentsConfig components = new QSimComponentsConfig();
		new StandardQSimComponentConfigurator(config).configure(components);
		
		components.addNamedComponent(FREEFLOATING_PARKING_POPULATION_AGENT_SOURCE);
		components.addNamedComponent(FFCS_VEHICLE_AGENT_SOURCE);
		
		return components;
	}
	
	static public String FREEFLOATING_PARKING_POPULATION_AGENT_SOURCE = "FreefloatingParkingPopulationAgentSource";
	static public String FFCS_VEHICLE_AGENT_SOURCE = "FFCSVehicleAgentSource";
	
	private static class ParkingSearchPopulationModule extends AbstractQSimModule  {
		@Override
		protected void configureQSim() {
			if (getConfig().transit().isUseTransit()) {
				throw new RuntimeException("parking search together with transit is not implemented (should not be difficult)") ;
			} else {
				bind(AgentFactory.class).to(FreefloatingParkingAgentFactory.class).asEagerSingleton(); // (**)
			}
			bind(FFCSVehicleAgentSource.class).asEagerSingleton();
			bind(FreefloatingParkingPopulationAgentSource.class).asEagerSingleton();
			
			bindNamedComponent(FreefloatingParkingPopulationAgentSource.class, FREEFLOATING_PARKING_POPULATION_AGENT_SOURCE);
			bindNamedComponent(FFCSVehicleAgentSource.class, FFCS_VEHICLE_AGENT_SOURCE);
		}
	}
	
}
