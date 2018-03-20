package playground.gthunig.cemdapSpecificationFileModifier;

import com.sun.xml.bind.v2.runtime.output.SAXOutput;

import java.io.*;

public class CemdapSpecificationFileModifier {

    public static void main(String[] args) throws IOException {

        String toRead = "C:\\Users\\gthunig\\Desktop\\CML_no_optionals.cml";
        String toWrite = "C:\\Users\\gthunig\\Desktop\\CML_no_optionals_new.cml";
        BufferedReader bufferedReader = new BufferedReader(new FileReader(toRead));
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(toWrite));

        String line = bufferedReader.readLine();
        while (line != null) {
            if (line.contains("Variable ID")) {
                String[] split = line.replaceAll("\\s+","").split("ID=\"");
                String idString = split[1].split("\"")[0];
                int id = Integer.parseInt(idString);
                if ((id > 5 && id < 10000) ||
                    (id > 10009 && id < 20000) ||
                    (id > 20000 && id < 30000)) {
                    System.out.println("Delete line with id " + id);
                } else {
                    bufferedWriter.write(line);
                    bufferedWriter.newLine();
                }
            } else {
                bufferedWriter.write(line);
                bufferedWriter.newLine();
            }

            line = bufferedReader.readLine();
        }
        bufferedWriter.close();
        bufferedReader.close();
    }
}
