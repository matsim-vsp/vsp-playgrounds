package playground.kai.bvwp;

import org.matsim.core.basic.v01.IdImpl;

import playground.kai.bvwp.Values.Entry;
import playground.kai.bvwp.Values.Mode;
import playground.kai.bvwp.Values.Type;

/**
 * @author Ihab
 *
 */

class ScenarioFictiveExampleSep2012GV { // Relationsbezogen_mit_generalisierten_Kosten

	static ScenarioForEvalData createNullfall1() {
		// set up the base case:
		ScenarioForEvalData nullfall = new ScenarioForEvalData() ;
	
		// construct values for one OD relation:
		Values nullfallForOD = new Values() ;
		nullfall.setValuesForODRelation(new IdImpl("BC"), nullfallForOD ) ;
		{
			// construct values for the road mode for this OD relation:
			ValuesForAMode roadValues = nullfallForOD.getByMode(Mode.road) ;
			{
				// passenger traffic:
				ValuesForAUserType valuesRoad = roadValues.getByDemandSegment(Type.GV) ;
				valuesRoad.setByEntry( Entry.XX, 3000. ) ; // number of persons
				valuesRoad.setByEntry( Entry.km, 38. ) ;
				valuesRoad.setByEntry( Entry.hrs, 0.3 ) ;
			}			
			
			// construct values for the rail mode for this OD relation:
			ValuesForAMode railValues = nullfallForOD.getByMode(Mode.rail) ;
			{
				// passenger traffic:
				ValuesForAUserType valuesRail = railValues.getByDemandSegment(Type.GV) ;
				valuesRail.setByEntry( Entry.XX, 2000. ) ; // number of persons
				valuesRail.setByEntry( Entry.km, 41. ) ;
				valuesRail.setByEntry( Entry.hrs, 0.43 ) ; 
				valuesRail.setByEntry( Entry.priceUser, 41.*0.1 ) ;
				valuesRail.setByEntry( Entry.costOfProduction, 41.*0.1 ) ;
			}			
			
//			
//			// rail values are just a copy of the road values:
//			ValuesForAMode railValues = roadValues.createDeepCopy() ;
//			nullfallForOD.setValuesForMode( Mode.rail, railValues ) ;
		}
		
		// return the base case:
		return nullfall;
	}

	static ScenarioForEvalData createPlanfall1(ScenarioForEvalData nullfall) {
		// (construct the policy case.  The base case can be used to simplify things ...)
		
		// The policy case is initialized as a complete copy of the base case:
		ScenarioForEvalData planfall = nullfall.createDeepCopy() ;
		
		// we are now looking at one specific OD relation (for this scenario, there is only one!)
		Values planfallForOD = planfall.getByODRelation(new IdImpl("BC")) ;
		{
			// modify the travel times for the rail mode:
			ValuesForAMode railValues = planfallForOD.getByMode( Mode.rail ) ;
			railValues.getByDemandSegment(Type.GV).incByEntry( Entry.hrs, -0.1 ) ;
			
			// modify some demand (presumably as a result):
			double delta = 100. ;
			railValues.getByDemandSegment(Type.GV).incByEntry( Entry.XX, delta ) ;
			planfall.getByODRelation(new IdImpl("BC")).getByMode(Mode.road).getByDemandSegment(Type.GV).incByEntry(Entry.XX, -delta ) ;
		}
		return planfall;
	}

}

