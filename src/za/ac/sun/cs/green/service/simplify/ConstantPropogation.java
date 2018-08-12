package za.ac.sun.cs.green.service.simplify;
/* TODO: implemement
   1. propagating one equality
   2. adding simplification, which could introduce more equalities to propagate
   3. do full propagotion until you reach a fixpoint

*/
/* External */
import java.util.Set;
import java.util.Stack;

/* local */
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.expr.Constant;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.VisitorException;
import za.ac.sun.cs.green.util.Reporter;


public class ConstantPropogation extends BasicService {
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
           final Expression e = propogateConstants(instance.getFullExpression(), map);
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
public Expression propogateConstants(Expression expression,
           Map<Variable, Variable> map) {
               //Use the visitor to propogate constants
               try {
                   log.log(Level.FINEST, "Before ConstantPropogation: " + expression);
       			    invocations++;

                    ConstantPropogationVisitor propogationVisitor = new ConstantPropogationVisitor();
        			expression.accept(propogationVisitor);
        			expression = propogationVisitor.getExpression();

                    log.log(Level.FINEST, "After ConstantPropogation: " + canonized);
        			return expression;
               } catch (VisitorException x) {
                   log.log(Level.SEVERE,
       					"encountered an exception -- this should not be happening!",
       					x);
               }
               return null;
           }
private static class ConstantPropogationVisitor extends Visitor {
   private Stack<Expression> stack;

   public ConstantPropogationVisitor() {
	   stack = new Stack<Expression>();
   }
   public Expression getExpression() {
       if (!stack.isEmpty()) {
           Expression x = stack.pop();
           log.log(Level.FINEST, "stack.pop(): " + x);
       }

       Expression c = null;

       //TODO

       return (c == null) ? Operation.TRUE : c;
   }
   @Override
   public void postVisit(Constant constant) {
   //TODO
   }
   @Override
   public void postVisit(Variable variable) {
   //TODO
   }
   @Override
	public void postVisit(Operation operation) throws VisitorException {

   }
}
}
