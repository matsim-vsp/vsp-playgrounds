package playground.gthunig.cemdap.cemdapOutputAnalysis;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Population;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;

public class CemdapOutputAnalyzer {

    public static void main(String[] args) throws FileNotFoundException {

        CemdapOutputAnalyzer cemdapOutputAnalyzer = new CemdapOutputAnalyzer(
                "C:\\Users\\gthunig\\VSP\\matsim\\shared-svn\\projects\\" +
//                        "nemo_mercator\\data\\matsim_input\\zz_archive\\plans\\2018_01_24\\200\\plans.xml");
                        "nemo_mercator\\data\\matsim_input\\zz_archive\\plans\\2018_02_26\\300\\plans.xml");
        cemdapOutputAnalyzer.analyze();
    }

    public static final Logger LOG = Logger.getLogger(CemdapOutputAnalyzer.class);

    private final String plansFile;
    private Population population;

    public CemdapOutputAnalyzer(String plansFile) {

        this.plansFile = plansFile;
    }

    public void analyze() throws FileNotFoundException {


        double numberOfAgents = 0;
        long numberOfActivities = 0;
        long numberOfHomeActivities = 0;
        long durationOfHomeActivities = 0;
        long numberOfWorkActivities = 0;
        long durationOfWorkActivities = 0;
        long numberOfLeisureActivities = 0;
        long durationOfLeisureActivities = 0;
        long numberOfShoppingActivities = 0;
        long durationOfShoppingActivities = 0;
        long numberOfOtherActivities = 0;
        long durationOfOtherActivities = 0;
        int timeInSeconds = 0;

        try (BufferedReader br = Files.newReader(new File(plansFile), getCorrectCharsetToApply())) {

            LOG.info("Start reading plansFile");
            String line;
            int currentNumberOfActivities = 0;
            boolean collectNumberOfActivities = false;
            while ((line = br.readLine()) != null) {
                if (line.contains("</person>")) {
                    numberOfAgents++;
                }
                if (line.contains("<plan selected=\"yes\">")) {
                    currentNumberOfActivities = 0;
                    collectNumberOfActivities = true;
                }
                if (collectNumberOfActivities && line.contains("<activity type=")) {
                    currentNumberOfActivities++;
                    int lastTimeInSeconds = timeInSeconds;
                    if (line.contains("end_time="))
                        timeInSeconds = getTimeInSeconds(line.split("time=\"")[1].split("\"")[0]);
                    else timeInSeconds = 24 * 3600;
                    if (line.contains("type=\"home")) {
                        numberOfHomeActivities++;
                        durationOfHomeActivities += timeInSeconds - lastTimeInSeconds;
                    }
                    else if (line.contains("type=\"work")) {
                        numberOfWorkActivities++;
                        durationOfWorkActivities += timeInSeconds - lastTimeInSeconds;
                    }
                    else if (line.contains("type=\"leisure")) {
                        numberOfLeisureActivities++;
                        durationOfLeisureActivities += timeInSeconds - lastTimeInSeconds;
                    }
                    else if (line.contains("type=\"shopping")) {
                        numberOfShoppingActivities++;
                        durationOfShoppingActivities += timeInSeconds - lastTimeInSeconds;
                    }
                    else if (line.contains("type=\"other")) {
                        numberOfOtherActivities++;
                        durationOfOtherActivities += timeInSeconds - lastTimeInSeconds;
                    }
                    else LOG.info("Unknown activity: " + line.split("type\"")[1].split("\" x")[0]);
                }
                if (collectNumberOfActivities && line.contains("</plan>")) {
                    numberOfActivities += currentNumberOfActivities;
                    collectNumberOfActivities = false;
                    timeInSeconds = 0;
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        double averageNumberOfActivities = numberOfActivities/numberOfAgents;
        double averageNumberOfHomeActivities = numberOfHomeActivities/numberOfAgents;
        double averageDurationOfHomeActivities = durationOfHomeActivities/(double)numberOfHomeActivities;
        double averageNumberOfWorkActivities = numberOfWorkActivities/numberOfAgents;
        double averageDurationOfWorkActivities = durationOfWorkActivities/(double)numberOfWorkActivities;
        double averageNumberOfLeisureActivities = numberOfLeisureActivities/numberOfAgents;
        double averageDurationOfLeisureActivities = durationOfLeisureActivities/(double)numberOfLeisureActivities;
        double averageNumberOfShoppingActivities = numberOfShoppingActivities/numberOfAgents;
        double averageDurationOfShoppingActivities = durationOfShoppingActivities/(double)numberOfShoppingActivities;
        double averageNumberOfOtherActivities = numberOfOtherActivities/numberOfAgents;
        double averageDurationOfOtherActivities = durationOfOtherActivities/(double)numberOfOtherActivities;

        LOG.info("Number of agents: " + numberOfAgents);
        LOG.info("Average number of activities: " + averageNumberOfActivities);
        LOG.info("Average number of home-activities: " + averageNumberOfHomeActivities);
        LOG.info("Average duration of home-activities: " + convertTimeFromSeconds((int)averageDurationOfHomeActivities));
        LOG.info("Average number of work-activities: " + averageNumberOfWorkActivities);
        LOG.info("Average duration of work-activities: " + convertTimeFromSeconds((int)averageDurationOfWorkActivities));
        LOG.info("Average number of leisure-activities: " + averageNumberOfLeisureActivities);
        LOG.info("Average duration of leisure-activities: " + convertTimeFromSeconds((int)averageDurationOfLeisureActivities));
        LOG.info("Average number of shopping-activities: " + averageNumberOfShoppingActivities);
        LOG.info("Average duration of shopping-activities: " + convertTimeFromSeconds((int)averageDurationOfShoppingActivities));
        LOG.info("Average number of other-activities: " + averageNumberOfOtherActivities);
        LOG.info("Average duration of other-activities: " + convertTimeFromSeconds((int)averageDurationOfOtherActivities));
    }

    private Charset getCorrectCharsetToApply() {
        return Charsets.UTF_8;
    }

    private int getTimeInSeconds(String timeString) {

        String[] units = timeString.split(":"); //will break the string up into an array
        int hours = Integer.parseInt(units[0]); //first element
        int minutes = Integer.parseInt(units[1]); //second element
        int seconds = Integer.parseInt(units[2]); //third element
        return 3600 * hours + 60 * minutes + seconds;
    }

    private String convertTimeFromSeconds(int timeInSeconds) {

        int hours = timeInSeconds / 3600; //first element
        while (timeInSeconds > 3600) timeInSeconds -= 3600;
        int minutes = timeInSeconds /60; //second element
        while (timeInSeconds > 60) timeInSeconds -= 60;
        int seconds = timeInSeconds; //third element
        return hours + ":" + minutes + ":" + seconds;
    }
}
