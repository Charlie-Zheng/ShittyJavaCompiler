import java.util.Stack;

/**
 * 
 */

/**
 * @author charl
 */
public class CodeGen {
	/**
	 * The AST that the generator will use to generator WebAssembly text files.
	 * Should be annotated
	 */
	private Tree tree;
	private final boolean debug = true;
	private int tabs = 0;
	private StringBuffer code = new StringBuffer();
	private StringBuffer output = new StringBuffer();

	private StringBuffer stringsToPrint = new StringBuffer();
	private int stringOffset;
	private String nameOfDefaultScope;
	/**
	 * Tracks how many blocks have been used before. Use to find what number the
	 * next block should be.
	 */
	private int blockNum;
	/**
	 * Name for the temporary variable used to short circuiting boolean operations
	 * || and &&
	 */
	private final String boolTemp = "$boolTemp";

	/**
	 * Tracks what the current loop to break out of is
	 */
	private Stack<Integer> loopStack = new Stack<Integer>();

	/**
	 * Creates a new code generator with the given tree and nameOfDefaultScope
	 * 
	 * @param tree
	 * @param nameOfDefaultScope
	 */
	public CodeGen(Tree tree, String nameOfDefaultScope) {
		this.tree = tree;
		this.nameOfDefaultScope = nameOfDefaultScope;

	}

	/**
	 * Generate compiled code. This is output to System.out
	 */
	public void generate() {
		openStructOutput("module");

		runTime();
		genAST();
		genStrings();

		closeStructOutput();
	}

	/**
	 * Generate code based on the AST
	 */
	private void genAST() {
		for (TreeNode node : tree.root.getChildren()) {
			switch (node.getType()) {
			case mainfunctiondeclaration:
				//Rename the function to $main
				openStructOutput("func $main");
				// Generate the code for the function
				genFunc(node);
				closeStructOutput();
				//start the main program
				printLineTabbedOutput("(start $main)");
				break;
			case functiondeclaration:
				//Find the parameters of the function
				StringBuffer params = new StringBuffer(" ");
				for (TreeNode param : node.getChild(2).getChildren()) {
					params.append("(param " + getNameOf(param.getChild(1)) + " i32) ");
				}
				//Find the return type of the function
				StringBuffer ret = new StringBuffer();
				if (node.getChild(0).getType() != TreeNodeType.VOID) {
					ret.append("(result i32)");
				}
				//Create a function with the specified signature
				openStructOutput("func " + getNameOf(node.getChild(1)) + params.toString() + ret.toString());

				genFunc(node);

				closeStructOutput();
				break;
			case variabledeclaration:
				//Create a local variable
				printLineTabbedOutput("(global " + getNameOf(node.getChild(1)) + " (mut i32) (i32.const 0))");
				break;
			}
		}
	}

	/**
	 * Creates an S-expression in the output. Increases the tabs to print by 1.
	 * 
	 * @param struct
	 *            The name of the S-expression
	 */
	private void openStructOutput(String struct) {
		printLineTabbedOutput("(" + struct);
		tabs++;
	}

	/**
	 * Closes an S-expression in the output. Decreases the tabs to print by 1.
	 */
	private void closeStructOutput() {
		tabs--;
		printLineTabbedOutput(")");

	}

	/**
	 * Opens an S-expression in the code. Increases the tabs to print by 1. Prints
	 * the concatenation of "(" and the string given, and creates a new line
	 * 
	 * @param struct
	 *            The name of the S-expression
	 */
	private void openStructCode(String struct) {
		printLineTabbedCode("(" + struct);
		tabs++;
	}

	/**
	 * Closes an S-expression in the code. Decreases the tabs to print by 1.
	 */
	private void closeStructCode() {
		tabs--;
		printLineTabbedCode(")");

	}

	/**
	 * Adds the code to add strings into the linear memory to output
	 */
	private void genStrings() {
		output.append(stringsToPrint.toString());
		printLineTabbedOutput("(memory " + ((stringOffset + 65535) / 65536) + ")");
	}

