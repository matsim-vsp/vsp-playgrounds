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

package playground.jbischoff.utils;/*
 * created by jbischoff, 20.02.2019
 */

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class ShrinkPop {

    public static void main(String[] args) {
        String inputPop = "D:/BSWOB2.0_Scenarios/plans/vw219.10pct_commuter_DRT_selected.xml.gz";
        String outputPop = "D:/BSWOB2.0_Scenarios/plans/vw219.10pct_commuter_DRT_selected_10pct.xml.gz";

        StreamingPopulationWriter spw = new StreamingPopulationWriter(0.1);
        StreamingPopulationReader spr = new StreamingPopulationReader(ScenarioUtils.createScenario(ConfigUtils.createConfig()));
        spw.startStreaming(outputPop);
        spr.addAlgorithm(spw);
        spr.readFile(inputPop);
        spw.closeStreaming();
    }
}
