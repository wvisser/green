package za.ac.sun.cs.green.service.simplify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.Constant;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.VisitorException;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.util.Reporter;

/**
 *
 * @author 19770235
 */
public class ConstantPropogation extends BasicService {

    private int invocations = 0;
    private HashMap<String, IntConstant> variables = new HashMap<>();

    public ConstantPropogation(Green solver) {
        super(solver);
    }

    @Override
    public Set<Instance> processRequest(Instance instance) {
        @SuppressWarnings("unchecked")
        Set<Instance> result = (Set<Instance>) instance.getData(getClass());
        if (result == null) {
            final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
            final Expression e = constant_propogation(instance.getFullExpression(), map);
            final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
            result = Collections.singleton(i);
            instance.setData(getClass(), result);
        }
        return result;
    }

    @Override
    public void report(Reporter reporter) {
        reporter.report(getClass().getSimpleName(), "invocations = " + invocations);
    }

    public Expression constant_propogation(Expression expression, Map<Variable, Variable> map) {
        try {
            log.log(Level.FINEST, "Before Constant Propogation: " + expression);
            variables.clear();
            invocations++;
            ConstantPropogationVisitor constantPropogationVisitor = new ConstantPropogationVisitor(variables);
            expression.accept(constantPropogationVisitor);
            expression = constantPropogationVisitor.getExpression();
            ReplacementVisitor replacementVisitor = new ReplacementVisitor(variables);
            expression.accept(replacementVisitor);
            expression = replacementVisitor.getExpression();
            log.log(Level.FINEST, "After Constant Propogation/Before Simplification: " + expression);
            SimplificationVisitor simplificationVisitor = new SimplificationVisitor();
            expression.accept(simplificationVisitor);
            expression = simplificationVisitor.getExpression();
            log.log(Level.FINEST, "After Simplification: " + expression);
            return expression;
        } catch (VisitorException x) {
            log.log(Level.SEVERE,
                    "encountered an exception -- this should not be happening!",
                    x);
        }
        return null;
    }

    /**
     * Gets variables values
     */
    private static class ConstantPropogationVisitor extends Visitor {

        private Stack<Expression> stack;
        private HashMap<String, IntConstant> variables;

        public ConstantPropogationVisitor(HashMap<String, IntConstant> variables) {
            this.stack = new Stack<Expression>();
            this.variables = variables;
        }

        public Expression getExpression() {
            return stack.pop();
        }

        @Override
        public void postVisit(Constant constant) {
            stack.push(constant);
        }

        @Override
        public void postVisit(Variable variable) {
            stack.push(variable);
        }

        @Override
        public void postVisit(Operation operation) {
            Operation.Operator op = operation.getOperator();
            Expression r = stack.pop();
            Expression l = stack.pop();
            System.out.println("postprocessing operator " + op + " with operands " + l + " and " + r);
            if (op == Operation.Operator.EQ) {
                // simple assignment
                if (r instanceof IntConstant && l instanceof IntVariable) {
                    if (variables.containsKey(l.toString())) {
                        System.out.println("variable " + l + " exists");
                        stack.push(new Operation(Operation.Operator.EQ, new IntConstant(1), new IntConstant(0)));
                        return;
                    }
                    System.out.println("adding variable " + l + " to list with value " + r);
                    variables.put(l.toString(), (IntConstant) r);
                } else if (r instanceof IntVariable && l instanceof IntConstant) {
                    if (variables.containsKey(r.toString())) {
                        System.out.println("variable " + r + " exists");
                        stack.push(new Operation(Operation.Operator.EQ, new IntConstant(1), new IntConstant(0)));
                        return;
                    }
                    System.out.println("adding variable " + r + " to list with value " + l);
                    variables.put(r.toString(), (IntConstant) l);
                }
                // complex assignment (1 +/- x) = 2, 2 = (1 +/- x)
                if (l instanceof IntConstant && r instanceof Operation) {
                    Expression r2 = ((Operation) r).getOperand(1);
                    Expression l2 = ((Operation) r).getOperand(0);
                    if (r2 instanceof IntVariable && l2 instanceof IntConstant) {
                        l = new IntConstant(((IntConstant) l).getValue() + ((IntConstant) l2).getValue()
                                * (((Operation) r).getOperator() == Operation.Operator.ADD ? -1 : 1));
                        r = r2;
                        System.out.println("adding variable " + r + " to list with value " + l);
                        variables.put(r.toString(), (IntConstant) l);
                    } else if (r2 instanceof IntConstant && l2 instanceof IntVariable) {
                        l = new IntConstant(((IntConstant) l).getValue() + ((IntConstant) r2).getValue()
                                * (((Operation) r).getOperator() == Operation.Operator.ADD ? -1 : 1));
                        r = l2;
                        System.out.println("adding variable " + r + " to list with value " + l);
                        variables.put(r.toString(), (IntConstant) l);
                    }
                } else if (l instanceof Operation && r instanceof IntConstant) {
                    Expression r2 = ((Operation) l).getOperand(1);
                    Expression l2 = ((Operation) l).getOperand(0);
                    if (r2 instanceof IntVariable && l2 instanceof IntConstant) {
                        r = new IntConstant(((IntConstant) r).getValue() + ((IntConstant) l2).getValue()
                                * (((Operation) l).getOperator() == Operation.Operator.ADD ? -1 : 1));
                        l = r2;
                        System.out.println("adding variable " + l + " to list with value " + r);
                        variables.put(l.toString(), (IntConstant) r);
                    } else if (r2 instanceof IntConstant && l2 instanceof IntVariable) {
                        r = new IntConstant(((IntConstant) r).getValue() + ((IntConstant) r2).getValue()
                                * (((Operation) l).getOperator() == Operation.Operator.ADD ? -1 : 1));
                        l = l2;
                        System.out.println("adding variable " + l + " to list with value " + r);
                        variables.put(l.toString(), (IntConstant) r);
                    }
                }
            }
            stack.push(new Operation(op, l, r));
        }
    }

