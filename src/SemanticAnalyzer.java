import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
/**
 * 
 */

/**
 * @author charl
 */
public class SemanticAnalyzer {
	
	public final String defaultScopeName = "D";
	/**
	 * The AST that the analyzer will analyze
	 */
	private Tree tree;
	/**
	 * Set this to true to view scopes (for debugging purposes, etc)
	 */
	private final boolean displayScopes = false;
	/**
	 * A stack of the scopes
	 */
	private Deque<Scope> scopes;
	/**
	 * The default scope, contains the getchar(), halt(), printb(boolean),
	 * printc(int), printi(int), prints(String) functions
	 */
	private GlobalScope defaultScope;
	/**
	 * The global scope, contains user defined functions and global variables
	 */
	private GlobalScope gScope ;

	/**
	 * Constructor for the semantic analyzer, also initializes the default scope to
	 * contain the required default functions, and creates an empty global scope
	 * 
	 * @param tree
	 */
	public SemanticAnalyzer(Tree tree) {
		this.tree = tree;
		scopes = new ArrayDeque<Scope>();
		//add the default runtime functions to default scope
		defaultScope = new GlobalScope(defaultScopeName);
		scopes.addFirst(defaultScope);

		defaultScope.addFunc("getchar", new ArrayList<Type>(), Type.INT);
		defaultScope.addFunc("halt", new ArrayList<Type>(), Type.VOID);
		defaultScope.addFunc("printb", new ArrayList<Type>((Arrays.asList(Type.BOOLEAN))), Type.VOID);
		defaultScope.addFunc("printc", new ArrayList<Type>((Arrays.asList(Type.INT))), Type.VOID);
		defaultScope.addFunc("printi", new ArrayList<Type>((Arrays.asList(Type.INT))), Type.VOID);
		defaultScope.addFunc("prints", new ArrayList<Type>((Arrays.asList(Type.STRING))), Type.VOID);

		//create global scope
		gScope = new GlobalScope("G");
		scopes.addFirst(gScope);
	}

	/**
	 * Analyze the AST for semantic errors. Exits the program if a semantic error is
	 * found
	 */
	public void analyze() {
		try {
			//display default scope if scope display is enabled
			if (displayScopes)
				defaultScope.print();
			//Run the 0th pass, which collects global functions and variables and stores them in the global scope
			pass0();
			if (displayScopes)
				gScope.print();
			//Run the 1st pass, which does type checking, argument checking for return and function calls, checks for
			//undeclared functions and variables, and checks if breaks are inside loops
			pass1();
			//Run the 2nd pass, which checks if non-void functions are guaranteed to return on all execution paths
			pass2();
			//If an error is throw by any of the passes, print info about the error and exit	
		} catch (SemanticException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		} catch (ASTFormatException e) {
			e.printStackTrace();
			System.exit(1);
		}

	}

	/**
	 * Collects the return type, parameter number, and parameter types of user
	 * defined global function, and the type of declared global variables, and
	 * stores them inside the global scope
	 * 
	 * @throws SemanticException
	 *             If a semantic error is found in the AST: a variable or function
	 *             name is redefined in the same scope, no main function is found,
	 *             multiple main functions are found
	 * @throws ASTFormatException
	 *             If the AST given does not conform to the grammar (or my code is
	 *             bugged)
	 */
	private void pass0() throws SemanticException, ASTFormatException {

		boolean hasMain = false;
		if (tree.root.getType() == TreeNodeType.globaldeclarations) {
			for (TreeNode node : tree.root.getChildren()) {

				//  The ID of the variable or function
				String ID = node.getChild(1).getAttr();

				// If the ID is null, the AST has improper structure
				if (ID == null) {
					throw new ASTFormatException(
							"An incorrect child of globaldeclarations was found  of type " + node.type);
				}

				switch (node.getType()) {

				case mainfunctiondeclaration:
					//If the node is a main function
					//If a main function has already been found, throw a duplicate main semantic error 
					if (hasMain) {
						throw new SemanticException(SemanticExceptionTypes.DuplicateMain, ID, node.getLoc(),
								"Program cannot have two main functions");
					}
					//Set the flag for alreadying having a main to be true
					hasMain = true;
					//Add the main function to the global scope
					gScope.addMainFunc(node.getChild(1).getAttr());
					//Set the signature of the function
					node.getChild(1).annotate("main()", gScope);
					break;
				case functiondeclaration:
					//If the node is a function
					//If a function or variable with the same name is already found, throw a duplicate function semantic error
					if (gScope.contains(ID)) {
						throw new SemanticException(SemanticExceptionTypes.DuplicateFunction, ID,
								node.getChild(1).getLoc(),
								"Function names must be unique from other functions or variables");
					}

					//ArrayList to store the parameter types of the function
					ArrayList<Type> argTypes = new ArrayList<Type>();
					//Get the return type of the function
					Type returnType = getTypeFromNodeType(node.getChild(0).getType());
					//Create a stringbuffer to store the text list of the parameter types to aid with error message
					StringBuffer paramTypes = new StringBuffer();
					//Add the parameter types to the arraylist of parameter types
					for (TreeNode formalParameter : node.getChild(2).getChildren()) {
						argTypes.add(getTypeFromNodeType(formalParameter.getChild(0).getType()));
					}
					//Build the string representation of the list of paramter types
					for (Type argType : argTypes) {
						if (paramTypes.length() != 0)
							paramTypes.append(", ");
						paramTypes.append(argType);
					}
					//Add the function, along with its parameter types and return type to the global scope
					gScope.addFunc(node.getChild(1).getAttr(), argTypes, returnType);
					//Set the signature of the function
					node.getChild(1).annotate(returnType + " f(" + paramTypes.toString() + ")", gScope);
					break;
				case variabledeclaration:
					//If the node is a variable
					//If a function or variable with the same name is already found, throw a duplicate variable semantic error
					if (gScope.contains(ID)) {
						throw new SemanticException(SemanticExceptionTypes.DuplicateVariable, ID,
								node.getChild(1).getLoc(),
								"Variable names must be unique from other functions or variables");
					}
					//Add the variable and its type to the global scope
					gScope.addVar(ID, getTypeFromNodeType(node.getChild(0).getType()));
					//Set the signature of the variable
					setSigForVar(node, gScope);
					break;
				default:
					throw new ASTFormatException(
							"An incorrect child of globaldeclarations was found of type " + node.type);
				}
			}
			//If, at the end of the pass, no main function was found, throw a no main function semantic error
			if (!hasMain) {
				throw new SemanticException(SemanticExceptionTypes.NoMain, "Program has no main function");
			}
		} else {
			throw new ASTFormatException("Root of tree is not globaldeclaration");
		}
	}

