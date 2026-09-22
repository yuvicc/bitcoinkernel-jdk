package org.bitcoinkernel;

import static org.bitcoinkernel.jextract.bitcoinkernel_h.*;

// Functions intended for testing purposes only
public class Testing {

    static {
        try {
            System.loadLibrary("bitcoinkernel");
        } catch (UnsatisfiedLinkError e) {
            // This can happen if the library is not in the search path.
        }
    }

    private Testing() {}

    /**
     * Override the current time with a fixed timestamp. This affects all kernel time
     * reads globally, so callers are responsible for gating its use (e.g. to regtest).
     *
     * @param timestamp Unix epoch seconds, or 0 to restore the system clock
     * @return false if the timestamp is outside the valid [0, 4294967295] range
     */
    public static boolean setMockTime(long timestamp) {
        return btck_set_mock_time(timestamp) == 0;
    }
}
