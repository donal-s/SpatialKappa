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
import org.demonsoft.spatialkappa.model.AgentDeclaration;
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
  agentDecl
  | compartmentDecl
  | channelDecl
  | initDecl
  | plotDecl
  | obsDecl
  | varDecl
  | modDecl
  | ruleDecl
  ;

ruleDecl
  :
  ^(RULE a=transition b=rate label?)
  {
    kappaModel.addTransition($label.result, $a.lhsLocation, $a.lhs, $a.channel, $a.rhsLocation, $a.rhs, $b.rate);
  }
  ;

transition returns [Location lhsLocation, List<Agent> lhs, Location rhsLocation, List<Agent> rhs, String channel]
  :
  ^(
    TRANSITION
    ^(LHS (source=location {$lhsLocation = $source.result;})? (a=agentGroup {$lhsLocation = $a.location;})?)
    ^(RHS (target=location {$rhsLocation = $target.result;})? (b=agentGroup {$rhsLocation = $b.location;})?)
    (^(CHANNEL channelName=id))?
   )
  {
    $lhs = $a.agents;
    $rhs = $b.agents;
    if ($lhsLocation == null) {
      $lhsLocation = Location.NOT_LOCATED;
    }
    if ($rhsLocation == null) {
      $rhsLocation = Location.NOT_LOCATED;
    }
    $channel = $channelName.text;
  }
  ;

agentGroup returns [List<Agent> agents, Location location]
  @init {
  List<Agent> agents = new ArrayList<Agent>();
  }
  :
  ^(
    AGENTS location?
    (
      a=agent {agents.add($a.result);}
    )+
   )
  {    
  $location = ($location.result != null) ? $location.result : Location.NOT_LOCATED;
  $agents = agents;
  }
  ;

agent returns [Agent result]
  @init {
  List<AgentSite> sites = new ArrayList<AgentSite>();
  }
  :
  ^(
    AGENT id location?
    (
      agentInterface {sites.add($agentInterface.result);}
    )*
   )
  {
    if ($location.result != null) {
      $result = new Agent($id.text, $location.result, sites);
    }
    else {
      $result = new Agent($id.text, Location.NOT_LOCATED, sites);
    }
  }
  ;

agentInterface returns [AgentSite result]
  :
  ^(INTERFACE id a=state? b=link?)
  {
    $result = new AgentSite($id.text, $a.result, $b.linkName, $b.channelName);
  }
  ;

state returns [String result]
  :
  ^(STATE id)
  {
    $result = $id.text;
  }
  ;

link returns [String linkName, String channelName]
  :
  ^(LINK (^(CHANNEL channelId=id))? INT)
  {$linkName = $INT.text; $channelName = $channelId.text;}
  |
  ^(LINK (^(CHANNEL channelId=id))? OCCUPIED)
  {$linkName = "_"; $channelName = $channelId.text;}
  |
  ^(LINK ANY)
  {$linkName = "?"; $channelName = null;}
  ;

rate returns [VariableExpression rate]
  :
  ^(RATE a=varAlgebraExpr)
  {
    $rate = $a.result;
  }
  ;

initDecl
options {backtrack=true;}
  :
  ^(INIT agentGroup INT)
  {
    kappaModel.addInitialValue($agentGroup.agents, $INT.text, $agentGroup.location);
  }
  |
  ^(INIT agentGroup label)
  {
    kappaModel.addInitialValue($agentGroup.agents, new VariableReference($label.result), $agentGroup.location);
  }
  ;
  
agentDecl
  @init {
  List<AggregateSite> sites = new ArrayList<AggregateSite>();
  }
  :
  ^(AGENT_DECL id (
      agentDeclInterface {sites.add($agentDeclInterface.result);}
    )*
  )
  {
    kappaModel.addAgentDeclaration(new AgentDeclaration($id.text, sites));
  }
  ;
  
agentDeclInterface returns [AggregateSite result]
  @init {
  List<String> states = new ArrayList<String>();
  }
  :
  ^(INTERFACE id (
      state {states.add($state.result);}
    )*
  )
  {
    $result = new AggregateSite($id.text, states, null);
  }
  ;

  
