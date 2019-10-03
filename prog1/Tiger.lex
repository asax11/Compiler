package Parse;
import ErrorMsg.ErrorMsg;

%% 

%implements Lexer
%function nextToken
%type java_cup.runtime.Symbol
%char

%{
private void newline() {
  errorMsg.newline(yychar);
}

private void err(int pos, String s) {
  errorMsg.error(pos,s);
}

private void err(String s) {
  err(yychar,s);
}

private java_cup.runtime.Symbol tok(int kind) {
    return tok(kind, null);
}

private java_cup.runtime.Symbol tok(int kind, Object value) {
    return new java_cup.runtime.Symbol(kind, yychar, yychar+yylength(), value);
}

private ErrorMsg errorMsg;

Yylex(java.io.InputStream s, ErrorMsg e) {
  this(s);
  errorMsg=e;
}

private int commentCount = 0; 
private StringBuffer str; 
private int charCount;
private int ascii;
private int control;
private char test;
private char test1;


%}



%eofval{
	{
	 return tok(sym.EOF, null);
        }
%eofval} 


%state COMMENT
%state STRING
%state STRINGIGNORE



%%



<YYINITIAL> " "	{}
<YYINITIAL> \t {}
<YYINITIAL> \n	{newline();}
<YYINITIAL> ","	{return tok(sym.COMMA, null);}
<YYINITIAL> ":" {return tok(sym.COLON, null);}
<YYINITIAL> ";" {return tok(sym.SEMICOLON, null);}
<YYINITIAL> "(" {return tok(sym.LPAREN, null);}
<YYINITIAL> ")" {return tok(sym.RPAREN, null);}
<YYINITIAL> "[" {return tok(sym.LBRACK, null);}
<YYINITIAL> "]" {return tok(sym.RBRACK, null);}
<YYINITIAL> "{" {return tok(sym.LBRACE, null);}
<YYINITIAL> "}" {return tok(sym.RBRACE, null);}
<YYINITIAL> "." {return tok(sym.DOT, null);} 
<YYINITIAL> "+" {return tok(sym.PLUS, null);}
<YYINITIAL> "-" {return tok(sym.MINUS, null);}
<YYINITIAL> "*" {return tok(sym.TIMES, null);} 
<YYINITIAL> "/" {return tok(sym.DIVIDE, null);} 
<YYINITIAL> "=" {return tok(sym.EQ, null);}
<YYINITIAL> "<" {return tok(sym.LT, null);} 
<YYINITIAL> ">" {return tok(sym.GT, null);}
<YYINITIAL> "<=" {return tok(sym.LE, null);}
<YYINITIAL> ">=" {return tok(sym.GE, null);}
<YYINITIAL> "<>" {return tok(sym.NEQ, null);}
<YYINITIAL> "&" {return tok(sym.AND, null);}
<YYINITIAL> "|" {return tok(sym.OR, null);}
<YYINITIAL> ":=" {return tok(sym.ASSIGN, null);}


<YYINITIAL> "/*" {commentCount++;
					yybegin(COMMENT);}
<COMMENT> "*/" { commentCount--;
				if (commentCount == 0)
					 yybegin(YYINITIAL);
				else
					yybegin(COMMENT);}
<COMMENT> . {}


<YYINITIAL> typedef {return tok(sym.TYPE);}
<YYINITIAL> else {return tok(sym.ELSE);}
<YYINITIAL> or {return tok(sym.OR);}
<YYINITIAL> nil {return tok(sym.NIL);}
<YYINITIAL> while {return tok(sym.WHILE);}
<YYINITIAL> for {return tok(sym.FOR);}
<YYINITIAL> to {return tok(sym.TO);}
<YYINITIAL> break {return tok(sym.BREAK);}
<YYINITIAL> let {return tok(sym.LET);}
<YYINITIAL> in {return tok(sym.IN);}
<YYINITIAL> end {return tok(sym.END);}
<YYINITIAL> var {return tok(sym.VAR);}
<YYINITIAL> type {return tok(sym.TYPE);}
<YYINITIAL> if {return tok(sym.IF);}
<YYINITIAL> then {return tok(sym.THEN);}
<YYINITIAL> do {return tok(sym.DO);}
<YYINITIAL> of {return tok(sym.OF);}
<YYINITIAL> array {return tok(sym.ARRAY);}
<YYINITIAL>[0-9]+ {return tok(sym.INT, new Integer(yytext()));}
<YYINITIAL>[a-zA-Z][a-zA-Z0-9_]* {return tok(sym.ID, yytext());}

<YYINITIAL> \" {yybegin(STRING); str = new StringBuffer(); charCount = yychar;}
<STRING> [^\\\"] {str.append(yytext());}
<STRING> \\n {str.append('\n');}
<STRING> \\t {str.append('\t');}
<STRING> \\[0-9][0-9][0-9]+ {ascii = new Integer(yytext().substring(1,4));
				str.append((char)ascii);}
<STRING> \\[^][A-z]+ {test = Character.toUpperCase(yytext().charAt(2));
			control = test - '@';
			test1 = (char)control; 
			str.append(test1);}
<STRING> \\\\ {str.append('\\');}
<STRING> \\\" {str.append('\"');}
<STRING> \\[\ \n\t\b\012] {yybegin(STRINGIGNORE);}
<STRING> \" {yybegin(YYINITIAL);
 return new java_cup.runtime.Symbol(sym.STRING, charCount, yychar, str.toString());}

<STRINGIGNORE> \\ {yybegin(STRING);}
<STRINGIGNORE> . {}

. { err("Illegal character: " + yytext()); }