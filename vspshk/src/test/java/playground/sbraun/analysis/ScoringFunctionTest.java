package playground.sbraun.analysis;

import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.TypicalDurationScoreComputation;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;


/**
* @author sbraun
*/

public class ScoringFunctionTest {

	@Test
	public final void test() {
		//scoringparam
		double start = 0.0;
		double end = 0.0;
		double typicalDuration = 20.;
		
		
		Person person =  PopulationUtils.getFactory().createPerson(Id.create("1", Person.class));
		Plan plan = PersonUtils.createAndAddPlan(person, true);
		
		Activity firstActivity = PopulationUtils.createAndAddActivity(plan, "h");
		firstActivity.setStartTime(start);
		firstActivity.setEndTime(end);
		
		
		
		//Parameter for Scoring function
		Config config = ConfigUtils.createConfig();	
		ActivityParams actParamsH = new ActivityParams("h");
		actParamsH.setTypicalDuration(typicalDuration);
		actParamsH.setLatestStartTime(Double.NEGATIVE_INFINITY);
		actParamsH.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
		
		config.planCalcScore().addActivityParams(actParamsH);
		config.planCalcScore().setPerforming_utils_hr(10.);
		
		//Szenario
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getPopulation().addPerson(person);
		
	
				
		ScoringFunctionFactory myscouringFunction = new CharyparNagelScoringFunctionFactory(scenario);
		ScoringFunction scoringFunction = myscouringFunction.createNewScoringFunction(person);
		scoringFunction.handleActivity(firstActivity);
		scoringFunction.finish();
		
		
		double defaultscore = scoringFunction.getScore();
		

		//assertEquals("The value is not zero",0., defaultscore, MatsimTestUtils.EPSILON);
		
		System.out.println("Parameters:");
		System.out.println("Duration of Activity: " +(end-start) +" ---- Typical Duration: "+ typicalDuration);
		System.out.println("gives a Score of "+defaultscore);
		
		assertTrue("The Score is a negative value", 0>=defaultscore);
	}

}
