package org.bitcoinkernel;

import java.lang.foreign.*;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.bitcoinkernel.jextract.bitcoinkernel_h.*;
import static org.bitcoinkernel.KernelData.*;

public class Transactions {

    static {
        try {
            System.loadLibrary("bitcoinkernel");
        } catch (UnsatisfiedLinkError e) {
            // This can happen if the library is not in the search path.
        }
    }

    // ===== Transaction =====
    public static class Transaction implements AutoCloseable {
        private MemorySegment inner;
        private final Arena arena;
        private final boolean ownsMemory;

        Transaction(MemorySegment inner) {
            this(inner, false);
        }

        Transaction(MemorySegment inner, boolean ownsMemory) {
            if (inner == MemorySegment.NULL) {
                throw new IllegalStateException("Transaction Object cannot be null");
            }
            this.inner = inner;
            this.arena = null;
            this.ownsMemory = ownsMemory;
        }

        public Transaction(byte[] rawTransaction) throws IllegalArgumentException {
            if (rawTransaction == null || rawTransaction.length == 0) {
                throw new IllegalArgumentException("Raw transaction cannot be null or empty");
            }
            this.arena = Arena.ofConfined();
            MemorySegment txSegment = arena.allocateFrom(ValueLayout.JAVA_BYTE, rawTransaction);
            this.inner = btck_transaction_create(txSegment, rawTransaction.length);
            if (this.inner == MemorySegment.NULL) {
                arena.close();
                throw new IllegalArgumentException("Failed to create Transaction from raw data");
            }
            this.ownsMemory = true;
        }

        public Transaction copy() {
            checkClosed();
            MemorySegment copied = btck_transaction_copy(inner);
            if (copied == MemorySegment.NULL) {
                throw new RuntimeException("Failed to copy Transaction");
            }
            return new Transaction(copied, true);
        }

        public long countInputs() {
            return btck_transaction_count_inputs(inner);
        }

        public long countOutputs() {
            return btck_transaction_count_outputs(inner);
        }

        public TransactionInput getInput(long index) {
            if (index < 0 || index >= countInputs()) {
                throw new IndexOutOfBoundsException("Index out of bounds: " + index);
            }
            MemorySegment inputStr = btck_transaction_get_input_at(inner, index);
            return new TransactionInput(inputStr);
        }

        public TransactionOutput getOutput(long index) {
            if (index < 0 || index >= countOutputs()) {
                throw new IndexOutOfBoundsException("Output index out of bounds: " + index);
            }
            MemorySegment outputPtr = btck_transaction_get_output_at(inner, index);
            return new TransactionOutput(outputPtr);
        }

        public Txid getTxid() {
            MemorySegment txidPtr = btck_transaction_get_txid(inner);
            return new Txid(txidPtr);
        }

        public boolean check(TxValidationState state) {
            checkClosed();
            return btck_transaction_check(inner, state.getInner()) != 0;
        }

        void checkClosed() {
            if (inner == MemorySegment.NULL) {
                throw new IllegalStateException("Transaction has been closed");
            }
        }

        MemorySegment getInner() {
            return inner;
        }

        @Override
        public void close() {
            if (inner != MemorySegment.NULL && ownsMemory) {
                btck_transaction_destroy(inner);
                inner = MemorySegment.NULL;
            }
            if (arena != null) {
                arena.close();
            }
        }
    }

    // ===== Transaction Input =====
    public static class TransactionInput implements AutoCloseable {
        private MemorySegment inner;
        private final boolean ownsMemory;

        TransactionInput(MemorySegment inner) {
            if (inner == MemorySegment.NULL) {
                throw new IllegalStateException("Transaction Input object cannot be null");
            }
            this.inner = inner;
            this.ownsMemory = false;
        }

        private TransactionInput(MemorySegment inner, boolean ownsMemory) {
            this.inner = inner;
            this.ownsMemory = ownsMemory;
        }

        public TransactionInput copy() {
            checkClosed();
            MemorySegment copied = btck_transaction_input_copy(inner);
            if (copied == MemorySegment.NULL) {
                throw new RuntimeException("Failed to copy TransactionInput");
            }
            return new TransactionInput(copied, true);
        }

