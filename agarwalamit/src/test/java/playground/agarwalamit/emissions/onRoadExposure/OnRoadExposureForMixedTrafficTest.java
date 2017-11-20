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

package playground.agarwalamit.emissions.onRoadExposure;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.types.HbefaVehicleCategory;
import org.matsim.contrib.emissions.utils.EmissionSpecificationMarker;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import playground.agarwalamit.analysis.emission.EmissionUtilsExtended;
import playground.agarwalamit.utils.MapUtils;

/**
 * Created by amit on 14.11.17.
 */
@RunWith(Parameterized.class)
public class OnRoadExposureForMixedTrafficTest {

    @Rule
    public final MatsimTestUtils helper = new MatsimTestUtils();
    private static final Logger logger = Logger.getLogger(OnRoadExposureForMixedTrafficTest.class);

    private final boolean isConsideringCO2Costs = false; // no local exposure for co2

    private final QSimConfigGroup.VehiclesSource vehiclesSource;
    private final EventsComparatorForEmissions.EventsOrder eventsOrder;

    public OnRoadExposureForMixedTrafficTest(QSimConfigGroup.VehiclesSource vehiclesSource, EventsComparatorForEmissions.EventsOrder eventsOrder) {
        this.vehiclesSource = vehiclesSource;
        this.eventsOrder = eventsOrder;
        logger.info("Each parameter will be used in all the tests i.e. all tests will be run while inclusing and excluding CO2 costs.");
    }

    @Parameterized.Parameters(name = "{index}: vehicleSource == {0}; EventsOrder == {1};")
    public static List<Object[]> considerCO2 () {
        Object[] [] considerCO2 = new Object [] [] {
                { QSimConfigGroup.VehiclesSource.fromVehiclesData, EventsComparatorForEmissions.EventsOrder.NATURAL_ORDER},
                { QSimConfigGroup.VehiclesSource.fromVehiclesData, EventsComparatorForEmissions.EventsOrder.EMISSION_EVENTS_BEFORE_LINK_LEAVE_EVENT} ,
                { QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData, EventsComparatorForEmissions.EventsOrder.NATURAL_ORDER} ,
                { QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData, EventsComparatorForEmissions.EventsOrder.EMISSION_EVENTS_BEFORE_LINK_LEAVE_EVENT}
        };
        return Arrays.asList(considerCO2);
    }

    /**
     * See the event handler for the details about the manual exposure calculation.
     *
     * TODO : Need to include two situations: (a) agents are not exposed of its own WARM emissions (b) cold emissions except departure link is thorwn at later time but on the departure link until distance travelled is more than 1km.
     */
    @Test
    public void excludeSelfExposureTest() {
        EquilTestSetUp equilTestSetUp = new EquilTestSetUp();
        Scenario sc = equilTestSetUp.createConfigAndReturnScenario();

        Controler controler = getControler(sc, equilTestSetUp);

        OnRoadExposureConfigGroup onRoadExposureConfigGroup = (OnRoadExposureConfigGroup) ConfigUtils.addOrGetModule( sc.getConfig(), OnRoadExposureConfigGroup.class);
        OnRoadExposureHandler onRoadExposureEventHandler = new OnRoadExposureHandler(onRoadExposureConfigGroup, this.eventsOrder);

        EmissionAggregator emissionAggregator = new EmissionAggregator();

        controler.addOverridingModule(new AbstractModule() {

            @Override
            public void install() {
                addEventHandlerBinding().toInstance(onRoadExposureEventHandler);
                addEventHandlerBinding().toInstance(emissionAggregator);
            }
        });

        controler.run();

        if (this.eventsOrder== EventsComparatorForEmissions.EventsOrder.NATURAL_ORDER) {
            // offline calculation
            Map<String, Double> totalInhaledMass_manual = new HashMap<>();
            {
                OnRoadExposureCalculator onRoadExposureCalculator = new OnRoadExposureCalculator(onRoadExposureConfigGroup);

                {
                    //car driver is exposed of cold emiss
                    Map<String, Double> inhaledByCarEmiss = emissionAggregator.coldEmissions.get("car");

                    // since the background concentration=0; travel time does not matter.
                    Map<String, Double> inhaledMass_car = onRoadExposureCalculator.calculate("car", inhaledByCarEmiss, 0.);
                    totalInhaledMass_manual = inhaledMass_car;
                }
                {
                    Map<String, Double> inhaledByBicycleEmiss = MapUtils.mergeMaps(emissionAggregator.warmEmissions.get("bicycle"),
                            emissionAggregator.coldEmissions.get("bicycle"));

                    Map<String, Double> inhaledMass_bicycle = onRoadExposureCalculator.calculate("bicycle", inhaledByBicycleEmiss, 0.);

                    totalInhaledMass_manual = MapUtils.mergeMaps(totalInhaledMass_manual, inhaledMass_bicycle);
                }
            }
            totalInhaledMass_manual.remove("CO2_TOTAL");

            Map<String, Double> totalInhaledMass_sim = onRoadExposureEventHandler.getOnRoadExposureTable().getTotalInhaledMass();
            for (String str : totalInhaledMass_sim.keySet()) {
                Assert.assertEquals("Calculation of inhaled mass of "+str+" is wrong.", totalInhaledMass_manual.get(str), totalInhaledMass_sim.get(str), Math.pow(10,-5));
            }
            totalInhaledMass_sim.entrySet().stream().forEach(e-> System.out.println(e.getKey() + " \t" + e.getValue() ));
        }  else {

        }


    }

