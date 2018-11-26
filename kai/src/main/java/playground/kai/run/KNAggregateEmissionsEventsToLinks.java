package playground.kai.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.Map;

class KNAggregateEmissionsEventsToLinks {
	
	enum Run { bau, BTb } ;
	
	
	public static void main ( String [] args ) {
		
		String netFilename = "/Users/kainagel/runs-svn/patnaIndia/BT-b/output_network.xml.gz" ;
		
		final Network network = ScenarioUtils.createScenario( ConfigUtils.createConfig() ).getNetwork();;
		NetworkUtils.readNetwork( network, netFilename );
		
		Map<Id<Link>,Double> map = new HashMap<>() ;
		Map<Id<Link>,Double> map2 = new HashMap<>() ;
		
		processEvents( map, Run.bau );
		processEvents( map2, Run.BTb );
		
		try (
				final BufferedWriter writer = IOUtils.getBufferedWriter( "/Users/kainagel/NO2OnLinks.txt" );
		) {
			writer.write( "link\tNO2PerMeterBau\tNO2PerMeterBTb\tNO2PerMeterDiff" ) ;
			writer.newLine();
			for ( Link link : network.getLinks().values() ) {
				Double val = map.get( link.getId() ) ;
				if ( val==null ) {
					val = 0. ;
				}
				
				Double val2 = map2.get( link.getId() ) ;
				if ( val2==null ) {
					val2 = 0.;
				}

				writer.write( link.getId().toString() + "\t" + val/ link.getLength() + "\t" + val2/link.getLength() + "\t" +
								  (val2-val)/link.getLength() ) ;
				writer.newLine();
			}
			
		} catch ( Exception ee ) {
			throw new RuntimeException(ee) ;
		}
		
	}
	
	private static void processEvents( final Map<Id<Link>, Double> map, Run pCase ) {
		String eventsFilename = null;
		switch ( pCase ) {
			case bau:
				eventsFilename = "/Users/kainagel/runs-svn/patnaIndia/bau/output_combinedEvents.xml.gz" ;
				break;
			case BTb:
				eventsFilename = "/Users/kainagel/runs-svn/patnaIndia/BT-b/output_combinedEvents.xml.gz" ;
				break;
		}
//		eventsFilename = "/Users/kainagel/runs-svn/patnaIndia/bau/output_cEvents.xml" ;
		
		
		final EventsManager events = new EventsManagerImpl() ;
		final MatsimEventsReader reader = new MatsimEventsReader( events );
		
		
		events.addHandler( new BasicEventHandler() {
			@Override public void handleEvent( final Event event ) {
				if ( event.getEventType().equals( ColdEmissionEvent.EVENT_TYPE ) ) {
					final Id<Link> linkId = Id.createLinkId( event.getAttributes().get("linkId") );
					double no2 = Double.parseDouble( event.getAttributes().get("NO2") );
					map.merge( linkId, no2, ( a, b ) -> a + b );
				}
				if ( event.getEventType().equals( WarmEmissionEvent.EVENT_TYPE ) ) {
					final Id<Link> linkId = Id.createLinkId( event.getAttributes().get("linkId") );
					double no2 = Double.parseDouble( event.getAttributes().get("NO2") );
					map.merge( linkId, no2, ( a, b ) -> a + b );
				}
			}
			
			@Override public void reset( final int iteration ) {
			}
		} );
		
		reader.readFile( eventsFilename );
	}
	
}
