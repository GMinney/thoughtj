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

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import org.bitcoinj.core.*;
import org.bitcoinj.utils.MonetaryFormat;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.utils.MonetaryFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import static com.google.common.base.Preconditions.checkState;

/**
 * Parameters for Bitcoin-like networks.
 */
public abstract class AbstractBitcoinNetParams extends NetworkParameters {
    /**
     * Scheme part for Bitcoin URIs.
     */
    public static final String BITCOIN_SCHEME = "thought";

    private static final Logger log = LoggerFactory.getLogger(AbstractBitcoinNetParams.class);

    protected int powDGWHeight;
    protected int powKGWHeight;
    protected boolean powAllowMinimumDifficulty;
    protected boolean powNoRetargeting;

    public AbstractBitcoinNetParams() {
        super();
    }


    /**
     * Checks if we are at a difficulty transition point.
     * @param storedPrev The previous stored block
     * @return If this is a difficulty transition point
     */
    protected boolean isDifficultyTransitionPoint(StoredBlock storedPrev) {
        int height = storedPrev.getHeight();
        return isDifficultyTransitionPoint(height);
    }

    protected boolean isDifficultyTransitionPoint(int height) {
        return height >= powKGWHeight || height >= powDGWHeight ? true :
                ((height + 1) % this.getInterval()) == 0;
    }

    @Override
    public void checkDifficultyTransitions(final StoredBlock storedPrev, final Block nextBlock,
                                           final BlockStore blockStore) throws VerificationException, BlockStoreException {
        int height = storedPrev.getHeight() + 1;
        if(height >= powDGWHeight) {
            DarkGravityWave(storedPrev, nextBlock, blockStore);
        } else if(height >= midasStartHeight) {
            Midas(storedPrev, nextBlock, blockStore);
        } else {
            checkDifficultyTransitions_BTC(storedPrev, nextBlock, blockStore);
        }
    }

    public void checkDifficultyTransitions_BTC(final StoredBlock storedPrev, final Block nextBlock,
                                                final BlockStore blockStore) throws VerificationException, BlockStoreException {

        if(powNoRetargeting)
            return;

        Block prev = storedPrev.getHeader();

        // Is this supposed to be a difficulty transition point?
        if (!isDifficultyTransitionPoint(storedPrev)) {

            if(powAllowMinimumDifficulty) {

                // On non-difficulty transition points, easy
                // blocks are allowed if there has been a span of 5 minutes without one.
                final long timeDelta = nextBlock.getTimeSeconds() - prev.getTimeSeconds();
                // There is an integer underflow bug in bitcoin-qt that means mindiff blocks are accepted when time
                // goes backwards.
                if (timeDelta <= NetworkParameters.TARGET_SPACING * 2) {
                    // Walk backwards until we find a block that doesn't have the easiest proof of work, then check
                    // that difficulty is equal to that one.
                    StoredBlock cursor = storedPrev;
                    while (!cursor.getHeader().equals(getGenesisBlock()) &&
                            cursor.getHeight() % getInterval() != 0 &&
                            cursor.getHeader().getDifficultyTargetAsInteger().equals(getMaxTarget()))
                        cursor = cursor.getPrev(blockStore);
                    BigInteger cursorTarget = cursor.getHeader().getDifficultyTargetAsInteger();
                    BigInteger newTarget = nextBlock.getDifficultyTargetAsInteger();
                    if (!cursorTarget.equals(newTarget))
                        throw new VerificationException("Testnet block transition that is not allowed: " +
                                Long.toHexString(cursor.getHeader().getDifficultyTarget()) + " vs " +
                                Long.toHexString(nextBlock.getDifficultyTarget()));
                } else {
                    if(nextBlock.getDifficultyTarget() != Utils.encodeCompactBits(maxTarget))
                        throw new VerificationException("Unexpected change in difficulty at height " + storedPrev.getHeight() +
                                ": " + Long.toHexString(Utils.encodeCompactBits(maxTarget)) + " vs " +
                                Long.toHexString(nextBlock.getDifficultyTarget()));
                }
                return;
            }
            // No ... so check the difficulty didn't actually change.
            if (nextBlock.getDifficultyTarget() != prev.getDifficultyTarget())
                throw new VerificationException("Unexpected change in difficulty at height " + storedPrev.getHeight() +
                        ": " + Long.toHexString(nextBlock.getDifficultyTarget()) + " vs " +
                        Long.toHexString(prev.getDifficultyTarget()));
            return;
        }

        // We need to find a block far back in the chain. It's OK that this is expensive because it only occurs every
        // two weeks after the initial block chain download.
        final Stopwatch watch = Stopwatch.createStarted();
        StoredBlock cursor = null;
        Sha256Hash hash = prev.getHash();
        for (int i = 0; i < this.getInterval(); i++) {
            cursor = blockStore.get(hash);
            if (cursor == null) {
                // This should never happen. If it does, it means we are following an incorrect or busted chain.
                throw new VerificationException(
                        "Difficulty transition point but we did not find a way back to the last transition point. Not found: " + hash);
            }
            hash = cursor.getHeader().getPrevBlockHash();
        }
        checkState(cursor != null && isDifficultyTransitionPoint(cursor.getHeight() - 1),
                "Didn't arrive at a transition point.");
        watch.stop();
        if (watch.elapsed(TimeUnit.MILLISECONDS) > 50)
            log.info("Difficulty transition traversal took {}", watch);

        Block blockIntervalAgo = cursor.getHeader();
        int timespan = (int) (prev.getTimeSeconds() - blockIntervalAgo.getTimeSeconds());
        // Limit the adjustment step.
        final int targetTimespan = this.getTargetTimespan();
        if (timespan < targetTimespan / 4)
            timespan = targetTimespan / 4;
        if (timespan > targetTimespan * 4)
            timespan = targetTimespan * 4;

        BigInteger newTarget = Utils.decodeCompactBits(prev.getDifficultyTarget());
        newTarget = newTarget.multiply(BigInteger.valueOf(timespan));
        newTarget = newTarget.divide(BigInteger.valueOf(targetTimespan));

        verifyDifficulty(storedPrev, nextBlock, newTarget);
    }

