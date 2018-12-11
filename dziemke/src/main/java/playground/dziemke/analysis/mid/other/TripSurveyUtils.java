package playground.dziemke.analysis.mid.other;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;
import playground.dziemke.analysis.mid.Mid2PopulationParser;
import playground.vsp.analysis.utils.GnuplotUtils;

import java.io.File;
import java.io.IOException;

public class TripSurveyUtils {

    public static void main(String[] args) throws IOException {

        String midFilePath = "C:/Users/gthunig/VSP/matsim/shared-svn/projects/nemo_mercator/data/original_files/MID/MiD2017_Wege_RVR-Gebiet.csv";
        String outputDirectory = "C:/Users/gthunig/VSP/matsim/shared-svn/projects/nemo_mercator/data/original_files/MID";
        String relativePathToGnuplotScript = "../../../../cemdapMatsimCadyts/analysis/gnuplot/plot_rel_path_survey_run.gnu";

        Population population = TripSurveyUtils.parseMiD(midFilePath);
        PopulationUtils.writePopulation(population, outputDirectory + "/plans.xml");
        TripSurveyUtils.performDefaultAnalysis(population, outputDirectory);
        TripSurveyUtils.plotDefaultGnuplots(outputDirectory, relativePathToGnuplotScript);
    }

    public static Population parseMiD(String midFilePath) throws IOException {

        return Mid2PopulationParser.parsePopulation(new File(midFilePath));
    }

    public static void performDefaultAnalysis(Population population, String outputDirectory) {

        PopulationAnalyzer populationAnalyzer = new PopulationAnalyzer(new PopulationAnalyzerBinWidhtConfig(), population);
        populationAnalyzer.analyzeAndWrite(outputDirectory);
    }

    public static void plotDefaultGnuplots(String outputDirectory, String relativePathToGnuplotScript) {

        String argument1= "wd_wt_carp_dist";
        GnuplotUtils.runGnuplotScript(outputDirectory, relativePathToGnuplotScript, argument1);
    }
}
