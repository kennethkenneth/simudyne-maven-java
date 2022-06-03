package org.example.models.kaligotla_macal_blockchain;
import java.util.LinkedList;

/*
PTQ - Pending Transaction Queue. A vector of unverified transactions.
PTQt = [τ1,τ2,...,τn],where τ(σ)!= 1
 */
public class TransactionListPTQ  {
    private final LinkedList<Transaction> ptq;
    private int queueLength; // log how many in queue currently, for UI outputs

    public TransactionListPTQ()
    {
        ptq = new LinkedList<>();
        queueLength = 0;
    }

    public Transaction get(int i)
    {
        if (!ptq.isEmpty()) {
            return ptq.get(i);
        }
        return null;
    }

    public void enqueueTransaction(Transaction t) {
        ptq.addLast(t);
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
}
