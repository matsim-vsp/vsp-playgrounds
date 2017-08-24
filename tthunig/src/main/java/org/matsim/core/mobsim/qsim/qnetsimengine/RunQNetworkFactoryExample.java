/**
 * 
 */
package org.matsim.core.mobsim.qsim.qnetsimengine;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine.NetsimInternalInterface;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;


/**
 * @author kainagel
 *
 */
class RunQNetworkFactoryExample {

	public class MyQNetworkFactory extends QNetworkFactory {
		private EventsManager events ;
		private Scenario scenario ;
		// (vis needs network and may need population attributes and config; in consequence, makes sense to have scenario here. kai, apr'16)
		private NetsimEngineContext context;
		private NetsimInternalInterface netsimEngine ;
		@Inject
		MyQNetworkFactory( EventsManager events, Scenario scenario ) {
			this.events = events;
			this.scenario = scenario;
		}

		@Override
		void initializeFactory(AgentCounter agentCounter, MobsimTimer mobsimTimer,
				NetsimInternalInterface netsimEngine1) {
			this.netsimEngine = netsimEngine1;
			double effectiveCellSize = scenario.getNetwork().getEffectiveCellSize() ;

			SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
			linkWidthCalculator.setLinkWidthForVis( scenario.getConfig().qsim().getLinkWidthForVis() );
			linkWidthCalculator.setLaneWidth( scenario.getNetwork().getEffectiveLaneWidth() );

			AbstractAgentSnapshotInfoBuilder agentSnapshotInfoBuilder = QNetsimEngine.createAgentSnapshotInfoBuilder( scenario, linkWidthCalculator );

			context = new NetsimEngineContext( events, effectiveCellSize, agentCounter, agentSnapshotInfoBuilder, scenario.getConfig().qsim(), 
					mobsimTimer, linkWidthCalculator );
			
		}

		@Override
		QNodeI createNetsimNode(Node node) {
			QNodeImpl.Builder builder = new QNodeImpl.Builder( netsimEngine, context ) ;

			TurnAcceptanceLogic turnAcceptanceLogic = new TurnAcceptanceLogic(){
				@Override public boolean isAcceptingTurn(Link currentLink, Id<Link> nextLinkId, QLinkI nextQLink,
						QVehicle veh) {
					
					// check if outgoing link is connected to
					// intersection (see DefaultTurnAcceptanceLogic; maybe even delegate to that)
					
					// check if there is a conflicting link which has vehicles in the buffer
					
					return true ;
				}
			} ;
			
			
			builder.setTurnAcceptanceLogic( turnAcceptanceLogic ) ;

			return builder.build( node ) ;
		}

		@Override
		QLinkI createNetsimLink(Link link, QNodeI toQueueNode) {
			QLinkImpl.Builder linkBuilder = new QLinkImpl.Builder(context, netsimEngine) ;
			return linkBuilder.build(link, toQueueNode) ;
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Config config = ConfigUtils.loadConfig(null) ;
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		Controler controler = new Controler( config ) ;
		
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install() { 
				bind( QNetworkFactory.class ).to( MyQNetworkFactory.class ) ;
			}
			
		} ) ;
		
		controler.run() ;
	}

}