	/**
	 * Runs a recursive check on the program, which will proceed line by line, top
	 * to bottom. Checks if the typing is correct for operators, return statements,
	 * or function calls. Also checks if break statements are inside a loop, if a
	 * variable is only declared in outermost block, if a duplicate variable is
	 * declared, if a variable used before it is declared, or if an undeclared
	 * function is called
	 * 
	 * @throws SemanticException
	 *             If a semantic error is found in the AST: type mismatch, argument
	 *             number mismatch, break not inside loop, variable declared in
	 *             inner block, a variable or function name is redefined in the same
	 *             scope, undeclared variable or function
	 * @throws ASTFormatException
	 *             If the AST given does not conform to the grammar (or my code is
	 *             bugged)
	 */
	private void pass1() throws SemanticException, ASTFormatException {
		//Check every function in the program
		for (TreeNode node : tree.root.getChildren()) {
			switch (node.getType()) {
			//Check the block inside functions
			case functiondeclaration:
			case mainfunctiondeclaration:
				funcBlock(node);
				break;
			case variabledeclaration:
				break;
			default:
				throw new ASTFormatException("An incorrect child of globaldeclarations was found of type " + node.type);
			}
		}
	}

	/**
	 * Checks the function that is contained in the funcNode block, which should
	 * have TreeNodeType functiondeclaration or mainfunctiondeclaration, for if the
	 * typing is correct for operators, return statements, or function calls. Also
	 * checks if break statements are inside a loop, if a variable is only declared
	 * in outermost block, if a duplicate variable is declared, if a variable used
	 * before it is declared, or if an undeclared function is called
	 * 
	 * @param funcNode
	 *            The node which contains the function block
	 * @throws SemanticException
	 *             If a semantic error is found in the sub-AST rooted by funcNode:
	 *             type mismatch, argument number mismatch, break not inside loop,
	 *             variable declared in inner block, a variable or function name is
	 *             redefined in the same scope, undeclared variable or function
	 * @throws ASTFormatException
	 *             If the AST given does not conform to the grammar (or my code is
	 *             bugged)
	 */
	private void funcBlock(TreeNode funcNode) throws SemanticException, ASTFormatException {

		if (funcNode.getType() == TreeNodeType.functiondeclaration
				|| funcNode.getType() == TreeNodeType.mainfunctiondeclaration) {
			//Adds a new function local scope to the scope stack
			scopes.addFirst(
					new Scope(ScopeType.func, funcNode.getLoc(), getTypeFromNodeType(funcNode.getChild(0).getType())));

			//Adds the variables declared by the parameters of the function declaration to the local scope
			for (TreeNode formalParameter : funcNode.getChild(2).getChildren()) {
				scopes.peek().addVar(formalParameter.getChild(1).getAttr(),
						getTypeFromNodeType(formalParameter.getChild(0).getType()));
				//Set the signature of the variable
				setSigForVar(formalParameter, scopes.peek());
			}
			//Analyze the remaining block
			analyzeBlock(funcNode.getChild(3));
		} else {
			throw new ASTFormatException(
					"funcBlock(TreeNode) was called on a node that is not a functiondeclaration or a mainfunctiondeclaration");
		}
	
	}

