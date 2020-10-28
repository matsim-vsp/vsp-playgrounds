package run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.run.drt.RunDrtOpenBerlinScenario;

import unitCapacityMatching.UnitCapcityZonalRequestInserterModule;

public class RunDrtOpenBerlinScenarioWithExpModules {

	private static final Logger log = Logger.getLogger(RunDrtOpenBerlinScenarioWithExpModules.class);

	public static void main(String[] args) throws CommandLine.ConfigurationException {

		for (String arg : args) {
			log.info(arg);
		}

		if (args.length == 0) {
			args = new String[] { "scenarios/berlin-v5.5-1pct/input/drt/berlin-drt-v5.5-1pct.config.xml" };
		}

		Config config = RunDrtOpenBerlinScenario.prepareConfig(args);
		Scenario scenario = RunDrtOpenBerlinScenario.prepareScenario(config);
		Controler controler = RunDrtOpenBerlinScenario.prepareControler(scenario);

		MultiModeDrtConfigGroup multiModeDrtConfig = MultiModeDrtConfigGroup.get(config);

		// Adding in the experimental module
		for (DrtConfigGroup drtCfg : multiModeDrtConfig.getModalElements()) {
//			controler.addOverridingQSimModule(new ExperimentalModuleInserstionWithZC(drtCfg));
			controler.addOverridingQSimModule(new UnitCapcityZonalRequestInserterModule(drtCfg));
		}

		controler.run();
	}
}
