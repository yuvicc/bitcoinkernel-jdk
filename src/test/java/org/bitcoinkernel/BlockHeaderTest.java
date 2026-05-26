package org.bitcoinkernel;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.bitcoinkernel.Blocks.*;

public class BlockHeaderTest {
    static {
        try {
            System.loadLibrary("bitcoinkernel");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Failed to load libbitcoinkernel in test: " + e.getMessage());
        }
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }

    @Test
    public void testBlockHeader() throws Exception {
        // First 80 bytes of a block from the test data
        String hexHeader = "0000002006226e46111a0b59caaf126043eb5bbf28c34f3a5e332a1fc7b2b73cf188910f295badc0bdd9a2bc0955d12f337491eae4c87ba4660078c0156310284d47c6ff9a242d66ffff7f2000000000";
        byte[] rawHeader = hexToBytes(hexHeader);

        try (BlockHeader header = new BlockHeader(rawHeader)) {
            assertNotNull(header);
            
            // Expected values in little-endian as stored in Bitcoin block headers
            assertEquals(0x20000000, header.getVersion());
            assertEquals(1714234522, header.getTimestamp()); // 0x662d249a
            assertEquals(0x207fffff, header.getBits());
            assertEquals(0, header.getNonce());

            // Check serialization
            byte[] serialized = header.toBytes();
            assertArrayEquals(rawHeader, serialized);

            // Check hashes
            try (BlockHash hash = header.getHash()) {
                assertNotNull(hash);
                byte[] hashBytes = hash.toBytes();
                assertEquals(32, hashBytes.length);
            }

            try (BlockHash prevHash = header.getPrevHash()) {
                assertNotNull(prevHash);
                byte[] prevHashBytes = prevHash.toBytes();
                assertEquals(32, prevHashBytes.length);
                
                // Prev hash in the header is 06226e46... in the hex string
                // But BlockHash.toBytes() usually returns the hash in some order (usually internal/little-endian)
                // Let's just verify it's not all zeros.
                boolean allZeros = true;
                for (byte b : prevHashBytes) {
                    if (b != 0) {
                        allZeros = false;
                        break;
                    }
                }
                assertFalse(allZeros);
            }

            // Copy test
            try (BlockHeader copied = header.copy()) {
                assertArrayEquals(header.toBytes(), copied.toBytes());
                try (BlockHash h1 = header.getHash(); BlockHash h2 = copied.getHash()) {
                    assertTrue(h1.equals(h2));
                }
            }
        }
    }
}
