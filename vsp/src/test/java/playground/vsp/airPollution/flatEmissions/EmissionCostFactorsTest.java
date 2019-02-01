package playground.vsp.airPollution.flatEmissions;

import org.junit.Test;

import static org.junit.Assert.*;
import static playground.vsp.airPollution.flatEmissions.EmissionCostFactors.*;

public class EmissionCostFactorsTest{

	@Test
	public void test() {

		System.out.println( "name=" + NOX.name() + "; factor=" + NOX.getCostFactor() );

		System.out.println( "noxFactor=" + EmissionCostFactors.getCostFactor( "NOX" ) ) ;

		System.out.println( "factor that does not exist=" + EmissionCostFactors.getCostFactor( "dummy" )) ;

//		for( EmissionCostFactors factor : values() ){
//			System.out.println( "name=" + factor.name() + "; factor=" + factor.getCostFactor() ) ;
//		}
	}

}
