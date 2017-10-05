/**
 * 
 */
package playground.kai.run;

import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.matsim4urbansim.utils.network.NetworkSimplifier;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.algorithms.NetworkCalcTopoType;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author kainagel
 *
 */
class KNRiyadhController {

	public static void main(String[] args) {

		Config config = ConfigUtils.loadConfig("/Users/kainagel/kairuns/riyadh/config.xml") ;

		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists ) ;
		config.controler().setOutputDirectory("/Users/kainagel/kairuns/riyadh/output");

		config.plans().setRemovingUnneccessaryPlanAttributes(true);

		//		config.controler().setRoutingAlgorithmType(RoutingAlgorithmType.FastAStarLandmarks);
		config.controler().setLastIteration(0);

		final double factor = 1. ;

		config.qsim().setFlowCapFactor(factor);
		config.qsim().setStorageCapFactor(factor);

		// ---

		Scenario scenario1 = ScenarioUtils.loadScenario(config) ;

		MutableScenario scenario2 = ScenarioUtils.createMutableScenario(config) ;

		new NetworkCleaner().run( scenario1.getNetwork() ) ;
		NetworkSimplifier simplifier = new NetworkSimplifier() ;
		Set<Integer> nodeTypesToMerge = new TreeSet<>() ;
		nodeTypesToMerge.add( NetworkCalcTopoType.PASS1WAY ) ;
		nodeTypesToMerge.add( NetworkCalcTopoType.PASS2WAY ) ;
		simplifier.setNodesToMerge(nodeTypesToMerge);
		simplifier.run( scenario1.getNetwork() );
		scenario2.setNetwork(scenario1.getNetwork()) ;

		MatsimRandom.reset(4711);
		for ( Person person : scenario1.getPopulation().getPersons().values() ) {
			Activity firstAct =  (Activity) person.getSelectedPlan().getPlanElements().get(0) ;
			if ( firstAct.getEndTime() >= 14.*3600 + 28.*60 + 20 ) {
				continue ;
			}
			//			if ( firstAct.getEndTime()== 14.*3600 + 28.*60 + 53 ) {
			//				continue ;
			//			}
			//			if ( firstAct.getEndTime()== 14.*3600 + 28.*60 + 54 ) {
			//				continue ;
			//			}
			//			if ( firstAct.getEndTime()== 14.*3600 + 28.*60 + 55 ) {
			//				continue ;
			//			}
			if ( MatsimRandom.getRandom().nextDouble() < factor ) {
				scenario2.getPopulation().addPerson( person );
			}
		}

		// ---

		Controler controler = new Controler( scenario2 ) ;

		controler.run();

	}

}
