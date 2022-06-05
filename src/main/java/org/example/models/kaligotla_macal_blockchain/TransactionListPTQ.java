package org.example.models.kaligotla_macal_blockchain;
import simudyne.core.abm.Action;

import java.util.ArrayList;
import java.util.LinkedList;

/*
PTQ - Pending Transaction Queue. A vector of unverified transactions.
PTQt = [τ1,τ2,...,τn],where τ(σ)!= 1
 */
public class TransactionListPTQ  {
    private final ArrayList<Transaction> ptq;
    private int queueLength; // log how many in queue currently, for UI outputs

    public TransactionListPTQ()
    {
        ptq = new ArrayList();
        queueLength = 0;
    }

    public Transaction get(int i)
    {
        if (!ptq.isEmpty() && ptq.size()>i) {
            return ptq.get(i);
        }
        return null;
    }

    public ArrayList getPTQ()
    {
        return ptq;
    }

    public void enqueueTransaction(Transaction t) {
        if (!ptq.contains(t))
        {
            ptq.add(t);
        }
        queueLength+=1;
    }

    public int getQueueLength()
    {
        return queueLength;
    }

    public void removeTransaction(Transaction tra)
    {
        ptq.remove(tra);
    }

    public void removeTransactionsOfBlock(Block block) {
        block.getTransactions().forEach(trans -> {
            removeTransaction(trans);
        });
    }
}
