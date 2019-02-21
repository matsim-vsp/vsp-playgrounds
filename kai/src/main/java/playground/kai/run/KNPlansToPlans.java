/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.kai.run;

import org.matsim.analysis.TransportPlanningMainModeIdentifier;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.algorithms.PlansFilterByLegMode;
import org.matsim.core.population.algorithms.PlansFilterByLegMode.FilterType;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Iterator;
import java.util.List;

import static org.matsim.core.router.TripStructureUtils.*;

/**
 * @author kn
 */
class KNPlansToPlans {

	void run(final String[] args) {
		String inputPopFilename = null ;
		String outputPopFilename = null ;
		String netFilename = null ;
				
		if ( args!=null ) {
			if ( !(args.length==2 || args.length==3) ) {
				System.err.println( "Usage: cmd inputPop.xml.gz outputPop.xml.gz [network.xml.gz]");
			} else {
				inputPopFilename = args[0] ;
				outputPopFilename = args[1] ;
				if ( args.length==3 ) {
					netFilename = args[2] ;
				}
			}
		}
		
		
		Config config = ConfigUtils.createConfig() ;
		config.network().setInputFile( netFilename ) ;
		config.plans().setInputFile( inputPopFilename ) ;

		Scenario sc = ScenarioUtils.loadScenario(config) ;
		
		Population pop = sc.getPopulation() ;

		// remove unselected plans:
//		for ( Person person : pop.getPersons().values() ) {
//			for ( Iterator<? extends Plan> it = person.getPlans().iterator() ; it.hasNext() ; ) {
//				Plan plan = it.next();
//				if ( plan.equals( person.getSelectedPlan() ) ) {
//					continue;
//				}
//				it.remove();
//			}
//		}

//		PlansFilterByLegMode pf = new PlansFilterByLegMode( TransportMode.pt, FilterType.keepAllPlansWithMode ) ;
//		pf.run(pop) ;

//		PlanMutateTimeAllocation pm = new PlanMutateTimeAllocation( 60, new Random() ) ;
//		for (Person person : pop.getPersons().values()) {
//			Plan plan = person.getPlans().iterator().next();
//			pm.run(plan);
//		}
		
//		final StageActivityTypes stageActivities = activityType -> activityType.contains( " interaction " );
//		final MainModeIdentifier mainModeIdentifier = new MainModeIdentifierImpl() ;
		//		for ( Iterator<? extends Person> personIt = pop.getPersons().values().iterator() ; personIt.hasNext() ; )  {
//
//			Person person = personIt.next() ;
//
//			// remove person if selected plan does not have pt trip:
//
//			boolean toRemove = true ;
//			final List<Trip> trips = TripStructureUtils.getTrips( person.getSelectedPlan(), stageActivities );
//			for ( Trip trip : trips ) {
//				if ( mainModeIdentifier.identifyMainMode( trip.getTripElements() ).equals( TransportMode.pt  ) ) {
//					toRemove = false ;
//				}
//			}
//			if ( toRemove ) {
//				personIt.remove();
//				continue ;
//			}
//
//		}

		for ( Iterator<? extends Person> personIt = pop.getPersons().values().iterator() ; personIt.hasNext() ; ){
			personIt.next() ;
			if ( Math.random() < 0.9 ) {
				personIt.remove();
			}
		}


		PopulationWriter popwriter = new PopulationWriter(pop,sc.getNetwork()) ;
		popwriter.write( outputPopFilename ) ;

		System.out.println("done.");
	}

	public static void main(final String[] args) {
		KNPlansToPlans app = new KNPlansToPlans();
		app.run(args);
	}

}
