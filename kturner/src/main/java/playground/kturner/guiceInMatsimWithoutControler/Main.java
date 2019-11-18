package playground.kturner.guiceInMatsimWithoutControler;

import com.google.inject.*;

public class Main {

    public static void main( String[] args) {

        Module module = new AbstractModule(){
            @Override protected void configure(){
                bind (Abc.class).to(AbcImpl1.class);
                bind (Helper.class).to(HelperImpl2.class);
            }
        };
        Injector injector = Guice.createInjector(module);
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