	/**
	 * <p>
	 * Checks the block that is contained in the blockNode block, which should have
	 * TreeNodeType block, for if the typing is correct for operators, return
	 * statements, or function calls. Also checks if break statements are inside a
	 * loop, if a variable is only declared in outermost block, if a duplicate
	 * variable is declared, if a variable used before it is declared, or if an
	 * undeclared function is called. Then pops the top element of the scope stack.
	 * </p>
	 * <p>
	 * Please create a new scope before calling this function
	 * </p>
	 * 
	 * @param blockNode
	 *            The node which contains the block
	 * @throws SemanticException
	 *             If a semantic error is found in the sub-AST rooted by bockNode:
	 *             type mismatch, argument number mismatch, break not inside loop,
	 *             variable declared in inner block, a variable or function name is
	 *             redefined in the same scope, undeclared variable or function
	 * @throws ASTFormatException
	 *             If the AST given does not conform to the grammar (or my code is
	 *             bugged)
	 */
	private void analyzeBlock(TreeNode blockNode) throws SemanticException, ASTFormatException {

		if (blockNode.type == TreeNodeType.block) {
			//Analyze each individual statement in the block for the specified errors
			for (TreeNode node : blockNode.getChildren()) {
				analyzeStatement(node);
			}
		} else {
			throw new ASTFormatException("Called analyzeBlock on non-block node: " + blockNode);
		}
		//display the local scope if scope display is enabled
		if (displayScopes)
			scopes.peek().print();
		//pop the local scope of the block
		scopes.pop();
	}

