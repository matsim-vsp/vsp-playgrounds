package playground.lu.run;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ExtractKeyDataFromOutputFolders {

	private static final String[] REBLANCE_STRATEGIES = { "Feedforward", "PlusOne", "Adaptive", "MinCostFlow",
			"NoRebalance" };
	private static final String[] FLEET_SIZES = { "300", "350", "400", "450", "500", "550", "600", "650", "700" };
	private static final String[] CRITERIA = { "meanWaitTime", "medianWaitTime", "waitTimeBelow600", "waitTimeBelow900",
			"totalDistance", "emptyDistance", "95Percentile" };

	private static final String ROOT_DIRECTORY = "D:\\TU_Berlin\\Projects\\RebalancingStudy_result_new\\";
	private static final String SUMMARY_RESULT_DIRECTORY = "D:\\TU_Berlin\\Projects\\RebalancingStudy_result_new\\SummarizedResults\\";

	public static void main(String[] args) throws IOException {
		String[][] meanWaitTime = new String[5][9];
		String[][] medianWaitTime = new String[5][9];
		String[][] waitTimeBelow600 = new String[5][9];
		String[][] waitTimeBelow900 = new String[5][9];
		String[][] totalDistance = new String[5][9];
		String[][] emptyDistance = new String[5][9];
		String[][] percentile95 = new String[5][9];
		
		String[][][] statistics = new String[7][5][9];
		statistics[0] = meanWaitTime;
		statistics[1] = medianWaitTime;
		statistics[2] = waitTimeBelow600;
		statistics[3] = waitTimeBelow900;
		statistics[4] = totalDistance;
		statistics[5] = emptyDistance;
		statistics[6] = percentile95;

		System.out.println("Reading data...");
		for (int i = 0; i < REBLANCE_STRATEGIES.length; i++) {
			for (int j = 0; j < FLEET_SIZES.length; j++) {
				String waitTimeFileName = REBLANCE_STRATEGIES[i] + "-" + FLEET_SIZES[j]
						+ "\\drt_customer_stats_drt.csv";
				String distanceFileName = REBLANCE_STRATEGIES[i] + "-" + FLEET_SIZES[j] + "\\drt_vehicle_stats_drt.csv";

				String pathToWaitStatisticsCsv = ROOT_DIRECTORY + waitTimeFileName;
				BufferedReader csvReader = new BufferedReader(new FileReader(pathToWaitStatisticsCsv));
				csvReader.readLine(); // Read first line (title)
				csvReader.readLine(); // Read iteration 0
				String csvRow = csvReader.readLine();
				String[] waitTimeStatistics = csvRow.split(";|,");
				csvReader.close();

				String pathToDistanceCsv = ROOT_DIRECTORY + distanceFileName;
				BufferedReader csvReader2 = new BufferedReader(new FileReader(pathToDistanceCsv));
				csvReader2.readLine(); // Read first line (title)
				csvReader2.readLine(); // Read iteration 0
				String distanceRow = csvReader2.readLine();
				String[] distanceStatistics = distanceRow.split(";|,");
				csvReader2.close();

				meanWaitTime[i][j] = waitTimeStatistics[3];
				medianWaitTime[i][j] = waitTimeStatistics[7];
				waitTimeBelow600[i][j] = waitTimeStatistics[8];
				waitTimeBelow900[i][j] = waitTimeStatistics[9];
				totalDistance[i][j] = distanceStatistics[3];
				emptyDistance[i][j] = distanceStatistics[4];
				percentile95[i][j] = waitTimeStatistics[5];
			}
		}
		System.out.println("Data successfully read and stored");

		System.out.println("Writing mean wait time summary");

		for (int k = 0; k < CRITERIA.length; k++) {
			FileWriter csvWriter = new FileWriter(SUMMARY_RESULT_DIRECTORY + CRITERIA[k] +".csv");
			csvWriter.append("Rebalancing Strategy");
			csvWriter.append(",");
			csvWriter.append("300");
			csvWriter.append(",");
			csvWriter.append("350");
			csvWriter.append(",");
			csvWriter.append("400");
			csvWriter.append(",");
			csvWriter.append("450");
			csvWriter.append(",");
			csvWriter.append("500");
			csvWriter.append(",");
			csvWriter.append("550");
			csvWriter.append(",");
			csvWriter.append("600");
			csvWriter.append(",");
			csvWriter.append("650");
			csvWriter.append(",");
			csvWriter.append("700");
			csvWriter.append("\n");

			for (int i = 0; i < statistics[k].length; i++) {
				csvWriter.append(REBLANCE_STRATEGIES[i]);
				csvWriter.append(",");
				csvWriter.append(String.join(",", statistics[k][i]));
				csvWriter.append("\n");
			}

			csvWriter.flush();
			csvWriter.close();
		}
	}

}
