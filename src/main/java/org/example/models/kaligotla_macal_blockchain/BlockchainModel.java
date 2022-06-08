package org.example.models.kaligotla_macal_blockchain;

import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.Group;
import simudyne.core.abm.Sequence;
import simudyne.core.annotations.ModelSettings;
import simudyne.core.annotations.Variable;
import java.util.ArrayList;
import java.util.Random;

/*
    Implementation of "A GENERALIZED AGENT BASED FRAMEWORK FOR MODELING A BLOCKCHAIN SYSTEM"
    Chaitanya Kaligotla, Charles M Macal (2018)
    Decision and Infrastructure Sciences. Argonne National Laboratory
    https://www.informs-sim.org/wsc18papers/includes/files/083.pdf
*/

@ModelSettings(timeUnit = "SECONDS", end=15*4)
public class BlockchainModel extends AgentBasedModel<Globals> {
    @Variable(name="Queue Length") public int queueLength;
    @Variable(name="Public Ledger Length") public int ledgerLength;
    @Variable(name="Total ETH Value in Miners") public int totalETHValueInMiners;
    @Variable(name="Total ETH Value in Markets") public int totalETHValueInMarkets;
    Globals gl;

    @Override
    public void init() {
        createLongAccumulator("energyConsumption", "Cumulative Energy Consumption");
        createLongAccumulator("energyConsumedPerVerifiedTransaction", "Energy Consumed per Verified Transaction");
        registerAgentTypes(MarketAgent.class, MinerAgent.class);
        registerLinkTypes(Links.MarketToMinerLink.class);
        registerLinkTypes(Links.MarketToMarketLink.class);
        registerLinkTypes(Links.MinerToMinerLink.class);
        registerLinkTypes(Links.MinerToMarketLink.class);
        registerLinkTypes(Links.MinerToBlockLink.class);
        registerMessageTypes(Messages.candidateTransactionMessage.class);
    }

    @Override
    public void setup() {
        gl = getGlobals();
        Group<MinerAgent> minerAgentGroup = generateGroup(MinerAgent.class,
                (int) (gl.numAgents*getGlobals().fracMiners),
                minerAgent -> { minerAgent.w=gl.initialMinerAgentBalance;
                                minerAgent.gl=gl;
                                minerAgent.createBlockList();
                                }
        );
        Group<MarketAgent> marketAgentGroup = generateGroup(MarketAgent.class,
                (int) (gl.numAgents*(1-gl.fracMiners)),
                marketAgent -> {    marketAgent.w=gl.initialMarketAgentBalance;
                                    marketAgent.gl=gl;});
        marketAgentGroup.fullyConnected(marketAgentGroup,   Links.MarketToMarketLink.class);
        marketAgentGroup.fullyConnected(minerAgentGroup,    Links.MarketToMinerLink.class);
        minerAgentGroup.fullyConnected(marketAgentGroup,    Links.MinerToMarketLink.class);
        minerAgentGroup.fullyConnected(minerAgentGroup,     Links.MinerToMinerLink.class);
        super.setup();
    }

    @Override
    public void step() {
        long tick = getContext().getTick();
        System.out.println("TICK: " + tick);
        System.out.println("============");
        if (tick == 0) {
            run(WalletAgent.assignWalletAddress());
            run(MarketAgent.fillUpMarketWalletAddressArray());
            System.out.println("Market Wallet Addresses: " + gl.marketWalletAddresses);
            run(MarketAgent.assignCoinbaseAddress());
            System.out.println("Coinbase Address: " + gl.coinbaseAgent.walletAddress);
            System.out.println("Coinbase Agent: "   + gl.coinbaseAgent);
            run(MarketAgent.provideInitialBalanceToCoinbase());
            run(MarketAgent.sendMoneyFromCoinbaseToMarkets(0));
        }
        else {
            ArrayList<WalletPair> wp = generateTransactionPairs(20);
            run(
                    Sequence.create(MarketAgent.generateRandomTransactions(tick, wp)),
                    Sequence.create(MarketAgent.broadcastTransactionsToMiners(),
                                    MinerAgent.addCandidateTransactionsToPTQ()));
            run(
                    Sequence.create(MinerAgent.createAndBroadcastCandidateBlocks(),
                                    MinerAgent.receiveCandidateBlocks()));
            run(
                    Sequence.create(MinerAgent.verifyCandidateBlocksAndTransferValueAndGas(),
                                    MarketAgent.receiveTransferValue(),
                                    MinerAgent.updateLedger()));
            run(MinerAgent.sumETHValue());
            run(MarketAgent.sumETHValue());
            run(MinerAgent.calculateQueueLength());
            run(MinerAgent.calculateLedgerLength());
        }
        if (getContext().getTick() == gl.simTime) {
        }
        totalETHValueInMiners=gl.totalETHValueInMiners;
        totalETHValueInMarkets=gl.totalETHValueInMarkets;
        queueLength=gl.queueLength;
        ledgerLength=gl.ledgerLength;
        gl.totalETHValueInMiners=0;
        gl.totalETHValueInMarkets=0;
    }

    public ArrayList<WalletPair> generateTransactionPairs(int numPairs)
    {
        ArrayList<WalletPair> wpa = new ArrayList<>();
        ArrayList<Integer> walletAddresses = (ArrayList<Integer>)gl.marketWalletAddresses.clone();
        Random rand = new Random();
        Integer origin, destination;
        System.out.println("numPairs=" + numPairs);
        for (int i=0;i<numPairs;i++)
        {
            origin = walletAddresses.get(rand.nextInt(walletAddresses.size()));
            walletAddresses.remove(origin);
            destination = walletAddresses.get(rand.nextInt(walletAddresses.size()));
            walletAddresses.remove(destination);
            WalletPair wp = new WalletPair(origin,destination);
            System.out.println("wp:" + wp.originWallet + "," + wp.destinationWallet);
            wpa.add(wp);
        }
        return wpa;
    }

    static class WalletPair
    {
        public int originWallet;
        public int destinationWallet;
        WalletPair(int o, int d)
        {
            originWallet = o;
            destinationWallet = d;
        }
    }
}