	/**
	 * @param statementNode
	 * @throws SemanticException
	 *             If a semantic error is found in the sub-AST rooted by
	 *             statementNode: type mismatch, argument number mismatch, break not
	 *             inside loop, variable declared in inner block, a variable or
	 *             function name is redefined in the same scope, undeclared variable
	 *             or function
	 * @throws ASTFormatException
	 *             If the AST given does not conform to the grammar (or my code is
	 *             bugged)
	 */
	private void analyzeStatement(TreeNode statementNode) throws SemanticException, ASTFormatException {
		Scope scope = null;
		//For each statement type, there are different actions to take:
		switch (statementNode.getType()) {
		case block:
			//If it's a block, add a new scope for the block, then analyze the block
			scopes.addFirst(new Scope(ScopeType.block, statementNode.getLoc()));
			analyzeBlock(statementNode);
			break;
		case variabledeclaration:
			//If it's a variable declaration
			//If the declaration is inside the outermost block of a function and if the scope doesn't already have the variable, add the variable to the scope
			//Otherwise throw the appropriate semantic error
			if (scopes.peek().getType() == ScopeType.func) {
				String ID = statementNode.getChild(1).getAttr();
				if (!scopes.peek().contains(ID)) {
					scopes.peek().addVar(ID, getTypeFromNodeType(statementNode.getChild(0).getType()));
					setSigForVar(statementNode, scopes.peek());
				} else {
					throw new SemanticException(SemanticExceptionTypes.DuplicateVariable, ID,
							statementNode.getChild(1).getLoc(),
							"Variable names must be unique from other functions or variables");
				}
			} else {
				throw new SemanticException(SemanticExceptionTypes.VariableDeclarationInInnerBlock,
						statementNode.getChild(0).getType() + " " + statementNode.getChild(1).getAttr(),
						statementNode.getLoc(),
						"Variables must be declared globally or in the outermost block of a function");
			}
			break;
		case statementexpression:
			//If it is a logical or mathematical expression, or a function call, get 
			//the expression value (boolean or int), which is also check the types of
			//all the operators and function call parameters
			getExpressionType(statementNode.getChild(0));
			break;
		case functioninvocation:
			//If it is a function call, checks the function call parameters
			functionInvocation(statementNode);
			break;
		case nullStatement:
			//do not need to check a null statement
			break;
		case IF:
			//If it is an if statement
			//If the conditional of the if statement is a boolean, do nothing, otherwise throw a type mismatch error
			if (getExpressionType(statementNode.getChild(0)) == Type.BOOLEAN) {
			} else {
				throw new SemanticException(SemanticExceptionTypes.TypeMismatch,
						getExpressionName(statementNode.getChild(0)), statementNode.getChild(0).getLoc(),
						"Type boolean was expected");
			}
			//Add a new scope for the if block
			scopes.addFirst(new Scope(ScopeType.block, statementNode.getChild(1).getLoc()));
			//If it's a block, analyze the block (which will pop the scope at the end), if it's a null statement, 
			//just pop the scope. If it's a single statement, analyze that statement and then pop the scope
			if (statementNode.getChild(1).getType() == TreeNodeType.block) {
				analyzeBlock(statementNode.getChild(1));
			} else if (statementNode.getChild(1).getType() == TreeNodeType.nullStatement) {
				scopes.pop();
			} else {
				analyzeStatement(statementNode.getChild(1));
				scopes.pop();
			}

			break;
		case IFELSE:

			//If it's an if-else statement, start the same way as the if statement
			if (getExpressionType(statementNode.getChild(0)) == Type.BOOLEAN) {
			} else {
				throw new SemanticException(SemanticExceptionTypes.TypeMismatch,
						getExpressionName(statementNode.getChild(0)), statementNode.getChild(0).getLoc(),
						"Type boolean was expected");
			}

			scopes.addFirst(new Scope(ScopeType.block, statementNode.getChild(1).getLoc()));
			if (statementNode.getChild(1).getType() == TreeNodeType.block) {
				analyzeBlock(statementNode.getChild(1));
			} else if (statementNode.getChild(1).getType() == TreeNodeType.nullStatement) {
				scopes.pop();
			} else {
				analyzeStatement(statementNode.getChild(1));
				scopes.pop();
			}

			//Same logic for the else block as with the if block
			scopes.addFirst(new Scope(ScopeType.block, statementNode.getChild(2).getLoc()));
			if (statementNode.getChild(2).getType() == TreeNodeType.block) {
				analyzeBlock(statementNode.getChild(2));
			} else if (statementNode.getChild(2).getType() == TreeNodeType.nullStatement) {
				scopes.pop();
			} else {
				analyzeStatement(statementNode.getChild(2));
				scopes.pop();
			}

			break;
		case WHILE:
			//Same logic as the if statement, but the new scope is ScopeType.loop
			if (getExpressionType(statementNode.getChild(0)) == Type.BOOLEAN) {
			} else {
				throw new SemanticException(SemanticExceptionTypes.TypeMismatch,
						getExpressionName(statementNode.getChild(0)), statementNode.getChild(0).getLoc(),
						"Type boolean was expected");
			}
			if (statementNode.getChildren().size() > 1) {
				scopes.addFirst(new Scope(ScopeType.loop, statementNode.getChild(1).getLoc()));
				if (statementNode.getChild(1).getType() == TreeNodeType.block) {
					analyzeBlock(statementNode.getChild(1));
				} else if (statementNode.getChild(1).getType() == TreeNodeType.nullStatement) {
					scopes.pop();
				} else {
					analyzeStatement(statementNode.getChild(1));
					scopes.pop();
				}
			}
			break;
		case RETURN:
			//If it is a return statement
			scope = null;
			//find the outermost scope of the function so that return type can be found
			for (Scope s : scopes) {
				if (s.getType() == ScopeType.func) {
					scope = s;
					break;
				}
			}
			//If the scope was found
			if (scope != null) {
				//If the function return type is non-void, the return argument type should match the function return type
				if (scope.getRetType() != Type.VOID) {
					if (statementNode.getChildren().size() != 0) {
						if (getExpressionType(statementNode.getChild(0)) == scope.getRetType()) {

						} else {
							throw new SemanticException(SemanticExceptionTypes.TypeMismatch,
									getExpressionName(statementNode.getChild(0)), statementNode.getLoc(),
									"Type " + scope.getRetType() + " was expected");
						}
					} else {
						throw new SemanticException(SemanticExceptionTypes.ArgumentMismatch, "return",
								statementNode.getLoc(), "1 argument of type " + scope.getRetType() + " expected");
					}
					//If the function return type is void, there should be no arguments to the return
				} else {
					if (statementNode.getChildren().size() == 0) {

					} else {
						throw new SemanticException(SemanticExceptionTypes.ArgumentMismatch, "return",
								statementNode.getLoc(), "0 arguments expected");
					}
				}
			} else {
				//ASTFormatException because grammar doesn't allow returns outside of a function
				throw new ASTFormatException("return not in a function");
			}
			break;
		case BREAK:
			//Find the innermost while loop scope that the break statement is in
			scope = null;
			for (Scope s : scopes) {
				if (s.getType() == ScopeType.loop) {
					scope = s;
					break;
				}
			}
			//If the scope was found, and the break node doesn't have children, then annotate the 
			//loop scope onto the break node, otherwise throw the appropriate semantic error
			if (scope != null) {
				if (statementNode.getChildren().size() == 0) {
					statementNode.annotate(scope);
				} else {
					throw new SemanticException(SemanticExceptionTypes.ArgumentMismatch, "BREAK",
							statementNode.getLoc(), "0 arguments expected");
				}
			} else {
				throw new SemanticException(SemanticExceptionTypes.BreakNotInLoop, statementNode.getLoc(),
						"Break statements must be inside a while loop");
			}
			break;
		default:
			throw new ASTFormatException("An incorrect child of block was found of type " + statementNode.getType()
					+ " at " + statementNode.getLoc() + "\nnode: " + statementNode.toString());

		}
	}

	/**
	 * Finds the topmost scope in the scope stack which contains the variable ID
	 * specified.
	 * 
	 * @param id
	 *            The ID TreeNode with the name of the scope stack
	 * @return The topmost scope in the scope stack which contains the specified
	 *         variable
	 * @throws SemanticException
	 *             If the variable is not declared
	 * @throws ASTFormatException
	 *             If the AST given does not conform to the grammar (or my code is
	 *             bugged)
	 */
	private Scope findVarInScopeStack(TreeNode id) throws SemanticException, ASTFormatException {
		if (id.getType() == TreeNodeType.ID) {
			String ID = id.getAttr();
			//Go through the scopes from the top
			for (Scope scope : scopes) {
				//If a scope contains the variable, return that scope
				if (scope.containsVar(ID)) {
					return scope;
				}
			}
			throw new SemanticException(SemanticExceptionTypes.UndeclaredVariable, ID, id.getLoc(),
					"Variables must be declared before use");
		} else {
			throw new ASTFormatException("Interpreted a non-ID node " + id + " as a variable");
		}
	}

