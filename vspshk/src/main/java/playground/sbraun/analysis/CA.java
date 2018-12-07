package playground.sbraun.analysis;

public class CA {
	
	private static String e = "-";
	private static String f = "M";
	private static Boolean signal = false;
	private static int t = 0;
	private static Boolean density = false;
	
	private static String[] street = new String[100];
	
	private static void initStreet(){
		for(int i = 0; i < street.length ; i++){
			street[i]=e;
		}	
	}
	private static void printStreet(){
		for(int i = 0; i < street.length ; i++){
			if(i==street.length-30){
				if(signal){
					System.out.print("|");
				}else System.out.print(" ");
			}
			System.out.print(street[i]);
		}
		System.out.println();	
	}
	private static void switchlight(){
		signal = !signal;
	}
	
	//This is the CA logic
	private static String[] streetNextSecond(String[] oldstreet){
		String[] newstreet = new String[street.length];
		for(int i = oldstreet.length-1; i>=0; i--){
			if (!(signal && i==street.length-30)){
				if (i != oldstreet.length-1){
					if(oldstreet[i].equals(f)){
						if(oldstreet[i+1].equals(f) || (i==street.length-31 && signal)){
								newstreet[i] = f;
						} else newstreet[i] = e;
					}else{
						if(i!=0){
							if(oldstreet[i-1].equals(f)){
								newstreet[i] = f;
							} else newstreet[i] = e;
						} else newstreet[i] =e;					
					}
				} else newstreet[i] = e;
			} else newstreet[i] = e;
		}
		return newstreet;
	}
	
	
	
	
	
	public static void main(String[] args ){
		initStreet();
		
		System.out.println("This is a simple simulation of a street with a traffic light to demonstrate kinematic waves using a Cellular Automaton.");
		System.out.println();
		System.out.println("time:                   ->                       ->                    Signal            ->");
		
		
		while(t<500){
			//This is for the time-axis
			if(t%5==0){
				if(t<10){
					System.out.print(t+"   ");
				}else if (t<100){
					System.out.print(t+"  ");
				}else System.out.print(t+" ");
			} else System.out.print("    ");
			
			printStreet();
			
		//Adapt here different densities in time; default: always the same density
			if(density){
				if(t%3 == 0) street[0]= f;
			} else{
				if(t%3 == 0) street[0]= f;
			}
			if(t%10 == 0) switchlight();
			
			street = streetNextSecond(street);
			
			if (t%40==0) density = !density;
			t++;
		}
		
		//This is for the time-axis
		if(t%5==0){
				if(t<10){
					System.out.print(t+"   ");
				}else if (t<100){
					System.out.print(t+"  ");
				}else System.out.print(t+" ");
			} else System.out.print("    ");
		printStreet();
	}
	
	
}