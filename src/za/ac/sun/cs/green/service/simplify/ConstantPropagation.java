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
            HashMap<IntVariable, IntConstant> variables = new HashMap<IntVariable, IntConstant>();

            //Search for assignments
            AssignmentVisitor assignmentVisitor = new AssignmentVisitor(variables);
            expression.accept(assignmentVisitor);
            expression = assignmentVisitor.getExpression();


            //Propagate assignments
            OrderingVisitor orderingVisitor = new OrderingVisitor(variables);
            expression.accept(orderingVisitor);
            expression = orderingVisitor.getExpression();
            log.log(Level.FINEST, "After Constant Propagation: " + expression);
            
            //Simplify
            expression = multipleSimplifications(expression, orderingVisitor);
            return expression;
        } catch (VisitorException x) {
            log.log(Level.SEVERE, "encountered an exception -- this should not be happening!", x);
        }
        return null;
    }



    private Expression multipleSimplifications(Expression expression, OrderingVisitor orderingVisitor) throws VisitorException {
        Boolean simplified = false;
        Boolean propagated = false;

        SimplifyingVisitor simplifyingVisitor = new SimplifyingVisitor();
        expression.accept(simplifyingVisitor);
        expression = simplifyingVisitor.getExpression();
        simplified = simplifyingVisitor.getSimplified();
        
        log.log(Level.FINEST, "After Simplification: " + expression);

        // while(simplified || propagated) {
            // orderingVisitor = new OrderingVisitor();
            // expression.accept(orderingVisitor);
            // expression = orderingVisitor.getExpression();
            // propagated = orderingVisitor.getPropagated();
            // log.log(Level.FINEST, "After Constant Propagation: " + expression);
            // simplifyingVisitor = new SimplifyingVisitor();
            // expression.accept(simplifyingVisitor);
            // expression = simplifyingVisitor.getExpression();
            // simplified = simplifyingVisitor.getSimplified();
            // log.log(Level.FINEST, "After Simplification: " + expression);
        // }
        return expression;
    }
    
    private static class AssignmentVisitor extends Visitor {
        private Stack<Expression> stack;
        private HashMap<IntVariable, IntConstant> variables;

        public AssignmentVisitor(HashMap<IntVariable, IntConstant> map) {
            stack = new Stack<Expression>();
            variables = map;
        }

        public Expression getExpression() {
            Expression finalExp = stack.pop();
            System.out.println("Final expression (assignment) is " + finalExp);
            return finalExp;
        }

        @Override
        public void postVisit(IntConstant constant) {
            System.out.println("Assignment: Pushing constant " + constant);
            stack.push(constant);
        }

        @Override
        public void postVisit(IntVariable variable) {
            System.out.println("Assignment: Pushing variable " + variable);
            stack.push(variable);
        }

        @Override
        public void postVisit(Operation operation) throws VisitorException {
            Operation.Operator op = operation.getOperator();
            Expression r = stack.pop();
            Expression l = stack.pop();

            // If operation is an EQ type. Add the equality to the HashMap
            if (op == Operation.Operator.EQ) {
                if ((l instanceof IntVariable) && (r instanceof IntConstant)) {
                    System.out.println("Assignment: Giving " + l + " value " + r);
                    variables.put((IntVariable) l, (IntConstant) r);
                } else if ((l instanceof IntConstant) && (r instanceof IntVariable)) {
                    System.out.println("Assignment: Giving " + r + " value " + l);
                    variables.put((IntVariable) r, (IntConstant) l);
                }
            }
            stack.push(new Operation(op, l, r));
        }

    }

    private static class OrderingVisitor extends Visitor {
        private Stack<Expression> stack;
        private HashMap<IntVariable, IntConstant> variables;
        private Boolean propagated;

        public OrderingVisitor(HashMap<IntVariable, IntConstant> map) {
            stack = new Stack<Expression>();
            variables = map;
            propagated = false;
        }

        public Expression getExpression() {
            Expression finalExp = stack.pop();
            System.out.println("Final expression (propagation) is " + finalExp);
            return finalExp;
        }

        public Boolean getPropagated() {
            return propagated;
        }

        @Override
        public void postVisit(IntConstant constant) {
            System.out.println("Propagation: pushing constant " + constant);
            stack.push(constant);
        }

        @Override
        public void postVisit(IntVariable variable) {
            // If variable exists in HashMap (it has been assigned a value)
            // if (variables.containsKey(variable)) {
            //     stack.push(variables.get(variable));
            //     propagated = true;
            // } else {
            //     stack.push(variable);
            // }
            System.out.println("Propagation: pushing variable " + variable);
            stack.push(variable);
        }

        @Override
        public void postVisit(Operation operation) throws VisitorException {
            Operation.Operator op = operation.getOperator();
            Expression r = stack.pop();
            Expression l = stack.pop();

            // If operation is an EQ type. Add the equality to the HashMap to propagate in
            // future
            if (op == Operation.Operator.EQ) {
                if ((l instanceof IntVariable) && (r instanceof IntConstant)) {
                    System.out.println("Propagation: " + l + " " + op + " " + r);
                    stack.push(new Operation(op, l, r));
                } else if ((l instanceof IntConstant) && (r instanceof IntVariable)) {
                    System.out.println("Propagation: " + l + " " + op + " " + r);
                    stack.push(new Operation(op, l, r));
                } else {
                    System.out.println("Propagation: " + l + " " + op + " " + r);
                    if (variables.containsKey(r)) {
                        r = variables.get(r);
                    }
                    if (variables.containsKey(l)) {
                        l = variables.get(l);
                    }
                    stack.push(new Operation(op, l, r));
                }
            } else {
                if (variables.containsKey(r)) {
                    r = variables.get(r);
                }
                if (variables.containsKey(l)) {
                    l = variables.get(l);
                }
                System.out.println("Propagation: " + l + " " + op + " " + r);
                stack.push(new Operation(op, l, r));
            }
        }

    }

    private static class SimplifyingVisitor extends Visitor {
        private Stack<Expression> stack;
        private Boolean simplified;

        public SimplifyingVisitor() {
            stack = new Stack<Expression>();
            simplified = false;
            System.out.println("Starting Simplifier");
        }

        public Expression getExpression() {
            Expression finalExp = stack.pop();
            System.out.println("Final expression: " + finalExp);
            return finalExp;
        }

        public Boolean getSimplified() {
            return simplified;
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
        public void postVisit(Operation operation) throws VisitorException {
            Operation.Operator op = operation.getOperator();
            Expression r = stack.pop();
            Expression l = stack.pop();

            System.out.println("Processing: " + l + " " + op + " " + r);

            if (l instanceof IntConstant && r instanceof IntConstant) {
                System.out.println("Have 2 constants");
                simplified = true;

                switch (op) {
                case EQ:
                    if ((((IntConstant) l).getValue() == ((IntConstant) r).getValue())) {
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
                case LE:
                    if (((IntConstant) l).getValue() <= ((IntConstant) r).getValue()) {
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
                    break;
                case GE:
                    if (((IntConstant) l).getValue() >= ((IntConstant) r).getValue()) {
                        stack.push(Operation.TRUE);
                    } else {
                        stack.push(Operation.FALSE);
                    }
                    break;
                case NE:
                    if (((IntConstant) l).getValue() != ((IntConstant) r).getValue()) {
                        stack.push(Operation.TRUE);
                    } else {
                        stack.push(Operation.FALSE);
                    }
                    break;
                case ADD:
                    IntConstant addResult = new IntConstant(
                            ((IntConstant) l).getValue() + ((IntConstant) r).getValue());
                    stack.push(addResult);
                    break;
                case SUB:
                    IntConstant subResult = new IntConstant(
                            ((IntConstant) l).getValue() - ((IntConstant) r).getValue());
                    stack.push(subResult);
                    break;
                case MUL:
                    IntConstant mulResult = new IntConstant(
                            ((IntConstant) l).getValue() * ((IntConstant) r).getValue());
                    stack.push(mulResult);
                    break;
                case DIV:
                    IntConstant divResult = new IntConstant(
                            ((IntConstant) l).getValue() / ((IntConstant) r).getValue());
                    stack.push(divResult);
                    break;
                default:
                    System.out.println("Hit default 1... Weird");
                    stack.push(operation);
                    break;
                }
            }

            else if (l instanceof Operation && r instanceof Operation) {
                System.out.println("Have 2 ops");
                simplified = true;

                switch (op) {
                case AND:
                    if (l.equals(Operation.TRUE) && r.equals(Operation.TRUE)) {
                        stack.push(Operation.TRUE);
                    } else if (l.equals(Operation.TRUE) && !r.equals(Operation.TRUE)) {
                        stack.push(r);
                    } else if (r.equals(Operation.TRUE) && !l.equals(Operation.TRUE)) {
                        stack.push(l);
                    } else if (l.equals(Operation.FALSE) || r.equals(Operation.FALSE)) {
                        stack.push(Operation.FALSE);
                    } else {
                        simplified = false;
                        stack.push(new Operation(op, l, r));
                    }
                    break;
                case OR:
                    if (l.equals(Operation.FALSE) && r.equals(Operation.FALSE)) {
                        stack.push(Operation.FALSE);
                    } else if (l.equals(Operation.TRUE) || r.equals(Operation.TRUE)) {
                        stack.push(Operation.TRUE);
                    } else {
                        simplified = false;
                        stack.push(new Operation(op, l, r));
                    }
                    break;
                default:
                    System.out.println("Hit default 2... Weird");
                    break;
                }

            }

            else if ((l instanceof Operation && r instanceof IntConstant)
                    || (l instanceof IntConstant && r instanceof Operation)) {
                System.out.println("Have an op and a constant");
                simplified = true;
                Operation insideOpp;
                IntConstant outsideConstant;
                if (l instanceof Operation) {
                    insideOpp = (Operation) l;
                    outsideConstant = (IntConstant) r;
                } else {
                    insideOpp = (Operation) r;
                    outsideConstant = (IntConstant) l;
                }

                switch (op) {
                case NE:
                case EQ:
                    switch (insideOpp.getOperator()) {
                    case SUB:
                        if (insideOpp.getOperand(0) instanceof IntConstant
                                && insideOpp.getOperand(1) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(0);

                            int result = constant.getValue() - outsideConstant.getValue();
                            operation = new Operation(Operation.Operator.EQ, insideOpp.getOperand(1),
                                    new IntConstant(result));
                            stack.push(operation);
                        } else if (insideOpp.getOperand(1) instanceof IntConstant
                                && insideOpp.getOperand(0) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(1);

                            int result = outsideConstant.getValue() + constant.getValue();
                            operation = new Operation(Operation.Operator.EQ, insideOpp.getOperand(0),
                                    new IntConstant(result));
                            stack.push(operation);
                        }
                        break;
                    case ADD:
                        if (insideOpp.getOperand(0) instanceof IntConstant
                                && insideOpp.getOperand(1) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(0);

                            int result = outsideConstant.getValue() - constant.getValue();
                            operation = new Operation(Operation.Operator.EQ, insideOpp.getOperand(1),
                                    new IntConstant(result));
                            stack.push(operation);
                        } else if (insideOpp.getOperand(1) instanceof IntConstant
                                && insideOpp.getOperand(0) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(1);

                            int result = outsideConstant.getValue() - constant.getValue();
                            operation = new Operation(Operation.Operator.EQ, insideOpp.getOperand(0),
                                    new IntConstant(result));
                            stack.push(operation);
                        }
                        break;
                    case DIV:
                        if (insideOpp.getOperand(0) instanceof IntConstant
                                && insideOpp.getOperand(1) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(0);

                            int result = constant.getValue() / outsideConstant.getValue();
                            operation = new Operation(Operation.Operator.EQ, insideOpp.getOperand(1),
                                    new IntConstant(result));
                            stack.push(operation);
                        } else if (insideOpp.getOperand(1) instanceof IntConstant
                                && insideOpp.getOperand(0) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(1);

                            int result = outsideConstant.getValue() / constant.getValue();
                            operation = new Operation(Operation.Operator.EQ, insideOpp.getOperand(0),
                                    new IntConstant(result));
                            stack.push(operation);
                        }
                        break;
                    case MUL:
                        if (insideOpp.getOperand(0) instanceof IntConstant
                                && insideOpp.getOperand(1) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(0);

                            int result = outsideConstant.getValue() / constant.getValue();
                            operation = new Operation(Operation.Operator.EQ, insideOpp.getOperand(1),
                                    new IntConstant(result));
                            stack.push(operation);
                        } else if (insideOpp.getOperand(1) instanceof IntConstant
                                && insideOpp.getOperand(0) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(1);

                            int result = outsideConstant.getValue() / constant.getValue();
                            operation = new Operation(Operation.Operator.EQ, insideOpp.getOperand(0),
                                    new IntConstant(result));
                            stack.push(operation);
                        }
                        break;
                    default:
                        System.out.println("Hit default 3... Weird");
                        break;
                    }
                    break;
                case LT:
                    switch (insideOpp.getOperator()) {
                    case SUB:
                        if (insideOpp.getOperand(0) instanceof IntConstant
                                && insideOpp.getOperand(1) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(0);

                            int result = constant.getValue() - outsideConstant.getValue();
                            operation = new Operation(Operation.Operator.LT, insideOpp.getOperand(1),
                                    new IntConstant(result));
                            stack.push(operation);
                        } else if (insideOpp.getOperand(1) instanceof IntConstant
                                && insideOpp.getOperand(0) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(1);

                            int result = outsideConstant.getValue() + constant.getValue();
                            operation = new Operation(Operation.Operator.LT, insideOpp.getOperand(0),
                                    new IntConstant(result));
                            stack.push(operation);
                        }
                        break;
                    case ADD:
                        if (insideOpp.getOperand(0) instanceof IntConstant
                                && insideOpp.getOperand(1) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(0);

                            int result = outsideConstant.getValue() - constant.getValue();
                            operation = new Operation(Operation.Operator.LT, insideOpp.getOperand(1),
                                    new IntConstant(result));
                            stack.push(operation);
                        } else if (insideOpp.getOperand(1) instanceof IntConstant
                                && insideOpp.getOperand(0) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(1);

                            int result = outsideConstant.getValue() - constant.getValue();
                            operation = new Operation(Operation.Operator.LT, insideOpp.getOperand(0),
                                    new IntConstant(result));
                            stack.push(operation);
                        }
                        break;
                    case DIV:
                        if (insideOpp.getOperand(0) instanceof IntConstant
                                && insideOpp.getOperand(1) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(0);

                            int result = constant.getValue() / outsideConstant.getValue();
                            operation = new Operation(Operation.Operator.LT, insideOpp.getOperand(1),
                                    new IntConstant(result));
                            if (constant.getValue() < 0) {
                                operation.setOperator(Operation.Operator.GT);
                            }
                            stack.push(operation);
                        } else if (insideOpp.getOperand(1) instanceof IntConstant
                                && insideOpp.getOperand(0) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(1);

                            int result = constant.getValue() / outsideConstant.getValue();
                            operation = new Operation(Operation.Operator.LT, insideOpp.getOperand(0),
                                    new IntConstant(result));
                            if (constant.getValue() < 0) {
                                operation.setOperator(Operation.Operator.GT);
                            }
                            stack.push(operation);
                        }
                        break;
                    case MUL:
                        if (insideOpp.getOperand(0) instanceof IntConstant
                                && insideOpp.getOperand(1) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(0);

                            int result = outsideConstant.getValue() / constant.getValue();
                            operation = new Operation(Operation.Operator.LT, insideOpp.getOperand(1),
                                    new IntConstant(result));
                            if (constant.getValue() < 0) {
                                operation.setOperator(Operation.Operator.GT);
                            }
                            stack.push(operation);
                        } else if (insideOpp.getOperand(1) instanceof IntConstant
                                && insideOpp.getOperand(0) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(1);

                            int result = outsideConstant.getValue() / constant.getValue();
                            operation = new Operation(Operation.Operator.LT, insideOpp.getOperand(0),
                                    new IntConstant(result));
                            if (constant.getValue() < 0) {
                                operation.setOperator(Operation.Operator.GT);
                            }
                            stack.push(operation);
                        }
                        break;
                    default:
                        System.out.println("Hit default 4... Weird");
                        break;
                    }
                    break;
                case LE:
                    switch (insideOpp.getOperator()) {
                    case SUB:
                        if (insideOpp.getOperand(0) instanceof IntConstant
                                && insideOpp.getOperand(1) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(0);

                            int result = constant.getValue() - outsideConstant.getValue();
                            operation = new Operation(Operation.Operator.LE, insideOpp.getOperand(1),
                                    new IntConstant(result));
                            stack.push(operation);
                        } else if (insideOpp.getOperand(1) instanceof IntConstant
                                && insideOpp.getOperand(0) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(1);

                            int result = outsideConstant.getValue() + constant.getValue();
                            operation = new Operation(Operation.Operator.LE, insideOpp.getOperand(0),
                                    new IntConstant(result));
                            stack.push(operation);
                        }
                        break;
                    case ADD:
                        if (insideOpp.getOperand(0) instanceof IntConstant
                                && insideOpp.getOperand(1) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(0);

                            int result = constant.getValue() - outsideConstant.getValue();
                            operation = new Operation(Operation.Operator.LE, insideOpp.getOperand(1),
                                    new IntConstant(result));
                            stack.push(operation);
                        } else if (insideOpp.getOperand(1) instanceof IntConstant
                                && insideOpp.getOperand(0) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(1);

                            int result = constant.getValue() - outsideConstant.getValue();
                            operation = new Operation(Operation.Operator.LE, insideOpp.getOperand(0),
                                    new IntConstant(result));
                            stack.push(operation);
                        }
                        break;
                    case DIV:
                        if (insideOpp.getOperand(0) instanceof IntConstant
                                && insideOpp.getOperand(1) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(0);

                            int result = constant.getValue() / outsideConstant.getValue();
                            operation = new Operation(Operation.Operator.LE, insideOpp.getOperand(1),
                                    new IntConstant(result));
                            stack.push(operation);
                        } else if (insideOpp.getOperand(1) instanceof IntConstant
                                && insideOpp.getOperand(0) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(1);

                            int result = outsideConstant.getValue() / constant.getValue();
                            operation = new Operation(Operation.Operator.LE, insideOpp.getOperand(0),
                                    new IntConstant(result));
                            stack.push(operation);
                        }
                        break;
                    case MUL:
                        if (insideOpp.getOperand(0) instanceof IntConstant
                                && insideOpp.getOperand(1) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(0);

                            int result = outsideConstant.getValue() / constant.getValue();
                            operation = new Operation(Operation.Operator.LE, insideOpp.getOperand(1),
                                    new IntConstant(result));
                            if (constant.getValue() < 0) {
                                operation.setOperator(Operation.Operator.GE);
                            }
                            stack.push(operation);
                        } else if (insideOpp.getOperand(1) instanceof IntConstant
                                && insideOpp.getOperand(0) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(1);

                            int result = outsideConstant.getValue() / constant.getValue();
                            operation = new Operation(Operation.Operator.LE, insideOpp.getOperand(0),
                                    new IntConstant(result));
                            if (constant.getValue() < 0) {
                                operation.setOperator(Operation.Operator.GE);
                            }
                            stack.push(operation);
                        }
                        break;
                    default:
                        System.out.println("Hit default 5... Weird");
                        break;
                    }
                    break;
                case GT:
                    switch (insideOpp.getOperator()) {
                    case SUB:
                        if (insideOpp.getOperand(0) instanceof IntConstant
                                && insideOpp.getOperand(1) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(0);

                            int result = constant.getValue() - outsideConstant.getValue();
                            operation = new Operation(Operation.Operator.GT, insideOpp.getOperand(1),
                                    new IntConstant(result));
                            stack.push(operation);
                        } else if (insideOpp.getOperand(1) instanceof IntConstant
                                && insideOpp.getOperand(0) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(1);

                            int result = outsideConstant.getValue() + constant.getValue();
                            operation = new Operation(Operation.Operator.GT, insideOpp.getOperand(0),
                                    new IntConstant(result));
                            stack.push(operation);
                        }
                        break;
                    case ADD:
                        if (insideOpp.getOperand(0) instanceof IntConstant
                                && insideOpp.getOperand(1) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(0);

                            int result = constant.getValue() - outsideConstant.getValue();
                            operation = new Operation(Operation.Operator.GT, insideOpp.getOperand(1),
                                    new IntConstant(result));
                            stack.push(operation);
                        } else if (insideOpp.getOperand(1) instanceof IntConstant
                                && insideOpp.getOperand(0) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(1);

                            int result = constant.getValue() - outsideConstant.getValue();
                            operation = new Operation(Operation.Operator.GT, insideOpp.getOperand(0),
                                    new IntConstant(result));
                            stack.push(operation);
                        }
                        break;
                    case DIV:
                        if (insideOpp.getOperand(0) instanceof IntConstant
                                && insideOpp.getOperand(1) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(0);

                            int result = constant.getValue() / outsideConstant.getValue();
                            operation = new Operation(Operation.Operator.GT, insideOpp.getOperand(1),
                                    new IntConstant(result));
                            stack.push(operation);
                        } else if (insideOpp.getOperand(1) instanceof IntConstant
                                && insideOpp.getOperand(0) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(1);

                            int result = outsideConstant.getValue() / constant.getValue();
                            operation = new Operation(Operation.Operator.GT, insideOpp.getOperand(0),
                                    new IntConstant(result));
                            stack.push(operation);
                        }
                        break;
                    case MUL:
                        if (insideOpp.getOperand(0) instanceof IntConstant
                                && insideOpp.getOperand(1) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(0);

                            int result = outsideConstant.getValue() / constant.getValue();
                            operation = new Operation(Operation.Operator.GT, insideOpp.getOperand(1),
                                    new IntConstant(result));
                            if (constant.getValue() < 0) {
                                operation.setOperator(Operation.Operator.LT);
                            }
                            stack.push(operation);
                        } else if (insideOpp.getOperand(1) instanceof IntConstant
                                && insideOpp.getOperand(0) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(1);

                            int result = outsideConstant.getValue() / constant.getValue();
                            operation = new Operation(Operation.Operator.GT, insideOpp.getOperand(0),
                                    new IntConstant(result));
                            if (constant.getValue() < 0) {
                                operation.setOperator(Operation.Operator.LT);
                            }
                            stack.push(operation);
                        }
                        break;
                    default:
                        System.out.println("Hit default 6... Weird");
                        break;
                    }
                    break;
                case GE:
                    switch (insideOpp.getOperator()) {
                    case SUB:
                        if (insideOpp.getOperand(0) instanceof IntConstant
                                && insideOpp.getOperand(1) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(0);

                            int result = constant.getValue() - outsideConstant.getValue();
                            operation = new Operation(Operation.Operator.GE, insideOpp.getOperand(1),
                                    new IntConstant(result));
                            stack.push(operation);
                        } else if (insideOpp.getOperand(1) instanceof IntConstant
                                && insideOpp.getOperand(0) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(1);

                            int result = outsideConstant.getValue() + constant.getValue();
                            operation = new Operation(Operation.Operator.GE, insideOpp.getOperand(0),
                                    new IntConstant(result));
                            stack.push(operation);
                        }
                        break;
                    case ADD:
                        if (insideOpp.getOperand(0) instanceof IntConstant
                                && insideOpp.getOperand(1) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(0);

                            int result = constant.getValue() - outsideConstant.getValue();
                            operation = new Operation(Operation.Operator.GE, insideOpp.getOperand(1),
                                    new IntConstant(result));
                            stack.push(operation);
                        } else if (insideOpp.getOperand(1) instanceof IntConstant
                                && insideOpp.getOperand(0) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(1);

                            int result = constant.getValue() - outsideConstant.getValue();
                            operation = new Operation(Operation.Operator.GE, insideOpp.getOperand(0),
                                    new IntConstant(result));
                            stack.push(operation);
                        }
                        break;
                    case DIV:
                        if (insideOpp.getOperand(0) instanceof IntConstant
                                && insideOpp.getOperand(1) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(0);

                            int result = constant.getValue() / outsideConstant.getValue();
                            operation = new Operation(Operation.Operator.GE, insideOpp.getOperand(1),
                                    new IntConstant(result));
                            stack.push(operation);
                        } else if (insideOpp.getOperand(1) instanceof IntConstant
                                && insideOpp.getOperand(0) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(1);

                            int result = outsideConstant.getValue() / constant.getValue();
                            operation = new Operation(Operation.Operator.GE, insideOpp.getOperand(0),
                                    new IntConstant(result));
                            stack.push(operation);
                        }
                        break;
                    case MUL:
                        if (insideOpp.getOperand(0) instanceof IntConstant
                                && insideOpp.getOperand(1) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(0);

                            int result = outsideConstant.getValue() / constant.getValue();
                            operation = new Operation(Operation.Operator.GE, insideOpp.getOperand(1),
                                    new IntConstant(result));
                            if (constant.getValue() < 0) {
                                operation.setOperator(Operation.Operator.LE);
                            }
                            stack.push(operation);
                        } else if (insideOpp.getOperand(1) instanceof IntConstant
                                && insideOpp.getOperand(0) instanceof IntVariable) {
                            IntConstant constant = (IntConstant) insideOpp.getOperand(1);

                            int result = outsideConstant.getValue() / constant.getValue();
                            operation = new Operation(Operation.Operator.GE, insideOpp.getOperand(0),
                                    new IntConstant(result));
                            if (constant.getValue() < 0) {
                                operation.setOperator(Operation.Operator.LE);
                            }
                            stack.push(operation);
                        }
                        break;
                    default:
                        System.out.println("Hit default 7... Weird");
                        break;
                    }
                    break;
                default:
                    System.out.println("Hit the default here. Odd");
                    break;
                }
            } else {
                System.out.println("Have none of the above");
                simplified = false;
                stack.push(new Operation(op, l, r));
            }
        }
    }
}