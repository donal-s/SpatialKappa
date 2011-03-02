tree grammar SpatialKappaWalker;

options {
  language     = Java;
  tokenVocab   = SpatialKappa;
  ASTLabelType = CommonTree;
}

@header {
package org.demonsoft.spatialkappa.parser;

import org.demonsoft.spatialkappa.model.Agent;
import org.demonsoft.spatialkappa.model.AgentSite;
import org.demonsoft.spatialkappa.model.CompartmentLink;
import org.demonsoft.spatialkappa.model.Direction;
import org.demonsoft.spatialkappa.model.Location;
import org.demonsoft.spatialkappa.model.MathExpression;
import org.demonsoft.spatialkappa.model.KappaModel;
import org.demonsoft.spatialkappa.model.Perturbation;
import org.demonsoft.spatialkappa.model.Perturbation.Assignment;
import org.demonsoft.spatialkappa.model.Perturbation.ConcentrationExpression;
import org.demonsoft.spatialkappa.model.Perturbation.Condition;
import org.demonsoft.spatialkappa.model.Perturbation.Inequality;
}

@members { 
KappaModel kappaModel = new KappaModel(); 
}

prog returns [KappaModel result]
  :
  {
    $result = kappaModel;
  }
  (
    line
  )+
  ;

line
  :
  ruleExpr
  | compartmentExpr
  | compartmentLinkExpr
  | transportExpr
  | initExpr
  | obsExpr
  | varExpr
  | modExpr
  ;

ruleExpr
  :
  ^(TRANSFORM a=transformExpr b=transitionKineticExpr LABEL?)
  {
    kappaModel.addTransform($a.direction, $LABEL.text, $a.lhs, $a.rhs, $b.rate1, $b.rate2, null);
  }
  | ^(TRANSFORM a=transformExpr b=transitionKineticExpr LABEL c=locationExpr)
  {
    kappaModel.addTransform($a.direction, $LABEL.text, $a.lhs, $a.rhs, $b.rate1, $b.rate2, $c.result );
  }
  ;

transformExpr returns [Direction direction, List<Agent> lhs, List<Agent> rhs]
  :
  ^(
    t=transformTransition
    ^(LHS a=agentGroup?)
    ^(RHS b=agentGroup?)
   )
  {
    $direction = $t.result;
    $lhs = $a.result;
    $rhs = $b.result;
  }
  | ^(
    t=transformTransition
    LHS
    ^(RHS b=agentGroup?)
   )
  {
    $direction = $t.result;
    $lhs = null;
    $rhs = $b.result;
  }
  | ^(
    t=transformTransition
    ^(LHS a=agentGroup?)
    RHS
   )
  {
    $direction = $t.result;
    $lhs = $a.result;
    $rhs = null;
  }
  ;

agentGroup returns [List<Agent> result]
  @init {
  List<Agent> agents = new ArrayList<Agent>();
  }
  :
  ^(
    AGENTS
    (
      a=agent {agents.add($a.result);}
    )+
   )
  {    
  $result = agents;
  }
  ;

agent returns [Agent result]
  @init {
  List<AgentSite> sites = new ArrayList<AgentSite>();
  }
  :
  ^(
    AGENT id
    (
      iface {sites.add($iface.result);}
    )*
   )
  {
    $result = new Agent($id.text, sites);
  }
  ;

iface returns [AgentSite result]
  :
  ^(INTERFACE id a=stateExpr? b=linkExpr?)
  {
    $result = new AgentSite($id.text, $a.result, $b.result);
  }
  ;

stateExpr returns [String result]
  :
  ^(STATE marker)
  {
    $result = $marker.text;
  }
  ;

linkExpr returns [String result]
  :
  ^(LINK INT)
  {$result = $INT.text;}
  |
  ^(LINK OCCUPIED)
  {$result = "_";}
  |
  ^(LINK ANY)
  {$result = "?";}
  ;

transitionKineticExpr returns [String rate1, String rate2]
  :
  ^(RATE a=rateValueExpr (b=rateValueExpr)?)
  {
    $rate1 = $a.text;
    $rate2 = $b.text;
  }
  ;

rateValueExpr returns [String rateValue]
  :
  ^(RATEVALUE number)
  {
    $rateValue = $number.text;
  }
  | ^(RATEVALUE INFINITE_RATE)
  {
    $rateValue = "$" + "INF";
  }
  ;


initExpr
  :
  ^(INIT agentGroup INT)
  {
    kappaModel.addInitialValue($agentGroup.result, $INT.text, null);
  }
  |
  ^(INIT agentGroup INT locationExpr)
  {
    kappaModel.addInitialValue($agentGroup.result, $INT.text, $locationExpr.result);
  }
  ;
  
compartmentExpr
  @init {
  List<Integer> dimensions = new ArrayList<Integer>();
  }
  :
  ^(COMPARTMENT LABEL (^(DIMENSION INT {dimensions.add(Integer.parseInt($INT.text));}))*)
  {
    kappaModel.addCompartment($LABEL.text, dimensions);
  }
  ;
  
compartmentLinkExpr
  :
  ^(COMPARTMENT_LINK linkName=LABEL sourceCompartment=locationExpr transportTransition targetCompartment=locationExpr)
  {
    kappaModel.addCompartmentLink(new CompartmentLink($linkName.text, $sourceCompartment.result, $targetCompartment.result, $transportTransition.result));
  }
  ;
  
