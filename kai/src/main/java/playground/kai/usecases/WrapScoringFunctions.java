package playground.kai.usecases;

import com.google.inject.Module;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.bicycle.BicycleConfigGroup;
import org.matsim.contrib.bicycle.BicycleModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.*;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class WrapScoringFunctions{

	public static void main( String[] args ){

		Config config = ConfigUtils.createConfig() ;
		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );
		config.controler().setLastIteration( 2 );

//		ConfigUtils.addOrGetModule( config, BicycleConfigGroup.class ) ;

		// ---

		Scenario scenario = ScenarioUtils.createScenario( config ) ;

		// ---
		// get default scoring function factory:

		AbstractModule module = new AbstractModule(){
			@Override public void install(){
				install( new NewControlerModule() ) ;
				install( new ControlerDefaultCoreListenersModule() );
				install( new ScenarioByInstanceModule( scenario ) ) ;
				install( new ControlerDefaultsModule() ) ;
			}
		} ;
		module = AbstractModule.override( Collections.singletonList(module), new AbstractModule(){
			@Override public void install(){
//				install( new BicycleModule() ) ;
				// not public, and I don't want to restart the "consistency with matsim" cycle again. kai, sep'19
			}
		} ) ;

		com.google.inject.Injector injector = Injector.createInjector( config, module );

		ScoringFunctionFactory sff1 = injector.getInstance( ScoringFunctionFactory.class );

		ScoringFunctionFactory sff2 = new ScoringFunctionFactory(){
			@Override public ScoringFunction createNewScoringFunction( Person person ){
				ScoringFunction previous = sff1.createNewScoringFunction( person );
				if ( previous instanceof SumScoringFunction ) {
					SumScoringFunction.BasicScoring addition = new MyScoringAddition() ;
					((SumScoringFunction) previous).addScoringFunction( addition );
				}
				return previous ;
			}
		} ;

		// ---

		Controler controler = new Controler(scenario) ;

		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				bind( ScoringFunctionFactory.class ).toInstance( sff2 ) ;
			}
		} ) ;

		controler.run() ;
	}

	private static class MyScoringAddition implements SumScoringFunction.BasicScoring{
		@Override public void finish(){
			throw new RuntimeException( "not implemented" );
		}
		@Override public double getScore(){
			throw new RuntimeException( "not implemented" );
		}
	}
}
