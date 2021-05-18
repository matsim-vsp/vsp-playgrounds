package playground.lu.run;

import com.google.common.collect.Sets;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.HashSet;
import java.util.Set;

public class RunFreightOnlyPlans {

    public static void main(String[] args) {
        String configPath = "/Users/luchengqi/Documents/SVN/shared-svn/projects/german-wide-freight/v1.2/run.config.xml";
        String freightOnlyPlans = "/Users/luchengqi/Documents/SVN/shared-svn/projects/german-wide-freight/v1.2/" +
                "german-wide-freight-25pct.xml.gz";
        String networkPath = "/Users/luchengqi/Documents/SVN/shared-svn/projects/german-wide-freight/" +
                "original-data/german-primary-road.network.xml.gz";
        String outputPath = "/Users/luchengqi/Documents/SVN/shared-svn/projects/german-wide-freight/v1.2/run";

        if (args.length != 0) {
            configPath = args[0];
            freightOnlyPlans = args[1];
            networkPath = args[2];
            outputPath = args[3];
        }

        Config config = ConfigUtils.loadConfig(configPath);
        config.global().setCoordinateSystem("EPSG:5677");
        config.controler().setLastIteration(0);
        config.controler().setOutputDirectory(outputPath);
        config.qsim().setFlowCapFactor(10000);
        config.qsim().setStorageCapFactor(10000);
        config.plans().setInputFile(freightOnlyPlans);
        config.network().setInputFile(networkPath);
        config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams
                ("freight_start").setTypicalDuration(60 * 15));
        config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams
                ("freight_end").setTypicalDuration(60 * 15));

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();

        for (Link link : network.getLinks().values()) {
            Set<String> modes = link.getAllowedModes();
            // allow freight traffic together with cars
            if (modes.contains("car")) {
                HashSet<String> newModes = Sets.newHashSet(modes);
                newModes.add("freight");
                link.setAllowedModes(newModes);
            }
        }

        Controler controler = new Controler(scenario);
        controler.run();

    }
}
