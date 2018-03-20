package playground.gthunig.cemdap.cemdapInputTest;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ZonesReader {

    private String zonesFile;
    private String spliterator;
    private int csvZoneIndex;


    public ZonesReader(String zonesFile) {

        this(zonesFile, "\t", 0);
    }

    public ZonesReader(String zonesFile, String spliterator) {

        this(zonesFile, spliterator, 0);
    }

    public ZonesReader(String zonesFile, int csvZoneIndex) {

        this(zonesFile, "\t", csvZoneIndex);
    }

    public ZonesReader(String zonesFile, String spliterator, int csvZoneIndex) {

        this.zonesFile = zonesFile;
        this.spliterator = spliterator;
        this.csvZoneIndex = csvZoneIndex;
    }

    public List<String> readZones() throws IOException {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(zonesFile));
        String line;
        List<String> zones = new ArrayList<>();
        while ((line = bufferedReader.readLine()) != null) {
            zones.add(line.split(spliterator)[csvZoneIndex]);
        }
        return zones;
    }
}
