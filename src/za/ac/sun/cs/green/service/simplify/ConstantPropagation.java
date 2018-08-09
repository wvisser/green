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
            log.log(Level.FINEST, "Before Canonization: " + expression);
            invocations++;
            OrderingVisitor orderingVisitor = new OrderingVisitor();
            expression.accept(orderingVisitor);
            expression = orderingVisitor.getExpression();
            /*CanonizationVisitor canonizationVisitor = new CanonizationVisitor();
            expression.accept(canonizationVisitor);
            Expression canonized = canonizationVisitor.getExpression();
            if (canonized != null) {
                canonized = new Renamer(map, canonizationVisitor.getVariableSet()).rename(canonized);
            }*/
            //log.log(Level.FINEST, "After Canonization: " + canonized);
            //return canonized;
            return expression;
        } catch (VisitorException x) {
            log.log(Level.SEVERE, "encountered an exception -- this should not be happening!", x);
        }
        return null;
    }

    private static class ConstantPropagationVisitor extends Visitor {
        private Stack<Expression> stack;
        private Map<IntVariable, IntConstant> variableMap;

        public ConstantPropagationVisitor() {
            stack = new Stack<Expression>();
            variableMap = new Map<IntVariable, IntConstant>();
        }
    }
}