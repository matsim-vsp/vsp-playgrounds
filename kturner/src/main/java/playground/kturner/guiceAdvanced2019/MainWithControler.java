package playground.kturner.guiceAdvanced2019;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

public class MainWithControler {

    public static void main( String[] args){

        Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));

        Scenario scenario = ScenarioUtils.loadScenario(config);

        Controler controler = new Controler(scenario);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
               //... bind( Abc.class).to(AbcImpl1.class); //  hier dann sachen einfügen. Die ganzen install() Sachen braucht man dann nicht.
            }
        });

        controler.run();

    }
}
