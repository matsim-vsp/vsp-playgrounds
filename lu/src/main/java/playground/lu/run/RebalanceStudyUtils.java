package playground.lu.run;

import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.Feedforward.FeedforwardRebalancingStrategyParams;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams.RebalancingTargetCalculatorType;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams.ZonalDemandEstimatorType;
import org.matsim.contrib.drt.optimizer.rebalancing.plusOne.PlusOneRebalancingStrategyParams;

public class RebalanceStudyUtils {
	public static void prepareAdaptiveRealTimeStrategy(RebalancingParams rebalancingParams) {
		MinCostFlowRebalancingStrategyParams minCostFlowRebalancingStrategyParams = new MinCostFlowRebalancingStrategyParams();
		minCostFlowRebalancingStrategyParams.setRebalancingTargetCalculatorType(
				RebalancingTargetCalculatorType.EqualRebalancableVehicleDistribution);
		minCostFlowRebalancingStrategyParams
				.setZonalDemandEstimatorType(ZonalDemandEstimatorType.PreviousIterationDemand);
		minCostFlowRebalancingStrategyParams.setTargetAlpha(1);
		minCostFlowRebalancingStrategyParams.setTargetBeta(0);
		minCostFlowRebalancingStrategyParams.setDemandEstimationPeriod(108000);
		rebalancingParams.addParameterSet(minCostFlowRebalancingStrategyParams);
	}

	public static void preparePlusOneStrategy(RebalancingParams rebalancingParams) {
		rebalancingParams.addParameterSet(new PlusOneRebalancingStrategyParams());
	}

	public static void prepareFeedforwardStrategy(RebalancingParams rebalancingParams) {
		FeedforwardRebalancingStrategyParams feedforwardRebalancingStrategyParams = new FeedforwardRebalancingStrategyParams();
		feedforwardRebalancingStrategyParams.setFeedbackSwitch(true);
		feedforwardRebalancingStrategyParams.setFeedforwardSignalLead(300);
		feedforwardRebalancingStrategyParams.setMinNumVehiclesPerZone(1);
		rebalancingParams.addParameterSet(feedforwardRebalancingStrategyParams);
	}
	
	public static void prepareMinCostFlowStrategy(RebalancingParams rebalancingParams) {
		MinCostFlowRebalancingStrategyParams minCostFlowRebalancingStrategyParams = new MinCostFlowRebalancingStrategyParams();
		minCostFlowRebalancingStrategyParams.setRebalancingTargetCalculatorType(
				RebalancingTargetCalculatorType.EstimatedDemand);
		minCostFlowRebalancingStrategyParams
				.setZonalDemandEstimatorType(ZonalDemandEstimatorType.PreviousIterationDemand);
		minCostFlowRebalancingStrategyParams.setTargetAlpha(0.8);
		minCostFlowRebalancingStrategyParams.setTargetBeta(0.3);
		minCostFlowRebalancingStrategyParams.setDemandEstimationPeriod(1800);
		rebalancingParams.addParameterSet(minCostFlowRebalancingStrategyParams);
	}
	
	// As we may want to see the waiting time in different zones, we need to use a dummy rebalancing strategy
	// This is due to the current setup in the zonal system creation. No rebalnce --> no DRT zonal system
	public static void prepareNoRebalance(RebalancingParams rebalancingParams) {
		FeedforwardRebalancingStrategyParams feedforwardRebalancingStrategyParams = new FeedforwardRebalancingStrategyParams();
		feedforwardRebalancingStrategyParams.setFeedbackSwitch(false);
		feedforwardRebalancingStrategyParams.setFeedforwardSignalStrength(0);
		rebalancingParams.addParameterSet(feedforwardRebalancingStrategyParams);
	}
}
