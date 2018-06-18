package playground.santiago.run;

import java.io.BufferedWriter;

import org.matsim.core.utils.io.IOUtils;

public class PathTest {

	public static void main(String[] args) {
		String pathToTest = "../../runs-svn/santiago/";
		String nameTestFile = "testingPaths.txt";
		String outputFile = pathToTest + nameTestFile;
		
		try (BufferedWriter writer = IOUtils.getBufferedWriter(outputFile)) {
			writer.write("This is a test");
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason "+e );
		}

	}

}
