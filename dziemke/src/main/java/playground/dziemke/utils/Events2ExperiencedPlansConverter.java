package playground.dziemke.utils;

import com.google.inject.Injector;
import com.google.inject.Module;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.ReplayEvents;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.*;

public class Events2ExperiencedPlansConverter {

    private Config config;
    private String eventsFile;
    private String outputExperiencedPlansFile;
    private boolean activateTransitSchedule = false;

    public static void main(String[] args) {
        String eventsFile = "../../runs-svn/reoccupBerlin/berlin-v5.5-1pct-b-02_2/berlin-v5.5-1pct-b-02.output_events.xml.gz";
        String inputNetworkFile  = "../../runs-svn/reoccupBerlin/berlin-v5.5-1pct-b-02_2/berlin-v5.5-1pct-b-02.output_network.xml.gz";

        String outputExperiencedPlansFile = "../../runs-svn/reoccupBerlin/berlin-v5.5-1pct-b-02_2/experiencedPlans_2.xml.gz";

        Config config = ConfigUtils.createConfig();
        config.global().setCoordinateSystem("EPSG:31468");
        config.transit().setUseTransit(true );
        config.transit().setTransitScheduleFile("../../runs-svn/reoccupBerlin/berlin-v5.5-1pct-b-02_2/berlin-v5.5-1pct-b-02.output_transitSchedule.xml.gz");
        config.network().setInputFile(inputNetworkFile);

        Events2ExperiencedPlansConverter events2ExperiencedPlansConverter = new Events2ExperiencedPlansConverter(config, eventsFile, outputExperiencedPlansFile);
        events2ExperiencedPlansConverter.activateTransitSchedule();
        events2ExperiencedPlansConverter.convert();
    }

    public Events2ExperiencedPlansConverter(Config config, String eventsFile, String outputExperiencedPlansFile) {

        this.config = config;
        this.eventsFile = eventsFile;
        this.outputExperiencedPlansFile = outputExperiencedPlansFile;
    }

    public void activateTransitSchedule() {

        this.activateTransitSchedule = true;
    }

    public void convert() {

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Injector injector = org.matsim.core.controler.Injector.createInjector(config,
                new Module[]{
                        new ExperiencedPlansModule(),
                        new ExperiencedPlanElementsModule(),
                        new EventsManagerModule(),
                        new ScenarioByInstanceModule(scenario),
                        new org.matsim.core.controler.ReplayEvents.Module()});
        if (activateTransitSchedule)
            ((EventsToLegs)injector.getInstance(EventsToLegs.class)).setTransitSchedule(scenario.getTransitSchedule());
        ReplayEvents replayEvents = (ReplayEvents)injector.getInstance(ReplayEvents.class);
        replayEvents.playEventsFile(eventsFile, 0);
        ((ExperiencedPlansService)injector.getInstance(ExperiencedPlansService.class)).writeExperiencedPlans(outputExperiencedPlansFile);

        //(new PopulationWriter(scenario.getPopulation(), null)).write(outputExperiencedPlansFile);
    }
}