        public TransactionOutPoint getOutPoint() {
            checkClosed();
            MemorySegment outPointPtr = btck_transaction_input_get_out_point(inner);
            return new TransactionOutPoint(outPointPtr);
        }

        private void checkClosed() {
            if (inner == MemorySegment.NULL) {
                throw new IllegalStateException("TransactionInput has been closed");
            }
        }

        MemorySegment getInner() {
            return inner;
        }

        @Override
        public void close() throws Exception {
            if (inner != MemorySegment.NULL && ownsMemory) {
                btck_transaction_input_destroy(inner);
                inner = MemorySegment.NULL;
            }
        }
    }

    // ===== Transaction OutPoint =====
    public static class TransactionOutPoint implements AutoCloseable {
        private MemorySegment inner;
        private final boolean ownsMemory;

        TransactionOutPoint(MemorySegment inner) {
            if (inner == MemorySegment.NULL) {
                throw new IllegalArgumentException("TransactionOutPoint cannot be null");
            }
            this.inner = inner;
            this.ownsMemory = false;
        }

        private TransactionOutPoint(MemorySegment inner, boolean ownsMemory) {
            this.inner = inner;
            this.ownsMemory = ownsMemory;
        }

        public TransactionOutPoint copy() {
            checkClosed();
            MemorySegment copied = btck_transaction_out_point_copy(inner);
            if (copied == MemorySegment.NULL) {
                throw new RuntimeException("Failed to copy TransactionOutPoint");
            }
            return new TransactionOutPoint(copied, true);
        }

        public long getIndex() {
            checkClosed();
            return Integer.toUnsignedLong(btck_transaction_out_point_get_index(inner));
        }

        public Txid getTxid() {
            checkClosed();
            MemorySegment txidPtr = btck_transaction_out_point_get_txid(inner);
            return new Txid(txidPtr);
        }

        private void checkClosed() {
            if (inner == MemorySegment.NULL) {
                throw new IllegalStateException("TransactionOutPoint has been closed");
            }
        }

        MemorySegment getInner() {
            return inner;
        }

        @Override
        public void close() throws Exception {
            if (inner != MemorySegment.NULL && ownsMemory) {
                btck_transaction_out_point_destroy(inner);
                inner = MemorySegment.NULL;
            }
        }
    }

    // ===== Transaction Output =====
    public static class TransactionOutput implements AutoCloseable {
        private MemorySegment inner;
        private final boolean ownsMemory;

        TransactionOutput(MemorySegment inner) {
            if (inner == MemorySegment.NULL) {
                throw new IllegalArgumentException("TransactionOutput cannot be null");
            }
            this.inner = inner;
            this.ownsMemory = false;
        }

        private TransactionOutput(MemorySegment inner, boolean ownsMemory) {
            this.inner = inner;
            this.ownsMemory = ownsMemory;
        }

        public TransactionOutput(ScriptPubkey scriptPubkey, long amount) {
            if (scriptPubkey == null) {
                throw new IllegalArgumentException("ScriptPubkey cannot be null");
            }
            this.inner = btck_transaction_output_create(scriptPubkey.getInner(), amount);
            if (this.inner == MemorySegment.NULL) {
                throw new IllegalArgumentException("Failed to create TransactionOutput");
            }
            this.ownsMemory = true;
        }

        public TransactionOutput copy() {
            checkClosed();
            MemorySegment copied = btck_transaction_output_copy(inner);
            if (copied == MemorySegment.NULL) {
                throw new RuntimeException("Failed to copy TransactionOutput");
            }
            return new TransactionOutput(copied, true);
        }

        public long getAmount() {
            checkClosed();
            return btck_transaction_output_get_amount(inner);
        }

        public ScriptPubkey getScriptPubKey() {
            checkClosed();
            MemorySegment scriptPtr = btck_transaction_output_get_script_pubkey(inner);
            return new ScriptPubkey(scriptPtr);
        }

        private void checkClosed() {
            if (inner == MemorySegment.NULL) {
                throw new IllegalStateException("TransactionOutput has been closed");
            }
        }

