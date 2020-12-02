package playground.lu.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import playground.lu.modifiedPlusOne.ModifiedPlueOneInstallationModule;
import playground.lu.unitCapacityMatching.SimpleUnitCapacityRequestInserterModule;

public class RunVulkaneifelScenario {

	public static void main(String[] args) {
		if (args.length == 0) {
			args = new String[] { "C:\\Users\\cluac\\MATSimScenarios\\Vulkaneifel\\drtOnly.config.xml" };
		}

		Config config = ConfigUtils.loadConfig(args[0], new MultiModeDrtConfigGroup(), new DvrpConfigGroup());
		MultiModeDrtConfigGroup multiModeDrtConfig = MultiModeDrtConfigGroup.get(config);
		DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfig, config.planCalcScore(), config.plansCalcRoute());

		Scenario scenario = ScenarioUtils.createScenario(config);
		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class,
				new DrtRouteFactory());
		ScenarioUtils.loadScenario(scenario);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new DvrpModule());
		controler.addOverridingModule(new MultiModeDrtModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(multiModeDrtConfig));

		// Adding in experimental module manually
		for (DrtConfigGroup drtCfg : multiModeDrtConfig.getModalElements()) {
			controler.addOverridingQSimModule(new SimpleUnitCapacityRequestInserterModule(drtCfg));
			controler.addOverridingQSimModule(new ModifiedPlueOneInstallationModule(drtCfg));
		}

		controler.run();
	}
}
