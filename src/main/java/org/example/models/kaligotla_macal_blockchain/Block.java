package org.example.models.kaligotla_macal_blockchain;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.System.exit;

public class Block  {
    // TODO: Include nonce, timestamp, stateRoot, difficulty, baseFeePerGas, etc. etc and any other fields missing
    private String blockId;                         // TODO: Replace with Ethereum's hash data type
    public String previousBlockId;                  // TODO: Replace with Ethereum's hash data type
    private final ArrayList<Transaction> trans;
    private final ArrayList<MinerAgent> verifiers;  //TODO: This is not in the Ethereum standard
    private boolean blockVerified;                  // TODO: This is not in the Ethereum standard
    private WalletAgent walletAgent;                         // TODO: This is not in the Ethereum standard

    public Block cloneBlock(WalletAgent a)
    {
        Block bl = new Block(a);
        bl.blockId = blockId;
        bl.previousBlockId = previousBlockId;
        bl.walletAgent = a;
        getVerifiers().forEach(bl::addVerifiers);
        getTransactions().forEach(trans-> bl.appendTransaction(trans.clone()));
        return bl;
    }

    public void appendTransaction(Transaction t)
    {
        if (t == null) return;
        if (t.isVerified() && getSize()<Globals.blockLength && verifyTransactionIsBacked(t))
        {
            //System.out.println("It was backed");
            trans.add(t);
            if (getSize()==Globals.blockLength) setBlockId();
        }
        else {
            //System.out.println("It was NOT backed");
        }
    }

    private boolean verifyTransactionIsBacked(Transaction t)
    {
        //System.out.println("verifyTransactionIsBacked...From:" + t.from);
        if (t.from == walletAgent.gl.coinbaseAgent.walletAddress) return true;
        WalletAgent wa1 = walletAgent.gl.marketWalletAddresses.getByAddress(t.from);
        WalletAgent wa2 = walletAgent.gl.minerWalletAddresses.getByAddress(t.from);
        // TODO: Take into consideration nonce and blocks containing multiple transactions from same agent
        if (wa1 instanceof MarketAgent)
        {
            //System.out.println("walletAgent: " + walletAgent  + " with address " + walletAgent.walletAddress + " is considered to be a MARKET");
            return (((MarketAgent)wa1).getBalanceFor(t.from)>=t.gas+t.value);
        }
        else if (wa2 instanceof MinerAgent) {
            //System.out.println("walletAgent: " + walletAgent  + " with address " + walletAgent.walletAddress + " is considered to be a miner");
            return (((MinerAgent) wa2).getBalanceFor(t.from) >= t.gas + t.value);
        }
        else
        {
            //System.out.println("We don't know what the class is for:" + walletAgent + ".");
            return false;
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
        AtomicInteger totalGas = new AtomicInteger();
        trans.forEach(t-> totalGas.addAndGet(t.gas));
        return totalGas.get();
    }

    public Block(WalletAgent a)
    {
        this.blockId = Integer.toString((int) Globals.random.uniform(0, Globals.maxBlockId).sample());
        trans = new ArrayList<>();
        verifiers = new ArrayList<>();
        blockVerified = false;
        walletAgent = a;
    }

    public String getBlockId()
    {
        return this.blockId;
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
        if ((verifiers.size()<Globals.agentsToVerifyTrans)&&(!verifiers.contains(miner)))
        {
            verifiers.add(miner);
            if (verifiers.size()==Globals.agentsToVerifyTrans)
            {
                blockVerified = true;
            }
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
            str.set(str.get().concat("\n{T id: " +t.transactionId+ " sender:" + t.from + " receiver:" + t.to +
                    " value:$" + t.value + " gas:$" + t.gas + "}, "));
        });
        return str.get() + ", previousBlockId:" + previousBlockId + "]";
    }
}
