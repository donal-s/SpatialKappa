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
import org.demonsoft.spatialkappa.model.AggregateAgent;
import org.demonsoft.spatialkappa.model.AggregateSite;
import org.demonsoft.spatialkappa.model.BooleanExpression;
import org.demonsoft.spatialkappa.model.CellIndexExpression;
import org.demonsoft.spatialkappa.model.Channel;
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
  )*
  ;

line
  :
  ruleExpr
  | compartmentExpr
  | channelDecl
  | initExpr
  | plotExpr
  | obsExpr
  | varExpr
  | modExpr
  | agentExpr
  ;

ruleExpr
options {backtrack=true;}
  :
  ^(RULE a=transformExpr b=kineticExpr label?)
  {
    kappaModel.addTransition($label.result, $a.lhsLocation, $a.lhs, $a.channel, $a.rhsLocation, $a.rhs, $b.rate);
  }
  ;

transformExpr returns [Location lhsLocation, List<Agent> lhs, Location rhsLocation, List<Agent> rhs, String channel]
options {backtrack=true;}
  :
  ^(
    TRANSITION
    ^(LHS source=locationExpr? a=agentGroup?)
    ^(RHS target=locationExpr? b=agentGroup?)
    (^(CHANNEL channelName=id))?
   )
  {
    $lhs = $a.result;
    $rhs = $b.result;
    $lhsLocation = ($source.result != null) ? $source.result : Location.NOT_LOCATED;
    $rhsLocation = ($target.result != null) ? $target.result : Location.NOT_LOCATED;
    $channel = $channelName.text;
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
    AGENT id locationExpr?
    (
      iface {sites.add($iface.result);}
    )*
   )
  {
    if ($locationExpr.result != null) {
      $result = new Agent($id.text, $locationExpr.result, sites);
    }
    else {
      $result = new Agent($id.text, Location.NOT_LOCATED, sites);
    }
  }
  ;

iface returns [AgentSite result]
  :
  ^(INTERFACE id a=stateExpr? b=linkExpr?)
  {
    $result = new AgentSite($id.text, $a.result, $b.linkName, $b.channelName);
  }
  ;

stateExpr returns [String result]
  :
  ^(STATE id)
  {
    $result = $id.text;
  }
  ;

linkExpr returns [String linkName, String channelName]
  :
  ^(LINK (^(CHANNEL channel=id))? INT)
  {$linkName = $INT.text; $channelName = $channel.text;}
  |
  ^(LINK (^(CHANNEL channel=id))? OCCUPIED)
  {$linkName = "_"; $channelName = $channel.text;}
  |
  ^(LINK ANY)
  {$linkName = "?"; $channelName = null;}
  ;

kineticExpr returns [VariableExpression rate]
  :
  ^(RATE a=varAlgebraExpr)
  {
    $rate = $a.result;
  }
  ;

initExpr
options {backtrack=true;}
  :
  ^(INIT agentGroup INT locationExpr?)
  {
    if ($locationExpr.result != null) {
      kappaModel.addInitialValue($agentGroup.result, $INT.text, $locationExpr.result);
    }
    else {
      kappaModel.addInitialValue($agentGroup.result, $INT.text, Location.NOT_LOCATED);
    }
  }
  |
  ^(INIT agentGroup label locationExpr?)
  {
    if ($locationExpr.result != null) {
      kappaModel.addInitialValue($agentGroup.result, new VariableReference($label.result), $locationExpr.result);
    }
    else {
      kappaModel.addInitialValue($agentGroup.result, new VariableReference($label.result), Location.NOT_LOCATED);
    }
  }
  ;
  
agentExpr
  @init {
  List<AggregateSite> sites = new ArrayList<AggregateSite>();
  }
  :
  ^(AGENT_DECL id (
      agentIfaceExpr {sites.add($agentIfaceExpr.result);}
    )*
  )
  {
    kappaModel.addAgentDeclaration(new AggregateAgent($id.text, sites));
  }
  ;
  
agentIfaceExpr returns [AggregateSite result]
  @init {
  List<String> states = new ArrayList<String>();
  }
  :
  ^(INTERFACE id (
      stateExpr {states.add($stateExpr.result);}
    )*
  )
  {
    $result = new AggregateSite($id.text, states, null);
  }
  ;

  
compartmentExpr
  @init {
  List<Integer> dimensions = new ArrayList<Integer>();
  }
  :
  ^(COMPARTMENT id (^(DIMENSION INT {dimensions.add(Integer.parseInt($INT.text));}))*)
  {
    kappaModel.addCompartment($id.result, dimensions);
  }
  ;
  
channelDecl
  @init {
  Channel channel = null;
  }
  :
  ^(CHANNEL (linkName=id { channel = new Channel($linkName.text); }) (channelExpr {channel.addLocationPair($channelExpr.source, $channelExpr.target);})+)
  {
    kappaModel.addChannel(channel);
  }
  ;

channelExpr returns [List<Location> source, List<Location> target]
  :
  ^(LOCATION_PAIR sourceCompartments=locationsExpr targetCompartments=locationsExpr)
  {
    $source = $sourceCompartments.locations; $target = $targetCompartments.locations;
  }
  ;

locationsExpr returns [List<Location> locations]
  @init {
  locations = new ArrayList<Location>();
  }
  :
  ^(LOCATIONS (
      locationExpr {locations.add($locationExpr.result);}
    )+
  )
  ;

locationExpr returns [Location result]
  @init {
  List<CellIndexExpression> dimensions = new ArrayList<CellIndexExpression>();
  }
  :
  ^(LOCATION name=id (compartmentIndexExpr {dimensions.add($compartmentIndexExpr.result);})*)
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
options {backtrack=true;}
  :
  ^(OBSERVATION agentGroup)
  {
    kappaModel.addVariable($agentGroup.result, $agentGroup.result.toString(), Location.NOT_LOCATED);
    kappaModel.addPlot($agentGroup.result.toString());
  }
  | ^(OBSERVATION agentGroup label)
  {
    kappaModel.addVariable($agentGroup.result, $label.result, Location.NOT_LOCATED);
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
    kappaModel.addVariable($agentGroup.result, $label.result, Location.NOT_LOCATED);
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
  | ^(CELL_INDEX_EXPR id)
  {
    $result = new CellIndexExpression(new VariableReference($id.text));
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
  