	/**
	 * Returns the function return type of the specified function invocation node,
	 * based on it's ID. Also checks if the arguments match the declared parameters
	 * of the function
	 * 
	 * @param funcInvoke
	 *            The function invocation TreeNode to get the return type for
	 * @return The return type of the function call
	 * @throws SemanticException
	 *             If the function is undeclared or the arguments are incorrect
	 * @throws ASTFormatException
	 *             If the AST given does not conform to the grammar (or my code is
	 *             bugged)
	 */
	private Type functionInvocation(TreeNode funcInvoke) throws SemanticException, ASTFormatException {
		if (funcInvoke.getType() == TreeNodeType.functioninvocation) {
			String ID = funcInvoke.getChild(0).getAttr();
			ArrayList<Type> argTypes = new ArrayList<Type>();

			//Get the expression type of each of the arguments
			for (TreeNode args : funcInvoke.getChild(1).getChildren()) {
				argTypes.add(getExpressionType(args));

			}
			//Make a string representation of the arguments for error messaging
			StringBuffer paramTypes = new StringBuffer();
			for (Type argType : argTypes) {
				if (paramTypes.length() != 0)
					paramTypes.append(", ");
				paramTypes.append(argType);
			}

			//If the global scope contains the function
			if (gScope.containsFunc(ID)) {
				//If the list of declared parameter types is equal to the argument types of the function call, 
				//annote the function call and return the function return type, otherwise throw an error about
				//how the function call is incorrect
				List<Type> declaredParamTypes = gScope.getFuncInputType(ID);
				if (declaredParamTypes.equals(argTypes)) {
					funcInvoke.getChild(0).annotate(gScope.getFuncReturnType(ID) + " f(" + paramTypes.toString() + ")",
							gScope);
					return gScope.getFuncReturnType(ID);
				} else {
					//Argument mismatch if the sizes are different
					if (declaredParamTypes.size() != argTypes.size()) {
						throw new SemanticException(SemanticExceptionTypes.ArgumentMismatch,
								getExpressionName(funcInvoke), funcInvoke.getLoc(),
								"Number of arguments must match the number of declared parameters of called function");
					} else {
						//Sizes are different, so find which specific type is wrong, and inform user though type
						//mismatch error
						for (int i = 0; i < argTypes.size(); i++) {
							if (declaredParamTypes.get(i) != argTypes.get(i)) {
								throw new SemanticException(SemanticExceptionTypes.TypeMismatch,
										getExpressionName(funcInvoke.getChild(1).getChild(i)),
										funcInvoke.getChild(1).getChild(i).getLoc(),
										"Type " + declaredParamTypes.get(i) + " was expected");
							}
						}
						throw new ASTFormatException("Size and elements of two unequal lists were equal");
					}
				}

				//Otherwise if the global scope contains the function
			} else if (defaultScope.containsFunc(ID)) {
				//If the list of declared parameter types is equal to the argument types of the function call, 
				//annote the function call and return the function return type, otherwise throw an error about
				//how the function call is incorrect
				List<Type> declaredParamTypes = defaultScope.getFuncInputType(ID);
				if (declaredParamTypes.equals(argTypes)) {
					funcInvoke.getChild(0).annotate(
							defaultScope.getFuncReturnType(ID) + " f(" + paramTypes.toString() + ")", defaultScope);
					return defaultScope.getFuncReturnType(ID);
				} else {
					//Argument mismatch if the sizes are different
					if (declaredParamTypes.size() != argTypes.size()) {
						throw new SemanticException(SemanticExceptionTypes.ArgumentMismatch,
								getExpressionName(funcInvoke), funcInvoke.getLoc(),
								"Number of arguments must match the number of declared parameters of called function");
					} else {
						//Sizes are different, so find which specific type is wrong, and inform user though type
						//mismatch error
						for (int i = 0; i < argTypes.size(); i++) {
							if (declaredParamTypes.get(i) != argTypes.get(i)) {
								throw new SemanticException(SemanticExceptionTypes.TypeMismatch,
										getExpressionName(funcInvoke.getChild(1).getChild(i)),
										funcInvoke.getChild(1).getChild(i).getLoc(),
										"Type " + declaredParamTypes.get(i) + " was expected");
							}
						}
						throw new ASTFormatException("Size and elements of two unequal lists were equal");
					}
				}
			} else {
				if(gScope.getMainFuncID().equals(ID)) {
					throw new SemanticException(SemanticExceptionTypes.CallMain, ID, funcInvoke.getLoc(),
							"Cannot call the main function");
				}
				throw new SemanticException(SemanticExceptionTypes.UndeclaredFunction, ID, funcInvoke.getLoc(),
						"Functions must be declared before use");
			}
		} else {
			throw new ASTFormatException("Function invokation called on wrong node " + funcInvoke);
		}
	}

