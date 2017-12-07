package playground.sbraun.actDurationAnalysis;



import java.io.IOException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.testcases.MatsimTestUtils;



/**
* @author sbraun
*/

public class ActDurationAnalysisTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void test1() throws IOException {
		EventsManager events = EventsUtils.createEventsManager();

		ActDurationHandler handler1 = new ActDurationHandler();
		events.addHandler(handler1);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(utils.getInputDirectory() + "events1.xml");
				
		Assert.assertEquals("Total Duration should be", 9., handler1.getTotalDuration(), MatsimTestUtils.EPSILON);
	}
	
	@Test
	public void test2() throws IOException {
		EventsManager events = EventsUtils.createEventsManager();

		ActDurationHandler handler1 = new ActDurationHandler();
		events.addHandler(handler1);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(utils.getInputDirectory() + "events2.xml");
		
		Assert.assertEquals("Total Duration should be", 0, handler1.getTotalDuration(), MatsimTestUtils.EPSILON);
	}	
}

