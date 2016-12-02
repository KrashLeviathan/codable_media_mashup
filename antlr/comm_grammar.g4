grammar comm_grammar;

@lexer::members {
  public static void print(String label, String value) {
     System.out.println(String.format("%1$-14s", label) + ":  " + value);
  }

  public static String removeWS(String str) {
      return str.replaceAll("[ \t\r\n]+","");
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

param  : URL | VNAME | INT ;

expr0  : LPAREN RPAREN ;
expr1  : LPAREN param RPAREN ;
expr2  : LPAREN param COMMA param RPAREN ;
expr3  : LPAREN param COMMA param COMMA param RPAREN ;

add_all: ADD expr1 SEMICOL ;
add_rng: ADD expr3 SEMICOL ;
add    : add_all | add_rng ;

line   : add ;

// ######################################################## LEXER RULES

WS_SKIPPED
       : WS -> skip ;

ADD    : 'add' ;
VAR    : 'var' ;

LPAREN : '(' ;
RPAREN : ')' ;
COMMA  : ',' ;
SEMICOL: ';' ;

URL    : (ALNUM | URL_CHAR)+ ;
VNAME  : ALNUM | '_' ;
INT    : DIGIT+ ;
