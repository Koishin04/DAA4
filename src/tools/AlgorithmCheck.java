import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Independent regression checks, using exhaustive or deliberately simple oracles.
 * Compile src first, then run: java -cp out tools/AlgorithmCheck.java
 * Any failure throws AssertionError and exits with a nonzero status.
 */
public class AlgorithmCheck {
    private static final Random RANDOM = new Random(4_254332L);
    private static int assertions;

    public static void main(String[] args) {
        checkStrains();
        checkMarkers();
        checkSharedWords();
        checkTrees();
        System.out.println("Algorithm checks passed: " + assertions + " assertions.");
    }

    private static void checkStrains() {
        List<String> words = new ArrayList<>();
        binaryWords("", 4, words);
        for (String a : words) {
            for (String b : words) {
                equal(editByEnumeratingAlignments(a, b, 0, 0), Strains.editDistance(a, b),
                        "edit distance: " + a + " / " + b);
                equal(lcsBySubsets(a, b), Strains.lcsLength(a, b), "LCS: " + a + " / " + b);
            }
        }
        for (int i = 0; i < 200; i++) {
            String a = dna(RANDOM.nextInt(6));
            String b = dna(RANDOM.nextInt(6));
            equal(editByEnumeratingAlignments(a, b, 0, 0), Strains.editDistance(a, b), "random edit");
            equal(lcsBySubsets(a, b), Strains.lcsLength(a, b), "random LCS");
        }
        equal(3, Strains.editDistance("GATTACA", "GCATGCA"), "documented edit example");
        equal(3, Strains.lcsLength("ACGTT", "AGTC"), "documented LCS example");
        System.out.println("  Strains: exhaustive short strings and independent subsequence oracle passed.");
    }

    private static void checkMarkers() {
        String[][] cases = {
            {"", ""}, {"ACG", ""}, {"", "A"}, {"A", "AAAA"},
            {"AAA", "AA"}, {"ACGACGACGT", "ACGAC"}, {"ACGT", "T"},
            {"ACACACACAC", "ACAC"}, {"GTGTGT", "A"}, {"AACAAAACAAAA", "AACAAAA"}
        };
        for (String[] entry : cases) checkSearch(entry[0], entry[1]);
        for (int i = 0; i < 700; i++) {
            checkSearch(dna(RANDOM.nextInt(90)), dna(RANDOM.nextInt(18)));
        }
        checkSearch("A".repeat(100_000), "A".repeat(1_999) + "B");
        checkSearch("A".repeat(4_000), "A".repeat(500));
        System.out.println("  Marker: direct prefix oracle, overlapping matches, empty and adversarial cases passed.");
    }

    private static void checkSearch(String text, String pattern) {
        arrayEqual(naivePrefix(pattern), Marker.prefixFunction(pattern), "prefix function");
        arrayEqual(naiveMatches(text, pattern), Marker.findAll(text, pattern), "KMP matches");
    }

    private static void checkSharedWords() {
        equal(1, Marker.sharedWords("ACGTAC", "TTACGT", 4), "documented shared word");
        equal(5, Marker.sharedWords("AAAAAA", "AAA", 2), "count positions, not distinct words");
        equal(0, Marker.sharedWords("", "ACGT", 1), "empty first strain");
        equal(0, Marker.sharedWords("ACGT", "", 1), "empty second strain");
        equal(0, Marker.sharedWords("ACGT", "ACGT", 5), "window too long");
        equal(1, Marker.sharedWords("ACGT", "ACGT", 4), "whole-strain window");
        for (int i = 0; i < 900; i++) {
            String a = dna(RANDOM.nextInt(40));
            String b = dna(RANDOM.nextInt(40));
            int k = 1 + RANDOM.nextInt(14);
            equal(naiveSharedWords(a, b, k), Marker.sharedWords(a, b, k), "random shared words");
        }
        expectIllegalArgument(() -> Marker.sharedWords("AC", "AC", 0), "zero window");
        expectIllegalArgument(() -> Marker.sharedWords("AC", "AC", -1), "negative window");

        // Birthday search creates an actual collision for the documented production hash.
        // The oracle still compares the original words, never just their hashes.
        String[] collision = collisionPair();
        equal(hash(collision[0]), hash(collision[1]), "collision fixture has equal hashes");
        require(!collision[0].equals(collision[1]), "collision fixture uses different words");
        equal(0, Marker.sharedWords(collision[0], collision[1], 12), "hash collision is not equality");
        String withBoth = collision[1] + collision[0];
        equal(naiveSharedWords(collision[0], withBoth, 12),
                Marker.sharedWords(collision[0], withBoth, 12), "collision bucket retains true match");
        System.out.println("  Shared words: exact-window oracle and real modular-hash collision passed.");
    }

