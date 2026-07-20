package org.bitcoinkernel;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.bitcoinkernel.Script.*;

public class ScriptPubkeyTest {
    static {
        try {
            System.loadLibrary("bitcoinkernel");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Failed to load libbitcoinkernel in test: " + e.getMessage());
        }
    }

    // P2SH: OP_HASH160 <20-byte hash> OP_EQUAL
    private static final byte[] P2SH_SCRIPT_PUBKEY = {
        (byte) 0xa9, 0x14,
        0x6c, 0x00, 0x2a, 0x68, 0x69, 0x59, 0x06, 0x7f, 0x48, 0x66,
        (byte) 0xb8, (byte) 0xfb, 0x49, 0x3a, (byte) 0xd7, (byte) 0x97, 0x02, (byte) 0x90, (byte) 0xab, 0x72,
        (byte) 0x87
    };

    @Test
    public void testToBytesRoundTrip() throws Exception {
        try (ScriptPubkey scriptPubkey = new ScriptPubkey(P2SH_SCRIPT_PUBKEY)) {
            assertArrayEquals(P2SH_SCRIPT_PUBKEY, scriptPubkey.toBytes());
        }
    }

    @Test
    public void testToBytesAfterCloseThrows() throws Exception {
        ScriptPubkey scriptPubkey = new ScriptPubkey(P2SH_SCRIPT_PUBKEY);
        scriptPubkey.close();

        assertThrows(IllegalStateException.class, scriptPubkey::toBytes);
    }
}
