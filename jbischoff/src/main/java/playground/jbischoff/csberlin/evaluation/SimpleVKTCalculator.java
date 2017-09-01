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

/**
 * 
 */
package playground.jbischoff.csberlin.evaluation;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class SimpleVKTCalculator {
	final static String EVENTSFILE = "D:/runs-svn/bmw_carsharing/avparking/randombehavior/output_events.xml.gz";
	final static String NETWORKFILE = "D:/runs-svn/bmw_carsharing/avparking/randombehavior/output_network.xml.gz";

	
	// according to http://www.co2online.de/klima-schuetzen/mobilitaet/auto-co2-ausstoss/
	
	public static void main(String[] args) {
		new SimpleVKTCalculator().run();
	}

	private void run(){
		MutableDouble avkm = new MutableDouble();
		MutableDouble carkm = new MutableDouble();
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(NETWORKFILE);
		EventsManager events = EventsUtils.createEventsManager();
		
        events.addHandler(new LinkEnterEventHandler() {
			
			@Override
			public void reset(int iteration) {
			
			}
			
			@Override
			public void handleEvent(LinkEnterEvent event) {
				double length = network.getLinks().get(event.getLinkId()).getLength() / 1000.0;
				if (event.getVehicleId().toString().endsWith("av")){
					avkm.add(length);
				} else {
					carkm.add(length);
				}
			}
		});
        new MatsimEventsReader(events).readFile(EVENTSFILE);
        
        System.out.println("avkm: "+avkm);
        System.out.println("carkm: "+carkm);
        
	}
}
