package za.ac.sun.cs.green.service.simplify;

import org.chocosolver.solver.variables.IntVar;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.*;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.service.canonizer.SATCanonizerService;

import java.util.*;
import java.util.logging.Level;

public class ConstantPropagation extends BasicService {

    /**
     * Number of times the slicer has been invoked.
     */
    private int invocations = 0;

    /**
     * Constructor for the basic service. It simply initializes its three
     * attributes.
     *
     * @param solver the {@link Green} solver this service will be added to
     */
    public ConstantPropagation(Green solver) {
        super(solver);
    }

    @Override
    public Set<Instance> processRequest(Instance instance) {
        @SuppressWarnings("unchecked")
        Set<Instance> result = (Set<Instance>) instance.getData(getClass());
        if (result == null) {
            final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
            final Expression e = simplify(instance.getFullExpression(), map);
            final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
            result = Collections.singleton(i);
            instance.setData(getClass(), result);
        }
        return result;
    }

    public Expression simplify(Expression expression,
                               Map<Variable, Variable> map) {
        try {
            log.log(Level.FINEST, "Before Simplification: " + expression);
            invocations++;

            if (map == null) {
                throw new VisitorException("");
            }

            ConstantPropagation.ConstantPropagationVisitor conProgVisitor = new ConstantPropagationVisitor();
            ConstantPropagation.SimplificationVisitor simpVisitor = new SimplificationVisitor();

            String result = "";
            while (true) {
                expression.accept(conProgVisitor);
                expression = conProgVisitor.getExpression();
                expression.accept(simpVisitor);
                expression = simpVisitor.getExpression();

                expression.accept(conProgVisitor);
                expression = conProgVisitor.getExpression();
                expression.accept(simpVisitor);
                expression = simpVisitor.getExpression();
                if (expression.toString().equals(result)) {
                    break;
                } else {
                    result = expression.toString();
                }
            }


            log.log(Level.FINEST, "After Simplification: " + expression);
            return expression;
        } catch (VisitorException x) {
            log.log(Level.SEVERE,
                    "encountered an exception -- this should not be happening!",
                    x);
        }
        return null;
    }

    private static class SimplificationVisitor extends Visitor {

        private Stack<Expression> stack;
        private Map<IntVariable, IntConstant> vars;

        public SimplificationVisitor() {
            stack = new Stack<>();
            vars = new HashMap<>();
        }

        public Expression getExpression() {
            return stack.pop();
        }

        public List<Expression> collectAndSimplify(Expression l, Expression r) {
            List<Expression> returnList = new ArrayList<>();

            if (l instanceof Operation) {
                Expression opLeft = ((Operation) l).getOperand(0);
                Expression opRight = ((Operation) l).getOperand(1);

                if (opLeft instanceof IntConstant && opRight instanceof IntVariable) {
                    int iVal = ((IntConstant) opLeft).getValue();

                    if (r instanceof IntConstant) {
                        switch (((Operation) l).getOperator()) {
                            case ADD:
                                iVal = ((IntConstant) r).getValue() - iVal;
                                returnList.add(opRight);
                                returnList.add(new IntConstant(iVal));
                                break;
                            case SUB:
                                iVal = ((IntConstant) r).getValue() + iVal;
                                returnList.add(opRight);
                                returnList.add(new IntConstant(iVal));
                                break;
                            default:
                                throw new UnsupportedOperationException(((Operation) l).getOperator().toString() + " Not Yet Supported.");
                        }
                        return returnList;
                    }
                } else if (opLeft instanceof IntVariable && opRight instanceof IntConstant) {
                    int iVal = ((IntConstant) opRight).getValue();

                    if (r instanceof IntConstant) {
                        switch (((Operation) l).getOperator()) {
                            case SUB:
                                iVal = ((IntConstant) r).getValue() + iVal;
                                returnList.add(opLeft);
                                returnList.add(new IntConstant(iVal));
                                break;
                        }
                        return returnList;
                    }
                }

            } else if (r instanceof Operation) {

            }
            return null;
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
            Operation.Operator op = operation.getOperator();

            if (op.getArity() == 2) {
                Expression r = stack.pop();
                Expression l = stack.pop();

                if (r instanceof Operation ^ l instanceof Operation) {
                    List<Expression> simplified = collectAndSimplify(l, r);

                    if (simplified != null) {
                        l = simplified.get(0);
                        r = simplified.get(1);
                    }
                    stack.push(new Operation(op, l, r));
                } else if (l instanceof IntConstant && r instanceof IntConstant) {
                    switch (op) {
                        case EQ:
                            if (((IntConstant) l).getValue() == ((IntConstant) r).getValue()) {
                                stack.push(Operation.TRUE);
                            } else {
                                stack.push(Operation.FALSE);
                            }
                            break;
                        case LT:
                            if (((IntConstant) l).getValue() < ((IntConstant) r).getValue()) {
                                stack.push(Operation.TRUE);
                            } else {
                                stack.push(Operation.FALSE);
                            }
                            break;
                        case GT:
                            if (((IntConstant) l).getValue() > ((IntConstant) r).getValue()) {
                                stack.push(Operation.TRUE);
                            } else {
                                stack.push(Operation.FALSE);
                            }
                    }
                } else {
                    stack.push(new Operation(op, l, r));
                }
            } else {
                for (int i = op.getArity(); i > 0; i--) {
                    stack.pop();
                }
                stack.push(operation);
            }
        }
    }


    private static class ConstantPropagationVisitor extends Visitor {

        private Stack<Expression> stack;
        private Map<IntVariable, IntConstant> vars;

        public ConstantPropagationVisitor() {
            stack = new Stack<>();
            vars = new HashMap<>();
        }

        public Expression getExpression() {
            return stack.pop();
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
            Operation.Operator op = operation.getOperator();

            if (op.getArity() == 2) {
                Expression r = stack.pop();
                Expression l = stack.pop();

                if (op == Operation.Operator.EQ) {
                    if (r instanceof IntVariable && l instanceof IntConstant) {
                        vars.put((IntVariable) r, (IntConstant) l);
                    } else if (l instanceof IntVariable && r instanceof IntConstant) {
                        vars.put((IntVariable) l, (IntConstant) r);
                    }
                    stack.push(new Operation(op, l, r));
                } else if (r instanceof IntVariable || l instanceof IntVariable) {
                    if (vars.containsKey(r)) {
                        r = vars.get(r);
                    }

                    if (vars.containsKey(l)) {
                        l = vars.get(l);
                    }
                    stack.push(new Operation(op, l, r));
                } else if (op == Operation.Operator.AND) {
                    if (l.equals(Operation.FALSE) || r.equals(Operation.FALSE)) {
                        stack.push(Operation.FALSE);
                        return;
                    } else if (l.equals(Operation.TRUE) && r.equals(Operation.TRUE)) {
                        stack.push(Operation.TRUE);
                    } else if (l.equals(Operation.TRUE)) {
                        stack.push(r);
                    } else if (r.equals(Operation.TRUE)) {
                        stack.push(l);
                    } else {
                        stack.push(new Operation(op, l, r));
                    }
                } else {
                    stack.push(new Operation(op, l, r));
                }
            } else {
                for (int i = op.getArity(); i > 0; i--) {
                    stack.pop();
                }
                stack.push(operation);
            }
        }

    }

}
