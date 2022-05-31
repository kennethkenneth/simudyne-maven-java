package org.example.models.kaligotla_macal_blockchain;

import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.Group;
import simudyne.core.annotations.ModelSettings;
import simudyne.core.annotations.Variable;

/*
    Implementation of "A GENERALIZED AGENT BASED FRAMEWORK FOR MODELING A BLOCKCHAIN SYSTEM"
    Chaitanya Kaligotla, Charles M Macal (2018)
    Decision and Infrastructure Sciences. Argonne National Laboratory
    https://www.informs-sim.org/wsc18papers/includes/files/083.pdf
*/

@ModelSettings(timeUnit = "SECONDS", end=15*4)
public class BlockchainModel extends AgentBasedModel<Globals> {
    TransactionListPTQ ptq;

    PublicLedger pl;

    @Variable public int queueLength;

    //@Variable public int verifiedTransactions;

    @Override
    public void init() {
        createLongAccumulator("numVerifiedTransactions", "Verified Transactions");
        createLongAccumulator("numPendingTransactions", "Pending Transactions");
        createLongAccumulator("energyConsumption", "Cumulative Energy Consumption");
        createLongAccumulator("energyConsumedPerVerifiedTransaction", "Energy Consumed per Verified Transaction");
        registerAgentTypes(MarketAgent.class, MinerAgent.class);
        registerLinkTypes(Links.MarketToMinerLink.class);
        registerLinkTypes(Links.MarketToMarketLink.class);
        registerMessageTypes(Messages.candidateTransactionMessage.class);
        registerLinkTypes(Links.MinerToMinerLink.class);
    }

    @Override
    public void setup() {
        pl = new PublicLedger();
        ptq = new TransactionListPTQ();
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
        minerAgentGroup.fullyConnected(marketAgentGroup,    Links.MarketToMinerLink.class);
        minerAgentGroup.fullyConnected(minerAgentGroup,     Links.MinerToMinerLink.class);
        super.setup();
    }

    @Override
    public void step() {
        long tick = getContext().getTick();
        if (tick == 0) {

        }
        else {
            run(  MarketAgent.generateRandomCandidateTransaction(tick)
                , MarketAgent.addCandidateTransactionsToPTQ(ptq));
            run( MinerAgent.selectNextBlockToVerify(ptq, getGlobals().blockLength));
        }
        if (getContext().getTick() == getGlobals().simTime) {
        }
        queueLength=ptq.getQueueLength();
        //verifiedTransactions = pl.getNumTransactions();
    }
}