package za.ac.sun.cs.green.service.simplify;

import static org.junit.Assert.*;

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

import java.util.Arrays;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;
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
import za.ac.sun.cs.green.util.Configuration;


public class ConstantPropogation  extends BasicService{

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
            invocations++;
            log.log(Level.FINEST, "Before propogating: " + expression);
            SimpleVisitor simpleVisitor = new SimpleVisitor();
            expression.accept(simpleVisitor);
            expression = simpleVisitor.getExpression();

            log.log(Level.FINEST, "After propogating: " + expression);
            return expression;
        } catch (VisitorException x) {
            log.log(Level.SEVERE,
                    "encountered an exception -- this should not be happening!",x);
        }
        return expression;
    }


    private static class SimpleVisitor extends Visitor {
        private Stack<Expression> stack;
        private Map<IntVariable, IntConstant> map;

        public SimpleVisitor() {
            stack = new Stack<Expression>();
            map = new TreeMap<IntVariable, IntConstant>();
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
        public void beforeVisit(Operation operation) throws VisitorException {
            Operation.Operator op = operation.getOperator();
            Operation.Operator nop = null;

            if (op.equals(Operation.Operator.EQ)) {
                Expression l = operation.getOperand(0);
                Expression r = operation.getOperand(1);

                if ((l instanceof IntVariable) && (r instanceof IntConstant)) {
                    map.put((IntVariable) l, (IntConstant) r);

                } else if ((l instanceof IntConstant) && (r instanceof IntVariable)) {
                    map.put((IntVariable) r, (IntConstant) l);
                }

            }
        }

        @Override
        public void postVisit(Operation operation) {

            Operation.Operator op = operation.getOperator();
            if (stack.size() >= 2) {
                Expression r = stack.pop();
                Expression l = stack.pop();

                if (!op.equals(Operation.Operator.EQ)) {
                    if (l instanceof IntVariable && map.containsKey(l)) {
                            l = map.get(l);
                        }

                    if (r instanceof IntVariable && map.containsKey(r)) {
                            r = map.get(r);
                        }
                }

                Operation e = new Operation(operation.getOperator(), l, r);
                stack.push(e);
            }
        }
    }
}
