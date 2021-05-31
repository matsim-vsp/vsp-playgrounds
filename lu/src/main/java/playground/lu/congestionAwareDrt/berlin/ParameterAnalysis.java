package playground.lu.congestionAwareDrt.berlin;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ParameterAnalysis {
    public static void main(String[] args) throws IOException {
        double[] discountFactors = {0.65, 0.7, 0.75, 0.8, 0.85, 0.9, 0.95, 1.0};
        double[] penaltyFactors = {1.0, 1.5, 2.0, 2.5, 3.0};
        double[] overFlowFactors = {1.0, 1.5, 2.0, 2.5, 3.0};

        String outputDirectory = "/Users/luchengqi/Documents/MATSimScenarios/Mielec/output/parameterTuning";
        double minValue = 1000000;
        double optimalDiscountFactor = 1.0;
        double optimalPenaltyFactor = 1.0;
        double optimalOverflowFactor = 1.0;

        for (double discountFactor: discountFactors) {
            for (double penaltyFactor:penaltyFactors) {
                for (double overflowFactor: overFlowFactors) {
                    String fileName = outputDirectory + "/d-" + discountFactor + "-p-" + penaltyFactor + "-o-" +
                            overflowFactor + "/drt_customer_stats_drt.csv";
                    BufferedReader csvReader = new BufferedReader(new FileReader(fileName));
                    csvReader.readLine(); // skip the first line
                    String[] data = csvReader.readLine().split(";");
                    double meanWaitTime = Double.parseDouble(data[3]);
                    double meanInVehicleTime = Double.parseDouble(data[10]);
                    csvReader.close();

                    if (meanInVehicleTime<minValue){
                        minValue = meanInVehicleTime;
                        optimalDiscountFactor = discountFactor;
                        optimalPenaltyFactor = penaltyFactor;
                        optimalOverflowFactor = overflowFactor;
                    }
                }
            }
        }

        System.out.println("Analysis is complete!");
        System.out.println("The optimal parameter set is as follow:");
        System.out.println("Discount Factor: " + optimalDiscountFactor);
        System.out.println("Penalty Factor: " + optimalPenaltyFactor);
        System.out.println("Overflow Factor: " + optimalOverflowFactor);


    }
}
