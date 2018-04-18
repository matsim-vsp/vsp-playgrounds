package saleem.ptoptimisation.optimisationintegration;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.utilities.math.Vector;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.opdyts.MATSimState;
import org.matsim.contrib.opdyts.MATSimStateFactory;
import org.matsim.core.controler.Controler;

/**
 * A factory class to create PTMatSimState objects.
 * 
 * @author Mohammad Saleem
 *
 */
public class PTMatsimStateFactoryImpl<U extends DecisionVariable> implements
		MATSimStateFactory<U> {
	private Scenario scenario;
	final double occupancyScale;
	public PTMatsimStateFactoryImpl(Scenario scenario, final double occupancyScale) {
		 this.scenario = scenario;
		 this.occupancyScale=occupancyScale;
	}

	@Override
	public MATSimState newState(final Population population,
								final Vector stateVector, final U decisionVariable) {
		return new PTMatsimState(population, stateVector, scenario, (PTSchedule)decisionVariable, occupancyScale);
		
	}

	@Override
	public void registerControler(Controler controler) {
		// TODO Auto-generated method stub
		
	}

}
