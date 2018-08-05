package za.ac.sun.cs.green.service.simplify;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.*;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.util.Reporter;

import java.util.*;
import java.util.logging.Level;

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
            final Expression e = simplify(instance.getFullExpression(), map);
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

    public Expression simplify(Expression expression, Map<Variable, Variable> map) {
        try {
            log.log(Level.FINEST, "Before Simplification: " + expression);
            invocations++;
            SimplificationVisitor simplificationVisitor = new SimplificationVisitor();
            expression.accept(simplificationVisitor);
            Expression simplified = simplificationVisitor.getExpression();
            log.log(Level.FINEST, "After Simplification: " + simplified);
            return simplified;
        } catch (VisitorException x) {
            log.log(Level.SEVERE, "encountered an exception -- this should not be happening!", x);
        }
        return null;
    }

    public class SimplificationVisitor extends Visitor {

        private Stack<Expression> stack;
        private TreeMap<IntVariable, IntConstant> map;

        public SimplificationVisitor() {
            stack = new Stack<Expression>();
            map = new TreeMap<IntVariable, IntConstant>();
        }

        public Expression getExpression() {
            return stack.pop();
        }

        @Override
        public void preVisit(Operation operation) throws VisitorException {
            if (operation.getOperator() == Operation.Operator.EQ) {
                Expression op0 = operation.getOperand(0);
                Expression op1 = operation.getOperand(1);
                if ((op0 instanceof IntVariable) && (op1 instanceof IntConstant)) {
                    map.put((IntVariable) op0, (IntConstant) op1);
                } else if ((op0 instanceof IntConstant) && (op1 instanceof IntVariable)) {
                    map.put((IntVariable) op1, (IntConstant) op0);
                }
            }
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
            Operation.Operator operator = operation.getOperator();
            Expression r = stack.pop();
            Expression l = stack.pop();
            if (!operator.equals(Operation.Operator.EQ)) {
                if (map.containsKey(r)) {
                    r = map.get(r);
                }
                if (map.containsKey(l)) {
                    l = map.get(l);
                }
            }
            Operation newOperation = new Operation(operator, l, r);
            stack.push(newOperation);
        }

    }
}
