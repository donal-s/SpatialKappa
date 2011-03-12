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
import org.demonsoft.spatialkappa.model.BooleanExpression;
import org.demonsoft.spatialkappa.model.CellIndexExpression;
import org.demonsoft.spatialkappa.model.CompartmentLink;
import org.demonsoft.spatialkappa.model.Direction;
import org.demonsoft.spatialkappa.model.Location;
import org.demonsoft.spatialkappa.model.IKappaModel;
import org.demonsoft.spatialkappa.model.KappaModel;
import org.demonsoft.spatialkappa.model.Perturbation;
import org.demonsoft.spatialkappa.model.PerturbationEffect;
import org.demonsoft.spatialkappa.model.VariableExpression;
import org.demonsoft.spatialkappa.model.VariableExpression.Constant;
import org.demonsoft.spatialkappa.model.VariableExpression.SimulationToken;
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
  List<CellIndexExpression> dimensions = new ArrayList<CellIndexExpression>();
  }
  :
  ^(LOCATION name=label (compartmentIndexExpr {dimensions.add($compartmentIndexExpr.result);})*)
  {
    $result = new Location($name.result, dimensions);
  }
  ;

compartmentIndexExpr returns [CellIndexExpression result]
  :
  ^(INDEX cellIndexExpr)
  {
    $result = $cellIndexExpr.result;
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
  | ^(VAR_EXPR PI)  
  {
    $result = new VariableExpression(Constant.PI);
  }  
  | ^(VAR_EXPR TIME)
  {
    $result = new VariableExpression(SimulationToken.TIME);
  }  
  | ^(VAR_EXPR EVENTS)
  {
    $result = new VariableExpression(SimulationToken.EVENTS);
  }  
  | ^(VAR_EXPR LOG a=varAlgebraExpr)
  {
    $result = new VariableExpression(VariableExpression.UnaryOperator.LOG, $a.result);
  }  
  | ^(VAR_EXPR SIN a=varAlgebraExpr)
  {
    $result = new VariableExpression(VariableExpression.UnaryOperator.SIN, $a.result);
  }  
  | ^(VAR_EXPR COS a=varAlgebraExpr)
  {
    $result = new VariableExpression(VariableExpression.UnaryOperator.COS, $a.result);
  }  
  | ^(VAR_EXPR TAN a=varAlgebraExpr)
  {
    $result = new VariableExpression(VariableExpression.UnaryOperator.TAN, $a.result);
  }  
  | ^(VAR_EXPR SQRT a=varAlgebraExpr)
  {
    $result = new VariableExpression(VariableExpression.UnaryOperator.SQRT, $a.result);
  }  
  | ^(VAR_EXPR EXP a=varAlgebraExpr)
  {
    $result = new VariableExpression(VariableExpression.UnaryOperator.EXP, $a.result);
  }  
  | ^(VAR_EXPR MODULUS a=varAlgebraExpr b=varAlgebraExpr)
  {
    $result = new VariableExpression($a.result, VariableExpression.Operator.MODULUS, $b.result);
  }  
  ;

modExpr
  :
  ^(PERTURBATION ^(CONDITION booleanExpression) effect untilExpression?)
  {
    kappaModel.addPerturbation(new Perturbation($booleanExpression.result, $effect.result, $untilExpression.result));
  }
  ;

booleanExpression returns [BooleanExpression result]
  :
  ^(BOOL_EXPR AND a=booleanExpression b=booleanExpression)
  {
    $result = new BooleanExpression(BooleanExpression.Operator.AND, $a.result, $b.result);
  }    
  | ^(BOOL_EXPR OR a=booleanExpression b=booleanExpression)
  {
    $result = new BooleanExpression(BooleanExpression.Operator.OR, $a.result, $b.result);
  }    
  | ^(BOOL_EXPR NOT a=booleanExpression)
  {
    $result = new BooleanExpression(BooleanExpression.Operator.NOT, $a.result);
  }    
  | ^(BOOL_EXPR TRUE)
  {
    $result = new BooleanExpression(true);
  }    
  | ^(BOOL_EXPR FALSE)
  {
    $result = new BooleanExpression(false);
  }    
  | ^(BOOL_EXPR op=relationalOperator c=varAlgebraExpr d=varAlgebraExpr)
  {
    $result = new BooleanExpression(BooleanExpression.RelationalOperator.getOperator($op.text), $c.result, $d.result);
  }    
  ;
  


relationalOperator
  :
  '<' | '>' | '='
  ;

effect returns [PerturbationEffect result]
  :
  ^(EFFECT SNAPSHOT)
  {
    $result = PerturbationEffect.SNAPSHOT;
  }    
  | ^(EFFECT STOP)
  {
    $result = PerturbationEffect.STOP;
  }    
  | ^(EFFECT ADD varAlgebraExpr agentGroup)
  {
    $result = new PerturbationEffect(PerturbationEffect.Type.ADD, $varAlgebraExpr.result, $agentGroup.result);
  }    
  | ^(EFFECT REMOVE varAlgebraExpr agentGroup)
  {
    $result = new PerturbationEffect(PerturbationEffect.Type.REMOVE, $varAlgebraExpr.result, $agentGroup.result);
  }    
  | ^(EFFECT SET ^(TARGET label) varAlgebraExpr)
  {
    $result = new PerturbationEffect($label.result, $varAlgebraExpr.result);
  }    
  ;
  
  
untilExpression returns [BooleanExpression result]
  :
  ^(UNTIL booleanExpression)
  {
    $result = $booleanExpression.result;
  }
  ;
  

cellIndexExpr returns [CellIndexExpression result]
  :
  ^(CELL_INDEX_EXPR operator a=cellIndexExpr b=cellIndexExpr)
  {
    $result = new CellIndexExpression($a.result, CellIndexExpression.Operator.getOperator($operator.text), $b.result);
  }    
  | ^(CELL_INDEX_EXPR INT)
  {
    $result = new CellIndexExpression($INT.text);
  }    
  | ^(CELL_INDEX_EXPR label)
  {
    $result = new CellIndexExpression(new VariableReference($label.result));
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
  

