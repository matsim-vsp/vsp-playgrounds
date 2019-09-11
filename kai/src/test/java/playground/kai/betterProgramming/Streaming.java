package playground.kai.betterProgramming;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class Streaming{

	@Test
	public void test() {

		List<Integer> numbers = Arrays.asList( 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 );

		Integer result = numbers.stream()
						.filter( n -> n > 3 )
						.filter( n -> n % 2 == 0 )
						.findAny()
						.map( n -> n * 2 )
						.get();

		Stream<Integer> result3 = numbers.stream().map( n -> n * 2 );

		result3.forEach( System.out::println ) ;

		System.out.println( result );

	}

}
