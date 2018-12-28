package playground.dziemke.analysis.generalNew;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

public class SurveyAdditionalAttributesUtils {

    public static void setSource(Population population, SurveyAdditionalAttributes.Source source) {

        population.getAttributes().putAttribute(SurveyAdditionalAttributes.SOURCE, source.name());
    }

    public static String getSource(Population population) {

        Object source = population.getAttributes().getAttribute(SurveyAdditionalAttributes.SOURCE);
        return source != null ? source.toString() : SurveyAdditionalAttributes.Source.MATSIM.name();
    }

    public static void setWeight(Population population, double weight) {

        population.getAttributes().putAttribute(SurveyAdditionalAttributes.AGGREGATED_WEIGHT, Double.toString(weight));
    }

    public static double getWeight(Population population) {

        return Double.parseDouble(population.getAttributes().getAttribute(SurveyAdditionalAttributes.AGGREGATED_WEIGHT).toString());
    }

    public static void setAge(Person person, int age) {

        person.getAttributes().putAttribute(SurveyAdditionalAttributes.AGE, Integer.toString(age));
    }

    public static int getAge(Person person) {

        return Integer.parseInt(person.getAttributes().getAttribute(SurveyAdditionalAttributes.AGE).toString());
    }

    public static void setWeight(Leg leg, double weight) {

        leg.getAttributes().putAttribute(SurveyAdditionalAttributes.WEIGHT, Double.toString(weight));
    }

    public static double getWeight(Leg leg) {

        return Double.parseDouble(leg.getAttributes().getAttribute(SurveyAdditionalAttributes.WEIGHT).toString());
    }

    public static void setDistanceBeeline_m(Leg leg, double distanceBeeline_m) {

        leg.getAttributes().putAttribute(SurveyAdditionalAttributes.DISTANCE_BEELINE_M, Double.toString(distanceBeeline_m));
    }

    public static double getDistanceBeeline_m(Leg leg) {

        return Double.parseDouble(leg.getAttributes().getAttribute(SurveyAdditionalAttributes.DISTANCE_BEELINE_M).toString());
    }

    public static void setSpeed_m_s(Leg leg, double speed_m_s) {

        leg.getAttributes().putAttribute(SurveyAdditionalAttributes.SPEED_M_S, Double.toString(speed_m_s));
    }

    public static double getSpeed_m_s(Leg leg) {

        return Double.parseDouble(leg.getAttributes().getAttribute(SurveyAdditionalAttributes.SPEED_M_S).toString());
    }
}
