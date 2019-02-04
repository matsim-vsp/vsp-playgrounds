/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package gunnar.ihop4.sampersutilities;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;

import cadyts.utilities.misc.Units;
import gunnar.ihop2.regent.demandreading.PopulationCreator;
import gunnar.ihop4.sampersutilities.SampersParameterUtils.Purpose;
import gunnar.rvu2013.RVU2013Analyzer;
import gunnar.rvu2013.TourSequenceTimeStructures.TimeStructure;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class AddSampersTimeParametersFromRVU {

	private final RVU2013Analyzer rvuAnalyzer;

	public AddSampersTimeParametersFromRVU(final String rvuFile) {
		this.rvuAnalyzer = new RVU2013Analyzer(rvuFile);
	}

	private final Map<List<String>, LinkedList<TimeStructure>> purposes2timeStructures = new LinkedHashMap<>();

	private TimeStructure drawTimeStructure(final String... purposes) {
		final List<String> purposeList = Arrays.asList(purposes);
		LinkedList<TimeStructure> timeStructures = this.purposes2timeStructures.get(purposeList);
		if (timeStructures == null || timeStructures.size() == 0) {
			timeStructures = new LinkedList<>(this.rvuAnalyzer.getTimeStructures().getTimeStructures(purposes));
			Collections.shuffle(timeStructures);
			this.purposes2timeStructures.put(purposeList, timeStructures);
		}
		return timeStructures.removeFirst();
	}

	public void enrich(final Plan plan) {

		boolean containsWork = false;
		boolean containsOther = false;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				final Activity act = (Activity) pe;
				containsWork |= PopulationCreator.WORK.equals(act.getType());
				containsOther |= PopulationCreator.OTHER.equals(act.getType());
			}
		}

		final TimeStructure timeStructure;
		{
			if (containsWork && !containsOther) {
				timeStructure = this.drawTimeStructure(Purpose.work.toString());
			} else if (!containsWork && containsOther) {
				timeStructure = this.drawTimeStructure(Purpose.other.toString());
			} else if (containsWork && containsOther) {
				timeStructure = this.drawTimeStructure(Purpose.work.toString(), Purpose.other.toString());
			} else {
				throw new RuntimeException("containsWork = " + containsWork + ", containsOsther = " + containsOther);
			}
		}

		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				final Activity act = (Activity) pe;

				if (PopulationCreator.INTERMEDIATE_HOME.equals(act.getType())) {

					act.getAttributes().putAttribute(SampersParameterUtils.ActivityAttribute.duration_h.toString(),
							Units.H_PER_S * timeStructure.intermedHomeDur_s(0));

				} else {

					if (Purpose.work.toString().equals(act.getType())) {

						act.getAttributes().putAttribute(SampersParameterUtils.ActivityAttribute.start_h.toString(),
								Units.H_PER_S * timeStructure.start_s(0));
						act.getAttributes().putAttribute(SampersParameterUtils.ActivityAttribute.duration_h.toString(),
								Units.H_PER_S * timeStructure.duration_s(0));

					} else if (Purpose.other.toString().equals(act.getType())) {

						int otherTourIndex = (containsWork ? 1 : 0);

						act.getAttributes().putAttribute(SampersParameterUtils.ActivityAttribute.start_h.toString(),
								Units.H_PER_S * timeStructure.start_s(otherTourIndex));
						act.getAttributes().putAttribute(SampersParameterUtils.ActivityAttribute.duration_h.toString(),
								Units.H_PER_S * timeStructure.duration_s(otherTourIndex));

					}
				}
			}
		}
	}

	public void enrich(final Population population) {
		for (Person person : population.getPersons().values()) {

			if (person.getAttributes()
					.getAttribute(SampersParameterUtils.PersonAttribute.income_SEK_yr.toString()) == null) {
				person.getAttributes().putAttribute(SampersParameterUtils.PersonAttribute.income_SEK_yr.toString(), 0);
			}

			PersonUtils.removeUnselectedPlans(person);
			this.enrich(person.getSelectedPlan());
		}
	}

	public static void main(String[] args) {

		final Config config = ConfigUtils
				.loadConfig("/Users/GunnarF/NoBackup/data-workspace/ihop4/production-scenario/config.xml");
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final AddSampersTimeParametersFromRVU enricher = new AddSampersTimeParametersFromRVU(
				"/Users/GunnarF/OneDrive - VTI/My Data/ihop4/rvu2013/MDRE_1113_original.csv");
		enricher.enrich(scenario.getPopulation());

		PopulationUtils.writePopulation(scenario.getPopulation(),
				"/Users/GunnarF/NoBackup/data-workspace/ihop4/production-scenario/1PctAllModes_enriched.xml");

	}

}
