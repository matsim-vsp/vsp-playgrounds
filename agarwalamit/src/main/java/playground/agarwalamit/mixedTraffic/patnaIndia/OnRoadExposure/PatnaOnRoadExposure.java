/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.agarwalamit.mixedTraffic.patnaIndia.OnRoadExposure;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;
import com.vividsolutions.jts.geom.Geometry;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import playground.agarwalamit.emissions.onRoadExposure.OnRoadExposureConfigGroup;
import playground.agarwalamit.emissions.onRoadExposure.OnRoadExposureHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.policies.analysis.PatnaEmissionsInputGenerator;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.kai.usecases.combinedEventsReader.CombinedMatsimEventsReader;

/**
 * Created by amit on 17.11.17.
 */

public class PatnaOnRoadExposure {

    private static final String wardsFile = "../../shared-svn/projects/patnaIndia/inputs/raw/others/wardFile/Wards.shp";
    private static final Collection<SimpleFeature> simpleFeatureCollection = ShapeFileReader.getAllFeatures(wardsFile);
    private static final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(PatnaUtils.EPSG, TransformationFactory.WGS84);
    private static final String networkFile = "";

    private static final Logger LOG = Logger.getLogger(PatnaOnRoadExposure.class);
    private static final boolean writeEmissionEventsFirst = false;

    private static final String data_dates [] = {"none","_22Nov2017","_22Jan2018","_22Mar2018","_22May2018"};

    public static void main(String[] args) {

        for (String date : data_dates) {
            PatnaOnRoadExposure patnaOnRoadExposure = new PatnaOnRoadExposure();

            {
                String outputDir = FileUtils.RUNS_SVN+"/patnaIndia/run111/onRoadExposure/bauLastItr/";
                String filesDir = FileUtils.RUNS_SVN+"/patnaIndia/run108/jointDemand/policies/0.15pcu/bau/";

                if (writeEmissionEventsFirst) {
                    String roadTypeMappingFile = outputDir+"/input/roadTypeMapping.txt";
                    String networkWithRoadType = outputDir+"/input/networkWithRoadTypeMapping.txt";

                    PatnaEmissionsInputGenerator.writeRoadTypeMappingFile(filesDir+"/output_network.xml.gz", roadTypeMappingFile, networkWithRoadType);
                    PatnaOnlineEmissionsWriter.main(new String [] {filesDir, outputDir+"/output/", roadTypeMappingFile, networkWithRoadType});
                }

                patnaOnRoadExposure.run(outputDir + "/output/output_events.xml.gz",
                        outputDir + "/analysis/",
                        LoadMyScenarios.loadScenarioFromNetwork(filesDir + "/output_network.xml.gz").getNetwork(), date);
            }
            {
                String outputDir = FileUtils.RUNS_SVN+"/patnaIndia/run111/onRoadExposure/BT-b_lastItr/";
                String filesDir = FileUtils.RUNS_SVN+"/patnaIndia/run108/jointDemand/policies/0.15pcu/BT-b/";

                if (writeEmissionEventsFirst) {
                    String roadTypeMappingFile = outputDir+"/input/roadTypeMapping.txt";
                    String networkWithRoadType = outputDir+"/input/networkWithRoadTypeMapping.txt";

                    PatnaEmissionsInputGenerator.writeRoadTypeMappingFile(filesDir+"/output_network.xml.gz", roadTypeMappingFile, networkWithRoadType);
                    PatnaOnlineEmissionsWriter.main(new String [] {filesDir, outputDir+"/output/", roadTypeMappingFile, networkWithRoadType});
                }

                patnaOnRoadExposure.run(outputDir + "/output/output_events.xml.gz",
                        outputDir + "/analysis/",
                        LoadMyScenarios.loadScenarioFromNetwork(filesDir + "/output_network.xml.gz").getNetwork(), date);
            }
        }
    }

