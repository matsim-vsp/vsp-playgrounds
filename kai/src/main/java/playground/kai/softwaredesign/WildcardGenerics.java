package playground.kai.softwaredesign;

import org.matsim.withinday.utils.EditTrips;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class WildcardGenerics{

	static class Stack<E> {
		private List<E> list ;
		Stack() {
			list = new ArrayList<>() ;
		}
		void push(E e) {
			list.add( e ) ;
		}
		E pop() {
			return list.remove( list.size()-1 ) ;
		}
		boolean isEmpty() {
			return list.isEmpty() ;
		}
		void pushAll( Iterable<? extends E> src ) {
			for ( E e : src ) {
				this.push( e );
			}
		}

	}

	public static void main( String[] args ){

		Stack<Number> numberStack = new Stack<>() ;

		numberStack.push( new Integer( 20 ) );

		System.out.println(  numberStack.pop() );

		List<Integer> list = new ArrayList<>() ;
		list.add( 30 );
		list.add( 2 );

		numberStack.pushAll( list );

	}

}
