package playground.gleich.misc;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunMATSim {

	public static void main(String[] args) {
		String configFilename = args[0];
		Config config = ConfigUtils.loadConfig(configFilename);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);

		controler.run();
	}

}
