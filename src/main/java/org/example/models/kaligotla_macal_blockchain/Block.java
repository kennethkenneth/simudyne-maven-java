package org.example.models.kaligotla_macal_blockchain;

import java.util.ArrayList;

public class Block {
    private ArrayList<Transaction> trans;

    private ArrayList<MinerAgent> verifiers;

    private ArrayList<MinerAgent> gasPaidTo;

    private boolean blockVerified;

    private boolean gasPaidToVerifiers;

    private int totalGas;

    private boolean hasValueBeenTransferred;

    public boolean isBlockVerified()
    {
        return this.blockVerified;
    }

    public void markBlockAsVerifiedBy(MinerAgent ma)
    {
        this.verifiers.add(ma);
    }

    public int getTotalGas()
    {
        return this.totalGas;
    }

    public void markBlockAsVerified()
    {
        this.blockVerified = true;
    }

    public void markBlockAsHavingGasPaidTo(MinerAgent ma)
    {
        this.gasPaidTo.add(ma);
        if (this.gasPaidTo.size()==this.verifiers.size())
            this.gasPaidToVerifiers = true;
    }

    public Block()
    {
        this.trans = new ArrayList();
        this.verifiers = new ArrayList<>();
        this.blockVerified = false;
        this.gasPaidToVerifiers = false;
        this.hasValueBeenTransferred = false;
        this.totalGas=0;
    }

    public void setGas(int totalGas)
    {
        this.totalGas = totalGas;
    }

    public Transaction[] getTransactions()
    {
        return this.trans.toArray(new Transaction[this.trans.size()]);
    }

    public int getSize()
    {
        return this.trans == null? 0: this.trans.size();
    }

    public void addTrans(Transaction t)
    {
        if (getSize()<5) {      // TODO: Replace with Globals...
            this.trans.add(t);
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
            ma.sendGasToMiners(this);
        });
        this.gasPaidToVerifiers = true;
    }

    public void removeFromPTQ(TransactionListPTQ ptq)
    {
        for (Transaction t: this.trans)
        {
            ptq.removeTransaction(t);
        }

    }

    public void sendValueToRecipients()
    {
        for (int i = 0; i < this.trans.size(); i++)
        {
            Transaction tra = trans.get(i);
            if (tra.isTransferDone())
            {
                tra.agentJ.w += tra.value;
                tra.markTransferAsDone();
            }
        }
    }

    public int getNumAgentsVerified()
    {
        return this.verifiers.size();
    }

    public ArrayList<MinerAgent> getVerifiers()
    {
        return this.verifiers;
    }
}