    protected long calculateNextDifficulty(StoredBlock storedBlock, Block nextBlock, BigInteger newTarget) {
        int currentBlockHeight = storedBlock.getHeight() + 1;
        BigInteger nProofOfWorkLimit = (currentBlockHeight >= cuckooHardForkBlockHeight) ? this.getMaxCuckooTarget()
                : this.getMaxTarget();
        if (newTarget.compareTo(nProofOfWorkLimit) > 0)
        {
            log.info("Difficulty hit proof of work limit: {}", newTarget.toString(16));
            newTarget = nProofOfWorkLimit;
        }

        int accuracyBytes = (int) (nextBlock.getDifficultyTarget() >>> 24) - 3;

        // The calculated difficulty is to a higher precision than received, so reduce here.
        BigInteger mask = BigInteger.valueOf(0xFFFFFFL).shiftLeft(accuracyBytes * 8);
        newTarget = newTarget.and(mask);
        return Utils.encodeCompactBits(newTarget);
    }

    protected void verifyDifficulty(StoredBlock storedPrev, Block nextBlock, BigInteger newTarget) throws VerificationException {
        long newTargetCompact = calculateNextDifficulty(storedPrev, nextBlock, newTarget);
        long receivedTargetCompact = nextBlock.getDifficultyTarget();

        if (newTargetCompact != receivedTargetCompact)
            throw new VerificationException("Network provided difficulty bits do not match what was calculated: " +
                    Long.toHexString(newTargetCompact) + " vs " + Long.toHexString(receivedTargetCompact));
    }

