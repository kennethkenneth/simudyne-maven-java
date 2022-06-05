package org.example.models.kaligotla_macal_blockchain;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;

import static java.lang.System.exit;

public class Block  {
    private String blockId;
    private final ArrayList<Transaction> trans;
    private final ArrayList<MinerAgent> verifiers;
    private final ArrayList<MinerAgent> gasPaidTo;
    private boolean blockVerified;
    private int totalGas;
    private boolean hasValueBeenTransferred;
    private final Globals gl;


    private Transaction cloneTransaction(Transaction t)
    {
        return new Transaction(t.tCreate, t.gas, t.value, t.agentI, t.agentJ, t.transactionId);
    }

    public Block cloneBlock()
    {
        Block bl = new Block(gl, getBlockId());
        getVerifiers().forEach(bl::addVerifiers);
        getTransactions().forEach(trans-> bl.appendTransaction(cloneTransaction(trans)));
        return bl;
    }

    public void appendTransaction(Transaction t)
    {
        if (t != null)
        {
            if (t.isVerified() && getSize()<gl.blockLength)
            {
                trans.add(t);
                setGas(getTotalGas()+t.gas);
            }
            if (getSize()==gl.blockLength)
            {
                setBlockId();
            }
        }
    }

    private void setBlockId(){
        trans.sort((o1, o2) -> {
            if (o1.transactionId == o2.transactionId)
                return 0;
            return o1.transactionId < o2.transactionId ? -1 : 1;
        });

        String concatTransId = "";
        for (Transaction tran : trans) {
            concatTransId = concatTransId.concat("ID:" + tran.transactionId + ";");
        }

        try{
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(StandardCharsets.UTF_8.encode(concatTransId));
            blockId = String.format("%032x", new BigInteger(1, md5.digest()));
        }
        catch(Exception e)
        {
            exit(1);
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
        if (!gasPaidTo.contains(ma))
        {
            gasPaidTo.add(ma);
        }
    }

    public void markBlockAsHavingValueTransferred()
    {
        hasValueBeenTransferred = true;
    }

    public Block(Globals gl, String blockId)
    {
        this.blockId = blockId;
        trans = new ArrayList<>();
        verifiers = new ArrayList<>();
        gasPaidTo = new ArrayList<>();
        blockVerified = false;
        hasValueBeenTransferred = false;
        totalGas=0;
        this.gl = gl;
    }

    public String getBlockId()
    {
        return this.blockId;
    }

    public void setGas(int totalGas)
    {
        this.totalGas = totalGas;
    }

    public int getSize()
    {
        return trans.size();
    }

    public boolean hasValueBeenTransferred()
    {
        return hasValueBeenTransferred;
    }

    public void sendValueToRecipients()
    {
        for (Transaction tran : this.trans) {
            if (!tran.isTransferDone()) {
                tran.agentJ.w += tran.value;
                tran.markTransferAsDone();
            }
        }
        hasValueBeenTransferred = true;
    }

    public ArrayList<MinerAgent> getVerifiers()
    {
        return verifiers;
    }

    public void addVerifiers(MinerAgent miner)
    {
        if (verifiers.size()<gl.agentsToVerifyTrans)
        {
            if (!verifiers.contains(miner))
            {
                verifiers.add(miner);
            }
        }
        if (verifiers.size()==gl.agentsToVerifyTrans)
        {
            System.out.println("The block has all the required verifications");
            blockVerified = true;
        }
    }

    public boolean verifyTransactions()
    {
        boolean verifiedTrans = true;
        for (Transaction t : getTransactions()) {
            if (!t.isVerified()) {
                verifiedTrans = false;
            }
        }
        return verifiedTrans;
    }
}
