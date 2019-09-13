package playground.ikaddoura.moneyTravelDisutility.data;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public final class AgentFilterNullImpl implements AgentFilter {
	@Override public String getAgentTypeFromId( Id<Person> id ){
		return null ;
	}
}
