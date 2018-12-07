package playground.jbischoff.pt.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;


public class RunRaptor{

	private static Logger log = Logger.getLogger(RunRaptor.class);

	public static void main(String[] args) {
		String configFilename = "C:/Users/Joschka/Documents/shared-svn/projects/ptrouting/niedersachsen_sample_scenario/config.xml";
		Config config = ConfigUtils.loadConfig(configFilename);
		config.controler().setOutputDirectory("output_raptor");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {

				// To use the fast pt router:
				install(new SwissRailRaptorModule());
			}
		});

		controler.run();

	}
	}
