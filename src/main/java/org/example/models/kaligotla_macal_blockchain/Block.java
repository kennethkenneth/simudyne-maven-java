package org.example.models.kaligotla_macal_blockchain;

import java.util.ArrayList;

public class Block {
    private ArrayList<Transaction> trans;

    private ArrayList<MinerAgent> verifiers;

    private boolean blockVerified;

    private boolean gasPaidToVerifiers;

    private int totalGas = 0;

    private boolean hasValueBeenTransferred;

    public boolean isBlockVerified()
    {
        return this.blockVerified;
    }

    public void markBlockAsVerifierBy(MinerAgent ma)
    {
        this.verifiers.add(ma);
    }

    public int getTotalGas()
    {
        return totalGas;
    }

    public void markBlockAsVerified()
    {
        this.blockVerified = true;
    }

    public Block()
    {
        trans = new ArrayList();                // Replace 5 with globals.XXX
        verifiers = new ArrayList<>();
        blockVerified = false;
        gasPaidToVerifiers = false;
    }

    public void setGas(int totalGas)
    {
        this.totalGas = totalGas;
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

    public boolean hasGasBeenPaidToMiners()
    {
        return this.gasPaidToVerifiers;
    }

    public boolean hasValueBeenTransferred()
    {
        return this.hasValueBeenTransferred;
    }

    public void markBlockHavingValueTransferred()
    {
        this.hasValueBeenTransferred = true;
    }

    public void payGasToMiners()
    {
        verifiers.forEach(ma -> {
            ma.sendGasToMiners();
        });
        this.gasPaidToVerifiers = true;
    }

    public int getNumAgentsVerified()
    {
        return verifiers.size();
    }
}
