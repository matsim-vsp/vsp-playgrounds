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
package optimize.cten.convert.Restrictions2Matsim;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.conflicts.ConflictData;
import org.matsim.contrib.signals.data.conflicts.ConflictDataImpl;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsReader20;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.dgrether.koehlerstrehlersignal.data.DgCrossing;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdPool;
import playground.dgrether.koehlerstrehlersignal.solutionconverter.KS2010CrossingSolution;
import playground.dgrether.koehlerstrehlersignal.solutionconverter.KS2010Solution2Matsim;
import playground.dgrether.koehlerstrehlersignal.solutionconverter.KS2014SolutionXMLParser;

/**
 * @author tthunig
 */
public class ConvertCottbusConflictData2Matsim {
	
	private Network networkSimplifiedAndSpatiallyExtended;
	private Network fullNetwork;
	private SignalSystemsData signalSystemsFullNetwork;
	
	private static final String matsimDir = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/";
	private static final String btuDir = "../../shared-svn/projects/cottbus/cb2ks2010/btu_restrictions/";
	
	public ConvertCottbusConflictData2Matsim() {
		// read some files of the matsim scenario that are needed for conversion
		// TODO which version??
		new MatsimNetworkReader(networkSimplifiedAndSpatiallyExtended).readFile(btuDir + "matsim_network_ks_model.xml.gz");
		// TODO check again, which network version we should use
		new MatsimNetworkReader(fullNetwork).readFile(matsimDir + "network_wgs84_utm33n.xml.gz");
		// TODO check again, which signal systems version we should use
		new SignalSystemsReader20(signalSystemsFullNetwork).readFile(matsimDir + "signal_systems_no_13.xml");
	}

	private void convertConflictData(String directory, String btuFile) {	
		// read btu scenario
		CtenRestrictionXMLParser restrictionParser = new CtenRestrictionXMLParser();
		restrictionParser.readFile(directory + btuFile);
		Map<Id<DgCrossing>, DgCrossing> crossings = restrictionParser.getCrossings();
		
		// create empty container for conflict data in matsim
		ConflictData conflictData = new ConflictDataImpl();
		
		// TODO which id conversions has robert used ???
		// (zur Not neue ausdenken und später anhand der coordinaten mergen)
		DgIdPool idPool = DgIdPool.readFromFile(directory + "id_conversions.txt");

		// fill matsim conflict data container with information from btu scenario
		Restriction2ConflictData converter = new Restriction2ConflictData(idPool, networkSimplifiedAndSpatiallyExtended, fullNetwork, signalSystemsFullNetwork);
		converter.convertConflicts(conflictData, crossings);
		
		new ConflictingDirectionsWriter(conflictData).write(directory + "conflictData_converted.xml");
	}

	public static void main(String[] args) {
		new ConvertCottbusConflictData2Matsim().convertConflictData(btuDir, "model.xml");
	}
	
}
