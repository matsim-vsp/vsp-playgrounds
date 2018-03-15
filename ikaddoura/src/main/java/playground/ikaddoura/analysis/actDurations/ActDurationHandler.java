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
package playground.ikaddoura.analysis.actDurations;


import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.io.IOUtils;


/**
 * Analyzes the activity types and durations, merges overnight activities if they have the same type
 * 
 * @author ikaddoura
 *
 */
public class ActDurationHandler implements ActivityStartEventHandler, ActivityEndEventHandler {

	private Map<Id<Person>,Double> personId2startTime = new HashMap<>();
	private Map<Id<Person>,String> personId2currentActivity = new HashMap<>();
	private Map<Id<Person>, Integer> personId2actNr = new HashMap<>();
	
	private Map<Id<Person>, Map<Integer, Double>> person2actNr2duration = new HashMap<>(); // skipping the morning activity
	private Map<Id<Person>, Map<Integer, String>> person2actNr2type = new HashMap<>(); // skipping the morning activity
	
	private Map<Id<Person>, Double> personId2morningActDuration = new HashMap<>();
	private Map<Id<Person>, String> personId2morningActType = new HashMap<>();
	
	@Override
	public void reset(int iteration) {
		this.personId2startTime.clear();
		this.personId2actNr.clear();
		this.person2actNr2duration.clear();
		this.person2actNr2type.clear();
		this.personId2currentActivity.clear();
		this.personId2actNr.clear();
		this.personId2morningActDuration.clear();
		this.personId2morningActType.clear();		
	}

	public void handleEvent(ActivityStartEvent event) {
		if(!event.getActType().contains("interaction")) {
		
			personId2startTime.put(event.getPersonId(), event.getTime());
			personId2currentActivity.put(event.getPersonId(), event.getActType());
			
			if (personId2actNr.get(event.getPersonId()) == null) {
				personId2actNr.put(event.getPersonId(), 2);
				
				Map<Integer, Double> actNr2duration = new HashMap<>();
				person2actNr2duration.put(event.getPersonId(), actNr2duration);
				
				Map<Integer, String> actNr2type = new HashMap<>();
				actNr2type.put(personId2actNr.get(event.getPersonId()), event.getActType());
				person2actNr2type.put(event.getPersonId(), actNr2type);
				
			} else {
				Integer newActNumber = personId2actNr.get(event.getPersonId()) + 1;
				personId2actNr.put(event.getPersonId(), newActNumber);
			}
		}
	}
	
	public void handleEvent(ActivityEndEvent event) {
		if(!event.getActType().contains("interaction")) {
			
			if (personId2startTime.get(event.getPersonId()) != null ) {
				
				if (personId2startTime.get(event.getPersonId()) > event.getTime()) {
					throw new RuntimeException("This should not happen. Aborting...");
				}
				
				double duration = event.getTime() - personId2startTime.get(event.getPersonId());
				
				int actNr = personId2actNr.get(event.getPersonId());
				
				person2actNr2duration.get(event.getPersonId()).put(actNr, duration);
				person2actNr2type.get(event.getPersonId()).put(actNr, event.getActType());
				
			} else {
				// morning activity	
				personId2morningActDuration.put(event.getPersonId(), event.getTime());
				personId2morningActType.put(event.getPersonId(), event.getActType());
			}
			
			personId2startTime.remove(event.getPersonId());
			personId2currentActivity.remove(event.getPersonId());
		}
	}

	public void writeOutput(Population population, String filename, double maxDurationToPrintOut) {
				
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);

		try {
			bw.write("person Id ; activity number ; activity type ; activity duration [sec]");
			bw.newLine();
		
			for (Person person : population.getPersons().values()) {
				
				if (person.getId().toString().contains("freight")) {
					// skipping freight agents
					
				} else {

					if (person2actNr2duration.get(person.getId()) != null) {
						
						int lastActNr = 0;
						
						for (Integer actNr : person2actNr2duration.get(person.getId()).keySet()) {
							
							double duration = person2actNr2duration.get(person.getId()).get(actNr);
							
							if (duration < maxDurationToPrintOut) {
								bw.write(person.getId().toString() + " ; "
										+ actNr + " ; "
										+ person2actNr2type.get(person.getId()).get(actNr) + " ; "
										+ duration
										);
								
								bw.newLine();
							}
							
							if (actNr > lastActNr) lastActNr = actNr;
						}
						
						// handling overnight activity
						double endOfDay = 24 * 3600.;
						
						if (personId2startTime.get(person.getId()) != null && personId2currentActivity.get(person.getId()) != null) {
							
							double duration = endOfDay - personId2startTime.get(person.getId());
							
							if (personId2morningActType.get(person.getId()).equals(personId2currentActivity.get(person.getId()))) {
								// merge durations
								
								double mergedDuration = duration + personId2morningActDuration.get(person.getId());
								
								if (mergedDuration < maxDurationToPrintOut) {
									bw.write(person.getId().toString() + " ; "
										+ (lastActNr + 1) + " ; "
										+ personId2morningActType.get(person.getId()) + " ; "
										+ mergedDuration
										);
								
									bw.newLine();
								}
								

							} else {
								
								double morningDuration = personId2morningActDuration.get(person.getId());
								
								if (morningDuration < maxDurationToPrintOut) {
									bw.write(person.getId().toString() + " ; "
											+ 1 + " ; "
											+ personId2morningActType.get(person.getId()) + " ; "
											+ personId2morningActDuration.get(person.getId())
											);
									
									bw.newLine();
								}
								
								if (duration < maxDurationToPrintOut) {
									bw.write(person.getId().toString() + " ; "
											+ (lastActNr + 1) + " ; "
											+ personId2currentActivity.get(person.getId()) + " ; "
											+ duration
											);
									
									bw.newLine();
								}
								
							}							
						}
						
					} else {
						// person does not appear in the events, probably a stay home person
						double duration = 24 * 3600.;
						
						if (duration < maxDurationToPrintOut) {
							bw.write(person.getId().toString() + " ; 1 ; " + " probably-a-stay-home-plan ; " + duration);
							bw.newLine();
						}
					}
				}
			}
			
			bw.flush();
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
