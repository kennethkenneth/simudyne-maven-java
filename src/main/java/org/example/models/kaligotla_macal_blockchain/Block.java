package org.example.models.kaligotla_macal_blockchain;

import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;

public class Block {

    private ArrayList<Transaction> trans;
    private int numAgentsVerified = 0;

    public Block()
    {
        trans = new ArrayList<Transaction>(); // Replace 5 with globals.XXX
    }

    public Transaction[] getTransactions()
    {
        return trans.toArray(new Transaction[trans.size()]);
    }

    public int getSize()
    {
        return trans == null? 0: trans.size();
    }

    public void addTrans(Transaction t)
    {
        if (getSize()<5) {      // TODO: Replace with Globals...
            trans.add(t);
        }
    }
}
