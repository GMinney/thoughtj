/*
 * Copyright 2013 Google Inc.
 * Copyright 2015 Andreas Schildbach
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

import org.bitcoinj.core.*;
import org.bitcoinj.quorums.LLMQParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.HashMap;

import static com.google.common.base.Preconditions.*;

/**
 * Parameters for the main production network on which people trade goods and services.
 */
public class MainNetParams extends AbstractBitcoinNetParams {
    private static final Logger log = LoggerFactory.getLogger(MainNetParams.class);

    public static final int MAINNET_MAJORITY_WINDOW = 1000;
    public static final int MAINNET_MAJORITY_REJECT_BLOCK_OUTDATED = 950;
    public static final int MAINNET_MAJORITY_ENFORCE_BLOCK_UPGRADE = 750;

    public MainNetParams() {
        super();
        interval = INTERVAL;
        targetTimespan = TARGET_TIMESPAN;


        maxTarget = Utils.decodeCompactBits(0x1e0fffffL); // Changed for Thought
        dumpedPrivateKeyHeader = 123; // Changed for Thought
        addressHeader = 7; // Changed for Thought
        p2shHeader = 9; // Changed for Thought
        port = 10618; // Changed for Thought
        packetMagic = 0x59472ee4; //ajh - this is thought's

        bip32HeaderP2PKHpub = 0x0488b21e; // The 4 byte header that serializes in base58 to "xpub".
        bip32HeaderP2PKHpriv = 0x0488ade4; // The 4 byte header that serializes in base58 to "xprv"
        dip14HeaderP2PKHpub = 0x0eecefc5; // The 4 byte header that serializes in base58 to "dpmp".
        dip14HeaderP2PKHpriv = 0x0eecf02e; // The 4 byte header that serializes in base58 to "dpms"

        genesisBlock.setDifficultyTarget(0x1d00ffffL); // Changed for Thought
        genesisBlock.setTime(1521039602L); // Changed for Thought
        genesisBlock.setNonce(2074325340); // Changed for Thought

        majorityEnforceBlockUpgrade = MAINNET_MAJORITY_ENFORCE_BLOCK_UPGRADE;
        majorityRejectBlockOutdated = MAINNET_MAJORITY_REJECT_BLOCK_OUTDATED;
        majorityWindow = MAINNET_MAJORITY_WINDOW;

        id = ID_MAINNET;
        subsidyDecreaseBlockCount = 210240;
        spendableCoinbaseDepth = 100;
        String genesisHash = genesisBlock.getHashAsString();
        checkState(genesisHash.equals("00000000917e049641189c33d6b1275155e89b7b498b3b4f16d488f60afe513b"),
                genesisHash); // Changed for Thought

        dnsSeeds = new String[] {
                "phee.thought.live",
                "phi.thought.live",
                "pho.thought.live",
                "phum.thought.live"
        }; // Changed for Thought

        // This contains (at a minimum) the blocks which are not BIP30 compliant. BIP30 changed how duplicate
        // transactions are handled. Duplicated transactions could occur in the case where a coinbase had the same
        // extraNonce and the same outputs but appeared at different heights, and greatly complicated re-org handling.
        // Having these here simplifies block connection logic considerably.

        checkpoints.put(  0, Sha256Hash.wrap("00000000917e049641189c33d6b1275155e89b7b498b3b4f16d488f60afe513b"));
        checkpoints.put(  2, Sha256Hash.wrap("00000000c4c1989f0979bae2b24840b48ddb5197866a8ee99c9399a2512ec588"));
        checkpoints.put(  5, Sha256Hash.wrap("000000003a062431a6e4430a6ade4ab402a29165462491338c98b336a8afb6ab"));
        checkpoints.put( 256, Sha256Hash.wrap("00000000acf5b9f9eb1ea8c56f07ff00c2e3b5335c1b574f98cc3b8b55b70ec3"));
        checkpoints.put( 1024, Sha256Hash.wrap("000000006aef3c0953d44120c972061811aca7a59167076573f9063e46265419"));
        checkpoints.put( 43010, Sha256Hash.wrap("00000000328f2e44914cf6af972de811d0f4869f9b4e9217e4093dd297c79f49"));
        checkpoints.put( 229731, Sha256Hash.wrap("000000006645878b6aa7c4f10044b9914e994f11e1c3905c72b7f7612c417a94"));
        checkpoints.put( 248000, Sha256Hash.wrap("006b52a5d017eb2590d25750c46542b2de43f7a3fdc6394d95db458cbcb35f85"));
        checkpoints.put( 388285, Sha256Hash.wrap("00e0d38562e2f576c3c501f4768b282824a7f9489778537c49e3b5492923f5c5"));


        // Dash does not have a Http Seeder
        // If an Http Seeder is set up, add it here.  References: HttpDiscovery
        httpSeeds = null;

        // updated with Dash Core 19.2 seed list
        addrSeeds = new int[] {
                0xddd53802,
                0x41e02303,
                0x22ed0905,
                0x21484e05,
                0xe12c6505,
                0x4f6ea105,
                0x077ea105,
                0x38e1af05,
                0x12cab505,
                0x6e3fbc05,
                0x5091bd05,
                0xc06aff05,
                0x09f48b12,
                0x94819d12,
                0xa645cc12,
                0x2af65117,
                0x0a855317,
                0xc4855317,
                0xcb00a317,
                0x6863941f,
                0x3204b21f,
                0x2e4de52b,
                0x91f8082d,
                0x9afa082d,
                0x1818212d,
                0xf93d212d,
                0x40383a2d,
                0xdd383a2d,
                0x5a6b3f2d,
                0x3a9e472d,
                0x6c9e472d,
                0x689f472d,
                0x5b534c2d,
                0xcfa94d2d,
                0x2d75552d,
                0xca75552d,
                0x9aa2562d,
                0x2aa3562d,
                0x90a3562d,
                0xd95e5b2d,
                0xc9138c2d,
                0x7fa2042e,
                0xbff10a2e,
                0xbdbd1e2e,
                0xd5bd1e2e,
                0xf228242e,
                0x79e7942e,
                0x06f1fe2e,
                0x18f1fe2e,
                0x1cf1fe2e,
                0xa66d6d2f,
                0xc538f32f,
                0x5baf1132,
                0xb70e7432,
                0xce600f33,
                0x2a750f33,
                0x5f1e5933,
                0xeda99e33,
                0x075bc333,
                0x3c8dca34,
                0x60b9a436,
                0x2bdada36,
                0xa6e06e3a,
                0x45f3f442,
                0x46f3f442,
                0xd76b3d45,
                0x8307244b,
                0x8507244b,
                0x0463df4d,
                0x0013534e,
                0x3b1f624f,
                0x5f1d8f4f,
                0xc2b85a50,
                0xaaead150,
                0x8acfd350,
                0xe784f050,
                0x76f00251,
                0xe131b151,
                0x098eb151,
                0x64a6b151,
                0x8ea6b151,
                0x33fae351,
                0x53e6ca52,
                0x1715d352,
                0xb315d352,
                0x6919d352,
                0xc119d352,
                0x2863ef53,
                0x34743454,
                0xccb3f254,
                0x5bf81155,
                0xb158c155,
                0x23f1d155,
                0x47f1d155,
                0xbaf1d155,
                0xbef1d155,
                0x56fd6257,
                0x712cf957,
                0x6ac0f95e,
                0x80c0f95e,
                0xb7c0f95e,
                0x8d33b75f,
                0x2c35b75f,
                0x08c4d35f,
                0x20c4d35f,
                0x2ec4d35f,
                0xdb5fa067,
                0xe15fa067,
                0xf95fa067,
                0x7223ee68,
                0x1609376a,
                0x5a18a16b,
                0x5f41eb6d,
                0x7241eb6d,
                0xaa45eb6d,
                0x8546eb6d,
                0xa640c17b,
                0x40e51285,
                0x081c448a,
                0x22d2ee8c,
                0x6b355f8d,
                0x5fcdca8e,
                0xa67f5b90,
                0xa78e7e90,
                0x421c8391,
                0x441c8391,
                0xd61d8391,
                0x602a8391,
                0x06309e96,
                0xe4454398,
                0x4aa2659e,
                0x1ca8659e,
                0x87ea16a5,
                0x3e4f56a7,
                0x045077a8,
                0xf155eba8,
                0x315deba8,
                0xbe68eba8,
                0x87aa4baa,
                0x7a15f9ad,
                0xcbe922ae,
                0xcce922ae,
                0xcee922ae,
                0xcfe922ae,
                0xdc115eb0,
                0x914166b0,
                0xc6397bb0,
                0xcb397bb0,
                0xcd397bb0,
                0xce397bb0,
                0x81793fb2,
                0xccfe80b2,
                0x7e5b9db2,
                0xb05b9db2,
                0xb35b9db2,
                0xaa973eb9,
                0xae973eb9,
                0x90d48eb9,
                0x22639bb9,
                0x75aba5b9,
                0x289eafb9,
                0xda39c6b9,
                0x2218d5b9,
                0x7153e4b9,
                0x9c53e4b9,
                0x5edf44bc,
                0x28e67fbc,
                0xf3ed7fbc,
                0x844fe1bc,
                0xdba634c0,
                0x8c5340c0,
                0x5706a9c0,
                0x15391dc1,
                0x603b1dc1,
                0xe051edc1,
                0x185287c2,
                0xd25f62c3,
                0x11d2b5c3,
                0x40d3b5c3,
                0x4cb57ac8,
                0xcb1205ca,
                0x7840a7cf,
                0x806e18d4,
                0xd20034d4,
                0x263f81d4,
                0xaef9a8d5,
                0x3ed96bd8,
                0x089abdd8,
                0x3461fad8,
                0xf00f45d9
        };

        strSporkAddress = "3vjBVUDb38RDsByGVFZ3AVkzB4eU1XJ9ox";
        minSporkKeys = 1;
        budgetPaymentsStartBlock = 385627;
        budgetPaymentsCycleBlocks = 26700;
        budgetPaymentsWindowBlocks = 100;

        DIP0001BlockHeight = 393500;

        fulfilledRequestExpireTime = 60*60;
        masternodeMinimumConfirmations = 15;
        superblockStartBlock = 614820;
        superblockCycle = 16616;
        nGovernanceMinQuorum = 40;
        nGovernanceFilterElements = 20000;

        powDGWHeight = 34140;
        powKGWHeight = 15200;
        powAllowMinimumDifficulty = false;
        powNoRetargeting = false;

        instantSendConfirmationsRequired = 6;
        instantSendKeepLock = 24;

        DIP0003BlockHeight = 1028160;
        deterministicMasternodesEnabledHeight = 1047200;
        deterministicMasternodesEnabled = true;

        maxCuckooTarget = new BigInteger("00ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);
        cuckooHardForkBlockHeight = 246500;
        cuckooRequiredBlockHeight = 248800;
        midasStartHeight = 337;
        midasValidHeight = 512;

        DIP0008BlockHeight = 1088640;
        DIP0024BlockHeight = 1737792 + 4 * 288; // DIP24 activation time + 4 cycles
        v19BlockHeight = 1899072;

        // long living quorum params
        addLLMQ(LLMQParameters.LLMQType.LLMQ_50_60);
        addLLMQ(LLMQParameters.LLMQType.LLMQ_400_60);
        addLLMQ(LLMQParameters.LLMQType.LLMQ_400_85);
        addLLMQ(LLMQParameters.LLMQType.LLMQ_100_67);
        addLLMQ(LLMQParameters.LLMQType.LLMQ_60_75);
        llmqChainLocks = LLMQParameters.LLMQType.LLMQ_400_60;
        llmqForInstantSend = LLMQParameters.LLMQType.LLMQ_50_60;
        llmqTypePlatform = LLMQParameters.LLMQType.LLMQ_100_67;
        llmqTypeDIP0024InstantSend = LLMQParameters.LLMQType.LLMQ_60_75;
        llmqTypeMnhf = LLMQParameters.LLMQType.LLMQ_400_85;

        BIP34Height = 951;    // 000001f35e70f7c5705f64c6c5cc3dea9449e74d5b5c7cf74dad1bcca14a8012
        BIP65Height = 619382; // 00000000000076d8fcea02ec0963de4abfd01e771fec0863f960c2c64fe6f357
        BIP66Height = 245817;

        coinType = 5;
    }

    private static MainNetParams instance;
    public static synchronized MainNetParams get() {
        if (instance == null) {
            instance = new MainNetParams();
        }
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_MAINNET;
    }

    @Override
    protected void verifyDifficulty(StoredBlock storedPrev, Block nextBlock, BigInteger newTarget) {

        long newTargetCompact = calculateNextDifficulty(storedPrev, nextBlock, newTarget);
        long receivedTargetCompact = nextBlock.getDifficultyTarget();
        int height = storedPrev.getHeight() + 1;

        // On mainnet before block 68589: incorrect proof of work (DGW pre-fork)
        // see ContextualCheckBlockHeader in src/validation.cpp in Core repo (dashpay/dash)
        String msg = "Network provided difficulty bits do not match what was calculated: " +
                Long.toHexString(newTargetCompact) + " vs " + Long.toHexString(receivedTargetCompact);
        if (height <= 68589) {
            double n1 = convertBitsToDouble(receivedTargetCompact);
            double n2 = convertBitsToDouble(newTargetCompact);

            if (java.lang.Math.abs(n1 - n2) > n1 * 0.5 )
                throw new VerificationException(msg);
        } else {
            if (newTargetCompact != receivedTargetCompact)
                throw new VerificationException(msg);
        }
    }

    static double convertBitsToDouble(long nBits) {
        long nShift = (nBits >> 24) & 0xff;

        double dDiff =
                (double)0x0000ffff / (double)(nBits & 0x00ffffff);

        while (nShift < 29)
        {
            dDiff *= 256.0;
            nShift++;
        }
        while (nShift > 29)
        {
            dDiff /= 256.0;
            nShift--;
        }

        return dDiff;
    }
}
