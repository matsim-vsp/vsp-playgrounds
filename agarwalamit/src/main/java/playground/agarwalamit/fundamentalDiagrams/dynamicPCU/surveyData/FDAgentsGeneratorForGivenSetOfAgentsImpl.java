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

package playground.agarwalamit.fundamentalDiagrams.dynamicPCU.surveyData;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.VehicleType;
import playground.agarwalamit.fundamentalDiagrams.core.FDConfigGroup;
import playground.agarwalamit.fundamentalDiagrams.core.FDDataContainer;
import playground.agarwalamit.fundamentalDiagrams.core.FDModule;
import playground.agarwalamit.fundamentalDiagrams.core.FDNetworkGenerator;
import playground.agarwalamit.fundamentalDiagrams.core.FDQSimProvider;
import playground.agarwalamit.fundamentalDiagrams.core.pointsToRun.FDAgentsGenerator;

/**
 * Created by amit on 20.05.18.
 */

public class FDAgentsGeneratorForGivenSetOfAgentsImpl implements FDAgentsGenerator {

    static final String survey_data_file_place_holder = "survey_data_file";

    private final FDConfigGroup FDConfigGroup;
    private final FDNetworkGenerator fdNetworkGenerator;
    private final String[] travelModes;
    private Double[] modalShareInPCU;
    private final Map<String, Double> mode2PCUs;
    private final Scenario scenario;

    private final FDDataContainer fdDataContainer;
    private final String fileName;

    @Inject
    FDAgentsGeneratorForGivenSetOfAgentsImpl(FDNetworkGenerator fdNetworkGenerator, Scenario scenario, FDDataContainer fdDataContainer,
                                             @Named(survey_data_file_place_holder) String fileName){
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

        FDModule.LOG.warn("The modal share in PCU is : " + Arrays.toString(this.modalShareInPCU));
        this.scenario = scenario;
        this.fdDataContainer.getListOfPointsToRun().clear();
        this.fileName = fileName;

        init();
    }

    private void init(){
        BufferedReader reader = IOUtils.getBufferedReader(fileName);
        try {
            String line = reader.readLine();
            List<String> labels = new ArrayList<>();

            while (line!=null){
                if (labels.isEmpty()) { //headers
                    labels.addAll(Arrays.asList(line.split("\t")));
                } else {
                    List<String> agents = Arrays.asList(line.split("\t"));
                    List<Integer> pointToRun = SurveyDataUtils.modes_coded_in_data_file.stream()
                                                                                       .map(mode -> Integer.valueOf(
                                                                                               agents.get(labels.indexOf(
                                                                                                       mode))))
                                                                                       .collect(Collectors.toList());
                    FDModule.LOG.info("Number of Agents - \t"+pointToRun);
                    fdDataContainer.getListOfPointsToRun().add(pointToRun);
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
    }

    @Override
    public void createPersons() {
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
}