 grammar SpatialKappa;

options {
  language     = Java;
  output       = AST;
  ASTLabelType = CommonTree;
}

tokens {
  ADD;
  AGENT;
  AGENT_DECL;
  AGENTS;
  AND;
  ANY;
  BOOL_EXPR;
  CELL_INDEX_EXPR;
  CHANNEL;
  COMPARTMENT;
  CONDITION;
  COS;
  DIMENSION;
  EFFECT;
  EVENTS;
  EXP;
  FALSE;
  ID;
  INDEX;
  INIT;
  INTERFACE;
  LHS;
  LINK;
  LOCATION;
  LOCATION_PAIR;
  LOCATIONS;
  LOG;
  MODULUS;
  NOT;
  OBSERVATION;
  OCCUPIED;
  OR;
  PERTURBATION;
  PI;
  PLOT;
  RATE;
  REMOVE;
  RHS;
  RULE;
  SET;
  SIN;
  SNAPSHOT;
  SQRT;
  STATE;
  STOP;
  TAN;
  TARGET;
  TIME;
  TRANSITION;
  TRUE;
  TYPE;
  UNTIL;
  VAR_EXPR;
  VAR_INFINITY;
  VARIABLE;
}

@header        {package org.demonsoft.spatialkappa.parser;}
@lexer::header {package org.demonsoft.spatialkappa.parser;}

prog
  :
  (line)*
  ;

line
  :
  agentDecl NEWLINE!
  | compartmentDecl NEWLINE!
  | channelDecl NEWLINE!
  | ruleDecl NEWLINE!
  | initDecl NEWLINE!
  | plotDecl NEWLINE!
  | obsDecl NEWLINE!
  | varDecl NEWLINE!
  | modDecl NEWLINE!
  | COMMENT!
  | NEWLINE!
  ;

ruleDecl
  :
  label? transition rate 
    -> 
      ^(RULE transition rate label?)
  ;

transition
options {backtrack=true;}
  :
  (source=location)? CHANNEL_TRANSITION channelName=id (target=location)?
    ->
      ^(
        TRANSITION
        ^(LHS $source?)
        ^(RHS $target?)
        ^(CHANNEL $channelName)?
       )
  |
  (a=agentGroup)? CHANNEL_TRANSITION channelName=id (b=agentGroup)?
    ->
      ^(
        TRANSITION
        ^(LHS $a?)
        ^(RHS $b?)
        ^(CHANNEL $channelName)?
       )
  |
  (a=agentGroup)? FORWARD_TRANSITION (b=agentGroup)?
    ->
      ^(
        TRANSITION
        ^(LHS $a?)
        ^(RHS $b?)
       )
  ;
  

agentGroup
  :
  location? agent (',' agent)*
    ->
      ^(AGENTS location? agent+)
  ;

agent
  :
  id (location)? ('(' (agentInterface (',' agentInterface)*)? ')')?
    ->
      ^(AGENT id location? agentInterface*)
  ;

agentInterface
  :
  id state? link?
    ->
      ^(INTERFACE id state? link?)
  ;

state
  :
  '~' id
    ->
      ^(STATE id)
  ;

link
  :
  '!' INT (':' channelName=id)?
    ->
      ^(LINK ^(CHANNEL $channelName)? INT)
  | '!' '_' (':' channelName=id)?
    ->
      ^(LINK ^(CHANNEL $channelName)? OCCUPIED)
  | '?'
    ->
      ^(LINK ANY)
  ;

rate
  :
  '@' varAlgebraExpr
    ->
      ^(RATE varAlgebraExpr)
  ;

initDecl
  :
  '%init:' (INT | label) agentGroup
    ->
      ^(INIT agentGroup INT? label?)
  ;

agentDecl
  :
  '%agent:' agentName=id ('(' (agentDeclInterface (',' agentDeclInterface)*)? ')')?
    ->
     ^(AGENT_DECL $agentName agentDeclInterface*)
  ;

agentDeclInterface
  :
  id state*
    ->
      ^(INTERFACE id state*)
  ;