        MemorySegment getInner() {
            return inner;
        }

        @Override
        public void close() {
            if (inner != MemorySegment.NULL && ownsMemory) {
                btck_transaction_output_destroy(inner);
                inner = MemorySegment.NULL;
            }
        }
    }

    // ===== Coin =====
    public static class Coin implements AutoCloseable {
        private MemorySegment inner;
        private final boolean ownsMemory;

        Coin(MemorySegment inner) {
            if (inner == MemorySegment.NULL) {
                throw new IllegalArgumentException("Coin cannot be null");
            }
            this.inner = inner;
            this.ownsMemory = false;
        }

        private Coin(MemorySegment inner, boolean ownsMemory) {
            this.inner = inner;
            this.ownsMemory = ownsMemory;
        }

        public long getConfirmationHeight() {
            checkClosed();
            return Integer.toUnsignedLong(btck_coin_confirmation_height(inner));
        }

        public boolean isCoinbase() {
            checkClosed();
            return btck_coin_is_coinbase(inner) != 0;
        }

        public TransactionOutput getOutput() {
            checkClosed();
            MemorySegment outputPtr = btck_coin_get_output(inner);
            return new TransactionOutput(outputPtr);
        }

        public Coin copy() {
            checkClosed();
            MemorySegment copied = btck_coin_copy(inner);
            if (copied == MemorySegment.NULL) {
                throw new RuntimeException("Failed to copy coin");
            }
            return new Coin(copied, true);
        }

        private void checkClosed() {
            if (inner == MemorySegment.NULL) {
                throw new IllegalStateException("Coin object has been closed");
            }
        }

        MemorySegment getInner() {
            return inner;
        }

        @Override
        public void close() throws Exception {
            if (inner != MemorySegment.NULL && ownsMemory) {
                btck_coin_destroy(inner);
                inner = MemorySegment.NULL;
            }
        }
    }

    // ===== Txid =====
    public static class Txid implements AutoCloseable {
        private MemorySegment inner;
        private final boolean ownsMemory;

        Txid(MemorySegment inner) {
            if (inner == MemorySegment.NULL) {
                throw new IllegalArgumentException("Txid cannot be null");
            }
            this.inner = inner;
            this.ownsMemory = false;
        }

        private Txid(MemorySegment inner, boolean ownsMemory) {
            this.inner = inner;
            this.ownsMemory = ownsMemory;
        }

        public Txid copy() {
            checkClosed();
            MemorySegment copied = btck_txid_copy(inner);
            if (copied == MemorySegment.NULL) {
                throw new RuntimeException("Failed to copy Txid");
            }
            return new Txid(copied, true);
        }

        public byte[] toBytes() {
            checkClosed();
            try (var arena = Arena.ofConfined()) {
                MemorySegment output = arena.allocate(32);
                btck_txid_to_bytes(inner, output);
                return output.toArray(ValueLayout.JAVA_BYTE);
            }
        }

        public boolean equals(Txid other) {
            checkClosed();
            if (other == null) {
                return false;
            }
            other.checkClosed();
            return btck_txid_equals(inner, other.getInner()) != 0;
        }

        @Override
        public int hashCode() {
            byte[] bytes = toBytes();
            int result = 1;
            for (byte b : bytes) {
                result = 31 * result + b;
            }
            return result;
        }

        private void checkClosed() {
            if (inner == MemorySegment.NULL) {
                throw new IllegalStateException("Txid has been closed");
            }
        }

        MemorySegment getInner() {
            return inner;
        }

        @Override
        public void close() throws Exception {
            if (inner != MemorySegment.NULL && ownsMemory) {
                btck_txid_destroy(inner);
                inner = MemorySegment.NULL;
            }
        }
    }

    // ===== Tx Validation State =====
    public static class TxValidationState implements AutoCloseable {
        private MemorySegment inner;

