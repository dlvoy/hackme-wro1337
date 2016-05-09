package hackapp;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author ddzienia
 */
public class HackApp {

    static int[] crcTable = new int[256];
    public static LargeArray revCRC;

    // --- converted from JS ---------------------------------------------------
    static void makeCRCTable() {

        for (int n = 0; n < 256; n++) {
            int c = n;
            for (int k = 0; k < 8; k++) {
                c = (((c & 1) != 0) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1));
            }
            crcTable[n] = c;
        }

    }

    static int checkCRC(int value) {

        int crc = 0 ^ (-1);

        for (int i = 0; i < 4; i++) {
            crc = (crc << 8) ^ crcTable[((crc ^ value) & 0xFF)];
            value >>= 8;
        }

        return (crc ^ (-1));
    }

    static String keyDerivate(int value) {
        String key = "";
        int valueOrg = value;
        for (int i = 0; i < 100000; i++) {
            value = checkCRC(value);
            if (i == 0 || i == (100000 - 2)) {
                key += Commons.getUnsignedInt(value);
            }
        }

        System.out.println("KDF, for starting secret: " + Commons.getUnsignedInt(valueOrg) + " CRC is: " + Commons.getUnsignedInt(value));

        return key;
    }

    // --- reverse functions ---------------------------------------------------
    static int reverseCRC(int value) {
        return revCRC.get(value);
    }

    static String reverseKeyDerivate(int value) {
        String key = "";
        int valueOrg = value;
        for (int i = 100000 - 1; i >= 0; i--) {
            value = reverseCRC(value);
            if (i == 0 || i == (100000 - 2)) {
                key = "" + Commons.getUnsignedInt(checkCRC(value)) + key;
            }
        }

        System.out.println("Reversed KDF, for result CRC: " + Commons.getUnsignedInt(valueOrg) + " starting secret is: " + Commons.getUnsignedInt(value));

        return key;
    }

    // --- main algorithm ------------------------------------------------------
    public static void main(String[] args) {

        makeCRCTable();

        if ((args.length == 1) && (args[0].equals("lookup"))) {
            testFullCRCLookup();
            return;
        }

        try {

            boolean create = !(new File("revcrc.memmap").exists());
            revCRC = new LargeArray("revcrc.memmap", Commons.MAX_UNSIGNED + 1);

            if (create) {

                buildReverseCRCTable();

            } else if ((args.length == 1) && (args[0].equals("test"))) {
                testCRC();
                testKDF();
            } else {
                // HACK!
                System.out.println("Reversed KDF secret key: " + reverseKeyDerivate(115056530));
                // test result
                System.out.println("KDF secret key: " + keyDerivate(1086825474));
            }

            revCRC.close();

        } catch (IOException ex) {
            Logger.getLogger(HackApp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Test how long it take to make full lookup over whole domain (CRCs for all
     * available arguments)
     *
     * it take about 20s on my machine
     */
    private static void testFullCRCLookup() {
        long start = System.nanoTime();
        for (long ui = 0; ui <= Commons.MAX_UNSIGNED; ui++) {
            checkCRC((int) ui);
            if (ui % 1000000 == 0) {
                System.out.println("Building... " + ui + " " + Commons.formatPromile(ui, Commons.MAX_UNSIGNED));
            }
        }
        long time = System.nanoTime() - start;
        System.out.printf("Full lookup took %,d ms \n", time / 1000 / 1000);
    }

    /**
     * Build reverse CRC lookup table
     */
    private static void buildReverseCRCTable() {
        long start = System.nanoTime();
        for (long round = 0; round < 16; round++) {
            for (long ui = 0L; ui <= Commons.MAX_UNSIGNED; ui++) {

                int out = checkCRC((int) ui);
                int calcround = (int) (Commons.getUnsignedInt(out) >> 28);
                if (calcround == round) {
                    revCRC.set(out, (int) ui);
                }
                if (ui % 1000000 == 0) {
                    System.out.println("Building [" + (round + 1) + "/16]... " + ui + " " + Commons.formatPromile(ui, Commons.MAX_UNSIGNED));
                }
            }
        }

        long time = System.nanoTime() - start;
        System.out.printf("Lookup table build took %,d ms \n", time / 1000 / 1000);
    }

    /**
     * Smoke test KDF and it's reverse function
     */
    private static void testKDF() {
        System.out.println("================================");
        System.out.println("KDF 0 key: " + keyDerivate(0));
        System.out.println("----------------");
        System.out.println("KDF 1 key: " + keyDerivate(1));
        System.out.println("----------------");
        System.out.println("KDF 2 key: " + keyDerivate(2));
        System.out.println("----------------");
        System.out.println("KDF 23454321 key: " + keyDerivate(23454321));
        System.out.println("================================");
        System.out.println("Reversed KDF 0 key: " + reverseKeyDerivate((int) 2517331564L));
        System.out.println("----------------");
        System.out.println("Reversed KDF 1 key: " + reverseKeyDerivate((int) 3784151453L));
        System.out.println("----------------");
        System.out.println("Reversed KDF 2 key: " + reverseKeyDerivate((int) 1714578838L));
        System.out.println("----------------");
        System.out.println("Reversed KDF 23454321 key: " + reverseKeyDerivate((int) 2437944843L));
        System.out.println("================================");
    }

    /**
     * Smoke test CRC and it's reverse function
     */
    private static void testCRC() {
        System.out.println("================================");
        System.out.println("CRC 0: " + checkCRC(0));
        System.out.println("CRC 1: " + checkCRC(1));
        System.out.println("CRC 12: " + checkCRC(12));
        System.out.println("CRC 1000: " + checkCRC(1000));
        System.out.println("CRC 2345: " + checkCRC(2345));
        System.out.println("================================");
        System.out.println("Reversed CRC -135508921: " + reverseCRC(-135508921));
        System.out.println("Reversed CRC 1675603121: " + reverseCRC(1675603121));
        System.out.println("Reversed CRC 165826307: " + reverseCRC(165826307));
        System.out.println("Reversed CRC 1260895812: " + reverseCRC(1260895812));
        System.out.println("Reversed CRC 1998446552: " + reverseCRC(1998446552));
    }

}
