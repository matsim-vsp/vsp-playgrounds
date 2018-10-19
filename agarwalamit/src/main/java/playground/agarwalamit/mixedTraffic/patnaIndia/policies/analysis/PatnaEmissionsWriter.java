/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.mixedTraffic.patnaIndia.policies.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.types.HbefaVehicleCategory;
import org.matsim.contrib.emissions.utils.EmissionUtils;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import playground.agarwalamit.utils.FileUtils;

/**
 * Created by amit on 23/12/2016.
 */

public class PatnaEmissionsWriter {

    private final String avgColdEmissFile = FileUtils.SHARED_SVN+"projects/detailedEval/matsim-input-files/hbefa-files/v3.2/EFA_ColdStart_vehcat_2005average.txt";
    private final String avgWarmEmissFile = FileUtils.SHARED_SVN+"projects/detailedEval/matsim-input-files/hbefa-files/v3.2/EFA_HOT_vehcat_2005average.txt";

    private static final String roadTypeMappingFile = FileUtils.RUNS_SVN+"patnaIndia/run108/jointDemand/policies/0.15pcu/input/roadTypeMappingFile.txt";
    private static final String networkWithRoadTypeMapping = FileUtils.RUNS_SVN+"patnaIndia/run108/jointDemand/policies/0.15pcu/input/networkWithRoadTypeMapping.xml.gz";

    public static void main(String[] args) {
//        String dir = FileUtils.RUNS_SVN+"patnaIndia/run108/jointDemand/policies/0.15pcu/bau/";
        String dir = args[0];
        PatnaEmissionsWriter pew = new PatnaEmissionsWriter();
        PatnaEmissionsInputGenerator.writeRoadTypeMappingFile(dir+"/output_network.xml.gz", roadTypeMappingFile, networkWithRoadTypeMapping);
        pew.writeEmissionEventsFile(dir);
    }

    private void writeEmissionEventsFile(final String outputDir){

        EmissionsConfigGroup ecg = new EmissionsConfigGroup();
        ecg.setUsingDetailedEmissionCalculation(false);
        ecg.setUsingVehicleTypeIdAsVehicleDescription(false);
        ecg.setAverageColdEmissionFactorsFile(avgColdEmissFile);
        ecg.setAverageWarmEmissionFactorsFile(avgWarmEmissFile);
        ecg.setEmissionRoadTypeMappingFile(roadTypeMappingFile);

        Config config = ConfigUtils.loadConfig(outputDir+"/output_config.xml.gz", ecg);
        config.plans().setInputFile(null);
        config.plans().setInputPersonAttributeFile(null);

        config.network().setInputFile(networkWithRoadTypeMapping);
        config.vehicles().setVehiclesFile("output_vehicles.xml.gz");

        Scenario scenario = ScenarioUtils.loadScenario(config);
        // need to store the vehicle description and also generate vehicles.
        for (VehicleType vt : scenario.getVehicles().getVehicleTypes().values()) {
            HbefaVehicleCategory vehicleCategory = HbefaVehicleCategory.PASSENGER_CAR;
            if (vt.getId().toString().equals(TransportMode.car)) vehicleCategory = HbefaVehicleCategory.PASSENGER_CAR;
            else if (vt.getId().toString().equals(TransportMode.bike)) vehicleCategory = HbefaVehicleCategory.ZERO_EMISSION_VEHICLE;
            else if  (vt.getId().toString().equals("motorbike")) vehicleCategory = HbefaVehicleCategory.MOTORCYCLE;
            else if  (vt.getId().toString().equals("truck")) vehicleCategory = HbefaVehicleCategory.HEAVY_GOODS_VEHICLE;
            else throw new RuntimeException("not implemented yet.");
    
            final String hbefaVehicleDescription = vehicleCategory.toString().concat( ";;;" );
            EmissionUtils.setHbefaVehicleDescription( vt, hbefaVehicleDescription );
        }

        PatnaEmissionVehicleCreatorHandler emissionVehicleCreatorHandler = new PatnaEmissionVehicleCreatorHandler(scenario);

        String emissionEventOutputFile = outputDir + "/output_emission_events.xml.gz";

        EventsManager eventsManager = EventsUtils.createEventsManager();
        EmissionModule emissionModule = new EmissionModule(scenario, eventsManager);
        eventsManager.addHandler(emissionVehicleCreatorHandler);

        EventWriterXML emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
        emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

        MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
        matsimEventsReader.readFile(outputDir+ "/output_events.xml.gz");

        emissionEventWriter.closeFile();

        emissionModule.writeEmissionInformation();
    }
    
    static class PatnaEmissionVehicleCreatorHandler implements PersonDepartureEventHandler {

        private final Scenario scenario;

        PatnaEmissionVehicleCreatorHandler(final Scenario scenario) {
            this.scenario = scenario;
        }

        @Override
        public void reset(int iteration) {

        }

        @Override
        public void handleEvent(PersonDepartureEvent event) {
            if (event.getLegMode().equals(TransportMode.pt) || event.getLegMode().equals(TransportMode.walk)) return;

            VehicleType vt = scenario.getVehicles().getVehicleTypes().get(Id.create(event.getLegMode(), VehicleType.class));
            Id<Vehicle> vehicleId;
            //following is internal thing in population agent source; probbaly, there should be a better way out.
            if(event.getLegMode().equals(TransportMode.car)) {
                vehicleId = Id.createVehicleId(event.getPersonId());
            } else {
                vehicleId = Id.createVehicleId(event.getPersonId()+"_"+event.getLegMode());
            }
            if (! scenario.getVehicles().getVehicles().containsKey(vehicleId)) {
                Vehicle vehicle = VehicleUtils.getFactory().createVehicle(vehicleId, vt);
                scenario.getVehicles().addVehicle(vehicle);
            }
        }
    }
}
