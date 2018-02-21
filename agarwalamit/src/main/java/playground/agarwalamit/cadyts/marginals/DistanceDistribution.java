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

package playground.agarwalamit.cadyts.marginals;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.cadyts.marginals.DistanceDistributionUtils.*;

/**
 * Created by amit on 21.02.18.
 */

public class DistanceDistribution {

    private static final Logger LOG = Logger.getLogger(DistanceDistribution.class);

    private final SortedMap<String, Set<DistanceBin>> mode2DistanceBins = new TreeMap<>();
    private boolean locked = false;

    public void fillDistanceDistribution(String inputFile, DistanceUnit distanceUnit, String itemSeparator) {
        if (locked) {
            throw new RuntimeException("Can't add any other data to distribution.");
        }

        LOG.info("Generating distance distribution from file :" + inputFile);
        // let's assume that input file has headers as
        try (BufferedReader reader = IOUtils.getBufferedReader(inputFile)) {
            String line = reader.readLine();
            List<String> labels = null;

            while (line != null) {
                String parts[] = line.split(itemSeparator);

                if (labels == null) {
                    labels = Arrays.asList(parts);
                } else {

                    DistanceBin.DistanceRange range = new DistanceBin.DistanceRange(
                            getMultiplierToConvertInMeter( distanceUnit) * Double.valueOf(parts[labels.indexOf(
                                    DistanceDistributionFileLabels.distanceLowerLimit.toString())]),
                            getMultiplierToConvertInMeter( distanceUnit) * Double.valueOf(parts[labels.indexOf(
                                    DistanceDistributionFileLabels.distanceUpperLimit.toString())])
                    );
                    DistanceBin bin = new DistanceBin(range);
                    bin.addToCount(Double.valueOf(parts[labels.indexOf(DistanceDistributionFileLabels.measuredCount.toString())]));

                    String mode = parts[labels.indexOf(DistanceDistributionFileLabels.mode.toString())];

                    Set<DistanceBin> bins = this.mode2DistanceBins.getOrDefault(mode, new HashSet<>());
                    bins.add(bin);
                    this.mode2DistanceBins.put(mode, bins);

                }

                line = reader.readLine();

            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
    }

    public void addToDistribution(String mode, DistanceBin.DistanceRange distanceRange, double val) {
        if (locked) {
            throw new RuntimeException("Can't add any other data to distribution.");
        }

        Set<DistanceBin> bins = this.mode2DistanceBins.getOrDefault(mode, new HashSet<>());
        DistanceBin bin = bins.stream()
                              .filter(b -> b.getDistanceRange().equals(distanceRange))
                              .findFirst()
                              .orElseGet(() -> new DistanceBin(distanceRange));
        bin.addToCount(val);
        bins.add(bin);
        this.mode2DistanceBins.put(mode, bins);
    }

    public SortedSet<String> getModes(){
        return new TreeSet<>(this.mode2DistanceBins.keySet());
    }

    public TreeSet<DistanceBin.DistanceRange> getDistanceRanges() {
        // don't let anyone change the distribution after this call.
        locked = true;
        return this.mode2DistanceBins.values()
                                     .stream()
                                     .flatMap(Collection::stream)
                                     .map(DistanceBin::getDistanceRange)
                                     .distinct()
                                     .collect(Collectors.toCollection(TreeSet::new));
    }

    public Map<Id<ModalBin>, ModalBin> getModalBins(){
        Map<Id<ModalBin>, ModalBin> modalDistanceBinMap = new HashMap<>();
        for(String mode : this.mode2DistanceBins.keySet()){
            for (DistanceBin bin : this.mode2DistanceBins.get(mode)){
                DistanceBin.DistanceRange range = bin.getDistanceRange();
                ModalBin modalDistanceBin = new ModalBin(mode, range);
                modalDistanceBinMap.put(modalDistanceBin.getId(), modalDistanceBin);
            }
        }
        return modalDistanceBinMap;
    }

    public Map<Id<ModalBin>, DistanceBin> getModalBinToDistanceBin(){
        Map<Id<ModalBin>, DistanceBin> modalDistanceBinMap = new HashMap<>();
        for(String mode : this.mode2DistanceBins.keySet()){
            for (DistanceBin bin : this.mode2DistanceBins.get(mode)){
                DistanceBin.DistanceRange range = bin.getDistanceRange();
                ModalBin modalDistanceBin = new ModalBin(mode, range);
                modalDistanceBinMap.put(modalDistanceBin.getId(), bin);
            }
        }
        return modalDistanceBinMap;
    }

    private double getMultiplierToConvertInMeter(DistanceUnit distanceUnit) {
        switch (distanceUnit) {
            case meter:
                return 1.;
            case kilometer:
                return 1000.;
            default:
                throw new RuntimeException("not implemented yet.");
        }
    }
}
