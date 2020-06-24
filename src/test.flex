import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Reader;
import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.Symbol;

%%

%public

//Name the scanner class Scan
%class Scan

%cup

//%type Token

//Enable line and column counting
%line
%column
%unicode

//User specified code
%{
	private static int warningCount = 0;
	static ComplexSymbolFactory symbolFactory = new ComplexSymbolFactory();
	static String inputFileName = "";

	public static void main(String[] args) {
		Scan scanner;
		//If there is a single arguement specified
		if (args.length == 1) {
			inputFileName = args[0];
			FileInputStream input = null;
			//Open the file specified. If the file was not found, print the error and exit
			try {
				input = new FileInputStream(new File(inputFileName));
			} catch (FileNotFoundException e1) {
				error("The file with filepath " + inputFileName
						+ " was not found. Enclose with \'\' or \"\" if filename has spaces");
			}
			//set up the Reader and Scan classes for the lexer
			Reader reader = new java.io.InputStreamReader(input);
			scanner = new Scan(reader);
			parser p = new parser(scanner, symbolFactory);
			//If the scanner is not at the end of the file, scan the tokens and print them
			try {
				p.parse();
				SemanticAnalyzer semAnalyze = new SemanticAnalyzer(p.tree);
				semAnalyze.analyze();
				CodeGen codeGen = new CodeGen(p.tree, semAnalyze.defaultScopeName);
				codeGen.generate();
				codeGen.fprint();
			} catch (Exception e) {
				e.printStackTrace();
			}
			//If the number of aurguements isn't 1, print the error and exit
		} else {
			error("Please give 1 and only 1 input file name. Enclose with \'\' or \"\" if filename has spaces");
		}
	}

	/**
	 * @return a new Token with the specified type and an empty attribute, and adds
	 *         the line and column info to the token
	 */
	public Symbol token(int terminalcode, String lexem) {
		return symbolFactory.newSymbol(sym.terminalNames[terminalcode], terminalcode,
				new NewLocation(inputFileName, yyline + 1, yycolumn + 1),
				new NewLocation(inputFileName, yyline + 1, yycolumn + yylength()), lexem);
	}

	/**
	 * @return a new Token with the specified type and attribute, and adds the line
	 *         and column info to the token
	 */
	private Symbol token(int terminalcode) {
		//Add one to line and column numbers to display more intuitively.
		return symbolFactory.newSymbol(sym.terminalNames[terminalcode], terminalcode,
				new NewLocation(inputFileName, yyline + 1, yycolumn + 1),
				new NewLocation(inputFileName, yyline + 1, yycolumn + yylength()));

	}

	/**
	 * Prints an error, and terminates the program with exit code 1.
	 * 
	 * @param s
	 *            The warning to be printed
	 */
	private static void error(String s) {
		System.err.println(s);
		System.err.println("Compiler exiting with code 1");
		System.exit(1);
	}

	/**
	 * Print a warning. When the total number of printed warnings exceeds ten, this
	 * will also terminate the program with exit code 1.
	 * 
	 * @param s
	 *            The warning to be printed
	 */
	private static void warning(String s) {
		System.err.println(s);
		if (warningCount < 10) {
			warningCount++;
		} else {
			System.err.println("Too many errors. Compiler exiting with code 1");
			System.exit(1);
		}
	}
	

%}
//this goes into the constructor
%init{
	
%init}
//Change end of file behaviour to return a EOF token
%eofval{
	return token(sym.EOF);
%eofval}

%%

//Anything after // on the same line
"//".* {}
"+"	{
	return token(sym.ADD);
}
"-"	{
	return token(sym.SUBTRACT);
}
"*"	{
	return token(sym.MULTIPLY);
}
"/"	{
	return token(sym.DIVIDE);
}
"%"	{
	return token(sym.MODULUS);
}
"<="	{
	return token(sym.LESSER_EQUAL);
}
">="	{
	return token(sym.GREATER_EQUAL);
}
"<"	{
	return token(sym.LESSER_THAN);
}
">"	{
	return token(sym.GREATER_THAN);
}

"=="	{
	return token(sym.EQUAL);
}
"="	{
	return token(sym.ASSIGN);
}
"!="	{
	return token(sym.NOT_EQUAL);
}
"!"	{
	return token(sym.NOT);
}
"&&" {
	return token(sym.AND);
}
"||" {
	return token(sym.OR);
}
"(" {
	return token(sym.OPEN_ROUND_BRACES);
}
")" {
	return token(sym.CLOSE_ROUND_BRACES);
}
"{" {
	return token(sym.OPEN_CURLY_BRACES);
}
"}" {
	return token(sym.CLOSE_CURLY_BRACES);
}
"," {
	return token(sym.COMMA);
}
";" {
	return token(sym.SEMI_COLON);
}
"int"	{
	return token(sym.KW_INT);
}
"boolean"	{
	return token(sym.KW_BOOLEAN);
}
"true"	{
	return token(sym.KW_TRUE);
}
"false"	{
	return token(sym.KW_FALSE);
}
"void"	{
	return token(sym.KW_VOID);
}
"if"	{
	return token(sym.KW_IF);
}
"else"	{
	return token(sym.KW_ELSE);
}
"while"	{
	return token(sym.KW_WHILE);
}
"break"	{
	return token(sym.KW_BREAK);
}
"return"	{
	return token(sym.KW_RETURN);
}
//Starts with a letter, and the rest is any number and combination of letters, numbers, or underscores
[a-zA-Z_][a-zA-Z0-9_]*	{
	return token(sym.ID, yytext());
}
//Composed of at least one number, and only composed of numbers
[0-9]+ {
	return token(sym.INT,yytext());
}
//starts with a " character, does not end with \", and ends with the next "
\"([^\"\n\\]|"\\"[bftrn'\"\\])*\" {
	//crop off the first and last character (the prefix and suffix quotation mark)
	return token(sym.STRING, yytext().substring(1,yytext().length()-1));
}
//when a newline happens in the middle of a string
\"([^\"\n\\]|"\\"[bftrn'\"\\])*[\n] {
	//Add one to line and column numbers to display more intuitively.
	warning("WARNING: Newline in String literal " + yytext().substring(0,yytext().length()-2) + " at end of line " + (yyline+1) + " in file " + inputFileName);
}
//when the file ends in the middle of a string
\"([^\"\n\\]|"\\"[bftrn'\"\\])* {
	//Add one to line and column numbers to display more intuitively.
	warning("WARNING: Missing closing quotation after " + yytext() + " at line " + (yyline+1)+", and column " + (yycolumn+1+yytext().length()) + " in file " + inputFileName);
}

[ \t]+	{}
[\r\n]+	{}
//anything not captured in the rules above is an unknown lexeme
.	{
	//Add one to line and column numbers to display more intuitively.
	warning("WARNING: Unknown character " + yytext() + " found at line " + (yyline+1) +", and column " + (yycolumn+1) + " in file " + inputFileName);
}

