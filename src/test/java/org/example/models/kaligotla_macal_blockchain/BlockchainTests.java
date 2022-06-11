package org.example.models.kaligotla_macal_blockchain;

import org.example.models.kaligotla_macal_blockchain.Utils;
import org.junit.jupiter.api.*;
import simudyne.core.abm.Action;
import simudyne.core.abm.Sequence;
import simudyne.core.abm.testkit.TestKit;
import simudyne.core.abm.testkit.TestResult;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class BlockchainTests {
    private static TestKit<Globals> testKit;
    private static Globals gl;
    @BeforeAll
    public static void init(){
        System.out.println("Running Blockchain Model Tests..");
        testKit = TestKit.create(Globals.class);
         gl= testKit.getGlobals();
    }
    @Test
    public void minersHaveZeroInitialBalance() {
        MinerAgent minerAgent1 = testKit.addAgent(MinerAgent.class, minerAgent -> minerAgent.gl = gl);
        assertEquals(0, minerAgent1.getBalance());
    }
    @Test
    public void marketsHaveZeroInitialBalance() {
        MarketAgent marketAgent1 = testKit.addAgent(MarketAgent.class, marketAgent -> marketAgent.gl = gl);
        assertEquals(0, marketAgent1.getBalance());
    }
    @Test
    public void insufficientFundsForMarketToMarketTransaction() {
        MinerAgent minerAgent1 = testKit.addAgent(MinerAgent.class, minerAgent -> minerAgent.gl = gl);
        MinerAgent minerAgent2 = testKit.addAgent(MinerAgent.class, minerAgent -> minerAgent.gl = gl);
        MarketAgent marketAgentCoinBase = testKit.addAgent(MarketAgent.class, marketAgent -> marketAgent.gl = gl);
        MarketAgent marketAgent1 = testKit.addAgent(MarketAgent.class, marketAgent -> marketAgent.gl = gl);
        MarketAgent marketAgent2 = testKit.addAgent(MarketAgent.class, marketAgent -> marketAgent.gl = gl);
        marketAgentCoinBase.walletAddress = 0;
        marketAgent1.walletAddress = 100;
        marketAgent2.walletAddress = 200;
        gl.coinbaseAgent = marketAgentCoinBase;
        /*Transaction t1 = new Transaction(0,0,100, minerAgent1.walletAddress,
                minerAgent2.walletAddress,
                (int) Globals.random.uniform(0, Globals.maxTransactionId).sample());*/
        ArrayList<Utils.AddressPair> wpa = new ArrayList<>();
        wpa.add(new Utils.AddressPair(marketAgent1.walletAddress, marketAgent2.walletAddress));
        boolean retValue = marketAgent1.generateTransactions(marketAgent1,0, wpa);
        assertEquals(true, retValue);
    }

    @Test
    public void coinbaseToMarketSuccessfulTransaction() {
        MarketAgent marketAgentCoinBase = testKit.addAgent(MarketAgent.class, marketAgent -> marketAgent.gl = gl);
        MarketAgent marketAgent1 = testKit.addAgent(MarketAgent.class, marketAgent -> marketAgent.gl = gl);
        MarketAgent marketAgent2 = testKit.addAgent(MarketAgent.class, marketAgent -> marketAgent.gl = gl);
        MarketAgent marketAgent3 = testKit.addAgent(MarketAgent.class, marketAgent -> marketAgent.gl = gl);
        MarketAgent marketAgent4 = testKit.addAgent(MarketAgent.class, marketAgent -> marketAgent.gl = gl);
        MinerAgent minerAgent1 =  testKit.addAgent(MinerAgent.class, minerAgent -> minerAgent.gl = gl);
        //MarketAgent marketAgent5 = testKit.addAgent(MarketAgent.class, marketAgent -> marketAgent.gl = gl);
        marketAgentCoinBase.walletAddress = 0;
        marketAgent1.walletAddress = 100;
        marketAgent2.walletAddress = 200;
        marketAgent3.walletAddress = 300;
        marketAgent4.walletAddress = 400;
        //marketAgent5.walletAddress = 500;
        minerAgent1.walletAddress = 600;
        gl.coinbaseAgent = marketAgentCoinBase;
        gl.marketWalletAddresses = new Utils.AddressAgentMap();
        testKit.testAction(marketAgent1,MarketAgent.fillUpMarketWalletAddressArray());
        testKit.testAction(marketAgent2,MarketAgent.fillUpMarketWalletAddressArray());
        testKit.testAction(marketAgent3,MarketAgent.fillUpMarketWalletAddressArray());
        testKit.testAction(marketAgent4,MarketAgent.fillUpMarketWalletAddressArray());
        testKit.testAction(marketAgentCoinBase, MarketAgent.sendMoneyFromCoinbaseToMarkets());/*
        testKit.testAction(marketAgentCoinBase, MarketAgent.broadcastTransactionsToMiners());*/
        testKit.testAction(minerAgent1,MinerAgent.updateMinerLedger());
        //testKit.testAction(minerAgent1,MinerAgent.addCandidateTransactionsToPTQ());
        /*testKit.testAction(minerAgent1,MinerAgent.createAndBroadcastCandidateBlocks());
        testKit.testAction(minerAgent1,MinerAgent.receiveCandidateBlocks());*/
        System.out.println("Miner PTQ Size:" + minerAgent1.getPTQ().getQueueLength());
        testKit.testAction(minerAgent1,MinerAgent.verifyCandidateBlocksAndWriteToLedger());
        testKit.testAction(minerAgent1,MinerAgent.updateMinerLedger());
        testKit.testAction(marketAgent2,MarketAgent.updateLedger());
        System.out.println("gl:" + gl.marketWalletAddresses.getAddresses());
        System.out.println("gl:" + gl.marketWalletAddresses.size());
        System.out.println("gl:" + minerAgent1.pl.toString());
        assertNotEquals(0, marketAgent2.getBalance());
    }
}