    private static void checkTrees() {
        checkTree(new int[][] {{}}, new int[] {7});
        checkTree(new int[][] {{}}, new int[] {0});
        checkTree(new int[][] {{1}, {0}}, new int[] {0, 0});
        checkTree(new int[][] {{1, 2, 3}, {0}, {0}, {0}}, new int[] {100, 1, 1, 1});
        checkTree(new int[][] {{1}, {0, 2}, {1, 3}, {2, 4}, {3, 5}, {4}},
                new int[] {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
                           Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE});
        for (int iteration = 0; iteration < 450; iteration++) {
            int n = 1 + RANDOM.nextInt(11);
            List<List<Integer>> neighbours = new ArrayList<>();
            for (int v = 0; v < n; v++) neighbours.add(new ArrayList<>());
            int[] labels = new int[n];
            for (int v = 0; v < n; v++) labels[v] = v;
            for (int v = n - 1; v > 0; v--) {
                int j = RANDOM.nextInt(v + 1);
                int temporary = labels[v]; labels[v] = labels[j]; labels[j] = temporary;
            }
            for (int v = 1; v < n; v++) {
                int first = labels[v], second = labels[RANDOM.nextInt(v)];
                neighbours.get(first).add(second);
                neighbours.get(second).add(first);
            }
            int[][] nb = new int[n][];
            int[] costs = new int[n];
            for (int v = 0; v < n; v++) {
                nb[v] = neighbours.get(v).stream().mapToInt(Integer::intValue).toArray();
                costs[v] = RANDOM.nextInt(12);
            }
            checkTree(nb, costs);
            Arrays.fill(costs, 1);
            checkTree(nb, costs);
        }
        System.out.println("  Trees: exhaustive vertex covers, reconstructed edges, large costs and all-pairs diameter passed.");
    }

    private static void checkTree(int[][] nb, int[] costs) {
        boolean[] chosen = Outbreak.cheapestTests(nb.length, nb, costs);
        equal(nb.length, chosen.length, "one choice per person");
        long total = 0;
        for (int v = 0; v < nb.length; v++) {
            if (chosen[v]) total += costs[v];
            for (int w : nb[v]) require(chosen[v] || chosen[w], "uncovered edge " + v + "-" + w);
        }
        equal(exhaustiveCover(nb, costs), total, "minimum weighted vertex cover");
        equal(diameterByAllPairsBfs(nb), Outbreak.longestChain(nb.length, nb), "tree diameter in links");
    }

    private static void binaryWords(String prefix, int remaining, List<String> words) {
        words.add(prefix);
        if (remaining == 0) return;
        binaryWords(prefix + "A", remaining - 1, words);
        binaryWords(prefix + "C", remaining - 1, words);
    }

    private static String dna(int length) {
        StringBuilder word = new StringBuilder();
        for (int i = 0; i < length; i++) word.append("ACGT".charAt(RANDOM.nextInt(4)));
        return word.toString();
    }

    private static int editByEnumeratingAlignments(String a, String b, int i, int j) {
        if (i == a.length()) return b.length() - j;
        if (j == b.length()) return a.length() - i;
        int diagonal = editByEnumeratingAlignments(a, b, i + 1, j + 1)
                + (a.charAt(i) == b.charAt(j) ? 0 : 1);
        int remove = 1 + editByEnumeratingAlignments(a, b, i + 1, j);
        int insert = 1 + editByEnumeratingAlignments(a, b, i, j + 1);
        return Math.min(diagonal, Math.min(remove, insert));
    }

