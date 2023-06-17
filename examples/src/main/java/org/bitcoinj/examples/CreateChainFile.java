/*
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

package org.bitcoinj.examples;

import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.CheckpointManager;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.core.listeners.NewBestBlockListener;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.Wallet;

import java.io.File;

/**
 * The following example shows you how to restore a HD wallet from a previously generated deterministic seed.
 * In this example we manually setup the blockchain, peer group, etc. You can also use the WalletAppKit which provides a restoreWalletFromSeed function to load a wallet from a deterministic seed.
 */
public class CreateChainFile {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("CreateSpvChainFile network height");
            System.out.println("  one or more arguments are missing!");
            return;
        }
        BriefLogFormatter.init();
        String network = args[0];
        int height = Integer.parseInt(args[1]);

        NetworkParameters params;
        switch (network) {
            case "testnet":
                params = TestNet3Params.get();
                break;
            default:
                params = MainNetParams.get();
                break;
        }
        Context context = Context.getOrCreate(params);

        // Bitcoinj supports hierarchical deterministic wallets (or "HD Wallets"): https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki
        // HD wallets allow you to restore your wallet simply from a root seed. This seed can be represented using a short mnemonic sentence as described in BIP 39: https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki

        // Here we restore our wallet from a seed with no passphrase. Also have a look at the BackupToMnemonicSeed.java example that shows how to backup a wallet by creating a mnemonic sentence.
        String seedCode = "yard impulse luxury drive today throw farm pepper survey wreck glass federal";
        String passphrase = "";
        long creationTime = Utils.currentTimeSeconds() - 60 * 60 * 24 * 30 * 2;


        DeterministicSeed seed = new DeterministicSeed(seedCode, null, passphrase, creationTime);

        // The wallet class provides a easy fromSeed() function that loads a new wallet from a given seed.
        Wallet wallet = Wallet.fromSeed(params, seed, Script.ScriptType.P2PKH);

        // Because we are importing an existing wallet which might already have transactions we must re-download the blockchain to make the wallet picks up these transactions
        // You can find some information about this in the guides: https://bitcoinj.github.io/working-with-the-wallet#setup
        // To do this we clear the transactions of the wallet and delete a possible existing blockchain file before we download the blockchain again further down.
        System.out.println(wallet.toString());
        wallet.clearTransactions(0);
        File chainFile = new File(params.getId() + "-get-chain.spvchain");
        if (chainFile.exists()) {
            chainFile.delete();
        }

        // Setting up the BlochChain, the BlocksStore and connecting to the network.
        SPVBlockStore chainStore = new SPVBlockStore(params, chainFile);
        BlockChain chain = new BlockChain(params, chainStore);

        // get a checkpoint from at least 2 months ago.
        StoredBlock checkpointBlock = new CheckpointManager(context).getCheckpointBefore(creationTime);
        chainStore.put(checkpointBlock);
        chainStore.setChainHead(checkpointBlock);

        PeerGroup peerGroup = new PeerGroup(params, chain);
        peerGroup.addPeerDiscovery(new DnsDiscovery(params));

        // Now we need to hook the wallet up to the blockchain and the peers. This registers event listeners that notify our wallet about new transactions.
        chain.addWallet(wallet);
        peerGroup.addWallet(wallet);

        DownloadProgressTracker bListener = new DownloadProgressTracker() {
            @Override
            public void doneDownload() {
                System.out.println("blockchain downloaded");
            }
        };

        chain.addNewBestBlockListener(new NewBestBlockListener() {
            @Override
            public void notifyNewBestBlock(StoredBlock block) throws VerificationException {
                if (block.getHeight() == height + 1) {
                    System.exit(0);
                }
            }
        });

        // Now we re-download the blockchain. This replays the chain into the wallet. Once this is completed our wallet should know of all its transactions and print the correct balance.
        peerGroup.start();
        peerGroup.startBlockChainDownload(bListener);

        bListener.await();

        // shutting down again
        peerGroup.stop();
    }
}
