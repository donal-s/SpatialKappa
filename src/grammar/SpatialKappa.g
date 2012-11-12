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
  DIMENSION;
  EFFECT;
  EFFECTS;
  ELAPSED_TIME;
  EVENTS;
  OP_EXP;
  FALSE;
  FIXED;
  ID;
  INDEX;
  INIT;
  INTERFACE;
  LHS;
  LINK;
  LOCATION;
  LOCATION_PAIR;
  LOCATIONS;
  MAX_EVENTS;
  MAX_TIME;
  MODEL;
  NOT;
  OBSERVATION;
  OCCUPIED;
  OCCUPIED_SITE;
  OP_ABS;
  OP_COS;
  OP_DIVIDE;
  OP_LOG;
  OP_MINUS;
  OP_MODULUS;
  OP_MULTIPLY;
  OP_PLUS;
  OP_POWER;
  OP_SIN;
  OP_SQRT;
  OP_TAN;
  OR;
  PERTURBATION;
  PERTURBATION_EXPR;
  PI;
  PLOT;
  RATE;
  REMOVE;
  RHS;
  RULE;
  SET;
  SNAPSHOT;
  STATE;
  STOP;
  TARGET;
  TIME;
  TRANSITION;
  TRUE;
  TYPE;
  UNTIL;
  VAR_EXPR;
  VAR_INFINITY;
  VARIABLE;
  VOXEL;
  WILDCARD;
}

@header        {package org.demonsoft.spatialkappa.parser;}
@lexer::header {package org.demonsoft.spatialkappa.parser;}

@members {
    private List<String> errors = new ArrayList<String>();
    public void emitErrorMessage(String message) {
        errors.add(message);
    }
    public List<String> getErrors() {
        return errors;
    }
}

@lexer::members {
    private List<String> errors = new ArrayList<String>();
    public void emitErrorMessage(String message) {
        errors.add(message);
    }
    public List<String> getErrors() {
        return errors;
    }
}


prog
  :
  (line)*
    -> 
      ^(MODEL line*)
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
  '(' agentGroup ')'
    ->
      agentGroup
  |
  location? agent (',' agent)*
    ->
      ^(AGENTS location? agent+)
  ;

agent
  :
  id (location)? '(' (agentInterface (',' agentInterface)*)? ')'
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
  '~' stateId
    ->
      ^(STATE stateId)
  ;

link
  :
  '!' INT (':' channelName=id)?
    ->
      ^(LINK ^(CHANNEL $channelName)? INT)
  | '!' '_' (':' channelName=id)?
    ->
      ^(LINK ^(CHANNEL $channelName)? OCCUPIED)
  | '!' interfaceName=id '.' agentName=id (':' channelName=id)?
    ->
      ^(LINK ^(CHANNEL $channelName)? ^(OCCUPIED_SITE ^(AGENT $agentName ^(INTERFACE $interfaceName))))
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
  '%agent:' agentName=id '(' (agentDeclInterface (',' agentDeclInterface)*)? ')'
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
  (type=id)? source=locations FORWARD_TRANSITION target=locations
    ->
      ^(LOCATION_PAIR (^(TYPE $type))? $source $target)
  ;

locations
  : location (',' location)*
    ->
     ^(LOCATIONS location+)
  ;

location
  : ':' 'fixed'
    ->
      ^(LOCATION FIXED)
  | ':' sourceCompartment=id compartmentIndexExpr*
    ->
      ^(LOCATION $sourceCompartment compartmentIndexExpr*)
  ;

compartmentIndexExpr
  : '[' '?' ']'
    ->
      ^(INDEX ^(CELL_INDEX_EXPR WILDCARD))
  | '[' cellIndexExpr ']'
    ->
      ^(INDEX cellIndexExpr)
  ;

plotDecl
  : '%plot:' label
    ->
      ^(PLOT label)
  ;

obsDecl
options {backtrack=true;}
  : '%obs:' label varAlgebraExpr
    ->
      ^(OBSERVATION varAlgebraExpr label)
  | '%obs:' 'voxel' label agentGroup
    ->
      ^(OBSERVATION VOXEL agentGroup label)
  | '%obs:' label agentGroup
    ->
      ^(OBSERVATION agentGroup label)
  ;