	/**
	 * Returns the type that an expression evaluates to. Also performs type checking
	 * on the expression
	 * 
	 * @param expressionNode
	 *            The expression to evaluate
	 * @return The return type of the expression
	 * @throws SemanticException
	 *             if the types for an operator are incorrect
	 * @throws ASTFormatException
	 *             If the AST given does not conform to the grammar (or my code is
	 *             bugged)
	 */
	private Type getExpressionType(TreeNode expressionNode) throws SemanticException, ASTFormatException {
		Type left;
		Type right;
		switch (expressionNode.getType()) {
		case ID:
			//If it is an ID, look for the type of that variable, annotate the variable, and return the type
			Scope scope = findVarInScopeStack(expressionNode);
			left = scope.getVarType(expressionNode.getAttr());
			expressionNode.annotate(left.toString(), scope);
			return left;
		case NUM:
			//Annotate the expression and return int
			expressionNode.annotate(Type.INT.toString());
			return Type.INT;
		case TRUE:
		case FALSE:
			//Annotate the expression and return boolean
			expressionNode.annotate(Type.BOOLEAN.toString());
			return Type.BOOLEAN;
		case STRING:
			//Annotate the expression and return string
			expressionNode.annotate(Type.STRING.toString());
			return Type.STRING;
		case ASSIGN:

			if (expressionNode.getChild(0).getType() == TreeNodeType.ID) {
				//Check if the right type matches the type of the variable, if they do, annotate the expression 
				//and return the type. Otherwise throw a type mismatch on the right hand side expression
				left = getExpressionType(expressionNode.getChild(0));
				right = getExpressionType(expressionNode.getChild(1));
				if (right == left) {
					expressionNode.annotate(left.toString());
					return left;
				} else {
					throw new SemanticException(SemanticExceptionTypes.TypeMismatch,
							getExpressionName(expressionNode.getChild(1)), expressionNode.getChild(1).getLoc(),
							"Type " + right + " was expected");
				}

			} else {
				//Grammar only allows IDs to the left side of an assignment
				throw new ASTFormatException(
						"Left hand side of assignment must be a variable, at " + expressionNode.getLoc());
			}
		case EQ:
		case NEQ:
			//Check if the right and left type match, if they do, annotate the expression to be boolean
			//and return boolean. Otherwise throw a type mismatch on the right hand side expression
			left = getExpressionType(expressionNode.getChild(0));
			right = getExpressionType(expressionNode.getChild(1));
			if (right == left) {
				expressionNode.annotate(Type.BOOLEAN.toString());
				return Type.BOOLEAN;
			} else {
				throw new SemanticException(SemanticExceptionTypes.TypeMismatch,
						getExpressionName(expressionNode.getChild(1)), expressionNode.getChild(1).getLoc(),
						"Type " + left + " was expected");
			}
		case SUB:
		case MUL:
		case DIV:
		case MOD:
		case ADD:
			//Check if the left and right types are ints. If they are, annotate the expression and return int.
			//Otherwise throw a type mismatch on the side that isn't an int
			left = getExpressionType(expressionNode.getChild(0));
			right = getExpressionType(expressionNode.getChild(1));
			if (right == Type.INT && left == Type.INT) {
				expressionNode.annotate(Type.INT.toString());
				return Type.INT;
			} else if (left != Type.INT) {
				throw new SemanticException(SemanticExceptionTypes.TypeMismatch,
						getExpressionName(expressionNode.getChild(0)), expressionNode.getChild(0).getLoc(),
						"Type int was expected");
			} else {
				throw new SemanticException(SemanticExceptionTypes.TypeMismatch,
						getExpressionName(expressionNode.getChild(1)), expressionNode.getChild(1).getLoc(),
						"Type int was expected");
			}
		case LT:
		case GT:
		case LE:
		case GE:
			//Check if the left and right types are ints. If they are, annotate the expression and return boolean.
			//Otherwise throw a type mismatch on the side that isn't an int
			left = getExpressionType(expressionNode.getChild(0));
			right = getExpressionType(expressionNode.getChild(1));
			if (right == Type.INT && left == Type.INT) {
				expressionNode.annotate(Type.BOOLEAN.toString());
				return Type.BOOLEAN;
			} else if (left != Type.INT) {
				throw new SemanticException(SemanticExceptionTypes.TypeMismatch,
						getExpressionName(expressionNode.getChild(0)), expressionNode.getChild(0).getLoc(),
						"Type int was expected");
			} else {
				throw new SemanticException(SemanticExceptionTypes.TypeMismatch,
						getExpressionName(expressionNode.getChild(1)), expressionNode.getChild(1).getLoc(),
						"Type int was expected");
			}

		case AND:
		case OR:
			//Check if the left and right types are booleans. If they are, annotate the expression and return boolean.
			//Otherwise throw a type mismatch on the side that isn't a boolean
			left = getExpressionType(expressionNode.getChild(0));
			right = getExpressionType(expressionNode.getChild(1));
			if (right == Type.BOOLEAN && left == Type.BOOLEAN) {
				expressionNode.annotate(Type.BOOLEAN.toString());
				return Type.BOOLEAN;
			} else if (left != Type.BOOLEAN) {
				throw new SemanticException(SemanticExceptionTypes.TypeMismatch,
						getExpressionName(expressionNode.getChild(0)), expressionNode.getChild(0).getLoc(),
						"Type boolean was expected");
			} else {
				throw new SemanticException(SemanticExceptionTypes.TypeMismatch,
						getExpressionName(expressionNode.getChild(1)), expressionNode.getChild(1).getLoc(),
						"Type boolean was expected");
			}
		case UNARY_SUB:
			//Check if the right side is an int. If it is, annotate the expression and return int.
			//Otherwise throw a type mismatch on the right side
			right = getExpressionType(expressionNode.getChild(0));
			if (right == Type.INT) {
				expressionNode.annotate(Type.INT.toString());
				return Type.INT;
			} else {
				throw new SemanticException(SemanticExceptionTypes.TypeMismatch,
						getExpressionName(expressionNode.getChild(0)), expressionNode.getChild(0).getLoc(),
						"Type int was expected");
			}
		case NOT:
			//Check if the right side is a boolean. If it is, annotate the expression and return boolean.
			//Otherwise throw a type mismatch on the right side
			right = getExpressionType(expressionNode.getChild(0));
			if (right == Type.BOOLEAN) {
				expressionNode.annotate(Type.BOOLEAN.toString());
				return Type.BOOLEAN;
			} else {
				throw new SemanticException(SemanticExceptionTypes.TypeMismatch,
						getExpressionName(expressionNode.getChild(0)), expressionNode.getChild(0).getLoc(),
						"Type boolean was expected");
			}
		case functioninvocation:
			//Return the type of the function call
			return functionInvocation(expressionNode);
		default:
			throw new ASTFormatException("Unexpected statement expression type " + expressionNode);
		}

	}

