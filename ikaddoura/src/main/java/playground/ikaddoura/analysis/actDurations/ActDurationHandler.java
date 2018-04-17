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
import java.util.List;
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
	
	private Map<Id<Person>, Map<Integer, Double>> person2actNr2durationWithoutMorningAct = new HashMap<>(); // skipping the morning activity
	private Map<Id<Person>, Map<Integer, String>> person2actNr2typeWithoutMorningAct = new HashMap<>(); // skipping the morning activity
	
	private Map<Id<Person>, Double> personId2morningActDuration = new HashMap<>();
	private Map<Id<Person>, String> personId2morningActType = new HashMap<>();
	
	private Map<Id<Person>, Map<Integer, Double>> personId2actNr2duration = new HashMap<>();
	private Map<Id<Person>, Map<Integer, String>> personId2actNr2type = new HashMap<>();
	
	private boolean dataProcessed = false;
	
	private final double endOfDay = 24 * 3600.;
	
	@Override
	public void reset(int iteration) {
		this.personId2startTime.clear();
		this.personId2actNr.clear();
		this.person2actNr2durationWithoutMorningAct.clear();
		this.person2actNr2typeWithoutMorningAct.clear();
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
				person2actNr2durationWithoutMorningAct.put(event.getPersonId(), actNr2duration);
				
				Map<Integer, String> actNr2type = new HashMap<>();
				actNr2type.put(personId2actNr.get(event.getPersonId()), event.getActType());
				person2actNr2typeWithoutMorningAct.put(event.getPersonId(), actNr2type);
				
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
				
				person2actNr2durationWithoutMorningAct.get(event.getPersonId()).put(actNr, duration);
				person2actNr2typeWithoutMorningAct.get(event.getPersonId()).put(actNr, event.getActType());
				
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
				
		if (dataProcessed == false) {
			process(population, null);
		}
		
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);

		try {
			bw.write("person Id ; activity number ; activity type ; activity duration [sec]");
			bw.newLine();
			
			for (Id<Person> personId : this.personId2actNr2duration.keySet()) {
								
				for (Integer actNr : this.personId2actNr2duration.get(personId).keySet()) {
					if (this.personId2actNr2duration.get(personId).get(actNr) < maxDurationToPrintOut) {
						bw.write(personId.toString() + " ; " + actNr + " ; " + this.personId2actNr2type.get(personId).get(actNr) + " ; " + this.personId2actNr2duration.get(personId).get(actNr));
						bw.newLine();
					}
				}
			}
			
			bw.flush();
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeSummary(Population population, String filename) {
		
		if (dataProcessed == false) {
			process(population, null);
		}
		
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);

		try {
			bw.write(" activity duration from ; activity duration to ; number of activities");
			bw.newLine();
			
			bw.write("0 ; 1 ; " + computeNumberOfActivities(0, 1));
			bw.newLine();
			
			bw.write("1 ; 900 ; " + computeNumberOfActivities(1, 900));
			bw.newLine();

			bw.write("900 ; 3600 ; " + computeNumberOfActivities(900, 3600));
			bw.newLine();

			bw.write("3600 ; 7200 ; " + computeNumberOfActivities(3600, 7200));
			bw.newLine();

			bw.write("7200 ; 99999999 ; " + computeNumberOfActivities(7200, 99999999));
			bw.newLine();
			
			bw.flush();
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private int computeNumberOfActivities(int from, int to) {
		int counter = 0;
		for (Id<Person> personId : this.personId2actNr2duration.keySet()) {
			
			for (Integer actNr : this.personId2actNr2duration.get(personId).keySet()) {
				double duration = this.personId2actNr2duration.get(personId).get(actNr);
				
				if (duration >= from && duration < to) {
					counter++;
				}
			}
		}
		return counter;
	}

	public void process(Population population, List<String> skippedPersonIdStrings) {

		this.dataProcessed = true;
		
		for (Person person : population.getPersons().values()) {

			boolean proceedWithThisAgent = true;

			if (skippedPersonIdStrings != null) {
				for (String skipString : skippedPersonIdStrings) {
					if (person.getId().toString().contains(skipString)) {
						proceedWithThisAgent = false;
					}
				}
			}
			
			if (proceedWithThisAgent) {

				if (person2actNr2durationWithoutMorningAct.get(person.getId()) != null) {
					
					int lastActNr = 0;
					
					Map<Integer, Double> actNr2duration = new HashMap<>();
					this.personId2actNr2duration.put(person.getId(), actNr2duration);
					
					Map<Integer, String> actNr2type = new HashMap<>();
					this.personId2actNr2type.put(person.getId(), actNr2type);
					
					for (Integer actNr : person2actNr2durationWithoutMorningAct.get(person.getId()).keySet()) {
						
						double duration = person2actNr2durationWithoutMorningAct.get(person.getId()).get(actNr);
						
						this.personId2actNr2duration.get(person.getId()).put(actNr, duration);
						this.personId2actNr2type.get(person.getId()).put(actNr, person2actNr2typeWithoutMorningAct.get(person.getId()).get(actNr));
						
						if (actNr > lastActNr) lastActNr = actNr;
					}
					
					// handling overnight activity
					
					if (personId2startTime.get(person.getId()) != null && personId2currentActivity.get(person.getId()) != null) {
						
						double duration = endOfDay - personId2startTime.get(person.getId());
						
						if (personId2morningActType.get(person.getId()).equals(personId2currentActivity.get(person.getId()))) {
							// merge durations
							
							double mergedDuration = duration + personId2morningActDuration.get(person.getId());
							int actNr = lastActNr + 1;
			
							this.personId2actNr2duration.get(person.getId()).put(actNr, mergedDuration);
							this.personId2actNr2type.get(person.getId()).put(actNr, personId2morningActType.get(person.getId()));
			
						} else {
							
							{
								double morningDuration = personId2morningActDuration.get(person.getId());
								int actNr = 1;
								
								this.personId2actNr2duration.get(person.getId()).put(actNr, morningDuration);
								this.personId2actNr2type.get(person.getId()).put(actNr, personId2morningActType.get(person.getId()));
							}
							
							{
								double eveningDuration = duration;
								int actNr = lastActNr + 1;
								
								this.personId2actNr2duration.get(person.getId()).put(actNr, eveningDuration);
								this.personId2actNr2type.get(person.getId()).put(actNr, personId2currentActivity.get(person.getId()));
							}
							
						}							
					}
					
				} else {
					// person does not appear in the events, probably a stay home person
					double duration = this.endOfDay;
					int actNr = 1;
					
					Map<Integer, Double> actNr2duration = new HashMap<>();
					actNr2duration.put(actNr, duration);
					
					this.personId2actNr2duration.put(person.getId(), actNr2duration);
					
					Map<Integer, String> actNr2type = new HashMap<>();
					actNr2type.put(actNr, "stay-home-plan");
					this.personId2actNr2type.put(person.getId(), actNr2type);
				}
			}
		}
	}

}
