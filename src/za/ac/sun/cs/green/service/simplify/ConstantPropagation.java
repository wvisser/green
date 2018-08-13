package za.ac.sun.cs.green.service.simplify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
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
            final Expression e = propagateConstants(instance.getFullExpression(), map);
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

    public Expression propagateConstants(Expression expression, Map<Variable, Variable> map) {
        try {
            log.log(Level.FINEST, "Before Constant Propagation: " + expression);
            invocations++;

            ConstantPropagationVisitor constantPropagationVisitor = new ConstantPropagationVisitor();
            expression.accept(constantPropagationVisitor);
            Expression propagated = constantPropagationVisitor.getExpression();

            log.log(Level.FINEST, "After Constant Propagation: " + propagated);
            return propagated;
        } catch (VisitorException x) {
            log.log(Level.SEVERE, "encountered an exception -- this should not be happening!", x);
        }

        return null;
    }

    private static class ConstantPropagationVisitor extends Visitor {
        private Stack<Expression> stack;
        private Map<IntVariable, IntConstant> vMap;

        public ConstantPropagationVisitor() {
            stack = new Stack<Expression>();
            vMap = new TreeMap<IntVariable, IntConstant>();
        }

        public Expression getExpression() {
            Expression e = null;
            if (stack.isEmpty()) {
                return e;
            } else {
            	e = stack.pop();
            }

            return e;
        }

        @Override
        public void postVisit(Constant constant) {
            if (!(constant instanceof IntConstant)) {
                stack.clear();
            } else {
                stack.push(constant);
            }
        }

        @Override
        public void postVisit(Variable variable) {
            if (!(variable instanceof IntVariable)) {
                stack.clear();
            } else {
                stack.push(variable);
            }
        }

        @Override
        public void postVisit(Operation operation) throws VisitorException {
            int ss = stack.size();
            if (ss > 1) {
                Expression r = stack.pop();
                Expression l = stack.pop();
                Operation.Operator op = operation.getOperator();
                if (op.equals(Operation.Operator.EQ)) {
                    if ((l instanceof IntVariable) && (r instanceof IntConstant)) {
                        vMap.put((IntVariable) l, (IntConstant) r);
                    } else if ((r instanceof IntVariable) && (l instanceof IntConstant)) {
                        vMap.put((IntVariable) r, (IntConstant) l);
                    }
                    stack.push(new Operation(op, l, r));
                } else if (!op.equals(Operation.Operator.EQ)) {
                    if (vMap.containsKey(l)) {
                        l = vMap.get(l);
                    } else if (vMap.containsKey(r)) {
                        r = vMap.get(r);
                    }
                    stack.push(new Operation(op, l, r));
                }
            }
        }
    }
}
