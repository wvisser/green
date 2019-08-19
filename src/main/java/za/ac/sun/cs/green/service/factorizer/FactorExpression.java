package za.ac.sun.cs.green.service.factorizer;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import za.ac.sun.cs.green.expr.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author JH Taljaard (USnr 18509193)
 * @author Willem Visser (Supervisor)
 * @author Jaco Geldenhuys (Supervisor)
 *
 * Description:
 * Helper class to factorize an expression.
 */
public class FactorExpression {
	private Set<Expression> factors = null;
	private CollectionVisitor collector = null;
	private UnionFind<Expression> uf = null;
	private Expression processedExpression = null;
	protected final Logger log;
	// stat variables
	private int conjunctCount = 0;
	public long collectorTime = 0;
	public long conjunctsTime = 0;

	public FactorExpression() {
		this.log = StatusLogger.getLogger();
	}

	public FactorExpression(final Logger log) {
		this.log = log;
	}

	public FactorExpression(Expression e) {
		this.log = StatusLogger.getLogger();
		factorize(e);
	}

	/**
	 * Factorize an expression. This is accomplished by traversing the expression,
	 * and grouping the propositions found based on the variables they contain.
	 *
	 * @param expression expression to factorize
	 * @return set of factors
	 */
	public Set<Expression> factorize(Expression expression) {
		CollectionVisitor collector = new CollectionVisitor();
		long startTime = System.currentTimeMillis();
		try {
			expression.accept(collector);
			collectorTime += (System.currentTimeMillis() - startTime);
		} catch (VisitorException x) {
			log.fatal("encountered an exception -- this should not be happening!", x);
		}
		startTime = System.currentTimeMillis();
		final Map<Expression, Expression> factors = new HashMap<>();
		UnionFind<Expression> disjointSet = collector.getDisjointSet();
		for (Expression proposition : disjointSet.getElements()) {
			Expression root = disjointSet.find(proposition);
			factors.merge(root, proposition, (e1, e2) -> new Operation(Operation.Operator.AND, e1, e2));
		}
		conjunctCount += disjointSet.getConjunctCount();
		conjunctsTime += (System.currentTimeMillis() - startTime);
		return new HashSet<Expression>(factors.values());
	}

	public Set<Expression> getFactors() {
		return factors;
	}

	public int getVariableCount() {
		return collector.getVarCount();
	}

	public int getConjunctCount() {
		return conjunctCount;
	}

	/*
	 ##################################################################
	 ############### VISITOR TO COLLECT PROPOSITIONS ##################
	 ##################################################################
	*/

	/**
	 * Visitor that traverses an expression, picks up propositions, and groups.
	 */
	private static class CollectionVisitor extends Visitor {

		/**
		 * Disjoint-set of propositions.
		 */
		private final UnionFind<Expression> disjointSet = new UnionFind<>();

		/**
		 * Map each variable to the representative proposition for the factor in which
		 * the variable appears.
		 */
		private final Map<Variable, Expression> rootMap = new HashMap<>();

		/**
		 * The current proposition being explored. This is set when, as be traverse down
		 * the expression tree, we encounter -- for the first time -- an operator that
		 * is not "and".
		 */
		private Expression currentProposition = null;

		/**
		 * Current depth of the traversal. This is needed to handle nested "and"
		 * operations and also "and" operations nested inside "or" operations.
		 *
		 * For example, given
		 *
		 * <pre>
		 * ((x == 0) && (y == 1)) && ((z == 0) && (q + 1 == 2))
		 * </pre>
		 *
		 * the whole expression is depth 0, and so is the
		 * <code>((x==0) && (y==1))</code> and <code>((z==2) && (q+1==3))</code>
		 * subexpressions. The expressions <code>(x==0)</code>, <code>(y==1)</code>,
		 * <code>(z==0)</code>, and <code>(q+1==2)</code> are depth 1, the expressions
		 * <code>x</code>, <code>y</code>, <code>z</code>, <code>q+1</code>, and the
		 * right-hand sides are depth 2. Lastly, <code>q</code> and <code>1</code> in
		 * the last equality are depth 3.
		 *
		 * On the other hand, given
		 *
		 * <pre>
		 * ((x == 0) && (y == 1)) || ((z == 0) && (q + 1 == 2))
		 * </pre>
		 *
		 * the whole expression is depth 1, and <code>((x==0) && (y==1))</code> and
		 * <code>((z==2) && (q+1==3))</code> are both depth 2. Subexpressions have
		 * greater depths, as before.
		 */
		private int depth = 0;
		private int varCount = 0;

