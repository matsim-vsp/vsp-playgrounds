package playground.kai.run;

import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.*;

import java.util.Iterator;

class KNCounts2Counts{

	public static void main( String[] args ){

		Counts<Link> counts = new Counts<>();

		new MatsimCountsReader( counts ).readFile( "/Users/kainagel/mnt/mathe/ils3/laudan/nemo-mercartor/nemo_cadytsV2/cadytsV2_012/output/cadytsV2_012.output_counts.xml.gz" ) ;

		for ( Iterator<Count<Link>> it = counts.getCounts().values().iterator() ; it.hasNext() ; ) {
			Count<Link> station = it.next();
			boolean small = false ;
//			boolean large = false ;
//			for( Volume volume : station.getVolumes().values() ){
//				if ( volume.getValue() > 200 ) {
//					large = true ;
//				}
//			}
			for ( int hour = 8 ; hour <= 18 ; hour ++ ) {
				if ( station.getVolume( hour ).getValue() < 300 ) {
					small = true ;
				}
			}
			if ( small ) {
				it.remove();
			}
		}

		new CountsWriter( counts ).write( "/Users/kainagel/counts-reduced.xml" );

	}

}
