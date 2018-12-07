package playground.kai.usecases.newqsimdesign;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.List;

import static org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists;

class NewQSimViaConfig {
	
	public static void main ( String [] args ) {
		
		Config config = ConfigUtils.createConfig() ;
		config.controler().setOverwriteFileSetting( deleteDirectoryIfExists );
		config.controler().setLastIteration( 1 );
		
		final QSimComponentsConfigGroup componentsConfig = ConfigUtils.addOrGetModule( config, QSimComponentsConfigGroup.class );;
		final List<String> sources = componentsConfig.getActiveComponents();
		final String MY_AGENT_SOURCE = "MyAgentSource";
		sources.add( MY_AGENT_SOURCE ) ;
		componentsConfig.setActiveComponents( sources );
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		Controler controler = new Controler( scenario ) ;
		
//		controler.addOverridingModule( new AbstractModule() {
//			@Override public void install() {
//				this.bindMobsim().toProvider( new Provider<Mobsim>(){
//					@Inject EventsManager events ;
//					@Override public Mobsim get() {
//						final QSimBuilder builder = new QSimBuilder( config );
//						builder.useDefaults() ;
//						builder.addQSimModule( new AbstractQSimModule() {
//							@Override
//							protected void configureQSim() {
//								this.addNamedComponent( MyAgentSource.class, MY_AGENT_SOURCE ) ;
//							}
//						} ) ;
//						return builder.build( scenario, events ) ;
//					}
//				} ) ;
//			}
//		} );

		controler.addOverridingQSimModule( new AbstractQSimModule(){
			@Override
			protected void configureQSim(){
				this.addNamedComponent( MyAgentSource.class, MY_AGENT_SOURCE );
			}
		} ) ;
		
		controler.run() ;
		
	}
	
	private static class MyAgentSource implements AgentSource {
		@Override
		public void insertAgentsIntoMobsim() {
			// todo
		}
	}
}
