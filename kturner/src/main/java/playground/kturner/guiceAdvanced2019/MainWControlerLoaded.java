package playground.kturner.guiceAdvanced2019;

// import com.google.inject.*;

import com.google.inject.Module;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.*;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

import java.net.URL;
import java.util.List;

public class MainWControlerLoaded {

    public static void main( String[] args) {

        URL url = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml");
        Config config = ConfigUtils.loadConfig(url);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Module module = new AbstractModule(){
            @Override
            public void install() {                 //Das so lernen. Man braucht all diese vier!
                install(new NewControlerModule());
                install(new ControlerDefaultCoreListenersModule());
                install(new ControlerDefaultsModule());
                install(new ScenarioByInstanceModule(scenario));
            }
        };
        com.google.inject.Injector injector = Injector.createInjector(config, module); //Note google Injector and MATSim part here.

        TripRouter tripRouter = injector.getInstance(TripRouter.class);
        String mainMode = TransportMode.car;
        Facility fromFacility = FacilitiesUtils.wrapLink(scenario.getNetwork().getLinks().get(Id.createLinkId("13")));
        Facility toFacility = FacilitiesUtils.wrapLink(scenario.getNetwork().getLinks().get(Id.createLinkId(3)));
        double departureTime = 8.*3600;
        Person person = null;

        List<? extends PlanElement> result = tripRouter.calcRoute(mainMode, fromFacility, toFacility, departureTime, person);

        for (PlanElement planElement : result){
            System.out.println(planElement.toString());
        }
    }

}
