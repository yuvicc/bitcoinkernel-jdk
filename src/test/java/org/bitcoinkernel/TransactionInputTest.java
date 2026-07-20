package org.bitcoinkernel;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.bitcoinkernel.Transactions.*;

public class TransactionInputTest {
    static {
        try {
            System.loadLibrary("bitcoinkernel");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Failed to load libbitcoinkernel in test: " + e.getMessage());
        }
    }

    // Legacy transaction with two P2PKH inputs: signature data lives in the scriptSig,
    // the witness stack is empty.
    private static final String LEGACY_TX_HEX =
        "020000000248c03e66fd371c7033196ce24298628e59ebefa00363026044e0f35e0325a65d000000006a4730440220048934"
        + "32347f39beaa280e99da595681ddb20fc45010176897e6e055d716dbfa022040a9e46648a5d10c33ef7cee5e6cf4b56bd51"
        + "3eae3ae044f0039824b02d0f44c012102982331a52822fd9b62e9b5d120da1d248558fac3da3a3c51cd7d9c8ad3da760efe"
        + "ffffffb856678c6e4c3c84e39e2ca818807049d6fba274b42af3c6d3f9d4b6513212d2000000006a473044022068bcedc7f"
        + "e39c9f21ad318df2c2da62c2dc9522a89c28c8420ff9d03d2e6bf7b0220132afd752754e5cb1ea2fd0ed6a38ec666781e34"
        + "b0e93dc9a08f2457842cf5660121033aeb9c079ea3e08ea03556182ab520ce5c22e6b0cb95cee6435ee17144d860cdfefff"
        + "fff0260d50b00000000001976a914363cc8d55ea8d0500de728ef6d63804ddddbdc9888ac67040f00000000001976a914c3"
        + "03bdc5064bf9c9a8b507b5496bd0987285707988ac6acb0700";

    private static final String INPUT_0_SCRIPT_SIG_HEX =
        "473044022004893432347f39beaa280e99da595681ddb20fc45010176897e6e055d716dbfa022040a9e46648a5d10c33ef7ce"
        + "e5e6cf4b56bd513eae3ae044f0039824b02d0f44c012102982331a52822fd9b62e9b5d120da1d248558fac3da3a3c51cd7d"
        + "9c8ad3da760e";

    private static final String INPUT_1_SCRIPT_SIG_HEX =
        "473044022068bcedc7fe39c9f21ad318df2c2da62c2dc9522a89c28c8420ff9d03d2e6bf7b0220132afd752754e5cb1ea2fd0"
        + "ed6a38ec666781e34b0e93dc9a08f2457842cf5660121033aeb9c079ea3e08ea03556182ab520ce5c22e6b0cb95cee6435e"
        + "e17144d860cd";

    // Transaction with a single P2WSH input: witness stack holds OP_0, two signatures and the
    // redeem script (0, 71, 71 and 105 bytes), and the scriptSig is empty.
    private static final String SEGWIT_TX_HEX =
        "010000000001011f97548fbbe7a0db7588a66e18d803d0089315aa7d4cc28360b6ec50ef36718a0100000000ffffffff02df1"
        + "776000000000017a9146c002a686959067f4866b8fb493ad7970290ab728757d29f0000000000220020701a8d401c84fb13"
        + "e6baf169d59684e17abd9fa216c8cc5b9fc63d622ff8c58d04004730440220565d170eed95ff95027a69b313758450ba84a"
        + "01224e1f7f130dda46e94d13f8602207bdd20e307f062594022f12ed5017bbf4a055a06aea91c10110a0e3bb23117fc0147"
        + "30440220647d2dc5b15f60bc37dc42618a370b2a1490293f9e5c8464f53ec4fe1dfe067302203598773895b4b16d37485cb"
        + "e21b337f4e4b650739880098c592553add7dd4355016952210375e00eb72e29da82b89367947f29ef34afb75e8654f6ea36"
        + "8e0acdfd92976b7c2103a1b26313f430c4b15bb1fdce663207659d8cac749a0e53d70eff01874496feff2103c96d495bfdd"
        + "5ba4145e3e046fee45e84a8a48ad05bd8dbb395c011a32cf9f88053ae00000000";

    private static final String WITNESS_ITEM_1_HEX =
        "30440220565d170eed95ff95027a69b313758450ba84a01224e1f7f130dda46e94d13f8602207bdd20e307f062594022f12ed"
        + "5017bbf4a055a06aea91c10110a0e3bb23117fc01";

