package za.ac.sun.cs.green.service.simplify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;

import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.util.Reporter;
import za.ac.sun.cs.green.expr.Constant;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.VisitorException;

public class ConstantPropagation extends BasicService {
    /**
     * Number of times the slicer has been invoked.
     */
    private int invocations = 0;

    public ConstantPropagation(Green solver) {
        super(solver);
    }

    @Override
    public Set<Instance> processRequest(Instance instance) {
        @SuppressWarnings("unchecked")
        Set<Instance> result = (Set<Instance>) instance.getData(getClass());
        if (result == null) {
            final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
            final Expression e = propagate(instance.getFullExpression(), map);
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

    public Expression propagate(Expression expression, Map<Variable, Variable> map) {
        try {
            log.log(Level.FINEST, "Before Constant Propagation: " + expression);
            invocations++;
            OrderingVisitor orderingVisitor = new OrderingVisitor();
            expression.accept(orderingVisitor);
            expression = orderingVisitor.getExpression();
            log.log(Level.FINEST, "After Constant Propagation: " + expression);

            SimplifyingVisitor simplifyingVisitor = new SimplifyingVisitor();
            expression.accept(simplifyingVisitor);
            expression = simplifyingVisitor.getExpression();
            log.log(Level.FINEST, "After Simplification: " + expression);
            return expression;
        } catch (VisitorException x) {
            log.log(Level.SEVERE, "encountered an exception -- this should not be happening!", x);
        }
        return null;
    }

    private static class OrderingVisitor extends Visitor {
        private Stack<Expression> stack;
        private HashMap<IntVariable, IntConstant> variables;

        public OrderingVisitor() {
            stack = new Stack<Expression>();
            variables = new HashMap<IntVariable, IntConstant>();
        }

        public Expression getExpression() {
            Expression finalExp = stack.pop();
            System.out.println("Final expression is " + finalExp);
            return finalExp;
        }

        @Override
        public void postVisit(IntConstant constant) {
            stack.push(constant);
        }

        @Override
        public void postVisit(IntVariable variable) {
            //If variable exists in HashMap (it has been assigned a value)
            if (variables.containsKey(variable)) {
                stack.push(variables.get(variable));
            } else {
                stack.push(variable);
            }
        }

        @Override
        public void postVisit(Operation operation) throws VisitorException {
            Operation.Operator op = operation.getOperator();
            Expression r = stack.pop();
            Expression l = stack.pop();

            //If operation is an EQ type. Add the equality to the HashMap to propagate in future
            if (op == Operation.Operator.EQ) {
                if ((l instanceof IntVariable) && (r instanceof IntConstant)) {
                    variables.put((IntVariable) l, (IntConstant) r);
                    stack.push(new Operation(op, l, r));
                } else if ((l instanceof IntConstant) && (r instanceof IntVariable)) {
                    variables.put((IntVariable) r, (IntConstant) l);
                    stack.push(new Operation(op, l, r));
                } else {
                    stack.push(new Operation(op, l, r));
                }
            } else {
                if (variables.containsKey(r)) {
                    r = variables.get(r);
                }
                if (variables.containsKey(l)) {
                    l = variables.get(l);
                }
                stack.push(new Operation(op, l, r));
            }
        }

    }

    private static class SimplifyingVisitor extends Visitor {
        private Stack<Expression> stack;

        public SimplifyingVisitor() {
            stack = new Stack<Expression>();
            System.out.println("Starting Simplifier");
        }

        public Expression getExpression() {
            Expression finalExp = stack.pop();
            System.out.println("Final expression: " + finalExp);
            return finalExp;
        }

        @Override
        public void postVisit(IntConstant constant) {
            System.out.println("Constant: " + constant);
            stack.push(constant);
        }

        @Override
        public void postVisit(IntVariable variable) {
            System.out.println("Variable: " + variable);
            stack.push(variable);
        }

        @Override
        public void postVisit(Operation operation) throws VisitorException {
            Operation.Operator op = operation.getOperator();
            Expression[] expressions = new Expression[op.getArity()];

            for(int i = op.getArity(); i > 0; i--) {
                expressions[i-1] = stack.pop();
                System.out.println(i + " " + expressions[i-1]);
            }

			if (operands[0] instanceof IntConstant && operands[1] instanceof IntConstant) {
				switch (op) {
                    case EQ:
                        if ((operands[0].getValue() == operands[1].getValue())) {
                            stack.push(Operation.TRUE);
                        } else {
                            stack.push(Operation.FALSE);
                        }
                        break;
                    case LT:
                        if(operands[0].getValue() < expressions[1].getValue()) {
                            stack.push(Operation.TRUE);
                        } else {
                            stack.push(Operation.FALSE);
                        }
                        break;
                    case LE:
                        if(operands[0].getValue() <= expressions[1].getValue()) {
                            stack.push(Operation.TRUE);
                        } else {
                            stack.push(Operation.FALSE);
                        }
                        break;
                    case GT:
                        if(operands[0].getValue() > expressions[1].getValue()) {
                            stack.push(Operation.TRUE);
                        } else {
                            stack.push(Operation.FALSE);
                        }
                        break;
                    case GE:
                        if(operands[0].getValue() >= expressions[1].getValue()) {
                            stack.push(Operation.TRUE);
                        } else {
                            stack.push(Operation.FALSE);
                        }
                        break;
                    case NE:
                        if(expressions[0].getValue() != expressions[1].getValue()) {
                            stack.push(Operation.TRUE);
                        } else {
                            stack.push(Operation.FALSE);
                        }
                        break;
                    case ADD:
                        IntConstant operationToConstantADD = new IntConstant(expressions[0].getValue() + expressions[1].getValue());
                        stack.push(operationToConstantADD);
                        break;
                    case SUB:
                        IntConstant operationToConstantSUB = new IntConstant(expressions[0].getValue() - expressions[1].getValue());
                        stack.push(operationToConstantSUB);
                        break;
                    case MUL:
                        IntConstant operationToConstantMUL = new IntConstant(expressions[0].getValue() * expressions[1].getValue());
                        stack.push(operationToConstantMUL);
                        break;
                    case DIV:
                        IntConstant operationToConstantDIV = new IntConstant(expressions[0].getValue() / expressions[1].getValue());
                        stack.push(operationToConstantDIV);
                        break;
                    default:
                        stack.push(operation);
                        break;
				}
			}
        }
    }
}