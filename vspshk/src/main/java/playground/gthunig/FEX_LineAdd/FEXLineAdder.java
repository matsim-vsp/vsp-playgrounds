package playground.gthunig.FEX_LineAdd;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleImpl;
import org.matsim.pt.transitSchedule.api.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gthunig on 29.08.2017.
 */
public class FEXLineAdder {

    public static void main(String[] args) {
        String transitSchedulePath = "C:\\Users\\gthunig\\VSP\\shared-svn\\studies\\countries\\de\\berlin_scenario_2016\\input\\gtfs\\matsim_schedules\\schedule_2017-09-26.xml";
        String outputTransitSchedulePath = "C:\\Users\\gthunig\\VSP\\shared-svn\\studies\\countries\\de\\berlin_scenario_2016\\input\\gtfs\\matsim_schedules\\schedule_2017-09-26_2.xml";

        MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
        scenario.getConfig().transit().setUseTransit(true);

        new TransitScheduleReader(scenario).readFile(transitSchedulePath);
        TransitScheduleImpl transitSchedule = (TransitScheduleImpl) scenario.getTransitSchedule();
        TransitScheduleFactory builder = transitSchedule.getFactory();


        TransitStopFacility berFac = builder.createTransitStopFacility(Id.create("000008010110", TransitStopFacility.class), new Coord(13.503333, 52.366667), false);
        berFac.setName("Flughafen Berlin Brandenburg Bhf");
        transitSchedule.addStopFacility(berFac);

        TransitStopFacility buckowFac = builder.createTransitStopFacility(Id.create("060072101003", TransitStopFacility.class), new Coord(13.382689, 52.410919), false);
        buckowFac.setName("Buckower Chaussee Bhf");
        transitSchedule.addStopFacility(buckowFac);

        TransitStopFacility suedkreuzFac = builder.createTransitStopFacility(Id.create("060058101503", TransitStopFacility.class), new Coord(13.365579, 52.475468), false);
        suedkreuzFac.setName("Südkreuz Bhf");
        transitSchedule.addStopFacility(suedkreuzFac);

        TransitStopFacility potsdamerPlatzFac = builder.createTransitStopFacility(Id.create("060100020455", TransitStopFacility.class), new Coord(13.376454, 52.50934), false);
        potsdamerPlatzFac.setName("Potsdamer Platz Bhf");
        transitSchedule.addStopFacility(potsdamerPlatzFac);

        TransitStopFacility hbfFac = builder.createTransitStopFacility(Id.create("060003201215", TransitStopFacility.class), new Coord(13.368924, 52.525847), false);
        hbfFac.setName("Berlin Hauptbahnhof");
        transitSchedule.addStopFacility(hbfFac);

        TransitLine line = builder.createTransitLine(Id.create("9900_106", TransitLine.class));

        TransitRouteStop berStop = builder.createTransitRouteStop(berFac, 0, 0);
        TransitRouteStop buckowStop = builder.createTransitRouteStop(buckowFac, 300, 300);
        TransitRouteStop suedkreuzStop = builder.createTransitRouteStop(suedkreuzFac, 600, 600);
        TransitRouteStop PotsdamerPlatzStop = builder.createTransitRouteStop(potsdamerPlatzFac, 900, 900);
        TransitRouteStop hbfStop = builder.createTransitRouteStop(hbfFac, 1200, 1200);

        List<TransitRouteStop> stopsToBerlin = new ArrayList<>();
        stopsToBerlin.add(berStop);
        stopsToBerlin.add(buckowStop);
        stopsToBerlin.add(suedkreuzStop);
        stopsToBerlin.add(PotsdamerPlatzStop);
        stopsToBerlin.add(hbfStop);
        TransitRoute routeToBerlin = builder.createTransitRoute(Id.create("9900_106_0", TransitRoute.class),null, stopsToBerlin, "rail");

        long departureId = 59990000;
        for (int i = 0; i<(24*60*60); i += 15*60) {
            Departure departure = builder.createDeparture(Id.create(departureId++, Departure.class), i);
            routeToBerlin.addDeparture(departure);
        }
        line.addRoute(routeToBerlin);

        TransitRouteStop hbfStop2 = builder.createTransitRouteStop(hbfFac, 0, 0);
        TransitRouteStop PotsdamerPlatzStop2 = builder.createTransitRouteStop(potsdamerPlatzFac, 300, 300);
        TransitRouteStop suedkreuzStop2 = builder.createTransitRouteStop(suedkreuzFac, 600, 600);
        TransitRouteStop buckowStop2 = builder.createTransitRouteStop(buckowFac, 900, 900);
        TransitRouteStop berStop2 = builder.createTransitRouteStop(berFac, 1200, 1200);

        List<TransitRouteStop> stopsToBER = new ArrayList<>();
        stopsToBER.add(hbfStop2);
        stopsToBER.add(PotsdamerPlatzStop2);
        stopsToBER.add(suedkreuzStop2);
        stopsToBER.add(buckowStop2);
        stopsToBER.add(berStop2);
        TransitRoute routeToBER = builder.createTransitRoute(Id.create("9900_106_1", TransitRoute.class),null, stopsToBER, "rail");

        for (int i = 0; i<=(24*60*60); i += 15*60) {
            Departure departure = builder.createDeparture(Id.create(departureId++, Departure.class), i);
            routeToBER.addDeparture(departure);
        }

        line.addRoute(routeToBER);

        transitSchedule.addTransitLine(line);

        TransitScheduleWriter writer = new TransitScheduleWriter(transitSchedule);
        writer.writeFile(outputTransitSchedulePath);
    }
}
