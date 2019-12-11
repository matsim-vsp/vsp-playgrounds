package playground.dziemke.advancedclass;

import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.*;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

import java.net.URL;
import java.util.List;

public class MainMatsim2 {

    public static void main(String[] args) {
//        Module module = new AbstractModule() {
//            @Override protected void configure() {
//                bind(Abc.class).to(AbcImpl1.class);
//                bind(Helper.class).to(HelperImpl2.class);
//            }
//        };
//        Injector injector = Guice.createInjector(module);
//        Abc abc = injector.getInstance(Abc.class);
//        abc.doSomething();

        URL url = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml");
        Config config = ConfigUtils.loadConfig(url);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        Scenario scenario = ScenarioUtils.createScenario(config);
        Module module = new AbstractModule() {
            @Override public void install() {
                install(new NewControlerModule());
                install(new ControlerDefaultCoreListenersModule());
                install((new ControlerDefaultsModule()));
                install(new ScenarioByInstanceModule(scenario));
            }
        };

        com.google.inject.Injector injector = Injector.createInjector(config, module);




        TripRouter tripRouter = injector.getInstance(TripRouter.class);
        String mainMode = TransportMode.car;
        Facility fromFacility = FacilitiesUtils.wrapLink(scenario.getNetwork().getLinks().get(Id.createLinkId("1")));
        Facility toFacility = FacilitiesUtils.wrapLink(scenario.getNetwork().getLinks().get(Id.createLinkId("23")));
        double departureTime = 8 * 3600.;
        Person person = null;
        List<? extends PlanElement> result = tripRouter.calcRoute(mainMode, fromFacility, toFacility, departureTime, person);

        for (PlanElement planElement : result) {
            planElement.toString();
        }

    }

    private static class HelperImpl2 implements Helper {
        @Override public void doHelp() {
            System.out.println("calling helper2");
        }
    }

    // above: your "matsim-script-in-java"
    // below: some implementations in the matsim framework

    private interface Abc {
        void doSomething();
    }

    private static class AbcImpl1 implements Abc {
        @Inject Helper helper;
        @Override public void doSomething() {
            System.out.println("calling impl1");
            helper.doHelp();
        }
    }

    private interface Helper {
        void doHelp();
    }

    private static class HelperImpl1 implements Helper {
        @Override public void doHelp() {
            System.out.println("calling helper1");
        }
    }
}