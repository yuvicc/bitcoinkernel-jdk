package org.bitcoinkernel;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.bitcoinkernel.Transactions.*;

public class TransactionTest {
    static {
        try {
            System.loadLibrary("bitcoinkernel");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Failed to load libbitcoinkernel in test: " + e.getMessage());
        }
    }

    // Version 2 transaction with two P2PKH inputs and no witness data.
    private static final String LEGACY_TX_HEX =
        "020000000248c03e66fd371c7033196ce24298628e59ebefa00363026044e0f35e0325a65d000000006a4730440220048934"
        + "32347f39beaa280e99da595681ddb20fc45010176897e6e055d716dbfa022040a9e46648a5d10c33ef7cee5e6cf4b56bd51"
        + "3eae3ae044f0039824b02d0f44c012102982331a52822fd9b62e9b5d120da1d248558fac3da3a3c51cd7d9c8ad3da760efe"
        + "ffffffb856678c6e4c3c84e39e2ca818807049d6fba274b42af3c6d3f9d4b6513212d2000000006a473044022068bcedc7f"
        + "e39c9f21ad318df2c2da62c2dc9522a89c28c8420ff9d03d2e6bf7b0220132afd752754e5cb1ea2fd0ed6a38ec666781e34"
        + "b0e93dc9a08f2457842cf5660121033aeb9c079ea3e08ea03556182ab520ce5c22e6b0cb95cee6435ee17144d860cdfefff"
        + "fff0260d50b00000000001976a914363cc8d55ea8d0500de728ef6d63804ddddbdc9888ac67040f00000000001976a914c3"
        + "03bdc5064bf9c9a8b507b5496bd0987285707988ac6acb0700";

    // Version 1 transaction with a single P2WSH input.
    private static final String SEGWIT_TX_HEX =
        "010000000001011f97548fbbe7a0db7588a66e18d803d0089315aa7d4cc28360b6ec50ef36718a0100000000ffffffff02df1"
        + "776000000000017a9146c002a686959067f4866b8fb493ad7970290ab728757d29f0000000000220020701a8d401c84fb13"
        + "e6baf169d59684e17abd9fa216c8cc5b9fc63d622ff8c58d04004730440220565d170eed95ff95027a69b313758450ba84a"
        + "01224e1f7f130dda46e94d13f8602207bdd20e307f062594022f12ed5017bbf4a055a06aea91c10110a0e3bb23117fc0147"
        + "30440220647d2dc5b15f60bc37dc42618a370b2a1490293f9e5c8464f53ec4fe1dfe067302203598773895b4b16d37485cb"
        + "e21b337f4e4b650739880098c592553add7dd4355016952210375e00eb72e29da82b89367947f29ef34afb75e8654f6ea36"
        + "8e0acdfd92976b7c2103a1b26313f430c4b15bb1fdce663207659d8cac749a0e53d70eff01874496feff2103c96d495bfdd"
        + "5ba4145e3e046fee45e84a8a48ad05bd8dbb395c011a32cf9f88053ae00000000";

    // Double SHA256 of the full witness serialization of SEGWIT_TX_HEX, in internal byte order.
    private static final String SEGWIT_WTXID_HEX =
        "e99fc84be6355b0ccf5db6d1218e6930a440d610d1c0a91ee4a88a231be8352c";

    // Transaction with version 0xffffffff, which must not come back negative.
    private static final String MAX_VERSION_TX_HEX =
        "ffffffff0100000000000000000000000000000000000000000000000000000000000000000000000000ffffffff01000000"
        + "00000000000000000000";

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    @Test
    public void testGetVersion() throws Exception {
        try (Transaction tx = new Transaction(hexToBytes(LEGACY_TX_HEX))) {
            assertEquals(2L, tx.getVersion());
        }

        try (Transaction tx = new Transaction(hexToBytes(SEGWIT_TX_HEX))) {
            assertEquals(1L, tx.getVersion());
        }

        try (Transaction tx = new Transaction(hexToBytes(MAX_VERSION_TX_HEX))) {
            assertEquals(0xffffffffL, tx.getVersion());
        }
    }

    @Test
    public void testHasWitness() throws Exception {
        try (Transaction tx = new Transaction(hexToBytes(LEGACY_TX_HEX))) {
            assertFalse(tx.hasWitness());
        }

        try (Transaction tx = new Transaction(hexToBytes(SEGWIT_TX_HEX))) {
            assertTrue(tx.hasWitness());
        }
    }

    @Test
    public void testWtxidWithoutWitnessEqualsTxid() throws Exception {
        try (Transaction tx = new Transaction(hexToBytes(LEGACY_TX_HEX))) {
            assertArrayEquals(tx.getTxid().toBytes(), tx.getWtxid().toBytes());
        }
    }

    @Test
    public void testWtxidWithWitness() throws Exception {
        try (Transaction tx = new Transaction(hexToBytes(SEGWIT_TX_HEX))) {
            byte[] wtxid = tx.getWtxid().toBytes();
            assertEquals(32, wtxid.length);
            assertArrayEquals(hexToBytes(SEGWIT_WTXID_HEX), wtxid);
            assertFalse(java.util.Arrays.equals(tx.getTxid().toBytes(), wtxid));
        }
    }

    @Test
    public void testWtxidCopyAndEquals() throws Exception {
        Wtxid copied;
        try (Transaction tx = new Transaction(hexToBytes(SEGWIT_TX_HEX))) {
            Wtxid wtxid = tx.getWtxid();
            copied = wtxid.copy();
            assertTrue(wtxid.equals(copied));
            assertEquals(wtxid.hashCode(), copied.hashCode());

            try (Transaction legacy = new Transaction(hexToBytes(LEGACY_TX_HEX))) {
                assertFalse(wtxid.equals(legacy.getWtxid()));
            }
            assertFalse(wtxid.equals(null));
        }

        // The copy owns its memory and outlives the transaction it came from.
        try (copied) {
            assertArrayEquals(hexToBytes(SEGWIT_WTXID_HEX), copied.toBytes());
        }
        assertThrows(IllegalStateException.class, copied::toBytes);
    }
}
