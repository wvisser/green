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
            invocations++;
            ConstantPropogationVisitor constantPropogationVisitor = new ConstantPropogationVisitor();
            expression.accept(constantPropogationVisitor);
            expression = constantPropogationVisitor.getExpression();
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

    private static class ConstantPropogationVisitor extends Visitor {

        private Stack<Expression> stack;
        private HashMap<IntVariable, IntConstant> variables;

        public ConstantPropogationVisitor() {
            this.stack = new Stack<Expression>();
            this.variables = new HashMap<IntVariable, IntConstant>();
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
        public void postVisit(IntVariable variable) {
            if (variables.containsKey(variable)) {
                System.out.println("replacing variable " + variable.getName() + " with value " + variables.get(variable));
                stack.push(variables.get(variable));
            } else {
                System.out.println("not replacing variable " + variable.getName());
                stack.push(variable);
            }
        }

        @Override
        public void postVisit(Operation operation) {
            Operation.Operator op = operation.getOperator();
            Expression r = stack.pop();
            Expression l = stack.pop();
            if (op == Operation.Operator.EQ
                    && (r instanceof IntConstant
                    && l instanceof IntVariable)) {
                System.out.println("adding variable " + l + " to list with value " + r);
                variables.put((IntVariable) l, (IntConstant) r);
                stack.push(new Operation(op, l, r));
            } else if (op == Operation.Operator.EQ
                    && (r instanceof IntVariable
                    && l instanceof IntConstant)) {
                System.out.println("adding variable " + r + " to list with value " + r);
                variables.put((IntVariable) r, (IntConstant) l);
                stack.push(new Operation(op, l, r));
            } else {
                stack.push(new Operation(op, l, r));
            }
        }
    }

    private static class SimplificationVisitor extends Visitor {

        private Stack<Expression> stack;

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

        @Override
        public void postVisit(Operation operation) {
            Operation.Operator op = operation.getOperator();
            Expression r = stack.pop();
            Expression l = stack.pop();
            Operation.Operator eq = Operation.Operator.EQ;
            if (r instanceof IntConstant && l instanceof IntConstant) {
                switch (op) {
                    case LT:
                        if (l.compareTo(r) < 0) {
                            stack.push(new Operation(eq, new IntConstant(0), new IntConstant(0)));
                        } else {
                            stack.push(new Operation(eq, new IntConstant(1), new IntConstant(0)));
                        }
                        return;
                    case LE:
                        if (l.compareTo(r) <= 0) {
                            stack.push(new Operation(eq, new IntConstant(0), new IntConstant(0)));
                        } else {
                            stack.push(new Operation(eq, new IntConstant(1), new IntConstant(0)));
                        }
                        return;
                    case GT:
                        if (l.compareTo(r) > 0) {
                            stack.push(new Operation(eq, new IntConstant(0), new IntConstant(0)));
                        } else {
                            stack.push(new Operation(eq, new IntConstant(1), new IntConstant(0)));
                        }
                        return;
                    case GE:
                        if (l.compareTo(r) >= 0) {
                            stack.push(new Operation(eq, new IntConstant(0), new IntConstant(0)));
                        } else {
                            stack.push(new Operation(eq, new IntConstant(1), new IntConstant(0)));
                        }
                        return;
                    case EQ:
                        if (l.compareTo(r) == 0) {
                            stack.push(new Operation(eq, new IntConstant(0), new IntConstant(0)));
                        } else {
                            stack.push(new Operation(eq, new IntConstant(1), new IntConstant(0)));
                        }
                        return;
                    case NE:
                        if (l.compareTo(r) != 0) {
                            stack.push(new Operation(eq, new IntConstant(0), new IntConstant(0)));
                        } else {
                            stack.push(new Operation(eq, new IntConstant(1), new IntConstant(0)));
                        }
                        return;
                    case AND:
                        break;
                    case OR:
                        break;
                    default:
                        break;
                }
            }
            stack.push(new Operation(op, l, r));
        }
    }
}
