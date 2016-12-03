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
fragment URL_CHAR
    : [._~:/?#@!$&'()*+,;=`.] | '[' | ']' ;


// ######################################################## PARSER RULES

// The start rule; begin parsing here.
program: stmnt+ ;

param  : URL | VNAME | INT | STR_LIT;

expr3  : LPAREN param COMMA param COMMA param RPAREN ;
expr2  : LPAREN param COMMA param RPAREN ;
expr1  : LPAREN param RPAREN ;
expr0  : LPAREN RPAREN {print("expr0", $LPAREN.text + $RPAREN.text);} ;

add_all: ADD expr1 SEMICOL ;
add_rng: ADD expr3 SEMICOL ;
add    : add_all | add_rng ;

stmnt  : add ;

// ######################################################## LEXER RULES

WS_SKIPPED
       : WS -> skip ;

ADD    : 'add' {sop("ADD");} ;
VAR    : 'var' {sop("VAR");} ;

LPAREN : '(' {sop("LPAREN");} ;
RPAREN : ')' {sop("RPAREN");} ;
COMMA  : ',' {sop("COMMA");} ;
SEMICOL: ';' {sop("SEMICOL");} ;

URL    : (ALNUM | URL_CHAR)+ {sop("URL");} ;
VNAME  : ALNUM | '_' {sop("VNAME");} ;
STR_LIT: '"' .*? '"' {sop("STR_LIT");} ;
INT    : DIGIT+ {sop("INT");} ;
