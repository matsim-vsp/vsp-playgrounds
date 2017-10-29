package playground.gthunig.BerlinBicycleCounts;

import java.util.ArrayList;

/**
 * @author gthunig on 13.07.2017.
 */
public class BicycleCount {

    private String installation;
    private String direction;
    private String link;
    private String url;
    private String answer;
    private ArrayList<Datapoint> entries = new ArrayList<>();

    String getInstallation() {
        return installation;
    }

    public void setInstallation(String installation) {
        this.installation = installation;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public ArrayList<Datapoint> getEntries() {
        return entries;
    }
}
