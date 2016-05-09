package hackapp;

/**
 * @author ddzienia
 */
public class Commons {

    public static final long MAX_UNSIGNED = 4294967295L;

    public static long getUnsignedInt(int x) {
        return x & 0x00000000ffffffffL;
    }
    
    public static String formatPromile(long now, long maxv) {
        return ""+(int)(((double)now/(double)maxv)*1000.0)/10.0+"%";
    }

}
