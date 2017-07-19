package matsimintegration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.contrib.opdyts.utils.TimeDiscretization;
import org.matsim.core.config.Config;

/**
 * Throws Gunnar's TimeDiscretization into the MATSim/Guice machinery.
 * 
 * @author Gunnar Flötteröd
 *
 */
@Singleton
public class TimeDiscretizationInjection {

	private final TimeDiscretization timeDiscr;

	@Inject
	TimeDiscretizationInjection(final Config config) {
		this.timeDiscr = TimeDiscretizationFactory.newInstance(config);
	}
	
	public TimeDiscretization getInstance() {
		return this.timeDiscr;
	}
}
