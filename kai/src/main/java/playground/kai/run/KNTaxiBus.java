/**
 * 
 */
package playground.kai.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.run.DrtConfigConsistencyChecker;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author kainagel
 *
 */
public final class KNTaxiBus {

	public static void main(String[] args) {
		
		Config config = ConfigUtils.loadConfig("drt_example/drtconfig.xml", new DrtConfigGroup(), new DvrpConfigGroup(), new OTFVisConfigGroup());
		config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);
		config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.kinematicWaves);
		config.addConfigConsistencyChecker(new DrtConfigConsistencyChecker());
		config.checkConsistency();
		
		DrtConfigGroup drtConfig = ConfigUtils.addOrGetModule(config,DrtConfigGroup.class) ;
		DvrpConfigGroup dvrpConfig = ConfigUtils.addOrGetModule(config,DvrpConfigGroup.class) ;
		
		drtConfig.setMaxTravelTimeAlpha(10.);
		drtConfig.setMaxTravelTimeBeta(3600.);
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		for ( Person person : scenario.getPopulation().getPersons().values() ) {
			for ( PlanElement pe : person.getSelectedPlan().getPlanElements() ) {
				((Activity)pe).setEndTime(7*3600.);
				break ;
			}
		}
		
		Controler controler = DrtControlerCreator.createControler(scenario,true) ;
		
		controler.run() ;
		
	}

}
