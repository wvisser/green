package za.ac.sun.cs.green.service.simplify;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.*;
import za.ac.sun.cs.green.service.BasicService;

import java.util.*;
import java.util.logging.Level;

/**
 * Class Defines a Service which Simplifies Expressions
 */
public class SimplificationService extends BasicService {

    /**
     * Constructor for the basic service. It simply initializes its three
     * attributes.
     *
     * @param solver the {@link Green} solver this service will be added to
     */
    public SimplificationService(Green solver) {
        super(solver);
    }

    /**
     *
     * Handles requests and invokes the simplifier on a Green Instance.
     *
     * @param instance
     *            the instance to solve
     * @return The solved instance.
     */
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

    /**
     *  Method Simplifies Expressions
     *
     * @param expression The Expression to Simplify
     * @param map Variable Map
     * @return The Simplified Expression
     */
    public Expression simplify(Expression expression,
                               Map<Variable, Variable> map) {
        try {
            log.log(Level.FINEST, "Before Simplification: " + expression);

            if (map == null) {
                throw new VisitorException("");
            }

            SimplificationService.ConstantPropagationVisitor conProgVisitor = new ConstantPropagationVisitor();
            SimplificationService.SimplificationVisitor simpVisitor = new SimplificationVisitor();

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

    /**
     * Class is a Visitor which Simplifies Expressions.
     */
    private static class SimplificationVisitor extends Visitor {

        /**
         * Expression Stack. Think RPN Calculator...
         */
        private Stack<Expression> stack;

        /**
         * Maps Variables to Constants.
         */
        private Map<IntVariable, IntConstant> vars;

        /**
         * Constructor for the Visitor.
         */
        public SimplificationVisitor() {
            stack = new Stack<>();
            vars = new HashMap<>();
        }

        /**
         * Method returns the simplified Expression
         *
         * @return the simplified expression.
         */
        public Expression getExpression() {
            return stack.pop();
        }

        /**
         * Collects Expressions
         * Moves constants to RHS
         * Moves vars to LHS
         *
         * Collapses and Simplifies Expressions
         *
         * @param l LHS Expressions
         * @param r RHS Expression
         * @return List of Simplified Expressions [0 = LHS, 1 = RHS]
         */
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

        /**
         * Method gets called by the Expression after visiting constant.
         *
         * @param constant Constant Visited
         */
        @Override
        public void postVisit(IntConstant constant) {
            stack.push(constant);
        }

        /**
         * Method gets called by the Expression after visiting variable.
         *
         * @param variable Variable Visited
         */
        @Override
        public void postVisit(IntVariable variable) {
            stack.push(variable);
        }

        /**
         *  Method gets called by the Expression after visiting Operation
         *
         * @param operation Operation Visited
         */
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


    /**
     * Class is a Visitor which Propagates Constants.
     */
    private static class ConstantPropagationVisitor extends Visitor {

        /**
         * Expression Stack. Think RPN Calculator...
         */
        private Stack<Expression> stack;

        /**
         * Maps Variables to Constants.
         */
        private Map<IntVariable, IntConstant> vars;

        /**
         * Constructor for Visitor
         */
        public ConstantPropagationVisitor() {
            stack = new Stack<>();
            vars = new HashMap<>();
        }

        /**
         * Method returns the propagated Expression
         *
         * @return the simplified expression.
         */
        public Expression getExpression() {
            return stack.pop();
        }

        /**
         * Method gets called by the Expression after visiting constant.
         *
         * @param constant Constant Visited
         */
        @Override
        public void postVisit(IntConstant constant) {
            stack.push(constant);
        }

        /**
         * Method gets called by the Expression after visiting variable.
         *
         * @param variable Variable Visited
         */
        @Override
        public void postVisit(IntVariable variable) {
            stack.push(variable);
        }

        /**
         *  Method gets called by the Expression after visiting Operation
         *
         * @param operation Operation Visited
         */
        @Override
        public void postVisit(Operation operation) {
            Operation.Operator op = operation.getOperator();

            // Only Operate if we have two Operands
            if (op.getArity() == 2) {
                Expression r = stack.pop();
                Expression l = stack.pop();

                // Create Mapping if we have var == const
                if (op == Operation.Operator.EQ) {

                    // const == var
                    if (r instanceof IntVariable && l instanceof IntConstant) {
                        vars.put((IntVariable) r, (IntConstant) l);

                    // var == const
                    } else if (l instanceof IntVariable && r instanceof IntConstant) {
                        vars.put((IntVariable) l, (IntConstant) r);
                    }

                    // place op back onto stack before we lose it.
                    stack.push(new Operation(op, l, r));

                // Do we have any vars we can replace?
                } else if (r instanceof IntVariable || l instanceof IntVariable) {

                    // Replace RHS
                    if (vars.containsKey(r)) {
                        r = vars.get(r);
                    }

                    // Replace LHS
                    if (vars.containsKey(l)) {
                        l = vars.get(l);
                    }

                    // place op back onto stack before we lose it.
                    stack.push(new Operation(op, l, r));

                   // Dangerous Simplification Stuff
                   // which does not belong here. YOLO.

                    // Collapses ops as follows

                    // T && T -> T
                    // T && F -> F
                    // F && T -> F
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
                // Shove everything else back onto the op stack
                } else {
                    stack.push(new Operation(op, l, r));
                }

            // Catch for some random edge case. Stole from SATCan.
            } else {
                for (int i = op.getArity(); i > 0; i--) {
                    stack.pop();
                }
                stack.push(operation);
            }
        }

    }

}