transportExpr
  :
  ^(TRANSPORT linkName=LABEL agentGroup? b=transportKineticExpr (transportName=LABEL)?)
  {
    kappaModel.addTransport($transportName.text, $linkName.text, $agentGroup.result, $b.result);
  }
  ;
  
transportKineticExpr returns [String result]
  :
  ^(RATE rateValueExpr)
  {
    $result = $rateValueExpr.text;
  }
  ;

locationExpr returns [Location result]
  @init {
  List<MathExpression> dimensions = new ArrayList<MathExpression>();
  }
  :
  ^(LOCATION name=LABEL (compartmentIndexExpr {dimensions.add($compartmentIndexExpr.result);})*)
  {
    $result = new Location($LABEL.text, dimensions);
  }
  ;

compartmentIndexExpr returns [MathExpression result]
  :
  ^(INDEX mathExpr)
  {
    $result = $mathExpr.result;
  }
  ;



obsExpr
  :
  ^(OBSERVATION agentGroup LABEL?)
  {
    kappaModel.addObservable($agentGroup.result, $LABEL.text, null, true);
  }
  | ^(OBSERVATION agentGroup LABEL locationExpr)
  {
    kappaModel.addObservable($agentGroup.result, $LABEL.text, $locationExpr.result, true);
  }
  |
  ^(OBSERVATION LABEL)
  {
    kappaModel.addObservable($LABEL.text);
  }  
  ;

varExpr
  :
  ^(VARIABLE agentGroup LABEL)
  {
    kappaModel.addObservable($agentGroup.result, $LABEL.text, null, false);
  }
  ;


modExpr
  :
  ^(PERTURBATION concentrationInequality assignment)
  {
    kappaModel.addPerturbation(new Perturbation($concentrationInequality.result, $assignment.result));
  }
  | ^(PERTURBATION timeInequality assignment)
  {
    kappaModel.addPerturbation(new Perturbation($timeInequality.result, $assignment.result));
  }
  ;
  
timeInequality returns [String result]
  :
  ^(TIME_INEQUALITY number)
  {
    $result = $number.text;
  }
  ;
  
concentrationInequality returns [Condition result]
options {backtrack=true;}
  :
  ^(CONCENTRATION_INEQUALITY a=concentrationExpression GREATER_THAN b=concentrationExpression)
  {
    $result = new Condition($a.result, Inequality.GREATER_THAN, $b.result);
  }
  | ^(CONCENTRATION_INEQUALITY a=concentrationExpression LESS_THAN b=concentrationExpression)
  {
    $result = new Condition($a.result, Inequality.LESS_THAN, $b.result);
  };
  

assignment returns [Assignment result]
  :
  ^(ASSIGNMENT LABEL INFINITE_RATE)
  {
    $result = new Assignment($LABEL.text, ConcentrationExpression.INFINITE_RATE);
  }
  | ^(ASSIGNMENT LABEL concentrationExpression)
  {
    $result = new Assignment($LABEL.text, $concentrationExpression.result);
  }
  ;
  

concentrationExpression returns [ConcentrationExpression result]
  :
  ^(CONCENTRATION_EXPRESSION LABEL operator a=concentrationExpression)
  {
    $result = new ConcentrationExpression($LABEL.text, $operator.text, $a.result);
  }
  | ^(CONCENTRATION_EXPRESSION number operator a=concentrationExpression)
  {
    $result = new ConcentrationExpression(Float.parseFloat($number.text), $operator.text, $a.result);
  }
  | ^(CONCENTRATION_EXPRESSION LABEL)
  {
    $result = new ConcentrationExpression($LABEL.text);
  }
  | ^(CONCENTRATION_EXPRESSION number)
  {
    $result = new ConcentrationExpression(Float.parseFloat($number.text));
  }
  ;


mathExpr returns [MathExpression result]
  :
  ^(MATH_EXPR operator a=mathExpr b=mathExpr)
  {
    $result = new MathExpression($a.result, MathExpression.Operator.getOperator($operator.text), $b.result);
  }    
  | ^(MATH_EXPR INT)
  {
    $result = new MathExpression($INT.text);
  }    
  | ^(MATH_EXPR VARIABLE_NAME)
  {
    $result = new MathExpression($VARIABLE_NAME.text);
  }    
  ;


id returns [String result]
  :
  ID
    { $result = $ID.text;}
  ;

marker
  :
  ( VARIABLE_NAME | ALPHANUMERIC | INT )
  ;

number
  :
  ( INT | FLOAT )
  ;
    
operator
  :
  ( '+' | '*' | '-' | '/' | '%' )
  ;

transformTransition returns [Direction result]
  :
  ( 
    FORWARD_TRANSITION       { $result = Direction.FORWARD; }
    | EQUILIBRIUM_TRANSITION { $result = Direction.BIDIRECTIONAL; }
  )
  ;
  
transportTransition returns [Direction result]
  :
  ( 
    FORWARD_TRANSITION       { $result = Direction.FORWARD; }
    | BACKWARD_TRANSITION    { $result = Direction.BACKWARD; }
    | EQUILIBRIUM_TRANSITION { $result = Direction.BIDIRECTIONAL; }
  )
  ;
  

