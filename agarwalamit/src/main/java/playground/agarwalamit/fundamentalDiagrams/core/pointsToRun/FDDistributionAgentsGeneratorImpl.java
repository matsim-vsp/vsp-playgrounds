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

package playground.agarwalamit.fundamentalDiagrams.core.pointsToRun;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.vehicles.VehicleType;
import playground.agarwalamit.fundamentalDiagrams.core.FDConfigGroup;
import playground.agarwalamit.fundamentalDiagrams.core.FDDataContainer;
import playground.agarwalamit.fundamentalDiagrams.core.FDModule;
import playground.agarwalamit.fundamentalDiagrams.core.FDNetworkGenerator;
import playground.agarwalamit.fundamentalDiagrams.core.FDQSimProvider;
import playground.agarwalamit.fundamentalDiagrams.core.FDStabilityTester;

/**
 * Created by amit on 20.05.18.
 */

public class FDDistributionAgentsGeneratorImpl implements  FDAgentsGenerator {

    private final FDConfigGroup FDConfigGroup;
    private final FDNetworkGenerator fdNetworkGenerator;
    private final String[] travelModes;
    private final Map<String, Double> mode2PCUs;
    private final Scenario scenario;

    private final FDDataContainer fdDataContainer;

    @Inject
    FDDistributionAgentsGeneratorImpl(FDNetworkGenerator fdNetworkGenerator, Scenario scenario,
                                      FDDataContainer fdDataContainer, FDStabilityTester stabilityTester){
        this.FDConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(), FDConfigGroup.class);
        this.fdNetworkGenerator = fdNetworkGenerator;
        this.fdDataContainer = fdDataContainer;
        this.travelModes = scenario.getConfig().qsim().getMainModes().toArray(new String[0]);
        this.mode2PCUs = scenario.getVehicles()
                                 .getVehicleTypes()
                                 .values()
                                 .stream()
                                 .collect(Collectors.toMap(v -> v.getId().toString(), VehicleType::getPcuEquivalents));

        Double[] modalShareInPCU = this.FDConfigGroup.getModalShareInPCU().toArray(new Double[0]);

        if (modalShareInPCU.length != this.travelModes.length) {
            FDModule.LOG.warn("Number of modes is not equal to the provided modal share (in PCU). Running for equal modal share");
            modalShareInPCU = new Double[this.travelModes.length];
            Arrays.fill(modalShareInPCU, 1.0);
            this.FDConfigGroup.setModalShareInPCU(
                    Arrays.stream(modalShareInPCU).map(String::valueOf).collect(Collectors.joining(","))
            );
        }

        FDModule.LOG.warn("The modal share in PCU is : " + Arrays.toString(modalShareInPCU));
        this.scenario= scenario;

        init();
    }

    private void init(){

        Integer[] startingPoint = new Integer [travelModes.length];
        Integer [] maxAgentDistribution = new Integer [travelModes.length];
        Integer [] stepSize = new Integer [travelModes.length];

        Arrays.fill(stepSize, this.FDConfigGroup.getReduceDataPointsByFactor());
        Arrays.fill(startingPoint, 1);

        double cellSizePerPCU = this.scenario.getNetwork().getEffectiveCellSize();
        double networkDensity = fdNetworkGenerator.getLengthOfTrack() * FDConfigGroup.getTrackLinkLanes() / cellSizePerPCU;


        IntStream.range(0, travelModes.length)
                 .forEach(index -> maxAgentDistribution[index] = (int) Math.floor(networkDensity / this.mode2PCUs.get(
                         travelModes[index])) + 1);

        int numberOfPoints = IntStream.range(0, travelModes.length)
                                      .map(jj -> (int) Math.floor((maxAgentDistribution[jj] - startingPoint[jj]) / stepSize[jj]) + 1)
                                      .reduce(1, (a, b) -> a * b);

        if(numberOfPoints > 1000) FDModule.LOG.warn("Total number of points to run is "+numberOfPoints+". This may take long time. "
                + "For lesser time to get the data reduce data points by some factor.");

        //Actually going through the n-dimensional grid
        BinaryAdditionModule iterationModule = new BinaryAdditionModule(Arrays.asList(maxAgentDistribution), Arrays.asList(stepSize), startingPoint);
        for (int i=0; i<numberOfPoints; i++){
            Integer[] newPoint = new Integer[maxAgentDistribution.length];
            System.arraycopy(iterationModule.getPoint(), 0, newPoint, 0, newPoint.length);
            fdDataContainer.getListOfPointsToRun().add(Arrays.asList(newPoint));
            FDModule.LOG.info("Just added point "+ Arrays.toString(iterationModule.getPoint()) +" to the collection.");
            if (i<numberOfPoints-1){
                iterationModule.addPoint();
            }
        }
    }

    @Override
    public void createPersons() {
        List<Integer> pointToRun = fdDataContainer.getListOfPointsToRun().remove(0);

        double density = IntStream.range(0, travelModes.length)
                                  .mapToDouble(index -> pointToRun.get(index) * this.mode2PCUs.get(travelModes[index]))
                                  .sum();

        double cellSizePerPCU = this.scenario.getNetwork().getEffectiveCellSize();
        double networkDensity = fdNetworkGenerator.getLengthOfTrack() * FDConfigGroup.getTrackLinkLanes() / cellSizePerPCU;

        if ( density > networkDensity + 5) {
            return;
        }

        FDModule.LOG.info("===============");
        FDModule.LOG.info("Going into run where number of Agents are - \t"+pointToRun);
        FDModule.LOG.info("Further, " + (fdDataContainer.getListOfPointsToRun().size()) +" combinations will be simulated.");
        FDModule.LOG.info("===============");

        Population population = scenario.getPopulation();

        //remove existing persons and person attributes
        population.getPersons().clear();
        population.getPersonAttributes().clear();

        for (int i=0; i<travelModes.length; i++){
            for (int ii = 0; ii < pointToRun.get(i); ii++){
                Id<Person> personId = Id.createPersonId(population.getPersons().size());
                Person person = population.getFactory().createPerson(personId);
                // a blank plan is necessary otherwise VspPlansCleaner will throw a NPE. Amit Apr'18
                person.addPlan(population.getFactory().createPlan());
                population.addPerson(person);
                population.getPersonAttributes().putAttribute(personId.toString(), FDQSimProvider.PERSON_MODE_ATTRIBUTE_KEY, travelModes[i]);
            }
            this.fdDataContainer.getTravelModesFlowData().get(travelModes[i]).setnumberOfAgents(pointToRun.get(i));
        }
        this.fdDataContainer.getGlobalData().setnumberOfAgents(population.getPersons().size());
    }

    private int getGCD(int a, int b){
        if(b==0) return a;
        else return getGCD(b, a%b);
    }

    private int getLCM(int a, int b){
        return a*b/getGCD(a,b);
    }

    private int getGCDOfList(List<Integer> list){
        int i, a, b, gcd;
        a = list.get(0);
        gcd = 1;
        for (i = 1; i < list.size(); i++){
            b = list.get(i);
            gcd = a*b/getLCM(a, b);
            a = gcd;
        }
        return gcd;
    }
}