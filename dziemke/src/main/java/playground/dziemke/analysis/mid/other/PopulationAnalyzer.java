package playground.dziemke.analysis.mid.other;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import playground.dziemke.analysis.AnalysisFileWriter;
import playground.dziemke.analysis.AnalysisUtils;

import java.util.*;

public class PopulationAnalyzer {

    private final Logger log = Logger.getLogger(PopulationAnalyzer.class);

    private final Population population;
    private final String source;
    private final boolean useWeight;

    private Network network;

    private final PopulationAnalyzerBinWidhtConfig config;

    private TripFilter tripFilter;

    private Map<Person, List<Trip>> person2TripsMap = new HashMap<>();

    private AnalysisFileWriter writer = new AnalysisFileWriter();
    private double aggregatedWeightOfConsideredTrips;

    PopulationAnalyzer(PopulationAnalyzerBinWidhtConfig config, Population population) {

        this.config = config;
        this.population = population;
        this.source = SurveyAdditionalAttributesUtils.getSource(population);
        this.useWeight = source.equals(SurveyAdditionalAttributes.Source.SRV.name());
    }

    private double getAggregatedWeight() {

        double aggregatedWeightOfConsideredTrips = 0;

        for (List<Trip> trips : person2TripsMap.values()) {
            for (Trip trip : trips) {
                aggregatedWeightOfConsideredTrips += trip.getWeight();
            }
        }
        return aggregatedWeightOfConsideredTrips;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public void setTripFilter(TripFilter tripFilter) {
        this.tripFilter = tripFilter;
    }

    private boolean isNetworkNeededButNotProvided() {

        return (source.equals(SurveyAdditionalAttributes.Source.MATSIM.name()) && network == null);
    }

    private void printNetworkNotProvidedWarning() {

        log.warn("The source of the population was identified as MATSim but there is no network provided.\n"
                + " Please provide a network with PopulationAnalyzer.setnetwork(Network network) so the calculated (routed and beeline) distance and speed can be analyzed");
    }

    void analyzeAndWrite(String outputDirectory) {

        fillPerson2TripsMap();

        aggregatedWeightOfConsideredTrips = getAggregatedWeight();
        if (isNetworkNeededButNotProvided()) printNetworkNotProvidedWarning();
        else analyzeAndWriteBeelineDistanceAndSpeed(outputDirectory);
        analyzeAndWriteDuration(outputDirectory);
        analyzeAndWriteDepartureTime(outputDirectory);
        analyzeAndWriteActivityTypes(outputDirectory);
        if (source.equals(SurveyAdditionalAttributes.Source.MATSIM.name()) && network != null) {
            analyzeAndWriteRoutedDistanceAndSpeed(outputDirectory, network);
        }
    }

    private void fillPerson2TripsMap() {

        population.getPersons().values().forEach(person -> {

            List<Trip> personsTrips = new ArrayList<>();
            List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
            for (int i = 0; i < planElements.size(); i++) {

                if (planElements.get(i) instanceof Leg) {

                    assert (planElements.get(i-1) instanceof Activity);
                    Activity activityBeforeTrip = (Activity)planElements.get(i-1);
                    Leg leg = (Leg)planElements.get(i);
                    assert (planElements.get(i+1) instanceof Activity);
                    Activity activityAfterTrip = (Activity)planElements.get(i+1);
                    Trip trip = new Trip(activityBeforeTrip, leg, activityAfterTrip, source, useWeight);
                    trip.setNetwork(network);
                    if (tripFilter == null || (tripFilter.isTripValid(trip))) personsTrips.add(trip);
                }
            }

            person2TripsMap.put(person, personsTrips);
        });
    }

    private void analyzeAndWriteDuration(String outputDirectory) {

        Map<Integer, Double> tripDurationMap = new TreeMap<>();
        List<Double> travelTimes = new ArrayList<>();
        person2TripsMap.values().forEach(trips -> {

            trips.forEach(trip -> {

                double travelTime_min = trip.getTravelTime_h() * 60;
                travelTimes.add(travelTime_min);
                AnalysisUtils.addToMapIntegerKeyCeiling(tripDurationMap, travelTime_min, config.getBinWidthDuration_min(), trip.getWeight());
            });
        });

        OptionalDouble average = travelTimes.stream().mapToDouble(a -> a).average();
        double averageTripDuration;
        if (!average.isPresent()) {
            averageTripDuration = -1;
            log.warn("No average trip duration present.");
        } else {
            averageTripDuration = average.getAsDouble();
        }

        writer.writeToFileIntegerKey(tripDurationMap, outputDirectory + "/tripDuration.txt",
                config.getBinWidthDuration_min(), aggregatedWeightOfConsideredTrips, averageTripDuration);
        writer.writeToFileIntegerKeyCumulative(tripDurationMap, outputDirectory + "/tripDurationCumulative.txt",
                config.getBinWidthDuration_min(), aggregatedWeightOfConsideredTrips, averageTripDuration);

    }

    private void analyzeAndWriteDepartureTime(String outputDirectory) {

        Map <Integer, Double> departureTimeMap = new TreeMap<>();

        person2TripsMap.values().forEach(trips -> {

            trips.forEach(trip -> {

                double departureTime_h = trip.getDepartureTime_h();

                // Note: Here, "floor" is used instead of "ceiling". A departure at 6:43 should go into the 6.a.m. bin.
                AnalysisUtils.addToMapIntegerKeyFloor(departureTimeMap, departureTime_h, config.getBinWidthTime_h(), trip.getWeight());

            });
        });

        writer.writeToFileIntegerKey(departureTimeMap, outputDirectory + "/departureTime.txt",
                config.getBinWidthTime_h(), aggregatedWeightOfConsideredTrips, Double.NaN);
    }

    private void analyzeAndWriteActivityTypes(String outputDirectory) {

        Map<String, Double> activityTypeMap = new TreeMap<>();

        person2TripsMap.values().forEach(trips -> {

            trips.forEach(trip -> {

                String activityType = trip.getActivityTypeAfterTrip();
                AnalysisUtils.addToMapStringKey(activityTypeMap, activityType, trip.getWeight());

            });
        });

        writer.writeToFileStringKey(activityTypeMap, outputDirectory + "/activityTypes.txt", aggregatedWeightOfConsideredTrips);
    }

    private void analyzeAndWriteBeelineDistanceAndSpeed(String outputDirectory) {

        //beelineDistance
        Map<Integer, Double> tripDistanceBeelineMap = new TreeMap<>();
        List<Double> beelineDistances = new ArrayList<>();

        //speed
        Map<Integer, Double> averageTripSpeedMap = new TreeMap<>();
        List<Double> speeds = new ArrayList<>();

        person2TripsMap.values().forEach(trips -> {

            trips.forEach(trip -> {

                //beelineDistance
                double distanceBeeline_km = trip.getBeelineDistance_km();
                beelineDistances.add(distanceBeeline_km);
                AnalysisUtils.addToMapIntegerKeyCeiling(tripDistanceBeelineMap, distanceBeeline_km, config.getBinWidthDistance_km(), trip.getWeight());

                //speed
                double speed = trip.getSpeed_km_h();
                speeds.add(speed);
                AnalysisUtils.addToMapIntegerKeyCeiling(averageTripSpeedMap, speed,
                        config.getBinWidthSpeed_km_h(), trip.getWeight());

            });
        });

        //beelineDistance
        OptionalDouble averageDistance = beelineDistances.stream().mapToDouble(a -> a).average();
        double averageTripDistanceBeeline_km;
        if (!averageDistance.isPresent()) {
            averageTripDistanceBeeline_km = -1;
            log.warn("No average trip duration present.");
        } else {
            averageTripDistanceBeeline_km = averageDistance.getAsDouble();
        }
        writer.writeToFileIntegerKey(tripDistanceBeelineMap, outputDirectory + "/tripDistanceBeeline.txt",
                config.getBinWidthDistance_km(), aggregatedWeightOfConsideredTrips, averageTripDistanceBeeline_km);
        writer.writeToFileIntegerKeyCumulative(tripDistanceBeelineMap, outputDirectory + "/tripDistanceBeelineCumulative.txt",
                config.getBinWidthDistance_km(), aggregatedWeightOfConsideredTrips, averageTripDistanceBeeline_km);

        //routedSpeed
        OptionalDouble averageSpeed = speeds.stream().mapToDouble(a -> a).average();
        double averageSpeed_km_h;
        if (!averageSpeed.isPresent()) {
            averageSpeed_km_h = -1;
            log.warn("No average trip duration present.");
        } else {
            averageSpeed_km_h = averageSpeed.getAsDouble();
        }
        writer.writeToFileIntegerKey(averageTripSpeedMap, outputDirectory + "/averageTripSpeedBeeline.txt",
                config.getBinWidthSpeed_km_h(), aggregatedWeightOfConsideredTrips, averageSpeed_km_h);
        writer.writeToFileIntegerKeyCumulative(averageTripSpeedMap, outputDirectory + "/averageTripSpeedBeelineCumulative.txt",
                config.getBinWidthSpeed_km_h(), aggregatedWeightOfConsideredTrips, averageSpeed_km_h);
    }

    private void analyzeAndWriteRoutedDistanceAndSpeed(String outputDirectory, Network network) {

        //routedDistance
        Map<Integer, Double> tripDistanceRoutedMap = new TreeMap<>();
        List<Double> routedDistances = new ArrayList<>();

        //routedSpeed
        Map<Integer, Double> averageTripSpeedRoutedMap = new TreeMap<>();
        List<Double> routedSpeeds = new ArrayList<>();

        person2TripsMap.values().forEach(trips -> {

            trips.forEach(trip -> {

                //routedDistance
                double tripDistanceRouted_km = trip.getRoutedDistance_km();
                routedDistances.add(tripDistanceRouted_km);
                AnalysisUtils.addToMapIntegerKeyCeiling(tripDistanceRoutedMap, tripDistanceRouted_km,
                        config.getBinWidthDistance_km(), trip.getWeight());

                //routedSpeed
                double routedSpeed = trip.getRoutedSpeed_km_h();
                routedSpeeds.add(routedSpeed);
                AnalysisUtils.addToMapIntegerKeyCeiling(averageTripSpeedRoutedMap, routedSpeed,
                        config.getBinWidthSpeed_km_h(), trip.getWeight());

            });
        });

        //routedDistance
        OptionalDouble averageDistance = routedDistances.stream().mapToDouble(a -> a).average();
        double averageTripDistanceRouted_km;
        if (!averageDistance.isPresent()) {
            averageTripDistanceRouted_km = -1;
            log.warn("No average trip duration present.");
        } else {
            averageTripDistanceRouted_km = averageDistance.getAsDouble();
        }
        writer.writeToFileIntegerKey(tripDistanceRoutedMap, outputDirectory + "/tripDistanceRouted.txt",
                config.getBinWidthDistance_km(), aggregatedWeightOfConsideredTrips, averageTripDistanceRouted_km);

        //routedSpeed
        OptionalDouble averageSpeed = routedSpeeds.stream().mapToDouble(a -> a).average();
        double averageRoutedSpeed_km_h;
        if (!averageSpeed.isPresent()) {
            averageRoutedSpeed_km_h = -1;
            log.warn("No average trip duration present.");
        } else {
            averageRoutedSpeed_km_h = averageSpeed.getAsDouble();
        }
        writer.writeToFileIntegerKey(averageTripSpeedRoutedMap, outputDirectory + "/averageTripSpeedRouted.txt",
                config.getBinWidthSpeed_km_h(), aggregatedWeightOfConsideredTrips, averageRoutedSpeed_km_h);
    }

}
