/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.agarwalamit.templates;

import org.matsim.core.utils.misc.ExeRunner;

/**
 * Created by amit on 03.07.17.
 */


public class RunGNUPlotScript {

    public static void main(String[] args) {


        String dir = "/Users/amit/Documents/git/playgrounds/agarwalamit/src/main/resources/gnuplot/";

        String inputFile = dir+"test/1.stateVector_networkModes.txt";
        String outputFile = dir+"test/1.stateVector_networkModes.eps";

        String cmd = "gnuplot -c histogram.gnu ";
        cmd += " "+inputFile;
        cmd += " "+outputFile;
        cmd += " networkModes";

        ExeRunner.run(cmd, dir+"/gnulog.log", 99999 ,dir);


    }


}
