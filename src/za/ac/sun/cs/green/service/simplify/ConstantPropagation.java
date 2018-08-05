package za.ac.sun.cs.green.service.simplify;
/* TODO: implemement
   1. propagating one equality
   2. adding simplification, which could introduce more equalities to propagate
   3. do full propagation until you reach a fixpoint

*/
/* External */
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


public class ConstantPropagation extends BasicService {
   private int invocations = 0;

   public ConstantPropagation(Green solver) {
       super(solver);
   }

   @Override
   public Set<Instance> processRequest(Instance instance) {
      //TODO
   }

   @Override
	public void report(Reporter reporter) {
		reporter.report(getClass().getSimpleName(), "invocations = " + invocations);
   }

private static class ConstantPropagationVisitor extends Visitor {
   public ConstantPropagationVisitor() {
    //TODO
   }
   public Expression getExpression() {
    //TODO
    return new Expression();
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
