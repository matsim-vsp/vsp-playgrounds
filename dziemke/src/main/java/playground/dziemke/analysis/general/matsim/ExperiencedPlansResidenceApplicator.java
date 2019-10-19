package playground.dziemke.analysis.general.matsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import playground.dziemke.utils.Events2ExperiencedPlansConverter;

public class ExperiencedPlansResidenceApplicator {

    public static void main(String[] args) {
    	//
    	//String runDirectoryRoot = "../../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-10pct/output-berlin-v5.2-10pct/";
        String runDirectoryRoot = "../../runs-svn/open_berlin_scenario/v5.3-policies/output/b-01/";
        //String runId = "berlin-v5.2-10pct";
        String runId = "berlin-v5.3-10pct-ctd-b-01";
        String outputExperiencedPlansFile = "experiencedPlans.xml.gz";
        String outputExperiencedPlansFileWithResidence = "experiencedPlans_withResidence.xml.gz";
//        String runDirectoryRoot = "../../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-1pct/output-berlin-v5.2-1pct/";
//        String runId = "berlin-v5.2-1pct";
//        String outputExperiencedPlansFile = "berlin-v5.2-1pct.experiencedPlans.xml.gz";
//        String outputExperiencedPlansFileWithResidence = "berlin-v5.2-1pct.experiencedPlans_withResidence.xml.gz";
        String areaShapeFile = "../../shared-svn/studies/countries/de/open_berlin_scenario/input/shapefiles/2013/Berlin_DHDN_GK4.shp";
        int areaId = 11000000;
        //

        if (args.length != 0) {
        	runDirectoryRoot = args[0];
            runId = args[1];
            outputExperiencedPlansFile = args[2];
            outputExperiencedPlansFileWithResidence = args[3];
            areaShapeFile = args[4];
            areaId = Integer.parseInt(args[5]);
//            String runsDirectoryRoot = args[0];
//            String runId = args[1];
//            String outputExperiencedPlansFile = args[2];
//            String outputExperiencedPlansFileWithResidence = args[3];
//            String areaShapeFile = args[4];
//            int areaId = Integer.parseInt(args[5]);
//
//            ExperiencedPlansResidenceApplicator epra = new ExperiencedPlansResidenceApplicator(runsDirectoryRoot, runId);
//            epra.convertEventsToExperiencedPlans(outputExperiencedPlansFile);
//
//            epra.applyResidenceFilter(outputExperiencedPlansFile, outputExperiencedPlansFileWithResidence, areaShapeFile, areaId);
        }
        
        ExperiencedPlansResidenceApplicator epra = new ExperiencedPlansResidenceApplicator(runDirectoryRoot, runId);
        epra.convertEventsToExperiencedPlans(outputExperiencedPlansFile);

        epra.applyResidenceFilter(outputExperiencedPlansFile, outputExperiencedPlansFileWithResidence, areaShapeFile, areaId);
    }

    private String networkFile;
    private String transitSchedule;
    private String plansFile;
    private String eventsFile;

    private ExperiencedPlansResidenceApplicator(String runDirectoryRoot, String runId) {

        this.networkFile = runDirectoryRoot + "/" + runId + ".output_network.xml.gz";
        this.transitSchedule = runDirectoryRoot + "/" + runId + ".output_transitSchedule.xml.gz";
        this.plansFile = runDirectoryRoot + "/" + runId + ".output_plans.xml.gz";
        this.eventsFile = runDirectoryRoot + "/" + runId + ".output_events.xml.gz";
    }

    private void convertEventsToExperiencedPlans(String outputExperiencedPlansFile) {

        //convert events to output_experiencedPlans
        Config config = ConfigUtils.createConfig();
        config.global().setCoordinateSystem("GK4");
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
        config.global().setCoordinateSystem("GK4");
        config.network().setInputFile(networkFile);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();

        ResidenceFilterWriter residenceFilterWriter = new ResidenceFilterWriter(network, areaShapeFile, areaId);
        residenceFilterWriter.filterAndWrite(outputExperiencedPlansFile, outputExperiencedPlansFileWithResidence);
        //application finished
    }

}
