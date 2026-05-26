package org.bitcoinkernel;

import org.junit.jupiter.api.Test;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.bitcoinkernel.Blocks.*;
import static org.bitcoinkernel.Chainstate.*;
import static org.bitcoinkernel.KernelTypes.*;

public class BlockCheckTest {
    private static final String BLOCK_DATA_FILE = "tests/block_data.txt";

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }

    private static List<byte[]> readBlockData() throws Exception {
        List<byte[]> blocks = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(BLOCK_DATA_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    blocks.add(hexToBytes(line));
                }
            }
        }
        return blocks;
    }

    @Test
    public void testBlockCheck() throws Exception {
        List<byte[]> blockData = readBlockData();
        assertFalse(blockData.isEmpty(), "Test data should not be empty");

        try (ChainParameters params = new ChainParameters(ChainType.REGTEST)) {
            ConsensusParams consensusParams = params.getConsensusParams();
            assertNotNull(consensusParams);

            for (byte[] rawBlock : blockData) {
                try (Block block = new Block(rawBlock);
                     BlockValidationState state = new BlockValidationState()) {
                    
                    // Run check with POW and MERKLE
                    boolean result = block.check(consensusParams, BlockCheckFlags.ALL, state);
                    
                    assertTrue(result, "Block check should succeed: " + state.getDescription());
                    assertTrue(state.isValid());
                }
            }
        }
    }

    @Test
    public void testBlockCheckInvalid() throws Exception {
        List<byte[]> blockData = readBlockData();
        byte[] rawBlock = blockData.get(0).clone();
        
        // Mutate the block (change a byte in the transactions area)
        rawBlock[100] ^= 0xFF;

        try (ChainParameters params = new ChainParameters(ChainType.REGTEST)) {
            ConsensusParams consensusParams = params.getConsensusParams();
            
            try (Block block = new Block(rawBlock);
                 BlockValidationState state = new BlockValidationState()) {
                
                // Merkle root check should fail
                boolean result = block.check(consensusParams, BlockCheckFlags.MERKLE, state);
                
                assertFalse(result, "Mutated block check should fail");
                assertTrue(state.isInvalid());
                assertEquals(BlockValidationState.BlockValidationResult.MUTATED, state.getBlockValidationResult());
            }
        }
    }
}
