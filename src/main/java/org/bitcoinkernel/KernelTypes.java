package org.bitcoinkernel;

import java.io.ByteArrayOutputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import org.bitcoinkernel.jextract.btck_WriteBytes;

import static org.bitcoinkernel.jextract.bitcoinkernel_h.*;

// Enum definitions and conversions for Bitcoin Kernel
public class KernelTypes {

    static {
        try {
            System.loadLibrary("bitcoinkernel");
        } catch (UnsatisfiedLinkError e) {
            // This can happen if the library is not in the search path.
            // We don't throw here to allow for manual loading or cases where it's already loaded.
        }
    }

    /**
     * Helper method to check if a MemorySegment is null.
     * Handles both MemorySegment.NULL and address == 0 cases.
     */
    public static boolean isNull(MemorySegment segment) {
        return segment == null || segment == MemorySegment.NULL || segment.address() == 0;
    }

    /**
     * A kernel function that serializes data through a btck_WriteBytes callback.
     */
    @FunctionalInterface
    interface ByteSerializer {
        int serialize(MemorySegment writer, MemorySegment userData);
    }

    /**
     * Collects the bytes handed to the btck_WriteBytes callback of a kernel serialization
     * function. The kernel returns whatever the callback returned, so a non-zero result means
     * the write failed.
     */
    static byte[] collectBytes(String description, ByteSerializer serializer) {
        try (Arena arena = Arena.ofConfined()) {
            ByteArrayOutputStream collected = new ByteArrayOutputStream();
            MemorySegment writer = btck_WriteBytes.allocate(
                    (bytes, size, userData) -> {
                        collected.writeBytes(bytes.reinterpret(size).toArray(ValueLayout.JAVA_BYTE));
                        return 0;
                    }, arena);
            if (serializer.serialize(writer, MemorySegment.NULL) != 0) {
                throw new RuntimeException("Failed to serialize " + description);
            }
            return collected.toByteArray();
        }
    }

    // ===== Log Category =====
    public enum LogCategory {
        ALL(btck_LogCategory_ALL()),
        BENCH(btck_LogCategory_BENCH()),
        BLOCKSTORAGE(btck_LogCategory_BLOCKSTORAGE()),
        COINDB(btck_LogCategory_COINDB()),
        LEVELDB(btck_LogCategory_LEVELDB()),
        MEMPOOL(btck_LogCategory_MEMPOOL()),
        PRUNE(btck_LogCategory_PRUNE()),
        RAND(btck_LogCategory_RAND()),
        REINDEX(btck_LogCategory_REINDEX()),
        VALIDATION(btck_LogCategory_VALIDATION()),
        KERNEL(btck_LogCategory_KERNEL());

        private final byte value;

        LogCategory(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }

        public static LogCategory fromByte(byte value) {
            for (LogCategory category : values()) {
                if (category.value == value) {
                    return category;
                }
            }
            throw new IllegalArgumentException("Invalid LogCategory: " + value);
        }
    }

    // ===== Log Level =====
    public enum LogLevel {
        TRACE(btck_LogLevel_TRACE()),
        DEBUG(btck_LogLevel_DEBUG()),
        INFO(btck_LogLevel_INFO());

        private final byte value;

        LogLevel(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }

        public static LogLevel fromByte(byte value) {
            for (LogLevel level : values()) {
                if (level.value == value) {
                    return level;
                }
            }
            throw new IllegalArgumentException("Invalid LogLevel: " + value);
        }
    }

    // ===== Block Check Flags =====
    public static class BlockCheckFlags {
        public static final int BASE = btck_BlockCheckFlags_BASE();
        public static final int POW = btck_BlockCheckFlags_POW();
        public static final int MERKLE = btck_BlockCheckFlags_MERKLE();
        public static final int ALL = btck_BlockCheckFlags_ALL();

        private BlockCheckFlags() {
            // Utility class, prevent instantiation
        }
    }

    // ===== Kernel Exception =====
    public static class KernelException extends Exception {
        public enum ScriptVerifyError {
            OK(btck_ScriptVerifyStatus_OK()),
            INVALID_FLAGS_COMBINATION(btck_ScriptVerifyStatus_ERROR_INVALID_FLAGS_COMBINATION()),
            SPENT_OUTPUTS_REQUIRED(btck_ScriptVerifyStatus_ERROR_SPENT_OUTPUTS_REQUIRED()),
            INVALID(0);

            private final int nativeValue;

            ScriptVerifyError(int nativeValue) {
                this.nativeValue = nativeValue;
            }

            public int getNativeValue() {
                return nativeValue;
            }

            public static ScriptVerifyError fromNative(int status) {
                for (ScriptVerifyError value: values()) {
                    if (value.nativeValue == status) {
                        return value;
                    }
                }
                return INVALID;
            }
        }

        private final ScriptVerifyError scriptVerifyError;

        public KernelException(String message) {
            super(message);
            this.scriptVerifyError = null;
        }

        public KernelException(ScriptVerifyError error) {
            super(error.toString());
            this.scriptVerifyError = error;
        }

        public ScriptVerifyError getScriptVerifyError() {
            return scriptVerifyError;
        }
    }
}
