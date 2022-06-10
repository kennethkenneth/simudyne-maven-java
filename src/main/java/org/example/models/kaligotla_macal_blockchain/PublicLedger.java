package org.example.models.kaligotla_macal_blockchain;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class PublicLedger {
    /*
    Public Ledger is a vector of verified transactions, which were previously in the Pending Transaction Queue.
    Lt = [τ1,τ2,...] where all τ(σ) = 1 are verified.
    When the candidate block is verified by at least μ other agents, and the candidate block
    is valid (none of the transactions in that candidate block have already been added to the Public Ledger),
    then the candidate block gets added to the Public Ledger
     */

    private final ArrayList<LinkedList<Block>> bls;
    private int publicLedgerMainBranchId = 0;

    PublicLedger()
    {
        bls = new ArrayList<>();
        bls.add(new LinkedList<>());
    }

    public int getNumBranches()
    {
        return bls.size();
    }
    public int getLedgerSize()
    {
        return bls.get(publicLedgerMainBranchId).size();
    }

    public String getLastBlockID()
    {
        LinkedList<Block> mainBranch = bls.get(publicLedgerMainBranchId);
        if (mainBranch.size()==0)
        {
            return Globals.START_BLOCK_ID;
        }
        return mainBranch.get(mainBranch.size()-1).getBlockId();
    }

    public String getLastBlockIDShort()
    {
        String s = getLastBlockID();
        return s.substring(0, Math.min(s.length(), 5));
    }

    /*
    Add the block to the end of the ledger (at the appropriate branch), respecting the link to previousBlockId.
    Will attempt to add it on the main branch. If not possible, it will try to do it in one of the alt branches.
    If that is not possible either, it will try to create a new branch
    After running this function, publicLedgerMainBranchId will take the value of to the id of the longer branch,
    which may or may not be the same branch where the block was appended to.
    @return true, it was possible to append the block to any of the branches.
            false: b.previousBlockId does not exit in any branch or this block exists already in a branch

     */
    public boolean addBlock(Block b)
    {
        LinkedList<Block> mainBranch = bls.get(publicLedgerMainBranchId);
        if (mainBranch.contains(b))
        {
            return false;
        }
        if (mainBranch.size()==0)
        {   //The first's block previousBlockId should be START_BLOCK_ID no matter what
            b.previousBlockId = Globals.START_BLOCK_ID;;
            mainBranch.addLast(b);
            return true;
        }
        if (getLastBlockID().compareTo(b.previousBlockId)==0)
        {
            mainBranch.addLast(b);
            return true;
        }
        else {
            //Not in the main chain. Seek branches for the first match
            boolean previousBlockIdFoundAtEnd = false;
            int branchId = -1;
            for (int i=0; i<bls.size(); i++)
            {
                if (bls.get(i).getLast().getBlockId().compareTo(b.previousBlockId)==0)
                {
                    previousBlockIdFoundAtEnd = true;
                    branchId = i;
                }
                for (int j=0; j<bls.get(i).size(); j++)
                {
                    if (bls.get(i).get(j).getBlockId().compareTo(b.getBlockId())==0)
                    {
                        //blockId exists in an alt-branch, skip
                        return false;
                    }
                }
            }
            if (previousBlockIdFoundAtEnd)
            {   //add at the end of one of the alt-branches
                LinkedList<Block> altBranch = bls.get(branchId);
                altBranch.addLast(b);
                if (bls.get(publicLedgerMainBranchId).size()<altBranch.size())
                {   //The branch in which block was added is now the longest and should become the main branch
                    publicLedgerMainBranchId = branchId;
                }
                return true;
            }
            // previousBlockId is not found in any of branches, so we cannot append this block.
            return false;
        }
    }
    public int getBalance(WalletAgent wa)
    {
        AtomicInteger balance = new AtomicInteger();
        //Add Inflows
        bls.forEach(b->{
            b.get(publicLedgerMainBranchId).getTransactions().stream()
                .filter(t->t.to==wa.walletAddress)
                .forEach(t->{
                    balance.addAndGet(t.value);
                });});
        //Subtract Outflows
        bls.forEach(b->{
            b.get(publicLedgerMainBranchId).getTransactions().stream()
                    .filter(t->t.from==wa.walletAddress)
                    .forEach(t->{
                        balance.addAndGet(-t.value);
                    });});
        //Add Gas Inflows
        bls.forEach(b->{
            b.get(publicLedgerMainBranchId).getVerifiers().stream()
                    .filter(v->v.walletAddress==wa.walletAddress)
                    .forEach(t->{
                        balance.addAndGet(b.get(publicLedgerMainBranchId).getTotalGas()/Globals.blockLength);
                    });});
        //Subtract Gas Outflows
        bls.forEach(b->{
            b.get(publicLedgerMainBranchId).getTransactions().stream()
                    .filter(t->t.from==wa.walletAddress)
                    .forEach(t->{
                        balance.addAndGet(-b.get(publicLedgerMainBranchId).getTotalGas());
                    });});
        return balance.get();
    }

    public String toString()
    {
        final AtomicReference<String> strOutput = new AtomicReference<>("Public Ledger: ");
        bls.forEach(b->{
            strOutput.set(strOutput.get()
                    .concat("block_" + b.get(publicLedgerMainBranchId)
                    .getBlockId() + " (previous: " + b.get(publicLedgerMainBranchId).previousBlockId + "), "));
        });
        return strOutput.get();
    }
    public String toString2()
    {
        final String[] str = {""};
        AtomicInteger j= new AtomicInteger();
        for (int i =0;i<bls.get(publicLedgerMainBranchId).size(); i++)
        {
            bls.get(publicLedgerMainBranchId).get(i).getTransactions()
                    .forEach(t->{
                        str[0] = str[0].concat("\nTxId:" + t.transactionId + ", from:" + t.from + ", to:" + t.to +
                                ", value:" + t.value + ", gas:" + t.gas + " t=" + t.tCreate + " (j=" + j.get() + ").");
                        j.getAndIncrement();
                    });
        }

        return str[0];
    }
}
