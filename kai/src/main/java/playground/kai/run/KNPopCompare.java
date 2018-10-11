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
package playground.kai.run;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.analysis.TransportPlanningMainModeIdentifier;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.analysis.kai.DataMap;
import org.matsim.contrib.analysis.kai.KNAnalysisEventsHandler;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.pt.PtConstants;

import static org.matsim.contrib.analysis.kai.KNAnalysisEventsHandler.*;

/**
 * @author nagel
 *
 */
public class KNPopCompare {
	private static final Logger logger = Logger.getLogger( KNPopCompare.class );

	// statistics types:
	private static enum StatType { ttime1, ttime2, ttimeDiff, payments1, payments2, paymentsDiff } ;
	// container that contains the statistics containers:
	private final Map<StatType,DataMap<String>> sumsContainer = new TreeMap<>() ;
	private final Map<StatType,DataMap<String>> cntsContainer = new TreeMap<>() ;
	
	private Scenario scenario1 ;
	private Scenario scenario2 ;
	
	public static void main(String[] args) {
		if ( args.length != 4 && args.length != 5 ) {
			System.out.println( "arg[0]: plans_base");
			System.out.println( "arg[1]: plans_policy");
			System.out.println( "arg[2]: extendedPersonAttribs_base");
			System.out.println( "arg[3]: extendedPersonAttribs_policy");
			System.out.println( "arg[4]: network") ;
			System.exit(-1) ;
		}
		
		new KNPopCompare().run( args ) ;
	}
		
	private void run( String[] args ) {

		Config config = ConfigUtils.createConfig();
		Population pop1 ;
		ActivityFacilitiesFactory ff ;
		{
			config.plans().setInputFile(args[0]);
			//			if ( args.length>2 && args[2]!=null && args[2]!="" ) {
			//				config.network().setInputFile(args[2]);
			//			}
			if ( args.length>2 && args[2]!=null && args[2]!="" ) {
				logger.info("setting person attributes file to: " + args[3] );
				config.plans().setInputPersonAttributeFile(args[2]);
			} else {
				throw new RuntimeException("no person attributes") ;
			}
			if ( args.length > 4 && args[4] != null && args[4] != "" ) {
				logger.info("setting network file to: " + args[4] );
				config.network().setInputFile( args[4] );
			}

			scenario1 = ScenarioUtils.loadScenario( config ) ;
			pop1 = scenario1.getPopulation() ;
			ff = ((MutableScenario)scenario1).getActivityFacilities().getFactory() ;
		}

		Population pop2 ;
		{
			Config config2 = ConfigUtils.createConfig();
			config2.plans().setInputFile(args[1]); 
			if ( args.length>3 && args[3]!=null && !args[3].equals( "" ) ) {
				logger.info("setting person attributes file to: " + args[3] );
				config2.plans().setInputPersonAttributeFile(args[3]);
			} else {
				throw new RuntimeException("no person attributes") ;
			}
			if ( args.length > 4 && args[4] != null && args[4] != "" ) {
				logger.info("setting network file to: " + args[4] );
				config2.network().setInputFile( args[4] );
			}
			scenario2 = ScenarioUtils.loadScenario( config2 ) ;
			pop2 = scenario2.getPopulation() ;
		}
		
		// yy could try to first sort the person files and then align them
		for ( Person person1 : pop1.getPersons().values() ) {
				Person person2 = pop2.getPersons().get( person1.getId() ) ;
				if ( person2==null ) {
					throw new RuntimeException( "did not find person in other population") ;
				}

				// determine coordinate of home location:
				Coord coord = ((Activity) person1.getSelectedPlan().getPlanElements().get(0)).getCoord() ;
				{ // process score differences
					final double deltaScore = person2.getSelectedPlan().getScore() - person1.getSelectedPlan().getScore();
					
					person1.getCustomAttributes().put( "deltaScore", deltaScore ) ;

					ActivityFacility fac = ff.createActivityFacility(Id.create(person1.getId().toString(), ActivityFacility.class), coord);
					fac.getCustomAttributes().put(GridUtils.WEIGHT, deltaScore ) ;
				}
				List<String> popTypes = new ArrayList<>() ;
				{ // process money differences:
					Double payments1 = (Double) pop1.getPersonAttributes().getAttribute(person1.getId().toString(), PAYMENTS ) ;
					if ( payments1==null ) {
						payments1 = 0. ;
					}
					Double payments2 = (Double) pop2.getPersonAttributes().getAttribute(person1.getId().toString(), PAYMENTS ) ;
					if ( payments2==null ) {
						payments2 = 0. ;
					}
					final double deltaPayments = payments2 - payments1;

					person1.getCustomAttributes().put( "deltaMoney", deltaPayments ) ;
					
					ActivityFacility fac = ff.createActivityFacility(Id.create(person1.getId().toString(), ActivityFacility.class), coord) ;
					fac.getCustomAttributes().put(GridUtils.WEIGHT, deltaPayments ) ; 

					// to which subPopType does the agent belong?
					popTypes.add( "zz_all" ) ; 
					
					final String subpopAttrName = config.plans().getSubpopulationAttributeName() ;
					final String subpopName = getSubpopName( person1.getId(), pop1.getPersonAttributes(), subpopAttrName);
					popTypes.add( "yy_" + subpopName ) ;

					if ( payments2!=0.) { // is a toll payer
						popTypes.add( subpopName + "_tollPayer" ) ;
					} else {
						popTypes.add( subpopName + "_nonPayer") ;
					}
					
					Double certainLinksCnt = (Double) pop1.getPersonAttributes().getAttribute( person1.getId().toString(), CERTAIN_LINKS_CNT ) ;
					if ( certainLinksCnt != null && payments2==0. ) {
						popTypes.add( subpopName + "_avoid" ) ;
					}
					
					addItemToStatsContainer( StatType.payments1, popTypes, payments1 );
					addItemToStatsContainer( StatType.payments2, popTypes, payments2 );
					addItemToStatsContainer( StatType.paymentsDiff, popTypes, deltaPayments );

				}
				{ // process travtime differences:
					Double item1 = (Double) pop1.getPersonAttributes().getAttribute(person1.getId().toString(), TRAV_TIME ) ;
					if ( item1==null ) {
						item1 = 0. ;
					}
					Double item2 = (Double) pop2.getPersonAttributes().getAttribute(person1.getId().toString(), TRAV_TIME ) ;
					if ( item2==null ) {
						item2 = 0. ;
					}
					final double deltaTtime = item2 - item1;

					person1.getCustomAttributes().put( "deltaTtime", deltaTtime ) ;
					
					ActivityFacility fac = ff.createActivityFacility(Id.create(person1.getId().toString(), ActivityFacility.class), coord) ;
					fac.getCustomAttributes().put(GridUtils.WEIGHT, deltaTtime ) ;
					
					addItemToStatsContainer( StatType.ttime1, popTypes, item1 ) ;
					addItemToStatsContainer( StatType.ttime2, popTypes, item2 ) ;
					addItemToStatsContainer( StatType.ttimeDiff, popTypes, deltaTtime ) ;

				}
//			}
		}
//		generateAndWriteSpatialGrids( network, spatialGrids, homesWithScoreDifferences, homesWithMoneyDifferences, homesWithTtimeDifferences );
		writePersonSpecificRecords( pop1, pop2 );
	}
	
