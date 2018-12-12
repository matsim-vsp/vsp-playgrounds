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
package playground.dziemke.accessibility.other;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

public class AVPickUpTimeMatcher {
	
	private static final Logger LOG = Logger.getLogger(AVPickUpTimeMatcher.class);
	
	private static final String ZONES_FILE = "../../shared-svn/projects/accessibility_berlin/av/grid/zones.csv";
//	private static final String SAV_FILE = "../../shared-svn/projects/accessibility_berlin/av/waittimes_500_access_grid/averageTaxiWaitTimes_dominik_10runs.csv";
	private static final String SAV_FILE = "../../shared-svn/projects/accessibility_berlin/av/waittimes_500_access_grid_rebalancing/averageTaxiWaitTimes_with_rebalancing_10runs.csv";
//	private static final String OUTPUT_FILE = "../../shared-svn/projects/accessibility_berlin/av/waittimes_500_access_grid/merged_10.csv";
	private static final String OUTPUT_FILE = "../../shared-svn/projects/accessibility_berlin/av/waittimes_500_access_grid_rebalancing/merged_10_rebalancing.csv";

	public static void main(String[] args) {
		Map<Integer, String> zonesLinesPerId = new HashMap<>();
		Map<Integer, String> savLinesPerId = new HashMap<>();
		String headerSAV;
		String headerZones;
		
		LOG.info("read zones file");
		BufferedReader br = null;
		try {
			br = IOUtils.getBufferedReader(ZONES_FILE);
			headerZones = br.readLine(); // header
			String line = br.readLine(); // first line
			
			while (line != null) {
				String[] s = line.split(";");
				Integer id = Integer.valueOf(s[s.length-1].trim());
				
				zonesLinesPerId.put(id, line);
				
				line = br.readLine();
			}
			br.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		LOG.info("read sav file");
		try {
			br = IOUtils.getBufferedReader(SAV_FILE);
			headerSAV = br.readLine(); // header
			String line = br.readLine(); // first line
			
			while (line != null) {
				if (!line.equals("")) {
					String[] s = line.split(";");
					Integer id = Integer.valueOf(s[0].trim());

					savLinesPerId.put(id, line);
				}
				
				line = br.readLine();
			}
			br.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		LOG.info("write merged file");
		BufferedWriter bw = IOUtils.getBufferedWriter(OUTPUT_FILE);
		try {
			bw.write(headerZones + ";" + headerSAV);
			bw.newLine();
			for (Entry<Integer, String> e : zonesLinesPerId.entrySet()){
				bw.write(e.getValue());
				bw.write(";");
				if (savLinesPerId.get(e.getKey()) != null) {
					bw.write(savLinesPerId.get(e.getKey()));
				}
				bw.newLine();
			}
			bw.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		};
	}
}