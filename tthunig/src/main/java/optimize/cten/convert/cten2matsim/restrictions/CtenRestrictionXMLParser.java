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

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import optimize.cten.data.DgCrossing;
import optimize.cten.data.DgCrossingNode;
import optimize.cten.data.DgStreet;
import optimize.cten.data.TtRestriction;

/**
 * @author tthunig
 */
public class CtenRestrictionXMLParser extends MatsimXmlParser {
	
	private final static Logger LOG = Logger.getLogger(CtenRestrictionXMLParser.class);

	private final static String ID = "id";
	private final static String FROM_NODE = "from";
	private final static String TO_NODE = "to";
	private final static String LIGHT = "light"; // as ID and start tag
	private final static String ALLOWED = "allowed";
	private final static String RESTRICTION = "restriction";
	private final static String CROSSING = "crossing";
	private final static String RESTRICTED_LIGHT = "rlight";
	private final static String ON = "on";
	private final static String OFF = "off";
	private final static String BEFORE = "before";
	private final static String AFTER = "after";
	
	
	private Map<Id<DgCrossing>, DgCrossing> crossings = new HashMap<>();
	
	private DgCrossing currentCrossing;
	private TtRestriction currentRestriction;
	
	public CtenRestrictionXMLParser() {
		this.setValidating(false);
	}
	
	@Override
	public void endTag(String arg0, String arg1, Stack<String> arg2) {
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (name.equals(CROSSING)) {
			// create crossing
			Id<DgCrossing> crossingId = Id.create(atts.getValue(ID), DgCrossing.class);
			this.currentCrossing = new DgCrossing(crossingId);
			crossings.put(crossingId, currentCrossing);
		} else if (name.equals(LIGHT)) {
			// create light and add to crossing
			this.currentCrossing.addLight(new DgStreet(Id.create(atts.getValue(ID), DgStreet.class), 
					new DgCrossingNode(Id.create(atts.getValue(FROM_NODE), DgCrossingNode.class)), 
					new DgCrossingNode(Id.create(atts.getValue(TO_NODE), DgCrossingNode.class))));
		} else if (name.equals(RESTRICTION)) {
			this.currentRestriction = new TtRestriction(Id.create(atts.getValue(LIGHT), DgStreet.class), 
					Boolean.getBoolean(atts.getValue(ALLOWED)));
			this.currentCrossing.addRestriction(currentRestriction);
		} else if (name.equals(RESTRICTED_LIGHT)) {
			if (atts.getValue(LIGHT) != null) {
				this.currentRestriction.addAllowedLight(Id.create(atts.getValue(LIGHT), DgStreet.class), Integer.valueOf(atts.getValue(BEFORE)), Integer.valueOf(atts.getValue(AFTER)));
			} else if (atts.getValue(ON) != null) {
				this.currentRestriction.addOnLight(Id.create(atts.getValue(ON), DgStreet.class));
			} else if (atts.getValue(OFF) != null) {
				this.currentRestriction.addOffLight(Id.create(atts.getValue(OFF), DgStreet.class));
			} else {
				throw new RuntimeException("There is a restriction for light " + this.currentRestriction.getLightId() + " that can not be parsed.");
			}
		}
	}
	
	public Map<Id<DgCrossing>, DgCrossing> getCrossings() {
		return crossings;
	}

}
