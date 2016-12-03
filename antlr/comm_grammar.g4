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
program: comm+ ;
comm   : comstmt stmnt+ ;

param  : vname | int_lit | str_lit | bool_lt ;
int_lit: INT ;
vname  : VNAME ;
str_lit: STR_LIT ;
bool_lt: TRUE | FALSE ;

add_all: ADD LPAREN (vname | str_lit) RPAREN ;
add_rng: ADD LPAREN (vname | str_lit) COMMA (vname | str_lit) COMMA (vname | str_lit) RPAREN ;

assign : VAR VNAME EQUALS param ;

req_vc : REQ_VC LPAREN ((vname | str_lit) COMMA)* (vname | str_lit) RPAREN ;

// config options
config : CONFIG (scale | scl_bh | scl_bw | pvt_ups | cache | no_cach) ;
scale  : SCALE   LPAREN (vname | int_lit) COMMA (vname | int_lit) COMMA (vname | bool_lt) RPAREN ;
scl_bh : SCL_BH  LPAREN (vname | int_lit) RPAREN ;
scl_bw : SCL_BW  LPAREN (vname | int_lit) RPAREN ;
pvt_ups: PVT_UPS LPAREN (vname | bool_lt) RPAREN ;
cache  : CACHE   LPAREN (vname | str_lit) RPAREN ;
no_cach: NO_CACH LPAREN RPAREN ;

comstmt: COMM VNAME SEMICOL ;
stmnt  : (add_all | add_rng | assign | req_vc | config) SEMICOL ;

// ######################################################## LEXER RULES

WS_SKIPPED
       : WS -> skip ;

COMMENT: '//' .*? [\r\n] {sop("COMMENT");skip();} ;

COMM   : 'CoMM ' {sop("COMM");} ;
ADD    : 'add' {sop("ADD");} ;
VAR    : 'var' {sop("VAR");} ;
FALSE  : 'false' {sop("FALSE");} ;
TRUE   : 'true' {sop("TRUE");} ;
CONFIG : 'config.' {sop("CONFIG");} ;
SCALE  : 'scale' {sop("SCALE");} ;
SCL_BH : 'scaleByHeight' {sop("SCL_BH");} ;
SCL_BW : 'scaleByWidth' {sop("SCL_BW");} ;
PVT_UPS: 'preventUpscaling' {sop("PVT_UPS");} ;
CACHE  : 'cache' {sop("CACHE");} ;
NO_CACH: 'noCache' {sop("NO_CACH");} ;
REQ_VC : 'requestVideoCredentials' {sop("REQ_VC");} ;

EQUALS : '=' {sop("EQUALS");} ;
LPAREN : '(' {sop("LPAREN");} ;
RPAREN : ')' {sop("RPAREN");} ;
COMMA  : ',' {sop("COMMA");} ;
SEMICOL: ';' {sop("SEMICOL");} ;

INT    : DIGIT+ {sop("INT");} ;
VNAME  : (ALNUM | '_')+ {sop("VNAME");} ;
STR_LIT: '"' .*? '"' {sop("STR_LIT");} ;
