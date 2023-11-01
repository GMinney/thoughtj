package org.bitcoinj.crypto.cuckoo;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface ThoughtClientInterface {
    interface Block extends Serializable
    {

        String hash();

        int confirmations();

        int size();

        int height();

        int version();

        String merkleRoot();

        List<String> tx();

        Date time();

        long nonce();

        String bits();

        double difficulty();

        String previousHash();

        String nextHash();

        String chainwork();

        Block previous() throws GenericRpcException;

        Block next() throws GenericRpcException;
    }

    interface BlockTemplate extends Serializable
    {

        List<String> capabilities();

        long version();

        List<String> rules();

        interface vbavailable {

        }

        int vbrequired();

        String previousblockhash();

        List<BlockTemplateTransaction> transactions();

        interface coinbaseaux {
            String flags();
        }

        long coinbasevalue();

        String longpollid();

        String target();

        long mintime();

        List<String> mutable();

        String noncerange();

        long sigoplimit();

        long sizelimit();

        long curtime();

        String bits();

        long height();

        List<Masternode> masternode();

        boolean masternode_payments_started();

        boolean masternode_payments_enforced();

        String coinbase_payload();
    }

    static interface BlockTemplateTransaction extends Serializable
    {

        String data();
        String hash();
        List<Long> depends();
        long fee();
        long sigops();
        boolean required();
    }

    static interface Masternode extends Serializable
    {
        String payee();

        String script();

        long amount();
    }

    static interface Transaction extends Serializable
    {

        String account();

        String address();

        String category();

        double amount();

        double fee();

        boolean generated();

        int confirmations();

        String blockHash();

        int blockIndex();

        Date blockTime();

        String txId();

        Date time();

        Date timeReceived();

        interface Details
        {
            String account();
            String address();
            String category();
            double amount();
            String label();
            int vout();
        }

        List<Details> details();

        String comment();

        String commentTo();

        RawTransaction raw();
    }

    interface RawTransaction extends Serializable
    {

        String hex();

        String txId();

        int version();

        long lockTime();

        long size();

        long vsize();

        String hash();

        /*
         *
         */
        interface In extends TxInput, Serializable
        {
            boolean isCoinbase();

            String coinbase();

            Map<String, Object> scriptSig();

            long sequence();

            RawTransaction getTransaction();

            Out getTransactionOutput();
        }

        /**
         * This method should be replaced someday
         *
         * @return the list of inputs
         */
        List<In> vIn(); // TODO : Create special interface instead of this

        interface Out extends Serializable
        {

            double value();

            int n();

            interface ScriptPubKey extends Serializable
            {

                String asm();

                String hex();

                int reqSigs();

                String type();

                List<String> addresses();
            }

            ScriptPubKey scriptPubKey();

            TxInput toInput();

            RawTransaction transaction();
        }

        /**
         * This method should be replaced someday
         */
        List<Out> vOut(); // TODO : Create special interface instead of this

        String blockHash();

        int confirmations();

        Date time();

        Date blocktime();
    }

    public BlockTemplate getBlockTemplate(String longpollid) throws GenericRpcException;

    public static interface TxInput extends Serializable
    {

        public String txid();

        public int vout();

        public String scriptPubKey();
    }
}
