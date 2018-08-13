package za.ac.sun.cs.green.service.simplify;
/* TODO: implemement
   1. propagating one equality
   2. adding simplification, which could introduce more equalities to propagate
   3. do full propagotion until you reach a fixpoint

*/
/* External */
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;

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
public Expression propogateConstants(Expression expression, Map<Variable, Variable> map) {
               //Use the visitor to propogate constants
               try {
                    log.log(Level.FINEST, "Before ConstantPropogation: " + expression);
       			    invocations++;

                    HashMap <Variable, Constant> var_map;

                    MapVisitor mapVisitor = new MapVisitor();
        			expression.accept(mapVisitor);
        			var_map = mapVisitor.getMap();
                    //log.log(Level.FINEST, "\n__getMap__\n " +
                    //mapVisitor.getLogStr());

                    ConstantPropogationVisitor propogationVisitor =
                    new ConstantPropogationVisitor(var_map);
                    expression.accept(propogationVisitor);
                    log.log(Level.FINEST, "\n__getExpression__\n " +
                    propogationVisitor.getLogStr());
                    expression = propogationVisitor.getExpression();
                    log.log(Level.FINEST, "\n__getExpression__\n " +
                    propogationVisitor.getLogStr());

                    log.log(Level.FINEST, "After ConstantPropogation: " + expression);
        			return expression;
               } catch (VisitorException x) {
                   log.log(Level.SEVERE,
       					"encountered an exception -- this should not be happening!",
       					x);
               }
               return null;
           }

/*
 * Goal is to obtain a HashMap of variables and constant values assigned in the
 * expression
 */
private static class MapVisitor extends Visitor {
   private Stack<Expression> stack;
   private String logstr;
   private HashMap <Variable, Constant> map;

   public MapVisitor() {
	   stack = new Stack<Expression>();
       map = new HashMap <Variable, Constant>();
       logstr = "\n";
   }

   public HashMap getMap() {
       logstr = "\n __Stack__\n";
       while (!stack.isEmpty()) {
           Expression x = stack.pop();

           if (x instanceof Variable) {
               //int c = ((IntConstant) map.get(var)).getValue();
               logstr += "VARIABLE : ";
               logstr += (x);
               logstr += "\n";
           } else if (x instanceof Operation) {
               Operation.Operator op = ((Operation) x).getOperator();

               if (op.equals( Operation.Operator.EQ)) {

                   Expression e1 = stack.pop();
                   Expression e2 = stack.pop();

                   if (e1 instanceof Constant && e2 instanceof Variable) {
                       map.put((Variable) e2, (Constant) e1);
                       logstr += (e2 + "==" + e1 + "\n");
                   } else if (e1 instanceof Variable && e2 instanceof Constant) {
                       map.put((Variable) e1, (Constant) e2);
                       logstr += (e1 + "==" + e2 + "\n");
                   } else {
                       logstr += "OPERATION : ";
                       logstr += op;
                       logstr += "\n";

                       stack.push(e2);
                       stack.push(e1);
                   }
               } else {
                   logstr += "OPERATION : ";
                   logstr += op;
                   logstr += "\n";
               }

           } else if (x instanceof Constant) {
               logstr += "Constant : ";
               logstr += x;
               logstr += "\n";
           } else {
               logstr += "Unknown : ";
               logstr += x;
               logstr += "\n";
           }

       }

       return map;
   }
   public String getLogStr() {
       return logstr;
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
	public void postVisit(Operation operation) throws VisitorException {
        stack.push(operation);
   }
}

/*
 * Goal : return an expression with assigned values changed
 */
private static class ConstantPropogationVisitor extends Visitor {
   private Stack<Expression> stack;
   private String logstr;
   private HashMap <Variable, Constant> map;
   private int SIZE;
   private Set variables;

   public ConstantPropogationVisitor(HashMap <Variable, Constant> map) {
	   this.stack = new Stack<Expression>();
       this.map = map;
       this.SIZE = 0;
       this.logstr = "\n";
       this.variables = this.map.keySet();
   }

   public Expression getExpression() {
       logstr = "\n __Stack__\n";

       int count = SIZE;
       Expression x = stack.pop();
       logstr += (x + "\n");
       return x;
   }

   public String getLogStr() {
       return logstr;
   }

   @Override
   public void postVisit(Constant constant) {
        stack.push(constant);
        SIZE++;
   }
   @Override
   public void postVisit(Variable variable) {
        stack.push(variable);
        SIZE++;
   }
   /*
   */
   @Override
	public void postVisit(Operation operation) throws VisitorException {
        Operation.Operator op = operation.getOperator();
        /*if (nop != null) {*/
        logstr += "\n__________\n";
            Expression r = stack.pop();
            logstr += ("right = " + r + "\n");
            Expression l = stack.pop();
            logstr += ("left = " + l + "\n");

            if ((r instanceof Variable)
                    && (l instanceof Variable)) {

                /* Check if the variables have been assigned */
                if (variables.contains(r)) {
                    r = map.get(r);
                }
                if (variables.contains(l)) {
                    l = map.get(l);
                }

                logstr += "X==Y ASSIGNMENT : ";
                logstr += (l + op.toString() + r + "\n");
                Expression n  = new Operation(op, l, r);
                logstr += ("n = " + n + "\n");

                stack.push(new Operation(op, l, r));

            } else if ((r instanceof Constant)
                    && (l instanceof Variable)
                    && op.equals(Operation.Operator.EQ)) {

                /* Its a variable assignment */
                logstr += "X==0 ASSIGNMENT : ";
                logstr += (l + op.toString() + r + "\n");

                stack.push(new Operation(op, l, r));

            } else if ((r instanceof Constant)
                    && (l instanceof Variable)) {
                /* Check if the variables have been assigned */
                if (variables.contains(l)) {
                    l = map.get(l);
                }
                logstr += "X+1 ASSIGNMENT : ";
                logstr += (l + op.toString() + r + "\n");

                Expression n  = new Operation(op, l, r);
                logstr += ("n = " + n + "\n");
                stack.push(n);

            } else {
                logstr += "UNKNOWN ASSIGNMENT : ";
                logstr += (new Operation(op, l, r) + "\n");

                stack.push(new Operation(op, l, r));
            }
            logstr += "\n__________\n";
        /*} else if (op.getArity() == 2) {
            Expression r = stack.pop();
            Expression l = stack.pop();
            logstr += "Arity ASSIGNMENT : ";
            logstr += (l + op.toString() + r + "\n");
            stack.push(new Operation(op, l, r));
        } else {
            for (int i = op.getArity(); i > 0; i--) {
                stack.pop();
            }
            logstr += "Another Arity ASSIGNMENT : ";
            logstr += (operation + "\n");
            stack.push(operation);
        }*/
    }

}

}
