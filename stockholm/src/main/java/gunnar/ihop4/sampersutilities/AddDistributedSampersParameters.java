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

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;

import gunnar.ihop2.regent.demandreading.PopulationCreator;
import gunnar.ihop4.sampersutilities.SampersParameterUtils.Purpose;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class AddDistributedSampersParameters {

	private final double relativeTravelTimeOverhead;

	// private Set<Purpose> allActs = new LinkedHashSet<>();

	private Map<Purpose, Double> act2minDur_h = new LinkedHashMap<>();
	private Map<Purpose, Double> act2maxDur_h = new LinkedHashMap<>();

	private Map<Purpose, Double> act2OpeningTime_h = new LinkedHashMap<>();
	private Map<Purpose, Double> act2ClosingTime_h = new LinkedHashMap<>();
	// private Map<Purpose, Double> act2minOpens_h = new LinkedHashMap<>();
	// private Map<Purpose, Double> act2maxOpens_h = new LinkedHashMap<>();
	// private Map<Purpose, Double> act2minCloses_h = new LinkedHashMap<>();
	// private Map<Purpose, Double> act2maxCloses_h = new LinkedHashMap<>();

	public AddDistributedSampersParameters(final double relativeTravelTimeOverhead) {

		this.relativeTravelTimeOverhead = relativeTravelTimeOverhead;

		this.act2minDur_h.put(Purpose.work, 4.0);
		this.act2maxDur_h.put(Purpose.work, 10.0);
		this.act2OpeningTime_h.put(Purpose.work, 7.0);
		this.act2ClosingTime_h.put(Purpose.work, 18.0);
		// this.act2minOpens_h.put(Purpose.work, 6.0);
		// this.act2maxOpens_h.put(Purpose.work, 10.0);
		// this.act2minCloses_h.put(Purpose.work, 14.0);
		// this.act2maxCloses_h.put(Purpose.work, 20.0);

		this.act2minDur_h.put(Purpose.other, 0.5);
		this.act2maxDur_h.put(Purpose.other, 2.0);
		this.act2OpeningTime_h.put(Purpose.other, 8.0);
		this.act2ClosingTime_h.put(Purpose.other, 22.0);
		// this.act2minOpens_h.put(Purpose.other, 6.0);
		// this.act2maxOpens_h.put(Purpose.other, 10.0);
		// this.act2minCloses_h.put(Purpose.other, 16.0);
		// this.act2maxCloses_h.put(Purpose.other, 23.0);

	}

	private static double drawUniform(double min, double max) {
		return (min + MatsimRandom.getRandom().nextDouble() * (max - min));
	}

	public void enrich(final Plan plan) {

		// for (int planElementIndex = 2; planElementIndex <
		// plan.getPlanElements().size(); planElementIndex += 4) {
		// final Activity act = (Activity) plan.getPlanElements().get(planElementIndex);
		// final Purpose purpose = Purpose.valueOf(act.getType());
		//
		// System.out.print("purpose = " + purpose + ", ");
		//
		// final Double opens_h = this.act2OpeningTime_h.get(purpose);
		// act.getAttributes().putAttribute(SampersParameterUtils.ActivityAttribute.opens_h.toString(),
		// opens_h);
		// System.out.print("opens = " + opens_h + ", ");
		//
		// final Double closes_h = this.act2ClosingTime_h.get(purpose);
		// act.getAttributes().putAttribute(SampersParameterUtils.ActivityAttribute.closes_h.toString(),
		// closes_h);
		// System.out.print("closes = " + closes_h + ", ");
		//
		// final Double dur_h = drawUniform(this.act2minDur_h.get(purpose),
		// this.act2maxDur_h.get(purpose));
		// act.getAttributes().putAttribute(SampersParameterUtils.ActivityAttribute.duration_h.toString(),
		// dur_h);
		// System.out.println("dur = " + dur_h);
		// }

		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				final Activity act = (Activity) pe;
				if (!PopulationCreator.HOME.equals(act.getType())
						&& (!PopulationCreator.INTERMEDIATE_HOME.equals(act.getType()))) {
					final Purpose purpose = Purpose.valueOf(act.getType());

					System.out.print("purpose = " + purpose + ", ");

					final Double opens_h = this.act2OpeningTime_h.get(purpose);
					act.getAttributes().putAttribute(SampersParameterUtils.ActivityAttribute.opens_h.toString(),
							opens_h);
					System.out.print("opens = " + opens_h + ", ");

					final Double closes_h = this.act2ClosingTime_h.get(purpose);
					act.getAttributes().putAttribute(SampersParameterUtils.ActivityAttribute.closes_h.toString(),
							closes_h);
					System.out.print("closes = " + closes_h + ", ");

					final Double dur_h = drawUniform(this.act2minDur_h.get(purpose), this.act2maxDur_h.get(purpose));
					act.getAttributes().putAttribute(SampersParameterUtils.ActivityAttribute.duration_h.toString(),
							dur_h);
					System.out.println("dur = " + dur_h);
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

		final AddDistributedSampersParameters enricher = new AddDistributedSampersParameters(0.2);
		enricher.enrich(scenario.getPopulation());

		PopulationUtils.writePopulation(scenario.getPopulation(),
				"/Users/GunnarF/NoBackup/data-workspace/ihop4/production-scenario/1PctAllModes_enriched.xml");

	}
}
