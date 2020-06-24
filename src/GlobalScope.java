import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 
 */

/**
 * GlobalScope extends Scopes, and maintains all functionality of Scope, but
 * also allowed the storage of a main function and other functions. Can store
 * function names, function return types, and function parameters.
 * 
 * @author charl
 */
public class GlobalScope extends Scope {
	/**
	 * @return the mainFuncID
	 */
	public String getMainFuncID() {
		return mainFuncID;
	}

	/**
	 * Creates a GlobalScope at line 0, column 0 with the given name
	 * 
	 * @param name
	 *            The name of the scope. Do not use a name containing a space. Use unique names for unique scopes
	 */

	public GlobalScope(String name) {
		super(ScopeType.block, new NewLocation(0, 0),name);
	}


	/**
	 * Stores the return types of the functions
	 */
	private ArrayList<Type> funcGlobalReturnType = new ArrayList<Type>();
	/**
	 * Stores the list of parameter types of the functions
	 */
	private ArrayList<List<Type>> funcGlobalInputType = new ArrayList<List<Type>>();
	/**
	 * Links the name of a function to the index that is used to access its info in
	 * funcGlobalReturnType and funcGlobalInputType
	 */
	private LinkedHashMap<String, Integer> funcGlobalNameToInt = new LinkedHashMap<String, Integer>();
	/**
	 * Stores the ID of the main function
	 */
	private String mainFuncID = "";
	/**
	 * The number of functions in this scope, which is also the index of the next
	 * function to be added
	 */
	private int numFunc = 0;

	/**
	 * Checks if the ID is already in the scope (either a function or a variable
	 * already uses that ID)
	 * 
	 * @param ID
	 *            The ID to check
	 * @return true if the ID is already in the scope, false otherwise
	 */
	@Override
	public boolean contains(String ID) {
		return funcGlobalNameToInt.containsKey(ID) || super.contains(ID) || mainFuncID.equals(ID);
	}

	/**
	 * Checks if there is a function with the ID in the scope
	 * 
	 * @param ID
	 *            The ID to check
	 * @return true if there is a function with the ID in the scope, false otherwise
	 */
	public boolean containsFunc(String ID) {
		return funcGlobalNameToInt.containsKey(ID);
	}

	/**
	 * Returns the parameter types of the specified function. Does not check if the
	 * function is in the scope
	 * 
	 * @param ID
	 *            The ID of the function to get the parameter types for
	 * @return A List containing the parameter types of the function in left to
	 *         right order.
	 */
	public List<Type> getFuncInputType(String ID) {
		return funcGlobalInputType.get(funcGlobalNameToInt.get(ID));
	}

	/**
	 * Returns the return type of the specified function. Does not check if the
	 * function is in the scope
	 * 
	 * @param ID
	 *            The ID of the function to get the return type for
	 * @return The return type of the function
	 */
	public Type getFuncReturnType(String ID) {
		return funcGlobalReturnType.get(funcGlobalNameToInt.get(ID));
	}

	/**
	 * Adds the specified function name, with the specified parameter types and
	 * return type to the scope.Does not check for duplicate IDs.
	 * 
	 * @param funcName
	 *            The name of the function
	 * @param argTypes
	 *            A list of the parameter types
	 * @param returnType
	 *            The return type
	 */
	public void addFunc(String funcName, List<Type> argTypes, Type returnType) {
		funcGlobalNameToInt.put(funcName, numFunc++);
		funcGlobalInputType.add(argTypes);
		funcGlobalReturnType.add(returnType);
	}

	/**
	 * Adds the main function specified to the scope. Does not check if one is
	 * already defined
	 * 
	 * @param funcName
	 *            The name of the main function to add
	 */
	public void addMainFunc(String funcName) {
		mainFuncID = funcName;
	}


	/**
	 * Prints the location, then the functions in the scope, along with their return
	 * type and parameter types, then prints the variables in the scope
	 */
	public void print() {
		super.printLoc();
		StringBuffer s = new StringBuffer();
		s.append("main func:\t");
		s.append(mainFuncID);
		s.append('\n');
		for (String name : funcGlobalNameToInt.keySet()) {
			s.append("func:\t" + funcGlobalReturnType.get(funcGlobalNameToInt.get(name)));
			s.append(" " + name + "(");
			for (Type type : funcGlobalInputType.get(funcGlobalNameToInt.get(name))) {
				s.append(type + " ");
			}
			if (s.charAt(s.length() - 1) == ' ') {
				s.setCharAt(s.length() - 1, ')');
				s.append("\n");
			} else {
				s.append(")\n");
			}
		}
		System.out.println(s.toString());
		super.printVar();
	}
}
