package playground.gthunig.plateauRouter.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import java.util.HashMap;
import java.util.Map;

class RunAnalysisTool {

		private static EventsManager events;
		private static MatsimEventsReader reader;

		private static final String POP1_NORMAL = "C:/Users/Tille/TU/MATSim/matsim/output/ha2/pop1_50it_normalRouter/ITERS/it.X/X.events.xml";
		private static final String POP1_PLATEAU = "C:/Users/Tille/TU/MATSim/matsim/output/ha2/pop1_50it_10longest/ITERS/it.X/X.events.xml";
		private static final String POP2_NORMAL = "C:/Users/Tille/TU/MATSim/matsim/output/ha2/pop2_50it_normalRouter/ITERS/it.X/X.events.xml";
		private static final String POP2_PLATEAU = "C:/Users/Tille/TU/MATSim/matsim/output/ha2/pop2_50it_20longestPlateaus/ITERS/it.X/X.events.xml";
		
	
	public static void main(String[] args) {
						
				Map<Id<Link>,String> pop1routes = new HashMap<>();
				Map<Id<Link>,String> pop2routes = new HashMap<>();
				
				pop1routes.put(Id.createLinkId(1660),"pop1 östlicher ring"); //pop1 östlicher ring
				pop1routes.put(Id.createLinkId(11842),"pop1 östliches stadtzentrum"); //pop1 östliches stadtzentrum
				pop1routes.put(Id.createLinkId(8281),"pop1 westliches zentrum = DIJKSTRA"); //pop1 westliches zentrum = DIJSTRA-route
				pop1routes.put(Id.createLinkId(12711),"pop1 westlicher ring"); //pop1 westlicher ring
				
				pop2routes.put(Id.createLinkId(5601),"pop2 östlicher ring = DIJKSTRA"); 
				pop2routes.put(Id.createLinkId(17757),"pop2 B109 westlicher Ring, östliche variante"); 
				pop2routes.put(Id.createLinkId(15395),"pop2 B96 westlicher Ring, westliche variante"); 
				pop2routes.put(Id.createLinkId(9798),"pop2 Stadtzentrum"); 
				
				//prepare the StringBuilders
				StringBuilder pop1Normal = new StringBuilder(POP1_NORMAL);
				StringBuilder pop1Plateau= new StringBuilder(POP1_PLATEAU);
				StringBuilder pop2Normal= new StringBuilder(POP2_NORMAL);
				StringBuilder pop2Plateau= new StringBuilder(POP2_PLATEAU);
				
				StringBuilder[] sbList = new StringBuilder[4];
				sbList[0] = (pop1Normal);
				sbList[1] = (pop1Plateau);
				sbList[2] = (pop2Normal);
				sbList[3] = (pop2Plateau);
				
			for(int i= 0; i< 4; i++){
				StringBuilder builder = sbList[i];
				events = EventsUtils.createEventsManager();
				ha2EventHandler handler;
				if (i > 1){
					System.out.println("--------------POP2-------------");
					handler = new ha2EventHandler(pop2routes);
				}
				else{
					System.out.println("------------POP1-----------------");
					handler = new ha2EventHandler(pop1routes);
				}
				System.out.println(builder.toString());
				events.addHandler(handler);
				reader = new MatsimEventsReader(events);
				
				
				for(int j = 0; j<51; j+=10){
					handler.reset(j);
					String str = "" + j + "/" + j;
					if(j<=10){
						builder.replace(builder.length()-14,builder.length()-11,str);
					}
					else{
						builder.replace(builder.length()-16,builder.length()-11,str);
					}
//					String str = "" + i +"/" + i + ".events.xml";
//					pop1Normal.r
//					pop1Normal.append(str);
					reader.readFile(builder.toString());
					handler.notifyIterationEnds();
				}
				handler.print();
			}
	}
}
