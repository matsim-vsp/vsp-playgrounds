package playground.gthunig.cemdap.cemdapInputTest;

import java.io.IOException;
import java.util.List;

public class ZoneChecker {

    private List<String> zones;

    public ZoneChecker(List<String> zones) {

        this.zones = zones;
    }

    public boolean checkZones(String zone2zoneFile, int csvZoneIndex) throws IOException {

        List<String> toZones = (new ZonesReader(zone2zoneFile, csvZoneIndex).readZones());
        return contains(toZones);
    }

    public boolean contains(List<String> zones2check) {

        boolean everythingOK = true;
        for (String currentZone : zones2check) {
            if (!zones.contains(currentZone) && !currentZone.equals("-99")) {
                everythingOK = false;
                System.out.println("Zone " + currentZone + " is not described in the zonesFile");
            }
        }
        return everythingOK;
    }
}
