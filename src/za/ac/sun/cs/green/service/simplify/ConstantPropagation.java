package za.ac.sun.cs.green.service.simplify;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.*;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.service.canonizer.SATCanonizerService;

import java.util.*;
import java.util.logging.Level;

public class ConstantPropagation extends BasicService {

    /**
     * Number of times the slicer has been invoked.
     */
    private int invocations = 0;

    /**
     * Constructor for the basic service. It simply initializes its three
     * attributes.
     *
     * @param solver the {@link Green} solver this service will be added to
     */
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

    public Expression simplify(Expression expression,
                               Map<Variable, Variable> map) {
        try {
            log.log(Level.FINEST, "Before Simplification: " + expression);
            invocations++;

            if (map == null) {
                throw new VisitorException("");
            }

            ConstantPropagation.ConstantPropagationVisitor conProgVisitor = new ConstantPropagation.ConstantPropagationVisitor();
            expression.accept(conProgVisitor);
            return conProgVisitor.getExpression();
//            SATCanonizerService.CanonizationVisitor canonizationVisitor = new SATCanonizerService.CanonizationVisitor();
//            expression.accept(canonizationVisitor);
//            Expression canonized = canonizationVisitor.getExpression();
//            if (canonized != null) {
//                canonized = new SATCanonizerService.Renamer(map,
//                        canonizationVisitor.getVariableSet()).rename(canonized);
//            }
//            log.log(Level.FINEST, "After Canonization: " + canonized);
            //return null;
        } catch (VisitorException x) {
            log.log(Level.SEVERE,
                    "encountered an exception -- this should not be happening!",
                    x);
        }
        return null;
    }

    private static class ConstantPropagationVisitor extends Visitor {

        private Stack<Expression> stack;
        private Map<IntVariable, IntConstant> vars;

        public ConstantPropagationVisitor() {
            stack = new Stack<>();
            vars = new HashMap<>();
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
        public void postVisit(Operation operation) {
            Operation.Operator op = operation.getOperator();

            if (op.getArity() == 2) {
                Expression r = stack.pop();
                Expression l = stack.pop();

                if (op == Operation.Operator.EQ) {
                    if (r instanceof IntVariable && l instanceof IntConstant) {
                        vars.put((IntVariable) r, (IntConstant) l);
                    } else if (l instanceof IntVariable && r instanceof IntConstant) {
                        vars.put((IntVariable) l, (IntConstant) r);
                    }
                    stack.push(new Operation(op, l, r));
                } else if (r instanceof IntVariable || l instanceof  IntVariable) {
                    if (vars.containsKey(r)) {
                        r = vars.get(r);
                    }

                    if (vars.containsKey(l)) {
                        l = vars.get(l);
                    }
                    stack.push(new Operation(op, l, r));
                } else {
                    stack.push(new Operation(op, l, r));
                }
            } else {
                for (int i = op.getArity(); i > 0; i--) {
                    stack.pop();
                }
                stack.push(operation);
            }
        }

    }

}
