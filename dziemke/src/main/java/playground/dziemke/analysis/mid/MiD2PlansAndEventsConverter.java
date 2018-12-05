package playground.dziemke.analysis.mid;


import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import playground.dziemke.analysis.Trip;
import playground.dziemke.analysis.srv.SrV2PlansAndEventsConverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;


public class MiD2PlansAndEventsConverter {

    public static void main(String[] args) throws IOException {

        String midFilePath = "C:\\Users\\gthunig\\VSP\\matsim\\shared-svn\\projects\\nemo_mercator\\data\\original_files\\MID\\MiD2017_Wege_RVR-Gebiet.csv";
        String outputDirectory = "C:\\Users\\gthunig\\VSP\\matsim\\shared-svn\\projects\\nemo_mercator\\data\\original_files\\MID";

        String fromCRS = "EPSG:31468"; // GK4
        String toCRS = "EPSG:31468"; // GK4
        CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(fromCRS, toCRS);

        MiD2PlansAndEventsConverter converter = new MiD2PlansAndEventsConverter();
        List<Trip> trips = converter.readMiDAndCreateTrips(midFilePath);
        TreeMap<Id<Person>, TreeMap<Double, Trip>> personTripsMap = createPersonTripsMap(trips);
        SrV2PlansAndEventsConverter.convert(personTripsMap, //network,
                ct, outputDirectory + "/");
    }

    private List<Trip> readMiDAndCreateTrips(String midPath) throws IOException {

        List<String> midLines = Files.readAllLines(new File(midPath).toPath());
        return createTrips(midLines);
    }

    private List<Trip> createTrips(List<String> midLines) {

        MidTripParser midTripParser = new MidTripParser(midLines.get(0));
        midLines.remove(0);
        for (String midLine : midLines) {
            midTripParser.collectTripLine(midLine);
        }
        return midTripParser.parseTrips();
    }

    private static TreeMap<Id<Person>, TreeMap<Double, Trip>> createPersonTripsMap(List<Trip> trips) {
        TreeMap<Id<Person>, TreeMap<Double, Trip>> personTripsMap = new TreeMap<>();

        for (Trip trip : trips) {
            String personId = trip.getPersonId().toString();
            Id<Person> idPerson = Id.create(personId, Person.class);

            if (!personTripsMap.containsKey(idPerson)) {
                personTripsMap.put(idPerson, new TreeMap<>());
            }

            double departureTime_s = trip.getDepartureTime_s();
            if (personTripsMap.get(idPerson).containsKey(departureTime_s)) {
                throw new RuntimeException("Person may not have two activites ending at the exact same time.");
            } else {
                personTripsMap.get(idPerson).put(departureTime_s, trip);
            }
        }
        return personTripsMap;
    }
}
