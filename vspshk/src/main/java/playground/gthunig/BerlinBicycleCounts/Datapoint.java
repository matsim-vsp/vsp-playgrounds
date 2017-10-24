package playground.gthunig.BerlinBicycleCounts;

import org.apache.log4j.Logger;

/**
 * @author gthunig on 13.07.2017.
 */
public class Datapoint {
    private final static Logger log = Logger.getLogger(Datapoint.class);

    private String date;
    private String comptage;
    private String timestamp;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getComptage() {
        return comptage;
    }

    public void setComptage(String comptage) {
        this.comptage = comptage;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public static Datapoint parseDatapoint(String line) {
        Datapoint result = new Datapoint();
        String[] entries = line.split(",");
        for (String entry : entries)
            parseEntry(result, entry);
        return result;
    }

    private static void parseEntry(Datapoint datapoint, String entry) {
        String[] sides = entry.split(":");
        sides = removeQuotationMarks(sides);
        switch (sides[0]) {
            case "date":
                datapoint.setDate(sides[1]);
                break;
            case "comptage":
                datapoint.setComptage(sides[1]);
                break;
            case "timestamp":
                datapoint.setTimestamp(sides[1]);
                break;
            default:
                log.warn("Unknown entry: " + sides[0]);
        }
    }

    private static String[] removeQuotationMarks(String[] sides) {
        for (int i = 0; i < sides.length; i++) {
            sides[i] = sides[i].replaceAll("\"", "");
        }
        return sides;
    }
}
