package playground.kai.usecases.multipleInjectors;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.PrepareForSim;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.scenario.ScenarioUtils;

class MultipleInjectors {
	
	public static void main( String[] args ) {
		Config config = ConfigUtils.createConfig() ;
		Scenario scenario = ScenarioUtils.createScenario(config) ;
		EventsManager events = new EventsManagerImpl() ;

		PrepareForSim pfs = PrepareForSimUtils.createDefaultPrepareForSim(scenario);
		

		QSim qsim = new QSimBuilder(scenario.getConfig()).useDefaults().build(scenario, events);
		
		pfs.run();
		
		qsim.run();
	}
	
}