    /*
     * Replaces variables
     */
    private static class ReplacementVisitor extends Visitor {

        private Stack<Expression> stack;
        private HashMap<String, IntConstant> variables;

        public Expression getExpression() {
            return stack.pop();
        }

        public ReplacementVisitor(HashMap<String, IntConstant> variables) {
            this.stack = new Stack<>();
            this.variables = variables;
        }

        @Override
        public void postVisit(IntConstant constant) {
            stack.push(constant);
        }

        @Override
        public void postVisit(IntVariable variable) {
            stack.push(variable);
        }

        @Override
        public void postVisit(Operation operation) {
            Expression r = stack.pop();
            Expression l = stack.pop();
            if (operation.getOperator() != Operation.Operator.EQ) {
                if (l instanceof Variable) {
                    if (variables.containsKey(l.toString())) {
                        System.out.println("replacing variable " + l);
                        l = variables.get(l.toString());
                    }
                }
                if (r instanceof Variable) {
                    if (variables.containsKey(r.toString())) {
                        System.out.println("replacing variable " + r);
                        r = variables.get(r.toString());
                    }
                }
            }
            stack.push(new Operation(operation.getOperator(), l, r));
        }
    }

    /**
     * Simplifies the equations
     */
    private static class SimplificationVisitor extends Visitor {

        private Stack<Expression> stack;
        private final Operation o_true = new Operation(Operation.Operator.EQ,
                new IntConstant(0), new IntConstant(0));
        private final Operation o_false = new Operation(Operation.Operator.EQ,
                new IntConstant(0), new IntConstant(1));

        public SimplificationVisitor() {
            this.stack = new Stack<Expression>();
        }

        public Expression getExpression() {
            return stack.pop();
        }

        @Override
        public void postVisit(Constant constant) {
            stack.push(constant);
        }

        @Override
        public void postVisit(Variable variable) {
            stack.push(variable);
        }

        /**
         * Simplifies operations
         *
         * @param operation
         */
        @Override
        public void postVisit(Operation operation) {
            Operation.Operator op = operation.getOperator();
            Expression r = stack.pop();
            Expression l = stack.pop();
            System.out.println("postprocessing operator " + op + " with operands " + l + " and " + r);
            // Operations on two constants
            if (r instanceof IntConstant && l instanceof IntConstant) {
                switch (op) {
                    case LT:
                        if (r.compareTo(l) < 0) {
                            stack.push(o_true);
                        } else {
                            stack.push(o_false);
                        }
                        return;
                    case LE:
                        if (r.compareTo(l) <= 0) {
                            stack.push(o_true);
                        } else {
                            stack.push(o_false);
                        }
                        return;
                    case GT:
                        if (r.compareTo(l) > 0) {
                            stack.push(o_true);
                        } else {
                            stack.push(o_false);
                        }
                        return;
                    case GE:
                        if (r.compareTo(l) >= 0) {
                            stack.push(o_true);
                        } else {
                            stack.push(o_false);
                        }
                        return;
                    case EQ:
                        if (r.compareTo(l) == 0) {
                            stack.push(o_true);
                        } else {
                            stack.push(o_false);
                        }
                        return;
                    case NE:
                        if (r.compareTo(l) != 0) {
                            stack.push(o_true);
                        } else {
                            stack.push(o_false);
                        }
                        return;
                    case ADD:
                        stack.push(new IntConstant(((IntConstant) r).getValue() + ((IntConstant) l).getValue()));
                        return;
                    case SUB:
                        stack.push(new IntConstant(((IntConstant) r).getValue() - ((IntConstant) l).getValue()));
                        return;
                    case MUL:
                        stack.push(new IntConstant(((IntConstant) r).getValue() * ((IntConstant) l).getValue()));
                        return;
                    default:
                        break;
                }
            }
            // Operations on two operations
            if (r instanceof Operation && l instanceof Operation) {
                switch (op) {
                    case AND:
                        if (r.equals(o_true) && l.equals(o_true)) {
                            System.out.println("both true and");
                            stack.push(o_true);
                            return;
                        } else if ((r.equals(o_false) && l.equals(o_false))
                                || r.equals(o_false) && l.equals(o_true)
                                || r.equals(o_true) && l.equals(o_false)) {
                            System.out.println("either false and");
                            stack.push(o_false);
                            return;
                        } else if (r.equals(o_false) || l.equals(o_false)) {
                            System.out.println("one false and");
                            stack.push(o_false);
                            return;
                        } else if (r.equals(o_true)) {
                            System.out.println("one true and");
                            stack.push(l);
                            return;
                        } else if (l.equals(o_true)) {
                            System.out.println("one true and");
                            stack.push(r);
                            return;
                        }
                    case OR:
                        if (r.equals(o_false) && l.equals(o_false)) {
                            System.out.println("both false or");
                            stack.push(o_false);
                            return;
                        } else if ((r.equals(o_true) && l.equals(o_true))
                                || r.equals(o_false) && l.equals(o_true)
                                || r.equals(o_true) && l.equals(o_false)) {
                            System.out.println("either true or");
                            stack.push(o_true);
                            return;
                        } else if (r.equals(o_true)) {
                            System.out.println("one true or");
                            stack.push(l);
                        } else if (l.equals(o_true)) {
                            System.out.println("one true or");
                            stack.push(r);
                        }
                    default:
                        break;
                }
            }
            stack.push(new Operation(op, l, r));
        }
    }
}