    private void run(String eventsFile, String outputFilesDir, Network network, String data_date){

        OnRoadExposureConfigGroup onRoadExposureConfigGroup = new OnRoadExposureConfigGroup();
        onRoadExposureConfigGroup.setUsingMicroGramUnits(true);

        if(data_date.equals("_22Nov2017")) {
            onRoadExposureConfigGroup.getPollutantToBackgroundConcentration_gm().put(WarmPollutant.PM.toString(), 217.36 * Math.pow(10,-6));
            onRoadExposureConfigGroup.getPollutantToBackgroundConcentration_gm().put(WarmPollutant.CO.toString(), 1410.0* Math.pow(10,-6));
            onRoadExposureConfigGroup.getPollutantToBackgroundConcentration_gm().put(WarmPollutant.NO2.toString(), 116.79* Math.pow(10,-6));
            onRoadExposureConfigGroup.getPollutantToBackgroundConcentration_gm().put(WarmPollutant.SO2.toString(), 21.44* Math.pow(10,-6));
        } else if (data_date.equals("_22Jan2018")) {
            onRoadExposureConfigGroup.getPollutantToBackgroundConcentration_gm().put(WarmPollutant.PM.toString(), 201.16 * Math.pow(10,-6));
            onRoadExposureConfigGroup.getPollutantToBackgroundConcentration_gm().put(WarmPollutant.CO.toString(), 3470.0* Math.pow(10,-6));
            onRoadExposureConfigGroup.getPollutantToBackgroundConcentration_gm().put(WarmPollutant.NO2.toString(), 44.70* Math.pow(10,-6));
            onRoadExposureConfigGroup.getPollutantToBackgroundConcentration_gm().put(WarmPollutant.SO2.toString(), 49.82* Math.pow(10,-6));
        } else if (data_date.equals("_22Mar2018")) {
            onRoadExposureConfigGroup.getPollutantToBackgroundConcentration_gm().put(WarmPollutant.PM.toString(), 174.79 * Math.pow(10,-6));
            onRoadExposureConfigGroup.getPollutantToBackgroundConcentration_gm().put(WarmPollutant.CO.toString(), 2140.0* Math.pow(10,-6));
            onRoadExposureConfigGroup.getPollutantToBackgroundConcentration_gm().put(WarmPollutant.NO2.toString(), 24.48* Math.pow(10,-6));
            onRoadExposureConfigGroup.getPollutantToBackgroundConcentration_gm().put(WarmPollutant.SO2.toString(), 52.57* Math.pow(10,-6));
        } else if (data_date.equals("_22May2018")) {
            onRoadExposureConfigGroup.getPollutantToBackgroundConcentration_gm().put(WarmPollutant.PM.toString(), 86.54 * Math.pow(10,-6));
            onRoadExposureConfigGroup.getPollutantToBackgroundConcentration_gm().put(WarmPollutant.CO.toString(), 1120.0 * Math.pow(10,-6));
            onRoadExposureConfigGroup.getPollutantToBackgroundConcentration_gm().put(WarmPollutant.NO2.toString(), 7.65 * Math.pow(10,-6));
            onRoadExposureConfigGroup.getPollutantToBackgroundConcentration_gm().put(WarmPollutant.SO2.toString(), 43.58 * Math.pow(10,-6));
        } else {
            onRoadExposureConfigGroup.getPollutantToBackgroundConcentration_gm().put(WarmPollutant.PM.toString(), 0.);
            onRoadExposureConfigGroup.getPollutantToBackgroundConcentration_gm().put(WarmPollutant.CO.toString(), 0.);
            onRoadExposureConfigGroup.getPollutantToBackgroundConcentration_gm().put(WarmPollutant.NO2.toString(), 0.);
            onRoadExposureConfigGroup.getPollutantToBackgroundConcentration_gm().put(WarmPollutant.SO2.toString(), 0.);
            data_date = "_withoutBackgroundConc";
        }

        onRoadExposureConfigGroup.getPollutantToPenetrationRate("motorbike");
        onRoadExposureConfigGroup.getPollutantToPenetrationRate("truck");
        onRoadExposureConfigGroup.getPollutantToPenetrationRate("bike");
        onRoadExposureConfigGroup.getPollutantToPenetrationRate("car");

        onRoadExposureConfigGroup.getModeToOccupancy().put("motorbike",1.0);
        onRoadExposureConfigGroup.getModeToOccupancy().put("truck",1.0);
        onRoadExposureConfigGroup.getModeToOccupancy().put("bike",1.0);
        onRoadExposureConfigGroup.getModeToOccupancy().put("car",1.2);

        onRoadExposureConfigGroup.getModeToBreathingRate().put("motorbike",0.66/3600.0 );
        onRoadExposureConfigGroup.getModeToBreathingRate().put("truck",0.66/3600.0 );
        onRoadExposureConfigGroup.getModeToBreathingRate().put("bike",3.06/3600.0 );
        onRoadExposureConfigGroup.getModeToBreathingRate().put("car",0.66/3600.0 );

        EventsManager eventsManager = EventsUtils.createEventsManager();

        // this will include exposure to agent which leave in the same time step.
        OnRoadExposureHandler onRoadExposureHandler = new OnRoadExposureHandler(onRoadExposureConfigGroup, network);
        eventsManager.addHandler(onRoadExposureHandler);

        CombinedMatsimEventsReader eventsReader = new CombinedMatsimEventsReader(eventsManager);
        eventsReader.readFile(eventsFile);

        TreeSet<String> pollutants = Arrays.stream(WarmPollutant.values())
                                           .map(Enum::toString).collect(Collectors.toCollection(TreeSet::new));

        if (! new File(outputFilesDir).exists()) new File(outputFilesDir).mkdir();
        {
            Map<String, Map<String, Double>> modeToInhaledMass = onRoadExposureHandler.getOnRoadExposureTable().getModeToInhaledMass();
            String outFile = outputFilesDir+"/modeToOnRoadExposure"+ data_date +".txt";
            try (BufferedWriter writer = IOUtils.getBufferedWriter(outFile)) {
                writer.write("mode\t");
                for (String poll : pollutants){
                    writer.write(poll+"\t");
                }
                writer.newLine();
                for (String mode : modeToInhaledMass.keySet()) {
                    writer.write(mode+"\t");
                    for (String poll : pollutants){
                        writer.write( modeToInhaledMass.get(mode).get(poll) + "\t");
                    }
                    writer.newLine();
                }
            } catch (IOException e) {
                throw new RuntimeException("Data is not written/read. Reason : " + e);
            }
            LOG.info("The data has written to "+outFile);
        }

        {
            Map<Id<Person>, Coord> person2homeCoord = getXYForHomeLocationsOfPersons();
            Map<Id<Person>, Map<String, Double>> personToInhaledMass = onRoadExposureHandler.getOnRoadExposureTable().getPersonToInhaledMass();
            String outFile = outputFilesDir+"/personToOnRoadExposure"+ data_date +"_urban.txt";
            try (BufferedWriter writer = IOUtils.getBufferedWriter(outFile)) {
                writer.write("personId\tzoneId\tX\tY\t");
                for (String poll : pollutants){
                    writer.write(poll+"\t");
                }
                writer.newLine();
                for (Id<Person> personId : personToInhaledMass.keySet()) {
                    if (! PatnaPersonFilter.isPersonBelongsToUrban(personId)) continue;

                    Coord coord = person2homeCoord.get(personId);
                    String zoneId;
                    if (coord==null) {
                        writer.write(personId+"\t"+"NA"+"\t"+ "NA"+"\t"+"NA"+"\t");
                    } else {
                        zoneId = getZoneId( coord );
                        writer.write(personId+"\t"+zoneId+"\t"+coord.getX()+"\t"+coord.getY()+"\t");
                    }
                    for (String poll : pollutants){
                        writer.write( personToInhaledMass.get(personId).get(poll) + "\t");
                    }
                    writer.newLine();
                }
            } catch (IOException e) {
                throw new RuntimeException("Data is not written/read. Reason : " + e);
            }
            LOG.info("The data has written to "+outFile);
        }
        {
            Map<Id<Link>, Map<String, Double>> linkToInhaledMass = onRoadExposureHandler.getOnRoadExposureTable().getLinkToInhaledMass();
            String outFile = outputFilesDir+"/linkToOnRoadExposure"+ data_date +".txt";
            try (BufferedWriter writer = IOUtils.getBufferedWriter(outFile)) {
                writer.write("linkId\t");
                for (String poll : pollutants){
                    writer.write(poll+"\t");
                }
                writer.newLine();
                for (Id<Link> linkId : linkToInhaledMass.keySet()) {
                    writer.write(linkId+"\t");
                    for (String poll : pollutants){
                        writer.write( linkToInhaledMass.get(linkId).get(poll) + "\t");
                    }
                    writer.newLine();
                }
            } catch (IOException e) {
                throw new RuntimeException("Data is not written/read. Reason : " + e);
            }
            LOG.info("The data has written to "+outFile);
        }
       onRoadExposureHandler.reset(0);
    }

    private static Map<Id<Person>, Coord> getXYForHomeLocationsOfPersons(){
        String plansFile = FileUtils.RUNS_SVN+"/patnaIndia/run108/jointDemand/policies/0.15pcu/bau/output_plans.xml.gz";
        Population population = LoadMyScenarios.loadScenarioFromPlans(plansFile).getPopulation();
        Map<Id<Person>, Coord> person2homeCoord = new HashMap<>();
        for (Person person : population.getPersons().values()) {
            Coord cord = ((Activity) person.getSelectedPlan().getPlanElements().get(0)).getCoord();
            if (cord!=null){
                person2homeCoord.put(person.getId(), cord);
            } else{
                person2homeCoord.put(person.getId(),null);
            }
        }
        return person2homeCoord;
    }

    private static String getZoneId (Coord cord){
        for(SimpleFeature simpleFeature : simpleFeatureCollection){
            if ( ((Geometry) simpleFeature.getDefaultGeometry()).contains(MGC.coord2Point(cord) ) ) {
                return String.valueOf(simpleFeature.getAttribute("ID1"));
            }
        }
        return "NA";
    }

}
