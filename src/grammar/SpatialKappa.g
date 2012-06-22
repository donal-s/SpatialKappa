 grammar SpatialKappa;

options {
  language     = Java;
  output       = AST;
  ASTLabelType = CommonTree;
}

tokens {
  OBSERVATION;
  VARIABLE;
  INIT;
  AGENTS;
  AGENT;
  INTERFACE;
  STATE;
  LINK;
  CHANNEL;
  OCCUPIED;
  ANY;
  LHS;
  RHS;
  RATE;
  PERTURBATION;
  COMPARTMENT;
  DIMENSION;
  LOCATION;
  LOCATION_PAIR;
  CELL_INDEX_EXPR;
  INDEX;
  TRANSITION;
  RULE;
  ID;
  LABEL;
  VAR_EXPR;
  VAR_INFINITY;
  PLOT;
  CONDITION;
  EFFECT;
  UNTIL;
  SNAPSHOT;
  STOP;
  ADD;
  REMOVE;
  SET;
  BOOL_EXPR;
  AND;
  OR;
  NOT;
  TRUE;
  FALSE;
  TIME;
  EVENTS;
  TARGET;
  LOG;
  MODULUS;
  PI;
  SIN;
  COS;
  TAN;
  SQRT;
  EXP;
  AGENT_DECL;
}

@header        {package org.demonsoft.spatialkappa.parser;}
@lexer::header {package org.demonsoft.spatialkappa.parser;}

prog
  :
  (line)*
  ;

line
  :
  ruleExpr NEWLINE!
  | COMMENT!
  | compartmentExpr NEWLINE!
  | channelDecl NEWLINE!
  | initExpr NEWLINE!
  | plotExpr NEWLINE!
  | obsExpr NEWLINE!
  | varExpr NEWLINE!
  | modExpr NEWLINE!
  | agentExpr NEWLINE!
  | NEWLINE!
  ;

ruleExpr
  :
  label? transformExpr kineticExpr 
    -> 
      ^(RULE transformExpr kineticExpr label?)
  ;

transformExpr
options {backtrack=true;}
  :
  (source=locationExpr)? (a=agentGroup)? CHANNEL_TRANSITION channelName=id (target=locationExpr)? (b=agentGroup)?
    ->
      ^(
        TRANSITION
        ^(LHS $source? $a?)
        ^(RHS $target? $b?)
        ^(CHANNEL $channelName)?
       )
  |
  (source=locationExpr)? (a=agentGroup)? FORWARD_TRANSITION (target=locationExpr)? (b=agentGroup)?
    ->
      ^(
        TRANSITION
        ^(LHS $source? $a?)
        ^(RHS $target? $b?)
       )
  ;
  

agentGroup
  :
  agent (',' agent)*
    ->
      ^(AGENTS agent+)
  ;

agent
  :
  id (locationExpr)? ('(' (iface (',' iface)*)? ')')?
    ->
      ^(AGENT id locationExpr? iface*)
  ;

iface
  :
  id stateExpr? linkExpr?
    ->
      ^(INTERFACE id stateExpr? linkExpr?)
  ;

stateExpr
  :
  '~' id
    ->
      ^(STATE id)
  ;

linkExpr
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

kineticExpr
  :
  '@' varAlgebraExpr
    ->
      ^(RATE varAlgebraExpr)
  ;

initExpr
options {backtrack=true;}
  :
  '%init:' INT locationExpr? agentGroup
    ->
      ^(INIT agentGroup INT locationExpr?)
  | '%init:' label locationExpr? agentGroup
    ->
      ^(INIT agentGroup label locationExpr?)
  ;

agentExpr
  :
  '%agent:' agentName=id ('(' (agentIfaceExpr (',' agentIfaceExpr)*)? ')')?
    ->
     ^(AGENT_DECL $agentName agentIfaceExpr*)
  ;

agentIfaceExpr
  :
  id stateExpr*
    ->
      ^(INTERFACE id stateExpr*)
  ;


compartmentExpr
  :
  '%compartment:' id ('[' INT ']')*
    ->
      ^(COMPARTMENT id ^(DIMENSION INT)*)
  ;

channelDecl
  :
  '%channel:' linkName=id channelExpr
    ->
      ^(CHANNEL $linkName channelExpr)
  |
  '%channel:' linkName=id '(' + channelExpr + ')' ('+' '('+ channelExpr + ')')*
    ->
      ^(CHANNEL $linkName channelExpr+)
  ;

channelExpr
  :
  sourceCompartment=locationExpr FORWARD_TRANSITION targetCompartment=locationExpr
    ->
      ^(LOCATION_PAIR $sourceCompartment $targetCompartment)
  ;

locationExpr
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

plotExpr
  :
  '%plot:' label
    ->
      ^(PLOT label)
  ;

obsExpr
options {backtrack=true;}
  :
  '%obs:' label? agentGroup
    ->
      ^(OBSERVATION agentGroup label?)
  | '%obs:' label? locationExpr agentGroup
    ->
      ^(OBSERVATION agentGroup label? locationExpr)
  ;

varExpr
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
options {backtrack=true;}
  :
  (a=varAlgebraMultExpr -> $a) (op=operator_add b=varAlgebraMultExpr -> ^(VAR_EXPR $op $varAlgebraExpr $b) )*
  ;
  
varAlgebraMultExpr
options {backtrack=true;}
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
options {backtrack=true;}
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
  
modExpr
  :
  '%mod:' booleanExpression 'do' effect untilExpression?
    ->
      ^(PERTURBATION ^(CONDITION booleanExpression) effect untilExpression?)
  ;
  

booleanExpression
options {backtrack=true;}
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
  
untilExpression
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

