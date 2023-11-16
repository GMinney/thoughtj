/*
 * Copyright 2011 Google Inc.
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
import org.bitcoinj.params.Networks;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptPattern;
import org.bitcoinj.script.Script.ScriptType;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;

import static org.bitcoinj.core.Utils.HEX;
import static org.junit.Assert.*;

public class AddressTest {
    private static final NetworkParameters TESTNET = TestNet3Params.get();
    private static final NetworkParameters MAINNET = MainNetParams.get();

    @Test
    public void testJavaSerialization() throws Exception {
        Address testAddress = Address.fromBase58(TESTNET, "kvdPDVw6T6ws8N2fAZiaFMHsJLXWDXtHiq");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ObjectOutputStream(os).writeObject(testAddress);
        Address testAddressCopy = (Address) new ObjectInputStream(new ByteArrayInputStream(os.toByteArray()))
                .readObject();
        assertEquals(testAddress, testAddressCopy);

        Address mainAddress = Address.fromBase58(MAINNET, "42jEVihde9MqeqgtdKCBYe215UgUrJ545v");
        os = new ByteArrayOutputStream();
        new ObjectOutputStream(os).writeObject(mainAddress);
        Address mainAddressCopy = (Address) new ObjectInputStream(new ByteArrayInputStream(os.toByteArray()))
                .readObject();
        assertEquals(mainAddress, mainAddressCopy);
    }

    @Test
    public void stringification() throws Exception {
        // Test a testnet address.
        Address a = Address.fromPubKeyHash(TESTNET, HEX.decode("0a63b5ce1e7fe6ce871528d8ac682d5297f2fe48"));
        assertEquals("ksngXjrUNbd5Xj9CWPEDXtQP4bM2fKtbHL", a.toString());
        assertEquals(Script.ScriptType.P2PKH, a.getOutputScriptType());

        Address b = Address.fromPubKeyHash(MAINNET, HEX.decode("688d5862951d300f68fe64c794e94a5d7cbe9a81"));
        assertEquals("3yu3VYxGVmGZFeStA5dmKodi7vP5J4AWeb", b.toString());
        assertEquals(Script.ScriptType.P2PKH, b.getOutputScriptType());
    }

    @Test
    public void decoding() throws Exception {
        Address a = Address.fromBase58(TESTNET, "kvdPDVw6T6ws8N2fAZiaFMHsJLXWDXtHiq");
        // Looking for the raw hash not in Wallet Import Format (WIF) - kvdPDVw6T6ws8N2fAZiaFMHsJLXWDXtHiq convert to hex, strip the first 1st byte of network, strip last 4 bytes - checksum =
        // kvdPDVw6T6ws8N2fAZiaFMHsJLXWDXtHiq -> 6d2989e3f5c9fb3ecbb08f573f6cba59ea4cb383e56b2f2252 -> 2989e3f5c9fb3ecbb08f573f6cba59ea4cb383e5
        assertEquals("2989e3f5c9fb3ecbb08f573f6cba59ea4cb383e5", Utils.HEX.encode(a.getHash()));

        Address b = Address.fromBase58(MAINNET, "3yu3VYxGVmGZFeStA5dmKodi7vP5J4AWeb");
        assertEquals("688d5862951d300f68fe64c794e94a5d7cbe9a81", Utils.HEX.encode(b.getHash()));
    }

    @Test
    public void errorPaths() {
        // Check what happens if we try and decode garbage.
        try {
            Address.fromBase58(TESTNET, "this is not a valid address!");
            fail();
        } catch (AddressFormatException.WrongNetwork e) {
            fail();
        } catch (AddressFormatException e) {
            // Success.
        }

        // Check the empty case.
        try {
            Address.fromBase58(TESTNET, "");
            fail();
        } catch (AddressFormatException.WrongNetwork e) {
            fail();
        } catch (AddressFormatException e) {
            // Success.
        }

        // Check the case of a mismatched network.
        try {
            Address.fromBase58(TESTNET, "3yu3VYxGVmGZFeStA5dmKodi7vP5J4AWeb");
            fail();
        } catch (AddressFormatException.WrongNetwork e) {
            // Success.
        } catch (AddressFormatException e) {
            fail();
        }
    }

    @Test
    public void getNetwork() throws Exception {
        NetworkParameters params = Address.getParametersFromAddress("3yu3VYxGVmGZFeStA5dmKodi7vP5J4AWeb");
        assertEquals(MAINNET.getId(), params.getId());
        params = Address.getParametersFromAddress("kvdPDVw6T6ws8N2fAZiaFMHsJLXWDXtHiq");
        assertEquals(TESTNET.getId(), params.getId());
    }

    @Test
    public void getAltNetwork() throws Exception {
        // An alternative network
        class AltNetwork extends MainNetParams {
            AltNetwork() {
                super();
                id = "alt.network";
                addressHeader = 48;
                p2shHeader = 5;
            }
        }
        AltNetwork altNetwork = new AltNetwork();
        // Add new network params
        Networks.register(altNetwork);
        // Check if can parse address
        NetworkParameters params = Address.getParametersFromAddress("LLxSnHLN2CYyzB5eWTR9K9rS9uWtbTQFb6");
        assertEquals(altNetwork.getId(), params.getId());
        // Check if main network works as before
        params = Address.getParametersFromAddress("Xtqn4ks8sJS7iG7S7r1Jf37eFFSJGwh8a8");
        assertEquals(MAINNET.getId(), params.getId());
        // Unregister network
        Networks.unregister(altNetwork);
        try {
            Address.getParametersFromAddress("LLxSnHLN2CYyzB5eWTR9K9rS9uWtbTQFb6");
            fail();
        } catch (AddressFormatException e) { }
    }

    @Test
    public void p2shAddress() throws Exception {
        // Test that we can construct P2SH addresses
        Address mainNetP2SHAddress = Address.fromBase58(MAINNET, "4DKrc7BEEd8CbJRF3wREov8RqDBfGndiaf"); //2ac4b0b501117cc8119c5797b519538d4942e90e
        assertEquals(mainNetP2SHAddress.getVersion(), MAINNET.p2shHeader);
        assertEquals(Script.ScriptType.P2SH, mainNetP2SHAddress.getOutputScriptType());

        Address testNetP2SHAddress = Address.fromBase58(TESTNET, "kvdPDVw6T6ws8N2fAZiaFMHsJLXWDXtHiq"); //18a0e827269b5211eb51a4af1b2fa69333efa722
        assertEquals(testNetP2SHAddress.getVersion(), TESTNET.p2shHeader);
        assertEquals(Script.ScriptType.P2SH, testNetP2SHAddress.getOutputScriptType());

        // Test that we can determine what network a P2SH address belongs to
        NetworkParameters mainNetParams = Address.getParametersFromAddress("4DKrc7BEEd8CbJRF3wREov8RqDBfGndiaf");
        assertEquals(MAINNET.getId(), mainNetParams.getId());
        NetworkParameters testNetParams = Address.getParametersFromAddress("kvdPDVw6T6ws8N2fAZiaFMHsJLXWDXtHiq");
        assertEquals(TESTNET.getId(), testNetParams.getId());

        // Test that we can convert them from hashes
        byte[] hex = HEX.decode("2ac4b0b501117cc8119c5797b519538d4942e90e");
        Address a = Address.fromScriptHash(MAINNET, hex);
        assertEquals("4DKrc7BEEd8CbJRF3wREov8RqDBfGndiaf", a.toString());
        Address b = Address.fromScriptHash(TESTNET, HEX.decode("18a0e827269b5211eb51a4af1b2fa69333efa722"));
        assertEquals("kvdPDVw6T6ws8N2fAZiaFMHsJLXWDXtHiq", b.toString());
        Address c = Address.fromScriptHash(MAINNET,
                ScriptPattern.extractHashFromP2SH(ScriptBuilder.createP2SHOutputScript(hex)));
        assertEquals("4DKrc7BEEd8CbJRF3wREov8RqDBfGndiaf", c.toString());
    }

    @Test
    public void p2shAddressCreationFromKeys() throws Exception {
        // import some keys from this example: https://gist.github.com/gavinandresen/3966071
        ECKey key1 = DumpedPrivateKey.fromBase58(MAINNET, "KJU4eLyXPgvKX1kY5HpEPma9RLgjNYRcHxFo1Sa3i6gnANMYzqU8").getKey();
        key1 = ECKey.fromPrivate(key1.getPrivKeyBytes());
        ECKey key2 = DumpedPrivateKey.fromBase58(MAINNET, "KGC1RCjiCHfCHdA34RTSwbYNKQEYGz3C3EdR1nXofozvTiLHr1mP").getKey();
        key2 = ECKey.fromPrivate(key2.getPrivKeyBytes());
        ECKey key3 = DumpedPrivateKey.fromBase58(MAINNET, "KFm5pPhVuXDdCWXVuj7SG3g4XwY4iDePvjSRKUBXkJGTWJR2DkF7").getKey();
        key3 = ECKey.fromPrivate(key3.getPrivKeyBytes());

        List<ECKey> keys = Arrays.asList(key1, key2, key3);
        Script p2shScript = ScriptBuilder.createP2SHOutputScript(3, keys);
        Address address = Address.fromScriptHash(MAINNET,
                ScriptPattern.extractHashFromP2SH(p2shScript));
        assertEquals("4gM6h3xYpATZMr7tAHUCftW4uVctfcbksu", address.toString());
    }

    @Test
    public void cloning() throws Exception {
        Address a = Address.fromPubKeyHash(TESTNET, HEX.decode("0a63b5ce1e7fe6ce871528d8ac682d5297f2fe48"));
        Address b = a.clone();

        assertEquals(a, b);
        assertNotSame(a, b);
    }

    @Test
    public void roundtripBase58() throws Exception {
        String base58 = "4DKrc7BEEd8CbJRF3wREov8RqDBfGndiaf";
        assertEquals(base58, Address.fromBase58(null, base58).toBase58());
    }

    @Test
    public void comparisonCloneEqualTo() throws Exception {
        Address a = Address.fromBase58(MAINNET, "4DKrc7BEEd8CbJRF3wREov8RqDBfGndiaf");
        Address b = a.clone();

        int result = a.compareTo(b);
        assertEquals(0, result);
    }

    @Test
    public void comparisonLessThan() throws Exception {
        Address a = Address.fromBase58(MAINNET, "42jEVihde9MqeqgtdKCBYe215UgUrJ545v");
        Address b = Address.fromBase58(MAINNET, "4DKrc7BEEd8CbJRF3wREov8RqDBfGndiaf");

        int result = a.compareTo(b);
        assertTrue(result < 0);
    }

    @Test
    public void comparisonGreaterThan() throws Exception {
        Address a = Address.fromBase58(MAINNET, "4DKrc7BEEd8CbJRF3wREov8RqDBfGndiaf");
        Address b = Address.fromBase58(MAINNET, "42jEVihde9MqeqgtdKCBYe215UgUrJ545v");

        int result = a.compareTo(b);
        assertTrue(result > 0);
    }

    @Test
    public void comparisonBytesVsString() throws Exception {
        // TODO: To properly test this we need a much larger data set
        Address a = Address.fromBase58(MAINNET, "42jEVihde9MqeqgtdKCBYe215UgUrJ545v");
        Address b = Address.fromBase58(MAINNET, "4DKrc7BEEd8CbJRF3wREov8RqDBfGndiaf");

        int resultBytes = a.compareTo(b);
        int resultsString = a.toString().compareTo(b.toString());
        assertTrue( resultBytes < 0 );
        assertTrue( resultsString < 0 );
    }
}
