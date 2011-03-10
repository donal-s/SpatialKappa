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
  COMPARTMENT;
  DIMENSION;
  COMPARTMENT_LINK;
  LOCATION;
  MATH_EXPR;
  INDEX;
  TRANSPORT;
  TRANSFORM;
  ID;
  LABEL;
  VAR;
  VAR_EXPR;
  VAR_INFINITY;
  PLOT;
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
  | plotExpr NEWLINE!
  | obsExpr NEWLINE!
  | varExpr NEWLINE!
  | modExpr NEWLINE!
  | NEWLINE!
  ;

ruleExpr
  :
  label? transformExpr transformKineticExpr 
    -> 
      ^(TRANSFORM transformExpr transformKineticExpr label?)
  | label locationExpr transformExpr transformKineticExpr 
    -> 
      ^(TRANSFORM transformExpr transformKineticExpr label locationExpr)
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
  '~' id
    ->
      ^(STATE id)
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
  '@' a=varAlgebraExpr (',' b=varAlgebraExpr)?
    ->
      ^(RATE $a ($b)?)
  ;

transportKineticExpr
  :
  '@' varAlgebraExpr
    ->
      ^(RATE varAlgebraExpr)
  ;

initExpr
options {backtrack=true;}
  :
  '%init:' locationExpr? INT '(' agentGroup ')'
    ->
      ^(INIT agentGroup INT locationExpr?)
  | '%init:' locationExpr? label '(' agentGroup ')'
    ->
      ^(INIT agentGroup label locationExpr?)
  ;

compartmentExpr
  :
  '%compartment:' label ('[' INT ']')*
    ->
      ^(COMPARTMENT label ^(DIMENSION INT)*)
  ;

compartmentLinkExpr
  :
  '%link:' linkName=label sourceCompartment=locationExpr transportTransition targetCompartment=locationExpr
    ->
      ^(COMPARTMENT_LINK $linkName $sourceCompartment transportTransition $targetCompartment)
  ;

transportExpr
  :
  '%transport:' (transportName=label)? linkName=label (agentGroup)? transportKineticExpr
    ->
      ^(TRANSPORT $linkName agentGroup? transportKineticExpr $transportName?)
  ;

locationExpr
  :
  sourceCompartment=label compartmentIndexExpr*
    ->
      ^(LOCATION $sourceCompartment compartmentIndexExpr*)
  ;

compartmentIndexExpr
  :
  '[' mathExpr ']'
    ->
      ^(INDEX mathExpr)
  ;

plotExpr
  :
  '%plot:' label
    ->
      ^(PLOT label)
  ;

obsExpr
  :
  '%obs:' label? agentGroup
    ->
      ^(OBSERVATION agentGroup label?)
  | '%obs:' label locationExpr agentGroup
    ->
      ^(OBSERVATION agentGroup label locationExpr)
  ;

varExpr
options {backtrack=true;}
  :
  '%var:' label agentGroup
    ->
      ^(VARIABLE agentGroup label)
  |
  '%var:' label varAlgebraExpr
    ->
      ^(VARIABLE varAlgebraExpr label)
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
  label ':=' '$INF'
    ->
      ^(ASSIGNMENT label VAR_INFINITY)
  | label ':=' a=concentrationExpression
    ->
      ^(ASSIGNMENT label $a)
  ;

concentrationExpression
  :
  '(' concentrationExpression ')'
    ->
      concentrationExpression
  | label operator concentrationExpression
    ->
      ^(CONCENTRATION_EXPRESSION label operator concentrationExpression)
  | number operator concentrationExpression
    ->
      ^(CONCENTRATION_EXPRESSION number operator concentrationExpression)
  | label
    ->
      ^(CONCENTRATION_EXPRESSION label)
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
  | label
    ->
      ^(MATH_EXPR label)
  ;
  
id
options {backtrack=true;}
  :
    ( ID_FRAGMENT | INT ) ( ID_FRAGMENT | INT | '_' | '-' )*
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
  
operator
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

operator_mult
  :
  '*'
  | '/'
  | '%'
  ;

operator_add
  :
  '+'
  | '-'
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

ID_FRAGMENT
  :
  ALPHANUMERIC
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
  '\'' .* '\''
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
