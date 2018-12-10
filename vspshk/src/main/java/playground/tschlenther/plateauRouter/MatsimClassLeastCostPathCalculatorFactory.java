package playground.tschlenther.plateauRouter;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * @author jbischoff
 *
 */
public class MatsimClassLeastCostPathCalculatorFactory implements LeastCostPathCalculatorFactory {

	@Override
	public LeastCostPathCalculator createPathCalculator(Network network,
			TravelDisutility travelCosts, TravelTime travelTimes) {
		return new PlateauRouter(network, travelCosts, travelTimes);
	}

}
