/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.agarwalamit.utils.plans;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.agarwalamit.utils.PersonFilter;

/**
 * This class checks the initial plans file and check
 * <p> 1) How many persons do not have same first and last activity?
 * <p> 2) Out of them how many are from urban group
 * <p> 3) Also writes the activity sequence of such inconsistent plans and their frequency.
 * <p> 4) How may activities have zero durations.
 * <p> 5) How many activities have end time higher than simulation end time.
 *
 * @author amit
 */
public class OutputPlansConsistencyCheck {
    private static final Logger LOG = Logger.getLogger(OutputPlansConsistencyCheck.class);
    private final Scenario sc;

    private final Map<Person, List<List<String>>> person2PlanIndex2ActivityTypes = new HashMap<>();
    private final Map<Person, List<List<String>>> person2PlanIndex2Legs = new HashMap<>();

    private PersonFilter pf;
    private BufferedWriter writer;

    private final Config config;
    private final String outputDir;

    public OutputPlansConsistencyCheck(String initialPlans, String configFile, String outputDir, PersonFilter pf) {
        config = new Config();
        config.addCoreModules();
        ConfigReader reader = new ConfigReader(config);
        reader.readFile(configFile);

        this.outputDir = outputDir;
        this.sc = LoadMyScenarios.loadScenarioFromPlans(initialPlans);
        this.pf = pf;
    }

    public OutputPlansConsistencyCheck(String initialPlans, String configFile, String outputDir) {
        this(initialPlans, configFile, outputDir, null);
    }

    public static void main(String[] args) {
//        {
//            String initialPlansFile = "/Users/amit/Documents/repos/runs-svn/detEval/emissionCongestionInternalization/input"
//                    + "/mergedPopulation_All_1pct_scaledAndMode_workStartingTimePeakAllCommuter0800Var2h_gk4.xml.gz";
//            String initialConfig = "/Users/amit/Documents/repos/runs-svn/detEval/emissionCongestionInternalization/input/config_munich_1pct_baseCase_modified.xml";
//            String outputFile = "/Users/amit/Documents/repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run7/";
//
//            new InitialPlansConsistencyCheck(initialPlansFile, initialConfig, outputFile, new MunichPersonFilter()).run();
//        }
        {
            String outputPlansFile = "/Users/amit/Documents/cluster/tub/agarwal/nemo/data/locationChoice/run1/output/run1.output_plans.xml.gz";
            String initialConfig = "/Users/amit/Documents/cluster/tub/agarwal/nemo/data/locationChoice/run1/output/run1.output_config.xml";
            String outputFile = "/Users/amit/Documents/cluster/tub/agarwal/nemo/data/locationChoice/run1/output/";
            new OutputPlansConsistencyCheck(outputPlansFile, initialConfig, outputFile).run();
        }
    }

    public void run() {
        if (this.pf!=null) writeUserGroupToPersons();

        // fill maps
        getPersonId2ActivitiesAndLegs();
        writeTestResultsFor1stAndLastActivity();
        writeTestResultsForActivitiesSequences();
        writeTestResultsForActivityDuration();
        writeTestResultsForActivityEndTimeAfterSimulationTime();
    }

    /**
     * Counting number of activities and legs for selected plan of each person.
     */
    private void getPersonId2ActivitiesAndLegs() {
        for (Person p : sc.getPopulation().getPersons().values()) {
            List<List<String>> planIndex2Activities = person2PlanIndex2ActivityTypes.containsKey(p) ? person2PlanIndex2ActivityTypes.get(p) : new ArrayList<>();
            List<List<String>> planIndex2Legs = person2PlanIndex2Legs.containsKey(p) ? person2PlanIndex2Legs.get(p) : new ArrayList<>();
            for (Plan plan: p.getPlans()) {
                int index = p.getPlans().indexOf(plan);

                List<String> acts = new ArrayList<>();
                List<String> legs = new ArrayList<>();

                for (PlanElement pe : plan.getPlanElements()) {
                    if (pe instanceof Activity) {
                        acts.add(((Activity) pe).getType());
                    } else if (pe instanceof Leg) {
                        legs.add(((Leg) pe).getMode());
                    } else {
                        throw new RuntimeException("Plan elements " + pe.toString() + " is not known.");
                    }
                }
                planIndex2Activities.add(index,acts);
                planIndex2Legs.add(index,legs);
            }
            person2PlanIndex2ActivityTypes.put(p,planIndex2Activities);
            person2PlanIndex2Legs.put(p,planIndex2Legs);
        }
    }

