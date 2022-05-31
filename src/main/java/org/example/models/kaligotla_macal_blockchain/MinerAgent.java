package org.example.models.kaligotla_macal_blockchain;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;

public class MinerAgent extends Agent<Globals> {
    int i;  //unique identifier for an agent -- TODO: Do we need this?
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
    Block nextBlockToVerify;

    /*
        A miner agent, selects a fixed number of transactions (blocklength) from the PTQ to form a candidate block
        of transactions to verify. The miner agent then begins the process of verifying the candidate block of
        transactions. When the candidate block is verified by at least μ other agents, and the candidate block is valid
        (none of the transactions in that candidate block have already been added to the Public Ledger),
        then the candidate block gets added to the Public Ledger.
    */

    public MinerAgent()
    {
        nextBlockToVerify = new Block();
    }

    public void incVerifiedTransactionsCounter(int i)
    {
        getLongAccumulator("numVerifiedTransactions").add(i);
    }

    public static Action<MinerAgent> selectNextBlockToVerify(TransactionListPTQ ptq, int blockLength) {
        return Action.create(MinerAgent.class, curMA -> {
            //  System.out.println("100000 " + blockLength + " " + ptq.getQueueLength());
            //  System.out.println("100001");
            Transaction t;
            int gasTotal = 0;
            //System.out.println("100002");
            if (ptq.getQueueLength()>=blockLength) {
                System.out.println("200000");
                for (int i = 0; i <= blockLength; i++) {
                    t = ptq.popFirst();
                    if (t != null)
                    {
                        System.out.println("200001");
                        if (t.isVerified()) {
                            System.out.println("200002");
                            curMA.nextBlockToVerify.addTrans(t);
                            System.out.println("200003   " + curMA.nextBlockToVerify.getSize());
                            gasTotal+=t.gas;
                        }
                    }
                }
                Globals gl = curMA.getGlobals();
                curMA.w+= gasTotal;
                System.out.println("200004 " + curMA.nextBlockToVerify.getSize());
                if (curMA.nextBlockToVerify.getSize()<=blockLength)
                {
                    System.out.println("300000");
                    gl.pl.addBlock(curMA.nextBlockToVerify);
                    curMA.incVerifiedTransactionsCounter(blockLength);
                }
            }
        });
    }
}