    public void DarkGravityWave(StoredBlock storedPrev, Block nextBlock,
                                  final BlockStore blockStore) throws VerificationException {
        /* current difficulty formula, darkcoin - DarkGravity v3, written by Evan Duffield - evan@darkcoin.io */
        long pastBlocks = 24;

        if (storedPrev == null || storedPrev.getHeight() == 0 || storedPrev.getHeight() < pastBlocks) {
            verifyDifficulty(storedPrev, nextBlock, getMaxTarget());
            return;
        }

        if(powAllowMinimumDifficulty)
        {
            // recent block is more than 2 hours old
            if (nextBlock.getTimeSeconds() > storedPrev.getHeader().getTimeSeconds() + 2 * 60 * 60) {
                verifyDifficulty(storedPrev, nextBlock, getMaxTarget());
                return;
            }
            // recent block is more than 10 minutes old
            if (nextBlock.getTimeSeconds() > storedPrev.getHeader().getTimeSeconds() + NetworkParameters.TARGET_SPACING*4) {
                BigInteger newTarget = storedPrev.getHeader().getDifficultyTargetAsInteger().multiply(BigInteger.valueOf(10));
                verifyDifficulty(storedPrev, nextBlock, newTarget);
                return;
            }
        }
        StoredBlock cursor = storedPrev;
        BigInteger pastTargetAverage = BigInteger.ZERO;
        for(int countBlocks = 1; countBlocks <= pastBlocks; countBlocks++) {
            BigInteger target = cursor.getHeader().getDifficultyTargetAsInteger();
            if(countBlocks == 1) {
                pastTargetAverage = target;
            } else {
                pastTargetAverage = pastTargetAverage.multiply(BigInteger.valueOf(countBlocks)).add(target).divide(BigInteger.valueOf(countBlocks+1));
            }
            if(countBlocks != pastBlocks) {
                try {
                    cursor = cursor.getPrev(blockStore);
                    if(cursor == null) {
                        //when using checkpoints, the previous block will not exist until 24 blocks are in the store.
                        return;
                    }
                } catch (BlockStoreException x) {
                    //when using checkpoints, the previous block will not exist until 24 blocks are in the store.
                    return;
                }
            }
        }


        BigInteger newTarget = pastTargetAverage;

        long timespan = storedPrev.getHeader().getTimeSeconds() - cursor.getHeader().getTimeSeconds();
        long targetTimespan = pastBlocks*TARGET_SPACING;

        if (timespan < targetTimespan/3)
            timespan = targetTimespan/3;
        if (timespan > targetTimespan*3)
            timespan = targetTimespan*3;

        // Retarget
        newTarget = newTarget.multiply(BigInteger.valueOf(timespan));
        newTarget = newTarget.divide(BigInteger.valueOf(targetTimespan));
        verifyDifficulty(storedPrev, nextBlock, newTarget);

    }

    protected void KimotoGravityWell(StoredBlock storedPrev, Block nextBlock, BlockStore blockStore)
            throws BlockStoreException, VerificationException {
    /* current difficulty formula, megacoin - kimoto gravity well */

        StoredBlock         BlockLastSolved             = storedPrev;
        StoredBlock         BlockReading                = storedPrev;
        Block               BlockCreating               = nextBlock;

        long				PastBlocksMass				= 0;
        long				PastRateActualSeconds		= 0;
        long				PastRateTargetSeconds		= 0;
        double				PastRateAdjustmentRatio		= 1f;
        BigInteger			PastDifficultyAverage = BigInteger.ZERO;
        BigInteger			PastDifficultyAveragePrev = BigInteger.ZERO;;
        double				EventHorizonDeviation;
        double				EventHorizonDeviationFast;
        double				EventHorizonDeviationSlow;

        long pastSecondsMin = (long)(targetTimespan * 0.025);
        long pastSecondsMax = targetTimespan * 7;
        long PastBlocksMin = pastSecondsMin / TARGET_SPACING;
        long PastBlocksMax = pastSecondsMax / TARGET_SPACING;

        if (BlockLastSolved == null || BlockLastSolved.getHeight() == 0 || (long)BlockLastSolved.getHeight() < PastBlocksMin)
        {
            verifyDifficulty(storedPrev, nextBlock, getMaxTarget());
        }

        for (int i = 1; BlockReading != null && BlockReading.getHeight() > 0; i++) {
            if (PastBlocksMax > 0 && i > PastBlocksMax) { break; }
            PastBlocksMass++;

            if (i == 1)	{ PastDifficultyAverage = BlockReading.getHeader().getDifficultyTargetAsInteger(); }
            else		{ PastDifficultyAverage = ((BlockReading.getHeader().getDifficultyTargetAsInteger().subtract(PastDifficultyAveragePrev)).divide(BigInteger.valueOf(i)).add(PastDifficultyAveragePrev)); }
            PastDifficultyAveragePrev = PastDifficultyAverage;

            PastRateActualSeconds			= BlockLastSolved.getHeader().getTimeSeconds() - BlockReading.getHeader().getTimeSeconds();
            PastRateTargetSeconds			= TARGET_SPACING * PastBlocksMass;
            PastRateAdjustmentRatio			= 1.0f;
            if (PastRateActualSeconds < 0) { PastRateActualSeconds = 0; }

            if (PastRateActualSeconds != 0 && PastRateTargetSeconds != 0) {
                PastRateAdjustmentRatio			= (double)PastRateTargetSeconds / PastRateActualSeconds;
            }
            EventHorizonDeviation			= 1 + (0.7084 * java.lang.Math.pow((Double.valueOf(PastBlocksMass)/Double.valueOf(28.2)), -1.228));
            EventHorizonDeviationFast		= EventHorizonDeviation;
            EventHorizonDeviationSlow		= 1 / EventHorizonDeviation;

            if (PastBlocksMass >= PastBlocksMin) {
                if ((PastRateAdjustmentRatio <= EventHorizonDeviationSlow) || (PastRateAdjustmentRatio >= EventHorizonDeviationFast))
                {
                    break;
                }
            }
            StoredBlock BlockReadingPrev = blockStore.get(BlockReading.getHeader().getPrevBlockHash());
            if (BlockReadingPrev == null)
            {
                //Since we are using the checkpoint system, there may not be enough blocks to do this diff adjust,
                //so skip until we do
                return;
            }
            BlockReading = BlockReadingPrev;
        }

        BigInteger newDifficulty = PastDifficultyAverage;
        if (PastRateActualSeconds != 0 && PastRateTargetSeconds != 0) {
            newDifficulty = newDifficulty.multiply(BigInteger.valueOf(PastRateActualSeconds));
            newDifficulty = newDifficulty.divide(BigInteger.valueOf(PastRateTargetSeconds));
        }

        if (newDifficulty.compareTo(getMaxTarget()) > 0) {
            log.info("Difficulty hit proof of work limit: {}", newDifficulty.toString(16));
            newDifficulty = getMaxTarget();
        }

        verifyDifficulty(storedPrev, nextBlock, newDifficulty);

    }

