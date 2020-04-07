package playground.dziemke.analysis.mid.other;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.dziemke.analysis.AnalysisFileWriter;
import playground.dziemke.analysis.AnalysisUtils;

public class PopulationAnalyzer {

    private final Logger log = Logger.getLogger(PopulationAnalyzer.class);

    private final Population population;
    private final String source;
    private final boolean useWeight;

    private Network network;

    private final PopulationAnalyzerBinWidhtConfig config;

    private AnalysisFileWriter writer = new AnalysisFileWriter();
    private final double aggregatedWeightOfConsideredTrips;

    PopulationAnalyzer(PopulationAnalyzerBinWidhtConfig config, Population population) {

        this.config = config;
        this.population = population;
        this.source = SurveyAdditionalAttributesUtils.getSource(population);
        this.useWeight = source.equals(SurveyAdditionalAttributes.Source.SRV.name());
        this.aggregatedWeightOfConsideredTrips = getAggregatedWeight();
    }

    private double getAggregatedWeight() {

        double aggregatedWeightOfConsideredTrips = 0;
        for (Person person : population.getPersons().values()) {

            for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {

                if (planElement instanceof Leg) {

                    aggregatedWeightOfConsideredTrips += getWeight((Leg) planElement);
                }
            }
        }
        return aggregatedWeightOfConsideredTrips;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    private boolean isNetworkNeededButNotProvided() {

        return (source.equals(SurveyAdditionalAttributes.Source.MATSIM.name()) && network == null);
    }

    private void printNetworkNotProvidedWarning() {

        log.warn("The source of the population was identified as MATSim but there is no network provided.\n"
                + " Please provide a network with PopulationAnalyzer.setnetwork(Network network) so the calculated (routed and beeline) distance and speed can be analyzed");
    }

    void analyzeAndWrite(String outputDirectory) {

        if (isNetworkNeededButNotProvided()) printNetworkNotProvidedWarning();
        else analyzeAndWriteBeelineDistanceAndSpeed(outputDirectory);
        analyzeAndWriteDuration(outputDirectory);
        analyzeAndWriteDepartureTime(outputDirectory);
        analyzeAndWriteActivityTypes(outputDirectory);
        if (source.equals(SurveyAdditionalAttributes.Source.MATSIM.name()) && network != null) {
            analyzeAndWriteRoutedDistanceAndSpeed(outputDirectory, network);
        }
    }

    private void analyzeAndWriteDuration(String outputDirectory) {

        Map<Integer, Double> tripDurationMap = new TreeMap<>();
        List<Double> travelTimes = new ArrayList<>();
        population.getPersons().values().forEach(person -> {

            for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {

                if (planElement instanceof Leg) {

					double travelTime = ((Leg)planElement).getTravelTime().seconds() / 60;
                    travelTimes.add(travelTime);
                    AnalysisUtils.addToMapIntegerKeyCeiling(tripDurationMap, travelTime, config.getBinWidthDuration_min(), getWeight((Leg) planElement));
                }
            }
        });
        OptionalDouble average = travelTimes.stream().mapToDouble(a -> a).average();
        assert average.isPresent();
        double averageTripDuration = average.getAsDouble();
        writer.writeToFileIntegerKey(tripDurationMap, outputDirectory + "/tripDuration.txt",
                config.getBinWidthDuration_min(), aggregatedWeightOfConsideredTrips, averageTripDuration);
        writer.writeToFileIntegerKeyCumulative(tripDurationMap, outputDirectory + "/tripDurationCumulative.txt",
                config.getBinWidthDuration_min(), aggregatedWeightOfConsideredTrips, averageTripDuration);

    }

    private void analyzeAndWriteDepartureTime(String outputDirectory) {

        Map <Integer, Double> departureTimeMap = new TreeMap<>();
        population.getPersons().values().forEach(person -> {

            for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {

                if (planElement instanceof Leg) {

					double departureTime_h = ((Leg)planElement).getDepartureTime().seconds() / 3600;

        		    // Note: Here, "floor" is used instead of "ceiling". A departure at 6:43 should go into the 6.a.m. bin.
                    AnalysisUtils.addToMapIntegerKeyFloor(departureTimeMap, departureTime_h, config.getBinWidthTime_h(), getWeight((Leg) planElement));
                }
            }
        });


        writer.writeToFileIntegerKey(departureTimeMap, outputDirectory + "/departureTime.txt",
                config.getBinWidthTime_h(), aggregatedWeightOfConsideredTrips, Double.NaN);
    }

    private void analyzeAndWriteActivityTypes(String outputDirectory) {

        Map<String, Double> activityTypeMap = new TreeMap<>();
        population.getPersons().values().forEach(person -> {

            List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
            for (int i = 0; i < planElements.size(); i++) {

                if (planElements.get(i) instanceof Leg) {

                    assert planElements.get(i+1) instanceof Activity;
                    String activityType = ((Activity) planElements.get(i+1)).getType();
                    AnalysisUtils.addToMapStringKey(activityTypeMap, activityType, getWeight((Leg) planElements.get(i)));
                }
            }
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

        population.getPersons().values().forEach(person -> {

            for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {

                if (planElement instanceof Leg) {

                    //beelineDistance
                    double distanceBeeline_km = getBeelineDistance_m((Leg) planElement) / 1000;
                    beelineDistances.add(distanceBeeline_km);
                    AnalysisUtils.addToMapIntegerKeyCeiling(tripDistanceBeelineMap, distanceBeeline_km, config.getBinWidthDistance_km(), getWeight((Leg) planElement));

                    //speed
					double travelTime_h = ((Leg)planElement).getTravelTime().seconds() / 3600;
                    double speed = distanceBeeline_km / travelTime_h;
                    speeds.add(speed);
                    AnalysisUtils.addToMapIntegerKeyCeiling(averageTripSpeedMap, speed,
                            config.getBinWidthSpeed_km_h(), getWeight((Leg) planElement));
                }
            }
        });

        //beelineDistance
        OptionalDouble average = beelineDistances.stream().mapToDouble(a -> a).average();
        assert average.isPresent();
        double averageTripDistanceBeeline_km = average.getAsDouble();
        writer.writeToFileIntegerKey(tripDistanceBeelineMap, outputDirectory + "/tripDistanceBeeline.txt",
                config.getBinWidthDistance_km(), aggregatedWeightOfConsideredTrips, averageTripDistanceBeeline_km);
        writer.writeToFileIntegerKeyCumulative(tripDistanceBeelineMap, outputDirectory + "/tripDistanceBeelineCumulative.txt",
                config.getBinWidthDistance_km(), aggregatedWeightOfConsideredTrips, averageTripDistanceBeeline_km);

        //routedSpeed
        OptionalDouble averageSpeed = speeds.stream().mapToDouble(a -> a).average();
        assert averageSpeed.isPresent();
        double averageSpeed_km_h = averageSpeed.getAsDouble();

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

        population.getPersons().values().forEach(person -> {

            for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {

                if (planElement instanceof Leg) {

                    //routedDistance
                    Route route = ((Leg) planElement).getRoute();
                    if (!(route instanceof NetworkRoute)) return;
                    List<Id<Link>> linkIds =((NetworkRoute) route).getLinkIds();
                    double tripDistance_m = 0;
                    if (linkIds.isEmpty()) {
                        log.warn("List of links is empty.");
                        return;
                    }
                    for (Id<Link> linkId : linkIds) {
                        Link link = network.getLinks().get(linkId);
                        tripDistance_m += link.getLength();
                    }

                    double tripDistanceRouted_km = tripDistance_m / 1000;
                    routedDistances.add(tripDistanceRouted_km);
                    AnalysisUtils.addToMapIntegerKeyCeiling(tripDistanceRoutedMap, tripDistanceRouted_km,
                            config.getBinWidthDistance_km(), getWeight((Leg) planElement));

                    //routedSpeed
					double travelTime_h = ((Leg)planElement).getTravelTime().seconds() / 3600;
                    double routedSpeed = tripDistanceRouted_km / travelTime_h;
                    routedSpeeds.add(routedSpeed);
                    AnalysisUtils.addToMapIntegerKeyCeiling(averageTripSpeedRoutedMap, routedSpeed,
                            config.getBinWidthSpeed_km_h(), getWeight((Leg) planElement));
                }
            }
        });

        //routedDistance
        OptionalDouble averageDistance = routedDistances.stream().mapToDouble(a -> a).average();
        assert averageDistance.isPresent();
        double averageTripDistanceRouted_km = averageDistance.getAsDouble();
        writer.writeToFileIntegerKey(tripDistanceRoutedMap, outputDirectory + "/tripDistanceRouted.txt",
                config.getBinWidthDistance_km(), aggregatedWeightOfConsideredTrips, averageTripDistanceRouted_km);

        //routedSpeed
        OptionalDouble averageSpeed = routedSpeeds.stream().mapToDouble(a -> a).average();
        assert averageSpeed.isPresent();
        double averageRoutedSpeed_km_h = averageSpeed.getAsDouble();

        writer.writeToFileIntegerKey(averageTripSpeedRoutedMap, outputDirectory + "/averageTripSpeedRouted.txt",
                config.getBinWidthSpeed_km_h(), aggregatedWeightOfConsideredTrips, averageRoutedSpeed_km_h);
    }

    private double getWeight(Leg leg) {

        if (useWeight) {
            return SurveyAdditionalAttributesUtils.getWeight(leg);
        } else {
            return 1;
        }
    }

    private double getBeelineDistance_m(Leg leg) {

        //TODO: for the long term use beeline between activities for MATSim source
        if (source.equals(SurveyAdditionalAttributes.Source.MATSIM.name())) {
            assert network != null;
            return calculateBeelineDistance_m(leg.getRoute().getStartLinkId(), leg.getRoute().getEndLinkId());
        } else {
            return SurveyAdditionalAttributesUtils.getDistanceBeeline_m(leg);
        }
    }

    @Deprecated
    private double calculateBeelineDistance_m(Id<Link> departureLinkId, Id<Link> arrivalLinkId) {

        Link departureLink = network.getLinks().get(departureLinkId);
        Link arrivalLink = network.getLinks().get(arrivalLinkId);

        return CoordUtils.calcEuclideanDistance(departureLink.getCoord(), arrivalLink.getCoord());
    }


}
