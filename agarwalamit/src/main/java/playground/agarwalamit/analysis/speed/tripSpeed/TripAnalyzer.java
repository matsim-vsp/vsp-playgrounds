/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package playground.agarwalamit.analysis.speed.tripSpeed;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.munich.utils.MunichPersonFilter;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.agarwalamit.utils.NumberUtils;
import playground.agarwalamit.utils.PersonFilter;
import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * Created by amit on 20.04.18.
 */

public class TripAnalyzer extends AbstractAnalysisModule {

    private final PersonTripHandler tripHandler ;
    private final String eventsFile;

    private final PersonFilter personFilter = new MunichPersonFilter();

    public TripAnalyzer(Network network, String eventsFile) {
        super(TripAnalyzer.class.getSimpleName());
        this.tripHandler = new PersonTripHandler(network);
        this.eventsFile = eventsFile;
    }

    public static void main(String[] args) {
        String outfile = "../../runs-svn/detEval/emissionCongestionInternalization/iatbr/output/baseCase/analysis/personTripInfo.txt";

        String eventsFile = "../../runs-svn/detEval/emissionCongestionInternalization/iatbr/output/baseCase/ITERS/it.1000/1000.events.xml.gz";
        String networkFile = "../../runs-svn/detEval/emissionCongestionInternalization/iatbr/output/baseCase/output_network.xml.gz";

        Network network = LoadMyScenarios.loadScenarioFromNetwork(networkFile).getNetwork();

        TripAnalyzer tripAnalyzer = new TripAnalyzer(network, eventsFile);
        tripAnalyzer.preProcessData();
        tripAnalyzer.postProcessData();
        tripAnalyzer.writeResults(outfile);
    }

    @Override
    public List<EventHandler> getEventHandler() {
        return Collections.singletonList(this.tripHandler);
    }

    @Override
    public void preProcessData() {
        EventsManager eventsManager = EventsUtils.createEventsManager();
        eventsManager.addHandler(this.tripHandler);
        MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
        reader.readFile(this.eventsFile);
    }

    @Override
    public void postProcessData() {

    }

    @Override
    public void writeResults(String outputFile) {
        try(BufferedWriter writer = IOUtils.getBufferedWriter(outputFile)){
            writer.write("personId\tuserGroup\ttripIndex\ttripMode\ttripDistanceInKm\ttripTravelTimeInHr\ttripSpeedInKPH\n");
            for(List<Trip> trips: this.tripHandler.getPersonToTrip().values()){
                for (Trip  trip : trips){
                    writer.write(trip.getPersonId()+"\t");
                    writer.write(this.personFilter.getUserGroupAsStringFromPersonId(trip.getPersonId())+"\t");
                    writer.write(trip.getTripIndex()+"\t");
                    writer.write(trip.getTripMode()+"\t");
                    writer.write(NumberUtils.round(trip.getDistance()/1000, 3) +"\t");
                    writer.write(NumberUtils.round(trip.getTravelTime()/3600, 2) +"\t");
                    writer.write(NumberUtils.round(trip.getAverageSpeed() * 3.6, 2) + "\n");
                }
            }
            writer.close();
        } catch (IOException e ){
            throw new RuntimeException("Data is not written. Reason: "+ e);
        }
    }
}
