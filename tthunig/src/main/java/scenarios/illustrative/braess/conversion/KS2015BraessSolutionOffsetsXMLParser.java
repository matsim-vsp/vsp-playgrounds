/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
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
package scenarios.illustrative.braess.conversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import optimize.cten.convert.cten2matsim.signalplans.data.FixCrossingSolution;
import optimize.cten.data.DgCrossing;
import optimize.cten.data.DgProgram;

/**
 * 
 * @author tthunig
 *
 */
public class KS2015BraessSolutionOffsetsXMLParser extends MatsimXmlParser{
	
	private final static String COORD = "coordination";
	private final static String CROSSING = "crossing";
	private final static String ID = "id";
	private final static String OFFSET = "offset";
	private final static String PROG = "prog";
	private final static String NAME = "name";
	
	private Map<String, List<FixCrossingSolution>> braessOffsets = new HashMap<>();
	private String currentCoord;
	
	public KS2015BraessSolutionOffsetsXMLParser() {
		this.setValidating(false);
	}
	
//	public void readFile(final String filename) {
//		readFile(filename);
//	}
	
	
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (name.equals(COORD)){
			this.currentCoord = atts.getValue(NAME);
			this.braessOffsets.put(this.currentCoord, new ArrayList<FixCrossingSolution>());
		}
		if (name.equals(CROSSING)){
			Id<DgCrossing> crossingId = Id.create(atts.getValue(ID), DgCrossing.class);
			int offsetSeconds = Integer.parseInt(atts.getValue(OFFSET));
			Id<DgProgram> programId = Id.create(atts.getValue(PROG), DgProgram.class); 
			FixCrossingSolution crossing = new FixCrossingSolution(crossingId, offsetSeconds, programId);
			this.braessOffsets.get(this.currentCoord).add(crossing);
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
	
	}


	public Map<String, List<FixCrossingSolution>> getBraessOffsets() {
		return braessOffsets;
	}

}
