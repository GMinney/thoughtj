/*
 * Copyright 2013 Google Inc.
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

package org.bitcoinj.params;

import static org.bitcoinj.core.Utils.HEX;

import org.bitcoinj.core.*;
import org.bitcoinj.quorums.LLMQParameters;

import java.math.BigInteger;

import static com.google.common.base.Preconditions.checkState;

/**
 * Parameters for the testnet, a separate public instance of Dash that has relaxed rules suitable for development
 * and testing of applications and new Dash versions.
 */
public class TestNet3Params extends AbstractBitcoinNetParams {

    public static final int TESTNET_MAJORITY_WINDOW = 100;
    public static final int TESTNET_MAJORITY_REJECT_BLOCK_OUTDATED = 75;
    public static final int TESTNET_MAJORITY_ENFORCE_BLOCK_UPGRADE = 51;

    public TestNet3Params() {
        super();
        id = ID_TESTNET;

        packetMagic = 0x2b9939bf; // Changed for Thought
        interval = INTERVAL;
        targetTimespan = TARGET_TIMESPAN;

        // 00000fffffffffffffffffffffffffffffffffffffffffffffffffffffffffff
        maxTarget = Utils.decodeCompactBits(0x1e0fffffL); // Changed for Thought
        port = 11618; // Changed for Thought
        addressHeader = 109; // Changed for Thought
        p2shHeader = 193; // Changed for Thought

        dumpedPrivateKeyHeader = 239;

        genesisBlock.setTime(1521039602L); // Changed for Thought
        genesisBlock.setDifficultyTarget(0x1d00ffffL); // Changed for Thought
        genesisBlock.setNonce(2074325340L); // Changed for Thought
        spendableCoinbaseDepth = 100;
        subsidyDecreaseBlockCount = 1299382; // Changed for Thought
        String genesisHash = genesisBlock.getHashAsString();

        checkState(genesisHash.equals("00000000917e049641189c33d6b1275155e89b7b498b3b4f16d488f60afe513b")); // Changed for Thought
        alertSigningKey = HEX.decode("04517d8a699cb43d3938d7b24faaff7cda448ca4ea267723ba614784de661949bf632d6304316b244646dea079735b9a6fc4af804efb4752075b9fe2245e14e412");

        dnsSeeds = new String[] {
                "phi.thought.live",
                "phee.thought.live",
        }; // Changed for Thought

        bip32HeaderP2PKHpub = 0x043587cf; // The 4 byte header that serializes in base58 to "tpub".
        bip32HeaderP2PKHpriv = 0x04358394; // The 4 byte header that serializes in base58 to "tprv"
        dip14HeaderP2PKHpub = 0x0eed270b; // The 4 byte header that serializes in base58 to "dptp".
        dip14HeaderP2PKHpriv = 0x0eed2774; // The 4 byte header that serializes in base58 to "dpts"

        // Changed for Thought
        checkpoints.put(    0, Sha256Hash.wrap("00000000917e049641189c33d6b1275155e89b7b498b3b4f16d488f60afe513b"));
        checkpoints.put(   128, Sha256Hash.wrap("000b288b55c8f6c919369ee26f517861f6552c294b7d262339c80de906fe01c8"));
        checkpoints.put(   154509, Sha256Hash.wrap("001ecb9553a2d270c7055fee8b91401ac63f6c5f8e8926d958d88b679d8ccb70"));


        // updated with Dash Core 0.17.0.3 seed list
        addrSeeds = new int[]{
                0x10a8302d,
                0x4faf4433,
                0x05dacd3c,
                0x939c6e8f,
                0xf9cb3eb2,
                0xf093bdce
        };
        bip32HeaderP2PKHpub = 0x5D405F7A;
        bip32HeaderP2PKHpriv = 0xB6F13F50;

        strSporkAddress = "kxkf3ojUeHpzBuU5qdXEWKND5E4LmkQ6qU";
        minSporkKeys = 1;
        budgetPaymentsStartBlock = 4100;
        budgetPaymentsCycleBlocks = 50;
        budgetPaymentsWindowBlocks = 10;

        majorityEnforceBlockUpgrade = TESTNET_MAJORITY_ENFORCE_BLOCK_UPGRADE;
        majorityRejectBlockOutdated = TESTNET_MAJORITY_REJECT_BLOCK_OUTDATED;
        majorityWindow = TESTNET_MAJORITY_WINDOW;

        DIP0001BlockHeight = 4400;

        fulfilledRequestExpireTime = 5 * 60;
        masternodeMinimumConfirmations = 1;
        superblockStartBlock = 250000;
        superblockCycle = 24;
        nGovernanceMinQuorum = 1;
        nGovernanceFilterElements = 500;

        powDGWHeight = 4002;
        powKGWHeight = 4002;
        powAllowMinimumDifficulty = true;
        powNoRetargeting = false;

        instantSendConfirmationsRequired = 2;
        instantSendKeepLock = 6;

        DIP0003BlockHeight = 300000;
        deterministicMasternodesEnabledHeight = 300000;
        deterministicMasternodesEnabled = true;

        maxCuckooTarget = new BigInteger("00ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);
        cuckooHardForkBlockHeight = 44;
        cuckooRequiredBlockHeight = 100;
        midasStartHeight = 2;
        midasValidHeight = 2;

        DIP0008BlockHeight = 78800;
        DIP0024BlockHeight = 769700 + 4 * 288;
        v19BlockHeight = 850100;

        //LLMQ parameters
        addLLMQ(LLMQParameters.LLMQType.LLMQ_50_60);
        addLLMQ(LLMQParameters.LLMQType.LLMQ_400_60);
        addLLMQ(LLMQParameters.LLMQType.LLMQ_400_85);
        addLLMQ(LLMQParameters.LLMQType.LLMQ_100_67);
        addLLMQ(LLMQParameters.LLMQType.LLMQ_60_75);
        addLLMQ(LLMQParameters.LLMQType.LLMQ_25_67);
        llmqChainLocks = LLMQParameters.LLMQType.LLMQ_50_60;
        llmqForInstantSend = LLMQParameters.LLMQType.LLMQ_50_60;
        llmqTypePlatform = LLMQParameters.LLMQType.LLMQ_25_67;
        llmqTypeDIP0024InstantSend = LLMQParameters.LLMQType.LLMQ_60_75;
        llmqTypeMnhf = LLMQParameters.LLMQType.LLMQ_50_60;

        BIP34Height = 76;   // 000008ebb1db2598e897d17275285767717c6acfeac4c73def49fbea1ddcbcb6
        BIP65Height = 2431; // 0000039cf01242c7f921dcb4806a5994bc003b48c1973ae0c89b67809c2bb2ab
        BIP66Height = 2075;

        coinType = 1;
        assumeValidQuorums.add(Sha256Hash.wrap("0000000007697fd69a799bfa26576a177e817bc0e45b9fcfbf48b362b05aeff2"));
        assumeValidQuorums.add(Sha256Hash.wrap("000000339cd97d45ee18cd0cba0fd590fb9c64e127d3c30885e5b7376af94fdf"));
        assumeValidQuorums.add(Sha256Hash.wrap("0000007833f1b154218be64712cabe0e7c695867cc0c452311b2d786e14622fa"));
     }

    private static TestNet3Params instance;

    public static synchronized TestNet3Params get() {
        if (instance == null) {
            instance = new TestNet3Params();
        }
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_TESTNET;
    }

    public static String[] MASTERNODES = {
            "54.213.94.216",
            "35.165.156.159",
            "35.90.157.206",
            "35.91.197.218",
            "35.90.254.245",
            "54.202.231.195",
            "35.88.122.202",
            "54.186.145.18",
            "35.90.193.169",
            "34.212.161.186",
            "34.220.155.3",
            "54.212.138.75",
            "54.188.69.89",
            "54.190.131.8",
            "34.220.194.253",
            "54.191.28.44",
            "35.87.238.118",
            "35.90.217.208",
            "34.220.243.24",
            "35.161.222.74",
            "54.190.61.70",
            "34.210.26.195",
            "34.217.191.164",
            "54.189.125.235",
            "34.220.175.29",
            "52.36.20.123",
            "54.185.69.133",
            "54.68.48.149",
            "34.210.84.163",
            "54.202.190.181",
            "35.91.239.75",
            "34.222.21.14",
            "34.220.134.30",
            "35.90.252.3",
            "35.89.166.118",
            "18.237.170.32",
            "35.162.18.116",
            "35.91.208.56",
            "34.219.33.231",
            "52.34.250.214",
            "35.91.134.89",
            "50.112.58.114",
            "54.191.146.137",
            "34.218.66.37",
            "34.221.196.103",
            "35.91.157.30",
            "34.221.102.51",
            "18.237.165.242",
            "52.37.61.9",
            "54.212.89.127",
            "34.209.238.228",
            "35.92.143.7",
            "35.89.113.195",
            "52.12.54.89",
            "34.219.153.30",
            "34.215.171.237",
            "54.70.243.3",
            "54.184.126.25",
            "34.222.85.18",
            "34.221.252.179",
            "35.85.33.152",
            "54.200.220.105",
            "54.245.75.47",
            "54.214.59.174",
            "35.164.77.177",
            "35.89.66.84",
            "35.91.150.34",
            "35.92.219.124",
            "34.222.82.127",
            "34.220.171.156",
            "35.90.42.64",
            "35.89.53.128",
            "35.93.151.188",
            "34.211.172.212",
            "34.220.118.79",
            "34.220.187.233",
            "34.220.85.81",
            "35.167.165.224",
            "34.210.26.93",
            "35.90.53.180",
            "35.91.113.101",
            "52.33.80.29",
            "52.24.57.86",
            "35.88.208.212",
            "35.86.83.145",
            "54.200.67.128",
            "54.201.222.80",
            "54.201.152.135",
            "34.222.115.164",
            "34.216.138.123",
            "52.38.233.122",
            "34.220.87.80",
            "52.10.114.66",
            "35.86.138.150",
            "54.184.56.224",
            "35.166.112.89",
            "35.91.190.13",
            "34.220.155.147",
            "52.38.30.249",
            "35.91.120.127",
            "34.221.86.37",
            "35.90.113.75",
            "35.162.160.180",
            "54.191.215.11",
            "54.187.6.239",
            "35.163.31.53",
            "35.89.98.198",
            "35.90.232.242",
            "34.213.217.43",
            "35.89.163.83",
            "34.212.169.162",
            "35.87.22.219",
            "54.184.204.144",
    };

    @Override
    public String[] getDefaultMasternodeList() {
        return MASTERNODES;
    }
}
