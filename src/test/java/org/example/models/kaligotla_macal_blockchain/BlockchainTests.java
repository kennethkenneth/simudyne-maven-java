package org.example.models.kaligotla_macal_blockchain;

import org.junit.jupiter.api.*;
import simudyne.core.abm.Group;
import simudyne.core.abm.testkit.TestKit;
import simudyne.core.abm.Group;
import simudyne.core.abm.AgentBasedModel;

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
        testKit.registerLinkTypes(Links.MarketToMinerLink.class);
    }
    @BeforeAll
    public static void setup()
    {

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
        BlockchainModel bm = new BlockchainModel();
        marketAgentCoinBase.walletAddress = 0;
        marketAgent1.walletAddress = 1;
        marketAgent2.walletAddress = 2;
        marketAgent3.walletAddress = 3;
        marketAgent4.walletAddress = 4;
        minerAgent1.walletAddress = 5;
        gl.coinbaseAgent = marketAgentCoinBase;
        gl.marketWalletAddresses = new Utils.AddressAgentMap();
        testKit.testAction(marketAgent1,MarketAgent.fillUpMarketWalletAddressArray());
        testKit.testAction(marketAgent2,MarketAgent.fillUpMarketWalletAddressArray());
        testKit.testAction(marketAgent3,MarketAgent.fillUpMarketWalletAddressArray());
        testKit.testAction(marketAgent4,MarketAgent.fillUpMarketWalletAddressArray());
        testKit.testAction(marketAgentCoinBase, MarketAgent.sendMoneyFromCoinbaseToMarkets());
        System.out.println("marketAgentCoinBase gl:" + marketAgentCoinBase.pl.toString());
        System.out.println("1st block:" + marketAgentCoinBase.pl.getBlock(0,0).toString());
        testKit.send(Messages.msgBlock.class,m->{m.block=marketAgentCoinBase.pl.getBlock(0,0);}).to(minerAgent1);
        testKit.send(Messages.msgBlock.class,m->{m.block=marketAgentCoinBase.pl.getBlock(0,0);}).to(marketAgent1);
        testKit.send(Messages.msgBlock.class,m->{m.block=marketAgentCoinBase.pl.getBlock(0,0);}).to(marketAgent2);
        testKit.testAction(minerAgent1,MinerAgent.updateMinerLedger());
        testKit.testAction(marketAgent2,MarketAgent.updateMarketLedger());
        System.out.println("gl:" + gl.marketWalletAddresses.getAddresses());
        System.out.println("gl:" + gl.marketWalletAddresses.size());
        System.out.println("minerAgent1 gl:" + minerAgent1.pl.toString());
        System.out.println("minerAgent1 gl 1st Block:" + minerAgent1.pl.getBlock(0,0).toString());
        assertEquals(gl.initialMarketAgentBalance, marketAgent2.getBalance());
        assertEquals(-4*gl.initialMarketAgentBalance, marketAgentCoinBase.getBalance());
    }
}