import java_cup.runtime.ComplexSymbolFactory.Location;

/**
 * 
 */

/**
 * Class to override the toString() method in ComplexSymbolFactory.Location
 * 
 * @author charl
 */
public class NewLocation extends Location {
	static void printc(long c) {
		System.out.print((char) c);

	}

	static void printi(int x){
		int div;
		div = 1;
		if(x == 0){
			printc(48);		
		}else if(x < 0) {
			printc(45); //the negative sign
			while(div<=-(x/10)) {
				div = div*10;
			}
			printc(-(x/div)+48);
			while(div>1) {
				printc((-(x%div))/(div/10)+48);
				div = div/10;
			}
			
		}else{
			while(div<=x/10) {
				div = div*10;
			}
			printc(x/div+48);
			while(div>1) {
				printc((x%div)/(div/10)+48);
				div = div/10;
			}
		}
	}
	static void prints(String s) {
		System.out.print(s);
	}
	public static void main(String args[]) {
		prints("asdf");
		prints("\b\t\n\f\r\"\'\\");
		prints("");
		if (true) {
			// printing this depends on string representation,
			// but the NUL should appear in the assembly generated
			prints("\0\0asdf");
		}
		printc(0);
//		System.out.println(
//				"java -jar \"java-cup-11b.jar\" -locations NewParser.cup && jflex test.flex && javac -d ./ -cp ./java-cup-11b-runtime.jar:. *.java");
//		String[] list = {
//				"art-life.j--",
//				"art-select.j--",
//				"art-sieve.j--",
//				"gen.t1",
//				"gen.t10",
//				"gen.t11",
//				"gen.t12",
//				"gen.t13",
//				"gen.t14",
//				"gen.t15",
//				"gen.t18",
//				"gen.t22",
//				"gen.t26",
//				"gen.t29",
//				"gen.t30",
//				"gen.t31",
//				"gen.t32",
//				"gen.t33",
//				"gen.t34",
//				"gen.t5",
//		};
//		for (String x : list)
//			System.out.println(
//					"java -cp \"java-cup-11b-runtime.jar:.\" Scan ~aycock/411/TEST/final/" + x
//							+ " > /home/ugb/ce.zheng1/cpsc411/Scanner/src/final/" + x + ".wat\r\n"
//							+
//							"~aycock/411/bin/wat2wasm /home/ugb/ce.zheng1/cpsc411/Scanner/src/final/" + x + ".wat\r\n" +
//							"~aycock/411/bin/wasm-interp --411 /home/ugb/ce.zheng1/cpsc411/Scanner/src/" + x + ".wasm");
	}

	/**
	 * Passes parameters to java_cup.runtime.ComplexSymbolFactory.Location
	 * constructor
	 */
	public NewLocation(Location other) {
		super(other);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Passes parameters to java_cup.runtime.ComplexSymbolFactory.Location
	 * constructor
	 */
	public NewLocation(int line, int column) {
		super(line, column);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Passes parameters to java_cup.runtime.ComplexSymbolFactory.Location
	 * constructor
	 */
	public NewLocation(String unit, int line, int column) {
		super(unit, line, column);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Passes parameters to java_cup.runtime.ComplexSymbolFactory.Location
	 * constructor
	 */
	public NewLocation(int line, int column, int offset) {
		super(line, column, offset);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Passes parameters to java_cup.runtime.ComplexSymbolFactory.Location
	 * constructor
	 */
	public NewLocation(String unit, int line, int column, int offset) {
		super(unit, line, column, offset);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @return Returns a more readable format for the location
	 */
	@Override
	public String toString() {
		return "line " + this.getLine() + ", column " + this.getColumn();
	}

}