compartmentDecl
  :
  '%compartment:' name=id (type=id)? ('[' INT ']')*
    ->
      ^(COMPARTMENT $name ^(TYPE $type)? ^(DIMENSION INT)*)
  ;

channelDecl
  :
  '%channel:' linkName=id channel
    ->
      ^(CHANNEL $linkName channel)
  |
  '%channel:' linkName=id '(' channel ')' ('+' '(' channel ')')*
    ->
      ^(CHANNEL $linkName channel+)
  ;

channel
  :
  source=locations FORWARD_TRANSITION target=locations
    ->
      ^(LOCATION_PAIR $source $target)
  ;

locations
  :
  location (',' location)*
    ->
     ^(LOCATIONS location+)
  ;

location
  :
  ':' sourceCompartment=id compartmentIndexExpr*
    ->
      ^(LOCATION $sourceCompartment compartmentIndexExpr*)
  ;

compartmentIndexExpr
  :
  '[' cellIndexExpr ']'
    ->
      ^(INDEX cellIndexExpr)
  ;

plotDecl
  :
  '%plot:' label
    ->
      ^(PLOT label)
  ;

obsDecl
  :
  '%obs:' label? agentGroup
    ->
      ^(OBSERVATION agentGroup label?)
  ;

varDecl
options {backtrack=true;}
  :
  '%var:' label varAlgebraExpr
    ->
      ^(VARIABLE varAlgebraExpr label)
   |
  '%var:' label agentGroup
    ->
      ^(VARIABLE agentGroup label)
 ;

varAlgebraExpr
  :
  (a=varAlgebraMultExpr -> $a) (op=operator_add b=varAlgebraMultExpr -> ^(VAR_EXPR $op $varAlgebraExpr $b) )*
  ;
  
varAlgebraMultExpr
  :
  (a=varAlgebraExpExpr -> $a) (op=operator_mult b=varAlgebraExpExpr -> ^(VAR_EXPR $op $varAlgebraMultExpr $b) )*
  ;
  
varAlgebraExpExpr
options {backtrack=true;}
  :
  a=varAlgebraAtom operator_exp b=varAlgebraExpExpr
    ->
      ^(VAR_EXPR operator_exp $a $b)
  | a=varAlgebraAtom
    ->
      $a
  ;
  
varAlgebraAtom
  :
  '(' varAlgebraExpr ')'
    ->
      varAlgebraExpr
  | number
    ->
      ^(VAR_EXPR number)
  | label
    ->
      ^(VAR_EXPR label)
  | '[' 'inf' ']'
    ->
      ^(VAR_EXPR VAR_INFINITY)
  | '[' 'pi' ']'
    ->
      ^(VAR_EXPR PI)
  | '[' 'T' ']'
    ->
      ^(VAR_EXPR TIME)
  | '[' 'E' ']'
    ->
      ^(VAR_EXPR EVENTS)
  | operator_unary varAlgebraAtom
    ->
      ^(VAR_EXPR operator_unary varAlgebraAtom)
  | operator_binary_prefix a=varAlgebraAtom b=varAlgebraAtom
    ->
      ^(VAR_EXPR operator_binary_prefix $a $b)
  ;
  
modDecl
  :
  '%mod:' booleanExpression 'do' effect until?
    ->
      ^(PERTURBATION ^(CONDITION booleanExpression) effect until?)
  ;
  

booleanExpression
  :
  (a=booleanAtom -> $a) (op=booleanOperator b=booleanAtom -> ^(BOOL_EXPR $op $booleanExpression $b) )*
  ;
  
booleanOperator
  :
  '&&' -> AND
  | '||' -> OR
  ;

relationalOperator
  :
  '<' | '>' | '='
  ;

  