    /**
     * Check if activity end time is higher than simulation end time. Such person will be "stuckAndAbort".
     */
    private void writeTestResultsForActivityEndTimeAfterSimulationTime() {
        double simEndTime = config.qsim().getEndTime();
        if ( Double.isInfinite(simEndTime)) {
            LOG.warn("Probably, simulation end time is not defined. Skipping this test...");
            return;
        }

        String outFile = this.outputDir + "analysis/plansConsistency_checkForActEndTime.txt";
        BufferedWriter writer = IOUtils.getBufferedWriter(outFile);

        LOG.info("The outcome of consistency check for activity end time is written to "+ outFile);
        try {
            writer.write("personId \t activityType \t activityEndTime\n");
            for (Person p : sc.getPopulation().getPersons().values()) {
               for (Plan plan : p.getPlans()) {
                   for (PlanElement pe : plan.getPlanElements()) {
                       if (pe instanceof Activity) {
                           double actEndTime = ((Activity) pe).getEndTime();
                           if (actEndTime > simEndTime) {
                               LOG.error("Activity end time is " + actEndTime + " whereas simulation end time is " + simEndTime);
                               writer.write(p.getId() + "\t" + ((Activity) pe).getType() + "\t" + actEndTime + "\n");
                           }
                       }
                   }
               }
            }
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException("Data is not written in file. Reason: " + e);
        }
    }

