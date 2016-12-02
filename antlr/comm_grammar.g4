grammar comm_grammar;

@lexer::members {
  boolean debugModeOn = true;

  public static void print(String label, String value) {
      System.out.println(String.format("%1$-14s", label) + ":  " + value);
  }

  public static void sop(String label) {
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

param  : URL | VNAME | INT {sop("param");} ;

expr3  : LPAREN param COMMA param COMMA param RPAREN {sop("expr3");} ;
expr2  : LPAREN param COMMA param RPAREN {sop("expr2");} ;
expr1  : LPAREN param RPAREN {sop("expr1");} ;
expr0  : LPAREN RPAREN {sop("expr0");} ;

add_all: ADD expr1 SEMICOL {sop("add_all");} ;
add_rng: ADD expr3 SEMICOL {sop("add_rng");} ;
add    : add_all | add_rng {sop("add");} ;

line   : add {sop("line");} ;

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
INT    : DIGIT+ {sop("INT");} ;
