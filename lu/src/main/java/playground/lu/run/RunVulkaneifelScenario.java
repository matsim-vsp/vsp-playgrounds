package playground.lu.run;

import java.io.IOException;

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

import playground.lu.drtAnalysis.DrtStayTaskXYPlotWriter;
import playground.lu.drtAnalysis.ZonalAvailabilityModule;
import playground.lu.targetLinkSelection.PredeterminedLinkSelectionModule;
import playground.lu.unitCapacityMatching.SimpleUnitCapacityRequestInserterModule;

public class RunVulkaneifelScenario {

	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			args = new String[] { "C:\\Users\\cluac\\MATSimScenarios\\Vulkaneifel\\config_for_rebalance_study.xml" };
		}

		Config config = ConfigUtils.loadConfig(args[0], new MultiModeDrtConfigGroup(), new DvrpConfigGroup());
		String outputDirectory = config.controler().getOutputDirectory();
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
			double maxEuclideanDistance = 3000;
			controler
					.addOverridingQSimModule(new SimpleUnitCapacityRequestInserterModule(drtCfg, maxEuclideanDistance));
			controler.addOverridingModule(new PredeterminedLinkSelectionModule(drtCfg));
			controler.addOverridingModule(new ZonalAvailabilityModule(drtCfg));
		}
		controler.run();

		// Plot idle vehicles location
		DrtStayTaskXYPlotWriter drtStayTaskXYPlotWriter = new DrtStayTaskXYPlotWriter(
				outputDirectory + "/output_events.xml.gz", outputDirectory + "/output_network.xml.gz",
				outputDirectory + "/stayTaskDataForXYPlot.csv");
		drtStayTaskXYPlotWriter.run();
	}
}
