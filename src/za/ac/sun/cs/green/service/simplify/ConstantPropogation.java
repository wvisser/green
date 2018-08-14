package za.ac.sun.cs.green.service.simplify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.ArrayList;

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
            final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
            final Expression e = cpropogate(instance.getFullExpression(), map);
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

    public Expression cpropogate(Expression expression, Map<Variable, Variable> map) {
        //receive an expression, and hook the visitor classes/instances into it here
        try {
            log.log(Level.FINEST, "Before cpropogation: " + expression);
            invocations++;
            PropogateVisitor propogateVisitor = new PropogateVisitor();
            expression.accept(propogateVisitor);
            Expression simplified = propogateVisitor.getExpression();
            log.log(Level.FINEST, "After cpropogation: " + simplified);
            return simplified;
        } catch (VisitorException x) {
            log.log(Level.SEVERE,
                    "encountered an exception -- this should not be happening!",
                    x);
        }
        return null;
    }

    private static class PropogateVisitor extends Visitor {//this is where we write the functions used to propogate the

        private Stack<Expression> stack;
        private HashMap<Variable, IntConstant> hmap;

        public PropogateVisitor() {
            stack = new Stack<Expression>();
            hmap = new HashMap<Variable, IntConstant>();
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
        public void postVisit(Operation operation) throws VisitorException {
            Operation.Operator op = operation.getOperator();
                Expression r = stack.pop();
                Expression l = stack.pop();
                if (op == Operation.Operator.EQ) {
               		 if ((r instanceof IntVariable) && (l instanceof IntConstant)) {
               		     hmap.put((Variable) r, (IntConstant) l);
               		 } else if ((r instanceof IntConstant) && (l instanceof IntVariable)) {
               		     hmap.put((Variable) l, (IntConstant) r);
               		 }
                } else if (hmap.containsKey(l)) {
                  	l = hmap.get(l);
                } else if (hmap.containsKey(r)) {
                		r = hmap.get(l);
                }
                stack.push(new Operation(op, l, r));
        }
    }
}
