/*
 * jtminer Java mining software for the Thought Network
 *
 * Copyright (c) 2018 - 2019, Thought Network LLC
 *
 * Contains code from bitcoinj:
 *   Copyright 2011 Google Inc.
 *   Copyright 2014 Andreas Schildbach
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package org.bitcoinj.crypto.cuckoo.data;

import java.util.ArrayList;
import java.util.List;

import org.bitcoinj.crypto.cuckoo.algo.SHA256d;

//The Merkle root is based on a tree of hashes calculated from the transactions:
//
//     root
//      / \
//   A      B
//  / \    / \
// t1 t2 t3 t4
//
// The tree is represented as a list: t1,t2,t3,t4,A,B,root where each
// entry is a hash.
//
// The hashing algorithm is double SHA-256. The leaves are a hash of the serialized contents of the transaction.
// The interior nodes are hashes of the concenation of the two child hashes.
//
// This structure allows the creation of proof that a transaction was included into a block without having to
// provide the full block contents. Instead, you can provide only a Merkle branch. For example to prove tx2 was
// in a block you can just provide tx2, the hash(tx1) and B. Now the other party has everything they need to
// derive the root, which can be checked against the block header. These proofs aren't used right now but
// will be helpful later when we want to download partial block contents.
//
// Note that if the number of transactions is not even the last tx is repeated to make it so (see
// tx3 above). A tree with 5 transactions would look like this:
//
//         root
//        /     \
//       1        5
//     /   \     / \
//    2     3    4  4
//  / \   / \   / \
// t1 t2 t3 t4 t5 t5
public class MerkleTree
{
    private SHA256d hasher;
    ArrayList<byte[]> tree = new ArrayList<>();

    public MerkleTree(CoinbaseTransaction cbtx, List<TransactionImpl> transactions)
    {
        hasher = new SHA256d(32);
        byte[] data = new byte[64];

        hasher.update(cbtx.getHex());
        byte[] cbtxHash = DataUtils.reverseBytes(hasher.doubleDigest());
        tree.add(cbtxHash);

        for (TransactionImpl t : transactions) {
            hasher.update(t.getHex());
            tree.add(DataUtils.reverseBytes(hasher.doubleDigest()));
        }
        int levelOffset = 0; // Offset in the list where the currently processed level starts.
        int treeSize = transactions.size() + 1;
        // Step through each level, stopping when we reach the root (levelSize == 1).
        for (int levelSize = treeSize; levelSize > 1; levelSize = (levelSize + 1) / 2) {
            // For each pair of nodes on that level:
            for (int left = 0; left < levelSize; left += 2) {
                // The right hand node can be the same as the left hand, in the case where we don't have enough
                // transactions.
                int right = Math.min(left + 1, levelSize - 1);
                byte[] leftBytes = DataUtils.reverseBytes(tree.get(levelOffset + left));
                byte[] rightBytes = DataUtils.reverseBytes(tree.get(levelOffset + right));
                System.arraycopy(leftBytes, 0, data, 0, 32);
                System.arraycopy(rightBytes, 0, data, 32, 32);
                hasher.update(data);
                tree.add(DataUtils.reverseBytes(hasher.doubleDigest()));
            }
            // Move to the next level.
            levelOffset += levelSize;
        }
    }


    public byte[] getRoot()
    {
        return tree.get(tree.size() - 1);
    }

}