	/**
	 * Generate code for a function declaration
	 * 
	 * @param node
	 */
	private void genFunc(TreeNode node) {
		TreeNode block = node.getChild(3);
		printLineTabbedOutput("(local " + boolTemp + " i32)"); //Every function has a temporary storage to use for short circuiting && and || operators
		genBlockOrStatement(block);
		printCodeToOutput();
	}

	/**
	 * Generate code to execute the given statement or block
	 * 
	 * @param node
	 */
	private void genBlockOrStatement(TreeNode node) {

		switch (node.getType()) {
		case block:
			for (TreeNode n : node.getChildren()) {
				genBlockOrStatement(n);
			}
			break;
		case variabledeclaration:
			printLineTabbedOutput("(local " + getNameOf(node.getChild(1)) + " i32)");
			break;
		case statementexpression:
			genExpression(node.getChild(0));
			printLineTabbedCode("drop"); //need to drop the result of the expression out of the stack, since all expressions evaluate to a value. This is the top level call of the expression
			break;
		case functioninvocation:
			funcInvoke(node);
			if (!node.getChild(0).sig.startsWith("void")) { //need to drop a value out of the stack if the function returned something, as this is a top level call
				printLineTabbedCode("drop");
			}
			break;
		case nullStatement:
			//do not need to do anything
			break;
		case IF:
			genExpression(node.getChild(0));
			openStructCode("if");

			openStructCode("then");
			genBlockOrStatement(node.getChild(1));
			closeStructCode();

			closeStructCode();
			break;
		case IFELSE:
			genExpression(node.getChild(0));

			openStructCode("if");

			openStructCode("then");
			genBlockOrStatement(node.getChild(1));
			closeStructCode();

			openStructCode("else");
			genBlockOrStatement(node.getChild(2));
			closeStructCode();

			closeStructCode();
			break;
		case WHILE:
			int num = blockNum;
			loopStack.add(num);
			openStructCode("block $B" + num);
			genExpression(node.getChild(0));
			printLineTabbedCode("i32.eqz");
			printLineTabbedCode("br_if $B" + num);

			openStructCode("loop $L" + num);
			blockNum++;

			genBlockOrStatement(node.getChild(1));

			genExpression(node.getChild(0));
			printLineTabbedCode("br_if $L" + num);

			closeStructCode();
			closeStructCode();
			loopStack.pop();
			break;
		case RETURN:
			if (node.getChildren().size() > 0) {
				genExpression(node.getChild(0));
			}
			printLineTabbedCode("return");

			break;
		case BREAK:
			printLineTabbedCode("br $B" + loopStack.peek());
			break;

		}
	}

	/**
	 * Generate code to calculate the expression, then append the original
	 * expression text to the end, if debugging is enabled
	 * 
	 * @param expressionNode
	 *            The node that contains an expression
	 */
	private void genExpression(TreeNode expressionNode) {
		if (debug) {
			printLineTabbedCode("");
		}
		genExpressionNoDebug(expressionNode);
		if (debug) {
			printLineTabbedCode(";; " + getExpressionName(expressionNode));
			printLineTabbedCode("");
		}
	}

