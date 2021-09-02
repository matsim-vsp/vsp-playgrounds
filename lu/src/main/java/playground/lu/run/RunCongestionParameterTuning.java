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
import playground.lu.congestionAwareDrt.CongestionAwareDrtModule;

public class RunCongestionParameterTuning {
    public static void main(String[] args) {
        double[] discountFactors = {0.7, 0.75, 0.8, 0.85, 0.9, 0.95, 1.0};
        double[] penaltyFactors = {1.0, 1.5, 2.0, 2.5, 3.0};
        double[] overFlowFactors = {1.0, 1.5, 2.0, 2.5, 3.0};

        // Single run
//        double[] discountFactors = {0.7, 0.65};
//        double[] penaltyFactors = {1.0, 1.5, 2.0, 2.5, 3.0};
//        double[] overFlowFactors = {1.0, 1.5, 2.0, 2.5, 3.0};

        String configPath = "/Users/luchengqi/Documents/MATSimScenarios/Mielec/config.xml";
        String outputPathCommonPart = "/Users/luchengqi/Documents/MATSimScenarios/Mielec/output/parameterTuning/";

        if (args.length != 0) {
            configPath = args[0];
            outputPathCommonPart = args[1];
        }


        for (double discountFactor : discountFactors) {
            for (double penaltyFactor : penaltyFactors) {
                for (double overFlowFactor : overFlowFactors) {
                    Config config = ConfigUtils.loadConfig(configPath, new MultiModeDrtConfigGroup(), new DvrpConfigGroup());
                    config.controler().setOutputDirectory
                            (outputPathCommonPart + "d-" + discountFactor + "-p-" + penaltyFactor + "-o-" + overFlowFactor);
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
                        controler.addOverridingQSimModule(new CongestionAwareDrtModule(drtCfg, config, discountFactor, penaltyFactor, overFlowFactor));
                    }
                    controler.run();
                }
            }
        }


    }
}
