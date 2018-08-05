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
import org.matsim.core.mobsim.qsim.components.QSimComponents;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentsConfigurator;
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
	QSimComponents provideQSimComponents(Config config) {
		QSimComponents components = new QSimComponents();
		new StandardQSimComponentsConfigurator(config).configure(components);
		
		components.activeAgentSources.add(FREEFLOATING_PARKING_POPULATION_AGENT_SOURCE);
		components.activeAgentSources.add(FFCS_VEHICLE_AGENT_SOURCE);
		
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
			
			bindAgentSource(FREEFLOATING_PARKING_POPULATION_AGENT_SOURCE).to(FreefloatingParkingPopulationAgentSource.class);
			bindAgentSource(FFCS_VEHICLE_AGENT_SOURCE).to(FFCSVehicleAgentSource.class);
		}
	}
	
}