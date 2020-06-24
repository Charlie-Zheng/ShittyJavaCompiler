import java_cup.runtime.ComplexSymbolFactory.Location;

/**
 * 
 */

/**
 * Records the type of semantic exception, and a message to be printed to the
 * user. The location of the error can also be set and printed with the message
 * 
 * @author charl
 */
public class SemanticException extends Exception {
	private SemanticExceptionTypes type;
	private Location loc;
	private String name;

	/**
	 * Creates a new SemanticException with the specified type, and message
	 * 
	 * @param type
	 *            The type of SemanticException
	 * @param msg
	 *            The message to send
	 */
	public SemanticException(SemanticExceptionTypes type, String msg) {
		super(type + "\n" + msg);
		this.type = type;

	}

	/**
	 * Creates a new SemanticException with the specified type, location, and
	 * message
	 * 
	 * @param type
	 *            The type of SemanticException
	 * @param loc
	 *            The location of the error
	 * @param msg
	 *            The message to send
	 */
	public SemanticException(SemanticExceptionTypes type, Location loc, String msg) {
		super(type + " at " + loc + " in file " + Scan.inputFileName + "\n" + msg);
		this.type = type;
		this.loc = loc;
	}

	/**
	 * Creates a new SemanticException with the specified type, token name,
	 * location, and message
	 * 
	 * @param type
	 *            The type of SemanticException
	 * @param name
	 *            The token name on which the error occured
	 * @param loc
	 *            The location of the error
	 * @param msg
	 *            The message to send
	 */
	public SemanticException(SemanticExceptionTypes type, String name, Location loc, String msg) {
		super(type + " at " + loc + " in file " + Scan.inputFileName + " on token: " + name + "\n" + msg);
		this.name = name;
		this.type = type;
		this.loc = loc;
	}

	/**
	 * @return the type
	 */
	public SemanticExceptionTypes getType() {
		return type;

	}
}

/**
 * The different types of semantic exceptions, and code to set the string
 * representation of the exception
 * 
 * @author charl
 */
enum SemanticExceptionTypes {
	DuplicateVariable("Error: Duplicate variable"), DuplicateFunction("Error: Duplicate function"), DuplicateMain(
			"Error: Duplicate main"), UndeclaredVariable("Error: Undeclared variable"), UndeclaredFunction(
					"Error: Undeclared function"), AssignmentLeft("Error: Assignment mismatch"), TypeMismatch(
							"Error: Type mismatch"), ArgumentMismatch(
									"Error: Argument mismatch"), VariableDeclarationInInnerBlock(
											"Error: Variable declaration in inner block"), BreakNotInLoop(
													"Error: Break not in loop"), NoMain(
															"Error: Main function not found"), FunctionDoesNotReturn(
																	"Error: Function does not return"), CallMain("Error: Attempted to call main function");
	String name;

	private SemanticExceptionTypes() {

	}

	private SemanticExceptionTypes(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		if (name == null) {
			return name();
		} else {
			return name;
		}
	}
}
