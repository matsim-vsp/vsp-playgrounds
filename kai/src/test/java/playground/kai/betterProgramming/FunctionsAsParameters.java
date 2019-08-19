package playground.kai.betterProgramming;

import org.junit.Test;

import java.util.*;
import java.util.function.Function;

public class FunctionsAsParameters{

	@Test public void test1() {

		List<Integer> numbers = Arrays.asList( 1, 10, 3, 2, 8 );

		numbers.sort( new Comparator<Integer>(){
			@Override public int compare( Integer x, Integer y ){
				return Integer.compare( x, y );
			}
		} ) ;

	}

}

class AnimalCollector {
	private final Map<String ,Integer> animals = new HashMap<>() ;
	private final Function<String,String> mapNoiseToAnimal  ;

	AnimalCollector( Function<String,String> mapNoseToAnimal ) {
		this.mapNoiseToAnimal = mapNoseToAnimal ;
	}

	void addAnimal( String animalNoise ) {
		String animal = this.mapNoiseToAnimal.apply( animalNoise );
		animals.merge( animal, 1, ( a, b ) -> {
			return Integer.sum( a, b );
		} ) ;
	}

}
