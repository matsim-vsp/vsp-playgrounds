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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;

import gunnar.ihop4.sampersutilities.SampersParameterUtils.Purpose;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class AddDistributedSampersParameters {

	private Set<Purpose> allActs = new LinkedHashSet<>();

	private Map<Purpose, Double> act2minDur_h = new LinkedHashMap<>();
	private Map<Purpose, Double> act2maxDur_h = new LinkedHashMap<>();
	private Map<Purpose, Double> act2minOpens_h = new LinkedHashMap<>();
	private Map<Purpose, Double> act2maxOpens_h = new LinkedHashMap<>();
	private Map<Purpose, Double> act2minCloses_h = new LinkedHashMap<>();
	private Map<Purpose, Double> act2maxCloses_h = new LinkedHashMap<>();

	public AddDistributedSampersParameters() {

		this.act2minDur_h.put(Purpose.work, 4.0);
		this.act2maxDur_h.put(Purpose.work, 10.0);
		this.act2minOpens_h.put(Purpose.work, 6.0);
		this.act2maxOpens_h.put(Purpose.work, 10.0);
		this.act2minCloses_h.put(Purpose.work, 14.0);
		this.act2maxCloses_h.put(Purpose.work, 20.0);

		this.act2minDur_h.put(Purpose.other, 0.5);
		this.act2maxDur_h.put(Purpose.other, 2.0);
		this.act2minOpens_h.put(Purpose.other, 6.0);
		this.act2maxOpens_h.put(Purpose.other, 10.0);
		this.act2minCloses_h.put(Purpose.other, 16.0);
		this.act2maxCloses_h.put(Purpose.other, 23.0);

	}

	private static double drawUniform(double min, double max) {
		return (min + MatsimRandom.getRandom().nextDouble() * (max - min));
	}

	public void enrich(final Plan plan) {

		final List<Purpose> purposes = new ArrayList<>();
		
		for (int planElementIndex = 2; planElementIndex < plan.getPlanElements().size(); planElementIndex += 4) {
			final Activity act = (Activity) plan.getPlanElements().get(planElementIndex);
			final Purpose purpose = Purpose.valueOf(act.getType());
			purposes.add(purpose);
			
			// TODO continue here
			
		}


		
		for (Purpose purpose : this.allActs) {

			final double dur_h = drawUniform(this.act2minDur_h.get(purpose), this.act2maxDur_h.get(purpose));
			double opens_h = drawUniform(this.act2minOpens_h.get(purpose), this.act2maxOpens_h.get(purpose));
			double closes_h = drawUniform(this.act2minCloses_h.get(purpose), this.act2maxCloses_h.get(purpose));

			final double center_h = 0.5 * (opens_h + closes_h);
			opens_h = Math.min(opens_h, center_h - 0.5 * dur_h);
			closes_h = Math.max(closes_h, center_h + 0.5 * dur_h);
		}
	}
}
