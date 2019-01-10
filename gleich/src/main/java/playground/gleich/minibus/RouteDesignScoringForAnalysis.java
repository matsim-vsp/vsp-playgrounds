package playground.gleich.minibus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.PConfigGroup.RouteDesignScoreParams;
import org.matsim.contrib.minibus.PConfigGroup.RouteDesignScoreParams.StopListToEvaluate;
import org.matsim.contrib.minibus.operator.PPlan;
import org.matsim.contrib.minibus.scoring.routeDesignScoring.RouteDesignScoringManager;
import org.matsim.contrib.minibus.scoring.routeDesignScoring.RouteDesignScoringManager.RouteDesignScoreFunctionName;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Run RouteDesignScoring functions of the minibus contrib on an existing TransitSchedule as an analysis measure.
 * 
 * @author gleich
 *
 */
public class RouteDesignScoringForAnalysis {
	
	private final static Logger log = Logger.getLogger(RouteDesignScoringForAnalysis.class);

	public static void main(String[] args) {
		String networkFile = "/home/gregor/git/capetown/output-minibus-wo-transit/lastGoodRun/output_network.xml.gz";
//		String scheduleFile = "/home/gregor/tmpDumpCluster/capetown/output-minibus-wo-transit/2018-12-10_100pct_withoutRouteDesignScoring/ITERS/it.1500/1500.transitScheduleScored.xml.gz";
		String scheduleFile = "/home/gregor/svn/runs-svn/capetown-minibuses/output-minibus-wo-transit/100pct/2018-12-03_100pct_1500it/ITERS/it.1500/1500.transitScheduleScored.xml.gz";
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		config.transit().setTransitScheduleFile(scheduleFile);
		config.global().setCoordinateSystem("SA_Lo19");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		// Configure scoring functions
		PConfigGroup pConfigStop2Stop = new PConfigGroup();
		RouteDesignScoreParams stop2stopVsBeeline = new RouteDesignScoreParams();
		stop2stopVsBeeline.setRouteDesignScoreFunction(RouteDesignScoreFunctionName.stop2StopVsBeelinePenalty);
		stop2stopVsBeeline.setCostFactor(-800);
		stop2stopVsBeeline.setStopListToEvaluate(StopListToEvaluate.transitRouteAllStops);
		stop2stopVsBeeline.setValueToStartScoring(2.6);
		pConfigStop2Stop.addRouteDesignScoreParams(stop2stopVsBeeline);
		
		RouteDesignScoringManager managerStop2Stop = new RouteDesignScoringManager();
		managerStop2Stop.init(pConfigStop2Stop, scenario.getNetwork());
		
		PConfigGroup pConfigAreaVsBeeline = new PConfigGroup();
		RouteDesignScoreParams areaVsBeeline = new RouteDesignScoreParams();
		areaVsBeeline.setRouteDesignScoreFunction(RouteDesignScoreFunctionName.areaBtwLinksVsBeelinePenalty);
		areaVsBeeline.setCostFactor(-800);
		areaVsBeeline.setStopListToEvaluate(StopListToEvaluate.transitRouteAllStops);
		areaVsBeeline.setValueToStartScoring(60);
		pConfigAreaVsBeeline.addRouteDesignScoreParams(areaVsBeeline);
		
		RouteDesignScoringManager managerAreaVsBeeline = new RouteDesignScoringManager();
		managerAreaVsBeeline.init(pConfigAreaVsBeeline, scenario.getNetwork());
		
		PConfigGroup pConfigStopServedMultiple = new PConfigGroup();
		RouteDesignScoreParams params = new RouteDesignScoreParams();
		params.setRouteDesignScoreFunction(RouteDesignScoreFunctionName.stopServedMultipleTimesPenalty);
		params.setCostFactor(-20000);
		params.setStopListToEvaluate(StopListToEvaluate.transitRouteAllStops);
		params.setValueToStartScoring(1.04);
		pConfigStopServedMultiple.addRouteDesignScoreParams(params);
		
		RouteDesignScoringManager managerStopServedMultiple = new RouteDesignScoringManager();
		managerStopServedMultiple.init(pConfigStopServedMultiple, scenario.getNetwork());
		
		// Output containers
		Map<Id<TransitLine>, Map<Id<TransitRoute>, Map<RouteDesignScoreFunctionName, Double>>> line2route = new HashMap<>();
		
		Map<RouteDesignScoreFunctionName, Double> routeDesignFunction2TotalScore = new HashMap<>();
		routeDesignFunction2TotalScore.put(RouteDesignScoreFunctionName.stop2StopVsBeelinePenalty, 0.0);
		routeDesignFunction2TotalScore.put(RouteDesignScoreFunctionName.areaBtwLinksVsBeelinePenalty, 0.0);
		routeDesignFunction2TotalScore.put(RouteDesignScoreFunctionName.stopServedMultipleTimesPenalty, 0.0);
		
		Map<RouteDesignScoreFunctionName, Integer> routeDesignFunction2NumRoutesScoreNotNull = new HashMap<>();
		routeDesignFunction2NumRoutesScoreNotNull.put(RouteDesignScoreFunctionName.stop2StopVsBeelinePenalty, 0);
		routeDesignFunction2NumRoutesScoreNotNull.put(RouteDesignScoreFunctionName.areaBtwLinksVsBeelinePenalty, 0);
		routeDesignFunction2NumRoutesScoreNotNull.put(RouteDesignScoreFunctionName.stopServedMultipleTimesPenalty, 0);
		
		int numRoutesTotalScoreNotNull = 0;

		int transitRouteCounter = 0;
		
		// Score all TransitRoutes
		for (TransitLine line: scenario.getTransitSchedule().getTransitLines().values()) {
			for (TransitRoute route: line.getRoutes().values()) {
				transitRouteCounter++;
				
				PPlan pPlan1 = new PPlan(Id.create("PPlan1", PPlan.class), "creator1", Id.create("PPlanParent1", PPlan.class));
				
				// Find stopsToBeServed: AreaBtwLinksVsTerminiBeelinePenalty, AreaBtwStopsVsTerminiBeelinePenalty, Stop2StopVsTerminiBeelinePenalty need stopsToBeServed to determine the terminus stops
				ArrayList<TransitStopFacility> stopsToBeServed = new ArrayList<>();
				String stopsToBeServedDescriptionStartString = ", Stops: ";
				int indexOfStopsStart = route.getDescription().indexOf(stopsToBeServedDescriptionStartString);
				String stopsToBeServedDescriptionEndString = ", line budget ";
				int indexOfStopsEnd = route.getDescription().indexOf(stopsToBeServedDescriptionEndString);
				String stopsToBeServedString = route.getDescription().substring(indexOfStopsStart + stopsToBeServedDescriptionStartString.length(), indexOfStopsEnd);
				String[] stopsToBeServedArray = stopsToBeServedString.split(", ");
				for (String str: stopsToBeServedArray) {
					stopsToBeServed.add(scenario.getTransitSchedule().getFacilities().get(Id.create(str,TransitStopFacility.class)));
				}
				pPlan1.setStopsToBeServed(stopsToBeServed);
				
				// In PPlans there is only one TransitLine with exactly one TransitRoute per PPlan. So that TransitRoute is not the same as the TransitRoute in the output TransitScheduleScored
				TransitScheduleFactory factory = scenario.getTransitSchedule().getFactory();
				TransitLine lineOnlyThisTransitRoute = factory.createTransitLine(Id.create("line1", TransitLine.class));
				
				// route design scoring of PPlans probably has complete TransitRoutes with all stops and NetworkRoute, so no need to create a new TransitRoute
				lineOnlyThisTransitRoute.addRoute(route);
				pPlan1.setLine(lineOnlyThisTransitRoute);
				
				if (! line2route.containsKey(line.getId())) {
					Map<Id<TransitRoute>, Map<RouteDesignScoreFunctionName, Double>> route2Scores = new HashMap<>();
					line2route.put(line.getId(), route2Scores);
				}
				Map<RouteDesignScoreFunctionName, Double> routeDesignFunction2Score = new HashMap<>();
				
				// score
				double stop2StopVsBeelinePenalty = managerStop2Stop.scoreRouteDesign(pPlan1);
				double areaBtwLinksVsBeelinePenalty = managerAreaVsBeeline.scoreRouteDesign(pPlan1);
				double stopServedMultipleTimesPenalty = managerStopServedMultiple.scoreRouteDesign(pPlan1);
				
				routeDesignFunction2Score.put(RouteDesignScoreFunctionName.stop2StopVsBeelinePenalty, stop2StopVsBeelinePenalty);
				routeDesignFunction2Score.put(RouteDesignScoreFunctionName.areaBtwLinksVsBeelinePenalty, areaBtwLinksVsBeelinePenalty);
				routeDesignFunction2Score.put(RouteDesignScoreFunctionName.stopServedMultipleTimesPenalty, stopServedMultipleTimesPenalty);
				
				line2route.get(line.getId()).put(route.getId(), routeDesignFunction2Score);
				
				routeDesignFunction2TotalScore.put(RouteDesignScoreFunctionName.stop2StopVsBeelinePenalty, 
						routeDesignFunction2TotalScore.get(RouteDesignScoreFunctionName.stop2StopVsBeelinePenalty) + 
						(Double.isFinite(stop2StopVsBeelinePenalty) ? stop2StopVsBeelinePenalty : 0));
				routeDesignFunction2TotalScore.put(RouteDesignScoreFunctionName.areaBtwLinksVsBeelinePenalty, 
						routeDesignFunction2TotalScore.get(RouteDesignScoreFunctionName.areaBtwLinksVsBeelinePenalty) + 
						(Double.isFinite(stop2StopVsBeelinePenalty) ? areaBtwLinksVsBeelinePenalty : 0));
				routeDesignFunction2TotalScore.put(RouteDesignScoreFunctionName.stopServedMultipleTimesPenalty, 
						routeDesignFunction2TotalScore.get(RouteDesignScoreFunctionName.stopServedMultipleTimesPenalty) + 
						(Double.isFinite(stop2StopVsBeelinePenalty) ? stopServedMultipleTimesPenalty : 0));
				
				routeDesignFunction2NumRoutesScoreNotNull.put(RouteDesignScoreFunctionName.stop2StopVsBeelinePenalty, 
						routeDesignFunction2NumRoutesScoreNotNull.get(RouteDesignScoreFunctionName.stop2StopVsBeelinePenalty) + 
						((Double.isFinite(stop2StopVsBeelinePenalty) && stop2StopVsBeelinePenalty < 0.0) ? 1 : 0));
				routeDesignFunction2NumRoutesScoreNotNull.put(RouteDesignScoreFunctionName.areaBtwLinksVsBeelinePenalty, 
						routeDesignFunction2NumRoutesScoreNotNull.get(RouteDesignScoreFunctionName.areaBtwLinksVsBeelinePenalty) + 
						((Double.isFinite(stop2StopVsBeelinePenalty) && areaBtwLinksVsBeelinePenalty < 0.0) ? 1 : 0));
				routeDesignFunction2NumRoutesScoreNotNull.put(RouteDesignScoreFunctionName.stopServedMultipleTimesPenalty, 
						routeDesignFunction2NumRoutesScoreNotNull.get(RouteDesignScoreFunctionName.stopServedMultipleTimesPenalty) + 
						((Double.isFinite(stop2StopVsBeelinePenalty) && stopServedMultipleTimesPenalty < 0.0) ? 1 : 0));
				
				numRoutesTotalScoreNotNull += ((Double.isFinite(stop2StopVsBeelinePenalty) && stop2StopVsBeelinePenalty < 0.0) || 
						(Double.isFinite(stop2StopVsBeelinePenalty) && areaBtwLinksVsBeelinePenalty < 0.0) || 
								(Double.isFinite(stop2StopVsBeelinePenalty) && stopServedMultipleTimesPenalty < 0.0) ? 1 : 0);
				
				log.debug("line " + line.getId().toString() + " route " + route.getId().toString() + routeDesignFunction2Score);
			}
		}
		
		// write output to console/log
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append(transitRouteCounter);
		strBuilder.append(" TransitRoutes. Total scores: ");
		for (Entry<RouteDesignScoreFunctionName, Double> entry: routeDesignFunction2TotalScore.entrySet()) {
			strBuilder.append(entry.getKey().toString());
			strBuilder.append(" = ");
			strBuilder.append(entry.getValue());
			strBuilder.append("; ");
		}
		log.info(strBuilder.toString());
		
		strBuilder = new StringBuilder();
		strBuilder.append(transitRouteCounter);
		strBuilder.append(" TransitRoutes. Mean scores: ");
		for (Entry<RouteDesignScoreFunctionName, Double> entry: routeDesignFunction2TotalScore.entrySet()) {
			strBuilder.append(entry.getKey().toString());
			strBuilder.append(" = ");
			strBuilder.append(entry.getValue() / transitRouteCounter);
			strBuilder.append("; ");
		}
		log.info(strBuilder.toString());

		strBuilder = new StringBuilder();
		strBuilder.append(transitRouteCounter);
		strBuilder.append(" TransitRoutes. Number of TransitRoutes with score < 0 (penalty is applied): ");
		for (Entry<RouteDesignScoreFunctionName, Integer> entry: routeDesignFunction2NumRoutesScoreNotNull.entrySet()) {
			strBuilder.append(entry.getKey().toString());
			strBuilder.append(" = ");
			strBuilder.append(entry.getValue());
			strBuilder.append("; ");
		}
		log.info(strBuilder.toString());
		
		log.info("numRoutesTotalScoreNotNull " + numRoutesTotalScoreNotNull);
	}

}