    /**
     * MIDAS POW Algorithm
     *
     * This is MIDAS (Multi Interval Difficulty Adjustment System), a novel
     * getnextwork algorithm. It responds quickly to huge changes in hashing power,
     * is immune to time warp attacks, and regulates the block rate to keep the
     * block height close to the block height expected given the nominal block
     * interval and the elapsed time. How close the correspondence between block
     * height and wall clock time is, depends on how stable the hashing power has
     * been. Maybe Bitcoin can wait 2 weeks between updates but no altcoin can.
     *
     * It is important that none of these intervals (5, 7, 9, 17) have any common
     * divisor; eliminating the existence of harmonics is an important part of
     * eliminating the effectiveness of timewarp attacks.
     *
     * @param storedPrev
     * @param blockStore
     * @return
     */
    public void Midas(StoredBlock storedPrev, Block next, final BlockStore blockStore)
            throws VerificationException, BlockStoreException
    {
        Averages averages;
        long toofast;
        long tooslow;
        long difficultyfactor = 10000;
        long now;
        long BlockHeightTime;

        long nFastInterval = (TARGET_SPACING * 9) / 10; // seconds per block desired when far behind schedule
        long nSlowInterval = (TARGET_SPACING * 11) / 10; // seconds per block desired when far ahead of schedule
        long nIntervalDesired = TARGET_SPACING;

        int currentBlockHeight = storedPrev.getHeight() + 1;
        BigInteger nProofOfWorkLimit = (currentBlockHeight >= cuckooHardForkBlockHeight) ? this.getMaxCuckooTarget()
                : this.getMaxTarget();
        long cPowLimit = Utils.encodeCompactBits(nProofOfWorkLimit);
        BigInteger proposed = BigInteger.ZERO;

        if (storedPrev == null)
        {
            // Genesis Block
            proposed = nProofOfWorkLimit;
        }

        // Special rule for post-cuckoo fork, so that the difficulty can come down
        // far enough for mining.
        if (currentBlockHeight > cuckooHardForkBlockHeight && currentBlockHeight < cuckooHardForkBlockHeight + 50)
        {
            proposed = nProofOfWorkLimit;
        }

        if (powAllowMinimumDifficulty)
        {
            // mining of a min-difficulty block.
            if (next.getTimeSeconds() > storedPrev.getHeader().getTimeSeconds() + TARGET_SPACING * 2)
            {
                proposed = nProofOfWorkLimit;
            }
            else
            {
                // Return the last non-special-min-difficulty-rules-block
                StoredBlock pindex = storedPrev;
                while (pindex.getPrev(blockStore) != null && pindex.getHeight() % nIntervalDesired != 0
                        && pindex.getHeader().getDifficultyTarget() == cPowLimit)
                    pindex = pindex.getPrev(blockStore);
                proposed = Utils.decodeCompactBits(pindex.getHeader().getDifficultyTarget());
            }
        }

        if (proposed == BigInteger.ZERO)
        {
            // Regulate block times so as to remain synchronized in the long run with the
            // actual time. The first step is to
            // calculate what interval we want to use as our regulatory goal. It depends on
            // how far ahead of (or behind)
            // schedule we are. If we're more than an adjustment period ahead or behind, we
            // use the maximum (nSlowInterval) or minimum
            // (nFastInterval) values; otherwise we calculate a weighted average somewhere
            // in between them. The closer we are
            // to being exactly on schedule the closer our selected interval will be to our
            // nominal interval (TargetSpacing).

            long then = blockStore.getParams().getGenesisBlock().getTimeSeconds();

            now = storedPrev.getHeader().getTimeSeconds();
            BlockHeightTime = then + storedPrev.getHeight() * TARGET_SPACING;

            if (now < BlockHeightTime + (TARGET_TIMESPAN / TARGET_SPACING) && now > BlockHeightTime)
            {
                // ahead of schedule by less than one interval.
                nIntervalDesired = (((TARGET_TIMESPAN / TARGET_SPACING) - (now - BlockHeightTime)) * TARGET_SPACING
                        + (now - BlockHeightTime) * nFastInterval) / (TARGET_TIMESPAN / TARGET_SPACING);
            }
            else if (now + (TARGET_TIMESPAN / TARGET_SPACING) > BlockHeightTime && now < BlockHeightTime)
            {
                // behind schedule by less than one interval.
                nIntervalDesired = (((TARGET_TIMESPAN / TARGET_SPACING) - (BlockHeightTime - now)) * TARGET_SPACING
                        + (BlockHeightTime - now) * nSlowInterval) / (TARGET_TIMESPAN / TARGET_SPACING);

                // ahead by more than one interval;
            }
            else if (now < BlockHeightTime)
            {
                nIntervalDesired = nSlowInterval;
            } // behind by more than an interval.
            else
            {
                nIntervalDesired = nFastInterval;
            }

            // find out what average intervals over last 5, 7, 9, and 17 blocks have been.
            averages = averageRecentTimestamps(storedPrev, blockStore);

            // check for emergency adjustments. These are to bring the diff up or down FAST
            // when a burst miner or multipool
            // jumps on or off. Once they kick in they can adjust difficulty very rapidly,
            // and they can kick in very rapidly
            // after massive hash power jumps on or off.

            // Important note: This is a self-damping adjustment because 8/5 and 5/8 are
            // closer to 1 than 3/2 and 2/3. Do not
            // screw with the constants in a way that breaks this relationship. Even though
            // self-damping, it will usually
            // overshoot slightly. But normal adjustment will handle damping without getting
            // back to emergency.
            toofast = (nIntervalDesired * 2) / 3;
            tooslow = (nIntervalDesired * 3) / 2;

            // both of these check the shortest interval to quickly stop when overshot.
            // Otherwise first is longer and second shorter.
            if (averages.avgOf5 < toofast && averages.avgOf9 < toofast && averages.avgOf17 < toofast)
            { // emergency adjustment, slow down (longer intervals because shorter blocks)
                // LogPrint(BCLog::MIDAS, "GetNextWorkRequired EMERGENCY RETARGET\n");
                difficultyfactor *= 8;
                difficultyfactor /= 5;
            }
            else if (averages.avgOf5 > tooslow && averages.avgOf7 > tooslow && averages.avgOf9 > tooslow)
            { // emergency adjustment, speed up (shorter intervals because longer blocks)
                // LogPrint(BCLog::MIDAS, "GetNextWorkRequired EMERGENCY RETARGET\n");
                difficultyfactor *= 5;
                difficultyfactor /= 8;
            }

            // If no emergency adjustment, check for normal adjustment.
            else if (((averages.avgOf5 > nIntervalDesired || averages.avgOf7 > nIntervalDesired) && averages.avgOf9 > nIntervalDesired
                    && averages.avgOf17 > nIntervalDesired)
                    || ((averages.avgOf5 < nIntervalDesired || averages.avgOf7 < nIntervalDesired) && averages.avgOf9 < nIntervalDesired
                    && averages.avgOf17 < nIntervalDesired))
            { // At least 3 averages too high or at least 3 too low, including the two
                // longest. This will be executed 3/16 of
                // the time on the basis of random variation, even if the settings are perfect.
                // It regulates one-sixth of the way
                // to the calculated point.
                // LogPrint(BCLog::MIDAS, "GetNextWorkRequired RETARGET\n");
                difficultyfactor *= (6 * nIntervalDesired);
                difficultyfactor /= averages.avgOf17 + (5 * nIntervalDesired);
            }

            // limit to doubling or halving. There are no conditions where this will make a
            // difference unless there is an
            // unsuspected bug in the above code.
            if (difficultyfactor > 20000)
                difficultyfactor = 20000;
            if (difficultyfactor < 5000)
                difficultyfactor = 5000;

            proposed = Utils.decodeCompactBits(storedPrev.getHeader().getDifficultyTarget());

            if (difficultyfactor != 10000)
            {
                proposed.divide(BigInteger.valueOf(difficultyfactor));
                proposed.multiply(BigInteger.valueOf(10000L));
            }

        }

        verifyDifficulty(storedPrev, next, proposed);
    }
    /**
     * Storage class for MIDAS averages.
     *
     * @author phil_000
     *
     */
    static class Averages
    {
        public long avgOf5;
        public long avgOf7;
        public long avgOf9;
        public long avgOf17;

