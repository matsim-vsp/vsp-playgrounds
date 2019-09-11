package playground.gleich.av_bus.runScenario;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.av.intermodal.router.VariableAccessTransitRouterModule;
import org.matsim.contrib.av.intermodal.router.config.VariableAccessConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

public class RunNullfallAsVariableAccess {
	
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig(args[0],
				new VariableAccessConfigGroup());
		config.controler().setOutputDirectory(IOUtils.extendUrl(config.getContext(), args[1]).getFile());

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new VariableAccessTransitRouterModule());
		controler.run();
	}

}
