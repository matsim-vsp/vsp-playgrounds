/* *********************************************************************** *
 * project: org.matsim.*
 * DgFigure9ToKoehlerStrehler2010ModelConverter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.figure9scenario;

import org.apache.log4j.Logger;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.lanes.Lanes;
import org.xml.sax.SAXException;
import playground.dgrether.koehlerstrehlersignal.conversion.M2KS2010NetworkConverter;
import playground.dgrether.koehlerstrehlersignal.conversion.M2KS2010SimpleDemandConverter;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodities;
import playground.dgrether.koehlerstrehlersignal.data.DgKSNetwork;
import playground.dgrether.koehlerstrehlersignal.data.KS2010ModelWriter;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdConverter;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdPool;

import javax.xml.transform.TransformerConfigurationException;
import java.io.IOException;


public class DgFigure9ToKoehlerStrehler2010ModelConverter {
	
	private static final Logger log = Logger
			.getLogger(DgFigure9ToKoehlerStrehler2010ModelConverter.class);
	
	public static void main(String[] args) throws SAXException, TransformerConfigurationException, IOException {
		MutableScenario sc = new DgFigure9ScenarioGenerator().loadScenario();
		DgIdPool idPool = new DgIdPool();
		DgIdConverter idConverter = new DgIdConverter(idPool);

		M2KS2010NetworkConverter converter = new M2KS2010NetworkConverter(idConverter);
		log.warn("Check times of demand!");
		DgKSNetwork net = converter.convertNetworkLanesAndSignals(
				sc.getNetwork(),
				(Lanes) sc.getScenarioElement(Lanes.ELEMENT_NAME),
				(SignalsData) sc.getScenarioElement(SignalsData.ELEMENT_NAME), 0.0, 3600.0);
		
		M2KS2010SimpleDemandConverter demandConverter = new M2KS2010SimpleDemandConverter();
		DgCommodities coms = demandConverter.convert(sc, net);
		
		KS2010ModelWriter writer = new KS2010ModelWriter();
		writer.write(net, coms, "Figure9Scenario", "", "../../shared-svn/studies/dgrether/koehlerStrehler2010/cplex_scenario_population_800_agents.xml");

		log.warn("Id conversions are not written, yet!");
		
	}

}