    /**
     * Check if there is any activity with zero duration  or duration less than zeroUtilityDuration in all plans.
     */
    public void writeTestResultsForActivityDuration() {
        String outFile = this.outputDir + "analysis/plansConsistency_activityDurations.txt";
        BufferedWriter writer = IOUtils.getBufferedWriter(outFile);

        LOG.info("The outcome of consistency check for activity duration is written to "+ outFile);
        int zeroDurCount = 0;
        int zeroUtilDurCount = 0;
        SortedMap<String, Double> actType2ZeroUtilDuration = getZeroUtilDuration();

        Map<String, Integer> actToZeroDurationCount = new HashMap<>();
        Map<String, Integer> actToZeroUtilDurCount = new HashMap<>();

        Map<String, Integer> actType2Counter = new HashMap<>();
        for(String act : config.planCalcScore().getActivityTypes()) {
            if (! act.contains("interaction")) {
                actType2Counter.put(act,0);
                actToZeroDurationCount.put(act,0);
                actToZeroUtilDurCount.put(act,0);
            }
        }

        try {
            writer.write("Person \t planIndex \t activity \t startTime \t endTime \t duration \n");
            for (Person p : sc.getPopulation().getPersons().values()) {
                for (Plan plan : p.getPlans()) {
                    double startTime = Double.NEGATIVE_INFINITY;
                    for ( PlanElement pe : plan.getPlanElements() ) {
                        if ( pe instanceof Activity  ) {
                            Activity act = (Activity) pe;
                            if (act.getType().contains("interaction")) continue;

                            actType2Counter.put(act.getType(), actType2Counter.get(act.getType())+1);

                            if (startTime==Double.NEGATIVE_INFINITY){
                                startTime = act.getEndTime();
                                continue;
                            }

                            double dur = act.getEndTime() - startTime;

                            double zeroUtilDur = actType2ZeroUtilDuration.get(act.getType());
                            if (dur <= 0 && dur!=Double.NEGATIVE_INFINITY) {
                                if (zeroDurCount < 1) {
                                    LOG.warn("Activity duration of person " + p.toString() + " for activity " +
                                            act.getType() + " is zero, it may result in higher utility loss.");
                                    LOG.warn(Gbl.ONLYONCE);
                                }
                                zeroDurCount++;
                                writer.write(p.getId() + "\t" + p.getPlans().indexOf(plan)+ "\t"+ act.getType() + "\t" + startTime +
                                        "\t" + act.getEndTime() + "\t" + dur + "\n");

                                actToZeroDurationCount.put( act.getType(), actToZeroDurationCount.get(act.getType())+1);

                            } else if (dur > 0 && dur<=zeroUtilDur) {
                                if (zeroUtilDurCount < 1) {
                                    LOG.warn("Activity duration of person " + p.toString() + " for activity " +
                                            act.getType() + " is " + dur + ". Utility of performing is zero at (=zero utility duration)" + zeroUtilDur + " sec. Any duration less than this will result in lesser score.");
                                    LOG.warn(Gbl.ONLYONCE);
                                }
                                zeroUtilDurCount++;
                                writer.write(p.getId() + "\t" + p.getPlans().indexOf(plan)+ "\t" + act.getType() + "\t" + startTime +
                                        "\t" + act.getEndTime() + "\t" + zeroUtilDur + "\n");

                                actToZeroUtilDurCount.put( act.getType(), actToZeroUtilDurCount.get(act.getType())+1);

                            }
                            // if dur is negative --> do not carry forward this negative effect to duration of other activities.
                            startTime = Math.max( startTime, act.getEndTime());
                        } else if (pe instanceof  Leg) {
                            startTime += ((Leg) pe).getRoute().getTravelTime(); // this is now --> start time of act after this leg
                        } else {
                            throw new RuntimeException("Plan elements " + pe.toString() + " is not known.");
                        }
                    }
                }
            }
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException("Data is not written. Reason - " + e);
        }
        LOG.warn("There are " + zeroDurCount + " instances where person have activity duration zero. Check for written file for detailed description.");
        LOG.warn("There are " + zeroUtilDurCount + " instances where person have activity duration is below zero utility duration. Check for written file for detailed description.");

        // further aggregation of file
        if (zeroDurCount > 0 || zeroUtilDurCount > 0) {
            outFile = this.outputDir + "analysis/plansConsistency_activityDurations_summary.txt";
            writer = IOUtils.getBufferedWriter(outFile);

            try {
                writer.write("ActType\tzeroUtilDuration\ttotalNumberOfActivities\tcountsForZeroDuration\tcountsForZeroUtilDuration\n");
                for (String act : actType2Counter.keySet()) {
                    writer.write(act + "\t" + actType2ZeroUtilDuration.get(act) + "\t" + actType2Counter.get(act) + "\t" + actToZeroDurationCount
                            .get(act) + "\t" + actToZeroUtilDurCount.get(act) + "\n");
                }
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException("Data is not written/read. Reason : " + e);
            }
        }
    }

    public void writeTestResultsForActivitiesSequences() {
        int numberOfPlansInChoiceSet = this.config.strategy().getMaxAgentPlanMemorySize();
        for (int index =0; index<numberOfPlansInChoiceSet; index++){
            String outFile = this.outputDir + "analysis/plansConsistency_CountsForActivitySequences_planIndex_"+index+".txt";
            BufferedWriter writer = IOUtils.getBufferedWriter(outFile);

            LOG.info("The activity sequence and their counts are written to  " + outFile);

            SortedMap<String, Integer> actSeq2Count = new TreeMap<>();

            int finalIndex = index;
            person2PlanIndex2ActivityTypes
                    .entrySet()
                    .stream()
                    .map(e -> e.getValue().get(finalIndex))
                    .forEach(acts -> actSeq2Count.put(acts.toString(), actSeq2Count.containsKey(acts.toString()) ? actSeq2Count.get(acts.toString()) + 1 : 1));
            try {
                writer.write("act Sequence \t count \n");
                for (String str : actSeq2Count.keySet()) {
                    writer.write(str + "\t" + actSeq2Count.get(str) + "\n");
                }
                writer.close();
            } catch (Exception e) {
                throw new RuntimeException("Data is not written to file. Reason " + e);
            }
        }
    }