booleanAtom
options {backtrack=true;}
  :
  '(' booleanExpression ')'
    ->
      booleanExpression
  | '[' 'true' ']'
    ->
      ^(BOOL_EXPR TRUE)
  | '[' 'false' ']'
    ->
      ^(BOOL_EXPR FALSE)
  | '[' 'not' ']' booleanAtom
    ->
      ^(BOOL_EXPR NOT booleanAtom)
  | a=varAlgebraExpr relationalOperator b=varAlgebraExpr
    ->
      ^(BOOL_EXPR relationalOperator $a $b)
  ;

effect
  :
  '$SNAPSHOT'
    ->
      ^(EFFECT SNAPSHOT)
  | '$STOP'
    ->
      ^(EFFECT STOP)
  | '$ADD' varAlgebraExpr agentGroup
    ->
      ^(EFFECT ADD varAlgebraExpr agentGroup)
  | '$DEL' varAlgebraExpr agentGroup
    ->
      ^(EFFECT REMOVE varAlgebraExpr agentGroup)
  | label ':=' varAlgebraExpr
    ->
      ^(EFFECT SET ^(TARGET label) varAlgebraExpr)
  ;
  
until
  :
  'until' booleanExpression
    ->
      ^(UNTIL booleanExpression)
  ;
  
cellIndexExpr
options {backtrack=true;}
  :
  a=cellIndexAtom operator_cell_index b=cellIndexAtom
    ->
      ^(CELL_INDEX_EXPR operator_cell_index $a $b)
  | a=cellIndexAtom
    ->
      $a
  ;
  
cellIndexAtom
options {backtrack=true;}
  :
  '(' cellIndexExpr ')'
    ->
      cellIndexExpr
  | INT
    ->
      ^(CELL_INDEX_EXPR INT)
  | id
    ->
      ^(CELL_INDEX_EXPR id)
  ;
  
id
  :
    ( INT | ID_FRAGMENT ) 
   ->
    {new CommonTree(new CommonToken(ID,$id.text.toString()))} // Avoid lexing as mutiple tokens
  ;

label
  :
  LABEL
   ->
    {new CommonTree(new CommonToken(LABEL, $label.text.substring(1, $label.text.length() - 1)))}
  ;

number
  :
  (
    INT
    | FLOAT
  )
  ;
  
operator_cell_index
  :
  '+'
  | '*'
  | '-'
  | '/'
  | '%'
  | '^'
  ;

operator_exp
  :
  | '^'
  ;

operator_unary
  :
  '[' 'log' ']' -> LOG
  | '[' 'sin' ']' -> SIN
  | '[' 'cos' ']' -> COS
  | '[' 'tan' ']' -> TAN
  | '[' 'sqrt' ']' -> SQRT
  | '[' 'exp' ']' -> EXP
  ;

operator_binary_prefix
  :
  '[' 'mod' ']' -> MODULUS
  ;

operator_mult
  :
  '*'
  | '/'
  ;

operator_add
  :
  '+'
  | '-'
  ;


CHANNEL_TRANSITION
  :
  '->:'
  ;

FORWARD_TRANSITION
  :
  '->'
  ;

INT
  :
  NUMERIC
  ;

FLOAT
    :   NUMERIC '.' NUMERIC EXPONENT?
    |   '.' NUMERIC EXPONENT?
    |   NUMERIC EXPONENT
    ;

ID_FRAGMENT
  :
  (
    ALPHANUMERIC
  ) 
  (
    ALPHANUMERIC
    | '_'
    | '-'
  )*
  ;

fragment
NUMERIC
  :
  ('0'..'9')+
  ;
  
fragment
ALPHANUMERIC
  :
  (NUMERIC | 'a'..'z' | 'A'..'Z')+
  ;
  
fragment
EXPONENT
  : ('e'|'E') ('+'|'-')? NUMERIC
  ;

LABEL
  :
  '\'' .* '\''
  ;

COMMENT
  :
  '#'
  ~(
    '\n'
    | '\r'
   )*
  {$channel=HIDDEN;}
  ;

WS
  :
  (
    ' '
    | '\t'
    | '\\' NEWLINE
  )+
  {$channel=HIDDEN;}
  ;

NEWLINE
  :
  '\r'? '\n'
  | '\r'
  ;

