package opdytsintegration.example.roadpricing;

import floetteroed.utilities.TimeDiscretization;
import floetteroed.utilities.math.Vector;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.opdyts.MATSimState;
import org.matsim.contrib.opdyts.MATSimStateFactory;
import org.matsim.core.controler.Controler;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RoadpricingStateFactory implements MATSimStateFactory<TollLevels> {

	private final TimeDiscretization timeDiscretization;

	private final double occupancyScale;

	private final double tollScale;

	public RoadpricingStateFactory(final TimeDiscretization timeDiscretization,
			final double occupancyScale, final double tollScale) {
		this.timeDiscretization = timeDiscretization;
		this.occupancyScale = occupancyScale;
		this.tollScale = tollScale;
	}

	public MATSimState newState(final Population population,
								final Vector stateVector, final TollLevels decisionVariable) {
		return new RoadpricingState(population, stateVector, decisionVariable,
				this.timeDiscretization, this.occupancyScale, this.tollScale);
	}

	@Override
	public void registerControler(Controler controler) {
		// TODO Auto-generated method stub
		
	}

}