        public enum TxValidationResult {
            UNSET(0),
            CONSENSUS(1),
            INPUTS_NOT_STANDARD(2),
            NOT_STANDARD(3),
            MISSING_INPUTS(4),
            PREMATURE_SPEND(5),
            WITNESS_MUTATED(6),
            WITNESS_STRIPPED(7),
            CONFLICT(8),
            MEMPOOL_POLICY(9),
            NO_MEMPOOL(10),
            RECONSIDERABLE(11),
            UNKNOWN(12);

            private final int value;

            TxValidationResult(int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }

            public static TxValidationResult fromInt(int value) {
                for (TxValidationResult result : values()) {
                    if (result.value == value) {
                        return result;
                    }
                }
                throw new IllegalArgumentException("Invalid TxValidationResult: " + value);
            }
        }

        public TxValidationState() {
            this.inner = btck_tx_validation_state_create.makeInvoker().apply();
            if (this.inner == MemorySegment.NULL) {
                throw new RuntimeException("Failed to create TxValidationState");
            }
        }

        public Blocks.ValidationMode getValidationMode() {
            byte mode = btck_tx_validation_state_get_validation_mode(inner);
            return Blocks.ValidationMode.fromByte(mode);
        }

        public TxValidationResult getTxValidationResult() {
            int result = btck_tx_validation_state_get_tx_validation_result(inner);
            return TxValidationResult.fromInt(result);
        }

        public boolean isValid() {
            return getValidationMode() == Blocks.ValidationMode.VALID;
        }

        public boolean isInvalid() {
            return getValidationMode() == Blocks.ValidationMode.INVALID;
        }

        public boolean hasError() {
            return getValidationMode() == Blocks.ValidationMode.INTERNAL_ERROR;
        }

        public String getDescription() {
            Blocks.ValidationMode mode = getValidationMode();
            if (mode == Blocks.ValidationMode.VALID) {
                return "Transaction is valid";
            } else if (mode == Blocks.ValidationMode.INVALID) {
                return "Transaction is invalid: " + getTxValidationResult().name();
            } else {
                return "Internal error during transaction validation";
            }
        }

        @Override
        public String toString() {
            return String.format("TxValidationState{mode=%s, result=%s}",
                    getValidationMode(), getTxValidationResult());
        }

        @Override
        public void close() {
            if (inner != MemorySegment.NULL) {
                btck_tx_validation_state_destroy(inner);
                inner = MemorySegment.NULL;
            }
        }

        MemorySegment getInner() {
            return inner;
        }
    }

    // ===== Transaction Spent Outputs
    public static class TransactionSpentOutputs implements Iterable<Coin> {
        private final MemorySegment inner;
        private final boolean ownsMemory;

        TransactionSpentOutputs(MemorySegment inner) {
            if (inner == MemorySegment.NULL) {
                throw new IllegalArgumentException("Transaction Outputs cannot be null");
            }
            this.inner = inner;
            this.ownsMemory = false;
        }

        private TransactionSpentOutputs(MemorySegment inner, boolean ownsMemory) {
            this.inner = inner;
            this.ownsMemory = ownsMemory;
        }

        public long count() {
            return btck_transaction_spent_outputs_count(inner);
        }

        public Coin getCoin(long index) {
            if (index < 0 || index >= count()) {
                throw new IndexOutOfBoundsException("Coin index out of bounds: " + index);
            }
            MemorySegment coinPtr = btck_transaction_spent_outputs_get_coin_at(inner, index);
            return new Coin(coinPtr);
        }

        public TransactionSpentOutputs copy() {
            MemorySegment copied = btck_transaction_spent_outputs_copy(inner);
            if (copied == MemorySegment.NULL) {
                throw new RuntimeException("Failed to copy coin object");
            }
            return new TransactionSpentOutputs(inner, true);
        }

        @Override
        public Iterator<Coin> iterator() {
            return new Iterator<>() {
                private long currentIndex = 0;
                private final long size = count();

                @Override
                public boolean hasNext() {
                    return currentIndex < size;
                }

                @Override
                public Coin next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return getCoin(currentIndex++);
                }
            };
        }

        MemorySegment getInner() {
            return inner;
        }

        public void close() {
            if (inner != MemorySegment.NULL) {
                btck_transaction_spent_outputs_destroy(inner);
            }
        }
    }
}
