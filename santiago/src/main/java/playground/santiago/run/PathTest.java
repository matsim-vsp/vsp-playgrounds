package playground.santiago.run;


import java.io.File;

public class PathTest {

	public static void main(String[] args) {
		File file = new File("Hola");
		String path = file.getAbsolutePath();
		System.out.println(path);

	}

}
