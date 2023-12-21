/*
 * Copyright 2011 Noa Resare
 * Copyright 2014 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bitcoinj.core;

import org.bitcoinj.params.MainNetParams;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.bitcoinj.core.Utils.HEX;
import static org.junit.Assert.*;

public class BitcoinSerializerTest {
    private static final NetworkParameters MAINNET = MainNetParams.get();
    private static final byte[] ADDRESS_MESSAGE_BYTES = HEX.decode("59472ee46164647200000000000000001f000000" +
            "68a4a12901e0c85e65050000000000000000000000000000000000ffff89dc3f1a297a");

    private static final byte[] TRANSACTION_MESSAGE_BYTES = HEX.decode(
            "59472ee4"+ //magic thought testnet
                    "747800000000000000000000" +//tx ?
                    "74010000"+ //size 372
                    "c0031e65" + //checksum
                    "02000000027476869ab12f0378f432b644dd5cde09662417a4fd06cc49fd6a7c24a06861c5000000006a47304402200ea120f208654cd0cf45b0e80932c9c8aced4f471669559814a497b4258a4ffb0220356163345017f84171e591c5a63d5f592c7e4aba196d90bb52a27183762abaf6012103bc81bad61df59b390e0445634a8868e93c9dd511957b3dd8e71392796a345436feffffffd1f6621cac885eff5d68acd232eead2e467d16501ae74db2cfb024a3b26dd9e1000000006a473044022062a40009fb726b7427fe5592609e0b3d1aa676d8b6bf872dbdd396af74c570b502202d63144cd9c05edd0103c739484d4547ec31a3c5a09cb7508f44bb53309656a80121033cd5c2c984e240686b445ff3e554f59989d1a6222d963c7fa6bc0fc3acde84e0feffffff02a618e4d3010000001976a914fff268b915840523c08a39a15ba1261147f7b78488ac807ee5d3010000001976a914d74beecc7f4e553c30cf617de5c3b5927277f92b88acfa421c00");

    @Test
    public void testAddr() throws Exception {
        MessageSerializer serializer = MAINNET.getDefaultSerializer();
        // the actual data from https://en.bitcoin.it/wiki/Protocol_specification#addr
        AddressMessage addressMessage = (AddressMessage) serializer.deserialize(ByteBuffer.wrap(ADDRESS_MESSAGE_BYTES));
        assertEquals(1, addressMessage.getAddresses().size());
        PeerAddress peerAddress = addressMessage.getAddresses().get(0);
        assertEquals(10618, peerAddress.getPort());
        assertEquals("137.220.63.26", peerAddress.getAddr().getHostAddress());
        ByteArrayOutputStream bos = new ByteArrayOutputStream(ADDRESS_MESSAGE_BYTES.length);
        serializer.serialize(addressMessage, bos);

        assertEquals(31, addressMessage.getMessageSize());
        addressMessage.addAddress(new PeerAddress(MAINNET, InetAddress.getLocalHost()));
        assertEquals(61, addressMessage.getMessageSize());
        addressMessage.removeAddress(0);
        assertEquals(31, addressMessage.getMessageSize());

        //this wont be true due to dynamic timestamps.
        //assertTrue(LazyParseByteCacheTest.arrayContains(bos.toByteArray(), addrMessage));
    }

    @Test
    public void testCachedParsing() throws Exception {
        MessageSerializer serializer = MAINNET.getSerializer(true);
        
        // first try writing to a fields to ensure uncaching and children are not affected
        Transaction transaction = (Transaction) serializer.deserialize(ByteBuffer.wrap(TRANSACTION_MESSAGE_BYTES));
        assertNotNull(transaction);
        assertTrue(transaction.isCached());

        transaction.setLockTime(1);
        // parent should have been uncached
        assertFalse(transaction.isCached());
        // child should remain cached.
        assertTrue(transaction.getInputs().get(0).isCached());

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        serializer.serialize(transaction, bos);
        assertFalse(Arrays.equals(TRANSACTION_MESSAGE_BYTES, bos.toByteArray()));

        // now try writing to a child to ensure uncaching is propagated up to parent but not to siblings
        transaction = (Transaction) serializer.deserialize(ByteBuffer.wrap(TRANSACTION_MESSAGE_BYTES));
        assertNotNull(transaction);
        assertTrue(transaction.isCached());

        transaction.getInputs().get(0).setSequenceNumber(1);
        // parent should have been uncached
        assertFalse(transaction.isCached());
        // so should child
        assertFalse(transaction.getInputs().get(0).isCached());

        bos = new ByteArrayOutputStream();
        serializer.serialize(transaction, bos);
        assertFalse(Arrays.equals(TRANSACTION_MESSAGE_BYTES, bos.toByteArray()));

        // deserialize/reserialize to check for equals.
        transaction = (Transaction) serializer.deserialize(ByteBuffer.wrap(TRANSACTION_MESSAGE_BYTES));
        assertNotNull(transaction);
        assertTrue(transaction.isCached());
        bos = new ByteArrayOutputStream();
        serializer.serialize(transaction, bos);
        assertTrue(Arrays.equals(TRANSACTION_MESSAGE_BYTES, bos.toByteArray()));

        // deserialize/reserialize to check for equals.  Set a field to it's existing value to trigger uncache
        transaction = (Transaction) serializer.deserialize(ByteBuffer.wrap(TRANSACTION_MESSAGE_BYTES));
        assertNotNull(transaction);
        assertTrue(transaction.isCached());

        transaction.getInputs().get(0).setSequenceNumber(transaction.getInputs().get(0).getSequenceNumber());

        bos = new ByteArrayOutputStream();
        serializer.serialize(transaction, bos);
        assertTrue(Arrays.equals(TRANSACTION_MESSAGE_BYTES, bos.toByteArray()));
    }

    /**
     * Get 1 header of the block number 1 (the first one is 0) in the chain
     */
    @Test
    public void testHeaders1() throws Exception {
        MessageSerializer serializer = MAINNET.getDefaultSerializer();

        byte[] headersMessageBytes = HEX.decode("59472ee4" + // magicbytes
                "686561646572730000000000" + // "headers" in ASCII
                "52000000" + // length
                "62cea2e2" + // checksum of payload
                // payload
                //
                "01" + // header count
                "010000000000000000000000000000000000000000000000000000000000000000000000d6c2031a679c5e9120f735629cc45a8eab5f5879aace2ee519f350a3bf983a48f238a95affff001d5cb1a37b00"); // header
        HeadersMessage headersMessage = (HeadersMessage) serializer.deserialize(ByteBuffer.wrap(headersMessageBytes));

        // The first block after the genesis
        // http://blockexplorer.com/b/1
        Block block = headersMessage.getBlockHeaders().get(0);
        assertEquals("00000000917e049641189c33d6b1275155e89b7b498b3b4f16d488f60afe513b", block.getHashAsString());
        assertNotNull(block.transactions);
        assertEquals("483a98bfa350f319e52eceaa79585fab8e5ac49c6235f720915e9c671a03c2d6", Utils.HEX.encode(block.getMerkleRoot().getBytes()));

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        serializer.serialize(headersMessage, byteArrayOutputStream);
        byte[] serializedBytes = byteArrayOutputStream.toByteArray();
        assertArrayEquals(headersMessageBytes, serializedBytes);
    }

    /**
     * Get 6 headers of blocks 1-6 in the chain
     */
    @Test
    public void testHeaders2() throws Exception {
        MessageSerializer serializer = MAINNET.getDefaultSerializer();

        byte[] headersMessageBytes1 = HEX.decode("f9beb4d96865616465" +
                "72730000000000e701000085acd4ea06010000006fe28c0ab6f1b372c1a6a246ae63f74f931e" +
                "8365e15a089c68d6190000000000982051fd1e4ba744bbbe680e1fee14677ba1a3c3540bf7b1c" +
                "db606e857233e0e61bc6649ffff001d01e3629900010000004860eb18bf1b1620e37e9490fc8a" +
                "427514416fd75159ab86688e9a8300000000d5fdcc541e25de1c7a5addedf24858b8bb665c9f36" +
                "ef744ee42c316022c90f9bb0bc6649ffff001d08d2bd610001000000bddd99ccfda39da1b108ce1" +
                "a5d70038d0a967bacb68b6b63065f626a0000000044f672226090d85db9a9f2fbfe5f0f9609b387" +
                "af7be5b7fbb7a1767c831c9e995dbe6649ffff001d05e0ed6d00010000004944469562ae1c2c74" +
                "d9a535e00b6f3e40ffbad4f2fda3895501b582000000007a06ea98cd40ba2e3288262b28638cec" +
                "5337c1456aaf5eedc8e9e5a20f062bdf8cc16649ffff001d2bfee0a9000100000085144a84488e" +
                "a88d221c8bd6c059da090e88f8a2c99690ee55dbba4e00000000e11c48fecdd9e72510ca84f023" +
                "370c9a38bf91ac5cae88019bee94d24528526344c36649ffff001d1d03e4770001000000fc33f5" +
                "96f822a0a1951ffdbf2a897b095636ad871707bf5d3162729b00000000379dfb96a5ea8c81700ea4" +
                "ac6b97ae9a9312b2d4301a29580e924ee6761a2520adc46649ffff001d189c4c9700");
        byte[] headersMessageBytes = HEX.decode("bf0c6bbd6865616465" +
                "72730000000000e7010000d8ee9a83"+
                "06"+
                "02000000b67a40f3cd5804437a108f105533739c37e6229bc1adcab385140b59fd0f0000a71c1aade44bf8425bec0deb611c20b16da3442818ef20489ca1e2512be43eef814cdb52f0ff0f1edbf7010000"+
                "02000000434341c0ecf9a2b4eec2644cfadf4d0a07830358aed12d0ed654121dd90700004bdcd337231c40e16ad4d46356dd3369d69acb46f9647106a52c03b0ce973a3b864cdb52f0ff0f1ef7d2010000"+
                "02000000aefe1eef743a873769ccf70ee174b541ef4775886f435c7cce1e57ccaf0b0000386851f9d572dce5b95554329a2f6706b0d4d3dcaaf291735479c8151d3329ec954cdb52f0ff0f1e8f07060000"+
                "02000000ffe08d0dce84f320f81eed925141b231909b2ca0e1d2c67bd956557069020000ae075ab56576f7f01000970ebd7c4b9e21bd90bd51ba62471552ca7db956026e9e4cdb52f0ff0f1e3c27030000"+
                "02000000e4679be8b765214df4923942bd37dccc2c629210a1d96e9925be6ce4fc06000025a0c01db1cfa0db1847bce702e92e0905d814f3a8fb6f04e0060d75765efd17a14cdb52f0ff0f1e2120010000"+
                "02000000a210ba37364085b6482d437071506c0b5e722604ccacfa687f2f15567f090000c125f400644f069983dac9a19d85a6cf65026ce6ca036cb4eadccf567fbedf95aa4cdb52f0ff0f1e079f030000");

        HeadersMessage headersMessage = (HeadersMessage) serializer.deserialize(ByteBuffer.wrap(headersMessageBytes));

        assertEquals(6, headersMessage.getBlockHeaders().size());

        // index 0 block is the number 1 block in the block chain
        // http://blockexplorer.com/b/1
        Block zeroBlock = headersMessage.getBlockHeaders().get(0);
        assertEquals("000007d91d1254d60e2dd1ae580383070a4ddffa4c64c2eeb4a2f9ecc0414343",
                zeroBlock.getHashAsString());
        assertEquals(128987, zeroBlock.getNonce());

        // index 3 block is the number 4 block in the block chain
        // http://blockexplorer.com/b/4
        Block thirdBlock = headersMessage.getBlockHeaders().get(3);
        assertEquals("000006fce46cbe25996ed9a11092622cccdc37bd423992f44d2165b7e89b67e4",
                thirdBlock.getHashAsString());
        assertEquals(206652, thirdBlock.getNonce());

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        serializer.serialize(headersMessage, byteArrayOutputStream);
        byte[] serializedBytes = byteArrayOutputStream.toByteArray();
        assertArrayEquals(headersMessageBytes, serializedBytes);
    }

    @Test(expected = BufferUnderflowException.class)
    public void testBitcoinPacketHeaderTooShort() {
        new BitcoinSerializer.BitcoinPacketHeader(ByteBuffer.wrap(new byte[] { 0 }));
    }

    @Test(expected = ProtocolException.class)
    public void testBitcoinPacketHeaderTooLong() {
        // Message with a Message size which is 1 too big, in little endian format.
        byte[] wrongMessageLength = HEX.decode("000000000000000000000000010000020000000000");
        new BitcoinSerializer.BitcoinPacketHeader(ByteBuffer.wrap(wrongMessageLength));
    }

    @Test(expected = BufferUnderflowException.class)
    public void testSeekPastMagicBytes() {
        // Fail in another way, there is data in the stream but no magic bytes.
        byte[] brokenMessage = HEX.decode("000000");
        MAINNET.getDefaultSerializer().seekPastMagicBytes(ByteBuffer.wrap(brokenMessage));
    }

    /**
     * Tests serialization of an unknown message.
     */
    @Test(expected = Error.class)
    public void testSerializeUnknownMessage() throws Exception {
        MessageSerializer serializer = MAINNET.getDefaultSerializer();

        Message unknownMessage = new Message() {
            @Override
            protected void parse() throws ProtocolException {
            }
        };
        ByteArrayOutputStream bos = new ByteArrayOutputStream(ADDRESS_MESSAGE_BYTES.length);
        serializer.serialize(unknownMessage, bos);
    }
}
