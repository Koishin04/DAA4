import java.util.*;

/**
 * Missions 3 and 4. Find a word in a long text.
 */
public class Marker {
    private static final long BASE = 257;
    private static final long MOD = 1_000_000_007;

    /**
     * Mission 3a. The prefix function of p. pi[i] = the length of the longest proper prefix of p[0..i] that is
     * also a suffix of p[0..i] ("proper" means shorter than p[0..i] itself).
     * Example: p = "ACGAC" gives [0, 0, 0, 1, 2]. The last number is the "Border length" line.
     */
    public static int[] prefixFunction(String p) {
        int[] pi = new int[p.length()];
        for (int i = 1; i < p.length(); i++) {
            int matched = pi[i - 1];
            while (matched > 0 && p.charAt(i) != p.charAt(matched)) {
                matched = pi[matched - 1];
            }
            if (p.charAt(i) == p.charAt(matched)) matched++;
            pi[i] = matched;
        }
        return pi;
    }

    /**
     * Mission 3b. KMP. All start positions (counting from 0, in increasing order) where p occurs in text.
     * Overlapping occurrences count: "ACG" occurs in "ACGACG" at 0 and 3, and "AA" in "AAA" at 0 and 1.
     * Use the prefix function: the text pointer never goes back. The number of positions is "Marker hits",
     * the first one is "First hit" (-1 when there is none).
     * An empty pattern matches at every boundary, including the end of the text.
     */
    public static int[] findAll(String text, String p) {
        if (p.isEmpty()) {
            int[] boundaries = new int[text.length() + 1];
            for (int i = 0; i < boundaries.length; i++) boundaries[i] = i;
            return boundaries;
        }
        int[] pi = prefixFunction(p);
        List<Integer> hits = new ArrayList<>();
        int matched = 0;
        for (int i = 0; i < text.length(); i++) {
            while (matched > 0 && text.charAt(i) != p.charAt(matched)) {
                matched = pi[matched - 1];
            }
            if (text.charAt(i) == p.charAt(matched)) matched++;
            if (matched == p.length()) {
                hits.add(i - p.length() + 1);
                // Keep the longest border so overlapping occurrences are found.
                matched = pi[matched - 1];
            }
        }
        int[] result = new int[hits.size()];
        for (int i = 0; i < result.length; i++) result[i] = hits.get(i);
        return result;
    }

    /**
     * Mission 4. Rabin-Karp. The number of positions i in a such that the word of k letters starting at i
     * (a[i..i+k-1]) also occurs somewhere in b. Count positions, not different words. This is the "Shared words" line.
     * The idea: give every window of k letters a number (a rolling hash, updated in O(1) when the window moves by one),
     * look the number up among the windows of b, and when it is found COMPARE THE LETTERS, because two different words
     * can have the same hash.
     * Example: a = "ACGTAC", b = "TTACGT", k = 4: the words of a are ACGT, CGTA, GTAC; only ACGT is in b, so the answer is 1.
     * k must be positive; strings shorter than k have no such windows.
     */
    public static int sharedWords(String a, String b, int k) {
        if (k <= 0) throw new IllegalArgumentException("Window length must be positive");
        if (a.length() < k || b.length() < k) return 0;

        long power = 1;
        for (int i = 1; i < k; i++) power = power * BASE % MOD;

        // Retain all positions in a bucket: different words can share a hash.
        Map<Long, List<Integer>> positions = new HashMap<>();
        long hash = firstHash(b, k);
        for (int j = 0; j <= b.length() - k; j++) {
            positions.computeIfAbsent(hash, ignored -> new ArrayList<>()).add(j);
            if (j < b.length() - k) {
                hash = roll(hash, b.charAt(j), b.charAt(j + k), power);
            }
        }

        int count = 0;
        hash = firstHash(a, k);
        for (int i = 0; i <= a.length() - k; i++) {
            List<Integer> candidates = positions.get(hash);
            if (candidates != null) {
                for (int j : candidates) {
                    if (sameWindow(a, i, b, j, k)) {
                        count++;
                        break; // Count this position of A once, regardless of B duplicates.
                    }
                }
            }
            if (i < a.length() - k) {
                hash = roll(hash, a.charAt(i), a.charAt(i + k), power);
            }
        }
        return count;
    }

    private static long firstHash(String s, int k) {
        long hash = 0;
        for (int i = 0; i < k; i++) hash = (hash * BASE + s.charAt(i)) % MOD;
        return hash;
    }

    private static long roll(long hash, char outgoing, char incoming, long power) {
        long removed = (hash - outgoing * power % MOD + MOD) % MOD;
        return (removed * BASE + incoming) % MOD;
    }

    private static boolean sameWindow(String a, int i, String b, int j, int k) {
        for (int offset = 0; offset < k; offset++) {
            if (a.charAt(i + offset) != b.charAt(j + offset)) return false;
        }
        return true;
    }
}
