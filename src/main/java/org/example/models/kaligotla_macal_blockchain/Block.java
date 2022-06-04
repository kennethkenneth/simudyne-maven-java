package org.example.models.kaligotla_macal_blockchain;
import java.util.ArrayList;

public class Block  {
    private int blockId;
    private ArrayList<Transaction> trans;
    private ArrayList<MinerAgent> verifiers;
    private ArrayList<MinerAgent> gasPaidTo;
    private boolean blockVerified;
    private boolean gasPaidToVerifiers;
    private int totalGas;
    private boolean hasValueBeenTransferred;

    private Globals gl;

    public void appendTransaction(Transaction t)
    {
        if (t != null)
        {
            if (t.isVerified() && getSize()<5)          // TODO  Replace 5
            {
                trans.add(t);
                setGas(getTotalGas()+t.gas);
            }
        }
    }

    public ArrayList<Transaction> getTransactions()
    {
        return trans;
    }

    public boolean hasGasBeenPaidTo(MinerAgent ma)
    {
        return gasPaidTo.contains(ma);
    }

    public boolean isBlockVerified()
    {
        return blockVerified;
    }

    public int getTotalGas()
    {
        return totalGas;
    }

    public void markBlockAsHavingGasPaidTo(MinerAgent ma)
    {
            gasPaidTo.add(ma);
            if (gasPaidTo.size()==verifiers.size())
                gasPaidToVerifiers = true;
            return;
    }

    public Block(Globals gl, int blockId)
    {
        this.blockId = blockId;
        trans = new ArrayList();
        verifiers = new ArrayList<>();
        gasPaidTo = new ArrayList<>();
        blockVerified = false;
        gasPaidToVerifiers = false;
        hasValueBeenTransferred = false;
        totalGas=0;
        this.gl = gl;
    }

    public int getBlockId()
    {
        return this.blockId;
    }

    public void setGas(int totalGas)
    {
        this.totalGas = totalGas;
        return;
    }

    public int getSize()
    {
        return trans == null? 0: trans.size();
    }

    public boolean hasGasBeenPaidToMiners()
    {
        return gasPaidToVerifiers;
    }

    public boolean hasValueBeenTransferred()
    {
        return hasValueBeenTransferred;
    }

    public void payGasToMiners()
    {
        verifiers.forEach(ma -> {
            ma.sendGasToMiners(this);
        });
        gasPaidToVerifiers = true;
        return;
    }
/*
    public void fillBlock(MinerAgent miner)
    {
        int blockLength =   5; // curBlock.getGlobals().blockLength;
        if (verifiers.size()< 5) // curBlock.getGlobals().agentsToVerifyTrans)
        {
            //Messages.fillBlockMessage msg = curBlock.getMessageOfType(Messages.fillBlockMessage.class);
            for (int i = 0; i < blockLength; i++)
            {
                Transaction t = miner.ptq.get(i);
                if (t != null)
                {
                    if (t.isVerified() && getSize()<blockLength) {
                        trans.add(t);
                        setGas(getTotalGas()+t.gas);
                    }
                }
            }
            if (getSize()==blockLength)
            {
                verifiers.add(miner);
                miner.blocksBeingVerified.add(this);
            }
        }
    }*/

    public void sendValueToRecipients()
    {
        for (int i = 0; i < this.trans.size(); i++) {
            Transaction tra = trans.get(i);
            if (tra.isTransferDone()) {
                tra.agentJ.w += tra.value;
                tra.markTransferAsDone();
            }
        }
        hasValueBeenTransferred = true;
        return;
    }

    public int getNumAgentsVerified()
    {
        return verifiers.size();
    }

    public ArrayList<MinerAgent> getVerifiers()
    {
        return verifiers;
    }

    public void addVerifiers(MinerAgent miner)
    {
        if (verifiers.size()<gl.agentsToVerifyTrans)    // TODO - replace
        {
            if (!verifiers.contains(miner))
            {
                verifiers.add(miner);
            }
        }
        if (verifiers.size()==gl.agentsToVerifyTrans)
        {
            blockVerified = true;
        }
    }
}
