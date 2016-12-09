grammar comm_grammar;

@header {
package comm_grammar;
}

fragment ALNUM
    : ALPHA | DIGIT ;
fragment ALPHA
    : [a-zA-Z] ;
fragment DIGIT
    : [0-9] ;
fragment WS
    : [ \t\r\n] ;

// ######################################################## PARSER RULES

// The start rule; begin parsing here.
program: comm+ EOF ;
comm   : comstmt stmnt* ;

param  : vname | int_lit | str_lit | bool_lt ;
int_lit: INT ;
vname  : VNAME ;
str_lit: STR_LIT ;
bool_lt: 'true' | 'false' ;

add_all: 'add' '(' (vname | str_lit)       ')' ;
add_rng: 'add' '(' (v1=vname | s1=str_lit) ','
                   (v2=vname | s2=str_lit) ','
                   (v3=vname | s3=str_lit) ')' ;

assign : 'var ' VNAME '=' param ;

req_vc : 'requestVideoCredentials' '(' ((vname | str_lit) ',')* (vname | str_lit) ')' ;

config : 'config' '.' (scale | scl_bh | scl_bw | pvt_ups | no_cach) ;

// config options
scale  : 'scale'            '(' (vname | int_lit) ','
                                (vname | int_lit) ','
                                (vname | bool_lt) ')' ;
scl_bh : 'scaleByHeight'    '(' (vname | int_lit) ')' ;
scl_bw : 'scaleByWidth'     '(' (vname | int_lit) ')' ;
pvt_ups: 'preventUpscaling' '(' (vname | bool_lt) ')' ;
no_cach: 'noCache'          '('                   ')' ;

comstmt: 'CoMM ' VNAME cache? ';' ;
cache  : ' cache' '(' VNAME ')' ;
stmnt  : (add_all | add_rng | assign | req_vc | config) ';' ;

// ######################################################## LEXER RULES

WS_SKIP: WS                -> skip ;
COMMENT: '//' .*? [\r\n]   -> skip ;

INT    : DIGIT+          ;
VNAME  : (ALNUM | '_')+  ;
STR_LIT: '"' .*? '"'     ;
