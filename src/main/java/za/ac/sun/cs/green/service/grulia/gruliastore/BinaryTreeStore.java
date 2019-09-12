package za.ac.sun.cs.green.service.grulia.gruliastore;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.util.Reporter;

import java.io.Serializable;
import java.util.*;

/**
 * @author JH Taljaard (USnr 18509193)
 * @author Willem Visser (Supervisor)
 * @author Jaco Geldenhuys (Supervisor)
 * <p>
 * Description:
 * Repository based on a RB-Tree.
 * @param <E> entry type to store in the repository
 */
public class BinaryTreeStore<E extends Entry> implements GruliaStore<E>, Comparator<E>, Serializable {

	/**
	 * Contains the entries in the repo.
	 */
	private TreeSet<E> entries;
	private Green solver;
	protected boolean default_zero;
	private final String KEY = "ENTRY:";
	protected long flushTime = 0;
	protected long storeTime = 0;
	protected long putTime = 0;
	protected long getTime = 0;
	protected long putCount = 0;
	protected long getCount = 0;


	public BinaryTreeStore(Green solver, boolean default_zero) {
		this.entries = new TreeSet<>();
		this.default_zero = default_zero;
		this.solver = solver;
	}

	@Override
	public int compare(E e1, E e2) {
		return Double.compare(e1.getSatDelta(), e2.getSatDelta());
	}

	/**
	 * To add an entry to the repo.
	 *
	 * @param entry the entry to be added.
	 */
	public void add(E entry) {
		long startTime = System.currentTimeMillis();
		if (this.entries.add(entry)) {
			putCount++;
		}
		putTime += (System.currentTimeMillis() - startTime);
		storeTime += (System.currentTimeMillis() - startTime);
	}

	/**
	 * @return all entries
	 */
	public List<E> getEntries() {
		return Collections.unmodifiableList(new ArrayList<>(entries));
	}

	/**
	 * @return the number of entries in the repo.
	 */
	public int size() {
		return this.entries.size();
	}

	/**
	 * Get the next entry in the list, filtered by the numOfVars.
	 * @param anchor the entry serving as the starting point for the search
	 * @param list List of entries
	 * @return the next Entry or null
	 */
	protected E next(Iterator<E> list, E anchor) {
		E tmp;
		while (list.hasNext()) {
			tmp = list.next();
			if (default_zero) {
				return tmp;
			} else {
				if (tmp instanceof SatEntry) {
					if (tmp.isValidFor(anchor)) {
						return tmp;
					}
				} else if (tmp instanceof UnsatEntry) {
					return tmp;
				}
			}
		}
		return null;
	}

	/**
	 * Creates a copy of all the entries in sorted order, with the filter of num vars
	 * <p>
	 * @param anchor the entry serving as the starting point for the search
	 * @return copy of all entries
	 */
	private List<E> allEntries(E anchor) {
		int n = this.size();
		List<E> entriesCopy = new ArrayList<>();
		Iterator<E> entries = this.entries.iterator();
		E temp = null;
		for (int i = 0; i < n; i++) {
			temp = next(entries, anchor);
			if (temp != null) {
				entriesCopy.add(temp);
			} else {
				break;
			}
		}
		return entriesCopy;
	}

	/**
	 * Search through repo to get k number of entries closest to the target sd.
	 * <p>
	 * @param anchor the entry serving as the starting point for the search
	 * @param k the limit number of entries to obtain
	 * @return array of closest entries
	 */
	private List<E> binarySearch(E anchor, int k) {
		NavigableSet<E> head = entries.headSet(anchor, true);
		NavigableSet<E> tail = entries.tailSet(anchor, false);

		Iterator<E> upper = tail.iterator();
		Iterator<E> lower = head.descendingSet().iterator();

		E l = next(lower, anchor);
		E u = next(upper, anchor);

		double deltaU, deltaL;
		List<E> closests = new ArrayList<>(k);
		// Do not have to check if size is less than k, because it is already
		// done before this method is called.
		for (int i = 0; i < k; i++) {
			// This strategy searches one up and one down,
			// from target and so on, taking the one with the smallest difference first,
			// until k entries are chosen.
			if (u != null) {
				deltaU = u.distanceTo(anchor);
			} else {
				deltaU = Double.MAX_VALUE;
			}

			if (l != null) {
				deltaL = l.distanceTo(anchor);
			} else {
				deltaL = Double.MAX_VALUE;
			}

			if (deltaL < deltaU) {
				closests.add(l);
				l = next(lower, anchor);
			} else {
				closests.add(u);
				u = next(upper, anchor);
			}
		}

		return closests;
	}

	/**
	 * Returns k entries closest to the given SATDelta.
	 * <p>
	 * @param anchor the entry serving as the starting point for the search
	 * @param k the number of entries to extract
	 * @return the filtered entries, sorted by increasing distance from the given SATDelta.
	 */
	private List<E> filterByProximity(E anchor, int k) {
		if (this.size() <= k) {
			// If the number of requested entries exceeds the available entries,
			// return them all in the right order.
			return allEntries(anchor);
		}

		return binarySearch(anchor, k);
	}

	/**
	 * Returns k entries closest to the given SATDelta.
	 * @param anchor the entry serving as the starting point for the search
	 * @param k the number of entries to extract
	 * @return the extracted entries, sorted by increasing distance from the given SATDelta.
	 */
	public List<E> extract(E anchor, int k) {
		long startTime = System.currentTimeMillis();
		List<E> entries;
		if (k <= 0) {
			entries = new ArrayList<>();
		} else {
			entries = this.filterByProximity(anchor, k);
		}
		getCount++;
		getTime += (System.currentTimeMillis() - startTime);
		storeTime += (System.currentTimeMillis() - startTime);
		return Collections.unmodifiableList(entries);
	}

	@Override
	public String getKey() {
		return KEY;
	}

	/**
	 * Flush all of the repo entries to the Green store
	 * (ideally when the store is Redis or some persistent storage.)
	 */
	@Override
	public void flushAll() {
		long startTime = System.currentTimeMillis();
		for (E e : getEntries()) {
			solver.getStore().put(getKey() + e.hashCode(), e);
		}
		flushTime += (System.currentTimeMillis() - startTime);
		storeTime += (System.currentTimeMillis() - startTime);
	}

	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("{");
		for (E e : entries) {
			s.append(e.toString()).append(", ");
		}
		s.append("}");
		return s.toString();
	}

	@Override
	public void clear() {
		entries.clear();
	}

	public void report(Reporter reporter) {
		reporter.report(getClass().getSimpleName(), "getCount = " + getCount);
		reporter.report(getClass().getSimpleName(), "putCount = " + putCount);
		reporter.report(getClass().getSimpleName(), "storeTime = " + storeTime);
		reporter.report(getClass().getSimpleName(), "getTime = " + getTime);
		reporter.report(getClass().getSimpleName(), "putTime = " + putTime);
		reporter.report(getClass().getSimpleName(), "flushTime = " + flushTime);
	}
}