    private static final String WITNESS_ITEM_2_HEX =
        "30440220647d2dc5b15f60bc37dc42618a370b2a1490293f9e5c8464f53ec4fe1dfe067302203598773895b4b16d37485cbe2"
        + "1b337f4e4b650739880098c592553add7dd435501";

    private static final String WITNESS_ITEM_3_HEX =
        "52210375e00eb72e29da82b89367947f29ef34afb75e8654f6ea368e0acdfd92976b7c2103a1b26313f430c4b15bb1fdce663"
        + "207659d8cac749a0e53d70eff01874496feff2103c96d495bfdd5ba4145e3e046fee45e84a8a48ad05bd8dbb395c011a32c"
        + "f9f88053ae";

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
    public void testGetScriptSig() throws Exception {
        try (Transaction tx = new Transaction(hexToBytes(LEGACY_TX_HEX))) {
            TransactionInput input0 = tx.getInput(0);
            TransactionInput input1 = tx.getInput(1);

            assertArrayEquals(hexToBytes(INPUT_0_SCRIPT_SIG_HEX), input0.getScriptSig());
            assertArrayEquals(hexToBytes(INPUT_1_SCRIPT_SIG_HEX), input1.getScriptSig());
        }
    }

    @Test
    public void testGetSequence() throws Exception {
        try (Transaction tx = new Transaction(hexToBytes(LEGACY_TX_HEX))) {
            // Both inputs signal RBF with sequence 0xfffffffe, which must not come back negative.
            assertEquals(0xfffffffeL, tx.getInput(0).getSequence());
            assertEquals(0xfffffffeL, tx.getInput(1).getSequence());
        }

        try (Transaction tx = new Transaction(hexToBytes(SEGWIT_TX_HEX))) {
            assertEquals(0xffffffffL, tx.getInput(0).getSequence());
        }
    }

    @Test
    public void testEmptyScriptSigForSegwitInput() throws Exception {
        try (Transaction tx = new Transaction(hexToBytes(SEGWIT_TX_HEX))) {
            assertEquals(0, tx.getInput(0).getScriptSig().length);
        }
    }

    @Test
    public void testEmptyWitnessStackForLegacyInput() throws Exception {
        try (Transaction tx = new Transaction(hexToBytes(LEGACY_TX_HEX));
             WitnessStack witnessStack = tx.getInput(0).getWitnessStack()) {

            assertEquals(0, witnessStack.countItems());
            assertFalse(witnessStack.iterator().hasNext());
        }
    }

    @Test
    public void testGetWitnessStackItems() throws Exception {
        try (Transaction tx = new Transaction(hexToBytes(SEGWIT_TX_HEX));
             WitnessStack witnessStack = tx.getInput(0).getWitnessStack()) {

            assertEquals(4, witnessStack.countItems());
            assertEquals(0, witnessStack.getItem(0).length);
            assertArrayEquals(hexToBytes(WITNESS_ITEM_1_HEX), witnessStack.getItem(1));
            assertArrayEquals(hexToBytes(WITNESS_ITEM_2_HEX), witnessStack.getItem(2));
            assertArrayEquals(hexToBytes(WITNESS_ITEM_3_HEX), witnessStack.getItem(3));

            assertThrows(IndexOutOfBoundsException.class, () -> witnessStack.getItem(4));
            assertThrows(IndexOutOfBoundsException.class, () -> witnessStack.getItem(-1));
        }
    }

    @Test
    public void testWitnessStackIsIterable() throws Exception {
        try (Transaction tx = new Transaction(hexToBytes(SEGWIT_TX_HEX));
             WitnessStack witnessStack = tx.getInput(0).getWitnessStack()) {

            List<byte[]> items = new ArrayList<>();
            for (byte[] item : witnessStack) {
                items.add(item);
            }

            assertEquals(4, items.size());
            for (int i = 0; i < items.size(); i++) {
                assertArrayEquals(witnessStack.getItem(i), items.get(i));
            }
        }
    }

    @Test
    public void testWitnessStackCopyOutlivesTransaction() throws Exception {
        WitnessStack copied;
        try (Transaction tx = new Transaction(hexToBytes(SEGWIT_TX_HEX));
             WitnessStack witnessStack = tx.getInput(0).getWitnessStack()) {
            copied = witnessStack.copy();
        }

        try (copied) {
            assertEquals(4, copied.countItems());
            assertArrayEquals(hexToBytes(WITNESS_ITEM_3_HEX), copied.getItem(3));
        }

        assertThrows(IllegalStateException.class, copied::countItems);
    }
}
