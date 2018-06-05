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
    private String outputExperiancedPlansFile;
    private boolean activateTransitSchedule = false;

    public Events2ExperiencedPlansConverter(Config config, String eventsFile, String outputExperiancedPlansFile) {

        this.config = config;
        this.eventsFile = eventsFile;
        this.outputExperiancedPlansFile = outputExperiancedPlansFile;
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
        ((ExperiencedPlansService)injector.getInstance(ExperiencedPlansService.class)).writeExperiencedPlans(outputExperiancedPlansFile);
    }
}
