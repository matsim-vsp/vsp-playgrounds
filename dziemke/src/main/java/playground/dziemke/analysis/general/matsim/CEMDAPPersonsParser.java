package playground.dziemke.analysis.general.matsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import playground.dziemke.accessibility.OTPMatrix.CSVReader;

/**
 * @author gthunig on 12.07.2017.
 */
public class CEMDAPPersonsParser {
    private final static Logger log = Logger.getLogger(CEMDAPPersonsParser.class);

    private Population population;

    public CEMDAPPersonsParser() {
        population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
    }

    public Population parseFrom(String cemdapPersonsFile) {
        CSVReader reader = new CSVReader(cemdapPersonsFile, "\t");

        String[] line = reader.readLine();
        while (line != null) {
            Person person = parsePerson(line);
            population.addPerson(person);
            line = reader.readLine();
        }
        return population;
    }

    private Person parsePerson(String[] personAttributes) {
        String uniquePersonId = personAttributes[CEMDAPPersonUtils.PERSON_ID];
        Person person = population.getFactory().createPerson(Id.create(uniquePersonId, Person.class));

        int age = Integer.parseInt(personAttributes[CEMDAPPersonUtils.AGE]);
        if (age >= 0) {
            PersonUtils.setAge(person, age);
        } else {
            log.warn("Age is not a positive number.");
        }

        int female = Integer.parseInt(personAttributes[CEMDAPPersonUtils.FEMALE]);
        if (female == 1) {
            PersonUtils.setSex(person, "female");
        } else if (female == 0) {
            PersonUtils.setSex(person, "male");
        } else {
            log.warn("Sex is neither male nor female.");
        }

        int employed = Integer.parseInt(personAttributes[CEMDAPPersonUtils.EMPLOYED]);
        if (employed == 1) {
            PersonUtils.setEmployed(person, true);
        } else if (employed == 0) {
            PersonUtils.setEmployed(person, false);
        } else {
            log.warn("No information on employment.");
        }

        int student = Integer.parseInt(personAttributes[CEMDAPPersonUtils.STUDYING]);
        if (student == 1) {
            CEMDAPPersonUtils.setStudent(person, true);
        } else if (student == 0) {
            CEMDAPPersonUtils.setStudent(person, false);
        } else {
            log.warn("No information on being student.");
        }

        int driversLicence = Integer.parseInt(personAttributes[CEMDAPPersonUtils.LICENSE]);
        if (driversLicence == 1) {
            PersonUtils.setLicence(person, "yes");
        } else {
            PersonUtils.setLicence(person, "no");
        }

        return person;
    }
}
