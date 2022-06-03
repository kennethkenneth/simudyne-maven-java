package org.example.models.kaligotla_macal_blockchain;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;

import java.util.ArrayList;

public class MinerAgent extends Agent<Globals> {
    int w;  //agents’ current balance of currency or value
    /*
    Relative computing power of the agent. computingPower = 1,2,...N denoting the compute power
    required by a miner agent to solve a cryptographic puzzle as a proof of computation work.
     */
    int computingPower;
    int miningCost;
    int totalMineCost;
    int blockList;
    int tEndVerify;
    int strategy; // 0: LARGEST, 1: POLAR, 2: PARTITION

    //Block nextBlockToVerify;
    //ArrayList<Block> blocksVerifiedByThisMiner;

    /*
        A miner agent, selects a fixed number of transactions (blocklength) from the PTQ to form a candidate block
        of transactions to verify. The miner agent then begins the process of verifying the candidate block of
        transactions. When the candidate block is verified by at least μ other agents, and the candidate block is valid
        (none of the transactions in that candidate block have already been added to the Public Ledger),
        then the candidate block gets added to the Public Ledger.
    */

    public void incVerifiedTransactionsCounter(int i)
    {
        getLongAccumulator("numVerifiedTransactions").add(i);
    }

    public static Action<MinerAgent> verifyBlocksAndInitiateValueTransfer()
    {
        return Action.create(MinerAgent.class, curMA -> {
            Globals gl = curMA.getGlobals();
            gl.blocksBeingVerified.forEach(block -> {
                if (!block.getVerifiers().contains(curMA))
                {
                    Transaction[] trs = block.getTransactions();
                    boolean allTransactionsVerified = true;
                    for (int i=0;i<trs.length;i++)
                    {
                        if (!trs[i].isVerified())
                        {
                            allTransactionsVerified = false;
                        }
                    }
                    if (allTransactionsVerified) {
                        //curMA.nextBlockToVerify.markBlockAsVerifiedBy(curMA);
                        block.markBlockAsVerifiedBy(curMA);
                    }
                    /*System.out.println("Yo yo yo yo" + allTransactionsVerified + " "
                            + block.getNumAgentsVerified() + " " + gl.agentsToVerifyTrans);*/
                    if (allTransactionsVerified && block.getNumAgentsVerified() == gl.agentsToVerifyTrans) {
                        System.out.println("10000111");
                        block.markBlockAsVerified();
                        block.payGasToMiners();
                        block.sendValueToRecipients();
                        block.markBlockHavingValueTransferred();
                        block.removeFromPTQ(gl.ptq);
                        curMA.getGlobals().pl.addBlock(block);
                        curMA.getGlobals().blocksBeingVerified.remove(block);
                    }
                }
            });
            return;
        });
    }

    public static Action<MinerAgent> sumETHValue()
    {
        return Action.create(MinerAgent.class, currMA -> {
            Globals gl = currMA.getGlobals();
            gl.totalETHValueInMiners+=currMA.w;
            return;
        });
    }

    public static Action<MinerAgent> sendGasToMiners(Block b)
    {
        return Action.create(MinerAgent.class, curMA -> {
                if (b.isBlockVerified() &&
                    !b.hasGasBeenPaidToMiners() &&
                    b.getVerifiers().contains(curMA))
                {
                    System.out.println("Paying gas....");
                    curMA.w += b.getTotalGas()/b.getVerifiers().size();
                    b.markBlockAsHavingGasPaidTo(curMA);
                }
            return;
        });
    }

    public static Action<MinerAgent> selectNextBlockToVerify(TransactionListPTQ ptq, int blockLength) {
        return Action.create(MinerAgent.class, curMA -> {
            int gasTotal = 0;
            Globals gl = curMA.getGlobals();
            Block bl = new Block();
            if (ptq.getQueueLength()>=blockLength) {
                for (int i = 0; i < blockLength; i++) {
                    Transaction t = ptq.get(i);
                    if (t != null)
                    {
                        if (t.isVerified() && bl.getSize()<gl.blockLength) {
                            bl.addTrans(t);
                            gasTotal+=t.gas;
                        }
                    }
                }
                if (bl.getSize()==gl.blockLength)
                {
                    bl.setGas(gasTotal);
                    bl.markBlockAsVerifiedBy(curMA);
                    gl.blocksBeingVerified.add(bl);
                    curMA.incVerifiedTransactionsCounter(blockLength);
                }
            }
        });
    }
}