package org.demonsoft.spatialkappa.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class VariableExpression implements Serializable {

    private static final long serialVersionUID = 1L;

    public static enum Operator {
        PLUS("+") {
            @Override
            ObservationElement eval(ObservationElement x, ObservationElement y) {
                return x.plus(y);
            }
            @Override
            int eval(int x, int y) {
                return x + y;
            }
            @Override
            float eval(float x, float y) {
                return x + y;
            }
        },
        MINUS("-") {
            @Override
            ObservationElement eval(ObservationElement x, ObservationElement y) {
                return x.minus(y);
            }
            @Override
            int eval(int x, int y) {
                return x - y;
            }
            @Override
            float eval(float x, float y) {
                return x - y;
            }
        },
        MULTIPLY("*") {
            @Override
            ObservationElement eval(ObservationElement x, ObservationElement y) {
                return x.times(y);
            }
            @Override
            int eval(int x, int y) {
                return x * y;
            }
            @Override
            float eval(float x, float y) {
                return x * y;
            }
        },
        DIVIDE("/") {
            @Override
            ObservationElement eval(ObservationElement x, ObservationElement y) {
                return x.dividedBy(y);
            }
            @Override
            int eval(int x, int y) {
                return x / y;
            }
            @Override
            float eval(float x, float y) {
                return x / y;
            }
        },
        // Used by cell index syntax
        MODULUS_SYMBOL("%") {
            @Override
            ObservationElement eval(ObservationElement x, ObservationElement y) {
                return x.modulus(y);
            }
            @Override
            int eval(int x, int y) {
                return x % y;
            }
            @Override
            float eval(float x, float y) {
                return x % y;
            }
        },
        MODULUS("[mod]") {
            @Override
            ObservationElement eval(ObservationElement x, ObservationElement y) {
                return x.modulus(y);
            }
            @Override
            int eval(int x, int y) {
                return x % y;
            }
            @Override
            float eval(float x, float y) {
                return x % y;
            }
        },
        POWER("^") {
            @Override
            ObservationElement eval(ObservationElement x, ObservationElement y) {
                return x.exponent(y);
            }
            @Override
            int eval(int x, int y) {
                return (int) Math.pow(x, y);
            }
            @Override
            float eval(float x, float y) {
                return (float) Math.pow(x, y);
            }
        };

        private final String text;
        
        Operator(String text) {
            this.text = text;
        }
        
        @Override
        public String toString() {
            return text;
        }

        abstract ObservationElement eval(ObservationElement x, ObservationElement y);
        
        abstract int eval(int x, int y);
        
        abstract float eval(float x, float y);
        
        public static Operator getOperator(String input) {
            for (Operator current : Operator.values()) {
                if (current.text.equals(input)) {
                    return current;
                }
            }
            throw new IllegalArgumentException(input);
        }
    }

    public static enum UnaryOperator {
        LOG("[log]") {
            @Override
            ObservationElement eval(ObservationElement x) {
                return x.log();
            }
            @Override
            float eval(float x) {
                return (float) Math.log(x);
            }
        },
        SIN("[sin]") {
            @Override
            ObservationElement eval(ObservationElement x) {
                return x.sin();
            }
            @Override
            float eval(float x) {
                return (float) Math.sin(x);
            }
        },
        COS("[cos]") {
            @Override
            ObservationElement eval(ObservationElement x) {
                return x.cos();
            }
            @Override
            float eval(float x) {
                return (float) Math.cos(x);
            }
        },
        TAN("[tan]") {
            @Override
            ObservationElement eval(ObservationElement x) {
                return x.tan();
            }
            @Override
            float eval(float x) {
                return (float) Math.tan(x);
            }
        },
        SQRT("[sqrt]") {
            @Override
            ObservationElement eval(ObservationElement x) {
                return x.sqrt();
            }
            @Override
            float eval(float x) {
                return (float) Math.sqrt(x);
            }
        },
        EXP("[exp]") {
            @Override
            ObservationElement eval(ObservationElement x) {
                return x.exp();
            }
            @Override
            float eval(float x) {
                return (float) Math.exp(x);
            }
        };

        private final String text;
        
        UnaryOperator(String text) {
            this.text = text;
        }
        
        @Override
        public String toString() {
            return text;
        }

        abstract ObservationElement eval(ObservationElement x);

        abstract float eval(float x);
    }

    public static enum Constant {
        INFINITY("[inf]", Float.POSITIVE_INFINITY),
        PI("[pi]", (float) Math.PI);
        
        private final String text;
        public final float value;
        
        Constant(String text, float value) {
            this.text = text;
            this.value = value;
        }
        
        @Override
        public String toString() {
            return text;
        }
    }

    public static enum SimulationToken {
        EVENTS {
            @Override
            public String toString() {
                return "[E]";
            }
        },
        TIME {
            @Override
            public String toString() {
                return "[T]";
            }
        },
    }

    public static enum Type {
        NUMBER, BINARY_EXPRESSION, UNARY_EXPRESSION, VARIABLE_REFERENCE, CONSTANT, SIMULATION_TOKEN, AGENT_GROUP
    }

    private static final ComplexMatcher MATCHER = new ComplexMatcher();
    
    public final Type type;
    public final VariableReference reference;
    protected float value;
    protected Operator operator;
    private UnaryOperator unaryOperator;
    protected VariableExpression lhsExpression;
    protected VariableExpression rhsExpression;
    private SimulationToken simulationToken;
    private Constant constant;
    private final List<Complex> complexes;
    
    public VariableExpression(String input) {
        if (input == null) {
            throw new NullPointerException();
        }
        if (input.length() == 0) {
            throw new IllegalArgumentException(input);
        }
        reference = null;
        value = Float.parseFloat(input);
        type = Type.NUMBER;
        complexes = null;
    }

    public VariableExpression(float input) {
        reference = null;
        value = input;
        type = Type.NUMBER;
        complexes = null;
    }

    public VariableExpression(VariableExpression expr1, Operator operator, VariableExpression expr2) {
        if (expr1 == null || operator == null || expr2 == null) {
            throw new NullPointerException();
        }
        reference = null;
        this.operator = operator;
        lhsExpression = expr1;
        rhsExpression = expr2;
        type = Type.BINARY_EXPRESSION;
        complexes = null;
    }

    public VariableExpression(VariableReference reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        this.reference = reference;
        type = Type.VARIABLE_REFERENCE;
        complexes = null;
    }

    public VariableExpression(Constant constant) {
        if (constant == null) {
            throw new NullPointerException();
        }
        reference = null;
        this.constant = constant;
        type = Type.CONSTANT;
        complexes = null;
    }

    public VariableExpression(SimulationToken token) {
        if (token == null) {
            throw new NullPointerException();
        }
        reference = null;
        simulationToken = token;
        type = Type.SIMULATION_TOKEN;
        complexes = null;
    }

    public VariableExpression(UnaryOperator unaryOperator, VariableExpression expr) {
        if (unaryOperator == null || expr == null) {
            throw new NullPointerException();
        }
        reference = null;
        this.unaryOperator = unaryOperator;
        lhsExpression = expr;
        type = Type.UNARY_EXPRESSION;
        complexes = null;
    }

    public VariableExpression(List<Agent> agents, Location location) {
        if (agents == null || location == null) {
            throw new NullPointerException();
        }
        if (agents.size() == 0) {
            throw new IllegalArgumentException("Agent group is empty");
        }
        complexes = Utils.getComplexes(agents);
        Utils.propogateLocation(agents, location);
        reference = null;
        type = Type.AGENT_GROUP;
    }

    @Override
    public String toString() {
        switch (type) {
        case BINARY_EXPRESSION:
            return "(" + lhsExpression + " " + operator + " " + rhsExpression + ")";
            
        case CONSTANT:
            return constant.toString();
            
        case NUMBER:
            return "" + value;
            
        case SIMULATION_TOKEN:
            return simulationToken.toString();
            
        case UNARY_EXPRESSION:
            return unaryOperator + " (" + lhsExpression + ")";
            
        case VARIABLE_REFERENCE:
            return reference.toString();
            
        case AGENT_GROUP:
            return "|" + complexes.toString() + "|";
            
        default:
            throw new IllegalStateException("Unknown expression");
        }
        
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((constant == null) ? 0 : constant.hashCode());
        result = prime * result + ((lhsExpression == null) ? 0 : lhsExpression.hashCode());
        result = prime * result + ((operator == null) ? 0 : operator.hashCode());
        result = prime * result + ((reference == null) ? 0 : reference.hashCode());
        result = prime * result + ((rhsExpression == null) ? 0 : rhsExpression.hashCode());
        result = prime * result + ((simulationToken == null) ? 0 : simulationToken.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((unaryOperator == null) ? 0 : unaryOperator.hashCode());
        result = prime * result + Float.floatToIntBits(value);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        VariableExpression other = (VariableExpression) obj;
        if (constant != other.constant)
            return false;
        if (lhsExpression == null) {
            if (other.lhsExpression != null)
                return false;
        }
        else if (!lhsExpression.equals(other.lhsExpression))
            return false;
        if (operator != other.operator)
            return false;
        if (reference == null) {
            if (other.reference != null)
                return false;
        }
        else if (!reference.equals(other.reference))
            return false;
        if (rhsExpression == null) {
            if (other.rhsExpression != null)
                return false;
        }
        else if (!rhsExpression.equals(other.rhsExpression))
            return false;
        if (simulationToken != other.simulationToken)
            return false;
        if (type != other.type)
            return false;
        if (unaryOperator != other.unaryOperator)
            return false;
        if (Float.floatToIntBits(value) != Float.floatToIntBits(other.value))
            return false;
        return true;
    }

    public ObservationElement evaluate(SimulationState state) {
        if (state == null) {
            throw new NullPointerException();
        }
        switch (type) {
        case BINARY_EXPRESSION:
            return operator.eval(lhsExpression.evaluate(state), rhsExpression.evaluate(state));
            
        case CONSTANT:
            if (constant == Constant.INFINITY) {
                throw new IllegalStateException();
            }
            return new ObservationElement(constant.value);
            
        case NUMBER:
            return new ObservationElement(value);
            
        case SIMULATION_TOKEN:
            switch (simulationToken) {
            case EVENTS:
                return new ObservationElement(state.getEventCount());

            case TIME:
                return new ObservationElement(state.getTime());

            default:
                throw new IllegalStateException("Unknown expression");
            }
            
        case UNARY_EXPRESSION:
            return unaryOperator.eval(lhsExpression.evaluate(state));
            
        case VARIABLE_REFERENCE:
            Variable target = state.getVariable(reference.variableName);
            if (target == null) {
                throw new IllegalArgumentException("Missing value: " + reference);
            }
            return target.evaluate(state);
            
        case AGENT_GROUP:
            return new ObservationElement(0);
            
        default:
            throw new IllegalStateException("Unknown expression");
        }
    }

    public float evaluate(Map<String, Variable> variables) {
        if (variables == null) {
            throw new NullPointerException();
        }
        switch (type) {
        case BINARY_EXPRESSION:
            return operator.eval(lhsExpression.evaluate(variables), rhsExpression.evaluate(variables));
            
        case CONSTANT:
            return constant.value;
            
        case NUMBER:
            return value;
            
        case SIMULATION_TOKEN:
        case AGENT_GROUP:
            throw new IllegalStateException();
            
        case UNARY_EXPRESSION:
            return unaryOperator.eval(lhsExpression.evaluate(variables));
            
        case VARIABLE_REFERENCE:
            Variable target = variables.get(reference.variableName);
            if (target == null) {
                throw new IllegalArgumentException("Missing value: " + reference);
            }
            return target.evaluate(variables);
            
        default:
            throw new IllegalStateException("Unknown expression");
        }
    }

    public ObservationElement evaluate(SimulationState state, TransitionInstance transitionInstance) {
        if (state == null || transitionInstance == null) {
            throw new NullPointerException();
        }
        switch (type) {
        case BINARY_EXPRESSION:
            return operator.eval(lhsExpression.evaluate(state, transitionInstance), rhsExpression.evaluate(state, transitionInstance));
            
        case CONSTANT:
            if (constant == Constant.INFINITY) {
                throw new IllegalStateException();
            }
            return new ObservationElement(constant.value);
            
        case NUMBER:
            return new ObservationElement(value);
            
        case SIMULATION_TOKEN:
            switch (simulationToken) {
            case EVENTS:
                return new ObservationElement(state.getEventCount());

            case TIME:
                return new ObservationElement(state.getTime());

            default:
                throw new IllegalStateException("Unknown expression");
            }
            
        case UNARY_EXPRESSION:
            return unaryOperator.eval(lhsExpression.evaluate(state, transitionInstance));
            
        case VARIABLE_REFERENCE:
            Variable target = state.getVariable(reference.variableName);
            if (target == null) {
                throw new IllegalArgumentException("Missing value: " + reference);
            }
            return target.evaluate(state); // TODO handle agent rates in variables ?
            
        case AGENT_GROUP:
            return new ObservationElement(getAgentGroupCountInTransitionInstance(transitionInstance));
            
            
        default:
            throw new IllegalStateException("Unknown expression");
        }

    }
    

    private int getAgentGroupCountInTransitionInstance(TransitionInstance transitionInstance) {
        int result = 0;
        for (Complex complex : complexes) {
            for (ComplexMapping complexMapping : transitionInstance.sourceMapping) {
                result += MATCHER.getPartialMatches(complex, complexMapping.target).size();
            }
        }
        return result;
    }

    public boolean isInfinite(Map<String, Variable> variables) {
        if (variables == null) {
            throw new NullPointerException();
        }
        switch (type) {
        case BINARY_EXPRESSION:
            return lhsExpression.isInfinite(variables) || rhsExpression.isInfinite(variables);
            
        case CONSTANT:
            return constant == Constant.INFINITY;
            
        case NUMBER:
        case SIMULATION_TOKEN:
        case AGENT_GROUP:
            return false;
            
        case UNARY_EXPRESSION:
            return lhsExpression.isInfinite(variables);
            
        case VARIABLE_REFERENCE:
            if (!variables.containsKey(reference.variableName)) {
                throw new IllegalArgumentException("Missing value: " + reference);
            }
            Variable target = variables.get(reference.variableName);
            return target.type == Variable.Type.VARIABLE_EXPRESSION && target.expression.isInfinite(variables);
            
        default:
            throw new IllegalStateException("Unknown expression");
        }
    }


    public boolean isFixed(Map<String, Variable> variables) {
        if (variables == null) {
            throw new NullPointerException();
        }
        switch (type) {
        case BINARY_EXPRESSION:
            return lhsExpression.isFixed(variables) && rhsExpression.isFixed(variables);
            
        case CONSTANT:
        case NUMBER:
            return true;
            
        case SIMULATION_TOKEN:
        case AGENT_GROUP:
            return false;
            
        case UNARY_EXPRESSION:
            return lhsExpression.isFixed(variables);
            
        case VARIABLE_REFERENCE:
            if (!variables.containsKey(reference.variableName)) {
                throw new IllegalArgumentException("Missing value: " + reference);
            }
            return variables.get(reference.variableName).expression.isFixed(variables);
            
        default:
            throw new IllegalStateException("Unknown expression");
        }
    }

    public int evaluate(IKappaModel kappaModel) {
        if (kappaModel == null) {
            throw new NullPointerException();
        }
        switch (type) {
        case BINARY_EXPRESSION:
            return operator.eval(lhsExpression.evaluate(kappaModel), rhsExpression.evaluate(kappaModel));
            
        case CONSTANT:
            if (constant == Constant.INFINITY) {
                throw new IllegalStateException();
            }
            return (int) constant.value;
            
        case NUMBER:
            return (int) value;
            
        case UNARY_EXPRESSION:
            return (int) unaryOperator.eval(new ObservationElement(lhsExpression.evaluate(kappaModel))).value;
            
        case VARIABLE_REFERENCE:
            Variable target = kappaModel.getVariables().get(reference.variableName);
            if (target == null) {
                throw new IllegalArgumentException("Missing value: " + reference);
            }
            return target.evaluate(kappaModel);
            
        case AGENT_GROUP:
            return 0;
            
        default:
            throw new IllegalStateException("Unknown expression");
        }
    }

}
