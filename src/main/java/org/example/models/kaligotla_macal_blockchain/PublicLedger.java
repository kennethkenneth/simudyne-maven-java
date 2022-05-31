package org.example.models.kaligotla_macal_blockchain;

import java.util.LinkedList;

public class PublicLedger {
    /*
    Public Ledger is a vector of verified transactions, which were previously in the Pending Transaction Queue.
    Lt = [τ1,τ2,...] where all τ(σ) = 1 are verified.
    When the candidate block is verified by at least μ other agents, and the candidate block
    is valid (none of the transactions in that candidate block have already been added to the Public Ledger),
    then the candidate block gets added to the Public Ledger
     */

    private final LinkedList<Transaction> l = new LinkedList<>();

    public void addBlock(Block b)
    {
        Transaction [] tr = b.getTransactions();
        for (int i = 0; i< tr.length;i++)
        {
            l.addLast(tr[i]);
        }
    }

    public int getNumTransactions()
    {
        return l.size();
    }
}
