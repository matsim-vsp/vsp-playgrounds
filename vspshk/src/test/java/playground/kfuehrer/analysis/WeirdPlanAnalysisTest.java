package playground.kfuehrer.analysis;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author
 * 
 */
public class WeirdPlanAnalysisTest {
		
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Ignore
	@Test
	public void test1() throws IOException {
		
		WeirdPlanAnalysis analysis = new WeirdPlanAnalysis();
				
		analysis.analyze(utils.getInputDirectory() + "plans1.xml");
		Assert.assertEquals("Wrong number of weird plans. ", 0, analysis.getWeirdPlans(), MatsimTestUtils.EPSILON);
		
		analysis.analyze(utils.getInputDirectory() + "plans2.xml");
		Assert.assertEquals("Wrong number of weird plans. ", 1, analysis.getWeirdPlans(), MatsimTestUtils.EPSILON);
		
		analysis.analyze(utils.getInputDirectory() + "plans3.xml");
		Assert.assertEquals("Wrong number of weird plans. ", 1, analysis.getWeirdPlans(), MatsimTestUtils.EPSILON);
		
	}
	
}
