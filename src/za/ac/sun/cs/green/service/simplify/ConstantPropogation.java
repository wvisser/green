package za.ac.sun.cs.green.service.simplify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.Constant;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.VisitorException;
import za.ac.sun.cs.green.service.BasicService;

/**
 *
 * @author 19770235
 */
public class ConstantPropogation extends BasicService {

    public ConstantPropogation(Green solver) {
        super(solver);
    }

    @Override
    public Set<Instance> processRequest(Instance instance) {
        @SuppressWarnings("unchecked")
        Set<Instance> result = (Set<Instance>) instance.getData(getClass());
        if (result == null) {
            final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
            final Expression e = constant_propogation(instance.getFullExpression(), map);
            final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
            result = Collections.singleton(i);
            instance.setData(getClass(), result);
        }
        return result;
    }

    public Expression constant_propogation(Expression expression, Map<Variable, Variable> map) {
        try {
            log.log(Level.FINEST, "Before Constant Propogation: " + expression);
            ConstantPropogation.ConstantPropogationVisitor constantPropogationVisitor = new ConstantPropogation.ConstantPropogationVisitor();
            expression.accept(constantPropogationVisitor);
            Expression processed = constantPropogationVisitor.getExpression();
            log.log(Level.FINEST, "After Constant Propogation: " + processed);
            return processed;
        } catch (VisitorException x) {
            log.log(Level.FINEST,
                    "encountered an exception -- this should not be happening!",
                    x);
        }
        return null;
    }

    private static class ConstantPropogationVisitor extends Visitor {

        Stack<Expression> stack;

        public Expression getExpression() {
            return stack.pop();
        }

        @Override
        public void postVisit(Constant constant) {
            stack.push(constant);
        }

        @Override
        public void postVisit(Variable variable) {
            stack.push(variable);
        }

        @Override
        public void postVisit(Operation operation) {
            stack.push(operation);
        }

    }

}
