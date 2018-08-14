package za.ac.sun.cs.green.service.simplify;

import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.VisitorException;

import java.util.*;
import java.util.logging.Level;


public class ConstantPropagation extends BasicService {

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

    public Expression propagate(Expression expression,
                                Map<Variable, Variable> map) {
        try {
            log.log(Level.FINEST, "Before Simplification: " + expression);

            ConstantPropagation.ConstantPropagationVisitor constantPropagationVisitor = new ConstantPropagation.ConstantPropagationVisitor();

            expression.accept(constantPropagationVisitor);
            expression = constantPropagationVisitor.getExpression();

            return expression;

        } catch (VisitorException x) {
            log.log(Level.SEVERE,
                    "encountered an exception -- this should not be happening!",
                    x);
        }
        return null;
    }

    /*
     * Called to start the constant propagation visit
     */
    public ConstantPropagation (Green solver) {
        super(solver);
    }

    /*
     *  Class extends visitor and is used to complete the propagation postVisit is used to
     *  save the values needed propagate the function correctly
     */
    private static class ConstantPropagationVisitor extends Visitor {

        private Stack<Expression> stack;

        private HashMap<IntVariable, IntConstant> hashmap;

        public ConstantPropagationVisitor() {
            stack = new Stack<Expression>();
            hashmap = new HashMap<IntVariable, IntConstant>();
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

            if (op.getArity() == 2) {
                Expression r = stack.pop();
                Expression l = stack.pop();
                if (r instanceof IntConstant && l instanceof IntVariable && op.name().equals("EQ")) {
                    hashmap.put((IntVariable) l, (IntConstant) r);


                } else if (r instanceof IntVariable && l instanceof IntConstant && op.name().equals("EQ")) {
                    hashmap.put((IntVariable) r, (IntConstant) l);


                } else if (r instanceof IntVariable || l instanceof IntVariable) {
                    if (hashmap.containsKey(l)) {
                        l = hashmap.get(l);
                    }
                    if (hashmap.containsKey(r)) {
                        r = hashmap.get(r);
                    }

                }
                stack.push(new Operation(op, l, r));
            }

        }
    }
}



