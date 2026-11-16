package bumbumapps.a2048game.engine;

/** Small deterministic PRNG (SplitMix64) whose whole state is one long, so it can be saved and restored. */
public final class SeededRandom {
    private long state;

    public SeededRandom(long seed) {
        this.state = seed;
    }

    public long getState() {
        return state;
    }

    public void setState(long state) {
        this.state = state;
    }

    public long nextLong() {
        state += 0x9E3779B97F4A7C15L;
        long z = state;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Uniform int in [0, bound). */
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound");
        }
        return (int) ((nextLong() >>> 1) % bound);
    }

    /** Uniform double in [0, 1). */
    public double nextDouble() {
        return (nextLong() >>> 11) * (1.0 / (1L << 53));
    }
}
