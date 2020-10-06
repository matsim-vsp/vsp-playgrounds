package playground.gleich.analysis.pt;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunCalculatePtOperatingCostsFromEventsTest {


    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig("C:\\Users\\jakob\\projects\\vsp-playgrounds\\gleich\\src\\main\\java\\playground\\gleich\\analysis\\pt\\input\\config.xml");
        config.controler().setOutputDirectory("C:\\Users\\jakob\\projects\\vsp-playgrounds\\gleich\\src\\main\\java\\playground\\gleich\\analysis\\pt\\output");
        Scenario scenario = ScenarioUtils.loadScenario(config) ;
        Controler controler = new Controler(scenario);
        controler.run();
    }



    @Test
    public void testOnePlan() {

        String networkFile = "C:\\Users\\jakob\\projects\\vsp-playgrounds\\gleich\\src\\main\\java\\playground\\gleich\\analysis\\pt\\output\\output_network.xml.gz";
        String inScheduleFile = "C:\\Users\\jakob\\projects\\vsp-playgrounds\\gleich\\src\\main\\java\\playground\\gleich\\analysis\\pt\\output\\output_transitSchedule.xml.gz";
        String inTransitVehicleFile = "C:\\Users\\jakob\\projects\\vsp-playgrounds\\gleich\\src\\main\\java\\playground\\gleich\\analysis\\pt\\output\\output_transitVehicles.xml.gz";
        String eventsFile = "C:\\Users\\jakob\\projects\\vsp-playgrounds\\gleich\\src\\main\\java\\playground\\gleich\\analysis\\pt\\output\\output_events.xml.gz";
        String shapeFile = "C:\\Users\\jakob\\Desktop\\box3\\box3.shp";


        String coordRefSystem = "epsg:3857";
        String minibusIdentifier = "";

        double costPerHour = 1;
        double costPerKm = 1;
        double costPerDayFixVeh = 1;

        CalculatePtOperatingCostsFromEvents costCalculator = new CalculatePtOperatingCostsFromEvents(networkFile, inScheduleFile, inTransitVehicleFile, coordRefSystem, minibusIdentifier);
        costCalculator.run(eventsFile, shapeFile, costPerHour, costPerKm, costPerDayFixVeh);

        Assert.assertEquals(370.0, costCalculator.kmDriven,0.);
        Assert.assertEquals(2.0, costCalculator.numVehUsed,0.);
        Assert.assertEquals(null, 4.9, costCalculator.pkm,0.);


        // h--> (45) --> 56 --> 66 --> w --> (66) --> 65 --> h,
        //              2400 +  100 +                2400       =  4900 = 4.9 pkm


        // Link 44 --> 45 --> 56 --> 66
        //         <-- 54 <-- 65 <--/
        // 50 * (100 + 1200 + 2400 + 100 + 2400 + 1200) / 1000 = 370 km
        // 370 km / 2 trains = 185 km/veh/day


    }


}
