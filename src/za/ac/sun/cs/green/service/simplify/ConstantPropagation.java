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

import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.service.canonizer.SATCanonizerService;
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

    /**
     * Request is Processed. An expression, variable map and instance is instantiated and the result of the
     * instance is returned.
     * @param instance
     *            the instance to solve
     * @return
     *         instance result
     */
    @Override
    public Set<Instance> processRequest(Instance instance) {
        @SuppressWarnings("unchecked")
        Set<Instance> result = (Set<Instance>) instance.getData(getClass());
        if (result == null) {
            final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
            final Expression e = simpies(instance.getFullExpression(), map);
            final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
            result = Collections.singleton(i);
            instance.setData(getClass(), result);
        }
        return result;
    }

    /**
     * An instance of ConstantPropagation is created. The expression is stored and and printed out before
     * propagation commences. If it fails to do so, an error message will be generated.
     * @param expression
     * @param map
     * @return
     */
    public Expression simpies(Expression expression,
                               Map<Variable, Variable> map) {
        try {
            log.log(Level.FINEST, "Before Propagation: " + expression);
            invocations++;
            ConstantPropagation.propConst orderingVisitor = new ConstantPropagation.propConst();
            expression.accept(orderingVisitor);
            expression = orderingVisitor.getExpression();
            log.log(Level.FINEST, "After Propagation: " + expression);
            return expression;
        } catch (VisitorException x) {
            log.log(Level.SEVERE,
                    "encountered an exception -- this should not be happening!",
                    x);
        }
        return null;
    }

    /**
     * An invocation message is parsed to the report.
     * @param reporter
     *            the mechanism through which reporting is done
     *
     */
    @Override
    public void report(Reporter reporter) {
        reporter.report(getClass().getSimpleName(), "invocations = " + invocations);
    }


    private static class propConst extends Visitor {

        private Stack<Expression> stack;
        private Map<IntVariable,IntConstant> varmap;

        public propConst() {
            stack = new Stack<Expression>();
            varmap = new HashMap<>();
        }

        /**
         * Pop item off stack
         * @return
         */
        public Expression getExpression() {
            return stack.pop();
        }

        /**
         * Posts visit for constant value
         * @param constant
         */
        @Override
        public void postVisit(IntConstant constant) {
            stack.push(constant);
        }

        /**
         * Post visit for variable value
         * @param variable
         */
        @Override
        public void postVisit(IntVariable variable) {
            stack.push(variable);
        }

        /**
         * The order of the left and right operands are identified and put onto map accordingly.
         * A final Operation is pushed to the stack.
         *
         * @param operation
         * @throws VisitorException
         */
        @Override
        public void postVisit(Operation operation) throws VisitorException {

            Operation.Operator op = operation.getOperator();

            Expression r = stack.pop(); // variables and constants
            Expression l = stack.pop();

            // Manipulate Variables

            if (op == Operation.Operator.EQ) { // " = "
                if (l instanceof IntConstant && r instanceof IntVariable) { // checks type of operands
                    varmap.put((IntVariable) r, (IntConstant) l);
                } else if (l  instanceof IntVariable && r instanceof IntConstant) {
                    varmap.put((IntVariable) l, (IntConstant) r);
                }
            } else {
                if (r instanceof IntVariable && varmap.containsKey(r)) {
                    r = varmap.get(r);
                } else if (l instanceof IntVariable && varmap.containsKey(l)) {
                    l = varmap.get(l);
                }
            }

            stack.push(new Operation(op, l, r));

        }

    }

}
