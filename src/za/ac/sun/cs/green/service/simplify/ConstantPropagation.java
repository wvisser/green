package za.ac.sun.cs.green.service.simplify;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.VisitorException;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.service.canonizer.SATCanonizerService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
            //expression.
//            SATCanonizerService.OrderingVisitor orderingVisitor = new SATCanonizerService.OrderingVisitor();
//            expression.accept(orderingVisitor);
//            expression = orderingVisitor.getExpression();
//            SATCanonizerService.CanonizationVisitor canonizationVisitor = new SATCanonizerService.CanonizationVisitor();
//            expression.accept(canonizationVisitor);
//            Expression canonized = canonizationVisitor.getExpression();
//            if (canonized != null) {
//                canonized = new SATCanonizerService.Renamer(map,
//                        canonizationVisitor.getVariableSet()).rename(canonized);
//            }
//            log.log(Level.FINEST, "After Canonization: " + canonized);
            return null;
        } catch (VisitorException x) {
            log.log(Level.SEVERE,
                    "encountered an exception -- this should not be happening!",
                    x);
        }
        return null;
    }


}
