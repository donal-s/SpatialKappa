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
  OCCUPIED;
  ANY;
  LHS;
  RHS;
  RATE;
  RATEVALUE;
  RULE;
  PERTURBATION;
  TIME_INEQUALITY;
  CONCENTRATION_INEQUALITY;
  GREATER_THAN;
  LESS_THAN;
  ASSIGNMENT;
  CONCENTRATION_EXPRESSION;
  INFINITE_RATE;
  COMPARTMENT;
  DIMENSION;
  COMPARTMENT_LINK;
  LOCATION;
  MATH_EXPR;
  INDEX;
  TRANSPORT;
  TRANSFORM;
  ID;
}

@header        {package org.demonsoft.spatialkappa.parser;}
@lexer::header {package org.demonsoft.spatialkappa.parser;}

prog
  :
  (line)+
  ;

line
  :
  ruleExpr NEWLINE!
  | COMMENT!
  | compartmentExpr NEWLINE!
  | compartmentLinkExpr NEWLINE!
  | transportExpr NEWLINE!
  | initExpr NEWLINE!
  | obsExpr NEWLINE!
  | varExpr NEWLINE!
  | modExpr NEWLINE!
  | NEWLINE!
  ;

ruleExpr
  :
  LABEL? transformExpr transformKineticExpr 
    -> 
      ^(TRANSFORM transformExpr transformKineticExpr LABEL?)
  | LABEL locationExpr transformExpr transformKineticExpr 
    -> 
      ^(TRANSFORM transformExpr transformKineticExpr LABEL locationExpr)
  ;

transformExpr
  :
  a=agentGroup transformTransition b=agentGroup
    ->
      ^(
        transformTransition
        ^(LHS $a)
        ^(RHS $b)
       )
  | agentGroup transformTransition
    ->
      ^(
        transformTransition
        ^(LHS agentGroup)
        ^(RHS)
       )
  | transformTransition agentGroup
    ->
      ^(
        transformTransition
        ^(LHS)
        ^(RHS agentGroup)
       )
  | transformTransition
    ->
  ;

agentGroup
  :
  agent (',' agent)*
    ->
      ^(AGENTS agent+)
  ;

agent
  :
  id '(' (iface (',' iface)*)? ')'
    ->
      ^(AGENT id iface*)
  ;

iface
  :
  id stateExpr? linkExpr?
    ->
      ^(INTERFACE id stateExpr? linkExpr?)
  ;

stateExpr
  :
  '~' marker
    ->
      ^(STATE marker)
  ;

linkExpr
  :
  '!' INT
    ->
      ^(LINK INT)
  | '!' '_'
    ->
      ^(LINK OCCUPIED)
  | '?'
    ->
      ^(LINK ANY)
  ;

transformKineticExpr
  :
  '@' a=rateValueExpr (',' b=rateValueExpr)?
    ->
      ^(RATE $a ($b)?)
  ;

transportKineticExpr
  :
  '@' rateValueExpr
    ->
      ^(RATE rateValueExpr)
  ;

rateValueExpr
  :
  a=number
    ->
      ^(RATEVALUE $a)
  | b='$INF'
    ->
      ^(RATEVALUE INFINITE_RATE)
  ;

initExpr
  :
  '%init:' INT '*' '(' agentGroup ')'
    ->
      ^(INIT agentGroup INT)
  | '%init:' locationExpr INT '*' '(' agentGroup ')'
    ->
      ^(INIT agentGroup INT locationExpr)
  ;

compartmentExpr
  :
  '%compartment:' LABEL ('[' INT ']')*
    ->
      ^(COMPARTMENT LABEL ^(DIMENSION INT)*)
  ;

compartmentLinkExpr
  :
  '%link:' linkName=LABEL sourceCompartment=locationExpr transportTransition targetCompartment=locationExpr
    ->
      ^(COMPARTMENT_LINK $linkName $sourceCompartment transportTransition $targetCompartment)
  ;

transportExpr
  :
  '%transport:' (transportName=LABEL)? linkName=LABEL (agentGroup)? transportKineticExpr
    ->
      ^(TRANSPORT $linkName agentGroup? transportKineticExpr $transportName?)
  ;

locationExpr
  :
  sourceCompartment=LABEL compartmentIndexExpr*
    ->
      ^(LOCATION $sourceCompartment compartmentIndexExpr*)
  ;

compartmentIndexExpr
  :
  '[' mathExpr ']'
    ->
      ^(INDEX mathExpr)
  ;

obsExpr
  :
  '%obs:' LABEL? agentGroup
    ->
      ^(OBSERVATION agentGroup LABEL?)
  | '%obs:' LABEL locationExpr agentGroup
    ->
      ^(OBSERVATION agentGroup LABEL locationExpr)
  | '%obs:' LABEL
    ->
      ^(OBSERVATION LABEL)
  ;

