package za.ac.sun.cs.green.service.simplify;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.*;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.util.Reporter;

import java.util.*;
import java.util.logging.Level;

import static za.ac.sun.cs.green.expr.Operation.Operator.EQ;
import static za.ac.sun.cs.green.expr.Operation.Operator.GE;
import static za.ac.sun.cs.green.expr.Operation.Operator.GT;

public class ConstantPropogation extends BasicService {

    /**
     * Number of times the slicer has been invoked.
     */
    private int invocations = 0;

    public ConstantPropogation(Green solver) {
        super(solver);
    }

    @Override
    public Set<Instance> processRequest(Instance instance) {
        @SuppressWarnings("unchecked")
        Set<Instance> result = (Set<Instance>) instance.getData(getClass());
        if (result == null) {
            final Expression e = simplify(instance.getFullExpression());
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

    public Expression simplify(Expression expression) {
        try {
            TreeMap<IntVariable, IntConstant> map = new TreeMap<>();
            TreeMap<IntVariable, Operation> constraintsMap = new TreeMap<>();
            invocations++;
            log.log(Level.FINEST, "Before Simplification: " + expression);
            SimplificationVisitor simplificationVisitor = new SimplificationVisitor(map, constraintsMap);
            expression.accept(simplificationVisitor);
            Expression simplified = simplificationVisitor.getExpression();
            do {
                expression = simplified;
                simplificationVisitor = new SimplificationVisitor(map, constraintsMap);
                simplified.accept(simplificationVisitor);
                simplified = simplificationVisitor.getExpression();
                simplificationVisitor = new SimplificationVisitor(map, constraintsMap);
                simplified.accept(simplificationVisitor);
                simplified = simplificationVisitor.getExpression();
            } while (!simplified.equals(expression) || simplificationVisitor.madeChange);
            log.log(Level.FINEST, "Original After Simplification" + expression);
            log.log(Level.FINEST, "Simplified After Simplification: " + simplified);
            return simplified;
        } catch (VisitorException x) {
            log.log(Level.SEVERE, "encountered an exception -- this should not be happening!", x);
        }
        return null;
    }

    public class SimplificationVisitor extends Visitor {

        private Stack<Expression> stack;
        private Stack<Operation> opStack;
        private TreeMap<IntVariable, IntConstant> map;
        private TreeMap<IntVariable, Operation> constraintsMap;
        public boolean madeChange;

        /**
         * Constructor for the SimplificationVisitor object.
         *
         * @param map a TreeMap containing values for variables that have already been defined.
         */
        public SimplificationVisitor(TreeMap<IntVariable, IntConstant> map, TreeMap<IntVariable, Operation> constraintsMap) {
            stack = new Stack<Expression>();
            opStack = new Stack<>();
            this.map = map;
            this.constraintsMap = new TreeMap<>();
            madeChange = false;
        }

        /**
         * Builds the simplified expression and returns it.
         *
         * @return the simplified expression
         */
        public Expression getExpression() {
            Operation trueOp = new Operation(EQ, new IntConstant(0), new IntConstant(0));
            Operation falseOp = new Operation(EQ, new IntConstant(0), new IntConstant(1));
            int constraintsSatisfied;
            if (opStack.size() == 0) {
                return null;
            }
            while (opStack.size() > 1) {
                Operation opRight = (Operation) propagateConstants(opStack.pop());
                Operation opLeft  = (Operation) propagateConstants(opStack.pop());
                opRight = (Operation) simplify(opRight);
                constraintsSatisfied = checkConstraints(opRight);
                if (constraintsSatisfied == 1) {
                    opRight = trueOp;
                } else if (constraintsSatisfied == -1) {
                    opRight = falseOp;
                }
                opLeft  = (Operation) simplify(opLeft);
                constraintsSatisfied = checkConstraints(opLeft);
                if (constraintsSatisfied == 1) {
                    opLeft = trueOp;
                } else if (constraintsSatisfied == -1) {
                    opLeft = falseOp;
                }
                opStack.push(new Operation(Operation.Operator.AND, opLeft, opRight));
            }
            Operation op = opStack.pop();
            op = (Operation) simplify(op);
            constraintsSatisfied = checkConstraints(op);
            if (constraintsSatisfied == 1) {
                op = trueOp;
            } else if (constraintsSatisfied == -1) {
                op = falseOp;
            }

            return op;
        }

        /**
         * Propagates constants for the current operation and pushes the operation to the
         * operation stack if applicable.
         *
         * @param operation the operation which must be processed
         * @throws VisitorException
         */
        @Override
        public void preVisit(Operation operation) throws VisitorException {
            switch (operation.getOperator()) {
                case EQ:
                case GE:
                case GT:
                case LE:
                case LT:
                case NE:
                    break;
                default:
                    return;
            }

            operation = (Operation) propagateConstants(operation);
            Expression l = operation.getOperand(0);
            Expression r = operation.getOperand(1);

            l = propagateConstants(l);
            r = propagateConstants(r);
            l = simplify(l);
            r = simplify(r);

            opStack.push(new Operation(operation.getOperator(), l, r));
        }


        private Expression propagateConstants(Expression e) {
            if ((e instanceof IntConstant) || (e instanceof IntVariable)) {
                return e;
            }

            Operation operation = (Operation) e;
            Expression l = operation.getOperand(0);
            Expression r = operation.getOperand(1);

            if ((l instanceof IntVariable) && (r instanceof IntConstant)) {
                switch (operation.getOperator()) {
                    case EQ:
                        if (!map.containsKey(l)) {
                            map.put((IntVariable) l, (IntConstant) r);
                            madeChange = true;
                        }
                        constraintsMap.put((IntVariable) l, operation);
                        break;
                    case GT:
                    case GE:
                    case LT:
                    case LE:
                    case NE:
                        if (!constraintsMap.containsKey(r)) {
                            constraintsMap.put((IntVariable) l, operation);
                        }
                        break;
                    default:
                        if (map.containsKey(l)) {
                            l = map.get(l);
                        }
                }
            } else if ((r instanceof IntVariable) && (l instanceof IntConstant)) {
                switch (operation.getOperator()) {
                    case EQ:
                        if (!map.containsKey(r)) {
                            map.put((IntVariable) r, (IntConstant) l);
                            madeChange = true;
                        }
                        constraintsMap.put((IntVariable) r, operation);
                        break;
                    case GT:
                    case GE:
                    case LT:
                    case LE:
                    case NE:
                        if (!constraintsMap.containsKey(r)) {
                            constraintsMap.put((IntVariable) r, operation);
                        }
                        break;
                    default:
                        if (map.containsKey(r)) {
                            r = map.get(r);
                        }
                }
            } else if ((l instanceof IntVariable) && (r instanceof IntVariable)) {
                if (map.containsKey(l)) {
                    l = map.get(l);
                    madeChange = true;
                }
                if (map.containsKey(r)) {
                    r = map.get(r);
                    madeChange = true;
                }
            }



            return new Operation(operation.getOperator(), l, r);
        }

        private Expression simplify(Expression e) {
            if ((e instanceof IntVariable) || (e instanceof IntConstant)) {
                return e;
            }

            Operation operation = (Operation) e;
            Operation trueOp = new Operation(Operation.Operator.EQ, new IntConstant(0), new IntConstant(0));
            Operation falseOp = new Operation(Operation.Operator.EQ, new IntConstant(0), new IntConstant(1));

            Operation.Operator operator = operation.getOperator();
            Expression l = operation.getOperand(0);
            Expression r = operation.getOperand(1);

            int constraintsSatisfied = checkConstraints(operation);
            if (constraintsSatisfied == 1) {
                return trueOp;
            } else if (constraintsSatisfied == -1) {
                return falseOp;
            }

            if (((l instanceof IntVariable) && (r instanceof IntConstant)) || ((l instanceof IntConstant) && (r instanceof IntVariable))) {
                operation = (Operation) propagateConstants(operation);
                operator = operation.getOperator();
                l = operation.getOperand(0);
                r = operation.getOperand(1);

            }

            // In the case where both l and r are constants, we can evaluate absolutely.
            if ((l instanceof IntConstant) && (r instanceof IntConstant)) {
                return SimplifyConstantOperation(operation);
            }

            // Try to replace variables
            if ((l instanceof IntVariable) && (r instanceof IntVariable)) {
                if (map.containsKey(l)) {
                    l = map.get(l);
                }
                if (map.containsKey(r)) {
                    r = map.get(r);
                }
            }

            // Eliminate all the cases where there is nothing to do
            if (((l instanceof IntVariable) && (r instanceof IntVariable))
                    || ((l instanceof Operation)   && (r instanceof IntVariable))
                    || ((l instanceof IntVariable) && (r instanceof Operation))
                    || ((l instanceof IntVariable) && (r instanceof IntConstant))
                    || ((l instanceof IntConstant) && (r instanceof IntVariable))) {
                return operation;
            }


            switch (operator) {
                case AND:
                    if (falseOp.equals(l) || falseOp.equals(r)) {
                        return falseOp;
                    } else if (trueOp.equals(l)) {
                        return r;
                    } else if (trueOp.equals(r)) {
                        return l;
                    } else {
                        return operation;
                    }
                case OR:
                    if (trueOp.equals(l) || trueOp.equals(r)) {
                        return trueOp;
                    }
                case EQ:
                case GT:
                case GE:
                case LT:
                case LE:
                case NE:
                    return evaluateExpression(operation);
            }

            return e;
        }

        /**
         * This method carries over constants, i.e. x + 1 == 10 would become x == 9
         */
        private Expression evaluateExpression(Operation operation) {
            Expression l = operation.getOperand(0);
            Expression r = operation.getOperand(1);
            Expression newL = null;
            Expression newR = null;

            if (l instanceof IntConstant) {
                newL = new IntConstant(((IntConstant) l).getValue());
                if (!(r instanceof Operation)) {
                    log.log(Level.SEVERE, "Invalid expression in evaluateExpression: " + operation);
                    return operation;
                }
                Operation rop = (Operation) r;
                for (Expression e : rop.getOperands()) {
                    if (e instanceof IntConstant) {
                        switch (rop.getOperator()) {
                            case ADD:
                                newL = new IntConstant(((IntConstant) l).getValue() - ((IntConstant) e).getValue());
                                break;
                            case SUB:
                                newL = new IntConstant(((IntConstant) l).getValue() + ((IntConstant) e).getValue());
                                break;
                            case MUL:
                                if ((((IntConstant) l).getValue() % ((IntConstant) e).getValue()) == 0) {
                                    newL = new IntConstant(((IntConstant) l).getValue() / ((IntConstant) e).getValue());
                                }
                                break;
                            case DIV:
                                newL = new IntConstant(((IntConstant) l).getValue() * ((IntConstant) e).getValue());
                        }
                    } else {
                        newR = e;
                    }
                }
            } else if (r instanceof IntConstant) {
                newR = new IntConstant(((IntConstant) r).getValue());
                if (!(l instanceof Operation)) {
                    log.log(Level.SEVERE, "Invalid expression in evaluateExpression: " + operation);
                    return operation;
                }
                Operation lop = (Operation) l;
                for (Expression e : lop.getOperands()) {
                    if (e instanceof IntConstant) {
                        switch (lop.getOperator()) {
                            case ADD:
                                newR = new IntConstant(((IntConstant) r).getValue() - ((IntConstant) e).getValue());
                                break;
                            case SUB:
                                newR = new IntConstant(((IntConstant) r).getValue() + ((IntConstant) e).getValue());
                                break;
                            case MUL:
                                if ((((IntConstant) r).getValue() % ((IntConstant) e).getValue()) == 0) {
                                    newR = new IntConstant(((IntConstant) r).getValue() / ((IntConstant) e).getValue());
                                }
                                break;
                            case DIV:
                                newR = new IntConstant(((IntConstant) r).getValue() * ((IntConstant) e).getValue());
                        }
                    } else {
                        newL = e;
                    }
                }
            }

            return new Operation(operation.getOperator(), newL, newR);
        }

        /**
         * If both operands of an expression are constants, the value of the expression is fully determined. This method
         * calculates that value.
         * @param operation the operation that must be simplified
         * @return the simplified operation
         */
        private Expression SimplifyConstantOperation(Operation operation) {
            Operation trueOp = new Operation(Operation.Operator.EQ, new IntConstant(0), new IntConstant(0));
            Operation falseOp = new Operation(Operation.Operator.EQ, new IntConstant(0), new IntConstant(1));
            int l = ((IntConstant) operation.getOperand(0)).getValue();
            int r = ((IntConstant) operation.getOperand(1)).getValue();
            switch (operation.getOperator()) {
                case EQ:
                    if (l == r) return trueOp;
                    else        return falseOp;
                case GE:
                    if (l >= r) return trueOp;
                    else        return falseOp;
                case GT:
                    if (l > r)  return trueOp;
                    else        return falseOp;
                case LE:
                    if (l <= r) return trueOp;
                    else        return falseOp;
                case LT:
                    if (l < r)  return trueOp;
                    else        return falseOp;
                case NE:
                    if (l != r) return trueOp;
                    else        return falseOp;
                case ADD:
                    return new IntConstant(l+r);
                case MUL:
                    return new IntConstant(l*r);
                case SUB:
                    return new IntConstant(l-r);
                default:
                    return operation;
            }
        }

        /**
         * Checks whether the given operation satisfies currently known constraints
         *
         * @param operation the operation which must be checked
         * @return 0 if it satisfies known constraints, 1 if it is a tautology and -1 if it contradicts an
         * existing constraint
         */
        private int checkConstraints(Operation operation) {
            Expression l = operation.getOperand(0);
            Expression r = operation.getOperand(1);

            if (!((l instanceof IntVariable) && (r instanceof IntConstant)) && !((r instanceof IntVariable) && (l instanceof IntConstant))) {
                return 0;
            }


            IntVariable var = (l instanceof IntVariable) ? (IntVariable) l : (IntVariable) r;
            IntConstant con = (l instanceof IntConstant) ? (IntConstant) l : (IntConstant) r;
            int val = con.getValue();

            Operation.Operator operator = operation.getOperator();

            // Ensure variable is on the left to reduce future code
            if (r instanceof IntConstant) {
                switch (operator) {
                    case GT:
                        operator = Operation.Operator.LT;
                        break;
                    case GE:
                        operator = Operation.Operator.LE;
                        break;
                    case LT:
                        operator = Operation.Operator.GT;
                        break;
                    case LE:
                        operator = Operation.Operator.GE;
                }
            }

            if (!constraintsMap.containsKey(var)) {
                return 0;
            }

            Operation constraint = constraintsMap.get(var);
            if (constraint.equals(operation)) {
                return 0;
            }
            Expression lc = constraint.getOperand(0);
            Expression rc = constraint.getOperand(1);
            int conValue = (lc instanceof IntConstant) ? ((IntConstant) lc).getValue() : ((IntConstant) rc).getValue();
            int lowBound = 0;
            int highBound = 99;

            if (lc instanceof IntVariable) {
                switch (constraint.getOperator()) {
                    case EQ:
                        lowBound = conValue;
                        highBound = conValue;
                        break;
                    case GE:
                        lowBound = conValue;
                        break;
                    case GT:
                        lowBound = conValue + 1;
                        break;
                    case LE:
                        highBound = conValue;
                        break;
                    case LT:
                        highBound = conValue - 1;
                        break;
                }
            } else {
                switch (constraint.getOperator()) {
                    case EQ:
                        lowBound = conValue;
                        highBound = conValue;
                        break;
                    case LE:
                        lowBound = conValue;
                        break;
                    case LT:
                        lowBound = conValue + 1;
                        break;
                    case GE:
                        highBound = conValue;
                        break;
                    case GT:
                        highBound = conValue - 1;
                        break;
                }

            }

            switch (operation.getOperator()) {
                case EQ:
                    if ((lowBound == highBound) && (lowBound != val)) {
                        return -1;
                    } else if ((lowBound == highBound) && (lowBound == val)) {
                        return 1;
                    } else {
                        return 0;
                    }
                case LT:
                    if (highBound < conValue) {
                        return 1;
                    } else if (lowBound >= conValue) {
                        return -1;
                    } else {
                        return 0;
                    }
            }

            return 0;
        }

    }
}
