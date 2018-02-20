/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.cadyts.multiModeCadyts;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;

/**
 * A class to create a object which contains modal and link information. 
 * 
 * @author amit
 */

public final class ModalLink implements Identifiable<ModalLink> {
	
	private final String mode;
	private final Id<Link> linkId;
	private final Id<ModalLink> id;
	
	private static final String separator = "_&_";
	
	public String getMode() {
		return mode;
	}

	public Id<Link> getLinkId() {
		return linkId;
	}

	ModalLink(final String mode, final Id<Link> linkId) {
		this.mode = mode;
		this.linkId = linkId;
		this.id = Id.create(this.mode.concat(getModeLinkSplitter()).concat(this.linkId.toString()), ModalLink.class);
	}

	@Override
	public String toString(){
		return this.mode.concat(getModeLinkSplitter()).concat(this.linkId.toString());
	}
	
	public static String getModeLinkSplitter(){
		return separator;
	}

	@Override
	public Id<ModalLink> getId() {
		return this.id;
	}
}
