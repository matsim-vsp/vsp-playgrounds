package playground.kturner.guiceMatsimWith;

// import com.google.inject.*;

import com.google.inject.Module;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;

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
        Module module = new AbstractModule(){
            @Override
            public void install() {
                bind (Abc.class).to(AbcImpl1.class);
                if (getConfig().controler().getRoutingAlgorithmType() == ControlerConfigGroup.RoutingAlgorithmType.Dijkstra){
                    bind(Helper.class).to(HelperImpl1.class).in(Singleton.class);
                }else {
                    bind(Helper.class).to(HelperImpl2.class).in(Singleton.class);
                }
            }
        };
        com.google.inject.Injector injector = Injector.createInjector(config, module); //Note google Injector and MATSim part here.

        Abc abc = injector.getInstance(Abc.class);
        abc.doSomething();
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
