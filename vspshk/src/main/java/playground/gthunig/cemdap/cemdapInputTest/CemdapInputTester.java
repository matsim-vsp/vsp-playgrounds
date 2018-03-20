package playground.gthunig.cemdap.cemdapInputTest;

import java.io.IOException;
import java.util.List;

public class CemdapInputTester {

    public static void main(String[] args) throws IOException {

        String baseDirectory = "C:\\Users\\gthunig\\VSP\\shared-svn\\studies\\countries\\de\\open_berlin_scenario\\be_4\\cemdap_input\\400\\";
        ZonesReader zonesReader = new ZonesReader(baseDirectory + "zones.dat");
        List<String> zones = zonesReader.readZones();
        //TODO zones as int
        ZoneChecker zoneChecker = new ZoneChecker(zones);
        boolean everythingOK = true;
//        everythingOK = everythingOK && zoneChecker.checkZones( baseDirectory + "zone2zone.dat", 0);
//        everythingOK = everythingOK && zoneChecker.checkZones( baseDirectory + "zone2zone.dat", 1);
//        System.out.println(everythingOK + " with zone2zone");
//        everythingOK = true;
//        everythingOK = everythingOK && zoneChecker.checkZones( baseDirectory + "households.dat", 3);
//        System.out.println(everythingOK + " with households");
//        everythingOK = true;
//        everythingOK = everythingOK && zoneChecker.checkZones( baseDirectory + "losoffpkam.dat", 0);
//        everythingOK = everythingOK && zoneChecker.checkZones( baseDirectory + "losoffpkam.dat", 1);
//        System.out.println(everythingOK + " with losoffpkam");
//        everythingOK = true;
//        everythingOK = everythingOK && zoneChecker.checkZones( baseDirectory + "lospeakam.dat", 0);
//        everythingOK = everythingOK && zoneChecker.checkZones( baseDirectory + "lospeakam.dat", 1);
//        System.out.println(everythingOK + " with lospeakam");
//        everythingOK = true;
        everythingOK = everythingOK && zoneChecker.checkZones( baseDirectory + "persons1.dat", 5);
        everythingOK = everythingOK && zoneChecker.checkZones( baseDirectory + "persons1.dat", 6);
        System.out.println(everythingOK + " with lospeakam");
    }
}
