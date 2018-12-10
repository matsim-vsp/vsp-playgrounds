package playground.dziemke.analysis.mid;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.households.Household;
import playground.dziemke.analysis.mid.other.SurveyAdditionalAttributeHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;

public class Mid2PopulationParser {

    private final static Logger log = Logger.getLogger(Mid2PopulationParser.class);

    private final String SEPERATOR = ";";

    private final String HOUSEHOLD_ID = "H_ID";
    private final String PERSON_ID = "HP_ID";
    private final String TRIP_ID = "W_SID";
    private final String ACTIVITY = "W_ZWECK";
    private final String DEPARTURE_TIME_H = "W_SZS";
    private final String DEPARTURE_TIME_M = "W_SZM";
    private final String ARRIVAL_TIME_H = "W_AZS";
    private final String ARRIVAL_TIME_M = "W_AZM";
    private final String DURATION = "wegmin";
    private final String DISTANCE_BEELINE = "wegkm";
    private final String SPEED = "tempo";
    private final String MODE = "hvm";

    private Map<String, Integer> headerMap = new HashMap<>();
    private Map<Id<Person>, Map<Integer, String>> person2sortedTripId2TripLine = new HashMap<>();

    private String[] currentMidAttributes;

    private PopulationFactory populationFactory;

    public static Population parsePopulation(File midFile) throws IOException {

        List<String> lines = Files.readAllLines(midFile.toPath());
        Mid2PopulationParser parser = new Mid2PopulationParser(lines.get(0));
        lines.remove(0);
        for (String line : lines) {
            parser.collectTripLine(line);
        }
        return parser.parsePopulation();
    }

    private Mid2PopulationParser(String headerLine) {

        initialize(headerLine);
    }

    private void initialize(String headerLine) {

        headerLine = removeEncodingIdicator(headerLine);
        String[] headerAttributes = headerLine.split(SEPERATOR);
        for (int i = 0; i < headerAttributes.length; i++) {
            headerMap.put(headerAttributes[i], i);
        }
    }

    private String removeEncodingIdicator(String headerLine) {

        if (headerLine.startsWith("\uFEFF")) {
            return headerLine.replaceFirst("\uFEFF", "");
        } else {
            return headerLine;
        }
    }

    private void collectTripLine(String line) {

        selectLine(line);
        Id<Person> id = parsePersonId();
        Map<Integer, String> personsLines = person2sortedTripId2TripLine.get(id);
        if (personsLines == null) personsLines = new TreeMap<>();
        Integer tripId = parseTripId();
        personsLines.put(tripId, line);
        person2sortedTripId2TripLine.put(id, personsLines);
    }

    private void selectLine(String line) {

        currentMidAttributes = line.split(SEPERATOR);
    }

    private Population parsePopulation() {

        Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
        population.getAttributes().putAttribute(SurveyAdditionalAttributeHandler.SURVEY, SurveyAdditionalAttributeHandler.MID);

        populationFactory = population.getFactory();

        Iterator iterator = person2sortedTripId2TripLine.entrySet().iterator();
        int personsNotWritten = 0;
        while (iterator.hasNext()) {
            Map.Entry<Id<Person>, Map<Integer, String>> personInformation = (Map.Entry)iterator.next();

            try {

                Person currentPerson = populationFactory.createPerson(personInformation.getKey());
                Plan plan = populationFactory.createPlan();

                Activity home = createActivity("home");
                plan.addActivity(home);

                Iterator personTripIterator = personInformation.getValue().entrySet().iterator();
                while (personTripIterator.hasNext()) {
                    Map.Entry<Integer, String> tripInformation = (Map.Entry) personTripIterator.next();

                    selectLine(tripInformation.getValue());

                    Leg currentLeg = populationFactory.createLeg(parseMode());
                    currentLeg.setDepartureTime(parseDepartureTime_s());
                    currentLeg.setTravelTime(parseDuration_s());
                    currentLeg.getAttributes().putAttribute(
                            SurveyAdditionalAttributeHandler.DISTANCE_BEELINE_M,
                            parseDistanceBeeline_m()
                    );
                    currentLeg.getAttributes().putAttribute(
                            SurveyAdditionalAttributeHandler.SPEED_M_S,
                            parseSpeed_m_s()
                    );
                    plan.addLeg(currentLeg);

                    Activity aimActivity = createActivity(parseActAfterTrip());
                    plan.addActivity(aimActivity);
                    personTripIterator.remove(); // avoids a ConcurrentModificationException
                }

                currentPerson.addPlan(plan);
                population.addPerson(currentPerson);
                iterator.remove(); // avoids a ConcurrentModificationException
            } catch(InadequateInformationException e) {
                log.warn("InadequateInformationException number " + ++personsNotWritten);
            }
        }

        return population;
    }

    private Activity createActivity(String activityType) {

        return populationFactory.createActivityFromCoord(activityType, new Coord(0,0));
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

    private double parseDepartureTime_s() throws InadequateInformationException {

        return parseTime_s(DEPARTURE_TIME_H, DEPARTURE_TIME_M);
    }

    private double parseArrivalTime_s() throws InadequateInformationException {

        return parseTime_s(ARRIVAL_TIME_H, ARRIVAL_TIME_M);
    }

    private double parseTime_s(String columnOfHours, String columnOfMinutes) throws InadequateInformationException {

        try {
            int depHours = Integer.parseInt(currentMidAttributes[headerMap.get(columnOfHours)]);
            int depMinutes = Integer.parseInt(currentMidAttributes[headerMap.get(columnOfMinutes)]);
            int depTime = depHours * 3600 + depMinutes * 60;
            if (depTime > 86400) throw new InadequateInformationException();
            return depTime;
        } catch (Exception e) {
            throw new InadequateInformationException();
        }
    }

    private double parseDuration_s() throws InadequateInformationException {

        int duration = Integer.parseInt(currentMidAttributes[headerMap.get(DURATION)]);
        if (duration > 480) throw new InadequateInformationException();
        return duration * 60;
    }

    private double parseDistanceBeeline_m() throws InadequateInformationException {

        String distanceBeelineString = currentMidAttributes[headerMap.get(DISTANCE_BEELINE)];
        NumberFormat format = NumberFormat.getInstance(Locale.GERMANY);
        Number number = null;
        try {
            number = format.parse(distanceBeelineString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        double distanceBeeline = number.doubleValue();
        if (distanceBeeline > 950) throw new InadequateInformationException();
        return distanceBeeline * 1000;
    }

    private double parseSpeed_m_s() throws InadequateInformationException {

        String tempoString = currentMidAttributes[headerMap.get(SPEED)];
        NumberFormat format = NumberFormat.getInstance(Locale.GERMANY);
        Number number = null;
        try {
            number = format.parse(tempoString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        double tempo = number.doubleValue();
        if (tempo > 640) throw new InadequateInformationException();
        return tempo/3.6;
    }

    private String parseMode() throws InadequateInformationException {

        int modeType = Integer.parseInt(currentMidAttributes[headerMap.get(MODE)]);
        return transformModeType(modeType);
    }

    private String transformModeType(int modeType) throws InadequateInformationException {

        switch (modeType) {
            case 1: return "walk";
            case 2: return "bicycle";
            case 3: return "ride";
            case 4: return "car";
            case 5: return "pt";

            default:
                return "car";
        }
    }

    private class InadequateInformationException extends Throwable {

    }
}