    private Controler getControler(Scenario sc, EquilTestSetUp equilTestSetUp){
        List<String> mainModes = Arrays.asList("car", "bicycle");
        equilTestSetUp.createNetwork(sc);

        // allow all modes on the links
        for (Link l : sc.getNetwork().getLinks().values()) {
            l.setAllowedModes(new HashSet<>(mainModes));
        }

        String carPersonId = "567417.1#12424";
        String bikePersonId = "567417.1#12425"; // no emissions
        String bikeVehicleId = bikePersonId;

        if (this.vehiclesSource.equals(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData)) {
            bikeVehicleId = bikePersonId + "_bicycle";
        }

        Vehicles vehs = sc.getVehicles();

        VehicleType car = vehs.getFactory().createVehicleType(Id.create(TransportMode.car, VehicleType.class));
        car.setMaximumVelocity(100.0 / 3.6);
        car.setPcuEquivalents(1.0);
        car.setDescription(EmissionSpecificationMarker.BEGIN_EMISSIONS.toString()
                + HbefaVehicleCategory.PASSENGER_CAR.toString().concat(";petrol (4S);>=2L;PC-P-Euro-0")
                + EmissionSpecificationMarker.END_EMISSIONS.toString() );
        // TODO "&gt;" is an escape character for ">" in xml (http://stackoverflow.com/a/1091953/1359166); need to be very careful with them.
        // thus, reading from vehicles file and directly passing to vehicles container is not the same.
        vehs.addVehicleType(car);

        VehicleType bike = vehs.getFactory().createVehicleType(Id.create("bicycle", VehicleType.class));
        bike.setMaximumVelocity(20. / 3.6);
        bike.setPcuEquivalents(0.25);
        bike.setDescription(EmissionSpecificationMarker.BEGIN_EMISSIONS.toString() +
                HbefaVehicleCategory.ZERO_EMISSION_VEHICLE.toString().concat(";;;") +
                EmissionSpecificationMarker.END_EMISSIONS.toString());
        vehs.addVehicleType(bike);

        if (!this.vehiclesSource.equals(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData)) {
            Vehicle carVeh = vehs.getFactory().createVehicle(Id.createVehicleId(carPersonId), car);
            vehs.addVehicle(carVeh);

            Vehicle bikeVeh = vehs.getFactory().createVehicle(Id.createVehicleId(bikeVehicleId), bike);
            vehs.addVehicle(bikeVeh);
        }

        sc.getConfig().qsim().setMainModes(mainModes);
        sc.getConfig().qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
        sc.getConfig().qsim().setVehiclesSource(this.vehiclesSource);
        sc.getConfig().qsim().setUsePersonIdForMissingVehicleId(true);

        sc.getConfig()
          .plansCalcRoute()
          .getOrCreateModeRoutingParams(TransportMode.pt)
          .setTeleportedModeFreespeedFactor(1.5);
        sc.getConfig().plansCalcRoute().setNetworkModes(mainModes);
        sc.getConfig().planCalcScore().getOrCreateModeParams("bicycle").setConstant(0.0);

        sc.getConfig().travelTimeCalculator().setAnalyzedModes("car,bicycle");
        sc.getConfig().travelTimeCalculator().setFilterModes(true);

        equilTestSetUp.createActiveAgents(sc, carPersonId, TransportMode.car, 6.0 * 3600.);
        equilTestSetUp.createActiveAgents(sc, bikePersonId, "bicycle", 6.0 * 3600. - 5.0);

        emissionSettings(sc);

        Controler controler = new Controler(sc);
        sc.getConfig().controler().setOutputDirectory(helper.getOutputDirectory());

        EmissionsConfigGroup emissionsConfigGroup = ((EmissionsConfigGroup) sc.getConfig()
                                                                              .getModules()
                                                                              .get(EmissionsConfigGroup.GROUP_NAME));
        emissionsConfigGroup.setEmissionEfficiencyFactor(1.0);
        emissionsConfigGroup.setConsideringCO2Costs(isConsideringCO2Costs);
        emissionsConfigGroup.setEmissionCostMultiplicationFactor(1.);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(EmissionModule.class).asEagerSingleton();
            }
        });
        return controler;
    }

    private void emissionSettings(Scenario scenario){
        String inputFilesDir = "../benjamin/test/input/playground/benjamin/internalization/";

        String roadTypeMappingFile = inputFilesDir + "/roadTypeMapping.txt";
        String emissionVehicleFile = inputFilesDir + "/equil_emissionVehicles_1pct.xml.gz";

        String averageFleetWarmEmissionFactorsFile = inputFilesDir + "/EFA_HOT_vehcat_2005average.txt";
        String averageFleetColdEmissionFactorsFile = inputFilesDir + "/EFA_ColdStart_vehcat_2005average.txt";

        boolean isUsingDetailedEmissionCalculation = true;
        String detailedWarmEmissionFactorsFile = inputFilesDir + "/EFA_HOT_SubSegm_2005detailed.txt";
        String detailedColdEmissionFactorsFile = inputFilesDir + "/EFA_ColdStart_SubSegm_2005detailed.txt";

        Config config = scenario.getConfig();
        EmissionsConfigGroup ecg = new EmissionsConfigGroup();
        ecg.setEmissionRoadTypeMappingFile(roadTypeMappingFile);

        scenario.getConfig().vehicles().setVehiclesFile(emissionVehicleFile);

        ecg.setAverageWarmEmissionFactorsFile(averageFleetWarmEmissionFactorsFile);
        ecg.setAverageColdEmissionFactorsFile(averageFleetColdEmissionFactorsFile);

        ecg.setUsingDetailedEmissionCalculation(isUsingDetailedEmissionCalculation);
        ecg.setDetailedWarmEmissionFactorsFile(detailedWarmEmissionFactorsFile);
        ecg.setDetailedColdEmissionFactorsFile(detailedColdEmissionFactorsFile);
        config.addModule(ecg);

        OnRoadExposureConfigGroup onRoadExposureConfigGroup = new OnRoadExposureConfigGroup();
        onRoadExposureConfigGroup.getModeToBreathingRate().put("bicycle",3.06/3600.);
        onRoadExposureConfigGroup.getModeToOccupancy().put("bicycle",1.0);
        onRoadExposureConfigGroup.getPollutantToPenetrationRate("bicycle"); // this will set the default values
        config.addModule(onRoadExposureConfigGroup);
    }

    private class EmissionAggregator implements ColdEmissionEventHandler, WarmEmissionEventHandler {

        private Map<String, Map<String, Double>> coldEmissions = new HashMap<>();
        private Map<String, Map<String, Double>> warmEmissions = new HashMap<>();

        private final EmissionUtilsExtended emissionUtilsExtended = new EmissionUtilsExtended();

        @Override
        public void reset(int iteration){
            this.coldEmissions.clear();
            this.warmEmissions.clear();
        }

        @Override
        public void handleEvent(ColdEmissionEvent event) {
            if (    (event.getLinkId().toString().equals("12") && event.getTime()==21595.0) || // self exposed bicycle
                    (event.getLinkId().toString().equals("45") && event.getTime()==50995.0) //self exposed bicycle
                    ) {
                Map<String, Double> emiss = emissionUtilsExtended.convertColdPollutantMap2String(event.getColdEmissions());
                if (coldEmissions.isEmpty() || coldEmissions.get("bicycle")==null ){
                    coldEmissions.put("bicycle", emiss);
                } else {
                    coldEmissions.put("bicycle", MapUtils.mergeMaps(coldEmissions.get("bicycle"), emiss));
                }
            } else if ( (event.getLinkId().toString().equals("12") && event.getTime()==21600.0) || //self exposed car
                    (event.getLinkId().toString().equals("45") && event.getTime()==51000.0)  //self exposed car
                    ) {
                Map<String, Double> emiss = emissionUtilsExtended.convertColdPollutantMap2String(event.getColdEmissions());
                if (coldEmissions.isEmpty() || coldEmissions.get("car")==null){
                    coldEmissions.put("car", emiss);
                } else {
                    coldEmissions.put("car", MapUtils.mergeMaps(coldEmissions.get("car"), emiss));
                }
            }
        }

        @Override
        public void handleEvent(WarmEmissionEvent event) {
            //when car leave link 23 at 21674.0, bicycle is on the link,--> exposed
            // car leave link 56 at 51038, bicycle is on the link, --> exposed
            if  ( (event.getLinkId().toString().equals("23") && event.getTime()==21674.0 ) || // car emissions--> bicycle exposed.
                    (event.getLinkId().toString().equals("56") && event.getTime()==51038.0 ) // car emissions -> bicycle exposed
                    ) {

                Map<String, Double> emiss = emissionUtilsExtended.convertWarmPollutantMap2String(event.getWarmEmissions());
                if (warmEmissions.isEmpty()){
                    warmEmissions.put("bicycle", emiss);
                } else {
                    warmEmissions.put("bicycle", MapUtils.mergeMaps(warmEmissions.get("bicycle"), emiss));
                }
            }
        }
    }
}
