package playground.dziemke.analysis.mid.other;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;
import playground.dziemke.analysis.mid.Mid2PopulationParser;

import java.io.File;
import java.io.IOException;

public class TripSurveyUtils {

    public static void main(String[] args) throws IOException {

        String midFilePath = "C:\\Users\\gthunig\\VSP\\matsim\\shared-svn\\projects\\nemo_mercator\\data\\original_files\\MID\\MiD2017_Wege_RVR-Gebiet.csv";
        String outputDirectory = "C:\\Users\\gthunig\\VSP\\matsim\\shared-svn\\projects\\nemo_mercator\\data\\original_files\\MID";

        Population population = TripSurveyUtils.parseMiD(midFilePath);
        PopulationUtils.writePopulation(population, outputDirectory + "\\plans.xml");
    }

    public static Population parseMiD(String midFilePath) throws IOException {
        return Mid2PopulationParser.parsePopulation(new File(midFilePath));
    }
}
