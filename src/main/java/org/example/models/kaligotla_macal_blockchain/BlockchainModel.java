package org.example.models.kaligotla_macal_blockchain;

import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.Group;
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
    //TransactionListPTQ ptq;
    //PublicLedger pl;

    @Variable(name="Queue Length") public int queueLength;
    @Variable(name="Ledger Length") public int ledgerLength;
    @Variable(name="Total ETH Value in Miners") public int totalETHValueInMiners;
    @Variable(name="Total ETH Value in Markets") public int totalETHValueInMarkets;

    @Override
    public void init() {
        createLongAccumulator("numVerifiedTransactions", "Number of Verified Transactions");
        //createLongAccumulator("numPendingTransactions", "Pending Transactions");
        createLongAccumulator("energyConsumption", "Cumulative Energy Consumption");
        createLongAccumulator("energyConsumedPerVerifiedTransaction", "Energy Consumed per Verified Transaction");
        registerAgentTypes(MarketAgent.class, MinerAgent.class);
        registerLinkTypes(Links.MarketToMinerLink.class);
        registerLinkTypes(Links.MarketToMarketLink.class);
        registerLinkTypes(Links.MinerToMinerLink.class);
        registerLinkTypes(Links.MinerToMarketLink.class);
        registerMessageTypes(Messages.candidateTransactionMessage.class);
    }

    @Override
    public void setup() {
        getGlobals().pl = new PublicLedger();
        getGlobals().ptq = new TransactionListPTQ();
        //pl = new PublicLedger();
        //ptq = new TransactionListPTQ();
        getGlobals().blocksBeingVerified = new ArrayList<>();
        Group<MinerAgent> minerAgentGroup = generateGroup(MinerAgent.class,
                (int) (getGlobals().numAgents* getGlobals().fracMiners),
                minerAgent -> { minerAgent.w=getGlobals().initialMinerAgentBalance;}
        );

        Group<MarketAgent> marketAgentGroup = generateGroup(MarketAgent.class,
                (int) (getGlobals().numAgents* (1-getGlobals().fracMiners)),
                marketAgent -> { marketAgent.w=getGlobals().initialMarketAgentBalance;}
        );
        marketAgentGroup.fullyConnected(marketAgentGroup,   Links.MarketToMarketLink.class);
        marketAgentGroup.fullyConnected(minerAgentGroup,    Links.MarketToMinerLink.class);
        minerAgentGroup.fullyConnected(marketAgentGroup,    Links.MinerToMarketLink.class);
        minerAgentGroup.fullyConnected(minerAgentGroup,     Links.MinerToMinerLink.class);
        super.setup();
    }

    @Override
    public void step() {
        long tick = getContext().getTick();
        if (tick == 0) {
        }
        else {
            run(MarketAgent.generateRandomCandidateTransaction(tick),
                    MarketAgent.addCandidateTransactionsToPTQ(getGlobals().ptq));
            run(MinerAgent.selectNextBlockToVerify(getGlobals().ptq, getGlobals().blockLength));
            run(MinerAgent.verifyBlocksAndInitiateValueTransfer());
            getGlobals().totalETHValueInMiners=0;
            getGlobals().totalETHValueInMarkets=0;
            run(MinerAgent.sumETHValue());
            run(MarketAgent.sumETHValue());


            //run(MarketAgent.sendMessageTransferValue());
            //run(MarketAgent.transferValue());
            //run(MinerAgent.sendGasToMiners());
            //run(MarketAgent.markBlocksAsValueTransferredAndAddToPublicLedgerAndRemoveFromPTQ());

        }
        if (getContext().getTick() == getGlobals().simTime) {
        }
        totalETHValueInMiners=getGlobals().totalETHValueInMiners;
        totalETHValueInMarkets=getGlobals().totalETHValueInMarkets;
        queueLength=getGlobals().ptq.getQueueLength();
        ledgerLength=getGlobals().pl.getNumTransactions();
    }
}