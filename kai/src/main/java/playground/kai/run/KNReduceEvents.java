package playground.kai.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.roadTypeMapping.VisumHbefaRoadTypeMapping;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class KNReduceEvents{

	private static final String filename = "/Users/kainagel/git/ABMTRANS2019Code/output/ABMTRANS2019/full/ITERS/it.0/0.events.xml.gz" ;

	public static void main ( String [] args ) {

		EventsManager eventsIn = EventsUtils.createEventsManager();
		EventsManager eventsOut = EventsUtils.createEventsManager() ;

		eventsIn.addHandler( new BasicEventHandler(){
			private Map<Id<Vehicle>,Id<Person>> personFromVehicleMap = new HashMap<>() ;
			Set<Id<Person>> alreadySeen = new HashSet<>(  ) ;
			Set<Id<Person>> toWrite = new HashSet<>(  ) ;
			long ii = 0 ;
			@Override public void handleEvent( Event event ){
				if ( event instanceof HasPersonId ) {
					Id<Person> personId = ((HasPersonId) event).getPersonId();
					if ( toWrite.contains( personId ) ) {
						eventsOut.processEvent( event );
					} else if ( alreadySeen.contains( personId  ) ) {
						// do nothing
					} else {
						alreadySeen.add( personId ) ;
						if ( ii % 10 == 0 ) {
							toWrite.add( personId ) ;
							eventsOut.processEvent( event );
						}
						ii++ ;
					}
					if ( event instanceof PersonEntersVehicleEvent ) {
						personFromVehicleMap.put( ((PersonEntersVehicleEvent) event).getVehicleId(), ((PersonEntersVehicleEvent) event).getPersonId()) ;
					}
				}
				if ( event instanceof LinkEnterEvent ){
					Id<Person> personId = personFromVehicleMap.get( ((LinkEnterEvent) event).getVehicleId() );
					;
					if( toWrite.contains( personId ) ){
						eventsOut.processEvent( event );
					}
				} else if ( event instanceof LinkLeaveEvent  ) {
					Id<Person> personId = personFromVehicleMap.get( ((LinkLeaveEvent) event).getVehicleId() );;
					if ( toWrite.contains( personId ) ) {
						eventsOut.processEvent( event );
					}
				}
			}
		} );

		final EventWriterXML writer = new EventWriterXML( "reduced_events.xml.gz" );
		eventsOut.addHandler( writer ) ;

		EventsUtils.readEvents( eventsIn, filename );

		writer.closeFile();

	}

}