	/**
	 * Generate code to calculate the expression
	 * 
	 * @param expressionNode
	 *            The node that contains an expression
	 */
	private void genExpressionNoDebug(TreeNode expressionNode) {
		switch (expressionNode.getType()) {
		case ID:
			if (expressionNode.getScope() instanceof GlobalScope) {
				printLineTabbedCode("global.get " + getNameOf(expressionNode));
			} else {
				printLineTabbedCode("local.get " + getNameOf(expressionNode));
			}
			break;
		case NUM:
			printLineTabbedCode("i32.const " + expressionNode.getAttr());
			break;
		case TRUE:
			printLineTabbedCode("i32.const 1");
			break;
		case FALSE:
			printLineTabbedCode("i32.const 0");
			break;
		case STRING:
			break;
		case ASSIGN:
			genExpressionNoDebug(expressionNode.getChild(1));
			if (expressionNode.getChild(0).getScope() instanceof GlobalScope) {
				printLineTabbedCode("global.set " + getNameOf(expressionNode.getChild(0)));
				printLineTabbedCode("global.get " + getNameOf(expressionNode.getChild(0))); //The assign operator still evaluates to a value
			} else {
				printLineTabbedCode("local.set " + getNameOf(expressionNode.getChild(0)));
				printLineTabbedCode("local.get " + getNameOf(expressionNode.getChild(0))); //The assign operator still evaluates to a value
			}
			break;
		case EQ:
			genExpressionNoDebug(expressionNode.getChild(0));
			genExpressionNoDebug(expressionNode.getChild(1));
			printLineTabbedCode("i32.eq");
			break;
		case NEQ:
			genExpressionNoDebug(expressionNode.getChild(0));
			genExpressionNoDebug(expressionNode.getChild(1));
			printLineTabbedCode("i32.ne");
			break;
		case LT:
			genExpressionNoDebug(expressionNode.getChild(0));
			genExpressionNoDebug(expressionNode.getChild(1));
			printLineTabbedCode("i32.lt_s");
			break;
		case GT:
			genExpressionNoDebug(expressionNode.getChild(0));
			genExpressionNoDebug(expressionNode.getChild(1));
			printLineTabbedCode("i32.gt_s");
			break;
		case LE:
			genExpressionNoDebug(expressionNode.getChild(0));
			genExpressionNoDebug(expressionNode.getChild(1));
			printLineTabbedCode("i32.le_s");
			break;
		case GE:
			genExpressionNoDebug(expressionNode.getChild(0));
			genExpressionNoDebug(expressionNode.getChild(1));
			printLineTabbedCode("i32.ge_s");
			break;
		case SUB:
			genExpressionNoDebug(expressionNode.getChild(0));
			genExpressionNoDebug(expressionNode.getChild(1));
			printLineTabbedCode("i32.sub");
			break;
		case MUL:
			genExpressionNoDebug(expressionNode.getChild(0));
			genExpressionNoDebug(expressionNode.getChild(1));
			printLineTabbedCode("i32.mul");
			break;
		case DIV:
			genExpressionNoDebug(expressionNode.getChild(0));
			genExpressionNoDebug(expressionNode.getChild(1));
			printLineTabbedCode("i32.div_s");
			break;
		case MOD:
			genExpressionNoDebug(expressionNode.getChild(0));
			genExpressionNoDebug(expressionNode.getChild(1));
			printLineTabbedCode("i32.rem_s");
			break;
		case ADD:
			genExpressionNoDebug(expressionNode.getChild(0));
			genExpressionNoDebug(expressionNode.getChild(1));
			printLineTabbedCode("i32.add");
			break;
		case AND:
			//Convert to shortcircuiting:
			//			If(a){
			//				return b
			//			}else {
			//				return false;
			//			}
			genExpressionNoDebug(expressionNode.getChild(0));

			openStructCode("if");

			openStructCode("then");
			genExpressionNoDebug(expressionNode.getChild(1));
			printLineTabbedCode("local.set " + boolTemp);
			closeStructCode();

			openStructCode("else");
			printLineTabbedCode("i32.const 0");
			printLineTabbedCode("local.set " + boolTemp);
			closeStructCode();

			closeStructCode();

			printLineTabbedCode("local.get " + boolTemp);
			break;
		case OR:
			//Convert to shortcircuiting:
			//			If(a){
			//				return true
			//			}else {
			//				return b;
			//			}
			genExpressionNoDebug(expressionNode.getChild(0));
			openStructCode("if");

			openStructCode("then");
			printLineTabbedCode("i32.const 1");
			printLineTabbedCode("local.set " + boolTemp);
			closeStructCode();

			openStructCode("else");
			genExpressionNoDebug(expressionNode.getChild(1));
			printLineTabbedCode("local.set " + boolTemp);
			closeStructCode();

			closeStructCode();
			printLineTabbedCode("local.get " + boolTemp);
			break;
		case UNARY_SUB:
			printLineTabbedCode("i32.const 0");
			genExpressionNoDebug(expressionNode.getChild(0));
			printLineTabbedCode("i32.sub");
			break;
		case NOT:
			genExpressionNoDebug(expressionNode.getChild(0));
			printLineTabbedCode("i32.eqz");//  0 eqz = 1, 1 eqz = 0
			break;
		case functioninvocation:
			funcInvoke(expressionNode);
			break;
		default:

		}
	}

