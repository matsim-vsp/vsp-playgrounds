package playground.kai.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriter;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

class KNTripsBetween7and9{

    public static void main ( String [] args ) {
        String filename = "/Users/kainagel/public-svn/matsim/scenarios/countries/de/berlin/2018-09-04_output-berlin-v5.2-10pct/2018-09-04_output-berlin-v5.2-10pct/berlin-v5" +
                ".2-10pct.output_events.xml.gz" ;

        EventsManager events = EventsUtils.createEventsManager() ;

        Map<Id<Person>,Id<Link>> consideredPersons = new HashMap<>() ;
        Map<Id<Vehicle>,Id<Person>> consideredVehicles = new HashMap<>() ;

        EventWriterXML writer = new EventWriterXML( "/Users/kainagel/events.xml.gz" ) ;
        events.addHandler( new BasicEventHandler(){
            @Override
            public void handleEvent( Event event ){
                if ( event.getTime() < 7.*3600. ) {
                    return ;
                }
                if ( event.getTime() > 10.*3600 ) {
                    writer.closeFile();
                    System.out.println("done") ;
                    System.exit(-1) ;
                }

                // register persons and vehicles that depart until a given time
                if ( event.getTime() < 8.*3600. ){
                    if( event instanceof ActivityEndEvent ){
                        consideredPersons.put( ((ActivityEndEvent) event).getPersonId(), ((ActivityEndEvent) event).getLinkId() );
                    } else if( event instanceof PersonEntersVehicleEvent ){
                        consideredVehicles.put( ((PersonEntersVehicleEvent) event).getVehicleId(), ((PersonEntersVehicleEvent) event).getPersonId() );
                    }
                }

                // unregister persons and vehicles that have arrived.  So they will be ignored after the given time
                if( event instanceof ActivityStartEvent ){
                    consideredPersons.remove( ((ActivityStartEvent) event).getPersonId());
                } else if( event instanceof PersonLeavesVehicleEvent ){
                    consideredVehicles.remove( ((PersonLeavesVehicleEvent) event).getVehicleId());
                }

                // write registered persons and vehicles
                if ( event instanceof HasPersonId ) {
                    if ( consideredPersons.containsKey(((HasPersonId) event).getPersonId() ) ){
                        writer.handleEvent( event );
                    }
                } else if ( event instanceof LinkLeaveEvent ) {
                    if ( consideredVehicles.containsKey( ((LinkLeaveEvent) event).getVehicleId() ) ) {
                        writer.handleEvent( event );
                    }
                } else if ( event instanceof LinkEnterEvent ) {
                    if ( consideredVehicles.containsKey( ((LinkEnterEvent) event).getVehicleId() ) ) {
                        writer.handleEvent( event );
                    }
                }
            }
        } );



        EventsUtils.readEvents( events, filename );

        writer.closeFile();

        System.out.println("done") ;
    }

}
