package playground.dziemke.analysis.mid;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.households.Household;
import playground.dziemke.analysis.Trip;
import playground.dziemke.cemdapMatsimCadyts.Zone;

import java.util.*;

public class MidTripParser {

    private final static Logger log = Logger.getLogger(MidTripParser.class);

    private final String SEPERATOR = ";";

    private final String HOUSEHOLD_ID = "H_ID";
    private final String PERSON_ID = "HP_ID";
    private final String TRIP_ID = "W_SID";
    private final String ACTIVITY = "W_ZWECK";
    private final String DEPARTURE_TIME_H = "W_SZS";
    private final String DEPARTURE_TIME_M = "W_SZM";
    private final String ARRIVAL_TIME_H = "W_AZS";
    private final String ARRIVAL_TIME_M = "W_AZM";

    private Map<String, Integer> headerMap = new HashMap<>();
    private Map<Id<Person>, Map<Integer, String>> person2sortedTripId2TripLine = new HashMap<>();

    private String[] currentMidAttributes;


    MidTripParser(String headerLine) {

        initialize(headerLine);
    }

    private void initialize(String headerLine) {

        String[] headerAttributes = headerLine.split(SEPERATOR);
        for (int i = 0; i < headerAttributes.length; i++) {
            headerMap.put(headerAttributes[i], i);
        }
    }

    void collectTripLine(String line) {

        selectLine(line);
        Id<Person> id = parsePersonId();
        Map<Integer, String> personsLines = person2sortedTripId2TripLine.get(id);
        if (personsLines == null) personsLines = new TreeMap<>();
        Integer tripId = parseTripId();
        personsLines.put(tripId, line);
    }

    private void selectLine(String line) {

        currentMidAttributes = line.split(SEPERATOR);
    }

    List<Trip> parseTrips() {

        List<Trip> trips = new ArrayList<>();
        Iterator iterator = person2sortedTripId2TripLine.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Id<Person>, Map<Integer, String>> person = (Map.Entry)iterator.next();

            try {
                List<Trip> personTrips = new ArrayList<>();
                Id<Person> personId = person.getKey();

                String activityBeforeTrip = "home";
                Iterator personTripIterator = person2sortedTripId2TripLine.entrySet().iterator();
                while (personTripIterator.hasNext()) {
                    Map.Entry<Integer, String> tripInformation = (Map.Entry) iterator.next();

                    selectLine(tripInformation.getValue());
                    Trip currentTrip = new Trip();
                    currentTrip.setPersonId(personId);
                    currentTrip.setTripId(Id.create(personId.toString() + tripInformation.getKey(), Trip.class));
                    currentTrip.setActivityTypeBeforeTrip(activityBeforeTrip);

                    String activityAfterTrip = parseActAfterTrip();
                    currentTrip.setActivityTypeAfterTrip(activityAfterTrip);
                    activityBeforeTrip = activityAfterTrip;

                    currentTrip.setHouseholdId(parseHouseholdId());
                    currentTrip.setDepartureTime_s(parseDepartureTime_s());
                    currentTrip.setArrivalTime_s(parseArrivalTime_s());

                    currentTrip.setDepartureZoneId(Id.create(0, Zone.class));
                    currentTrip.setArrivalZoneId(Id.create(0, Zone.class));

                    personTrips.add(currentTrip);
                    personTripIterator.remove(); // avoids a ConcurrentModificationException
                }

                trips.addAll(personTrips);
                iterator.remove(); // avoids a ConcurrentModificationException
            } catch(InadequatTimeException e) {
                log.warn("InadequatTimeException");
            }
        }

        return trips;
    }

    private Id<Person> parsePersonId() {

        String idString = currentMidAttributes[headerMap.get(PERSON_ID)];
        return Id.create(idString, Person.class);
    }

    private int parseTripId() {

        String idString = currentMidAttributes[headerMap.get(TRIP_ID)];
        return Integer.parseInt(idString);
    }

    private String parseActAfterTrip() {

        int actTypeCode = Integer.parseInt(currentMidAttributes[headerMap.get(ACTIVITY)]);
        return transformActType(actTypeCode);
    }

    private String transformActType(int actTypeCode) {
        switch (actTypeCode) {
            case 1: return "work";
            case 2: return "work";
            case 3: return "educational";
            case 4: return "shopping";
            case 7: return "leisure";
            case 8: return "home";

            default:
                return "other";
        }
    }

    private Id<Household> parseHouseholdId() {

        String idString = currentMidAttributes[headerMap.get(HOUSEHOLD_ID)];
        return Id.create(idString, Household.class);
    }

    private double parseDepartureTime_s() throws InadequatTimeException {

        return parseTime_s(DEPARTURE_TIME_H, DEPARTURE_TIME_M);
    }

    private double parseArrivalTime_s() throws InadequatTimeException {

        return parseTime_s(ARRIVAL_TIME_H, ARRIVAL_TIME_M);
    }

    private double parseTime_s(String columnOfHours, String columnOfMinutes) throws InadequatTimeException {

        try {
            int depHours = Integer.parseInt(currentMidAttributes[headerMap.get(columnOfHours)]);
            int depMinutes = Integer.parseInt(currentMidAttributes[headerMap.get(columnOfMinutes)]);
            return depHours * 3600 + depMinutes * 60;
        } catch (Exception e) {
            throw new InadequatTimeException();
        }
    }
}
