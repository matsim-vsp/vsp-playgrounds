package playground.dziemke.analysis.general.matsim;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import playground.dziemke.utils.Events2ExperiencedPlansConverter;

/**
 * @author dziemke
 */
public class CreateExperiencedPlansAndAssignResidence {

    private String networkFile;
    private String transitSchedule;
    private String plansFile;
    private String eventsFile;
    private String crs;

    public static void main(String[] args) {
        String runDirectoryRoot = "../../runs-svn/open_berlin_scenario/v5.3-policies/output/b-01/";
        String runId = "berlin-v5.3-10pct-ctd-b-01";
        String outputExperiencedPlansFile = "experiencedPlans.xml.gz";
        String outputExperiencedPlansFileWithResidence = "experiencedPlans_withResidence.xml.gz";

        String areaShapeFile = "../../shared-svn/studies/countries/de/open_berlin_scenario/input/shapefiles/2013/Berlin_DHDN_GK4.shp";
        String attributeCaption = "NR";
        String distinctiveFeatureId = "11000000";
        String crs = "GK4";

        if (args.length != 0 && args.length == 8) {
            runDirectoryRoot = args[0];
            runId = args[1];
            outputExperiencedPlansFile = args[2];
            outputExperiencedPlansFileWithResidence = args[3];
            areaShapeFile = args[4];
            attributeCaption = args[5];
            distinctiveFeatureId = args[6];
            crs = args[7];
        }

        CreateExperiencedPlansAndAssignResidence createExperiencedPlansAndAssignResidence = new CreateExperiencedPlansAndAssignResidence(runDirectoryRoot, runId, crs);
        createExperiencedPlansAndAssignResidence.convertEventsToExperiencedPlans(outputExperiencedPlansFile);
        createExperiencedPlansAndAssignResidence.applyResidenceFilter(outputExperiencedPlansFile, outputExperiencedPlansFileWithResidence, areaShapeFile, attributeCaption, distinctiveFeatureId);
    }

    private CreateExperiencedPlansAndAssignResidence(String runDirectoryRoot, String runId, String crs) {
        this.networkFile = runDirectoryRoot + "/" + runId + ".output_network.xml.gz";
        this.transitSchedule = runDirectoryRoot + "/" + runId + ".output_transitSchedule.xml.gz";
        this.plansFile = runDirectoryRoot + "/" + runId + ".output_plans.xml.gz";
        this.eventsFile = runDirectoryRoot + "/" + runId + ".output_events.xml.gz";
        this.crs = crs;
    }

    private void convertEventsToExperiencedPlans(String outputExperiencedPlansFile) {
        Config config = ConfigUtils.createConfig();
        config.global().setCoordinateSystem(crs);
        config.network().setInputFile(networkFile);
        config.transit().setTransitScheduleFile(transitSchedule);
        config.plans().setInputFile(plansFile);

        Events2ExperiencedPlansConverter events2ExperiencedPlansConverter = new Events2ExperiencedPlansConverter(
                config, eventsFile, outputExperiencedPlansFile);
        events2ExperiencedPlansConverter.activateTransitSchedule();
        events2ExperiencedPlansConverter.convert();
    }

    private void applyResidenceFilter(String outputExperiencedPlansFile, String outputExperiencedPlansFileWithResidence,
                                      String areaShapeFile, String attributeCaption, String distinctiveFeatureId) {
        PopulationResidenceFilter populationResidenceFilter = new PopulationResidenceFilter(networkFile, outputExperiencedPlansFile,
                areaShapeFile, attributeCaption, distinctiveFeatureId);
        populationResidenceFilter.changeIdentifier("BERLIN", "BRANDENBURG", "unknown");
        populationResidenceFilter.assignResidenceAttribute();
        populationResidenceFilter.writePopulation(outputExperiencedPlansFileWithResidence);
    }
}