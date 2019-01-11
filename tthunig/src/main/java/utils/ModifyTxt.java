/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author tthunig
 */
public class ModifyTxt {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String dir = "../../shared-svn/projects/cottbus/data/optimization/cb2ks2010/2018-11-13_minflow_50.0_time19800.0-34200.0_speedFilter15.0_SP_tt_cBB50.0_sBB500.0/randoms/";
		FileReader frCten = new FileReader(new File(dir + "tt_cten.txt"));
		FileReader frMatsim = new FileReader(new File(dir + "tt_matsim_1000it_tbs900_beta2_matsimRoutes.txt"));
		BufferedReader inX = new BufferedReader(frCten);
		BufferedReader inY = new BufferedReader(frMatsim);

		FileWriter fw = new FileWriter(new File(dir + "tt_cten_matsim_1000it_tbs900_beta2_matsimRoutes.txt"));
		BufferedWriter out = new BufferedWriter(fw);
		
		// write header
		String seperator = "\t";
		out.write("coord \ttotal_cost_cten[s] \ttotal_driving_time_cten[s] \ttotal_waiting_time_cten[s] "
				+ "\ttotal_tt_matsim[s] \ttotal_delay_matsim[s] \ttotal_dist_matsim[m]");
		out.newLine();

		String lineX = inX.readLine();
		String lineY = inY.readLine();
		// skip header
		lineX = inX.readLine();
		lineY = inY.readLine();
		
		while ( lineY != null ) {
			String[] lineXarray = lineX.trim().split(seperator);
			String[] lineYarray = lineY.trim().split(seperator);
						
			out.write(lineXarray[0] + seperator + lineXarray[1] + seperator + lineXarray[2] + seperator 
//					+ (lineXarray[2].equals("NaN")? 0 : lineXarray[2]) + seperator
//					+ Integer.parseInt(lineXarray[3]) * Integer.parseInt(lineXarray[2]) / 100 + seperator
//					+ Integer.parseInt(lineXarray[4]) * Integer.parseInt(lineXarray[2]) / 100 + seperator
//					+ lineYarray[2] + seperator
//					+ Integer.parseInt(lineYarray[3]) * Integer.parseInt(lineYarray[2]) / 100// + seperator
//					+ Integer.parseInt(lineYarray[4]) * Integer.parseInt(lineYarray[2]) / 100);
//					+ (lineYarray[2].equals("NaN")? 0 : lineYarray[2])
					+ lineXarray[3] + seperator + lineYarray[1] + seperator + lineYarray[2] + seperator + lineYarray[3] + seperator 
					);
			out.newLine();
			
			lineX = inX.readLine();
			lineY = inY.readLine();
		}
		
		inX.close();
		inY.close();
		out.close();
		
		System.out.println("done :)");
	}

}
