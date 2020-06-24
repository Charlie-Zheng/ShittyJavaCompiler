import java.util.ArrayList;
import java.util.LinkedHashMap;

import java_cup.runtime.ComplexSymbolFactory.Location;

/**
 * 
 */

/**
 * Scopes stores the name and return type of a variable, as well as the expected
 * return type of the scope, the starting location of the scope, and the type of
 * the scope.
 * 
 * @author charl
 */
public class Scope {
	/**
	 * The type of scope
	 */
	ScopeType type;
	/**
	 * Stores the type of the variables
	 */
	private ArrayList<Type> varType = new ArrayList<Type>();
	/**
	 * Links the name of a variable to the index that is used to access its info in
	 * varType
	 */
	private LinkedHashMap<String, Integer> varNameToInt = new LinkedHashMap<String, Integer>();
	/**
	 * The number of variables in this scope, which is also the index of the next
	 * variable to be added
	 */
	private int numVar = 0;
	/**
	 * Where the scope starts, helps for identifying the scope
	 */
	protected Location start;
	/**
	 * The name of the scope, used for code generation
	 */
	private String name = "L";
	/**
	 * What the scope is expected to return
	 */
	private Type retType;

	/**
	 * Creates a scope with the specified type and start location, a null return
	 * type, and name "L"
	 * 
	 * @param type
	 *            The type of scope
	 * @param start
	 *            The starting location of the scope
	 */
	public Scope(ScopeType type, Location start) {
		this.type = type;
		this.start = start;
	}

	/**
	 * Creates a scope with the specified type and start location, and a null return
	 * type and the specified name
	 * 
	 * @param type
	 *            The type of scope
	 * @param start
	 *            The starting location of the scope
	 */
	public Scope(ScopeType type, Location start, String name) {
		this.type = type;
		this.start = start;
		this.name = name;
	}

	/**
	 * Creates a scope with the specified type and start location, a null return
	 * type, and name "L"
	 * 
	 * @param type
	 *            The ScopeType of scope
	 * @param start
	 *            The starting location of the scope
	 * @param retType
	 *            The return Type of the scope
	 */
	public Scope(ScopeType type, Location start, Type retType) {
		this.type = type;
		this.start = start;
		this.retType = retType;
	}

	/**
	 * @return The type of scope
	 */
	public ScopeType getType() {
		return type;
	}

	/**
	 * Checks if the ID is already in the scope
	 * 
	 * @param ID
	 *            The ID to check
	 * @return true if the ID is already in the scope, false otherwise
	 */
	public boolean contains(String ID) {
		return varNameToInt.containsKey(ID);
	}

	/**
	 * Checks if there is a variable with the ID in the scope
	 * 
	 * @param ID
	 *            The ID to check
	 * @return true if there is a variable with the ID in the scope, false otherwise
	 */
	public boolean containsVar(String ID) {
		return varNameToInt.containsKey(ID);
	}

	/**
	 * Returns the type of the specified variable. Does not check if the variable is
	 * in the scope
	 * 
	 * @param ID
	 *            The ID of the variable to get the type for
	 * @return The type of the variable
	 */
	public Type getVarType(String ID) {
		return varType.get(varNameToInt.get(ID));
	}

	/**
	 * @return the return type of the scope
	 */
	public Type getRetType() {
		return retType;
	}

	/**
	 * @return the name of the scope
	 */
	public String getName() {
		return name;
	}

	/**
	 * Adds the specified variable name with the specified type to the scope. Does
	 * not check for duplicate IDs.
	 * 
	 * @param varName
	 *            The name of the variable
	 * @param type
	 *            The type of the variable
	 */
	public void addVar(String varName, Type type) {
		varNameToInt.put(varName, numVar++);
		varType.add(type);
	}

	/**
	 * Prints the location of the scope
	 */
	protected void printLoc() {
		System.out.println("------------------------------------------------------");
		System.out.println("Starts at: " + start + "\t\t" + this);
	}

	/**
	 * Prints the variable names and types in the scope, in the order they were
	 * added
	 */
	protected void printVar() {
		StringBuffer s = new StringBuffer();
		for (String name : varNameToInt.keySet()) {
			s.append("var:\t" + varType.get(varNameToInt.get(name)) + " ");
			s.append(name);
			s.append("\n");
		}
		System.out.print(s);
	}

	/**
	 * Prints the location then the variables in the scope
	 */
	public void print() {
		printLoc();
		printVar();
	}

}

/**
 * The types of scopes
 * 
 * @author charl
 */
enum ScopeType {
	func, block, loop;
}