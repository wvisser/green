package za.ac.sun.cs.green.service.simplify;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.*;
import za.ac.sun.cs.green.service.BasicService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;

/**
 * Class Defines a Service which Propagates Constants
 */
public class ConstantPropagationService extends BasicService {

    /**
     * Constructor for the basic service. It simply initializes its three
     * attributes.
     *
     * @param solver the {@link Green} solver this service will be added to
     */
    public ConstantPropagationService(Green solver) {
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
            log.log(Level.FINEST, "Before Constant Propagation: " + expression);

            if (map == null) {
                throw new VisitorException("");
            }

            ConstantPropagationService.ConstantPropagationVisitor conProgVisitor = new ConstantPropagationService.ConstantPropagationVisitor();
            expression.accept(conProgVisitor);
            expression = conProgVisitor.getExpression();
            log.log(Level.FINEST, "After Constant Propagation: " + expression);
            return expression;
        } catch (VisitorException x) {
            log.log(Level.SEVERE,
                    "encountered an exception -- this should not be happening!",
                    x);
        }
        return null;
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
                    stack.push(new Operation(op, l, r));
                // Do we have any vars we can replace?
                } else if (r instanceof IntVariable || l instanceof  IntVariable) {

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
                } else {
                    // Shove everything else back onto the op stack
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
