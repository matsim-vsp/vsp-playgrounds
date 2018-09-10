package cba.misc;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class EstimateTimePressure {

	private EstimateTimePressure() {
	}

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		final String networkFileName = "/Nobackup/Profilen/git/matsim/playgrounds/stockholm/ihop2-data/network-output/network.xml";
		final String populationFileName = "/Nobackup/Profilen/Documents/proposals/2015/IHOP2/"
				+ "showcase/2015-11-23ab_LARGE_RegentMATSim/2015-11-23a_No_Toll_large/"
				+ "summary/iteration-3/it.400/400.plans.xml.gz";

		final Config config = ConfigUtils.createConfig();
		config.getModule("network").addParam("inputNetworkFile", networkFileName);
		config.getModule("plans").addParam("inputPlansFile", populationFileName);
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final OLSMultipleLinearRegression regr = new OLSMultipleLinearRegression();

		for (Person person : scenario.getPopulation().getPersons().values()) {

			final Plan plan = person.getSelectedPlan();
			System.out.println("Person " + person.getId() + " with selected plan score " + plan.getScore());

			final List<String> types = new ArrayList<>(3);
			final List<Double> desiredDurations_s = new ArrayList<>(3);
			final List<Double> realizedDurations_s = new ArrayList<>(3);

			double lastLegEndTime_s = 0.0;
			double homeDuration_s = 0.0;

			for (PlanElement planElement : plan.getPlanElements()) {

				if (planElement instanceof Leg) {

					final Leg leg = (Leg) planElement;
					lastLegEndTime_s = leg.getDepartureTime() + leg.getTravelTime();

				}
				if (planElement instanceof Activity) {
					final Activity currentAct = (Activity) planElement;

					if (currentAct.getType().toUpperCase().startsWith("H")) {
						homeDuration_s += Math.min(24 * 3600, currentAct.getEndTime()) - lastLegEndTime_s;
					} else if (currentAct.getType().toUpperCase().startsWith("W")) {
						types.add(currentAct.getType());
						desiredDurations_s.add(8.0 * 3600);
						realizedDurations_s.add(currentAct.getEndTime() - lastLegEndTime_s);
					} else if (currentAct.getType().toUpperCase().startsWith("O")) {
						types.add(currentAct.getType());
						desiredDurations_s.add(1.5 * 3600);
						realizedDurations_s.add(currentAct.getEndTime() - lastLegEndTime_s);
					}
				}
			}

			types.add("home");
			desiredDurations_s.add(13.5 * 3600);
			realizedDurations_s.add(homeDuration_s);

			// AND NOW HERE THE MODEL SHOULD BE ESTIMATED

			for (int i = 0; i < types.size(); i++) {
				System.out.println(types.get(i));
				System.out.println(realizedDurations_s.get(i));
				System.out.println(desiredDurations_s.get(i));
				System.out.println();
			}

		}

		System.out.println("... DONE");

	}

}
