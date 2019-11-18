package playground.kturner.guiceMatsimWith;

// import com.google.inject.*;

import com.google.inject.Module;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.*;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.Facility;

public class Main {

    public static void main( String[] args) {
//--- This was without MATSim stuff
//        Module module = new AbstractModule(){
//            @Override protected void configure(){
//                bind (Abc.class).to(AbcImpl1.class);
//                bind (Helper.class).to(HelperImpl2.class);
//            }
//        };
//        Injector injector = Guice.createInjector(module);
//        Abc abc = injector.getInstance(Abc.class);
//        abc.doSomething();
// ---------

        Config config = ConfigUtils.createConfig();
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        Scenario scenario = ScenarioUtils.createScenario(config);
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
//        String mainMode;
//        Facility fromFacility;
//        Facility toFacility;
//        double departureTime;
//        Person person;
//        tripRouter.calcRoute(mainMode, fromFacility, toFacility, departureTime, person);
    }

    private static class HelperImpl2 implements Helper{
        @Override
        public void doHelp() {
            System.out.println("calling Helper2");
        }
    }


    //---above is your "matsim-script-in-java"

    //below are some implementations ion matsim framework

    private interface Abc{
        void doSomething();
    }

    private static class AbcImpl1 implements Abc{
        @Inject Helper helper;
        @Override
        public void doSomething() {
            System.out.println("calling impl1");
            helper.doHelp();
        }
    }


    private interface Helper{
        void doHelp();
    }

    private static class HelperImpl1 implements Helper{
        @Override
        public void doHelp() {
            System.out.println("calling Helper1");
        }
    }
}