	private void generateAndWriteSpatialGrids( final Network network, final List<SpatialGrid> spatialGrids, final ActivityFacilities homesWithScoreDifferences, final ActivityFacilities homesWithMoneyDifferences, final ActivityFacilities homesWithTtimeDifferences ) {
		BoundingBox bbox = null ;
		if ( network != null ) {
			bbox = BoundingBox.createBoundingBox( network );
		} else {
			bbox = BoundingBox.createBoundingBox( 300000., -2.95e6, 500000., -2.7e6 );
		}
		
		double resolution = 2000.;
		{
			SpatialGrid spatialGrid = new SpatialGrid( bbox, resolution, 0. ) ;
			spatialGrid.setLabel("scoreDiffSum");
			SpatialGrid spatialGridCnt = new SpatialGrid( bbox, resolution, 0. ) ;
			spatialGridCnt.setLabel("scoreDiffCnt");
			SpatialGrid spatialGridAv = new SpatialGrid( bbox, resolution, 0. ) ;
			spatialGridAv.setLabel("scoreDiffAv");
			GridUtils.aggregateFacilitiesIntoSpatialGrid(homesWithScoreDifferences, spatialGrid, spatialGridCnt, spatialGridAv);
			spatialGrids.add( spatialGrid ) ;
			spatialGrids.add( spatialGridCnt ) ;
			spatialGrids.add( spatialGridAv ) ;
		}
		
		{
			SpatialGrid spatialGrid = new SpatialGrid( bbox, resolution, 0. ) ;
			spatialGrid.setLabel("moneyDiffSum");
			SpatialGrid spatialGridCnt = new SpatialGrid( bbox, resolution, 0. ) ;
			spatialGridCnt.setLabel("moneyDiffCnt");
			SpatialGrid spatialGridAv = new SpatialGrid( bbox, resolution, 0. ) ;
			spatialGridAv.setLabel("moneyDiffAv");
			GridUtils.aggregateFacilitiesIntoSpatialGrid(homesWithMoneyDifferences, spatialGrid, spatialGridCnt, spatialGridAv );
			spatialGrids.add( spatialGrid ) ;
			spatialGrids.add( spatialGridCnt ) ;
			spatialGrids.add( spatialGridAv ) ;
		}
		
		{
			SpatialGrid spatialGrid = new SpatialGrid( bbox, resolution, 0. ) ;
			spatialGrid.setLabel("ttimeDiffSum");
			SpatialGrid spatialGridCnt = new SpatialGrid( bbox, resolution, 0. ) ;
			spatialGridCnt.setLabel("ttimeDiffCnt");
			SpatialGrid spatialGridAv = new SpatialGrid( bbox, resolution, 0. ) ;
			spatialGridAv.setLabel("ttimeDiffAv");
			GridUtils.aggregateFacilitiesIntoSpatialGrid(homesWithTtimeDifferences, spatialGrid, spatialGridCnt, spatialGridAv );
			spatialGrids.add( spatialGrid ) ;
			spatialGrids.add( spatialGridCnt ) ;
			spatialGrids.add( spatialGridAv ) ;
		}
		
		GridUtils.writeSpatialGrids(spatialGrids, "popcompare_grid.csv");
		
		try ( BufferedWriter writer = IOUtils.getBufferedWriter("popcompare.txt") ) {
			// the excel "open" trick works only with txt (and tab, I think)
			writer.write( "#subpoptype") ;
			for ( StatType statType : StatType.values() ) {
				writer.write( "\t" + statType.toString() + "_sum \t" + statType.toString() + "_cnt \t" + statType.toString() + "_av" ) ;
			}
			writer.newLine() ;
			for ( String popType : sumsContainer.get( StatType.ttime1).keySet() ) { // abusing ttime1 container to get the poptypes
				writer.write( popType.toString() + ":" ) ;
				for ( StatType statType : StatType.values() ) {
					final DataMap<String> sumsContainer_statType = sumsContainer.get(statType);
					Double sum ;
					if ( sumsContainer_statType != null ) {
						sum = sumsContainer_statType.get(popType);
					} else {
						throw new RuntimeException( "null: " + statType.toString() ) ;
					}
					final Double cnt = cntsContainer.get(statType).get(popType);
					writer.write( "\t" + sum + "\t" + cnt + "\t" + sum/cnt ) ;
				}
				writer.newLine();
			}
			writer.close();
		} catch ( IOException e) {
			e.printStackTrace();
		}
	}
	
