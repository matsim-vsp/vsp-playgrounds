package playground.dziemke.analysis.generalNew;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import playground.dziemke.analysis.general.matsim.ResidenceFilterReader;
import playground.dziemke.analysis.general.matsim.ResidenceFilterWriter;
import playground.dziemke.analysis.mid.Mid2PopulationParser;
import playground.vsp.analysis.utils.GnuplotUtils;

import java.io.File;
import java.io.IOException;

public class TripSurveyUtils {

    public static void main(String[] args) throws IOException {

//        String midFilePath = "C:/Users/gthunig/VSP/matsim/shared-svn/projects/nemo_mercator/data/original_files/MID/MiD2017_Wege_RVR-Gebiet.csv";
//        String outputDirectory = "C:/Users/gthunig/VSP/matsim/shared-svn/projects/nemo_mercator/data/original_files/MID/test";
//        String relativePathToGnuplotScript = "../../../../../cemdapMatsimCadyts/analysis/gnuplot/plot_rel_path_survey_run.gnu";
        String relativePathToGnuplotScript = "../plot_rel_path_run.gnu";
//
//        Population population = TripSurveyUtils.parseMiD(midFilePath);
//        File directory = new File(outputDirectory);
//        if (!directory.exists()) directory.mkdirs();
//        PopulationUtils.writePopulation(population, outputDirectory + "/plans.xml");
//
//        PopulationAnalyzer populationAnalyzer = new PopulationAnalyzer(new PopulationAnalyzerBinWidhtConfig(), population);
//        TripFilter tripFilter = new TripFilter();
//        tripFilter.activateDist(25,50);
//        populationAnalyzer.setTripFilter(tripFilter);
//        populationAnalyzer.analyzeAndWrite(outputDirectory);
//
////        TripSurveyUtils.performDefaultAnalysis(population, outputDirectory);
//        TripSurveyUtils.plotDefaultGnuplots(outputDirectory, relativePathToGnuplotScript);

    	// --
//        String runDirectory = "../../public-svn\\matsim\\scenarios\\countries\\de\\berlin\\berlin-v5.2-1pct\\berlin-v5.2-1pct";
//        String populationFile = runDirectory + "\\berlin-v5.2-1pct.experiencedPlans_withResidence.xml.gz";
//        String networkFile = runDirectory + "\\berlin-v5.2-1pct.output_network.xml.gz";
//        String outputDir = runDirectory + "\\..\\analysisNew_ber_dist";
        
//        String runDirectory = "../../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-1pct";
        String runDirectory = "/Users/dominik/Workspace/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-1pct";
        String populationFile = runDirectory + "/berlin-v5.2-1pct.experiencedPlans_withResidence.xml.gz";
        String networkFile = runDirectory + "/output-berlin-v5.2-1pct/berlin-v5.2-1pct.output_network.xml.gz";
        String outputDir = runDirectory + "/analysisNew_ber_dist_dz";
        // --

//        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//        new PopulationReader(scenario).readFile(populationFile);
//        Population population = scenario.getPopulation();

        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(networkFile);
        config.global().setCoordinateSystem("GK4");
        Scenario scenario1 = ScenarioUtils.loadScenario(config);
        Network network = scenario1.getNetwork();

        ResidenceFilterReader residenceFilterReader = new ResidenceFilterReader(populationFile);
//        Population population = residenceFilterReader.getWholePopulation();
        Population population = residenceFilterReader.filter(ResidenceFilterWriter.INTERIOR_OF_AREA);

        PopulationAnalyzer populationAnalyzer = new PopulationAnalyzer(new PopulationAnalyzerBinWidhtConfig(), population);
        populationAnalyzer.setNetwork(network);
        TripFilter tripFilter = new TripFilter();
        tripFilter.activateDist(0,100);
        populationAnalyzer.setTripFilter(tripFilter);
        populationAnalyzer.analyzeAndWrite(outputDir);
        
        //
        TripSurveyUtils.plotDefaultGnuplots(outputDir, relativePathToGnuplotScript);
        //
    }

    public static Population parseMiD(String midFilePath) throws IOException {

        return Mid2PopulationParser.parsePopulation(new File(midFilePath));
    }

    public static void performDefaultAnalysis(Population population, String outputDirectory) {

        PopulationAnalyzer populationAnalyzer = new PopulationAnalyzer(new PopulationAnalyzerBinWidhtConfig(), population);
        populationAnalyzer.analyzeAndWrite(outputDirectory);
    }

    public static void plotDefaultGnuplots(String outputDirectory, String relativePathToGnuplotScript) {

//        String argument1= "wd_wt_carp_dist";
        String argument1= "";
        GnuplotUtils.runGnuplotScript(outputDirectory, relativePathToGnuplotScript, argument1);
    }
}
