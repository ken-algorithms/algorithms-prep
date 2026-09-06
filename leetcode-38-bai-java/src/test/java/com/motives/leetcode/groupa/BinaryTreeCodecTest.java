package com.motives.leetcode.groupa;

import static com.motives.leetcode.support.TreeNodes.fromLevelOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.motives.leetcode.common.TreeNode;
import org.junit.jupiter.api.Test;

class BinaryTreeCodecTest {

    private final BinaryTreeCodec codec = new BinaryTreeCodec();

    @Test
    void deserializeReconstructsExactlyWhatWasSerialized() {
        TreeNode root = fromLevelOrder(1, 2, 3, null, null, 4, 5);

        String serialized = codec.serialize(root);
        TreeNode restored = codec.deserialize(serialized);

        assertEquals(serialized, codec.serialize(restored));
    }
}