	/**
	 * Generate code for the function call represented by the input node
	 * 
	 * @param node
	 */
	private void funcInvoke(TreeNode funcNode) {
		GlobalScope gScope = (GlobalScope) funcNode.getChild(0).getScope();
		if (gScope.getFuncInputType(funcNode.getChild(0).getAttr()).contains(Type.STRING)) {//Only the special runtime function prints() can have a string as the argument
			String arg = funcNode.getChild(1).getChild(0).getAttr();
			printLineTabbedCode("i32.const " + stringOffset);
			printLineTabbedCode("i32.const " + stringLength(arg));
			printLineTabbedCode("call " + getNameOf(funcNode.getChild(0)));
			addString(arg);
		} else {
			TreeNode argList = funcNode.getChild(1);
			for (TreeNode argument : argList.getChildren()) {
				genExpression(argument);
			}
			printLineTabbedCode("call " + getNameOf(funcNode.getChild(0)));
		}
		if (getNameOf(funcNode.getChild(0)).equals("$" + nameOfDefaultScope + "halt")) {
			printLineTabbedCode("unreachable");
		}
	}

	/**
	 * Returns the name of that should be used to access the variable represented by
	 * the ID.
	 * 
	 * @param IDnode
	 * @return
	 */
	private String getNameOf(TreeNode IDnode) {
		return "$" + IDnode.getScope().getName() + IDnode.getAttr();
	}

	/**
	 * Calculates the length of a interpreted string. E.g. The string s = "\n" has
	 * length 1.
	 * 
	 * @param s
	 *            The string to find the length for
	 * @return The length of the string
	 */
	private int stringLength(String s) {
		if (s.length() > 0) {
			char last = s.charAt(0);
			int length = 1;
			for (int i = 1; i < s.length(); i++) {
				if (last == '\\') {
					last = 'a';
				} else {
					length++;
					last = s.charAt(i);
				}

			}
			return length;
		} else {
			return 0;
		}
	}

	/**
	 * Print the compiled code.
	 */
	public void fprint() {
		System.out.println(output);
	}

	/**
	 * Appends the content of code into the output, then clears code
	 */
	private void printCodeToOutput() {
		output.append(code.toString());
		code = new StringBuffer();
	}

	/**
	 * Prints the given string into the code with preceding tabs, and ends with a
	 * newline.
	 * 
	 * @param s
	 *            The string to print into code
	 */
	private void printLineTabbedCode(String s) {
		for (int i = 0; i < tabs; i++) {
			code.append('\t');
		}
		code.append(s);
		code.append('\n');
	}

	/**
	 * Prints the given string into the output with preceding tabs, and ends with a
	 * newline.
	 * 
	 * @param s
	 *            The string to print into output
	 */
	private void printLineTabbedOutput(String s) {
		for (int i = 0; i < tabs; i++) {
			output.append('\t');
		}
		output.append(s);
		output.append("\r\n");
	}

	/**
	 * Adds the given string to the strings to print. Increments stringOffset by
	 * stringLength(add)
	 * 
	 * @param add
	 *            The string to be added to the linear memory
	 */
	private void addString(String add) {
		//Need to reduce size for escaped characters
		StringBuffer newFormat = new StringBuffer();
		if (add.length() > 0) {
			char last = add.charAt(0);
			if (last == '\\') {
				newFormat.append('\\');
			} else {
				newFormat.append("\\" + Integer.toHexString(last / 16)
						+ Integer.toHexString(last % 16));
			}
			for (int i = 1; i < add.length(); i++) {
				if (last == '\\') {
					switch (add.charAt(i)) {
					case 'b':
						newFormat.append("08");
						break;
					case 'f':
						newFormat.append("0c");
						break;
					case 'r':
						newFormat.append("0d");
						break;
					default:
						newFormat.append(add.charAt(i));
					}

					last = 'a';
				} else {
					last = add.charAt(i);
					if (last == '\\') {
						newFormat.append('\\');
					} else {
						newFormat.append("\\" + Integer.toHexString(last / 16)
								+ Integer.toHexString(last % 16));
					}
				}

			}
		}
		stringsToPrint.append("\t(data 0 (i32.const " + stringOffset + ") \"" + newFormat + "\")\r\n");
		stringOffset += stringLength(add);
	}

