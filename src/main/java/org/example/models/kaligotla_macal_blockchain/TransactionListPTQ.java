package org.example.models.kaligotla_macal_blockchain;

import java.util.Comparator;
import java.util.PriorityQueue;

/*
PTQ - Pending Transaction Queue. A vector of unverified transactions.
PTQt = [τ1,τ2,...,τn],where τ(σ)!= 1
 */
public class TransactionListPTQ  {
    //private final ArrayList<Transaction> ptq;
    private final PriorityQueue<Transaction> ptq;
    //private int queueLength; // log how many in queue currently, for UI outputs

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
        //queueLength = 0;
    }

    //peek()	Retrieves, but does not remove, the head of this queue, or returns null if this queue is empty.
    //poll()	Retrieves and removes the head of this queue, or returns null if this queue is empty.
    public Transaction poll()
    {
        return ptq.poll();
        /*Transaction trans;
        if (ptq.size()>0)
        {
            do {
                trans = ptq.poll();
            } while (!trans.isVerified() && ptq.size()>0);
            if (trans.isVerified()) {
                //Queue was not null, return the first verified transaction
                return trans;
            }
            else {
                //Queue was not null, but no verified transactions were found
                return null;
            }
        }
        else {
            // Queue was null to begin with
            return null;
        }*/
    }

    public void enqueueTransaction(Transaction t) {
        if (!ptq.contains(t) && t.isVerified())
        {
            ptq.add(t);
        }
        //queueLength+=1;
    }

    public int getQueueLength()
    {
        return ptq.size();
        //return queueLength;
    }

    public void removeTransaction(Transaction tra)
    {
        ptq.remove(tra);
    }

    public void removeTransactionsOfBlock(Block block) {
        block.getTransactions().forEach(this::removeTransaction);
    }
}
