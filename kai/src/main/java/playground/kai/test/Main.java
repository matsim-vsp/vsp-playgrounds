/**
 * 
 */
package playground.kai.test;

public class Main {

	public static void main(String[] args) {

		MyDouble val1 = new MyDouble( 5. ) ;
		MyDouble val2 = new MyDouble( 6. ) ;
		
//		double val = val1 + val2 ;

	}
	
	static class MyDouble extends Number {
		
		Double delegate ;
		MyDouble( double val ) {
			delegate = new Double( val ) ;
		}
		
		public static String toString( final double d ) {
			return Double.toString( d );
		}
		
		public static String toHexString( final double d ) {
			return Double.toHexString( d );
		}
		
		public static Double valueOf( final String s ) throws NumberFormatException {
			return Double.valueOf( s );
		}
		
		public static Double valueOf( final double d ) {
			return Double.valueOf( d );
		}
		
		public static double parseDouble( final String s ) throws NumberFormatException {
			return Double.parseDouble( s );
		}
		
		public static boolean isNaN( final double v ) {
			return Double.isNaN( v );
		}
		
		public static boolean isInfinite( final double v ) {
			return Double.isInfinite( v );
		}
		
		public static boolean isFinite( final double d ) {
			return Double.isFinite( d );
		}
		
		public boolean isNaN() {
			return delegate.isNaN();
		}
		
		public boolean isInfinite() {
			return delegate.isInfinite();
		}
		
		@Override public String toString() {
			return delegate.toString();
		}
		
		@Override public byte byteValue() {
			return delegate.byteValue();
		}
		
		@Override public short shortValue() {
			return delegate.shortValue();
		}
		
		@Override public int intValue() {
			return delegate.intValue();
		}
		
		@Override public long longValue() {
			return delegate.longValue();
		}
		
		@Override public float floatValue() {
			return delegate.floatValue();
		}
		
		@Override public double doubleValue() {
			return delegate.doubleValue();
		}
		
		@Override public int hashCode() {
			return delegate.hashCode();
		}
		
		public static int hashCode( final double value ) {
			return Double.hashCode( value );
		}
		
		@Override public boolean equals( final Object obj ) {
			return delegate.equals( obj );
		}
		
		public static long doubleToLongBits( final double value ) {
			return Double.doubleToLongBits( value );
		}
		
		public static long doubleToRawLongBits( final double value ) {
			return Double.doubleToRawLongBits( value );
		}
		
		public static double longBitsToDouble( final long bits ) {
			return Double.longBitsToDouble( bits );
		}
		
		public int compareTo( final Double anotherDouble ) {
			return delegate.compareTo( anotherDouble );
		}
		
		public static int compare( final double d1, final double d2 ) {
			return Double.compare( d1, d2 );
		}
		
		public static double sum( final double a, final double b ) {
			return Double.sum( a, b );
		}
		
		public static double max( final double a, final double b ) {
			return Double.max( a, b );
		}
		
		public static double min( final double a, final double b ) {
			return Double.min( a, b );
		}
	}

}