    private static int lcsBySubsets(String a, String b) {
        int best = 0;
        for (int subset = 0; subset < (1 << a.length()); subset++) {
            int j = 0, count = 0;
            boolean possible = true;
            for (int i = 0; i < a.length(); i++) {
                if ((subset & (1 << i)) == 0) continue;
                while (j < b.length() && b.charAt(j) != a.charAt(i)) j++;
                if (j == b.length()) { possible = false; break; }
                count++; j++;
            }
            if (possible) best = Math.max(best, count);
        }
        return best;
    }

    private static int[] naivePrefix(String pattern) {
        int[] prefix = new int[pattern.length()];
        for (int end = 0; end < pattern.length(); end++) {
            for (int length = end; length > 0; length--) {
                if (sameWindow(pattern, 0, pattern, end - length + 1, length)) {
                    prefix[end] = length; break;
                }
            }
        }
        return prefix;
    }

    private static int[] naiveMatches(String text, String pattern) {
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i + pattern.length() <= text.length(); i++) {
            if (sameWindow(text, i, pattern, 0, pattern.length())) positions.add(i);
        }
        return positions.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int naiveSharedWords(String a, String b, int k) {
        int count = 0;
        for (int i = 0; i + k <= a.length(); i++) {
            for (int j = 0; j + k <= b.length(); j++) {
                if (sameWindow(a, i, b, j, k)) { count++; break; }
            }
        }
        return count;
    }

    private static boolean sameWindow(String a, int startA, String b, int startB, int k) {
        for (int i = 0; i < k; i++) if (a.charAt(startA + i) != b.charAt(startB + i)) return false;
        return true;
    }

    private static long hash(String word) {
        long value = 0;
        for (int i = 0; i < word.length(); i++) value = (value * 257 + word.charAt(i)) % 1_000_000_007L;
        return value;
    }

    private static String[] collisionPair() {
        Map<Long, String> seen = new HashMap<>();
        Random random = new Random(92_384);
        for (int attempt = 0; attempt < 300_000; attempt++) {
            StringBuilder word = new StringBuilder();
            for (int i = 0; i < 12; i++) word.append("ACGT".charAt(random.nextInt(4)));
            String current = word.toString();
            String previous = seen.putIfAbsent(hash(current), current);
            if (previous != null && !previous.equals(current)) return new String[] {previous, current};
        }
        throw new AssertionError("Could not construct the deterministic hash-collision fixture");
    }

    private static long exhaustiveCover(int[][] nb, int[] cost) {
        long best = Long.MAX_VALUE;
        for (int mask = 0; mask < (1 << nb.length); mask++) {
            boolean covered = true;
            long total = 0;
            for (int v = 0; v < nb.length; v++) {
                if ((mask & (1 << v)) != 0) total += cost[v];
                for (int w : nb[v]) {
                    if ((mask & (1 << v)) == 0 && (mask & (1 << w)) == 0) covered = false;
                }
            }
            if (covered) best = Math.min(best, total);
        }
        return best;
    }

    private static int diameterByAllPairsBfs(int[][] nb) {
        int maximum = 0;
        for (int start = 0; start < nb.length; start++) {
            int[] distance = new int[nb.length];
            Arrays.fill(distance, -1);
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            distance[start] = 0; queue.add(start);
            while (!queue.isEmpty()) {
                int vertex = queue.remove();
                for (int next : nb[vertex]) {
                    if (distance[next] != -1) continue;
                    distance[next] = distance[vertex] + 1;
                    maximum = Math.max(maximum, distance[next]);
                    queue.add(next);
                }
            }
        }
        return maximum;
    }

    private static void expectIllegalArgument(Runnable operation, String label) {
        try { operation.run(); } catch (IllegalArgumentException expected) { assertions++; return; }
        throw new AssertionError(label + ": expected IllegalArgumentException");
    }

    private static void arrayEqual(int[] expected, int[] actual, String label) {
        require(Arrays.equals(expected, actual), label + ": expected " + Arrays.toString(expected)
                + ", got " + Arrays.toString(actual));
    }

    private static void equal(long expected, long actual, String label) {
        require(expected == actual, label + ": expected " + expected + ", got " + actual);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        assertions++;
    }
}
