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

package playground.agarwalamit.opdyts.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.analysis.modalShare.ModalShareFromPlans;
import playground.agarwalamit.analysis.tripDistance.LegModeBeelineDistanceDistributionFromPlansAnalyzer;
import playground.agarwalamit.opdyts.DistanceDistribution;
import playground.agarwalamit.opdyts.ObjectiveFunctionEvaluator;
import playground.agarwalamit.opdyts.equil.EquilMixedTrafficObjectiveFunctionPenalty;
import playground.agarwalamit.opdyts.plots.BestSolutionVsDecisionVariableChart;
import playground.agarwalamit.opdyts.plots.OpdytsConvergenceChart;

/**
 * Created by amit on 20/09/16.
 */

public class OpdytsModalStatsControlerListener implements StartupListener, ShutdownListener, BeforeMobsimListener, AfterMobsimListener {

    @Inject
    private Scenario scenario;

    @Inject
    private ObjectiveFunctionEvaluator objectiveFunctionEvaluator;

    public static final String OPDYTS_STATS_LABEL_STARTER = "iterationNr";
    public static final String OPDYTS_STATS_FILE_NAME = "opdyts_modalStats";

    private final DistanceDistribution referenceStudyDistri ;
    private final List<Double> dists;
    private final Map<String, double[]> realCounts;

    private final SortedMap<String, SortedMap<Double, Integer>> initialMode2DistanceClass2LegCount = new TreeMap<>();

    private BufferedWriter writer;
    private final Collection<String> modes2consider;

    /*
     * after every 10/20/50 iterations, distance distribution will be written out.
     */
    private int writeDistanceDistributionEveryIteration = 5;

    private double fromStateObjFunValue = 0.; // useful only to check if all decision variables start at the same point.

    public OpdytsModalStatsControlerListener(final Collection<String> modes2consider, final DistanceDistribution referenceStudyDistri) {
        this.modes2consider = modes2consider;
        this.referenceStudyDistri = referenceStudyDistri;
        this.dists = Arrays.stream(this.referenceStudyDistri.getDistClasses()).boxed().collect(Collectors.toList());
        this.realCounts = this.referenceStudyDistri.getMode2DistanceBasedLegs();
    }

//    public OpdytsModalStatsControlerListener() {
//        this(Arrays.asList(TransportMode.car, TransportMode.pt), null);
//    }

