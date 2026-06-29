package org.bitcoinkernel;

import java.lang.foreign.*;

import static org.bitcoinkernel.KernelTypes.isNull;
import static org.bitcoinkernel.jextract.bitcoinkernel_h.*;
import static org.bitcoinkernel.Transactions.*;

public class Script {

    public enum ScriptVerifyStatus {
        OK(btck_ScriptVerifyStatus_OK()),
        ERROR_INVALID_FLAGS_COMBINATION(btck_ScriptVerifyStatus_ERROR_INVALID_FLAGS_COMBINATION()),
        ERROR_SPENT_OUTPUTS_REQUIRED(btck_ScriptVerifyStatus_ERROR_SPENT_OUTPUTS_REQUIRED());

        private final byte value;

        ScriptVerifyStatus(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }

        public static ScriptVerifyStatus fromByte(byte value) {
            for (ScriptVerifyStatus status : values()) {
                if (status.value == value) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Invalid ScriptVerifyStatus: " + value);
        }
    }

    public static class ScriptVerificationFlags {
        public static final int SCRIPT_VERIFY_NONE = btck_ScriptVerificationFlags_NONE();
        public static final int SCRIPT_VERIFY_P2SH = btck_ScriptVerificationFlags_P2SH();
        public static final int SCRIPT_VERIFY_DERSIG = btck_ScriptVerificationFlags_DERSIG();
        public static final int SCRIPT_VERIFY_NULLDUMMY = btck_ScriptVerificationFlags_NULLDUMMY();
        public static final int SCRIPT_VERIFY_CHECKLOCKTIMEVERIFY = btck_ScriptVerificationFlags_CHECKLOCKTIMEVERIFY();
        public static final int SCRIPT_VERIFY_CHECKSEQUENCEVERIFY = btck_ScriptVerificationFlags_CHECKSEQUENCEVERIFY();
        public static final int SCRIPT_VERIFY_WITNESS = btck_ScriptVerificationFlags_WITNESS();
        public static final int SCRIPT_VERIFY_TAPROOT = btck_ScriptVerificationFlags_TAPROOT();
        public static final int SCRIPT_VERIFY_ALL = btck_ScriptVerificationFlags_ALL();

        private ScriptVerificationFlags() {}
    }

    public static class ScriptPubkey implements AutoCloseable {
        private MemorySegment inner;
        private final Arena arena;
        private final boolean ownsMemory;

        public ScriptPubkey(byte[] scriptPubkey) throws KernelTypes.KernelException {
            this.arena = Arena.ofConfined();
            MemorySegment scriptSegment = arena.allocateFrom(ValueLayout.JAVA_BYTE, scriptPubkey);
            this.inner = btck_script_pubkey_create(scriptSegment, scriptPubkey.length);
            if (inner == MemorySegment.NULL) {
                arena.close();
                throw new KernelTypes.KernelException("Failed to create ScriptPubkey");
            }
            this.ownsMemory = true;
        }

        ScriptPubkey(MemorySegment inner) {
            if (inner == MemorySegment.NULL) {
                throw new IllegalArgumentException("ScriptPubkey cannot be null");
            }
            this.inner = inner;
            this.arena = null;
            this.ownsMemory = false;
        }

        private ScriptPubkey(MemorySegment inner, boolean ownsMemory) {
            this.inner = inner;
            this.arena = null;
            this.ownsMemory = ownsMemory;
        }

        public int verify(long amount, Transaction txTo, TransactionOutput[] spentOutputs,
                int inputIndex, int flags) throws KernelTypes.KernelException {
            checkClosed();
            txTo.checkClosed();

            if (inputIndex < 0 || inputIndex >= txTo.countInputs()) {
                throw new IndexOutOfBoundsException(
                    "Input index " + inputIndex + " is out of bounds for transaction with " +
                    txTo.countInputs() + " inputs");
            }

            try (var arena = Arena.ofConfined()) {
                int numOutputs = (spentOutputs != null) ? spentOutputs.length : 0;
                MemorySegment outputPtrs;

                if (numOutputs > 0) {
                    outputPtrs = arena.allocate(ValueLayout.ADDRESS, numOutputs);
                    for (int i = 0; i < spentOutputs.length; i++) {
                        outputPtrs.setAtIndex(ValueLayout.ADDRESS, i, spentOutputs[i].getInner());
                    }
                } else {
                    outputPtrs = MemorySegment.NULL;
                }

                MemorySegment statusPtr = arena.allocate(ValueLayout.JAVA_BYTE);
                MemorySegment precomputedData = MemorySegment.NULL;
                if (numOutputs > 0) {
                    precomputedData = btck_precomputed_transaction_data_create(txTo.getInner(), outputPtrs, numOutputs);
                }

                int result = btck_script_pubkey_verify(
                    inner,
                    amount,
                    txTo.getInner(),
                    precomputedData,
                    inputIndex,
                    flags,
                    statusPtr
                );

                if (!isNull(precomputedData)) {
                    btck_precomputed_transaction_data_destroy(precomputedData);
                }

                if (result == 0) {
                    byte status = statusPtr.get(ValueLayout.JAVA_BYTE, 0);
                    throw new KernelTypes.KernelException(
                        KernelTypes.KernelException.ScriptVerifyError.fromNative(status));
                }
                return result;
            }
        }

        public byte[] toBytes() {
            checkClosed();
            try (var arena = Arena.ofConfined()) {
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                org.bitcoinkernel.jextract.btck_WriteBytes.Function writer = (bytes, size, userData) -> {
                    try {
                        baos.write(bytes.reinterpret(size).toArray(ValueLayout.JAVA_BYTE));
                        return 0;
                    } catch (Exception e) {
                        return 1;
                    }
                };
                MemorySegment writerSegment = org.bitcoinkernel.jextract.btck_WriteBytes.allocate(writer, arena);
                int result = btck_script_pubkey_to_bytes(inner, writerSegment, MemorySegment.NULL);
                if (result != 0) {
                    throw new RuntimeException("Failed to serialize ScriptPubkey");
                }
                return baos.toByteArray();
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize ScriptPubkey", e);
            }
        }

        public ScriptPubkey copy() {
            checkClosed();
            MemorySegment copied = btck_script_pubkey_copy(inner);
            if (copied == MemorySegment.NULL) {
                throw new RuntimeException("Failed to copy ScriptPubkey");
            }
            return new ScriptPubkey(copied, true);
        }

        private void checkClosed() {
            if (inner == MemorySegment.NULL) {
                throw new IllegalStateException("ScriptPubkey has been closed");
            }
        }

        MemorySegment getInner() {
            return inner;
        }

        @Override
        public void close() {
            if (inner != MemorySegment.NULL && ownsMemory) {
                btck_script_pubkey_destroy(inner);
                inner = MemorySegment.NULL;
            }
            if (arena != null) {
                arena.close();
            }
        }
    }
}
