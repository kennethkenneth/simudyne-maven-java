package org.example.models.kaligotla_macal_blockchain;

import java.util.Comparator;
import java.util.PriorityQueue;

/*
PTQ - Pending Transaction Queue. A vector of unverified transactions.
PTQt = [τ1,τ2,...,τn],where τ(σ)!= 1
 */
public class TransactionListPTQ  {
    private final PriorityQueue<Transaction> ptq;
    private static class CompTransaction implements Comparator<Transaction>
    {
        public int compare(Transaction t1, Transaction t2)
        {
            if (t1.gas>t2.gas)
            {
                return -1;
            }
            else if (t1.gas<t2.gas)
            {
                return 1;
            }
            else // (t1.gas==t2.gas)
            {
                return Long.compare(t2.tCreate, t1.tCreate);
            }
        }
    }
    public TransactionListPTQ()
    {
        ptq = new PriorityQueue(2000, new CompTransaction());
    }

    //peek()	Retrieves, but does not remove, the head of this queue, or returns null if this queue is empty.
    //poll()	Retrieves and removes the head of this queue, or returns null if this queue is empty.
    public Transaction poll()
    {
        return ptq.poll();
    }

    public void enqueueTransaction(Transaction t) {
        if (!ptq.contains(t) && t.isVerified())
        {
            ptq.add(t);
        }
    }

    public int getQueueLength()
    {
        return ptq.size();
    }

    public void removeTransaction(Transaction tra)
    {
        ptq.remove(tra);
    }

    public void removeTransactionsOfBlock(Block block) {
        block.getTransactions().forEach(this::removeTransaction);
    }
}