varDecl
options {backtrack=true;}
  : '%var:' label varAlgebraExpr
    ->
      ^(VARIABLE varAlgebraExpr label)
  | '%var:' 'voxel' label agentGroup
    ->
      ^(VARIABLE VOXEL agentGroup label)
  | '%var:' label agentGroup
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
  :
  (a=varAlgebraAtom -> $a) ('^' b=varAlgebraAtom -> ^(VAR_EXPR OP_POWER $varAlgebraExpExpr $b) )*
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
  | '[' 'Tsim' ']'
    ->
      ^(VAR_EXPR ELAPSED_TIME)
  | '[' 'Tmax' ']'
    ->
      ^(VAR_EXPR MAX_TIME)
  | '[' 'Emax' ']'
    ->
      ^(VAR_EXPR MAX_EVENTS)
  | '[' 'T' ']'
    ->
      ^(VAR_EXPR TIME)
  | '[' 'E' ']'
    ->
      ^(VAR_EXPR EVENTS)
  | operator_unary varAlgebraAtom
    ->
      ^(VAR_EXPR operator_unary varAlgebraAtom)
  | '|' agentGroup '|'
    ->
      ^(VAR_EXPR agentGroup)
  ;
  
modDecl
  :
  '%mod:' 'repeat' perturbationExpression 'until' booleanExpression
    ->
      ^(PERTURBATION perturbationExpression ^(UNTIL booleanExpression))
  |
  '%mod:' perturbationExpression
    ->
      ^(PERTURBATION perturbationExpression)
  ;

perturbationExpression
options {backtrack=true;}
  :
  '(' perturbationExpression ')'
    ->
      perturbationExpression
  |
  booleanExpression 'do' effects
    ->
      ^(PERTURBATION_EXPR ^(CONDITION booleanExpression) effects)
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
  '<' | '>' | '=' | '<>'
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

effects
  : '(' effects ')'
    ->
      effects
  | effect (';' effect)*
    ->
      ^(EFFECTS effect+)
  ;

effect
  : '$SNAPSHOT'
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
  | '$UPDATE' label varAlgebraExpr
    ->
      ^(EFFECT SET ^(TARGET label) varAlgebraExpr)
  ;
  
cellIndexExpr
options {backtrack=true;}
  : a=cellIndexAtom operator_cell_index b=cellIndexAtom
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
  ( 'inf' | 'pi' | 'T' | 'E' | 'Tmax' | 'Tsim' | 'Emax' | 'repeat' | 'until' | 'do'
  | 'set' | 'true' | 'false' | 'not' | 'mod' | 'log' | 'sin' | 'cos' | 'tan' | 'sqrt' | 'exp' | 'int' 
  | ID_FRAGMENT | COMMON_ID_FRAGMENT )
   ->
    {new CommonTree(new CommonToken(ID,$id.text.toString()))} // Avoid lexing as multiple tokens
  ;

stateId
  :
  ( 'inf' | 'pi' | 'T' | 'E' | 'Tmax' | 'Tsim' | 'Emax' | 'repeat' | 'until' | 'do'
  | 'set' | 'true' | 'false' | 'not' | 'mod' | 'log' | 'sin' | 'cos' | 'tan' | 'sqrt' | 'exp' | 'int' 
  | STATE_ID_FRAGMENT | INT | COMMON_ID_FRAGMENT )
   ->
    {new CommonTree(new CommonToken(ID,$stateId.text.toString()))} // Avoid lexing as multiple tokens
  ;

label
  :
  LABEL
   ->
    {new CommonTree(new CommonToken(LABEL, ($label.text != null && $label.text.length() >= 2) ? 
      $label.text.substring(1, $label.text.length() - 1) : $label.text))}
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

operator_unary
  :
  '[' 'log' ']' -> OP_LOG
  | '[' 'sin' ']' -> OP_SIN
  | '[' 'cos' ']' -> OP_COS
  | '[' 'tan' ']' -> OP_TAN
  | '[' 'sqrt' ']' -> OP_SQRT
  | '[' 'exp' ']' -> OP_EXP
  | '[' 'int' ']' -> OP_ABS
  ;

operator_mult
  :
  '*' -> OP_MULTIPLY
  | '/' -> OP_DIVIDE
  | '[' 'mod' ']' -> OP_MODULUS
  ;

operator_add
  :
  '+' -> OP_PLUS
  | '-' -> OP_MINUS
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

COMMON_ID_FRAGMENT
  :
  (
    'a'..'z' | 'A'..'Z'
  ) 
  ALPHANUMERIC?
  ;

STATE_ID_FRAGMENT
  :
  NUMERIC ALPHANUMERIC?
  ;

ID_FRAGMENT
  :
  (
    'a'..'z' | 'A'..'Z'
  ) 
  (
    ALPHANUMERIC
    | '_'
    | '-'
    | '+'
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

