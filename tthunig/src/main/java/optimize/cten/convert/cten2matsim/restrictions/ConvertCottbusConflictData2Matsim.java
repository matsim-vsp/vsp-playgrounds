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
package optimize.cten.convert.cten2matsim.restrictions;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.data.conflicts.ConflictData;
import org.matsim.contrib.signals.data.conflicts.ConflictDataImpl;
import org.matsim.contrib.signals.data.conflicts.io.ConflictingDirectionsWriter;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataImpl;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsReader20;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import optimize.cten.data.DgCrossing;
import optimize.cten.ids.DgIdPool;

/**
 * @author tthunig
 */
public class ConvertCottbusConflictData2Matsim {
	
	private Network fullNetwork = NetworkUtils.createNetwork();
	private SignalSystemsData signalSystemsFullNetwork = new SignalSystemsDataImpl();
	
	private static final String matsimDir = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/";
	
	public ConvertCottbusConflictData2Matsim() {
		// read some files of the matsim scenario that are needed for conversion
		new MatsimNetworkReader(fullNetwork).readFile(matsimDir + "network_wgs84_utm33n.xml.gz");
		new SignalSystemsReader20(signalSystemsFullNetwork).readFile(matsimDir + "signal_systems_no_13.xml");
	}

	private void convertConflictData(String btuMainDir, String btuRestrDir, String btuFile) {	
		// read btu scenario
		CtenRestrictionXMLParser restrictionParser = new CtenRestrictionXMLParser();
		restrictionParser.readFile(btuRestrDir + btuFile);
		Map<Id<DgCrossing>, DgCrossing> crossings = restrictionParser.getCrossings();
		
		// create empty container for conflict data in matsim
		ConflictData conflictData = new ConflictDataImpl();
		
		// read in id conversion file
		DgIdPool idPool = DgIdPool.readFromFile(btuMainDir + "id_conversions.txt");

		// fill matsim conflict data container with information from btu scenario
		Restriction2ConflictData converter = new Restriction2ConflictData(idPool, fullNetwork, signalSystemsFullNetwork);
		converter.convertConflicts(conflictData, crossings);
		
		new ConflictingDirectionsWriter(conflictData).write(btuRestrDir + "conflictData_converted.xml");
	}

	public static void main(String[] args) {
		String btuRestrDir = "../../shared-svn/projects/cottbus/data/optimization/cb2ks2010/btu_restrictions/";
		String btuMainDir = "../../shared-svn/projects/cottbus/data/optimization/cb2ks2010/2015-02-25_minflow_50.0_morning_peak_speedFilter15.0_SP_tt_cBB50.0_sBB500.0/";
		
		new ConvertCottbusConflictData2Matsim().convertConflictData(btuMainDir, btuRestrDir, "model_new.xml");
//		new ConvertCottbusConflictData2Matsim().convertConflictData(btuMainDir, btuRestrDir, "model.xml");
	}
	
}
