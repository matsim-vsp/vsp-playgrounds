/* *********************************************************************** *
 * project: org.matsim.*
 * LinksEventHandler.java
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

/**
 * 
 */
package playground.sbraun.actDurationAnalysis;


import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.io.BufferedWriter;
import java.io.IOException;


/**
 * @author sbraun
 *
 */
public class ActDurationHandler implements ActivityStartEventHandler,ActivityEndEventHandler {

	Map<Id<Person>,Double> duration_la = new HashMap<>();
	Map<Id<Person>,String> Activitys = new HashMap<>();
	List<String> activityChain = new ArrayList<>();
	String agent_activity;
	double duration;
	Network network;

	
	
	public ActDurationHandler(Network network) {
		this.network = network;
	}


	@Override
	public void reset(int iteration) {

	}

	
	
	public void handleEvent(ActivityStartEvent event) {
		if(!event.getActType().equals("pt interaction")) {
			duration_la.put(event.getPersonId(), event.getTime());
			Activitys.put(event.getPersonId(), event.getActType());
		}
	}
	
	public void handleEvent(ActivityEndEvent event) {
		if((Activitys.get(event.getPersonId())!=null || duration_la.get(event.getPersonId())!=null)&& !event.getActType().equals("pt interaction")) {
			String agent_activity = event.getPersonId() +";"+Activitys.get(event.getPersonId()) + ";" +(event.getTime()-duration_la.get(event.getPersonId()))+"sec;";
			activityChain.add(agent_activity);
		}
	}

	public void output(String filename){
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try {
			bw.write("AgentId ; activitytype ; Duration");
			bw.newLine();
		
			for (String line : activityChain){
				bw.write(line);
				bw.newLine();
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