    @Override
    public void notifyStartup(StartupEvent event) {

        StringBuilder stringBuilder = new StringBuilder(OPDYTS_STATS_LABEL_STARTER + "\t");
        stringBuilder.append("fromStateObjFunValue"+"\t");
        modes2consider.forEach(mode -> stringBuilder.append("legs_").append(mode).append("\t"));
        modes2consider.forEach(mode -> stringBuilder.append("asc_").append(mode).append("\t"));
        modes2consider.forEach(mode -> stringBuilder.append("util_trav_").append(mode).append("\t"));
        modes2consider.forEach(mode -> stringBuilder.append("util_dist_").append(mode).append("\t"));
        modes2consider.forEach(mode -> stringBuilder.append("money_dist_rate_").append(mode).append("\t"));
        stringBuilder.append("objectiveFunctionValue"+"\t");
        stringBuilder.append("penaltyForObjectiveFunction"+"\t");
        stringBuilder.append("totalObjectiveFunctionValue");

        String outFile = event.getServices().getConfig().controler().getOutputDirectory() + "/"+OPDYTS_STATS_FILE_NAME+".txt";
        writer = IOUtils.getBufferedWriter(outFile);
        try {
            writer.write(stringBuilder.toString());
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("File not found.");
        }
    }

    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent event) {
       final int iteration = event.getIteration();
       final Config config = event.getServices().getConfig();

       LegModeBeelineDistanceDistributionFromPlansAnalyzer beelineDistanceDistributionFromPlansAnalyzer = getBeelineDistanceDistributionHandler();

        // initializing it here so that scenario can be accessed.
        if (iteration == config.controler().getFirstIteration()) {
            this.initialMode2DistanceClass2LegCount.putAll(beelineDistanceDistributionFromPlansAnalyzer.getMode2DistanceClass2LegCount());
        } else {
            ModalShareFromPlans modalShareFromPlans = new ModalShareFromPlans(scenario.getPopulation());
            modalShareFromPlans.run();
            SortedMap<String, Integer> mode2Legs = modalShareFromPlans.getModeToNumberOfLegs();

            double objectiveFunctionValue = getValueOfObjFun(beelineDistanceDistributionFromPlansAnalyzer);
            double penalty = 0.;
            switch (this.referenceStudyDistri.getOpdytsScenario()) {
                case EQUIL:
                case PATNA_1Pct:
                case PATNA_10Pct:
                    break;
                case EQUIL_MIXEDTRAFFIC:
                    double ascBicycle = config.planCalcScore().getModes().get("bicycle").getConstant();
                    double bicycleShare = beelineDistanceDistributionFromPlansAnalyzer.getModeToShare().get("bicycle");
                    penalty = EquilMixedTrafficObjectiveFunctionPenalty.getPenalty(bicycleShare, ascBicycle);
            }

            try {
                // write modalParams
                Map<String, PlanCalcScoreConfigGroup.ModeParams> mode2Params = config.planCalcScore().getModes();

                StringBuilder stringBuilder = new StringBuilder(iteration + "\t");
                stringBuilder.append(String.valueOf(fromStateObjFunValue)).append("\t"); // useful only to check if all decision variables start at the same point.
                modes2consider.forEach(mode -> stringBuilder.append(mode2Legs.containsKey(mode) ? mode2Legs.get(mode) + "\t" : String
                                     .valueOf(0) + "\t"));
                modes2consider.forEach(mode -> stringBuilder.append(mode2Params.get(mode).getConstant()).append("\t"));
                modes2consider.forEach(mode -> stringBuilder.append(mode2Params.get(mode)
                                                                               .getMarginalUtilityOfTraveling())
                                                            .append("\t"));
                modes2consider.forEach(mode -> stringBuilder.append(mode2Params.get(mode)
                                                                               .getMarginalUtilityOfDistance())
                                                            .append("\t"));
                modes2consider.forEach(mode -> stringBuilder.append(mode2Params.get(mode)
                                                                               .getMonetaryDistanceRate()).append("\t"));
                stringBuilder.append(objectiveFunctionValue).append("\t");
                stringBuilder.append(penalty).append("\t");
                stringBuilder.append(String.valueOf(objectiveFunctionValue + penalty));

                writer.write(stringBuilder.toString());
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                throw new RuntimeException("File not found.");
            }
        }

        if( iteration % writeDistanceDistributionEveryIteration == 0 || iteration == config.controler().getLastIteration() ) {
            // dist-distribution file
            String distriDir = config.controler().getOutputDirectory() + "/distanceDistri/";
            new File(distriDir).mkdir();
            String outFile = distriDir + "/" + iteration + ".distanceDistri.txt";
            writeDistanceDistribution(outFile, beelineDistanceDistributionFromPlansAnalyzer.getMode2DistanceClass2LegCount());
        }
    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event) {
        // get value of objective function in advance at this point to check if all decision variables start at the same point.
        // the state changes during re-planning
        fromStateObjFunValue = getValueOfObjFun(getBeelineDistanceDistributionHandler());
    }


    @Override
    public void notifyShutdown(ShutdownEvent event) {
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("File not found.");
        }

        // opdyts files are not in OPDYTS_TRANSITION folders
        String outDir = new File(event.getServices().getControlerIO().getOutputPath()).getParent()+"/";

        // post-process
        String opdytsConvergencefile = outDir +"/opdyts.con";
        if (new File(opdytsConvergencefile).exists()) {
            OpdytsConvergenceChart opdytsConvergencePlotter = new OpdytsConvergenceChart();
            opdytsConvergencePlotter.readFile(outDir +"/opdyts.con");
            opdytsConvergencePlotter.plotData(outDir +"/convergence.png");
        }
        BestSolutionVsDecisionVariableChart bestSolutionVsDecisionVariableChart = new BestSolutionVsDecisionVariableChart(new ArrayList<>(modes2consider));
        bestSolutionVsDecisionVariableChart.readFile(outDir +"/opdyts.log");
        bestSolutionVsDecisionVariableChart.plotData(outDir +"/decisionVariableVsASC.png");
    }

    private LegModeBeelineDistanceDistributionFromPlansAnalyzer getBeelineDistanceDistributionHandler(){
        LegModeBeelineDistanceDistributionFromPlansAnalyzer beelineDistanceDistributionHandler = new LegModeBeelineDistanceDistributionFromPlansAnalyzer(dists);
        beelineDistanceDistributionHandler.init(scenario);
        beelineDistanceDistributionHandler.preProcessData();
        beelineDistanceDistributionHandler.postProcessData();
        return beelineDistanceDistributionHandler;
    }

    private double getValueOfObjFun (final LegModeBeelineDistanceDistributionFromPlansAnalyzer beelineDistanceDistributionHandler){
        Map<String, double[]> simCounts = new TreeMap<>();

        // initialize simcounts array for each mode
        realCounts.forEach((key, value) -> simCounts.put(key, new double[value.length]));

        SortedMap<String, SortedMap<Double, Integer>> simCountsHandler = beelineDistanceDistributionHandler.getMode2DistanceClass2LegCount();
        for (Map.Entry<String, SortedMap<Double, Integer>> e : simCountsHandler.entrySet()) {
            if (!realCounts.containsKey(e.getKey())) continue;
            double[] counts = new double[realCounts.get(e.getKey()).length];
            int index = 0;
            for (Integer count : simCountsHandler.get(e.getKey()).values()) {
                counts[index++] = count;
            }
            simCounts.put(e.getKey(), counts);
        }
        return objectiveFunctionEvaluator.getObjectiveFunctionValue(realCounts,simCounts);
    }

    private void writeDistanceDistribution(String outputFile, final SortedMap<String, SortedMap<Double, Integer>> mode2dist2counts) {
        final BufferedWriter writer2 = IOUtils.getBufferedWriter(outputFile);
        try {
            writer2.write( "distBins" + "\t" );
            for(Double d : referenceStudyDistri.getDistClasses()) {
                writer2.write(d + "\t");
            }
            writer2.newLine();

            // from initial plans
            {
                writer2.write("===== begin writing distribution from initial plans ===== ");
                writer2.newLine();

                for (String mode : this.initialMode2DistanceClass2LegCount.keySet()) {
                    writer2.write(mode + "\t");
                    for (Double d : this.initialMode2DistanceClass2LegCount.get(mode).keySet()) {
                        writer2.write(this.initialMode2DistanceClass2LegCount.get(mode).get(d) + "\t");
                    }
                    writer2.newLine();
                }

                writer2.write("===== end writing distribution from initial plans ===== ");
                writer2.newLine();
            }

            // from objective function
            {
                writer2.write("===== begin writing distribution from objective function ===== ");
                writer2.newLine();

                Map<String, double []> mode2counts = this.referenceStudyDistri.getMode2DistanceBasedLegs();
                for (String mode : mode2counts.keySet()) {
                    writer2.write(mode + "\t");
                    for (Double d : mode2counts.get(mode)) {
                        writer2.write(d + "\t");
                    }
                    writer2.newLine();
                }

                writer2.write("===== end writing distribution from objective function ===== ");
                writer2.newLine();
            }

            // from simulation
            {
                writer2.write("===== begin writing distribution from simulation ===== ");
                writer2.newLine();

                for (String mode : mode2dist2counts.keySet()) {
                    writer2.write(mode + "\t");
                    for (Double d : mode2dist2counts.get(mode).keySet()) {
                        writer2.write(mode2dist2counts.get(mode).get(d) + "\t");
                    }
                    writer2.newLine();
                }

                writer2.write("===== end writing distribution from simulation ===== ");
                writer2.newLine();
            }
            writer2.close();
        } catch (IOException e) {
            throw new RuntimeException("Data is not written. Reason "+ e);
        }
    }

    public void setWriteDistanceDistributionEveryIteration(int writeDistanceDistributionEveryIteration) {
        this.writeDistanceDistributionEveryIteration = writeDistanceDistributionEveryIteration;
    }
}
