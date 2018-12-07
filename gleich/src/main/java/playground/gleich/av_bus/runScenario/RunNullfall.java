package playground.gleich.av_bus.runScenario;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import playground.gleich.av_bus.FilePaths;

/**
 * @author gleich
 *
 */
public class RunNullfall {

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("/Users/gleich/Desktop/aktuell/test_input_config_ohne_VA.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		controler.run();
	}

}
