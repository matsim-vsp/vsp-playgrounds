package playground.dziemke.analysis.general.matsim;

import org.matsim.api.core.v01.population.Person;

/**
 * @author gthunig on 13.07.2017.
 */
public class CEMDAPPersonUtils {

    public static final int PERSON_ID = 1;
    public static final int EMPLOYED = 2;
    public static final int STUDYING = 3;
    public static final int LICENSE = 4;
    public static final int FEMALE = 7;
    public static final int AGE = 8;

    private static final String STUDENT_IDENTIFIER = "student";

    static Boolean isStudent(Person person) {
        return (Boolean) person.getCustomAttributes().get(STUDENT_IDENTIFIER);	}

    static void setStudent(Person person, Boolean student) {
        if (student!=null){
            //person.getCustomAttributes().put(CA_STUDENT, student);
            person.getAttributes().putAttribute(STUDENT_IDENTIFIER, student);
        }
    }
}
