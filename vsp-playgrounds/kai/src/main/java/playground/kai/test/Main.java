/**
 * 
 */
package playground.kai.test;

import java.io.File;
import java.net.URL;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.kai.test.Main.MyScoringFunctionFactory;

public class Main {

	class MyScoringFunctionFactory {

	}

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig() ;
		
		// modify the config
		
		// ---
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		// modify the scenario
		
		// ---

		Controler controler = new Controler( scenario ) ;
		
		// modify the controler:
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install() {
			}
		} ) ;
		
		
		controler.run() ;
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
}
