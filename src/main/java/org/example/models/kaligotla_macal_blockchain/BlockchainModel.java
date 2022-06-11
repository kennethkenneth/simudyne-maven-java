package org.example.models.kaligotla_macal_blockchain;

import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.Group;
import simudyne.core.abm.Sequence;
import simudyne.core.abm.Split;
import simudyne.core.annotations.ModelSettings;
import simudyne.core.annotations.Variable;

import java.util.ArrayList;

/*
    Implementation of "A GENERALIZED AGENT BASED FRAMEWORK FOR MODELING A BLOCKCHAIN SYSTEM"
    Chaitanya Kaligotla, Charles M Macal (2018)
    Decision and Infrastructure Sciences. Argonne National Laboratory
    https://www.informs-sim.org/wsc18papers/includes/files/083.pdf
*/



@ModelSettings(timeUnit = "SECONDS", end=15*4)
public class BlockchainModel extends AgentBasedModel<Globals> {
    @Variable(name="Queue Length (txs)") public int queueLength;
    @Variable(name="Public Ledger Length (txs)") public int ledgerLength;
    @Variable(name="Total ETH Value in Miners") public int totalETHValueInMiners;
    @Variable(name="Total ETH Value in Markets") public int totalETHValueInMarkets;
    Globals gl;

    @Override
    public void init() {
        createLongAccumulator("energyConsumption", "Cumulative Energy Consumption");
        createLongAccumulator("energyConsumedPerVerifiedTransaction", "Energy Consumed per Verified Transaction");
        registerAgentTypes(MarketAgent.class, MinerAgent.class);
        registerLinkTypes(Links.MarketToMinerLink.class);
        registerLinkTypes(Links.MarketToMarketLink.class); //TODO: Deprecate
        registerLinkTypes(Links.MinerToMinerLink.class);
        registerLinkTypes(Links.MinerToMarketLink.class);
        registerLinkTypes(Links.MinerToBlockLink.class);
        registerMessageTypes(Messages.msgTransaction.class);
        registerMessageTypes(Messages.msgBlock.class);
        registerMessageTypes(Messages.msgCandidateBlock.class);
    }

    @Override
    public void setup() {
        gl = getGlobals();
        Group<MinerAgent> minerAgentGroup = generateGroup(MinerAgent.class,
                (int) (gl.numAgents*gl.fracMiners),
                minerAgent -> minerAgent.gl=gl);
        Group<MarketAgent> marketAgentGroup = generateGroup(MarketAgent.class,
                (int) (gl.numAgents*(1-gl.fracMiners)),
                marketAgent -> marketAgent.gl=gl);
        marketAgentGroup.fullyConnected(minerAgentGroup, Links.MarketToMinerLink.class);
        minerAgentGroup.fullyConnected(marketAgentGroup, Links.MinerToMarketLink.class);
        minerAgentGroup.fullyConnected(minerAgentGroup, Links.MinerToMinerLink.class);
        marketAgentGroup.fullyConnected(marketAgentGroup, Links.MarketToMarketLink.class);
        super.setup();
    }

    @Override
    public void step() {
        String setBold = "\033[1m";
        String setNormal = "\033[0m";
        long tick = getContext().getTick();
        System.out.println(setBold + "TICK: " + tick + setNormal);
        System.out.println(setBold + "============" + setNormal);
        if (tick == 0) {
            run(WalletAgent.assignWalletAddress());
            run(MarketAgent.fillUpMarketWalletAddressArray());
            run(MinerAgent.fillUpMinerWalletAddressArray());
            System.out.println("Market Addresses (" + gl.marketWalletAddresses.size() + "): " + gl.marketWalletAddresses);
            System.out.println("Miner Addresses (" + gl.minerWalletAddresses.size() + "): " + gl.minerWalletAddresses);
            run(MarketAgent.assignCoinbaseAddress());   //Coin Base is just another Market Wallet
            System.out.println("Coin Base Address: " + gl.coinbaseAgent.walletAddress);
            System.out.println("Coin Base Agent: "   + gl.coinbaseAgent);
            run(Sequence.create(MarketAgent.sendMoneyFromCoinbaseToMarkets(),
                                Split.create(MinerAgent.updateMinerLedger(),
                                            MarketAgent.updateMarketLedger())));
        }
        else {
            run(Sequence.create(MarketAgent.generateRandomTransactions(tick)),
                Sequence.create(MinerAgent.addCandidateTransactionsToPTQ()));
            run(Sequence.create(MinerAgent.createAndBroadcastCandidateBlocks(),
                                MinerAgent.receiveCandidateBlocks()));
            run(Sequence.create(MinerAgent.verifyCandidateBlocksAndWriteToLedger(),
                                MinerAgent.updateMinerLedger(),
                                MarketAgent.updateMarketLedger()));
            gl.ledgerBlocks = setBold + "\nMiner Agents" + setNormal + " (" +  gl.minerWalletAddresses.size() + "): ";
            run(MinerAgent.sumETHValue());
            gl.ledgerBlocks = gl.ledgerBlocks + setBold + "\n\nMarket Agents" + setNormal + " (" +  gl.marketWalletAddresses.size() + "): ";
            run(MarketAgent.sumETHValue());
            run(MinerAgent.calculateQueueLength());
            run(MinerAgent.calculateLedgerLength());
        }
        if (getContext().getTick() == gl.simTime) {
        }
        System.out.println(gl.ledgerBlocks);
        gl.ledgerBlocks = "";
        totalETHValueInMiners=gl.totalETHValueInMiners;
        totalETHValueInMarkets=gl.totalETHValueInMarkets;
        queueLength=gl.queueLength;
        ledgerLength=gl.ledgerLength;
        gl.totalETHValueInMiners=0;
        gl.totalETHValueInMarkets=0;
    }
}