
/**
 * The different types of TreeNodes
 * 
 * @author charl
 */
public enum TreeNodeType {
	NUM, STRING, TRUE("true"), FALSE("false"), BOOL("boolean"), INT("int"), VOID(
			"void"), globaldeclarations, variabledeclaration, ID, functiondeclaration, functionheader, functiondeclarator, formalparameterlist, formalparameter, mainfunctiondeclaration, mainfunctiondeclarator, block, blockstatements, BREAK(
					"break"), RETURN("return"), WHILE("while"), IFELSE("if(...) else"), IF(
							"if"), statementexpression, argumentlist, functioninvocation, NOT("!"), UNARY_SUB("-"), SUB(
									"-"), MUL("*"), DIV("/"), MOD("%"), ADD("+"), LT("<"), GT(">"), LE("<="), GE(
											">="), EQ("=="), NEQ("!="), AND("&&"), OR("||"), ASSIGN("="), nullStatement;
	/*
	 * Addition code to set the string representation of the type
	 */
	private String name;

	private TreeNodeType(String name) {
		this.name = name;
	}

	private TreeNodeType() {
	}

	/**
	 * The string representation of the TreeNodeType
	 */
	@Override
	public String toString() {
		if (name != null)
			return name;
		else
			return name();
	}
}
