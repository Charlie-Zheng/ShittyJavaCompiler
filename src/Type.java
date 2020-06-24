
/**
 * The different Types of variables, function parameters, function returns
 * 
 * @author charl
 */
public enum Type {
	INT, BOOLEAN, VOID, STRING;
	/**
	 * @return The string representation of the Type
	 */
	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
