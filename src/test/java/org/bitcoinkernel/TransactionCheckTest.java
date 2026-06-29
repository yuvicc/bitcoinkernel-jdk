package org.bitcoinkernel;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.bitcoinkernel.Transactions.*;

public class TransactionCheckTest {

    // Minimal valid coinbase transaction:
    //   version=1, 1 input (null outpoint/coinbase), 3-byte coinbase script, 1 output, locktime=0
    private static final byte[] VALID_COINBASE_TX = {
        // version (LE int32 = 1)
        0x01, 0x00, 0x00, 0x00,
        // vin count
        0x01,
        // outpoint txid (32 zero bytes = coinbase indicator)
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        // outpoint index (0xffffffff = coinbase)
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        // scriptSig length (3 bytes; coinbase requires 2-100)
        0x03,
        // scriptSig data
        (byte) 0xab, (byte) 0xab, (byte) 0xab,
        // sequence
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        // vout count
        0x01,
        // value (330 satoshis, LE int64)
        0x4e, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        // scriptPubKey length (0 = empty)
        0x00,
        // locktime
        0x00, 0x00, 0x00, 0x00
    };

    // Coinbase transaction with a 1-byte script — invalid because coinbase script must be 2-100 bytes.
    private static final byte[] INVALID_COINBASE_TX = {
        // version
        0x01, 0x00, 0x00, 0x00,
        // vin count
        0x01,
        // outpoint txid (null, 32 bytes)
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        // outpoint index (coinbase)
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        // scriptSig length (1 byte — too short for coinbase!)
        0x01,
        // scriptSig data
        (byte) 0xab,
        // sequence
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        // vout count
        0x01,
        // value (330 satoshis)
        0x4e, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        // scriptPubKey length (0)
        0x00,
        // locktime
        0x00, 0x00, 0x00, 0x00
    };

    @Test
    public void testCheckValidTransaction() {
        try (Transaction tx = new Transaction(VALID_COINBASE_TX);
             TxValidationState state = new TxValidationState()) {

            boolean result = tx.check(state);

            assertTrue(result, "Valid coinbase transaction check should succeed: " + state.getDescription());
            assertTrue(state.isValid());
            assertEquals(Blocks.ValidationMode.VALID, state.getValidationMode());
        }
    }

    @Test
    public void testCheckInvalidTransaction() {
        try (Transaction tx = new Transaction(INVALID_COINBASE_TX);
             TxValidationState state = new TxValidationState()) {

            boolean result = tx.check(state);

            assertFalse(result, "Coinbase with 1-byte script should fail check");
            assertTrue(state.isInvalid());
            assertEquals(Blocks.ValidationMode.INVALID, state.getValidationMode());
            assertEquals(TxValidationState.TxValidationResult.CONSENSUS, state.getTxValidationResult());
        }
    }

    @Test
    public void testTxValidationStateDescription() {
        try (Transaction tx = new Transaction(VALID_COINBASE_TX);
             TxValidationState state = new TxValidationState()) {

            tx.check(state);
            String description = state.getDescription();
            assertNotNull(description);
            assertTrue(description.contains("valid") || description.contains("Valid"));
        }
    }

    @Test
    public void testTxValidationStateToString() {
        try (TxValidationState state = new TxValidationState()) {
            String str = state.toString();
            assertNotNull(str);
            assertTrue(str.contains("TxValidationState"));
        }
    }
}
