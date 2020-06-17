/**
 * 
 */
package playground.kai.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author kainagel
 *
 */
public final class KNTaxiBus {

	public static void main(String[] args) {

		Config config = ConfigUtils.loadConfig("examples/drt_example/drtconfig_door2door.xml",
				new MultiModeDrtConfigGroup(), new DvrpConfigGroup(), new OTFVisConfigGroup());
		config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);
		config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.kinematicWaves);

		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(config);
		drtConfig.setMaxTravelTimeAlpha(10.);
		drtConfig.setMaxTravelTimeBeta(3600.);
		
		config.checkConsistency();

		Controler controler = DrtControlerCreator.createControler(config, true);
		KNTaxiBus.customizeDrtScenario(controler.getScenario());
		controler.run();
	}

	private static void customizeDrtScenario(Scenario scenario) {
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
				((Activity)pe).setEndTime(0 * 3600. + MatsimRandom.getRandom().nextDouble() * 7200.);
				break;
			}
		}
	}
}
