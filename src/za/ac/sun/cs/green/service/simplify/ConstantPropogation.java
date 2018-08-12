package za.ac.sun.cs.green.service.simplify;

import org.chocosolver.solver.constraints.nary.nValue.amnv.mis.F;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.*;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.util.Reporter;

import java.util.*;
import java.util.logging.Level;

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
        System.out.println("Processing request");
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
        System.out.println("Simplifying");
        try {
            System.out.println("######################################################################################");
            TreeMap<IntVariable, IntConstant> map = new TreeMap<>();
            invocations++;
            log.log(Level.FINEST, "Before Simplification: " + expression);
            SimplificationVisitor simplificationVisitor = new SimplificationVisitor(map);
            expression.accept(simplificationVisitor);
            Expression simplified = simplificationVisitor.getExpression();
            while (!simplified.equals(expression)) {
                expression = simplified;
                simplificationVisitor = new SimplificationVisitor(map);
                simplified.accept(simplificationVisitor);
                simplified = simplificationVisitor.getExpression();
            }
            log.log(Level.FINEST, "Original After Simplification" + expression);
            log.log(Level.FINEST, "Simplified After Simplification: " + simplified);
            System.out.println("######################################################################################");
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

        public SimplificationVisitor(TreeMap<IntVariable, IntConstant> map) {
            stack = new Stack<Expression>();
            opStack = new Stack<>();
            this.map = map;
            constraintsMap = new TreeMap<>();
            madeChange = false;
        }

        public Expression getExpression() {
            if (opStack.size() == 0) {
                return null;
            }
            while (opStack.size() > 1) {
                Operation opRight = (Operation) propagateConstants(opStack.pop());
                Operation opLeft  = (Operation) propagateConstants(opStack.pop());
                opRight = (Operation) simplify(opRight);
                opLeft  = (Operation) simplify(opLeft);
                opStack.push(new Operation(Operation.Operator.AND, opLeft, opRight));
            }
            return (Operation) simplify(opStack.pop());
        }

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

            opStack.push(new Operation(operation.getOperator(), l, r));
        }

        @Override
        public void preVisit(IntConstant constant) {
        }

        @Override
        public void preVisit(IntVariable variable) {
        }

        @Override
        public void postVisit(IntConstant constant) {
        }

        @Override
        public void postVisit(IntVariable variable) {
        }

        @Override
        public void postVisit(Operation operation) {
        }

        private Expression propagateConstants(Expression e) {
            if ((e instanceof IntConstant) || (e instanceof IntVariable)) {
                return e;
            }

            Operation operation = (Operation) e;
            Expression l = operation.getOperand(0);
            Expression r = operation.getOperand(1);

            if ((l instanceof IntVariable) && (r instanceof IntConstant)) {
                if (operation.getOperator() == Operation.Operator.EQ) {
                    map.put((IntVariable) l, (IntConstant) r);
                } else {
                    constraintsMap.put((IntVariable) l, operation);
                }
            } else if ((r instanceof IntVariable) && (l instanceof IntConstant)) {
                if (operation.getOperator() == Operation.Operator.EQ) {
                    map.put((IntVariable) r, (IntConstant) l);
                } else {
                    constraintsMap.put((IntVariable) r, operation);
                }
            } else if ((l instanceof IntVariable) && (r instanceof IntVariable)) {
                if (map.containsKey(l)) {
                    l = map.get(l);
                }
                if (map.containsKey(r)) {
                    r = map.get(r);
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

            Expression l = operation.getOperand(0);
            Expression r = operation.getOperand(1);

            if ((l instanceof IntConstant) && (r instanceof IntConstant)) {
                System.out.println("Simplifying constant operation");
                return simplify_constant_operation(operation);
            }

            if ((operation.getOperator() == Operation.Operator.AND) && (l instanceof Operation) && (r instanceof Operation)) {
                if (l.equals(trueOp) && r.equals(trueOp)) {
                    return trueOp;
                } else if (l.equals(falseOp) || r.equals(falseOp)) {
                    return falseOp;
                }
            }

            return e;
        }

        private Operation simplify_constant_operation(Operation operation) {
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
                default:
                    return operation;
            }
        }

        public boolean hasMadeChange() {
            return madeChange;
        }

    }
}
