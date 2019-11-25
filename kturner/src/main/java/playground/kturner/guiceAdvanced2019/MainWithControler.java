package playground.kturner.guiceAdvanced2019;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

public class MainWithControler {

    public static void main( String[] args){

        Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));

        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        Controler controler = new Controler(scenario);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
//                this.bindScoringFunctionFactory().to(MyScoringFunctionFactory.class);
//                this.bindScoringFunctionFactory().toInstance(new MyScoringFunctionFactory()); //Variante A
//                this.bindScoringFunctionFactory().toInstance(new MyScoringFunctionFactory(config)); //Variante B
                //Slides 4-20
                this.bindScoringFunctionFactory().to(MyScoringFunctionFactory.class);
                this.addEventHandlerBinding().to(MyEventHandler.class);


            }
        });

        controler.run();

    }


    private static class MyScoringFunctionFactory implements ScoringFunctionFactory {

//        Wie bekommt man hier was rein? (Zwei Möglichkeiten, die beide gehen und zwar auch gemischt.)

//       //Variante A) Bei Variablen injecten
//        @Inject private Config config;      //Variable direkt injecten.
//
//        @Override
//        public ScoringFunction createNewScoringFunction(Person person) {
//            System.err.println("blabla = " + config.controler().getRoutingAlgorithmType());
//            return null;
//        }

        //Variante B) Eigenen Constructor nehmen und diesen Injecten -< Injected dann die Variablen automatisch
        private Config config;
        @Inject Scenario scenario;      //Variable direkt injectend

        @Inject
        MyScoringFunctionFactory(Config config){
            this.config = config;
        }

        @Override
        public ScoringFunction createNewScoringFunction(Person person) {
            System.err.println("blabla = " + config.controler().getRoutingAlgorithmType());
            System.err.println("scenario = " + scenario.getNetwork().getLinks().values().iterator().next());
            return null;
        }
    }

    private static class MyEventHandler implements EventHandler {

    }
}
