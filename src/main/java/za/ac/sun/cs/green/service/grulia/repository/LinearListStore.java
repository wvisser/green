//package za.ac.sun.cs.green.service.grulia;
//
//import za.ac.sun.cs.green.expr.IntVariable;
//
//import java.util.ArrayList;
//import java.util.Comparator;
//import java.util.HashMap;
//import java.util.SortedSet;
//
///**
// * @date: 2018/06/20
// * @author: JH Taljaard.
// * Student Number: 18509193.
// * Supervisor:  Willem Visser   (2018),
// *              Jaco Geldenhuys (2017)
// *
// * Description:
// * Parent for cache implementing ArrayList with PQ building.
// */
//public abstract class LinearListStore<E extends Entry> implements Repository {
//    /**
//     * Contains the entries in the repo.
//     */
//    private ArrayList<E> entries;
//    private HashMap<Double, String> hashcache;
//    private boolean default_zero;
//
//    public LinearListStore(boolean default_zero) {
//        this.entries = new ArrayList<E>();
//        this.hashcache = new HashMap<>();
//        this.default_zero = default_zero;
//    }
//
//    public abstract void add(E entry);
//
//    /**
//     * To check if model is already in cache.
//     * @param entry entry containing model to check for
//     * @return if model is in the repo
//     */
//    public boolean contains(E entry) {
//        if (this.hashcache.containsKey(entry.getSatDelta())) {
//            return false;
//        } else if (this.hashcache.get(entry.getSatDelta()) == null) {
//            return false;
//        }
//        if (entry instanceof SatEntry) {
//			return this.hashcache.get(entry.getSatDelta()).equals(((SatEntry) entry).getSolution().toString());
//		} else if (entry instanceof UnsatEntry){
//			return this.hashcache.get(entry.getSatDelta()).equals(((UnsatEntry) entry).getSolution().toString());
//		} else {
//        	// This should not be happening
//        	return false;
//		}
//    }
//
//    public Entry[] getEntries() {
//        return (Entry[]) entries.toArray();
//    }
//
//    /**
//     * @return the number of entries in the repo.
//     */
//    public abstract int size();
//
//    remove protected abstract E[] allEntries(double satDelta, int N);
//
//   remove protected abstract E[] linearSearch(double satDelta, int N, int k);
//
//    class CompareToReferenceScore implements Comparator<E> {
//        private final Double referenceScore;
//
//        public CompareToReferenceScore(Double referenceScore) {
//            this.referenceScore = referenceScore;
//        }
//
//        @Override
//        public int compare(E e1, E e2) {
//            return Double.compare(e1.getSatDelta(), e2.getSatDelta());
//        }
//    }
//
//    /**
//     * Returns k entries closest to the given SATDelta.
//     *
//     * @author Andrea Aquino
//     * @param SATDelta the given SATDelta to use as reference for filtering
//     * @param variables a given list of variables used in the expression
//     * @param k the number of entries to extract
//     * @return the filtered entries, sorted by increasing distance from the given SATDelta.
//     */
//    private Entry[] filterByProximity(Double SATDelta, SortedSet<IntVariable> variables, int k) {
//        int N = variables.size();
//        if (this.size() <= k) {
//            // If the number of requested entries exceeds the available entries,
//            // return them all in the right order.
//            return allEntries(SATDelta, N);
//        } else {
//            return linearSearch(SATDelta, N, k);
//        }
//    }
//
//    /**
//     * Returns k entries closest to the given SATDelta.
//     *
//     * @param SATDelta the given SATDelta to use as reference for filtering
//     * @param variables a given list of variables used in the expression
//     * @param k the number of entries to extract
//     * @return the extracted entries, sorted by increasing distance from the given SATDelta.
//     */
//    public Entry[] extract(Double SATDelta, SortedSet<IntVariable> variables, int k) {
//        if (k <= 0) {
//            return new Entry[1];
//        } else {
//            return this.filterByProximity(SATDelta, variables, k);
//        }
//    }

//    @Override
//    protected Entry[] allEntries(Double SATDelta, int N) {
//        Entry[] entriesCopy = new Entry[this.size()];
//        this.entries.sort(new CompareToReferenceScore(0.0));
//        int i = 0;
//        for (Entry e : this.entries) {
//            if (isValid(e.getSize(), N)) {
//                entriesCopy[i] = e;
//                i++;
//            }
//        }
//        return entriesCopy;
//    }


//    @Override
//    protected Entry[] linearSearch(Double SATDelta, int N, int k) {
//        PriorityQueue<Pair<Double, Entry>> queue = new PriorityQueue<>(
//                k,
//                (p1, p2) -> ((-1) * (p1.getL().compareTo(p2.getL()))));
//
//        // Load the first k entries in the queue, then keep updating the queue
//        // inserting elements whose distance from satDelta is smaller than the
//        // distance of the maximum element in the queue. The maximum is removed
//        // whenever a new elements is inserted so that the overall complexity
//        // of this method is O(n*log(k)).
//        int i = 0;
//        for (final Entry entry : this.entries) {
//            if (!default_zero) {
//                if (!isValid(entry.getSize(), N)) {
//                    // Entries containing models with less variables than
//                    // the reference expression are immediately discarded.
//                    continue;
//                }
//            }
//            Double delta = Math.abs(entry.getSATDelta() - SATDelta);
//
//            if (i++ < k) {
//                queue.add(new Pair<>(delta, entry));
//            } else {
//                Pair<Double, Entry> head = queue.peek();
//                if (delta.compareTo(head.getL()) < 0) {
//                    queue.poll();
//                    queue.add(new Pair<>(delta, entry));
//                }
//            }
//        }
//
//        Entry[] closest = new Entry[k];
//        int j = k-1;
//        while (!queue.isEmpty()) {
//            closest[j] = queue.remove().getR();
//            j--;
//        }
//
//        return closest;
//    }
//}
