/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.dziemke.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author dziemke
 */
public class PopulationFilterByAttribute {
	private final static Logger LOG = Logger.getLogger(PopulationFilterByAttribute.class);

	private String inputPlansFile;
	private String outputPlansFile;
	private String attributeLabel;
	private String attributeValue;

	public static void main(String[] args) {
		// Check if args has an interpretable length
		if (args.length != 0 && args.length != 3) {
			throw new IllegalArgumentException("Arguments array must have a length of 0 or 3!");
		}

		String inputPlansFile = "../../runs-svn/open_berlin_scenario/v5.3-policies/output/n2-01/berlin-v5.3-10pct-ctd-n2-01.experiencedPlans_withResidence.xml.gz";
		String outputPlansFile = "../../runs-svn/open_berlin_scenario/v5.3-policies/output/n2-01/berlin-v5.3-10pct-ctd-n2-01.experiencedPlans_withResidence_inside.xml.gz";

		String attributeLabel = "home";
		String attributeValue = "inside";

		if (args.length == 3) {
			inputPlansFile = args[0];
			outputPlansFile = args[1];
			attributeLabel = args[2];
			attributeValue = args[3];
		}

		PopulationFilterByAttribute populationFilterByAttribute = new PopulationFilterByAttribute(inputPlansFile, outputPlansFile, attributeLabel, attributeValue);
		Population population = populationFilterByAttribute.getFilteredPopulationselectPlans();
		populationFilterByAttribute.writePopulation(population);
	}

	public PopulationFilterByAttribute(String inputPlansFile, String outputPlansFile, String attributeLabel, String attributeValue) {
		this.inputPlansFile = inputPlansFile;
		this.outputPlansFile = outputPlansFile;
		this.attributeLabel = attributeLabel;
		this.attributeValue = attributeValue;
	}

	public Population getFilteredPopulationselectPlans() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(inputPlansFile);
		Population population = scenario.getPopulation();

		Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population population2 = scenario2.getPopulation();

		int agentCounterOriginal = 0;
		int agentCounterSelected = 0;

		for (Person person : population.getPersons().values()) {
			agentCounterOriginal++;
			String value = (String) person.getAttributes().getAttribute(attributeLabel);
			if (value.equals(attributeValue)) {
				population2.addPerson(person);
				agentCounterSelected++;
			}
		}
		LOG.info(agentCounterSelected + " agents out of " + agentCounterOriginal + " original agents selected into filtered population.");
		return population2;
	}

	public void writePopulation(Population population) {
		new PopulationWriter(population, null).write(outputPlansFile);
		LOG.info("Modified plans file has been written to " + outputPlansFile);
	}
}