	/**
	 * Checks if all non-void functions in the program return a value on all
	 * execution paths
	 * 
	 * @throws SemanticException
	 *             If a non-void function doesn't always return a value
	 * @throws ASTFormatException
	 *             If the AST given does not conform to the grammar (or my code is
	 *             bugged)
	 */
	private void pass2() throws SemanticException, ASTFormatException {
		for (TreeNode node : tree.root.getChildren()) {
			switch (node.getType()) {
			case functiondeclaration:
			case mainfunctiondeclaration:
				//Only need to check for non-void functions
				if (node.getChild(0).getType() != TreeNodeType.VOID) {
					//If the function block does not always return, throw a funciton does not return error
					if (checkReturnBlock(node.getChild(3)) != ReturnTypes.AlwaysReturns) {
						throw new SemanticException(SemanticExceptionTypes.FunctionDoesNotReturn,
								node.getChild(1).getAttr(), node.getLoc(),
								node.getChild(0).getType().toString() + " functions must return a value");
					}
				}
				break;
			case variabledeclaration:
				break;
			default:
				throw new ASTFormatException("An incorrect child of globaldeclarations was found of type " + node.type);
			}
		}
	}

	/**
	 * Checks the return type of the block rooted by the given node
	 * 
	 * @param node
	 *            The block node which contains the block statements
	 * @return The return type of the block. ReturnTypes.AlwaysReturns if the block
	 *         always returns, ReturnTypes.AlwaysBreaks, if the block always breaks,
	 *         and ReturnTypes.DoesNotAlwaysReturn by default
	 * @throws SemanticException
	 * @throws ASTFormatException
	 *             If the AST given does not conform to the grammar (or my code is
	 *             bugged)
	 */
	private ReturnTypes checkReturnBlock(TreeNode node) throws SemanticException, ASTFormatException {
		//If the node is a block
		if (node.getType() == TreeNodeType.block) {
			//If one of the statements of the block always break or always return, then this block always breaks or always returns, respectively
			for (TreeNode expressionNode : node.getChildren()) {
				ReturnTypes ret = checkReturnStatement(expressionNode);
				if (ret != ReturnTypes.DoesNotAlwaysReturn) {
					return ret;
				}
			}
			//Otherwise the block does not always return
			return ReturnTypes.DoesNotAlwaysReturn;
		} else {
			//Otherwise it's a single statement, return the return type of the single statement
			return checkReturnStatement(node);

		}
	}

