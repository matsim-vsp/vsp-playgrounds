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

package playground.agarwalamit.opdyts.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.utils.NumberUtils;

/**
 * Created by amit on 18.04.18.
 */

public class ModeStatsExtractor {

    private static final int totalLegs = 14160;

    public static void main(String[] args) {

        BiFunction<String, String, Map<String, Integer>> getModalShare = new BiFunction<String, String, Map<String, Integer>>() {
            @Override
            public Map<String, Integer> apply(String inputFile, String iteration) {

                Map<String, Integer> legs = new TreeMap<>();

                try (BufferedReader reader = IOUtils.getBufferedReader(inputFile)) {
                    String line = reader.readLine();
                    boolean header = true;
                    List<String> labels = null;
                    while (line != null) {
                        if (header) {
                            labels = Arrays.asList(line.split("\t"));
                            header = false;
                        } else {
                            if (line.startsWith(String.valueOf(iteration))) {
                                String[] parts = line.split("\t");

                                for (int i = 1; i < labels.size(); i++) {
                                    legs.put(labels.get(i), (int) NumberUtils.round(totalLegs * Double.valueOf(parts[i]),0));
                                }
                            }
                        }
                        line = reader.readLine();
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Data is not read. Reason: " + e);
                }
                return legs;
            }
        };

        String baseDir = "../../runs-svn/opdyts/patna/networkModes/beforeOct2017/output/ascAnalysis/";

        File[] files = new File(baseDir).listFiles();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("asc_bike\tasc_car\tasc_motorbike\t\tlegs_bike\tlegs_car\tlegs_motorbike\n");

        for (File file : files) {
            if (file!=null && file.isDirectory()) {
                String configFile = file.getAbsolutePath() + "/output_config.xml.gz";
                String modeStatsFile = file.getAbsolutePath() + "/modestats.txt";

                Config config = ConfigUtils.loadConfig(configFile);

                Map<String, Double> ASCs = config.planCalcScore()
                                                 .getModes()
                                                 .values()
                                                 .stream()
                                                 .collect(Collectors.toMap(ModeParams::getMode,
                                                         ModeParams::getConstant));
                Map<String, Integer> legs = getModalShare.apply(modeStatsFile, String.valueOf(config.controler().getLastIteration()));


                stringBuilder.append(String.valueOf(ASCs.get("bike")))
                             .append("\t")
                             .append(String.valueOf(ASCs.get("car")))
                             .append("\t")
                             .append(String.valueOf(ASCs.get("motorbike")))
                             .append("\t\t")
                             .append(String.valueOf(legs.get("bike")))
                             .append("\t")
                             .append(String.valueOf(legs.get("car")))
                             .append("\t")
                             .append(String.valueOf(legs.get("motorbike")))
                             .append("\n");
            }
        }

        try (BufferedWriter writer = IOUtils.getBufferedWriter(baseDir+"/ascLegsStats.txt")){
            writer.write(stringBuilder.toString());
            writer.close();
        }catch (IOException e) {
            throw new RuntimeException("Data is not written. Reason: " + e);
        }
    }
}
