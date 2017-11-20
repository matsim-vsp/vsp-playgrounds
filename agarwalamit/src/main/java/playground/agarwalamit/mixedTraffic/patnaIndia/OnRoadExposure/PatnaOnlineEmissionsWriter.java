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

package playground.agarwalamit.mixedTraffic.patnaIndia.OnRoadExposure;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.types.HbefaVehicleCategory;
import org.matsim.contrib.emissions.utils.EmissionSpecificationMarker;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import playground.agarwalamit.analysis.modalShare.ModalShareControlerListener;
import playground.agarwalamit.analysis.modalShare.ModalShareEventHandler;
import playground.agarwalamit.analysis.tripTime.ModalTravelTimeControlerListener;
import playground.agarwalamit.analysis.tripTime.ModalTripTravelTimeHandler;
import playground.agarwalamit.mixedTraffic.counts.MultiModeCountsControlerListener;
import playground.agarwalamit.mixedTraffic.patnaIndia.policies.PatnaPolicyControler;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForBike;
import playground.agarwalamit.mixedTraffic.patnaIndia.scoring.PtFareEventHandler;
import playground.agarwalamit.utils.FileUtils;

/**
 * Created by amit on 23/12/2016.
 */

public class PatnaOnlineEmissionsWriter {

    private static final String avgColdEmissFile = FileUtils.SHARED_SVN+"projects/detailedEval/matsim-input-files/hbefa-files/v3.2/EFA_ColdStart_vehcat_2005average.txt";
    private static final String avgWarmEmissFile = FileUtils.SHARED_SVN+"projects/detailedEval/matsim-input-files/hbefa-files/v3.2/EFA_HOT_vehcat_2005average.txt";

    public static void main(String[] args) {
//        String dir = FileUtils.RUNS_SVN+"patnaIndia/run108/jointDemand/policies/0.15pcu/bau/";
        String filesDir = args[0];
        String outputDir = args[1];
        String roadTypeMappingFile = args[2];
        String networkWithRoadTypeMapping = args[3];

        Config config = ConfigUtils.loadConfig(filesDir+"/output_config.xml.gz");
        config.network().setInputFile(networkWithRoadTypeMapping);
        config.plans().setInputFile(filesDir+"/output_plans.xml.gz");
        config.plans().setInputPersonAttributeFile(filesDir+"/output_personAttributes.xml.gz");
        config.vehicles().setVehiclesFile(filesDir+"/output_vehicles.xml.gz");

        int lastIt = config.controler().getLastIteration();
        config.controler().setFirstIteration(lastIt);
        config.controler().setLastIteration(lastIt);
        config.controler().setOutputDirectory(outputDir);
        config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);

        EmissionsConfigGroup ecg = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);
        ecg.setUsingDetailedEmissionCalculation(false);
        ecg.setUsingVehicleTypeIdAsVehicleDescription(false);
        ecg.setAverageColdEmissionFactorsFile(avgColdEmissFile);
        ecg.setAverageWarmEmissionFactorsFile(avgWarmEmissFile);
        ecg.setEmissionRoadTypeMappingFile(roadTypeMappingFile);
        ecg.setWritingEmissionsEvents(true);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        // need to store the vehicle description and also generate vehicles.
        for (VehicleType vt : scenario.getVehicles().getVehicleTypes().values()) {
            HbefaVehicleCategory vehicleCategory = HbefaVehicleCategory.PASSENGER_CAR;
            if (vt.getId().toString().equals(TransportMode.car)) vehicleCategory = HbefaVehicleCategory.PASSENGER_CAR;
            else if (vt.getId().toString().equals(TransportMode.bike)) vehicleCategory = HbefaVehicleCategory.ZERO_EMISSION_VEHICLE;
            else if  (vt.getId().toString().equals("motorbike")) vehicleCategory = HbefaVehicleCategory.MOTORCYCLE;
            else if  (vt.getId().toString().equals("truck")) vehicleCategory = HbefaVehicleCategory.HEAVY_GOODS_VEHICLE;
            else throw new RuntimeException("not implemented yet.");

            vt.setDescription(  EmissionSpecificationMarker.BEGIN_EMISSIONS.toString()+
                    vehicleCategory.toString().concat(";;;")+
                    EmissionSpecificationMarker.END_EMISSIONS.toString() );
        }

        final Controler controler = new Controler(scenario);

        controler.getConfig().controler().setDumpDataAtEnd(true);
        controler.getConfig().strategy().setMaxAgentPlanMemorySize(10);

        controler.addOverridingModule(new AbstractModule() { // plotting modal share over iterations
            @Override
            public void install() {
                this.bind(ModalShareEventHandler.class);
                this.addControlerListenerBinding().to(ModalShareControlerListener.class);

                this.bind(ModalTripTravelTimeHandler.class);
                this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);

                this.addControlerListenerBinding().to(MultiModeCountsControlerListener.class);
                bind(EmissionModule.class).asEagerSingleton();
            }
        });

        // adding pt fare system based on distance
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.addEventHandlerBinding().to(PtFareEventHandler.class);
            }
        });
        // for above make sure that util_dist and monetary dist rate for pt are zero.
        PlanCalcScoreConfigGroup.ModeParams mp = controler.getConfig().planCalcScore().getModes().get("pt");
        mp.setMarginalUtilityOfDistance(0.0);
        mp.setMonetaryDistanceRate(0.0);

        // add income dependent scoring function factory
        PatnaPolicyControler.addScoringFunction(controler);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addTravelTimeBinding(TransportMode.bike).to(FreeSpeedTravelTimeForBike.class);
            }
        });

        controler.run();
    }
}
