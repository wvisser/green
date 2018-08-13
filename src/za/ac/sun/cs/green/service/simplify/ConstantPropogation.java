package za.ac.sun.cs.green.service.simplify;
/* TODO: implemement
   1. propagating one equality
   2. adding simplification, which could introduce more equalities to propagate
   3. do full propagotion until you reach a fixpoint

*/
/* External */
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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

                    ConstantPropogationVisitor propogationVisitor =
                    new ConstantPropogationVisitor(var_map);
                    expression.accept(propogationVisitor);
                    //log.log(Level.FINEST, "\n__getExpression__\n " +
                    //propogationVisitor.getLogStr());
                    expression = propogationVisitor.getExpression();
                    //log.log(Level.FINEST, "\n after \n " +
                    //propogationVisitor.getLogStr());
                    log.log(Level.FINEST, "\n After Simplification\n " + expression);

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
        logstr += "\n__________\n";
        Expression r = stack.pop();
        logstr += ("right = " + r + "\n");
        Expression l = stack.pop();
        logstr += ("left = " + l + "\n");
        Expression n;

        if ((r instanceof Variable)
                && (l instanceof Variable)) {
            /* Check if the variables have been assigned */
            boolean chk = false;
            if (variables.contains(r)) {
                r = map.get(r);
                chk = !chk;
            }
            if (variables.contains(l)) {
                l = map.get(l);
                chk = !chk;
            }

            if (op.equals(Operation.Operator.EQ)) {
                if (chk) {
                    if (r instanceof Constant) {
                        map.put((Variable) l, (Constant) r);
                        l = r;
                    } else if (l instanceof Constant) {
                        map.put((Variable) r, (Constant) l);
                        r = l;
                    }
                    /* both l and r are constants now expect simplify */
                }
            }

            logstr += "\nX==Y ASSIGNMENT : ";
            n = new Operation(op, l, r);
            logstr += (n + "\n");
        } else if ((r instanceof Constant)
                && (l instanceof Variable)
                && op.equals(Operation.Operator.EQ)) {
            /* Its a variable assignment */
            logstr += "\nX==0 ASSIGNMENT : ";

            n = new Operation(op, l, r);
            logstr += (n + "\n");
        } else if ((r instanceof Constant)
                && (l instanceof Variable)) {
            /* Check if the variables have been assigned */
            if (variables.contains(l)) {
                l = map.get(l);
            }
            logstr += "\nX+1 ASSIGNMENT : ";

            n = new Operation(op, l, r);
            logstr += (n + "\n");
        } else if ((r instanceof Operation)
                && (l instanceof Constant)) {
            logstr += "\nLeft is a constant, right is an operation : ";
            //n = new Operation(op, l, r);
            //logstr += (n + "\n");
            Constant X = null;

            Iterator<Expression> ops = ((Operation) r).getOperands().iterator();
            Operation.Operator O = ((Operation) r).getOperator();

            logstr += "Operands \n";
            logstr += ("Operator : " + O.toString());

            Expression op1 = ops.next();
            Expression op2 = ops.next();

            if (op1 instanceof Constant) {
                X = (Constant) op1;
                r = op2;
            } else if (op2 instanceof Constant) {
                X = (Constant) op2;
                r = op1;
            }

            if (X != null) { /* We can simplify! */
                Operation.Operator nop = getNOP(O);
                Operation temp = new Operation(nop, l, X);

                if (nop == null) {
                    logstr += "NOP is null! \n";
                } else if (l == null) {
                    logstr += "l is null \n";
                } else if (X == null) {
                    logstr += "X is null \n";
                }
                l = temp.apply(nop, r, X);
                logstr += ("new right =  " + r + "\n");
                logstr += ("new left =  " + l + "\n");
            }

            n = new Operation(op, l, r);
            logstr += (n + "\n");
        }  else if ((r instanceof Constant)
                && (l instanceof Operation)
                && (op.equals(Operation.Operator.EQ))) {
            logstr += "\nLeft is an Operation, right is an constant : ";

            Constant X = null;

            Iterator<Expression> ops = ((Operation) l).getOperands().iterator();
            Operation.Operator O = ((Operation) l).getOperator();

            logstr += "Operands \n";
            logstr += ("Operator : " + O.toString());

            Expression op1 = ops.next();
            Expression op2 = ops.next();

            if (op1 instanceof Constant) {
                X = (Constant) op1;
                l = op2;
            } else if (op2 instanceof Constant) {
                X = (Constant) op2;
                l = op1;
            }

            if (X != null) { /* We can simplify! */
                Operation.Operator nop = getNOP(O);
                Operation temp = new Operation(nop, r, X);

                if (nop == null) {
                    logstr += "NOP is null! \n";
                } else if (r == null) {
                    logstr += "r is null \n";
                } else if (X == null) {
                    logstr += "X is null \n";
                }
                r = temp.apply(nop, r, X);
                logstr += ("new right =  " + r + "\n");
                logstr += ("new left =  " + l + "\n");
            }

            n = new Operation(op, l, r);
            logstr += (n + "\n");
        }  else if ((r instanceof Operation)
                && (l instanceof Operation)
                && (op.equals(Operation.Operator.AND))) {
            logstr += "op && op : \n";
            if (r.equals(Operation.FALSE) || l.equals(Operation.FALSE)){
                n = Operation.FALSE;
            } else if (r.equals(Operation.TRUE) || l.equals(Operation.TRUE)){
                n = Operation.TRUE;
            } else if (((Operation)l).like((Operation)r)) {
                /* x==2 && 2==x becomes 0==0*/
                n = Operation.TRUE;
            } else if (isAssignment((Operation) l) && isAssignment((Operation) r)
                        && op.equals(Operation.Operator.AND)){
                if (sameVariables((Operation)l, (Operation)r)) {
                    logstr += "Same Variables detected\n";
                    if (sameConstants((Operation)l, (Operation)r)) {
                        n = Operation.TRUE;
                        logstr += "Same Constants detected\n";
                    } else {
                        n = Operation.FALSE;
                        logstr += "different Constants detected\n";
                    }
                } else { n = new Operation(Operation.Operator.AND, l, r);}
            }else {
                n = new Operation(Operation.Operator.AND, l, r);
            }
            /*if (r.equals(Operation.FALSE) || l.equals(Operation.FALSE)){
                n = Operation.FALSE;
            } else if (r.equals(Operation.TRUE) || l.equals(Operation.TRUE)){
                n = Operation.TRUE;
            } else {
                n = new Operation(Operation.Operator.AND, l, r);
            }*/
        } else {
            logstr += "\nUNKNOWN ASSIGNMENT : ";

            n = new Operation(op, l, r);
            logstr += (n + "\n");
        }

        /* Simplify if possible */
        if ((r instanceof Constant)
                && (l instanceof Constant)) {
            /* opportunity to simplify */
            logstr += "\n1+1 SIMPLIFICATION : ";
             n = operation.apply(op, l, r);

             logstr += (l + op.toString() + r + " = " + n.toString() + "\n");
        }

        stack.push(n);
        logstr += "\n__________\n";
    }// end post visit

    private boolean isAssignment(Operation o) {
        Operation.Operator operator = o.getOperator();
        Iterator<Expression> iter = o.getOperands().iterator();
        Expression o0 = iter.next();
        Expression o1 = iter.next();

        if (!operator.equals(Operation.Operator.EQ)){
            return false;
        }

        if (o0 instanceof Variable && o1 instanceof Constant) {return true;}
        else  if (o0 instanceof Constant && o1 instanceof Variable){return true;}
        return false;
    }

    /* to compare expressions of form (x==2)&&(x==4) vs (x==2)&&(y==4) */
    private boolean sameVariables(Operation l, Operation r) {
        Iterator<Expression> loperands = l.getOperands().iterator();
        Iterator<Expression> roperands = r.getOperands().iterator();

        Expression l0 = loperands.next();
        Expression l1 = loperands.next();
        Expression r0 = roperands.next();
        Expression r1 = roperands.next();

        Variable lvar, rvar;
        Constant lcon, rcon;

        if (l0 instanceof Variable) {lvar = (Variable)l0; lcon = (Constant) l1;}
        else  {lvar = (Variable)l1; lcon = (Constant) l0;}

        if (r0 instanceof Variable) {rvar = (Variable)r0; rcon = (Constant)r1;}
        else  {rvar = (Variable)r1; rcon = (Constant)r0;}

        if (((IntVariable)lvar).equals((IntVariable)rvar)) {
            return true;
        }
        return false;
    }

    /* to compare expressions of form (x==2)&&(x==2) vs (x==2)&&(x==4) */
    private boolean sameConstants(Operation l, Operation r) {
        Iterator<Expression> loperands = l.getOperands().iterator();
        Iterator<Expression> roperands = r.getOperands().iterator();

        Expression l0 = loperands.next();
        Expression l1 = loperands.next();
        Expression r0 = roperands.next();
        Expression r1 = roperands.next();

        Variable lvar, rvar;
        Constant lcon, rcon;

        if (l0 instanceof Variable) {lvar = (Variable)l0; lcon = (Constant) l1;}
        else  {lvar = (Variable)l1; lcon = (Constant) l0;}

        if (r0 instanceof Variable) {rvar = (Variable)r0; rcon = (Constant)r1;}
        else  {rvar = (Variable)r1; rcon = (Constant)r0;}

        if (((IntVariable)lvar).equals((IntVariable)rvar)) {
            if (((IntConstant)lcon).equals((IntConstant)rcon)){
                return true;
            }
        }
        return false;
    }


    private Operation.Operator getNOP(Operation.Operator op) {
        Operation.Operator nop = null;
        switch (op) {
        case EQ:
            nop = Operation.Operator.EQ;
            break;
        case NE:
            nop = Operation.Operator.NE;
            break;
        case LT:
            nop = Operation.Operator.GT;
            break;
        case LE:
            nop = Operation.Operator.GE;
            break;
        case GT:
            nop = Operation.Operator.LT;
            break;
        case GE:
            nop = Operation.Operator.LE;
            break;
        case MUL:
            nop = Operation.Operator.DIV;
            break;
        case AND:
            nop = Operation.Operator.AND;
            break;
        case ADD:
            nop = Operation.Operator.SUB;
            break;
        case SUB:
            nop = Operation.Operator.ADD;
            break;
        case OR:
            nop = Operation.Operator.OR;
            break;
        default:
            nop = op;
            break;
        }
        return nop;
    }


}// end ConstantPropogationVisitor


/* Goal Simplify expressions
 * Must traverse an expression already visited by ConstantPropogationVisitor
 */

/*private static class SimplificationVisitor extends Visitor {
    private Stack<Expression> stack;
    private String logstr;

    public SimplificationVisitor () {
        this.stack = new Stack<Expression>();
        this.logstr = "\n";
    }
    public String getLogStr() {
        return logstr;

    }

    public Expression getExpression() {
        logstr = "\n __Stack__\n";
        Expression x = stack.pop();
        logstr += (x + "\n");
        return x;
    }

    @Override
    public void postVisit(Constant constant) {
         stack.push(constant);
    }
    @Override
    public void postVisit(Variable variable) {
         stack.push(variable);
    }
    /*
    */
/*    @Override
     public void postVisit(Operation operation) throws VisitorException {
         Operation.Operator op = operation.getOperator();
         Expression r = stack.pop();
         Expression l = stack.pop();

         // if constant +/-/* constant
         if ((r instanceof Constant) && (l instanceof Constant)) {

         }

     }
}*/
}
