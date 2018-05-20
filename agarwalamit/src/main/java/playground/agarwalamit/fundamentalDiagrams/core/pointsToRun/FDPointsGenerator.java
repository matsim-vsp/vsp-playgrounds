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

import java.util.ArrayList;
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
import org.matsim.core.controler.TerminationCriterion;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.vehicles.VehicleType;
import playground.agarwalamit.fundamentalDiagrams.core.FDDataContainer;
import playground.agarwalamit.fundamentalDiagrams.core.FDNetworkGenerator;
import playground.agarwalamit.fundamentalDiagrams.core.FDQSimProvider;
import playground.agarwalamit.fundamentalDiagrams.core.FDConfigGroup;
import playground.agarwalamit.fundamentalDiagrams.core.FDModule;

/**
 * Created by amit on 20.05.18.
 */

public class FDPointsGenerator implements IterationStartsListener, TerminationCriterion {

    private final FDConfigGroup FDConfigGroup;
    private final FDNetworkGenerator fdNetworkGenerator;
    private final String[] travelModes;
    private Double[] modalShareInPCU;
    private final Map<String, Double> mode2PCUs;
    private final Scenario scenario;

    private final FDDataContainer fdDataContainer;

    @Inject
    FDPointsGenerator(FDNetworkGenerator fdNetworkGenerator, Scenario scenario, FDDataContainer fdDataContainer){
        this.FDConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(), FDConfigGroup.class);
        this.fdNetworkGenerator = fdNetworkGenerator;
        this.fdDataContainer = fdDataContainer;
        this.travelModes = scenario.getConfig().qsim().getMainModes().toArray(new String[0]);
        this.mode2PCUs = scenario.getVehicles()
                                 .getVehicleTypes()
                                 .values()
                                 .stream()
                                 .collect(Collectors.toMap(v -> v.getId().toString(), VehicleType::getPcuEquivalents));

        this.modalShareInPCU = this.FDConfigGroup.getModalShareInPCU().toArray(new Double[0]);

        if (this.modalShareInPCU.length != this.travelModes.length) {
            FDModule.LOG.warn("Number of modes is not equal to the provided modal share (in PCU). Running for equal modal share");
            this.modalShareInPCU = new Double[this.travelModes.length];
            Arrays.fill(this.modalShareInPCU, 1.0);
            this.FDConfigGroup.setModalShareInPCU(
                    Arrays.stream(this.modalShareInPCU).map(String::valueOf).collect(Collectors.joining(","))
            );
        }

        FDModule.LOG.warn("The modal share in PCU is : " + this.modalShareInPCU);
        this.scenario = scenario;
        this.fdDataContainer.getListOfPointsToRun().clear();

        init();
    }

    private void init(){
        List<Double> pcus = Arrays.stream(travelModes)
                                  .map(this.mode2PCUs::get)
                                  .collect(Collectors.toList());

        List<Integer> minSteps = Arrays.stream(modalShareInPCU).map(modalSplit -> (int) (modalSplit * 100))
                                       .collect(Collectors.toList());

        int commonMultiplier = 1;
        for (int i=0; i<travelModes.length; i++){
            double pcu = pcus.get(i);
            //heavy vehicles
            if ( (pcu>1) && (minSteps.get(i)%pcu != 0) ){
                double lcm = getLCM((int) pcu, minSteps.get(i));
                commonMultiplier = (int) (commonMultiplier * lcm/minSteps.get(i) );
            }
        }
        for (int i=0; i<travelModes.length; i++){
            minSteps.set(i, (int) (minSteps.get(i)*commonMultiplier/pcus.get(i)));
        }
        int pgcd = getGCDOfList(minSteps);
        for (int i=0; i<travelModes.length; i++){
            minSteps.set(i, minSteps.get(i)/pgcd);
        }

        if(minSteps.size()==1){
            minSteps.set(0, 1);
        }

        if(FDConfigGroup.getReduceDataPointsByFactor()!=1) {
            for(int index=0;index<minSteps.size();index++){
                minSteps.set(index, minSteps.get(index)* FDConfigGroup.getReduceDataPointsByFactor());
            }
        }

        //set up number of Points to run.
        double networkDensity = fdNetworkGenerator.getLengthOfTrack() * FDConfigGroup.getTrackLinkLanes() / scenario.getNetwork().getEffectiveCellSize();
        double sumOfPCUInEachStep = IntStream.range(0, travelModes.length)
                                             .mapToDouble(index -> minSteps.get(index) * this.mode2PCUs.get(travelModes[index]))
                                             .sum();

        int numberOfPoints = (int) Math.ceil( networkDensity / sumOfPCUInEachStep ) + 5 ;

        for ( int m=1; m<numberOfPoints; m++ ){
            List<Integer> pointToRun = new ArrayList<>();
            for (int i=0; i<travelModes.length; i++){
                pointToRun.add(minSteps.get(i)*m);
            }
            FDModule.LOG.info("Number of Agents - \t"+pointToRun);
            fdDataContainer.getListOfPointsToRun().add(pointToRun);
        }
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        List<Integer> pointToRun = fdDataContainer.getListOfPointsToRun().remove(0);

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

    @Override
    public boolean continueIterations(int iteration) {
        return ! this.fdDataContainer.getListOfPointsToRun().isEmpty();
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