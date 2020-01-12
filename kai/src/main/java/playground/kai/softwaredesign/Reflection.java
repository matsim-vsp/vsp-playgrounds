package playground.kai.softwaredesign;

import java.lang.reflect.Field;
import java.lang.reflect.TypeVariable;

class Reflection{

	public static void main( String[] args ){

		final Class<Number> stringClass = Number.class;

		for( Field field : stringClass.getFields() ){
			System.out.println( field );
		}

		for( Field declaredField : stringClass.getDeclaredFields() ){
			System.out.println( declaredField );
		}

		for( TypeVariable<Class<Number>> typeParameter : stringClass.getTypeParameters() ){
			System.out.println(  typeParameter );
		}


	}

}
