package playground.lu.helloworld;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class ReadingCsv {
    public static void main(String[] args) throws IOException {
        BufferedReader csvReader = new BufferedReader(new FileReader("/Users/luchengqi/Documents/SVN/shared-svn/projects/german-wide-freight/original-data/Boundary_Links.csv"));
        String[] linksIdStrings = csvReader.readLine().split(",");
        for (String string : linksIdStrings) {
            System.out.println(string);
        }

    }
}