	/**
	 * Returns the string representation of the expression to aid debugging
	 * 
	 * @param expressionNode
	 *            The node for which to get a string representation of
	 * @return A string representation of the node
	 */
	private String getExpressionName(TreeNode expressionNode) {
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
			return "";
		}

	}

	/**
	 * Appends the runtime code to the output
	 */
	private void runTime() {
		// printi(int) coded in J-- and compiled, then added to runtime

		//		 void printi(int x){
		//				int div;
		//				div = 1;
		//				if(x == 0){
		//					printc(48);		
		//				}else if(x < 0) {
		//					printc(45); //the negative sign
		//					//First finds how many digits there are to print
		//					while(div<=-(x/10)) {
		//						div = div*10;
		//					}
		//					//Then prints that many digits
		//					printc(-(x/div)+48);
		//					while(div>1) {
		//						printc((-(x%div))/(div/10)+48);
		//						div = div/10;
		//					}
		//					
		//				}else{
		//					//Same logic as the negative
		//					while(div<=x/10) {
		//						div = div*10;
		//					}
		//					printc(x/div+48);
		//					while(div>1) {
		//						printc((x%div)/(div/10)+48);
		//						div = div/10;
		//					}
		//				}
		//			}
		output.append("	(import \"host\" \"exit\" (func $" + nameOfDefaultScope + "halt))\r\n" +
				"	(import \"host\" \"getchar\" (func $" + nameOfDefaultScope + "getchar (result i32)))\r\n" +
				"	(import \"host\" \"putchar\" (func $" + nameOfDefaultScope + "printc (param i32)))\r\n" +
				"	(func $" + nameOfDefaultScope + "prints (param $start i32) (param $length i32)\r\n" +
				"		(local $end i32)\r\n" +
				"		local.get $start\r\n" +
				"		local.get $length\r\n" +
				"		i32.add\r\n" +
				"		local.set $end\r\n" +
				"		(block $b0\r\n" +
				"			(loop $l0\r\n" +
				"				local.get $start\r\n" +
				"				local.get $end\r\n" +
				"				i32.ge_s\r\n" +
				"				(br_if $b0)\r\n" +
				"				\r\n" +
				"				local.get $start\r\n" +
				"				i32.load\r\n" +
				"				call $" + nameOfDefaultScope + "printc\r\n" +
				"				\r\n" +
				"				local.get $start\r\n" +
				"				i32.const 1\r\n" +
				"				i32.add\r\n" +
				"				local.set $start\r\n" +
				"				(br $l0)\r\n" +
				"			)\r\n" +
				"		)\r\n" +
				"	)\r\n" +
				"	(func $" + nameOfDefaultScope + "printi (param $Lx i32) \r\n" +
				"		(local $boolTemp i32)\r\n" +
				"		(local $Ldiv i32)\r\n" +
				"		\r\n" +
				"		i32.const 1\r\n" +
				"		local.set $Ldiv\r\n" +
				"		local.get $Ldiv\r\n" +
				"		;; div = 1\r\n" +
				"		\r\n" +
				"		drop\r\n" +
				"		\r\n" +
				"		local.get $Lx\r\n" +
				"		i32.const 0\r\n" +
				"		i32.eq\r\n" +
				"		;; x == 0\r\n" +
				"		\r\n" +
				"		(if\r\n" +
				"			(then\r\n" +
				"				\r\n" +
				"				i32.const 48\r\n" +
				"				;; 48\r\n" +
				"				\r\n" +
				"				call $" + nameOfDefaultScope + "printc\r\n" +
				"			)\r\n" +
				"			(else\r\n" +
				"				\r\n" +
				"				local.get $Lx\r\n" +
				"				i32.const 0\r\n" +
				"				i32.lt_s\r\n" +
				"				;; x < 0\r\n" +
				"				\r\n" +
				"				(if\r\n" +
				"					(then\r\n" +
				"						\r\n" +
				"						i32.const 45\r\n" +
				"						;; 45\r\n" +
				"						\r\n" +
				"						call $" + nameOfDefaultScope + "printc\r\n" +
				"						(block $B0\r\n" +
				"							(loop $L0\r\n" +
				"								\r\n" +
				"								local.get $Ldiv\r\n" +
				"								i32.const 0\r\n" +
				"								local.get $Lx\r\n" +
				"								i32.const 10\r\n" +
				"								i32.div_s\r\n" +
				"								i32.sub\r\n" +
				"								i32.le_s\r\n" +
				"								;; div <= -x / 10\r\n" +
				"								\r\n" +
				"								i32.eqz\r\n" +
				"								br_if $B0\r\n" +
				"								\r\n" +
				"								local.get $Ldiv\r\n" +
				"								i32.const 10\r\n" +
				"								i32.mul\r\n" +
				"								local.set $Ldiv\r\n" +
				"								local.get $Ldiv\r\n" +
				"								;; div = div * 10\r\n" +
				"								\r\n" +
				"								drop\r\n" +
				"								\r\n" +
				"								local.get $Ldiv\r\n" +
				"								i32.const 0\r\n" +
				"								local.get $Lx\r\n" +
				"								i32.const 10\r\n" +
				"								i32.div_s\r\n" +
				"								i32.sub\r\n" +
				"								i32.le_s\r\n" +
				"								;; div <= -x / 10\r\n" +
				"								\r\n" +
				"								br_if $L0\r\n" +
				"							)\r\n" +
				"						)\r\n" +
				"						\r\n" +
				"						i32.const 0\r\n" +
				"						local.get $Lx\r\n" +
				"						local.get $Ldiv\r\n" +
				"						i32.div_s\r\n" +
				"						i32.sub\r\n" +
				"						i32.const 48\r\n" +
				"						i32.add\r\n" +
				"						;; -x / div + 48\r\n" +
				"						\r\n" +
				"						call $" + nameOfDefaultScope + "printc\r\n" +
				"						(block $B1\r\n" +
				"							(loop $L1\r\n" +
				"								\r\n" +
				"								local.get $Ldiv\r\n" +
				"								i32.const 1\r\n" +
				"								i32.gt_s\r\n" +
				"								;; div > 1\r\n" +
				"								\r\n" +
				"								i32.eqz\r\n" +
				"								br_if $B1\r\n" +
				"								\r\n" +
				"								i32.const 0\r\n" +
				"								local.get $Lx\r\n" +
				"								local.get $Ldiv\r\n" +
				"								i32.rem_s\r\n" +
				"								i32.sub\r\n" +
				"								local.get $Ldiv\r\n" +
				"								i32.const 10\r\n" +
				"								i32.div_s\r\n" +
				"								i32.div_s\r\n" +
				"								i32.const 48\r\n" +
				"								i32.add\r\n" +
				"								;; -x % div / div / 10 + 48\r\n" +
				"								\r\n" +
				"								call $" + nameOfDefaultScope + "printc\r\n" +
				"								\r\n" +
				"								local.get $Ldiv\r\n" +
				"								i32.const 10\r\n" +
				"								i32.div_s\r\n" +
				"								local.set $Ldiv\r\n" +
				"								local.get $Ldiv\r\n" +
				"								;; div = div / 10\r\n" +
				"								\r\n" +
				"								drop\r\n" +
				"								\r\n" +
				"								local.get $Ldiv\r\n" +
				"								i32.const 1\r\n" +
				"								i32.gt_s\r\n" +
				"								;; div > 1\r\n" +
				"								\r\n" +
				"								br_if $L1\r\n" +
				"							)\r\n" +
				"						)\r\n" +
				"					)\r\n" +
				"					(else\r\n" +
				"						(block $B2\r\n" +
				"							(loop $L2\r\n" +
				"								\r\n" +
				"								local.get $Ldiv\r\n" +
				"								local.get $Lx\r\n" +
				"								i32.const 10\r\n" +
				"								i32.div_s\r\n" +
				"								i32.le_s\r\n" +
				"								\r\n" +
				"								i32.eqz\r\n" +
				"								br_if $B2\r\n" +
				"								\r\n" +
				"								local.get $Ldiv\r\n" +
				"								i32.const 10\r\n" +
				"								i32.mul\r\n" +
				"								local.set $Ldiv\r\n" +
				"								local.get $Ldiv\r\n" +
				"								;; div = div * 10\r\n" +
				"								\r\n" +
				"								drop\r\n" +
				"								\r\n" +
				"								local.get $Ldiv\r\n" +
				"								local.get $Lx\r\n" +
				"								i32.const 10\r\n" +
				"								i32.div_s\r\n" +
				"								i32.le_s\r\n" +
				"								\r\n" +
				"								br_if $L2\r\n" +
				"							)\r\n" +
				"						)\r\n" +
				"						\r\n" +
				"						local.get $Lx\r\n" +
				"						local.get $Ldiv\r\n" +
				"						i32.div_s\r\n" +
				"						i32.const 48\r\n" +
				"						i32.add\r\n" +
				"						\r\n" +
				"						call $" + nameOfDefaultScope + "printc\r\n" +
				"						(block $B3\r\n" +
				"							(loop $L3\r\n" +
				"								\r\n" +
				"								local.get $Ldiv\r\n" +
				"								i32.const 1\r\n" +
				"								i32.gt_s\r\n" +
				"								;; div > 1\r\n" +
				"								\r\n" +
				"								i32.eqz\r\n" +
				"								br_if $B3\r\n" +
				"								\r\n" +
				"								local.get $Lx\r\n" +
				"								local.get $Ldiv\r\n" +
				"								i32.rem_s\r\n" +
				"								local.get $Ldiv\r\n" +
				"								i32.const 10\r\n" +
				"								i32.div_s\r\n" +
				"								i32.div_s\r\n" +
				"								i32.const 48\r\n" +
				"								i32.add\r\n" +
				"								;; x % div / div / 10 + 48\r\n" +
				"								\r\n" +
				"								call $" + nameOfDefaultScope + "printc\r\n" +
				"								\r\n" +
				"								local.get $Ldiv\r\n" +
				"								i32.const 10\r\n" +
				"								i32.div_s\r\n" +
				"								local.set $Ldiv\r\n" +
				"								local.get $Ldiv\r\n" +
				"								;; div = div / 10\r\n" +
				"								\r\n" +
				"								drop\r\n" +
				"								\r\n" +
				"								local.get $Ldiv\r\n" +
				"								i32.const 1\r\n" +
				"								i32.gt_s\r\n" +
				"								;; div > 1\r\n" +
				"								\r\n" +
				"								br_if $L3\r\n" +
				"							)\r\n" +
				"						)\r\n" +
				"					)\r\n" +
				"				)\r\n" +
				"			)\r\n" +
				"		)\r\n" +
				"	)\r\n" +
				"	\r\n" +
				"	(func $" + nameOfDefaultScope + "printb (param $b i32) \r\n" +
				"		local.get $b\r\n" +
				"		(if\r\n" +
				"			(then\r\n" +
				"				i32.const 0\r\n" +
				"				i32.const 4\r\n" +
				"				call $" + nameOfDefaultScope + "prints\r\n" +
				"			)\r\n" +
				"			(else\r\n" +
				"				i32.const 4\r\n" +
				"				i32.const 5\r\n" +
				"				call $" + nameOfDefaultScope + "prints\r\n" +
				"			)\r\n" +
				"		)\r\n" +
				"	)");
		addString("true");
		addString("false");
	}
}
