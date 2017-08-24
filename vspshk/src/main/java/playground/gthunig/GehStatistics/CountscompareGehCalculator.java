package playground.gthunig.GehStatistics;

import org.matsim.contrib.util.CSVReaders;
import playground.gthunig.utils.CSVWriter;

import java.util.List;

/**
 * @author gthunig on 22.08.2017.
 */
public class CountscompareGehCalculator {

    private static final String GEH_Header = "GEH";

    private String dataSource;
    private char sourceSeperator;
    private int rowOfRealCountValue;
    private int rowOfSimulatedCountValue;

    private List<String[]> counts;
    private CSVWriter writer;

    private double[] aggregatedGeh = new double[24];

    public static void main(String[] args) {
        String dataSource = "C:\\Users\\gthunig\\Desktop\\GEHTest\\be_251.500.countscompare.txt";
        char seperator = '\t';
        int rowOfRealCountValue = 3;
        int rowOfSimulatedCountValue = 2;

        String outputFile = "C:\\Users\\gthunig\\Desktop\\GEHTest\\be_251.500.countscompare_withGEH.txt";

        CountscompareGehCalculator calculator = new CountscompareGehCalculator(
                dataSource, seperator, rowOfRealCountValue, rowOfSimulatedCountValue
        );
        calculator.calculateAndWriteTo(outputFile, String.valueOf(seperator));
    }

    public CountscompareGehCalculator(String dataSource, char seperator, int rowOfRealCountValue, int rowOfSimulatedCountValue) {
        this.dataSource = dataSource;
        this.sourceSeperator = seperator;
        this.rowOfRealCountValue = rowOfRealCountValue;
        this. rowOfSimulatedCountValue = rowOfSimulatedCountValue;
    }

    public void calculateAndWriteTo(String outputFile, String seperator) {
        counts = CSVReaders.readFile(dataSource, sourceSeperator);
        writer = new CSVWriter(outputFile, seperator);

        writeNewHeader(counts.get(0));
        for (int i = 1; i < counts.size(); i++) {
            writeCountWithGeh(counts.get(i));
        }
        for (int i = 0; i < aggregatedGeh.length; i++) {
            aggregatedGeh[i] /= (counts.size()-1)/24;
            System.out.println(i+1 + ": " + aggregatedGeh[i]);
        }
        double overallGeh = 0;
        for (double value : aggregatedGeh) {
            overallGeh += value;
        }
        overallGeh /= 24;
        System.out.println("overallGeh = " + overallGeh);
        writer.close();
    }

    private void writeNewHeader(String[] oldHeader) {
        String[] newHeader = new String[oldHeader.length+1];
        System.arraycopy(oldHeader, 0, newHeader, 0, oldHeader.length);
        newHeader[newHeader.length-1] = GEH_Header;
        writer.writeLine(newHeader);
    }

    private void writeCountWithGeh(String[] count) {
        double gehValue = calculateGehFrom(count);

        String[] newCount = new String[count.length+1];
        System.arraycopy(count, 0, newCount, 0, count.length);
        newCount[newCount.length-1] = String.valueOf(gehValue);

        writer.writeLine(newCount);

        int hour = Integer.valueOf(count[1])-1;
        aggregatedGeh[hour] += gehValue;
    }

    private double calculateGehFrom(String[] count) {
        double realCountValue = Double.valueOf(count[rowOfRealCountValue]);
        double simulatedCountValue = Double.valueOf(count[rowOfSimulatedCountValue]);
        return GehCalculator.calculateGEH(simulatedCountValue, realCountValue);
    }

}