	/**
	 * * Checks the return type of statement represented by the given node
	 * 
	 * @param expressionNode
	 *            The statement node
	 * @return The return type of the statement. ReturnTypes.AlwaysReturns if the
	 *         block always returns, ReturnTypes.AlwaysBreaks, if the block always
	 *         breaks, and ReturnTypes.DoesNotAlwaysReturn by default
	 * @throws SemanticException
	 * @throws ASTFormatException
	 *             If the AST given does not conform to the grammar (or my code is
	 *             bugged)
	 */
	private ReturnTypes checkReturnStatement(TreeNode expressionNode) throws SemanticException, ASTFormatException {
		switch (expressionNode.getType()) {
		case block:
			//If the statement is a block, return the return type of the block
			ReturnTypes ret = checkReturnBlock(expressionNode);
			return ret;
		case RETURN:
			//If the statement is a return, the statement always returns
			return ReturnTypes.AlwaysReturns;
		case IFELSE:
			//If the statement is an ifelse, and both branches always return, then the ifelse always returns
			//If both branches always break, then the ifelse always breaks
			ReturnTypes ret1 = checkReturnBlock(expressionNode.getChild(1));
			ReturnTypes ret2 = checkReturnBlock(expressionNode.getChild(2));
			if (ret1 == ReturnTypes.AlwaysReturns && ret2 == ReturnTypes.AlwaysReturns) {
				return ReturnTypes.AlwaysReturns;
			} else if (ret1 == ReturnTypes.AlwaysBreaks && ret2 == ReturnTypes.AlwaysBreaks) {
				return ReturnTypes.AlwaysBreaks;
			}
			break;
		case BREAK:
			//If the statement is a break, the statement always breaks
			return ReturnTypes.AlwaysBreaks;
		case WHILE:
		case variabledeclaration:
		case statementexpression:
		case nullStatement:
		case IF:
			//For these given statements, the behaviour is not always a break or a return, so it goes to default return
			break;
		case functioninvocation:
			//Extra code so that having a function call the default halt() instead is also an alternative to having to return a value
			String ID = expressionNode.getChild(0).getAttr();
			if (ID.equals("halt")) {
				for (Scope scope : scopes) {
					//If the halt() function is found in a non-default scope, then it doesn't halt the program
					if (scope != defaultScope && scope.contains(ID)) {
						break;
						//Otherwise if the halt() function is found in the default scope, the it always halts the program
					} else if (scope == defaultScope && scope.contains(ID)) {
						return ReturnTypes.AlwaysReturns;
					}
				}
			}
			break;
		default:
			throw new ASTFormatException("An incorrect child of block was found of type " + expressionNode.getType()
					+ " at " + expressionNode.getLoc() + "\nnode: " + expressionNode.toString());

		}
		//The default is does not always return
		return ReturnTypes.DoesNotAlwaysReturn;
	}

	/**
	 * The three different return behaviors
	 * 
	 * @author charl
	 */
	private enum ReturnTypes {
		DoesNotAlwaysReturn, AlwaysReturns, AlwaysBreaks
	}

	/**
	 * Returns the string representation of the expression to aid error messaging
	 * 
	 * @param expressionNode
	 *            The node for which to get a string representation of
	 * @return A string representation of the node
	 * @throws ASTFormatException
	 *             If the AST given does not conform to the grammar (or my code is
	 *             bugged)
	 */
	private String getExpressionName(TreeNode expressionNode) throws ASTFormatException {
		switch (expressionNode.getType()) {
		case ID:
		case STRING:
		case NUM:
			//Return the attribute
			return expressionNode.getAttr();
		case BOOL:
		case TRUE:
		case FALSE:
			//return the literal
			return expressionNode.getType().toString();
		case functioninvocation:
			//Make a string representation of the arguments for error messaging
			StringBuffer paramNames = new StringBuffer();
			for (TreeNode args : expressionNode.getChild(1).getChildren()) {
				if (paramNames.length() != 0)
					paramNames.append(", ");
				paramNames.append(getExpressionName(args));

			}

			//return the id and the arguments of the function call
			return expressionNode.getChild(0).getAttr() + "(" + paramNames + ")";

		case ASSIGN:
		case EQ:
		case NEQ:
		case SUB:
		case MUL:
		case DIV:
		case MOD:
		case ADD:
		case LT:
		case GT:
		case LE:
		case GE:
		case AND:
		case OR:
			//return the left of the operator, the operator, the right of the operator
			return getExpressionName(expressionNode.getChild(0)) + " " + expressionNode.getType() + " "
					+ getExpressionName(expressionNode.getChild(1));

		case UNARY_SUB:
		case NOT:
			//return the operator and the right of the operator
			return expressionNode.getType() + getExpressionName(expressionNode.getChild(0));
		default:
			throw new ASTFormatException("Unexpected statement expression type " + expressionNode);
		}

	}

	/**
	 * Convert the NodeTypes which represent Types to Types
	 * 
	 * @param nt
	 *            The nodeType which represents a Type
	 * @return The Type of nt
	 * @throws ASTFormatException
	 *             If the AST given does not conform to the grammar (or my code is
	 *             bugged)
	 */
	private Type getTypeFromNodeType(TreeNodeType nt) throws ASTFormatException {
		switch (nt) {
		case INT:
			return Type.INT;

		case VOID:
			return Type.VOID;

		case BOOL:
		case TRUE:
		case FALSE:
			return Type.BOOLEAN;

		default:
			throw new ASTFormatException("Attempted to get type from a non-type node");
		}
	}

	/**
	 * Annotates the variable with it's ID and scope
	 * 
	 * @param node
	 *            The variable node to annotate
	 * @param scope
	 *            The scope to annotate onto the node
	 */
	private void setSigForVar(TreeNode node, Scope scope) {
		node.getChild(1).annotate(node.getChild(0).getType().toString(), scope);
	}
}
