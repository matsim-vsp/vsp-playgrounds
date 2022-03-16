package playground.gleich.misc;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.BufferedWriter;
import java.io.IOException;

public class WriteRunScripts {

    public static void main( String[] args) {
        int firstRunIdNumber = 551;
        int lastRunIdNumber = 577;
        String runIdPrefix = "i";
        String runIdPostfix = "";

        String preRunId= "#!/bin/bash --login\n" +
                "#$ -l h_rt=792000\n" +
                "#$ -N ";

        String postRunId = "\n" +
                "#$ -o ./logfile_$JOB_NAME.log\n" +
                "#$ -j y\n" +
                "#$ -m be\n" +
                "#$ -M leich@vsp.tu-berlin.de\n" +
                "#$ -cwd\n" +
                "#$ -pe mp 12\n" +
                "#$ -l mem_free=12G\n" +
                "\n" +
                "date\n" +
                "hostname\n" +
                "\n" +
                "runId=$JOB_NAME\n" +
                "runIdToCompare=\"i500\"\n" +
                "\n" +
                "#used memeory Java\n" +
                "java_memory=\"-Xmx130G\"\n" +
                "\n" +
                "classpath=\"matsim-berlin-5.6.x-SNAPSHOT-253cbbd5-jar-with-dependencies.jar\"\n" +
                "\n" +
                "echo \"***\"\n" +
                "echo \"classpath: $classpath\"\n" +
                "echo \"***\"\n" +
                "\n" +
                "# java command\n" +
                "java_command=\"java -Djava.awt.headless=true $java_memory -cp $classpath\"\n" +
                "\n" +
                "# main\n" +
                "main=\"org.matsim.run.drt.RunDrtOpenBerlinScenarioWithDrtSpeedUpAndModeCoverage\"\n" +
                "\n" +
                "# arguments\n" +
                "arguments=\"./berlin-drt-v5.6-10pct.config-intermodal-$JOB_NAME.xml --config:controler.runId $JOB_NAME --config:controler.outputDirectory ./output/output-$JOB_NAME\"\n" +
                "\n" +
                "# command\n" +
                "command=\"$java_command $main $arguments\"\n" +
                "\n" +
                "echo \"\"\n" +
                "echo \"command is $command\"\n" +
                "\n" +
                "echo \"\"\n" +
                "echo \"using alternative java\"\n" +
                "module add java/11\n" +
                "java -version\n" +
                "\n" +
                " $command\n" +
                "\n" +
                "# analysis\n" +
                "\n" +
                "# experienced legs/trips\n" +
                "classpath_LegsTrips=\"gleich-14.0-SNAPSHOT-83bdf629-jar-with-dependencies.jar\"\n" +
                "java_command_LegsTrips=\"java -Djava.awt.headless=true $java_memory -cp $classpath_LegsTrips\"\n" +
                "main_LegsTrips=\"playground.gleich.analysis.experiencedTrips.Events2ExperiencedTripsCSV\"\n" +
                "arguments_LegsTrips=\"/net/ils3/leich/intermod_neu/output/output-$runId/$runId. https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp\"\n" +
                "command_LegsTrips=\"$java_command_LegsTrips $main_LegsTrips $arguments_LegsTrips\"\n" +
                "\n" +
                "# pt analysis\n" +
                "java_command_pt=\"java -Djava.awt.headless=true $java_memory -cp $classpath_LegsTrips\"\n" +
                "main_pt=\"playground.gleich.analysis.pt.stop2stop.RunPtStop2StopOffline\"\n" +
                "arguments_pt=\"/net/ils3/leich/intermod_neu/output/output-$runId/$runId. EPSG:31468\"\n" +
                "command_pt=\"$java_command_pt $main_pt $arguments_pt\"\n" +
                "\n" +
                "# configure matsim-analysis version\n" +
                "classpath=\"matsim-analysis-v3.2-9bb7ba46/matsim-analysis-v3.2.jar\"\n" +
                "\n" +
                "echo \"***\"\n" +
                "echo \"classpath: $classpath\"\n" +
                "echo \"***\"\n" +
                "\n" +
                "# java command\n" +
                "java_command=\"java -Djava.awt.headless=true $java_memory -cp $classpath\"\n" +
                "\n" +
                "# main\n" +
                "main=\"org.matsim.analysis.AnalysisRunExampleOpenBerlinScenarioIntermodal\"\n" +
                "\n" +
                "# arguments\n" +
                "arguments=\"./output/output-$runId $runId ./output/output-$runIdToCompare $runIdToCompare null EPSG:31468 https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/shp-files/shp-stadtteile-split-zone-3/Bezirksregionen_zone_GK4_fixed.shp EPSG:31468 NO https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp EPSG:31468 0.0\"\n" +
                "\n" +
                "# command\n" +
                "command_analysis=\"$java_command $main $arguments\"\n" +
                "\n" +
                "echo \"\"\n" +
                "echo \"command LegsTrips is $command_LegsTrips\"\n" +
                "echo \"command pt analysis is $command_pt\"\n" +
                "echo \"command is $command_analysis\"\n" +
                "\n" +
                "echo \"\"\n" +
                "echo \"command LegsTrips is $command_LegsTrips\"\n" +
                "   $command_LegsTrips\n" +
                "\n" +
                "echo \"\"\n" +
                "echo \"command pt analysis is $command_pt\"\n" +
                "   $command_pt\n" +
                "\n" +
                "echo \"\"\n" +
                "echo \"command is $command_analysis\"\n" +
                "$command_analysis\n" +
                "\n" +
                "echo \"matsim-analysis done.\"\n" +
                "\n" +
                "\n";

        for (int i = firstRunIdNumber; i <= lastRunIdNumber; i++) {
            BufferedWriter writer = IOUtils.getBufferedWriter(runIdPrefix + i + runIdPostfix + ".sh");
            try {
                writer.write(preRunId);
                writer.write(runIdPrefix + i + runIdPostfix);
                writer.write(postRunId);
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
                throw new UncheckedIOException(e);
            }
        }
    }
}
