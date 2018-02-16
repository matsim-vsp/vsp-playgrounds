/* *********************************************************************** *
 * project: org.matsim.*                                                   *
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

package playground.ikaddoura.berlinTimeMutation;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author ikaddoura
 */
public class CemdapStopsDurationParser {

	private final static Logger LOG = Logger.getLogger(CemdapStopsDurationParser.class);

	private static final int P_ID = 1;
	private static final int STOP_DUR = 7;

	public final void parse(Population population, String cemdapStopsFile, String outputplansfile) {
		int lineCount = 0;

		try {
			BufferedReader bufferedReader = IOUtils.getBufferedReader(cemdapStopsFile);
			String currentLine;
			Id<Person> currentPersonId = null;
			int currentActivityNumber = 0;

			Map<Id<Person>, Map<Integer, Double>> personId2actNr2cemdapDuration = new HashMap<>();
			
			while ((currentLine = bufferedReader.readLine()) != null) {
				String[] entries = currentLine.split("\t", -1);
				lineCount++;
				
				if (lineCount % 1000000 == 0) {
					LOG.info("Line " + lineCount);// + ": " + population.getPersons().size() + " persons stored so far.");
					Gbl.printMemoryUsage();
				}

				Id<Person> personId = Id.create(Integer.parseInt(entries[P_ID]), Person.class);
				
				if (currentPersonId == null || !personId.toString().equals(currentPersonId.toString())) {
					currentActivityNumber = 0;
					currentPersonId = personId;
				} else {				
					currentActivityNumber++;
				}
				
				int durationMinutes = Integer.parseInt(entries[STOP_DUR]);
				
				if (personId2actNr2cemdapDuration.get(personId) == null) {
					Map<Integer, Double> actNr2duration = new HashMap<>();
					actNr2duration.put(currentActivityNumber, durationMinutes * 60.);
					personId2actNr2cemdapDuration.put(personId, actNr2duration);
				} else {
					personId2actNr2cemdapDuration.get(personId).put(currentActivityNumber, durationMinutes * 60.);
				}		
			}
			
			LOG.info(lineCount + " lines parsed.");
			
			for (Id<Person> personId : population.getPersons().keySet()) {
				
				if (personId2actNr2cemdapDuration.get(personId) == null) {				
					LOG.warn("Person Id " + personId + " not found in the cemdap stops file.");

					PopulationWriter popWriter = new PopulationWriter(population);
					popWriter.write(outputplansfile);
					
					throw new RuntimeException("Aborting... Plansfile doesn't contain all agents and shouldn't be used.");
				}

				int personActivityCounter = 0;

				for (PlanElement pE : population.getPersons().get(personId).getSelectedPlan().getPlanElements()) {
					if (pE instanceof Activity) {
						Activity act = (Activity) pE;

						if (personActivityCounter < personId2actNr2cemdapDuration.get(personId).size()) {							
							double duration = personId2actNr2cemdapDuration.get(personId).get(personActivityCounter);
							act.getAttributes().putAttribute("cemdap-duration", duration);
							
							// use duration instead of activity end time if cemdap-duration <= 30 min
							if (duration <= 30. * 60.) {
								act.setEndTime(Double.MIN_VALUE);
								act.setMaximumDuration(duration);
							}
													
						} else if (personActivityCounter == personId2actNr2cemdapDuration.get(personId).size()) {
							// skip last activity, probably overnight home activity
						} else {						
							
							LOG.warn("Activity number " + personActivityCounter + " of person Id " + personId + " not found in the cemdap stops file.");

							PopulationWriter popWriter = new PopulationWriter(population);
							popWriter.write(outputplansfile);
							
							throw new RuntimeException("Aborting... Plansfile doesn't contain all agents and shouldn't be used.");
						}

						personActivityCounter++;

					}
				}
			}
			
			PopulationWriter popWriter = new PopulationWriter(population);
			popWriter.write(outputplansfile);
			
		} catch (IOException e) {
			LOG.error(e);
		}		
	}
	
}