compartmentDecl
  @init {
  List<Integer> dimensions = new ArrayList<Integer>();
  }
  :
  ^(COMPARTMENT name=id (^(TYPE type=id))? (^(DIMENSION INT {dimensions.add(Integer.parseInt($INT.text));}))*)
  {
    kappaModel.addCompartment($name.result, $type.result, dimensions);
  }
  ;
  
channelDecl
  @init {
  Channel channel = null;
  }
  :
  ^(CHANNEL (linkName=id { channel = new Channel($linkName.text); }) (channel {channel.addChannelComponent($channel.channelType, $channel.source, $channel.target);})+)
  {
    kappaModel.addChannel(channel);
  }
  ;

channel returns [String channelType, List<Location> source, List<Location> target]
  :
  ^(LOCATION_PAIR (^(TYPE type=id))? sourceLocations=locations targetLocations=locations)
  {
    $channelType = $type.result; $source = $sourceLocations.locations; $target = $targetLocations.locations;
  }
  ;

locations returns [List<Location> locations]
  @init {
  locations = new ArrayList<Location>();
  }
  :
  ^(LOCATIONS (
      location {locations.add($location.result);}
    )+
  )
  ;

location returns [Location result]
  @init {
  List<CellIndexExpression> dimensions = new ArrayList<CellIndexExpression>();
  }
  :
  ^(LOCATION FIXED)
  {
    $result = Location.FIXED_LOCATION;
  }
  |
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


plotDecl
  :
  ^(PLOT label)
  {
    kappaModel.addPlot($label.result);
  }
  ;


obsDecl
options {backtrack=true;}
  :
  ^(OBSERVATION VOXEL agentGroup)
  {
    kappaModel.addVariable($agentGroup.agents, $agentGroup.agents.toString(), $agentGroup.location, true);
    kappaModel.addPlot($agentGroup.agents.toString());
  }
  |
  ^(OBSERVATION agentGroup)
  {
    kappaModel.addVariable($agentGroup.agents, $agentGroup.agents.toString(), $agentGroup.location, false);
    kappaModel.addPlot($agentGroup.agents.toString());
  }
  | ^(OBSERVATION VOXEL agentGroup label)
  {
    kappaModel.addVariable($agentGroup.agents, $label.result, $agentGroup.location, true);
    kappaModel.addPlot($label.result);
  }
  | ^(OBSERVATION agentGroup label)
  {
    kappaModel.addVariable($agentGroup.agents, $label.result, $agentGroup.location, false);
    kappaModel.addPlot($label.result);
  }
  ;

varDecl
  :
  ^(VARIABLE VOXEL agentGroup label)
  {
    kappaModel.addVariable($agentGroup.agents, $label.result, $agentGroup.location, true);
  }
  |
  ^(VARIABLE agentGroup label)
  {
    kappaModel.addVariable($agentGroup.agents, $label.result, $agentGroup.location, false);
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
  | ^(VAR_EXPR agentGroup)
  {
    $result = new VariableExpression($agentGroup.agents, $agentGroup.location);
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

modDecl
  :
  ^(PERTURBATION ^(CONDITION booleanExpression) effect until?)
  {
    kappaModel.addPerturbation(new Perturbation($booleanExpression.result, $effect.result, $until.result));
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
    $result = new PerturbationEffect(PerturbationEffect.Type.ADD, $varAlgebraExpr.result, $agentGroup.agents);
  }    
  | ^(EFFECT REMOVE varAlgebraExpr agentGroup)
  {
    $result = new PerturbationEffect(PerturbationEffect.Type.REMOVE, $varAlgebraExpr.result, $agentGroup.agents);
  }    
  | ^(EFFECT SET ^(TARGET label) varAlgebraExpr)
  {
    $result = new PerturbationEffect($label.result, $varAlgebraExpr.result);
  }    
  ;
  
  
until returns [BooleanExpression result]
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