        public Averages()
        {
            avgOf5 = 0;
            avgOf7 = 0;
            avgOf9 = 0;
            avgOf17 = 0;
        }
    }

    protected Averages averageRecentTimestamps(StoredBlock storedPrev, BlockStore blockStore)
    {
        Averages retval = new Averages();
        long blocktime = 0;
        long oldblocktime = 0;

        if (null != storedPrev)
        {
            blocktime = storedPrev.getHeader().getTimeSeconds();
        }

        StoredBlock prev = storedPrev;
        for (int blockoffset = 0; blockoffset < 17; blockoffset++)
        {
            oldblocktime = blocktime;
            if (null != prev)
            {
                try
                {
                    prev = prev.getPrev(blockStore);
                    if (null != prev)
                        blocktime = prev.getHeader().getTimeSeconds();
                    else
                        blocktime = 0;
                }
                catch (BlockStoreException e)
                {
                    blocktime = 0;
                }
            }
            else
            {
                blocktime -= TARGET_SPACING;
            }
            // for each block, add interval.
            if (blockoffset < 5)
                retval.avgOf5 += (oldblocktime - blocktime);
            if (blockoffset < 7)
                retval.avgOf7 += (oldblocktime - blocktime);
            if (blockoffset < 9)
                retval.avgOf9 += (oldblocktime - blocktime);
            retval.avgOf17 += (oldblocktime - blocktime);
        }

        retval.avgOf5 /= 5;
        retval.avgOf7 /= 7;
        retval.avgOf9 /= 9;
        retval.avgOf17 /= 17;

        return retval;
    }

    @Override
    public Coin getMaxMoney() {
        return MAX_MONEY;
    }

    @Override
    public Coin getMinNonDustOutput() {
        return Transaction.MIN_NONDUST_OUTPUT;
    }

    @Override
    public MonetaryFormat getMonetaryFormat() {
        return new MonetaryFormat();
    }

    @Override
    public int getProtocolVersionNum(final ProtocolVersion version) {
        return version.getBitcoinProtocolVersion();
    }

    @Override
    public BitcoinSerializer getSerializer(boolean parseRetain) {
        return new BitcoinSerializer(this, parseRetain);
    }

    @Override
    public String getUriScheme() {
        return BITCOIN_SCHEME;
    }

    @Override
    public boolean hasMaxMoney() {
        return true;
    }
}
