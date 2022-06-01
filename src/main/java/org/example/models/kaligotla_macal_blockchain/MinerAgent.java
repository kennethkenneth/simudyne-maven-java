package org.example.models.kaligotla_macal_blockchain;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;

import java.util.ArrayList;

public class MinerAgent extends Agent<Globals> {
    //int i;  //unique identifier for an agent -- TODO: Do we need this?
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
    ArrayList<Block> blocksVerifiedByThisMiner;

    /*
        A miner agent, selects a fixed number of transactions (blocklength) from the PTQ to form a candidate block
        of transactions to verify. The miner agent then begins the process of verifying the candidate block of
        transactions. When the candidate block is verified by at least μ other agents, and the candidate block is valid
        (none of the transactions in that candidate block have already been added to the Public Ledger),
        then the candidate block gets added to the Public Ledger.
    */

    public MinerAgent(){
        this.nextBlockToVerify= new Block();
        this.blocksVerifiedByThisMiner = new ArrayList<>();
    }

    public void incVerifiedTransactionsCounter(int i)
    {
        getLongAccumulator("numVerifiedTransactions").add(i);
    }

    public static Action<MinerAgent> verifyBlocksAndInitiateValueTransfer()
    {
        return Action.create(MinerAgent.class, curMA -> {
            Globals gl = curMA.getGlobals();
            //ArrayList<Block>blocksToVerify = gl.blocksBeingVerified;
            gl.blocksBeingVerified.forEach(block -> {
                if (!curMA.blocksVerifiedByThisMiner.contains(block))
                {
                    Transaction[] trs = block.getTransactions();
                    int nbrVerif = 0;
                    for (int i=0;i<trs.length;i++)
                    {
                        if (trs[i].isVerified())
                        {
                            nbrVerif++;
                        }
                    }
                    if (nbrVerif==trs.length)
                    {
                        //block.incNumAgentsVerified();
                        block.markBlockAsVerifierBy(curMA);
                        curMA.blocksVerifiedByThisMiner.add(block);
                    }
                    if (block.getNumAgentsVerified() == gl.agentsToVerifyTrans) {
                        for (int i = 0; i < trs.length; i++)
                        {
                            Transaction curTran = trs[i];
                            curMA.getLinks(Links.MarketToMarketLink.class)
                                    .send(Messages.TransferAmount.class,
                                            (msg, link) -> {
                                                msg.amount = curTran.value;
                                                msg.receiver = curTran.agentJ;
                                            });
                            curMA.getLinks(Links.MarketToMarketLink.class)
                                    .send(Messages.SubstractAmount.class,
                                            (msg, link) -> {
                                                msg.amount = curTran.value;
                                                msg.sender = curTran.agentI;
                                            });
                        }
                        block.payGasToMiners();
                        block.markBlockAsVerified();
                        //gl.blocksBeingVerified.remove(block);  //////////////
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

    public static Action<MinerAgent> sendGasToMiners()
    {
        return Action.create(MinerAgent.class, curMA -> {
                    Globals gl = curMA.getGlobals();
                    gl.blocksBeingVerified.forEach(bl -> {
                        curMA.w += bl.getTotalGas();
                    });
                    return;
                }
        );
    }

    public static Action<MinerAgent> selectNextBlockToVerify(TransactionListPTQ ptq, int blockLength) {
        return Action.create(MinerAgent.class, curMA -> {
            Transaction t;
            int gasTotal = 0;
            if (ptq.getQueueLength()>=blockLength) {
                for (int i = 0; i < blockLength; i++) {
                    t = ptq.popFirst();
                    if (t != null)
                    {
                        if (t.isVerified()) {
                            curMA.nextBlockToVerify.addTrans(t);
                            gasTotal+=t.gas;
                        }
                    }
                }
                Globals gl = curMA.getGlobals();
                if (curMA.nextBlockToVerify.getSize()==gl.blockLength)
                {
                    curMA.nextBlockToVerify.setGas(gasTotal);
                    gl.blocksBeingVerified.add(curMA.nextBlockToVerify);
                    curMA.blocksVerifiedByThisMiner.add(curMA.nextBlockToVerify);
                    curMA.incVerifiedTransactionsCounter(blockLength);
                }
            }
        });
    }
}