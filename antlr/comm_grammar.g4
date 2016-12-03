grammar comm_grammar;

@parser::members {
  public static boolean debugModeOn = true;

  public static void print(String label, String value) {
      System.out.println(String.format("%1$-14s", label) + ":  " + value);
  }
}

@lexer::members {
  public static boolean debugModeOn = true;

  public static void print(String label, String value) {
      System.out.println(String.format("%1$-14s", label) + ":  " + value);
  }

  void sop(String label) {
      if (debugModeOn) print(label, getText());
  }
}


fragment ALNUM
    : ALPHA | DIGIT ;
fragment ALPHA
    : [a-zA-Z] ;
fragment DIGIT
    : [0-9] ;
fragment WS
    : [ \t\r\n] ;
// Any other character needs to be encoded with the percent-encoding (%hh).
//fragment URL_CHAR
//    : [._~:/?#@!$&'*+,=`.] | '[' | ']' ;


// ######################################################## PARSER RULES

// The start rule; begin parsing here.
program: comstmt stmnt+ ;

param  : vname | int_lit | str_lit | bool_lt ;
int_lit: INT ;
vname  : VNAME ;
str_lit: STR_LIT ;
bool_lt: TRUE | FALSE ;

add_all: ADD LPAREN (vname | str_lit) RPAREN ;
add_rng: ADD LPAREN (vname | str_lit) COMMA (vname | str_lit) COMMA (vname | str_lit) RPAREN ;

assign : VAR VNAME EQUALS param ;

comstmt: COMM VNAME SEMICOL ;
stmnt  : (add_all | add_rng | assign) SEMICOL ;

// ######################################################## LEXER RULES

WS_SKIPPED
       : WS -> skip ;

COMMENT: '//' .*? [\r\n] {sop("COMMENT");skip();} ;

COMM   : 'CoMM ' {sop("COMM");} ;
ADD    : 'add' {sop("ADD");} ;
VAR    : 'var' {sop("VAR");} ;
TRUE   : 'true' {sop("TRUE");} ;
FALSE  : 'false' {sop("FALSE");} ;

EQUALS : '=' {sop("EQUALS");} ;
LPAREN : '(' {sop("LPAREN");} ;
RPAREN : ')' {sop("RPAREN");} ;
COMMA  : ',' {sop("COMMA");} ;
SEMICOL: ';' {sop("SEMICOL");} ;

INT    : DIGIT+ {sop("INT");} ;
VNAME  : (ALNUM | '_')+ {sop("VNAME");} ;
STR_LIT: '"' .*? '"' {sop("STR_LIT");} ;