    /**
     * Check if first and last activities are same or not. Report the numbers.
     * Also report the numbers for each user group.
     */
    public void writeTestResultsFor1stAndLastActivity() {
        int numberOfPlansInChoiceSet = this.config.strategy().getMaxAgentPlanMemorySize();
        for (int index =0; index<numberOfPlansInChoiceSet; index++){
            String outFile = this.outputDir + "analysis/plansConsistency_DifferentFirstAndLastActivities_planIndex"+index+".txt";
            LOG.info("The outcome of consistency check for equality of first and last activity in a plan is written to " + outFile);

            SortedMap<String, Integer> userGroupToDiffActsCount = new TreeMap<>();

            int warnCount = 0;
            for (Person p : person2PlanIndex2ActivityTypes.keySet()) {
                List<String> acts = person2PlanIndex2ActivityTypes.get(p).get(index);
                if (!acts.get(0).equals(acts.get(acts.size() - 1))) {
                    warnCount++;

                    if (pf != null) {
                        String ug = this.pf.getUserGroupAsStringFromPersonId(p.getId());
                        userGroupToDiffActsCount.put(ug,
                                userGroupToDiffActsCount.containsKey(ug) ? userGroupToDiffActsCount.get(ug) + 1 : 1);
                    }
                }
            }

            if (warnCount==0) return;

            BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
            try {
                writer.write("Number of persons not having first and last activity same \t " + warnCount + "\n");
                for (String ug : userGroupToDiffActsCount.keySet()) {
                    writer.write("Number of such persons from " + ug + " population \t " + userGroupToDiffActsCount.get(ug) + "\n");
                }
                writer.close();
            } catch (Exception e) {
                throw new RuntimeException("Data is not written to file. Reason " + e);
            }
        }
    }

    public void writeUserGroupToPersons() {
        if (this.pf == null) return;

        String outFile = this.outputDir + "analysis/plansConsistency_userGroupToPersonsCount.txt";
        LOG.info("Writing user group to persons count to " + outFile);
        Map<String, Integer> userGroup2NumberOfPersons = getUserGrp2NumberOfPersons();

        BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
        try {
            writer.write("UserGroup \t numberOfPersons \n");
            for (String ug : userGroup2NumberOfPersons.keySet()) {
                writer.write(ug + "\t" + userGroup2NumberOfPersons.get(ug) + "\n");
            }
            writer.write("Total persons \t " + userGroup2NumberOfPersons.values()
                                                                        .stream()
                                                                        .reduce(0, Integer::sum) + "\n");
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException("Data is not written to file. Reason " + e);
        }
    }

    /**
     * Simply counts the number of persons belongs to each user group.
     */
    private Map<String, Integer> getUserGrp2NumberOfPersons() {
        Map<String, Integer> userGroup2NumberOfPersons = new HashMap<>();
        sc.getPopulation()
          .getPersons()
          .values()
          .stream()
          .map(p -> pf.getUserGroupAsStringFromPersonId(p.getId()))
          .forEach(ug -> {
              if (userGroup2NumberOfPersons.containsKey(ug)) {
                  userGroup2NumberOfPersons.put(ug, userGroup2NumberOfPersons.get(ug) + 1);
              } else {
                  userGroup2NumberOfPersons.put(ug, 1);
              }
          });
        return userGroup2NumberOfPersons;
    }

    private SortedMap<String, Double> getZeroUtilDuration() {
        PlanCalcScoreConfigGroup params = config.planCalcScore();

        SortedMap<String, Double> actType2ZeroUtilDuration = new TreeMap<>();
        params.getActivityTypes()
              .stream()
              .filter(actType -> ! actType.contains("interaction"))
              .forEach(actType -> {
                  ActivityUtilityParameters.Builder builder = new ActivityUtilityParameters.Builder(params.getActivityParams(
                          actType));
                  ActivityUtilityParameters ppp = builder.build();
                  double zeroUtilDurSec = ppp.getZeroUtilityDuration_h() * 3600.;
                  actType2ZeroUtilDuration.put(actType, zeroUtilDurSec);
        });
        return actType2ZeroUtilDuration;
    }
}