		/**
		 * Return the disjoint-set computed by the visitor.
		 *
		 * @return computed disjoint-set
		 */
		public UnionFind<Expression> getDisjointSet() {
			return disjointSet;
		}

		/**
		 * Check if the given variable occurs in a disjoint-set. If so, merge that set
		 * with the set of the current proposition and update the {@link #rootMap} for
		 * the variable. Otherwise, update {@link #rootMap} to "place" the variable in
		 * the disjoint-set to which the current proposition belongs.
		 *
		 * @param variable variable to handle
		 * @see za.ac.sun.cs.green.expr.Visitor#postVisit(za.ac.sun.cs.green.expr.Variable)
		 */
		@Override
		public void postVisit(Variable variable) {
			Expression proposition = rootMap.get(variable);
			if (proposition == null) {
				varCount++;
				rootMap.put(variable, disjointSet.find(currentProposition));
			} else {
				Expression newRoot = disjointSet.union(proposition, currentProposition);
				if (newRoot != proposition) {
					rootMap.put(variable, newRoot);
				}
			}
		}

		/**
		 * Increment the depth if we are not dealing with an "and" operation or if we
		 * have already passed depth 1. If the resulting depth is 1, the current
		 * operation becomes the current proposition and it is added to the disjoint-set
		 * (since this is the first time we encounter it).
		 *
		 * @param operation operation to handle
		 * @see za.ac.sun.cs.green.expr.Visitor#preVisit(za.ac.sun.cs.green.expr.Operation)
		 */
		@Override
		public void preVisit(Operation operation) {
			if ((operation.getOperator() != Operation.Operator.AND) || (depth > 0)) {
				depth++;
			}
			if (depth == 1) {
				currentProposition = operation;
				disjointSet.addElement(operation);
			}
		}

		/**
		 * Re-adjust the depth.
		 *
		 * @param operation operation to handle
		 * @see za.ac.sun.cs.green.expr.Visitor#postVisit(za.ac.sun.cs.green.expr.Operation)
		 */
		@Override
		public void postVisit(Operation operation) {
			if (depth > 0) {
				depth--;
			}
		}

		public int getVarCount() {
			return varCount;
		}
	}
	/*##################################################################*/


	/*
	 ##################################################################
	 ################# Union-Find Implementation ######################
	 ##################################################################
	*/

	/*
	 * (C) Copyright 2010-2018, by Tom Conerly and Contributors.
	 *
	 * JGraphT : a free Java graph-theory library
	 *
	 * This program and the accompanying materials are dual-licensed under
	 * either
	 *
	 * (a) the terms of the GNU Lesser General Public License version 2.1
	 * as published by the Free Software Foundation, or (at your option) any
	 * later version.
	 *
	 * or (per the licensee's choosing)
	 *
	 * (b) the terms of the Eclipse Public License v1.0 as published by
	 * the Eclipse Foundation.
	 */

	/**
	 * An implementation of <a href="http://en.wikipedia.org/wiki/Disjoint-set_data_structure">Union
	 * Find</a> data structure. Union Find is a disjoint-set data structure. It supports two operations:
	 * finding the set a specific element is in, and merging two sets. The implementation uses union by
	 * rank and path compression to achieve an amortized cost of $O(\alpha(n))$ per operation where
	 * $\alpha$ is the inverse Ackermann function. UnionFind uses the hashCode and equals method of the
	 * elements it operates on.
	 *
	 * @param <T> element type
	 * @author Tom Conerly
	 * @since Feb 10, 2010
	 */
	public static class UnionFind<T> {
		private final Map<T, T> parentMap = new LinkedHashMap<>(); // map each element to parent of set
		private final Map<T, Integer> rankMap = new LinkedHashMap<>(); // map each element to its rank
		private int count; // number of components/conjuncts
		private long unionTime = 0;
		private long findTime = 0;

		/**
		 * Adds a new element to the data structure in its own set.
		 *
		 * @param element The element to add.
		 */
		public void addElement(T element) {
//			if (parentMap.containsKey(element))
//				throw new IllegalArgumentException(
//						"element is already contained in UnionFind: " + element);
//			parentMap.put(element, element);
//			rankMap.put(element, 0);
//			count++;
			if (!parentMap.containsKey(element)) {
				parentMap.put(element, element);
				rankMap.put(element, 0);
				count++;
			}
		}