varExpr
  :
  '%var:' LABEL agentGroup
    ->
      ^(VARIABLE agentGroup LABEL)
  ;

modExpr
  :
  '%mod:' concentrationInequality 'do' assignment
    ->
      ^(PERTURBATION concentrationInequality assignment)
  | '%mod:' timeInequality 'do' assignment
    ->
      ^(PERTURBATION timeInequality assignment)
  ;
  
timeInequality
  :
  '$T' '>' number
    ->
      ^(TIME_INEQUALITY number)
  ;
  
concentrationInequality
options {backtrack=true;}
  :
  concentrationExpression '>' concentrationExpression
    ->
      ^(CONCENTRATION_INEQUALITY concentrationExpression GREATER_THAN concentrationExpression)
  | concentrationExpression '<' concentrationExpression
    ->
      ^(CONCENTRATION_INEQUALITY concentrationExpression LESS_THAN concentrationExpression)
  ;
  

assignment
  :
  LABEL ':=' '$INF'
    ->
      ^(ASSIGNMENT LABEL INFINITE_RATE)
  | LABEL ':=' a=concentrationExpression
    ->
      ^(ASSIGNMENT LABEL $a)
  ;

concentrationExpression
  :
  '(' concentrationExpression ')'
    ->
      concentrationExpression
  | LABEL operator concentrationExpression
    ->
      ^(CONCENTRATION_EXPRESSION LABEL operator concentrationExpression)
  | number operator concentrationExpression
    ->
      ^(CONCENTRATION_EXPRESSION number operator concentrationExpression)
  | LABEL
    ->
      ^(CONCENTRATION_EXPRESSION LABEL)
  | number
    ->
      ^(CONCENTRATION_EXPRESSION number)
  ;
  
mathExpr
options {backtrack=true;}
  :
  a=mathAtom operator b=mathAtom
    ->
      ^(MATH_EXPR operator $a $b)
  | a=mathAtom
    ->
      $a
  ;
  
mathAtom
options {backtrack=true;}
  :
  '(' mathExpr ')'
    ->
      mathExpr
  | INT
    ->
      ^(MATH_EXPR INT)
  | VARIABLE_NAME
    ->
      ^(MATH_EXPR VARIABLE_NAME)
  ;
  
id
  :
  (
    ( ALPHANUMERIC | VARIABLE_NAME | MARKER | INT ) ( ALPHANUMERIC | VARIABLE_NAME | MARKER | INT | '_' | '^' | '-' )*
  )
   ->
    {new CommonTree(new CommonToken(ID,$id.text.toString()))} // Avoid lexing as mutiple tokens
  ;

marker
  :
  (
    VARIABLE_NAME
    | MARKER
    | INT
  )
  ;


number
  :
  (
    INT
    | FLOAT
  )
  ;
  
operator
  :
  '+'
  | '*'
  | '-'
  | '/'
  | '%'
  ;

transformTransition
  :
  ( 
    FORWARD_TRANSITION
    | EQUILIBRIUM_TRANSITION
  )
  ;

transportTransition
  :
  ( 
    FORWARD_TRANSITION
    | BACKWARD_TRANSITION
    | EQUILIBRIUM_TRANSITION
  )
  ;

EQUILIBRIUM_TRANSITION
  :
  '<->'
  ;

FORWARD_TRANSITION
  :
  '->'
  ;

BACKWARD_TRANSITION
  :
  '<-'
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

VARIABLE_NAME
  :
  ('a'..'z' | 'A'..'Z') ('a'..'z' | 'A'..'Z' | '0'..'9')*
  ;

MARKER
  :
  ALPHANUMERIC
  ;

fragment
ID
  :
  ALPHANUMERIC
  (
    ALPHANUMERIC
    | '_'
    | '^'
    | '-'
  )*
  ;

fragment
ALPHANUMERIC
  :
  (
    NUMERIC
    | 'a'..'z'
    | 'A'..'Z'
  )+
  ;

fragment
NUMERIC
  :
  ('0'..'9')+
  ;
  
fragment
EXPONENT
  : ('e'|'E') ('+'|'-')? NUMERIC
  ;

LABEL
  :
  '\'' .* '\''  { setText(getText().substring(1, getText().length() - 1)); }
  ;

COMMENT
  :
  '#'
  ~(
    '\n'
    | '\r'
   )*
  NEWLINE {$channel=HIDDEN;}
  ;

NEWLINE
  :
  '\r'? '\n'
  | '\r'
  ;

WS
  :
  (
    ' '
    | '\t'
  )+
  {$channel=HIDDEN;}
  ;
