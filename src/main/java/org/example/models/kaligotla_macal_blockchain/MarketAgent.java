package org.example.models.kaligotla_macal_blockchain;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.graph.FilteredLinks;

public class MarketAgent extends Agent<Globals>{
    int w; //agents’ current balance of currency or value
    Globals gl;
    public static Action<MarketAgent> sumETHValue()
    {
        return Action.create(MarketAgent.class, currMA -> {
            Globals gl = currMA.getGlobals();
            gl.totalETHValueInMarkets+=currMA.w;
        });
    }

    public static Action<MarketAgent> broadcastTransactionsToMiners() {
        return Action.create(MarketAgent.class, curMarketAgent -> {
            if (curMarketAgent.hasMessagesOfType(Messages.candidateTransactionMessage.class))
            {
                Messages.candidateTransactionMessage msg =
                        curMarketAgent.getMessageOfType(Messages.candidateTransactionMessage.class);
                curMarketAgent.getLinks(Links.MarketToMinerLink.class)
                        .send(Messages.broadcastTransactionsToMinersPTQ.class, (message, link) -> {
                            message.gas = msg.gas;
                            message.sender = msg.sender;
                            message.receiver = curMarketAgent;
                            message.value = msg.value;
                            message.createTick = msg.createTick;
                        });
            }
        });
    }

    public static Action<MarketAgent> generateRandomCandidateTransaction(long tick) {
        return Action.create(MarketAgent.class, curMA -> {
            Globals gl = curMA.getGlobals();
            if (curMA.getPrng().uniform(0, 1).sample() <= gl.probAgentTransacting)
            {
                FilteredLinks<Links.MarketToMarketLink> l = curMA.getLinks(Links.MarketToMarketLink.class);
                if (l.size()>0)
                {
                    int gas = (int) curMA.getPrng().uniform(0,gl.gasFee).sample();         // TODO: Improve
                    int value = (int) curMA.getPrng().uniform(0,100).sample();          // TODO: Improve
                    if (curMA.w > gas + value)
                    {
                        curMA.w-=(gas+value);
                        //TO-DO: Take just one
                        l.send(Messages.candidateTransactionMessage.class, (message, link) -> {
                                    message.gas = gas;
                                    message.sender = curMA;
                                    message.value = value;
                                    message.createTick = tick;
                                });
                    }
                }
            }
        });
    }
}