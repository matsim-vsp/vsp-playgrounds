package playground.dziemke.analysis.general.matsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import playground.dziemke.utils.Events2ExperiencedPlansConverter;

public class ExperiencedPlansResidenceApplicator {

    public static void main(String[] args) {

        if (args.length != 0) {
            String runsDirectoryRoot = args[0];
            String runId = args[1];
            String outputExperiencedPlansFile = args[2];
            String outputExperiencedPlansFileWithResidence = args[3];
            String areaShapeFile = args[4];
            int areaId = Integer.parseInt(args[5]);

            ExperiencedPlansResidenceApplicator epra = new ExperiencedPlansResidenceApplicator(runsDirectoryRoot, runId);
            epra.convertEventsToExperiencedPlans(outputExperiencedPlansFile);

            epra.applyResidenceFilter(outputExperiencedPlansFile, outputExperiencedPlansFileWithResidence, areaShapeFile, areaId);
        }
    }

    private String networkFile;
    private String transitSchedule;
    private String plansFile;
    private String eventsFile;

    private ExperiencedPlansResidenceApplicator(String runsDirectoryRoot, String runId) {

        this.networkFile = runsDirectoryRoot + "/" + runId + "/" + runId + ".output_network.xml.gz";
        this.transitSchedule = runsDirectoryRoot + "/" + runId + "/" + runId + ".output_transitSchedule.xml.gz";
        this.plansFile = runsDirectoryRoot + "/" + runId + "/" + runId + ".output_plans.xml.gz";
        this.eventsFile = runsDirectoryRoot + "/" + runId + "/" + runId + ".output_events.xml.gz";
    }

    private void convertEventsToExperiencedPlans(String outputExperiencedPlansFile) {

        //convert events to output_experiencedPlans
        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(networkFile);
        config.transit().setTransitScheduleFile(transitSchedule);
        config.plans().setInputFile(plansFile);

        Events2ExperiencedPlansConverter events2ExperiencedPlansConverter = new Events2ExperiencedPlansConverter(
                config, eventsFile, outputExperiencedPlansFile
        );
        events2ExperiencedPlansConverter.activateTransitSchedule();
        events2ExperiencedPlansConverter.convert();
        //conversion finished
    }

    private void applyResidenceFilter(String outputExperiencedPlansFile, String outputExperiencedPlansFileWithResidence,
                                        String areaShapeFile, int areaId) {

        //apply residenceFilter to experienced plans
        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(networkFile);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();

        ResidenceFilterWriter residenceFilterWriter = new ResidenceFilterWriter(network, areaShapeFile, areaId);
        residenceFilterWriter.filterAndWrite(outputExperiencedPlansFile, outputExperiencedPlansFileWithResidence);
        //application finished
    }

}
