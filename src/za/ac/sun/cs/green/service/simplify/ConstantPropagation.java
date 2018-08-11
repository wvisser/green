package za.ac.sun.cs.green.service.simplify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;

import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.expr.*;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.util.Reporter;

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
            final Expression e = simplify(instance.getFullExpression());
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

    /**
     * This method is the main driver of constant propagation.
     *
     * @param expression The expression to be simplified.
     * @return The simplified expression.
     */
    public Expression simplify(Expression expression) {
        /* Log a sanity check and increment the invocation counter. */
        log.log(Level.FINEST, "Before Propagation: " + expression);
        invocations++;

        /* We have two visitors, the first being ScoutVisitor that finds constant
         * assertions (e.g. x == 1) and makes a note of these in a map called
         * replacements.
         */
        ScoutVisitor scoutVisitor = new ScoutVisitor();

        try {
            expression.accept(scoutVisitor);
        } catch (VisitorException visitorException) {
            visitorException.printStackTrace();
        }

        Map<Variable, Constant> replacements =
                scoutVisitor.getReplacements();

        log.log(Level.FINEST, "Replacements: " + replacements.toString());

        /* Now that we have the replacements we can have our second visitor,
         * ConstantPropagation visitor, to apply the replacements and return the
         * new expression.
         */
        ConstantPropagationVisitor constantPropagationVisitor =
                new ConstantPropagationVisitor(replacements);

        try {
            expression.accept(constantPropagationVisitor);
        } catch (VisitorException visitorException) {
            visitorException.printStackTrace();
        }

        /* Grab the propagated expression. */
        Expression propagatedExpression = constantPropagationVisitor.getExpression();

        log.log(Level.FINEST, "After Propagation: " + propagatedExpression);

        /* A third visitor is now run over the expression to take care of
         * simplifications.
         */
        SimplificationVisitor simplificationVisitor =
                new SimplificationVisitor();

        try {
            propagatedExpression.accept(simplificationVisitor);
        } catch (VisitorException visitorException) {
            visitorException.printStackTrace();
        }

        /* Now that we have our new expression, we can return this to processRequest */
        Expression simplifiedExpression = simplificationVisitor.getExpression();

        log.log(Level.FINEST, "After Simplification: " + simplifiedExpression);

        return simplifiedExpression;

    }

    /**
     * The visitor responsible for simplifying mathematical expressions
     */
    private static class SimplificationVisitor extends Visitor {
        private Stack<Expression> expressionStack;

        public SimplificationVisitor() {
            this.expressionStack = new Stack<>();
        }

        /**
         * Constants just get pushed onto the stack.
         *
         * @param constant
         */
        @Override
        public void postVisit(Constant constant) {
            expressionStack.push(constant);
        }

        /**
         * Variables just get pushed onto the stack.
         *
         * @param variable
         */
        @Override
        public void postVisit(Variable variable) {
            expressionStack.push(variable);
        }

        /**
         * This method is responsible for rewriting triplets of Constant + Op + Constant
         * into their evaluated, "final" form.
         *
         * @param operation
         */
        @Override
        public void postVisit(Operation operation) {

            /* Simple test case, binary add with both operands being constants. */
            if (operation.getOperator() == Operation.Operator.ADD) {
                Expression LHS = operation.getOperand(0);
                Expression RHS = operation.getOperand(1);

                /* If both are integer constants, we can add them. */
                if (LHS instanceof IntConstant && RHS instanceof IntConstant) {
                    int LHSValue = ((IntConstant) LHS).getValue();
                    int RHSValue = ((IntConstant) RHS).getValue();

                    int result = LHSValue + RHSValue;

                    Expression resultExpr = new IntConstant(result);

                    expressionStack.push(resultExpr);

                }

            } else {
                /* The catch-all case. */
                int arity = operation.getOperator().getArity();

                Expression operands[] = new Expression[arity];
                for (int i = arity; i > 0; i--) {
                    operands[i - 1] = expressionStack.pop();
                }
                expressionStack.push(new Operation(operation.getOperator(), operands));

            }
        }

        /**
         * @return The expression resulting from the propagation.
         */
        public Expression getExpression() {
            return expressionStack.pop();
        }
    }

    /**
     * The visitor responsible for applying the replacements generated by the
     * {@link ScoutVisitor}.
     */
    private static class ConstantPropagationVisitor extends Visitor {
        private Stack<Expression> expressionStack;
        private Map<Variable, Constant> replacements;

        /**
         * @param replacements The map of replacements.
         */
        public ConstantPropagationVisitor(Map<Variable, Constant> replacements) {
            this.expressionStack = new Stack<>();
            this.replacements = replacements;
        }

        /**
         * This is the meat of the {@link ConstantPropagationVisitor}. We visit a
         * variable, see if it features somewhere in the map of replacements, and if it
         * does we do mutation.
         *
         * @param variable The variable we are visiting.
         */
        @Override
        public void postVisit(Variable variable) {
            /* See if a suitable replacement exists. */
            Constant constant = replacements.get(variable);
            if (constant == null) {
                /* No suitable replacement, just push it back down. */
                expressionStack.push(variable);
            } else {
                /* Magic line! This is where the replacement happens. */
                expressionStack.push(constant);
            }
        }

        /**
         * Constants just get pushed onto the stack.
         *
         * @param constant
         */
        @Override
        public void postVisit(Constant constant) {
            expressionStack.push(constant);
        }

        /**
         * Some leg work surrounding operations has been "borrowed" from the
         * {@link za.ac.sun.cs.green.service.renamer.RenamerService.Renamer}.
         *
         * @param operation
         */
        @Override
        public void postVisit(Operation operation) {
            int arity = operation.getOperator().getArity();
            Expression operands[] = new Expression[arity];
            for (int i = arity; i > 0; i--) {
                operands[i - 1] = expressionStack.pop();
            }
            expressionStack.push(new Operation(operation.getOperator(), operands));
        }

        /**
         * @return The expression resulting from the propagation.
         */
        public Expression getExpression() {
            return expressionStack.pop();
        }

    }

    /**
     * The visitor responsible for finding possible propagations on an expression.
     */
    private static class ScoutVisitor extends Visitor {
        private Map<Variable, Constant> replacements;

        public ScoutVisitor() {
            replacements = new HashMap<>();
        }

        /**
         * @return A map of suitable assertions from variables to constants.
         */
        public Map<Variable, Constant> getReplacements() {
            return this.replacements;
        }

        /**
         * We are particularly interested in operations, as assertions are operations,
         * and assertions between variables and constants is the core idea behind
         * constant propagation.
         *
         * @param operation The operation we are visiting.
         * @throws VisitorException
         */
        @Override
        public void postVisit(Operation operation) {
            /* Grab the operator, and just continue if it is not an assertion. */
            Operation.Operator op = operation.getOperator();

            if (op != Operation.Operator.EQ) {
                return;
            }

            /* Grab the left and right hand sides of the assertion. */
            Expression LHS = operation.getOperand(0);
            Expression RHS = operation.getOperand(1);

            /* We are only interested in the case of a single variable being asserted
             * against a single constant. If we find a match, put it into the
             * replacements map.
             */
            if (LHS instanceof Constant && RHS instanceof Variable) {
                replacements.put((Variable) RHS, (Constant) LHS);
            } else if (RHS instanceof Constant && LHS instanceof Variable) {
                replacements.put((Variable) LHS, (Constant) RHS);
            }
        }
    }
}
