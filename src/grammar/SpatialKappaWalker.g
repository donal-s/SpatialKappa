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
import org.demonsoft.spatialkappa.model.IKappaModel;
import org.demonsoft.spatialkappa.model.KappaModel;
import org.demonsoft.spatialkappa.model.Perturbation;
import org.demonsoft.spatialkappa.model.Perturbation.Assignment;
import org.demonsoft.spatialkappa.model.Perturbation.ConcentrationExpression;
import org.demonsoft.spatialkappa.model.Perturbation.Condition;
import org.demonsoft.spatialkappa.model.Perturbation.Inequality;
import org.demonsoft.spatialkappa.model.VariableExpression;
import org.demonsoft.spatialkappa.model.VariableExpression.Constant;
import org.demonsoft.spatialkappa.model.VariableReference;
}

@members { 
IKappaModel kappaModel = new KappaModel();

// Hook for unit tests to override the model implementation
public void setKappaModel(IKappaModel kappaModel) {
  this.kappaModel = kappaModel;
} 
}

prog returns [IKappaModel result]
  @after {
    kappaModel.validate();
    $result = kappaModel;
  }
  :
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
  | plotExpr
  | obsExpr
  | varExpr
  | modExpr
  ;

ruleExpr
options {backtrack=true;}
  :
  ^(TRANSFORM a=transformExpr b=transitionKineticExpr label?)
  {
    kappaModel.addTransform($a.direction, $label.result, $a.lhs, $a.rhs, $b.rate1, $b.rate2, null);
  }
  | ^(TRANSFORM a=transformExpr b=transitionKineticExpr label c=locationExpr)
  {
    kappaModel.addTransform($a.direction, $label.result, $a.lhs, $a.rhs, $b.rate1, $b.rate2, $c.result );
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
  ^(STATE id)
  {
    $result = $id.text;
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

transitionKineticExpr returns [VariableExpression rate1, VariableExpression rate2]
  :
  ^(RATE a=varAlgebraExpr (b=varAlgebraExpr)?)
  {
    $rate1 = $a.result;
    $rate2 = $b.result;
  }
  ;

initExpr
  :
  ^(INIT agentGroup INT locationExpr?)
  {
    kappaModel.addInitialValue($agentGroup.result, $INT.text, $locationExpr.result);
  }
  |
  ^(INIT agentGroup label locationExpr?)
  {
    kappaModel.addInitialValue($agentGroup.result, new VariableReference($label.result), $locationExpr.result);
  }
  ;
  
compartmentExpr
  @init {
  List<Integer> dimensions = new ArrayList<Integer>();
  }
  :
  ^(COMPARTMENT label (^(DIMENSION INT {dimensions.add(Integer.parseInt($INT.text));}))*)
  {
    kappaModel.addCompartment($label.result, dimensions);
  }
  ;
  
compartmentLinkExpr
  :
  ^(COMPARTMENT_LINK linkName=label sourceCompartment=locationExpr transportTransition targetCompartment=locationExpr)
  {
    kappaModel.addCompartmentLink(new CompartmentLink($linkName.result, $sourceCompartment.result, $targetCompartment.result, $transportTransition.result));
  }
  ;
  
transportExpr
  :
  ^(TRANSPORT linkName=label agentGroup? b=transportKineticExpr (transportName=label)?)
  {
    kappaModel.addTransport($transportName.result, $linkName.result, $agentGroup.result, $b.result);
  }
  ;
  
transportKineticExpr returns [VariableExpression result]
  :
  ^(RATE varAlgebraExpr)
  {
    $result = $varAlgebraExpr.result;
  }
  ;

locationExpr returns [Location result]
  @init {
  List<MathExpression> dimensions = new ArrayList<MathExpression>();
  }
  :
  ^(LOCATION name=label (compartmentIndexExpr {dimensions.add($compartmentIndexExpr.result);})*)
  {
    $result = new Location($name.result, dimensions);
  }
  ;

compartmentIndexExpr returns [MathExpression result]
  :
  ^(INDEX mathExpr)
  {
    $result = $mathExpr.result;
  }
  ;


plotExpr
  :
  ^(PLOT label)
  {
    kappaModel.addPlot($label.result);
  }
  ;


obsExpr
  :
  ^(OBSERVATION agentGroup)
  {
    kappaModel.addVariable($agentGroup.result, $agentGroup.result.toString(), null);
    kappaModel.addPlot($agentGroup.result.toString());
  }
  | ^(OBSERVATION agentGroup label)
  {
    kappaModel.addVariable($agentGroup.result, $label.result, null);
    kappaModel.addPlot($label.result);
  }
  | ^(OBSERVATION agentGroup label locationExpr)
  {
    kappaModel.addVariable($agentGroup.result, $label.result, $locationExpr.result);
    kappaModel.addPlot($label.result);
  }
  ;

varExpr
  :
  ^(VARIABLE agentGroup label)
  {
    kappaModel.addVariable($agentGroup.result, $label.result, null);
  }
  |
  ^(VARIABLE varAlgebraExpr label)
  {
    kappaModel.addVariable($varAlgebraExpr.result, $label.result);
  }
  ;

varAlgebraExpr returns [VariableExpression result]
  :
  ^(VAR_EXPR operator a=varAlgebraExpr b=varAlgebraExpr)
  {
    $result = new VariableExpression($a.result, VariableExpression.Operator.getOperator($operator.text), $b.result);
  }    
  | ^(VAR_EXPR number)
  {
    $result = new VariableExpression($number.text);
  }    
  | ^(VAR_EXPR label)
  {
    $result = new VariableExpression(new VariableReference($label.result));
  }  
  | ^(VAR_EXPR VAR_INFINITY)  
  {
    $result = new VariableExpression(Constant.INFINITY);
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
  ^(ASSIGNMENT label VAR_INFINITY)
  {
    $result = new Assignment($label.result, ConcentrationExpression.INFINITE_RATE);
  }
  | ^(ASSIGNMENT label concentrationExpression)
  {
    $result = new Assignment($label.result, $concentrationExpression.result);
  }
  ;
  

concentrationExpression returns [ConcentrationExpression result]
  :
  ^(CONCENTRATION_EXPRESSION label operator a=concentrationExpression)
  {
    $result = new ConcentrationExpression($label.result, $operator.text, $a.result);
  }
  | ^(CONCENTRATION_EXPRESSION number operator a=concentrationExpression)
  {
    $result = new ConcentrationExpression(Float.parseFloat($number.text), $operator.text, $a.result);
  }
  | ^(CONCENTRATION_EXPRESSION label)
  {
    $result = new ConcentrationExpression($label.result);
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
  | ^(MATH_EXPR label)
  {
    $result = new MathExpression($label.result);
  }    
  ;


id returns [String result]
  :
  ID
    { $result = $ID.text;}
  ;

label returns [String result]
  :
  LABEL
    { $result = $LABEL.text;}
  ;

number
  :
  ( INT | FLOAT )
  ;
    
operator
  :
  ( '+' | '*' | '-' | '/' | '%' | '^' )
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
  

