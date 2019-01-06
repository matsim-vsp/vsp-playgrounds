package playground.kai.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class KNEquil{

	public static void main( String [] args ) {

		Config config = ConfigUtils.loadConfig( IOUtils.newUrl( ExamplesUtils.getTestScenarioURL( "equil" ) , "config.xml" ) ) ;

		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setLastIteration( 1 );

		// ---

		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		for( Link link : scenario.getNetwork().getLinks().values() ){

			Set<String> modes = new HashSet<>(  Arrays.asList( new String [] {TransportMode.car, TransportMode.bike} ) ) ;
			link.setAllowedModes( modes );
		}

		// ---

		Controler controler = new Controler( scenario ) ;

		// ---

		controler.run() ;
	}

}
