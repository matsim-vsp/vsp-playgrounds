/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 * 
 */
package playground.tschlenther.counts;

import java.util.logging.Logger;

import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.matsim.counts.CountsWriter;

/**
 * @author tschlenther
 *
 */
public class CountMerger {

	private final String file1;
	private final String file2;
	private Counts counts;
	Logger log = Logger.getLogger(CountMerger.class.getName());
	
	/**
	 * 
	 */
	public CountMerger(String countFile1, String countFile2) {
		this.file1 = countFile1;
		this.file2 = countFile2;
		this.counts = new Counts();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String file1 = "../../../VSP//BASt/2016_A_S_TUE_THU_Pkw_Berlin.xml.gz";
		String file2 = "../../../VSP//BASt/2016_B_S_TUE_THU_Pkw_Berlin.xml.gz";
		String output = "../../../VSP//BASt/2016_BASt_TUE_THU_Pkw_Berlin.xml.gz";
		
		if(args.length > 0) {
			file1 = args[0];
			file2 = args[1];
			output = args[2];
		}
		
		CountMerger merger = new CountMerger(file1, file2);
		merger.mergeCounts();
		merger.writeCounts(output);
	}

	public void mergeCounts() {
		CountsReaderMatsimV1 reader = new CountsReaderMatsimV1(counts);
		reader.readFile(file1);
		reader.readFile(file2);
		log.info("finished merging...");
	}
	
	public void writeCounts(String outputPath) {
		CountsWriter writer = new CountsWriter(counts);
		log.info("writing result to " + outputPath);
		writer.write(outputPath);
		log.info("FINISHED");
	}
	
	
}
