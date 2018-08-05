/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.kai.usecases.parkingSearch;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.PopulationModule;
import org.matsim.core.mobsim.qsim.QSimModule;
import org.matsim.core.mobsim.qsim.QSimProvider;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.config.TransitConfigGroup;

import com.google.inject.Provides;

/**
 * @author nagel
 *
 */
final class KNParkingSearch {
	private static class ParkingSearchQSimModule extends com.google.inject.AbstractModule {
		@Override 
		protected void configure() {
			bind(Mobsim.class).toProvider(QSimProvider.class);
		}
		@SuppressWarnings("static-method")
		@Provides 
		Collection<AbstractQSimModule> provideQSimPlugins(TransitConfigGroup transitConfigGroup, NetworkConfigGroup networkConfigGroup, Config config) {
			Collection<AbstractQSimModule> modules = new ArrayList<>(QSimModule.getDefaultQSimModules());
			modules.removeIf(PopulationModule.class::isInstance);
			modules.add(new ParkingSearchPopulationModule());
			return modules;
		}
	}
	private static class ParkingSearchPopulationModule extends AbstractQSimModule {
		@Override
		protected void configureQSim() {
			bind(PopulationAgentSource.class).asEagerSingleton();
			
			if (getConfig().transit().isUseTransit()) {
				throw new RuntimeException("parking search together with transit is not implemented (should not be difficult)") ;
			} else {
				bind(AgentFactory.class).to(ParkingSearchAgentFactory.class).asEagerSingleton(); // (**)
			}
			
			addAgentSourceBinding(PopulationModule.POPULATION_AGENT_SOURCE_NAME).to(PopulationAgentSource.class);
		}
	}
	private static class ParkingSearchAgentFactory implements AgentFactory {
		@Inject Netsim netsim ;
		@Override
		public MobsimAgent createMobsimAgentFromPerson(Person p) {
			return new MyParkingSearchAgent( p.getSelectedPlan(), netsim ) ;
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("examples/equil/config.xml") ;
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		config.qsim().setSnapshotStyle( SnapshotStyle.queue);

		Scenario scenario = ScenarioUtils.loadScenario( config );

		Controler controler = new Controler( scenario ) ;

		controler.addOverridingModule(new OTFVisLiveModule() ) ;
		
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install() {
				this.install( new ParkingSearchQSimModule() ) ;
			}
		});

		controler.run();
	}

}