	private void writePersonSpecificRecords( final Population pop1, final Population pop2 ) {
		StageActivityTypes stateActivityTypes = activityType -> activityType.contains( " interaction" ) ;
		MainModeIdentifier mainModeIdentifier = new TransportPlanningMainModeIdentifier() ;
		
		try ( BufferedWriter writer = IOUtils.getBufferedWriter("personscompare.txt") ) {

			boolean first = true ;
			for ( Person person : pop1.getPersons().values() ) {
				if ( first ) {
					first = false ;
					writer.write("personId\tx\ty\tdeltaPt\tdeltaBicycle\tmode1\tmode2");
					for ( Object key : person.getCustomAttributes().keySet() ) { // design is such that this might vary person by person :-(
						writer.write( "\t" + key );
					}
					writer.newLine() ;
				}
				// ===
				writer.write( person.getId().toString() ) ;
				// ---
				final Activity activity = (Activity) person.getSelectedPlan().getPlanElements().get( 0 );
				Coord coord = PopulationUtils.decideOnCoordForActivity( activity, scenario1 ) ;
				
				
				writer.write( "\t" + coord.getX() + "\t" + coord.getY() );
				// ---
				double deltaPt = 0 ;
				double deltaBicycle = 0 ;
				String mode1="" ;
				{
					// pop1 is base
					List<Trip> trips = TripStructureUtils.getTrips( person.getSelectedPlan(), stateActivityTypes ) ;
					//				for ( Trip trip : trips ) {
					Trip trip = trips.get(0) ;
					mode1 = mainModeIdentifier.identifyMainMode( trip.getTripElements() ) ;
					if ( mode1.equals( TransportMode.pt ) ) {
						deltaPt -- ;
					} else if ( mode1.equals( TransportMode.bike ) ) {
						deltaBicycle-- ;
					}
					//				}
				}
				String mode2="" ;
				{
					// pop1 is policy
					Person person2 = pop2.getPersons().get( person.getId() ) ;
					List<Trip> trips = TripStructureUtils.getTrips( person2.getSelectedPlan(), stateActivityTypes ) ;
					//				for ( Trip trip : trips ) {
					Trip trip = trips.get(0) ;
					mode2 = mainModeIdentifier.identifyMainMode( trip.getTripElements() ) ;
					if ( mode2.equals( TransportMode.pt ) ) {
						deltaPt ++ ;
					} else if ( mode2.equals( TransportMode.bike ) ) {
						deltaBicycle ++ ;
					}
					//				}
				}
				writer.write("\t" + deltaPt + "\t" + deltaBicycle + "\t" + mode1 + "\t" + mode2 );
				// ---
				for ( Entry<String,Object> entry : person.getCustomAttributes().entrySet() ) {
					writer.write( /* "\t" + entry.getKey() + */ "\t" + entry.getValue() ) ;
				}
				writer.newLine();
			}
			
		} catch ( IOException ee ) {
			ee.printStackTrace();
		}
	}
	
	private void addItemToStatsContainer(StatType statType, List<String> popTypes, Double item) {
		DataMap<String> sums = sumsContainer.get( statType ) ;
		if ( sums == null ) {
			sums = new DataMap<>() ;
			sumsContainer.put( statType, sums ) ;
		}
		DataMap<String> cnts = cntsContainer.get( statType ) ;
		if ( cnts == null ) {
			cnts = new DataMap<>() ;
			cntsContainer.put( statType, cnts ) ;
		}
		for ( String popType : popTypes ) {
			sums.addValue( popType, item ) ;
			cnts.inc( popType ) ;
		}
	}



}
