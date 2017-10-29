package playground.gthunig.BerlinBicycleCounts;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import playground.gthunig.utils.CSVWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;

/**
 * @author gthunig on 13.07.2017.
 */
public class BerlinBicycleCountParser {

    public static void main(String[] args) throws Exception {
        BerlinBicycleCountParser countsParser = new BerlinBicycleCountParser("20170420", "20170420", "3");
        countsParser.getCountsContainer().loadBicycleCountsFrom("C:\\Users\\gthunig\\Desktop\\berlinBicycleCountInstallations.txt");
        for (BicycleCount currentCount : countsParser.getCountsContainer().getCounts()) {
            countsParser.requestCount(currentCount);
        }
        countsParser.parseCounts();
        CountsWriter writer = new CountsWriter(countsParser.getMatsimCounts());
        writer.write("C:\\Users\\gthunig\\Desktop\\berlinBicycleCounts.txt");
    }

    private final static Logger log = Logger.getLogger(BerlinBicycleCountParser.class);

    private BicycleCountContainer countsContainer = new BicycleCountContainer();

    private Counts<Link> matsimCounts = new Counts<>();

    private String startingDay; //f.E. "20160420";
    private String endingDay; //f.E. "20170419";

    // datapointIntervall=2 counts per 15minutes
    // datapointIntervall=3 counts per hour
    // datapointIntervall=4 counts per day
    private String datapointIntervall; //f.E. "3";

    private BerlinBicycleCountParser(String startingDay, String endingDay, String datapointIntervall) {
        this.startingDay = startingDay;
        this.endingDay = endingDay;
        this.datapointIntervall = datapointIntervall;
    }

    public BicycleCountContainer getCountsContainer() {
        return countsContainer;
    }

    public Counts<Link> getMatsimCounts() {
        return this.matsimCounts;
    }

    private void requestCount(BicycleCount count) {
        String urlString = getBicycleCountUrlString(count);

        try {
            URL url = new URL(urlString);
            try {
                InputStreamReader inputStreamReader = new InputStreamReader(url.openStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                log.info("Loading " + count.getInstallation() + " " + count.getDirection() + " ...");

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    System.out.println(line);
                    identifyParseAndAddDatapoints(line, count);
                }

            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }

    private String getBicycleCountUrlString(BicycleCount count) {
        return count.getUrl() + "?begin=" + startingDay + "&end=" + endingDay + "&step=" + datapointIntervall;
    }

    private void identifyParseAndAddDatapoints(String line, BicycleCount count) {
        count.setAnswer(line);
        String[] datapointStrings = line.split("\\},\\{");
        for (String datapointString : datapointStrings) {
            datapointString = removeBracketsFrom(datapointString);
            count.getEntries().add(Datapoint.parseDatapoint(datapointString));
        }
    }

    private String removeBracketsFrom(String line) {
        line = line.replaceAll("}", "");
        line = line.replaceAll("\\{", "");
        line = line.replaceAll("\\[", "");
        line = line.replaceAll("]", "");
        return line;
    }

    private void parseCounts() {

        for (BicycleCount count : countsContainer.getCounts()) {
            Id<Link> linkId = createLinkId(count);
            if (!Objects.equals(linkId.toString(), "")) {
                matsimCounts.createAndAddCount(linkId, createCountName(count));
                for (Datapoint datapoint : count.getEntries()) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(Long.parseLong(datapoint.getTimestamp()));
                    int hour = calendar.get(Calendar.HOUR_OF_DAY)+1;
                    if (hour > 0) {
                        System.out.println(count.getDirection() + ": " + Double.parseDouble(datapoint.getComptage()));
                        //TODO i assume that the count at 0:00 is the count from 0:00-1:00
                        matsimCounts.getCount(linkId).createVolume(hour, Double.parseDouble(datapoint.getComptage()));
                    }
                }
            }
        }
    }

    private Id<Link> createLinkId(BicycleCount count) {
        String linkId = count.getLink();
        //TODO wich kind of link is that?
        return Id.create(linkId, Link.class);
    }

    private String createCountName(BicycleCount count) {
        return count.getInstallation() + "_" + count.getDirection();
    }
}
