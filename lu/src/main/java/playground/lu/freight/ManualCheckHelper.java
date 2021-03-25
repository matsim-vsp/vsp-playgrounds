package playground.lu.freight;

import java.io.BufferedReader;
import java.io.FileReader;

public class ManualCheckHelper {
	private static final String ORIGINAL_FILE = "C:\\Users\\cluac\\MATSimScenarios\\Dusseldorf\\freight\\lookup-table-format-B.csv";

	public static void main(String[] args) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(ORIGINAL_FILE));
			reader.readLine(); // Skip first line
			String line = reader.readLine();
			while (line != null) {
				String[] splitedLine = line.split(";");
				String verKehrszellen = splitedLine[0];
				String verkehrszellenname = splitedLine[1];
				String nutsName = splitedLine[5];
				String verkehrszellennameShort = verkehrszellenname.split(",")[0].split(" ")[0];
				String nutsNameShort = nutsName.split(",")[0].split(" ")[0];

				if (!verkehrszellennameShort.equals(nutsNameShort)) {
					System.err.println(
							"Check region: Id=" + verKehrszellen + ", " + verkehrszellenname + " ?= " + nutsName);
				}
				line = reader.readLine();
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
