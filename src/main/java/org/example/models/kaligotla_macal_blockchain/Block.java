package org.example.models.kaligotla_macal_blockchain;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.System.exit;

public class Block  {
    // TODO: Include nonce, timestamp, stateRoot, difficulty, baseFeePerGas, etc. etc and any other fields missing
    private String blockId;                         // TODO: Replace with Ethereum's hash data type
    public String previousBlockId;                  // TODO: Replace with Ethereum's hash data type
    private final ArrayList<Transaction> trans;
    private final ArrayList<MinerAgent> verifiers;  //TODO: This is not in the Ethereum standard
    private boolean blockVerified;                  // TODO: This is not in the Ethereum standard
    private int totalGas;                           // TODO: This is not in the Ethereum standard

    private Transaction cloneTransaction(Transaction t)
    {
        return new Transaction(t.tCreate, t.gas, t.value, t.from, t.to, t.transactionId);
    }

    public Block cloneBlock(WalletAgent a)
    {
        Block bl = new Block(a);
        bl.blockId = blockId;
        bl.previousBlockId = previousBlockId;
        bl.totalGas = totalGas;
        getVerifiers().forEach(bl::addVerifiers);
        getTransactions().forEach(trans-> bl.appendTransaction(cloneTransaction(trans)));
        return bl;
    }

    public void appendTransaction(Transaction t)
    {
        if (t != null)
        {
            if (t.isVerified() && getSize()<Globals.blockLength)
            {
                trans.add(t);
                addGasToBlock(t.gas);
            }
            if (getSize()==Globals.blockLength)
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

    public boolean isBlockVerified()
    {
        return blockVerified;
    }

    public int getTotalGas()
    {
        return totalGas;
    }

    public Block(WalletAgent a)
    {
        // origin = walletAddresses.get((int)random.uniform(0, walletAddresses.size()).sample());
        // this.blockId = Integer.toString((int) a.getPrng().uniform(0, a.gl.maxBlockId).sample());
        this.blockId = Integer.toString((int) a.gl.random.uniform(0, a.gl.maxBlockId).sample());
        trans = new ArrayList<>();
        verifiers = new ArrayList<>();
        blockVerified = false;
        totalGas=0;
    }

    public String getBlockId()
    {
        return this.blockId;
    }

    public void addGasToBlock(int totalGas)
    {
        this.totalGas+= totalGas;
    }

    public int getSize()
    {
        return trans.size();
    }

    public ArrayList<MinerAgent> getVerifiers()
    {
        return verifiers;
    }

    public void addVerifiers(MinerAgent miner)
    {
        if (verifiers.size()<Globals.agentsToVerifyTrans)
        {
            if (!verifiers.contains(miner))
            {
                verifiers.add(miner);
            }
        }
        if (verifiers.size()==Globals.agentsToVerifyTrans)
        {
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
    public String toString()
    {
        AtomicReference<String> str = new AtomicReference<>("[Block: " + blockId + " (" + trans.size() + " transactions). ");
        trans.forEach(t->{
            str.set(str.get().concat("{T id: " +t.transactionId+ " sender:" + t.from + " receiver:" + t.to + " value:$" + t.value + "}, "));
        });
        return str.get() + ", previousBlockId:" + previousBlockId + "]";
    }
}