		/**
		 * @return map from element to parent element
		 */
		protected Map<T, T> getParentMap() {
			return parentMap;
		}

		public Set<T> getElements() {
			return getParentMap().keySet();
		}

		/**
		 * @return map from element to rank
		 */
		protected Map<T, Integer> getRankMap() {
			return rankMap;
		}

		/**
		 * Find the root (representative) element of a given element. This
		 * implementation uses Tarjan and Van Leeuwen's "path splitting" mechanism.
		 *
		 * @param element the element to search for
		 * @return representative element
		 */
		public T find(T element) {
//			if (!parentMap.containsKey(element)) {
//				throw new IllegalArgumentException(
//						"element is not contained in this UnionFind data structure: " + element);
//			}
//
//			T current = element;
//			while (true) {
//				T parent = parentMap.get(current);
//				if (parent.equals(current)) {
//					break;
//				}
//				current = parent;
//			}
//			final T root = current;
//
//			current = element;
//			while (!current.equals(root)) {
//				T parent = parentMap.get(current);
//				parentMap.put(current, root);
//				current = parent;
//			}
//
//			return root;
			T parent = parentMap.get(element);
			if (parent == null) {
				throw new IllegalArgumentException("element is not contained in this data structure: " + element);
			}
			long startTime = System.currentTimeMillis();
			while (!parent.equals(element)) {
				T prev = element;
				element = parent;
				parent = parentMap.get(element);
				parentMap.put(prev, parent);
			}
			findTime += (System.currentTimeMillis() - startTime);
			return element;
		}

		/**
		 * Merge the disjoint sets in which the parameters appear and return the
		 * representative for the new, merged set. This implementation uses
		 * union-by-rank.
		 *
		 * @param element1 first element
		 * @param element2 second element
		 * @return representative of the newly-merged set that contains the parameters
		 */
		public T union(T element1, T element2) {
			if (!parentMap.containsKey(element1) || !parentMap.containsKey(element2)) {
				throw new IllegalArgumentException("elements must be contained in given set");
			}
			long startTime = System.currentTimeMillis();
			T parent1 = find(element1);
			T parent2 = find(element2);
			if (parent1.equals(parent2)) {
				unionTime += (System.currentTimeMillis() - startTime);
				return parent1;
			}
			int rank1 = rankMap.get(parent1);
			int rank2 = rankMap.get(parent2);
			T parent;
			if (rank1 < rank2) {
				parentMap.put(parent1, parent2);
				parent = parent2;
			} else if (rank1 > rank2) {
				parentMap.put(parent2, parent1);
				parent = parent1;
			} else {
				parentMap.put(parent2, parent1);
				rankMap.put(parent1, rank1 + 1);
				parent = parent1;
			}
//			count--; // one less component
			unionTime += (System.currentTimeMillis() - startTime);
			return parent;
		}

		/**
		 * @return the number of conjuncts
		 */
		public int getConjunctCount() {
			return count;
		}

		/**
		 * Returns the total number of elements in this data structure.
		 *
		 * @return the total number of elements in this data structure.
		 */
		public int size() {
			return parentMap.size();
		}

		/**
		 * Resets the UnionFind data structure: each element is placed in its own singleton set.
		 */
		public void reset() {
			for (T element : parentMap.keySet()) {
				parentMap.put(element, element);
				rankMap.put(element, 0);
			}
			count = parentMap.size();
		}

		/**
		 * Returns a string representation of this data structure. Each component is represented as
		 * $\left{v_i:v_1,v_2,v_3,...v_n\right}$, where $v_i$ is the representative of the set.
		 *
		 * @return string representation of this data structure
		 */
		public String toString() {
			Map<T, Set<T>> setRep = new LinkedHashMap<>();
			for (T t : parentMap.keySet()) {
				T representative = find(t);
				if (!setRep.containsKey(representative))
					setRep.put(representative, new LinkedHashSet<>());
				setRep.get(representative).add(t);
			}

			return setRep
					.keySet().stream()
					.map(
							key -> "{" + key + ":" + setRep.get(key).stream().map(Objects::toString).collect(
									Collectors.joining(",")) + "}")
					.collect(Collectors.joining(", ", "{", "}"));
		}

		public long getUnionTime() {
			return unionTime;
		}

		public long getFindTime() {
			return findTime;
		}
	}
	/*##################################################################*/
}
