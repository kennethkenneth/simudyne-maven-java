package org.example.models.kaligotla_macal_blockchain;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.graph.FilteredLinks;

public class MarketAgent extends Agent<Globals>{
    int w; //agents’ current balance of currency or value

    public static Action<MarketAgent> addCandidateTransactionsToPTQ(TransactionListPTQ ptq) {
        return Action.create(MarketAgent.class, curMA -> {
            if (curMA.hasMessagesOfType(Messages.candidateTransactionMessage.class))
            {
                System.out.println("Adding candidate transaction to PTQ");
                Messages.candidateTransactionMessage msg =
                        curMA.getMessageOfType(Messages.candidateTransactionMessage.class);
                ptq.enqueueTransaction(new Transaction(msg.createTick, msg.gas, msg.value, msg.sender, curMA));
            }
        });
    }


    public static Action<MarketAgent> sumETHValue()
    {
        return Action.create(MarketAgent.class, currMA -> {
            Globals gl = currMA.getGlobals();
            gl.totalETHValueInMarkets+=currMA.w;
            return;
        });
    }
/*
    public static Action<MarketAgent> markBlocksAsValueTransferredAndAddToPublicLedgerAndRemoveFromPTQ()
    {
        return Action.create(MarketAgent.class, curMA -> {
            ArrayList<Block> blocks = new ArrayList<>();
            AtomicLong nbrTransfersDone = new AtomicLong();
            curMA.getGlobals().blocksBeingVerified
                .stream()
                .filter(b->b.isBlockVerified())
                .filter(b->!b.hasValueBeenTransferred())
                .forEach(b-> {
                    long transfersDone = Arrays.stream(b.getTransactions()).filter(t -> t.transferDone).count();
                    if (transfersDone>0)
                    {
                        System.out.println("nbrTransfersDone: " + nbrTransfersDone);
                        System.out.println("Adding: " + Arrays
                                .stream(b.getTransactions()).filter(t -> t.transferDone).count());
                    }
                    nbrTransfersDone.addAndGet(Arrays.stream(b.getTransactions())
                            .filter(t -> t.transferDone).count());
                    if (nbrTransfersDone.longValue()==curMA.getGlobals().blockLength)
                    {
                        System.out.println("==============");
                        b.markBlockHavingValueTransferred();
                        curMA.getGlobals().pl.addBlock(b);
                        blocks.add(b);
                    }
                });
                blocks.forEach(b-> {
                    for(Transaction t:b.getTransactions()) {
                        curMA.getGlobals().ptq.removeTransaction(t);
                    }
                    curMA.getGlobals().blocksBeingVerified.remove(b);
                });
            });
    }*/

    /*public static Action<MarketAgent> transferValue()
    {
        return Action.create(MarketAgent.class, curMA -> {
            curMA.getGlobals().blocksBeingVerified
                .stream()
                .filter(b->b.isBlockVerified())
                .filter(b->!b.hasValueBeenTransferred())
                .forEach(b->{
                    Arrays.stream(b.getTransactions())
                    .filter(t->(t.agentJ==curMA && !t.transferDone))
                    .forEach(t->{
                        curMA.w+=t.value;
                        t.markTransferAsDone();
                    });});
            return;
        });
    }*/

    public static Action<MarketAgent> generateRandomCandidateTransaction(long tick) {
        return Action.create(MarketAgent.class, curMA -> {
            Globals gl = curMA.getGlobals();
            if (curMA.getPrng().uniform(0, 1).sample() <= gl.probAgentTransacting)
            {
                FilteredLinks l = curMA.getLinks(Links.MarketToMarketLink.class);
                if (l.size()>0)
                {
                    int gas = (int) curMA.getPrng().uniform(0,10).sample();         // TODO: Improve
                    int value = (int) curMA.getPrng().uniform(0,100).sample();      // TODO: Improve
                    if (curMA.w > gas + value)
                    {
                        System.out.println("No Idea...");
                        curMA.w-=(gas+value);
                        curMA.getLinks(Links.MarketToMarketLink.class)
                                .send(Messages.candidateTransactionMessage.class, (message, link) -> {
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

    /*public static Action<MarketAgent> sendMessageTransferValue()
    {
        return Action.create(MarketAgent.class, curMA -> {
            curMA.getMessagesOfType(Messages.TransferAmount.class)
                    .stream()
                    .filter(m->m.receiver == curMA)
                    .forEach(
                            msg -> {
                                curMA.w+= msg.amount;
                            }
                    );
            return;
        });
    }*/

    /*public static Action<MarketAgent> transferValue()
    {
        return Action.create(MarketAgent.class, curMA -> {
            curMA.getMessagesOfType(Messages.TransferAmount.class)
                    .stream()
                    .filter(m->m.receiver == curMA)
                    .forEach(
                            msg -> {
                                curMA.w+= msg.amount;
                            }
                    );
            return;
        });
    }*/
}