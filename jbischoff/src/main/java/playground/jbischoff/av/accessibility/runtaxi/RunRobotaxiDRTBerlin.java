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

package playground.jbischoff.av.accessibility.runtaxi;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * This class runs an example robotaxi scenario including scoring. The simulation runs for 10 iterations, this takes
 * quite a bit time (25 minutes or so). You may switch on OTFVis visualisation in the main method below. The scenario
 * should run out of the box without any additional files. If required, you may find all input files in the resource
 * path or in the jar maven has downloaded). There are two vehicle files: 2000 vehicles and 5000, which may be set in
 * the config. Different fleet sizes can be created using
 * {@link org.matsim.contrib.av.robotaxi.vehicles.CreateTaxiVehicles}
 */
public class RunRobotaxiDRTBerlin {

    public static void main(String[] args) {
        for (int i = 4; i < 10; i++) {
            String configFile = "D:/runs-svn/avsim/av_accessibility/input/drtconfig.xml";

            RunRobotaxiDRTBerlin.run(configFile, false, i);
        }

    }

    public static void run(String configFile, boolean otfvis, int run) {
        Config config = ConfigUtils.loadConfig(configFile, new DvrpConfigGroup(), new DrtConfigGroup(),
                new OTFVisConfigGroup());
        DrtConfigGroup.get(config).setRequestRejection(false);
        DrtConfigGroup.get(config).setMaxWaitTime(400);
        config.controler().setOutputDirectory(config.controler().getOutputDirectory() + run);
        config.plans().setInputFile("taxiplans_" + run + ".xml.gz");
        Controler controler = DrtControlerCreator.createControler(config, false);
        controler.addOverridingModule(new SwissRailRaptorModule());
        controler.run();
    }

}
