package za.ac.sun.cs.green.service.grulia.gruliastore;

import za.ac.sun.cs.green.Green;

import java.io.Serializable;
import java.util.*;

/**
 * @author JH Taljaard (USnr 18509193)
 * @author Willem Visser (Supervisor)
 * @author Jaco Geldenhuys (Supervisor)
 * <p>
 * Description:
 * Cache implementing ArrayList with PQ building.
 */
public class LinearListStore<E extends Entry> implements GruliaStore<E>, Serializable {
	/**
	 * Contains the entries in the repo.
	 */
	private ArrayList<E> entries;
	private HashMap<Double, String> hashcache;
	private Green solver;
	private boolean default_zero;
	private final String KEY = "ENTRY:";
	protected long flushTime = 0;
	protected long storeTime = 0;
	protected long putTime = 0;
	protected long getTime = 0;
	protected long putCount = 0;
	protected long getCount = 0;

	public LinearListStore(Green solver, boolean default_zero) {
		this.entries = new ArrayList<>();
		this.hashcache = new HashMap<>();
		this.default_zero = default_zero;
		this.solver = solver;

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
	 * To check if model is already in cache.
	 * @param entry entry containing model to check for
	 * @return if model is in the repo
	 */
	public boolean contains(E entry) {
		if (this.hashcache.containsKey(entry.getSatDelta())) {
			return false;
		} else if (this.hashcache.get(entry.getSatDelta()) == null) {
			return false;
		}
		if (entry instanceof SatEntry) {
			return this.hashcache.get(entry.getSatDelta()).equals(((SatEntry) entry).getSolution().toString());
		} else if (entry instanceof UnsatEntry){
			return this.hashcache.get(entry.getSatDelta()).equals(((UnsatEntry) entry).getSolution().toString());
		} else {
			// This should not be happening
			return false;
		}
	}

	/**
	 * @return all entries
	 */
	public List<E> getEntries() {
		return Collections.unmodifiableList(entries);
	}

	/**
	 * @return the number of entries in the repo.
	 */
	public int size() {
		return this.entries.size();
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
		} else {
			return linearSearch(anchor, k);
		}
	}

	/**
	 * Returns k entries closest to the given SATDelta.
	 * @param anchor the entry serving as the starting point for the search
	 * @param k the number of entries to extract
	 * @return the extracted entries, sorted by increasing distance from the given SATDelta.
	 */
	public List<E> extract(E anchor, int k) {
		List<E> entries;
		if (k <= 0) {
			entries = new ArrayList<>();
		} else {
			entries = this.filterByProximity(anchor, k);
		}
		return Collections.unmodifiableList(entries);
	}

	@Override
	public void clear() {
		entries.clear();
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
	 * Search through repo to get k number of entries closest to the target sd.
	 * <p>
	 * @param anchor the entry serving as the starting point for the search
	 * @param k the limit number of entries to obtain
	 * @return array of closest entries
	 */
	private List<E> linearSearch(E anchor, int k) {
		PriorityQueue<Pair<Double, Entry>> queue = new PriorityQueue<>(
				k,
				(p1, p2) -> ((-1) * (p1.getL().compareTo(p2.getL()))));

		// Load the first k entries in the queue, then keep updating the queue
		// inserting elements whose distance from satDelta is smaller than the
		// distance of the maximum element in the queue. The maximum is removed
		// whenever a new elements is inserted so that the overall complexity
		// of this method is O(n*log(k)).
		int i = 0;
		for (final Entry entry : this.entries) {
			if (!default_zero) {
				if (entry.isValidFor(anchor)) {
					// Entries containing models with less variables than
					// the reference expression are immediately discarded.
					continue;
				}
			}
			Double delta = entry.distanceTo(anchor);

			if (i++ < k) {
				queue.add(new Pair<>(delta, entry));
			} else {
				Pair<Double, Entry> head = queue.peek();
				if (delta.compareTo(head.getL()) < 0) {
					queue.poll();
					queue.add(new Pair<>(delta, entry));
				}
			}
		}

		List<E> closests = new ArrayList<>(k);
		while (!queue.isEmpty()) {
			closests.add((E) queue.remove().getR());
		}

		return closests;
	}
}
