package link2link;

import org.matsim.api.core.v01.Scenario;

import playground.vsp.createTestNetworks.ForkNetworkCreator;

public class Link2LinkTestNetworkCreator extends ForkNetworkCreator {

	public Link2LinkTestNetworkCreator(Scenario scenario, boolean lanes,
			boolean signals) {
		super(scenario, lanes, signals);
	}

	@Override
	public void createNetwork(){
		super.createNetwork();
		//use signals
		if(this.UseSignals){
			Link2LinkTestSignalsCreator signalsCreator = new Link2LinkTestSignalsCreator(this.scenario, this.UseLanes);
			signalsCreator.createSignals();
		}
	}
}