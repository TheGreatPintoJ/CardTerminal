// @formatter:off
package me.pintoadmin.cardTerminal.dev.solitaire;

import java.util.*;
import me.pintoadmin.cardTerminal.games.solitaire.Solitaire;

public class Solver {
    // card = suit*13 + rank (rank 0=Ace..12=King); suits SPADE,CLUB,HEART,DIAMOND as in Card.Suit
    static int suit(int c) { return c / 13; }
    static int rank(int c) { return c % 13; }
    static boolean red(int c) { return suit(c) >= 2; }
    static final String SU = "SCHD", RA = "A23456789TJQK";
    static String n(int c) { return "" + RA.charAt(rank(c)) + SU.charAt(suit(c)); }

    static final class S {
        byte[][] col = new byte[7][]; int[] down = new int[7];
        byte[] stock; int pos; int[] fnd = new int[4];
        int draw = 1; // cards turned per draw; constant within a search, so not part of key()
        int g; S parent; String mv;
        S copy() {
            S s = new S();
            for (int i = 0; i < 7; i++) s.col[i] = col[i];
            s.down = down.clone(); s.stock = stock; s.pos = pos; s.fnd = fnd.clone();
            s.draw = draw; s.g = g; return s;
        }
        int found() { return fnd[0] + fnd[1] + fnd[2] + fnd[3]; }
        String key() {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < 7; i++) { b.append(down[i]).append(':'); for (byte c : col[i]) b.append((char) (c + 65)); b.append('|'); }
            for (byte c : stock) b.append((char) (c + 65));
            b.append('/').append(pos).append(Arrays.toString(fnd));
            return b.toString();
        }
    }

    static int drawCount(Solitaire.GameType type) { return type == Solitaire.GameType.KLONDIKE3 ? 3 : 1; }

    static S deal(long seed, Solitaire.GameType type) {
        List<Integer> d = new LinkedList<>();
        for (int s = 0; s < 4; s++) for (int r = 0; r < 13; r++) d.add(s * 13 + r);
        Collections.shuffle(d, new Random(seed));
        S s = new S(); int k = 0; s.draw = drawCount(type);
        for (int c = 0; c < 7; c++) { s.col[c] = new byte[c + 1]; for (int r = 0; r <= c; r++) s.col[c][r] = (byte) (int) d.get(k++); s.down[c] = c; }
        s.stock = new byte[24]; for (int i = 0; i < 24; i++) s.stock[i] = (byte) (int) d.get(k++);
        return s;
    }

    static byte[] rm(byte[] a, int i) { byte[] b = new byte[a.length - 1]; System.arraycopy(a, 0, b, 0, i); System.arraycopy(a, i + 1, b, i, a.length - i - 1); return b; }
    static byte[] app(byte[] a, byte[] x, int from) { byte[] b = Arrays.copyOf(a, a.length + x.length - from); System.arraycopy(x, from, b, a.length, x.length - from); return b; }
    static boolean fits(byte[] dst, int c) { if (dst.length == 0) return rank(c) == 12; int t = dst[dst.length - 1]; return red(t) != red(c) && rank(t) == rank(c) + 1; }
    static void flip(S s, int i) { if (s.down[i] >= s.col[i].length && s.down[i] > 0) s.down[i] = s.col[i].length - 1; }

    static List<S> moves(S s) {
        List<S> out = new ArrayList<>();
        // tableau -> foundation
        for (int i = 0; i < 7; i++) if (s.col[i].length > s.down[i]) {
            int c = s.col[i][s.col[i].length - 1];
            if (s.fnd[suit(c)] == rank(c)) { S t = s.copy(); t.col[i] = Arrays.copyOf(s.col[i], s.col[i].length - 1); flip(t, i); t.fnd[suit(c)]++; t.mv = n(c) + " col" + (i + 1) + "->F"; out.add(t); }
        }
        // waste -> foundation / tableau
        if (s.pos > 0) {
            int c = s.stock[s.pos - 1];
            if (s.fnd[suit(c)] == rank(c)) { S t = s.copy(); t.stock = rm(s.stock, s.pos - 1); t.pos--; t.fnd[suit(c)]++; t.mv = n(c) + " waste->F"; out.add(t); }
            for (int j = 0; j < 7; j++) if (fits(s.col[j], c)) { S t = s.copy(); t.stock = rm(s.stock, s.pos - 1); t.pos--; t.col[j] = app(s.col[j], new byte[]{(byte) c}, 0); t.mv = n(c) + " waste->col" + (j + 1); out.add(t); }
        }
        // tableau -> tableau
        for (int i = 0; i < 7; i++) for (int k = s.down[i]; k < s.col[i].length; k++) {
            int c = s.col[i][k];
            if (k == 0 && rank(c) == 12) continue; // pointless king shuffle
            if (k - s.down[i] >= 9 && k < s.col[i].length - 1) continue; // the game's row keys only reach 1-9 and last
            for (int j = 0; j < 7; j++) if (j != i && fits(s.col[j], c)) {
                S t = s.copy(); t.col[j] = app(s.col[j], s.col[i], k); t.col[i] = Arrays.copyOf(s.col[i], k); flip(t, i);
                t.mv = n(c) + " col" + (i + 1) + "->col" + (j + 1); out.add(t); break;
            }
        }
        // draw / redeal
        if (s.stock.length > 0) { S t = s.copy(); if (s.pos < s.stock.length) { t.pos = Math.min(s.pos + s.draw, s.stock.length); t.mv = "draw"; } else { t.pos = 0; t.mv = "redeal"; } out.add(t); }
        for (S t : out) { t.g = s.g + 1; t.parent = s; }
        return out;
    }

    static int h(S s) {
        int down = 0; for (int i = 0; i < 7; i++) down += s.down[i];
        return (52 - s.found()) + down;
    }

    static S solve(S start, int budget, double w) {
        PriorityQueue<S> pq = new PriorityQueue<>(Comparator.comparingDouble(x -> x.g + w * h(x)));
        Map<String, Integer> seen = new HashMap<>();
        pq.add(start); seen.put(start.key(), 0);
        int n = 0;
        while (!pq.isEmpty() && n++ < budget) {
            S s = pq.poll();
            if (s.found() == 52) return s;
            for (S t : moves(s)) { String k = t.key(); Integer o = seen.get(k); if (o == null || o > t.g) { seen.put(k, t.g); pq.add(t); } }
        }
        return null;
    }

    public static void main(String[] a) {
        int tries = Integer.parseInt(a[0]);
        SplittableRandom r = new SplittableRandom(Long.parseLong(a[1]));
        Solitaire.GameType type = a.length > 2 ? Solitaire.GameType.valueOf(a[2]) : Solitaire.GameType.KLONDIKE1;
        long best = 0; int bestLen = Integer.MAX_VALUE; int solved = 0;
        for (int i = 0; i < tries; i++) {
            long seed = r.nextLong();
            S sol = solve(deal(seed, type), 20000, 3.0);
            if (sol == null) continue;
            solved++;
            if (sol.g < bestLen) { bestLen = sol.g; best = seed; System.out.println("seed " + seed + " len " + sol.g); }
        }
        System.out.println("solved " + solved + "/" + tries + " best " + best + " len " + bestLen);
        if (solved == 0) return;
        S sol = solve(deal(best, type), 2000000, 1.5);
        if (sol == null) sol = solve(deal(best, type), 20000, 3.0);
        List<String> path = new ArrayList<>(); for (S s = sol; s.parent != null; s = s.parent) path.add(s.mv);
        Collections.reverse(path);
        S d = deal(best, type);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 7; i++) { b.append("col" + (i + 1) + ": "); for (int k = 0; k < d.col[i].length; k++) b.append(k < d.down[i] ? "[" + n(d.col[i][k]) + "] " : n(d.col[i][k]) + " "); b.append('\n'); }
        b.append("stock: "); for (byte c : d.stock) b.append(n(c) + " ");
        System.out.println(b);
        System.out.println("solution (" + path.size() + "): " + String.join(", ", path));
    }
}
// @formatter:on
