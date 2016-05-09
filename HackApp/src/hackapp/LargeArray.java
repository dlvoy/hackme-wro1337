package hackapp;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Based on:
 * http://vanillajava.blogspot.de/2011/12/using-memory-mapped-file-for-huge.html
 * Changed to work with one dimensional array
 */
public class LargeArray implements Closeable {

    private static final int MAPPING_SIZE = 1 << 30;
    private final RandomAccessFile raf;
    private final List<ByteBuffer> mappings = new ArrayList<>();

    public LargeArray(String filename, long arraySize) throws IOException {
        this.raf = new RandomAccessFile(filename, "rw");
        try {

            long size = 4L * arraySize;
            for (long offset = 0; offset < size; offset += MAPPING_SIZE) {
                long size2 = Math.min(size - offset, MAPPING_SIZE);
                mappings.add(raf.getChannel().map(FileChannel.MapMode.READ_WRITE, offset, size2));
            }

            System.out.println("Mappings count: " + (mappings.size()) + ", " + MAPPING_SIZE + "B each");
        } catch (IOException e) {
            raf.close();
            throw e;
        }
    }

    protected long position(int n) {
        return getUnsignedInt(n);
    }

    public static long getUnsignedInt(long x) {
        return x & 0x00000000ffffffffL;
    }

    public int get(int n) {
        return get(position(n));
    }

    public void set(int n, int v) {
        set(position(n), v);
    }

    public int get(long n) {
        long p = n * 4L;
        int mapN = (int) (p / MAPPING_SIZE);
        int offN = (int) (p % MAPPING_SIZE);
        return mappings.get(mapN).getInt(offN);
    }

    public void set(long n, int v) {
        long p = n * 4L;
        int mapN = (int) (p / MAPPING_SIZE);
        int offN = (int) (p % MAPPING_SIZE);
        mappings.get(mapN).putInt(offN, v);
    }

    @Override
    public void close() throws IOException {
        // A System.gc() is required to remove the memory mappings.
        mappings.clear();
        raf.close();
        System.gc();
    }

}
