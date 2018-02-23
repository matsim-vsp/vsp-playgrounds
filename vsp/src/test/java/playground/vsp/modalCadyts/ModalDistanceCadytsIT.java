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

package playground.vsp.modalCadyts;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import playground.vsp.cadyts.marginals.prep.DistanceBin;
import playground.vsp.cadyts.marginals.ModalDistanceDistributionControlerListener;
import playground.vsp.cadyts.marginals.RunExample;

/**
 * Created by amit on 22.02.18.
 */

@RunWith(Parameterized.class)
public class ModalDistanceCadytsIT {

    private static final URL EQUIL_DIR = ExamplesUtils.getTestScenarioURL("equil-mixedTraffic");

    private double cadytsWt ;

    public ModalDistanceCadytsIT(double cadytsWt){
        this.cadytsWt = cadytsWt;
    }

    @Parameterized.Parameters(name = "{index}: cadytsWeight == {0};")
    public static Collection<Object> parameterObjects () {
        return Arrays.asList(0.0, 2500.0);
    }

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public final void test() {
        int lastIteration = 20;
        String outDir = utils.getOutputDirectory();
        RunExample.main(
                new String [] {
                        outDir,
                        String.valueOf(this.cadytsWt),
                        String.valueOf(lastIteration)
                },
                IOUtils.newUrl(EQUIL_DIR,"config-with-mode-vehicles.xml")
        );

        //
        String outputFile =  outDir+"ITERS/it."+lastIteration+"/"+lastIteration+ "."+ ModalDistanceDistributionControlerListener.fileName;
        BufferedReader reader = IOUtils.getBufferedReader(outputFile);
        try {
            String line = reader.readLine();
            boolean header = true;
            while (line != null){
                if (header) {
                    header = false;
                    line = reader.readLine();
                    continue;
                }
                String parts [] = line.split("\t");
                DistanceBin.DistanceRange range = new DistanceBin.DistanceRange(Double.valueOf(parts[1]), Double.valueOf(parts[2]));
                if (parts[0].equals("bicycle")) {
                    if ( range.toString().equals(new DistanceBin.DistanceRange(0.0, 6000.0).toString())) {
                        Assert.assertEquals("Wrong measured count for mode bicycle and "+range.toString(), "8.0", parts[3] );
                        if (this.cadytsWt==0.0){
                            Assert.assertEquals("Wrong real count for mode bicycle and "+range.toString(), "0.0", parts[4] );
                        } else {
                            Assert.assertEquals("Wrong real count for mode bicycle and "+range.toString(), "3.25", parts[4] );
                        }
                    } else if (range.toString().equals(new DistanceBin.DistanceRange(6000.0, 12000.0).toString())){
                        Assert.assertEquals("Wrong measured count for mode bicycle and "+range.toString(), "4.0", parts[3] );
                        if (this.cadytsWt==0.0){
                            Assert.assertEquals("Wrong real count for mode bicycle and "+range.toString(), "0.0", parts[4] );
                        } else {
                            Assert.assertEquals("Wrong real count for mode bicycle and "+range.toString(), "5.5", parts[4] );
                        }
                    } else if (range.toString().equals(new DistanceBin.DistanceRange(12000.0, 18000.0).toString())){
                        Assert.assertEquals("Wrong measured count for mode bicycle and "+range.toString(), "2.0", parts[3] );
                        if (this.cadytsWt==0.0){
                            Assert.assertEquals("Wrong real count for mode bicycle and "+range.toString(), "0.0", parts[4] );
                        } else {
                            Assert.assertEquals("Wrong real count for mode bicycle and "+range.toString(), "0.0", parts[4] );
                        }
                    } else if (range.toString().equals(new DistanceBin.DistanceRange(18000.0, 86000.0).toString())) {
                        Assert.assertEquals("Wrong measured count for mode bicycle and "+range.toString(), "0.0", parts[3] );
                        Assert.assertEquals("Wrong real count for mode bicycle and "+range.toString(), "0.0", parts[4] );
                    } else {
                        throw new RuntimeException(range+" should not be present.");
                    }
                } else {
                    if (range.toString().equals(new DistanceBin.DistanceRange(0.0, 6000.0).toString())) {
                        Assert.assertEquals("Wrong measured count for mode car and "+range.toString(), "2.0", parts[3] );
                        if (this.cadytsWt==0.0){
                            Assert.assertEquals("Wrong real count for mode car and "+range.toString(), "10.0", parts[4] );
                        } else {
                            Assert.assertEquals("Wrong real count for mode car and "+range.toString(), "6.75", parts[4] );
                        }
                    } else if (range.toString().equals(new DistanceBin.DistanceRange(6000.0, 12000.0).toString())){
                        Assert.assertEquals("Wrong measured count for mode car and "+range.toString(), "4.0", parts[3] );
                        if (this.cadytsWt==0.0){
                            Assert.assertEquals("Wrong real count for mode car and "+range.toString(), "12.0", parts[4] );
                        } else {
                            Assert.assertEquals("Wrong real count for mode car and "+range.toString(), "6.5", parts[4] );
                        }
                    } else if (range.toString().equals(new DistanceBin.DistanceRange(12000.0, 18000.0).toString())){
                        Assert.assertEquals("Wrong measured count for mode car and "+range.toString(), "5.0", parts[3] );
                        if (this.cadytsWt==0.0){
                            Assert.assertEquals("Wrong real count for mode car and "+range.toString(), "3.0", parts[4] );
                        } else {
                            Assert.assertEquals("Wrong real count for mode car and "+range.toString(), "3.0", parts[4] );
                        }
                    } else if (range.toString().equals(new DistanceBin.DistanceRange(18000.0, 86000.0).toString())) {
                        Assert.assertEquals("Wrong measured count for mode car and "+range.toString(), "20.0", parts[3] );
                        Assert.assertEquals("Wrong real count for mode car and "+range.toString(), "20.0", parts[4] );
                    } else {
                        throw new RuntimeException(range+" should not be present.");
                    }
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
    }
}
