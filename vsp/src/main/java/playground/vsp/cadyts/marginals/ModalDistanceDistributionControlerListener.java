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

package playground.vsp.cadyts.marginals;

import java.io.BufferedWriter;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;
import playground.vsp.cadyts.marginals.prep.DistanceBin;
import playground.vsp.cadyts.marginals.prep.DistanceDistribution;
import playground.vsp.cadyts.marginals.prep.DistanceDistributionUtils.DistanceDistributionFileLabels;
import playground.vsp.cadyts.marginals.prep.ModalDistanceBinIdentifier;

/**
 * Created by amit on 22.02.18.
 */

public class ModalDistanceDistributionControlerListener implements StartupListener, IterationEndsListener {
    private static final Logger LOG = Logger.getLogger(ModalDistanceDistributionControlerListener.class);

    private static final String fileName_avg = "multiMode_distanceDistributionCounts_average.txt";
    private static final String fileName_abs = "multiMode_distanceDistributionCounts_absolute.txt";

    private final ControlerConfigGroup controlerConfigGroup;
    private final CountsConfigGroup config;
    private final DistanceDistribution inputDistanceDistribution;
    private final BeelineDistanceCollector beelineDistanceCollector;
    private final OutputDirectoryHierarchy controlerIO;
    private int iterationsUsed = 0;

    private final DistanceDistribution stats = new DistanceDistribution();

    @Inject
    private ModalDistanceDistributionControlerListener(
            ControlerConfigGroup controlerConfigGroup,
            CountsConfigGroup countsConfigGroup,
            BeelineDistanceCollector beelineDistanceCollector,
            OutputDirectoryHierarchy controlerIO,
            DistanceDistribution inputDistanceDistribution
    ) {
        this.controlerConfigGroup = controlerConfigGroup;
        this.config = countsConfigGroup;
        this.inputDistanceDistribution = inputDistanceDistribution;
        this.beelineDistanceCollector = beelineDistanceCollector;
        this.controlerIO = controlerIO;
    }

    @Override
    public void notifyStartup(final StartupEvent controlerStartupEvent) {
        if (inputDistanceDistribution == null || inputDistanceDistribution.getModalBins().isEmpty()) {
            throw new RuntimeException("The input distance distribution is null or is empty.");
        }
        // initialize stats
        inputDistanceDistribution.getModalBins()
                                 .values()
                                 .forEach(e -> stats.addToDistribution(e.getMode(), e.getDistanceRange(), 0.));
    }

    @Override
    public void notifyIterationEnds(final IterationEndsEvent event) {
        if ( event.getIteration() == controlerConfigGroup.getFirstIteration() ) {
            // write the data for first iteration too
            addCounts(beelineDistanceCollector.getOutputDistanceDistribution());
            writeData(event, this.stats, fileName_abs);
            reset();
        } else if (this.config.getWriteCountsInterval() > 0) {
            if (useStatsOfIteration(event.getIteration(), controlerConfigGroup.getFirstIteration())) {
                addCounts(beelineDistanceCollector.getOutputDistanceDistribution());
            }

            if (createCountsInIteration(event.getIteration())) {
                DistanceDistribution averages;
                if (this.iterationsUsed > 1) {
                    averages = new DistanceDistribution();
                    this.stats.getModalBinToDistanceBin()
                              .forEach(
                                      (key, value) -> averages.addToDistribution(
                                              this.stats.getModalBins().get(key).getMode(),
                                              value.getDistanceRange(),
                                              value.getCount() / this.iterationsUsed));

                } else {
                    averages = this.stats;
                }

                writeData(event, averages, fileName_avg);
                writeData(event, beelineDistanceCollector.getOutputDistanceDistribution(), fileName_abs);

                reset();
            }
        }
    }

    private void writeData(final IterationEndsEvent event, DistanceDistribution averages, String fileName) {
        String filename = controlerIO.getIterationFilename(event.getIteration(),
                fileName);
        try (BufferedWriter writer = IOUtils.getBufferedWriter(filename)) {
            writer.write(DistanceDistributionFileLabels.mode + "\t" +
                    DistanceDistributionFileLabels.distanceLowerLimit + "\t" +
                    DistanceDistributionFileLabels.distanceUpperLimit + "\t" +
                    DistanceDistributionFileLabels.measuredCount + "\t" +
                    "simulationCount");
            writer.newLine();


            for (SortedMap.Entry<ModalDistanceBinIdentifier, DistanceBin> entry : getSortedMap(averages).entrySet()) {
                writer.write(
                        entry.getKey().getMode() + "\t"
                                + entry.getKey().getDistanceRange().getLowerLimit() + "\t"
                                + entry.getKey().getDistanceRange().getUpperLimit() + "\t" +
                                this.inputDistanceDistribution.getModalBinToDistanceBin()
                                                              .get(entry.getKey().getId())
                                                              .getCount() + "\t" +
                                entry.getValue().getCount());
                writer.newLine();
            }
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException("Data is not written. Reason :" + e);
        }
    }

    private boolean useStatsOfIteration(final int iteration, final int firstIteration) {
        int iterationMod = iteration % this.config.getWriteCountsInterval();
        int effectiveIteration = iteration - firstIteration;
        int averaging = Math.min(this.config.getAverageCountsOverIterations(), this.config.getWriteCountsInterval());
        if (iterationMod == 0) {
            return ((this.config.getAverageCountsOverIterations() <= 1) ||
                    (effectiveIteration >= averaging));
        }
        return (iterationMod > (this.config.getWriteCountsInterval() - this.config.getAverageCountsOverIterations())
                && (effectiveIteration + (this.config.getWriteCountsInterval() - iterationMod) >= averaging));
    }

    private boolean createCountsInIteration(final int iteration) {
        return ((iteration % this.config.getWriteCountsInterval() == 0) && (this.iterationsUsed >= this.config.getAverageCountsOverIterations()));
    }

    private void addCounts(final DistanceDistribution distribution) {
        this.iterationsUsed++;

        distribution.getModalBinToDistanceBin()
                    .forEach((key, value) -> this.stats.getModalBinToDistanceBin()
                                                       .get(key)
                                                       .addToCount(value.getCount() * this.inputDistanceDistribution.getModalBins().get(key).getScalingFactor() ));
    }

    private void reset() {
        this.iterationsUsed = 0;
        this.stats.getModalBinToDistanceBin()
                  .forEach((key, value) -> value.addToCount( - value.getCount()));
    }

    //technically, following can be applied directly in DistanceDistribution, however, ordering is imp here only.
    private SortedMap<ModalDistanceBinIdentifier, DistanceBin> getSortedMap(DistanceDistribution distanceDistribution) {
        SortedMap<ModalDistanceBinIdentifier, DistanceBin> sortedMap = new TreeMap<>();
        distanceDistribution.getModalBinToDistanceBin()
                            .forEach((key, value) -> sortedMap.put(distanceDistribution.getModalBins().get(key),
                                    value));
        return sortedMap;
    }
}
