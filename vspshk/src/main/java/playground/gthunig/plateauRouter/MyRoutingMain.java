package playground.gthunig.plateauRouter;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author jbischoff
 *
 */
public class MyRoutingMain {
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("input/ha2/ha2_routerconfig.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
			bindLeastCostPathCalculatorFactory().to(MatsimClassLeastCostPathCalculatorFactory.class);	
			}
		}
		);
	controler.run();
	}
	
}
