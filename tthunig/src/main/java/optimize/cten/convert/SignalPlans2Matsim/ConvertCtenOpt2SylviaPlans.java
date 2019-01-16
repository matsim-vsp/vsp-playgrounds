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
package optimize.cten.convert.SignalPlans2Matsim;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.matsim.contrib.signals.controller.sylvia.SylviaPreprocessData;
import org.xml.sax.SAXException;

/**
 * @author tthunig
 */
public class ConvertCtenOpt2SylviaPlans {

	public static void main(String[] args)
			throws JAXBException, SAXException, ParserConfigurationException, IOException {
//		String optDir = "../../shared-svn/projects/cottbus/data/optimization/cb2ks2010/2018-06-7_minflow_50.0_time19800.0-34200.0_speedFilter15.0_SP_tt_cBB50.0_sBB500.0/btu/";
		String optDir = "../../shared-svn/projects/cottbus/data/optimization/cb2ks2010/2018-11-20-v3_minflow_50.0_time19800.0-34200.0_speedFilter15.0_SP_tt_cBB50.0_sBB500.0/btu/";
		String signalControlFile = optDir + "signal_control_optimized.xml";
		String signalControlOutFile = optDir + "signal_control_optimized_sylvia.xml";
		String[] strArray = {signalControlFile, signalControlOutFile};
		SylviaPreprocessData.main(strArray); 
	}

}
