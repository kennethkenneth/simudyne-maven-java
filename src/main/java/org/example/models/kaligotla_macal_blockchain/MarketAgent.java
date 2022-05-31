package org.example.models.kaligotla_macal_blockchain;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.graph.FilteredLinks;

public class MarketAgent extends Agent<Globals>{
    int i;  //unique identifier for an agent    --- TODO: Do we need this?
    int w; //agents’ current balance of currency or value

    public static Action<MarketAgent> step() {
        return null;
    }

    public static Action<MarketAgent> addCandidateTransactionsToPTQ(TransactionListPTQ ptq) {
        return Action.create(MarketAgent.class, curMA -> {
            if (curMA.hasMessagesOfType(Messages.candidateTransactionMessage.class))
            {
                Messages.candidateTransactionMessage msg = curMA.getMessageOfType(Messages.candidateTransactionMessage.class);
                if (curMA.w>msg.value + msg.gas) {
                    Transaction t = new Transaction(msg.createTick, msg.gas, msg.value, msg.sender, curMA);
                    curMA.w-=(msg.value+msg.gas);
                    ptq.enqueueTransaction(t);
                }
            }
        });
    }

    public static Action<MarketAgent> generateRandomCandidateTransaction(long tick) {
        return Action.create(MarketAgent.class, curMA -> {
            Globals gl = curMA.getGlobals();
            if (curMA.getPrng().uniform(0, 1).sample() <= gl.probAgentTransacting)
            {
                FilteredLinks l = curMA.getLinks(Links.MarketToMarketLink.class);
                if (l.size()>0)
                {
                    int nbr = (int) curMA.getPrng().uniform(0, l.size()).sample();
                    float nbr2 = l.get(nbr).getVertex().getID();
                    curMA.getLinks(Links.MarketToMarketLink.class)
                            //.filter(trLink -> trLink.getTo()==nbr2)
                            .send(Messages.candidateTransactionMessage.class, (message, link) -> {
                                message.gas = (int) curMA.getPrng().uniform(0,10).sample();    //TODO: Improve
                                message.sender = curMA;
                                message.value = (int) curMA.getPrng().uniform(0,100).sample(); //TODO: Improve
                                message.createTick = tick;
                            });
                }
            }
        });
    }

    public static Action<MarketAgent> transferValueTo(Transaction t)
    {
        return null;
    }
}