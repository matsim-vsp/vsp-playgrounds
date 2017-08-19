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

package playground.agarwalamit.nemo.demand;

import java.util.Arrays;
import playground.agarwalamit.utils.FileUtils;
import playground.vsp.demandde.cemdap.input.DemandGeneratorCensus;

/**
 * Created by amit on 22.06.17.
 */

public class CempdapInputGenerator {

    public static void main(String[] args) {

        String sharedSVNDir = FileUtils.SHARED_SVN; // "../../../shared-svn";

        String baseDir = sharedSVNDir + "/projects/nemo_mercator/30_Scenario/cemdap_input/";
        String commuterFileOutgoing1 = baseDir + "/pendlerstatistik/051NRW2009Ga.txt";
        String commuterFileOutgoing2 = baseDir + "/pendlerstatistik/053NRW2009Ga.txt";
        String commuterFileOutgoing3 = baseDir + "/pendlerstatistik/055NRW2009Ga.txt";
        String commuterFileOutgoing4 = baseDir + "/pendlerstatistik/057NRW2009Ga.txt";
        String commuterFileOutgoing5 = baseDir + "/pendlerstatistik/059NRW2009Ga.txt";

        String censusFile = baseDir + "/zensus_2011/Zensus11_Datensatz_Bevoelkerung_NRW.csv";
//        String shapeFileLors = baseDir + "/shapeFiles/shapeFile_Ruhrgebiet/dvg2gem_ruhrgebiet.shp";
        String outputBase = baseDir + "/100/" ;

        // Parameters
        int numberOfPlansPerPerson = 5;
//        String planningAreaId = ""; // no specific planning Area Id
        double defaultAdultsToEmployeesRatio = 1.23;  // Calibrated based on sum value from Zensus 2011.
        double defaultEmployeesToCommutersRatio = 2.5;  // This is an assumption, oriented on observed values, deliberately chosen slightly too high.
        boolean writeMatsimPlanFiles = true;
        boolean includeChildren = false;
//        String LORAttributeKey = "KN";

        String[] commuterFilesOutgoing = {commuterFileOutgoing1, commuterFileOutgoing2, commuterFileOutgoing3, commuterFileOutgoing4, commuterFileOutgoing5};

        {// person and plans file which contains only attributes; these will be required to generate matsim plans files
            DemandGeneratorCensus demandGeneratorCensus = new DemandGeneratorCensus(commuterFilesOutgoing, censusFile, outputBase, numberOfPlansPerPerson,
                    Arrays.asList("05"), defaultAdultsToEmployeesRatio, defaultEmployeesToCommutersRatio);
            demandGeneratorCensus.setWriteMatsimPlanFiles(writeMatsimPlanFiles);
            demandGeneratorCensus.setIncludeChildren(includeChildren);
            demandGeneratorCensus.generateDemand();
        }
        { // zones and lor files
//            new ZoneAndLOSGeneratorV2(commuterFilesOutgoing, shapeFileLors, outputBase);
        }
    }
}
