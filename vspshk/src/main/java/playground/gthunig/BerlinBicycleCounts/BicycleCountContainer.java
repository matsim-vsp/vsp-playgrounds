package playground.gthunig.BerlinBicycleCounts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author gthunig on 13.07.2017.
 */
public class BicycleCountContainer {

    private static final String INSTALLATION = "installation";
    private static final String DIRECTION = "direction";
    private static final String LINK = "link";
    private static final String URL = "url";

    private ArrayList<BicycleCount> counts = new ArrayList<>();

    public ArrayList<BicycleCount> getCounts() {
        return counts;
    }

    public void loadBicycleCountsFrom(String bicycleCountsFile) throws IOException {
        FileReader fileReader = new FileReader(bicycleCountsFile);
        BufferedReader bufferedReader = new BufferedReader(fileReader);

        String line = bufferedReader.readLine();
        while (line != null) {
            if (line.contains("exit"))
                break;
            if (line.contains("skip")) {
                bufferedReader.readLine();
                bufferedReader.readLine();
            }
            if (line.contains("'installation':")) {
                counts.add(parseBicycleCount(line));
            }
            line = bufferedReader.readLine();
        }

        bufferedReader.close();
    }

    public static BicycleCount parseBicycleCount(String line) {
        BicycleCount count = new BicycleCount();
        count.setInstallation(getValueOf(INSTALLATION, line));
        count.setDirection(getValueOf(DIRECTION, line));
        count.setLink(getValueOf(LINK, line));
        count.setUrl(getValueOf(URL, line));
        return count;
    }

    private static String getValueOf(String key, String line) {
        String valueLine = line.split(key + "':\"" )[1];
        return valueLine.split("\"")[0];
    }

}
