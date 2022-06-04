package org.example.models.kaligotla_macal_blockchain;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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

    PublicLedger pl = new PublicLedger();
    TransactionListPTQ ptq = new TransactionListPTQ();

    Globals gl;

    private BlockList blocksBeingVerified;

    private class BlockList{
        ArrayList<Block> blocksBeingVerified;
        BlockList(){
            blocksBeingVerified = new ArrayList<>();
        }
        private boolean contains(int blockId)
        {
            AtomicBoolean found = new AtomicBoolean(false);
            blocksBeingVerified.forEach(bl->{
                if (bl.getBlockId()==blockId)
                {
                    found.set(true);
                }
            });
            return found.get();
        }

        private Block get(int blockId)
        {
            Block blockSearch = null;
            for (int i=0;i<blocksBeingVerified.size();i++)
            {
                Block b = blocksBeingVerified.get(i);
                if (b.getBlockId()==blockId)
                {
                    blockSearch=b;
                }
            }
            return blockSearch;
        }

        private void append(Block b)
        {
            if (!blocksBeingVerified.contains(b.getBlockId()))
            {
                blocksBeingVerified.add(b);
            }
        }

        private void remove(Block b)
        {
            int blockId = b.getBlockId();
            if (contains(blockId))
            {
                remove(get(blockId));
            }
        }

        public ArrayList<Block> getList()
        {
            return blocksBeingVerified;
        }

    }

    public void createBlockList()
    {
        blocksBeingVerified = new BlockList();
    }

    /*
        A miner agent, selects a fixed number of transactions (blocklength) from the PTQ to form a candidate block
        of transactions to verify. The miner agent then begins the process of verifying the candidate block of
        transactions. When the candidate block is verified by at least μ other agents, and the candidate block is valid
        (none of the transactions in that candidate block have already been added to the Public Ledger),
        then the candidate block gets added to the Public Ledger.
    */

    public static Action<MinerAgent> addCandidateTransactionsToPTQ() {
        return Action.create(MinerAgent.class, curMinerAgent -> {
            if (curMinerAgent.hasMessagesOfType(Messages.broadcastTransactionsToMinersPTQ.class))
            {
                Messages.broadcastTransactionsToMinersPTQ msg =
                        curMinerAgent.getMessageOfType(Messages.broadcastTransactionsToMinersPTQ.class);
                curMinerAgent.ptq.enqueueTransaction(new Transaction(msg.createTick, msg.gas, msg.value,
                        msg.sender, msg.receiver,
                        (int) curMinerAgent.getPrng().uniform(0,curMinerAgent.gl.maxTransactionId).sample()));
            }
        });
    }

    public static Action<MinerAgent> calculateQueueLength()
    {
        return Action.create(MinerAgent.class, curMA -> {
            curMA.getGlobals().queueLength = curMA.ptq.getQueueLength();
             return;
        });
    }

    public static Action<MinerAgent> calculateLedgerLength()
    {
        return Action.create(MinerAgent.class, curMA -> {
            curMA.getGlobals().ledgerLength = curMA.pl.getLedgerSize();
            return;
        });
    }

    public static Action<MinerAgent> updateLedger()
    {
        return Action.create(MinerAgent.class, curMinerAgent -> {
            if (curMinerAgent.hasMessagesOfType(Messages.broadcastBlockToLedgers.class))
            {
                Messages.broadcastBlockToLedgers msg =
                        curMinerAgent.getMessageOfType(Messages.broadcastBlockToLedgers.class);
                curMinerAgent.ptq.removeTransactionsOfBlock(msg.block);
                curMinerAgent.pl.addBlock(msg.block);
                curMinerAgent.blocksBeingVerified.remove(msg.block);
                // Remove blocks containing any of the transactions from the appended block
                ArrayList<Block> blocksToRemove = new ArrayList<>();
                curMinerAgent.blocksBeingVerified.getList().forEach(block->{
                    block.getTransactions().forEach(trans->{
                        if (msg.block.getTransactions().contains(trans))
                        {
                            blocksToRemove.add(block);
                        }
                    });
                });
                blocksToRemove.forEach(bl->{
                    curMinerAgent.blocksBeingVerified.remove(bl);
                });

            }
        });
    }

    public static Action<MinerAgent> receiveBroadcastVerifications()
    {
        return Action.create(MinerAgent.class, curMA -> {
            if (curMA.hasMessagesOfType(Messages.broadcastVerificationToMiners.class)) {
                Messages.broadcastVerificationToMiners msg =
                        curMA.getMessageOfType(Messages.broadcastVerificationToMiners.class);
                Block b = curMA.blocksBeingVerified.get(msg.block.getBlockId());
                if (b!=null)
                {
                    msg.block.getVerifiers().forEach(ma->{
                        b.addVerifiers(ma);
                    });
                }
            }
        });
    }

    public static Action<MinerAgent> verifyBlocksAndTransferValue()
    {
        return Action.create(MinerAgent.class, curMA -> {
            curMA.blocksBeingVerified.getList().forEach(block -> {
                if (!block.getVerifiers().contains(curMA))
                {
                    boolean allTransactionsVerified = true;
                    for (int i=0; i<block.getTransactions().size();i++)
                    {
                        if (!block.getTransactions().get(i).isVerified())
                        {
                            allTransactionsVerified=false;
                        }
                    }
                    if (allTransactionsVerified) {
                        block.addVerifiers(curMA);
                        //System.out.println("GROSS000");
                        if (block.isBlockVerified())
                        {
                            System.out.println("GROSS");
                            block.payGasToMiners();
                            block.sendValueToRecipients();
                            curMA.ptq.removeTransactionsOfBlock(block);
                            curMA.pl.addBlock(block);
                            curMA.blocksBeingVerified.remove(block);
                            curMA.getLinks(Links.MinerToMinerLink.class)
                                    .send(Messages.broadcastBlockToLedgers.class, (message, link) -> {
                                        message.block = block;
                                    });
                        }
                        else {
                            curMA.getLinks(Links.MinerToMinerLink.class)
                                    .send(Messages.broadcastVerificationToMiners.class, (message, link) -> {
                                        message.block = block;
                                    });
                        }
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
                if (b.isBlockVerified() && !b.hasGasBeenPaidToMiners() &&
                    b.getVerifiers().contains(curMA) && !b.hasGasBeenPaidTo(curMA))
                {
                    System.out.println("Paying gas....");
                    curMA.w += b.getTotalGas()/b.getVerifiers().size();
                    b.markBlockAsHavingGasPaidTo(curMA);
                }
            return;
        });
    }

    public void fillBlock(Block b)
    {
        if (!b.isBlockVerified())
        {
            for (int i = 0; i < ptq.getQueueLength(); i++)
            {
                Transaction trans = ptq.get(i);
                if (trans != null)
                {
                    if (trans.isVerified() && b.getTransactions().size() < gl.blockLength) {
                        b.appendTransaction(trans);
                        if (b.getSize() == gl.blockLength) {
                            b.addVerifiers(this);
                        }
                    }
                }
            }
            b.getTransactions().forEach(trans->{
                ptq.removeTransaction(trans);
            });
        }
    }

    private Transaction cloneTransaction(Transaction t)
    {
        return new Transaction(t.tCreate, t.gas, t.value, t.agentI, t.agentJ, t.transactionId);
    }
    private Block cloneBlock(Block b)
    {
        Block bl = new Block(gl, b.getBlockId());
        b.getTransactions().forEach(trans->{
            bl.appendTransaction(cloneTransaction(trans));
        });
        return bl;
    }

    public static Action<MinerAgent> receiveBroadcastBlocks(){
        return Action.create(MinerAgent.class, curMA -> {
            if (curMA.hasMessagesOfType(Messages.broadcastBlockToMiners.class)) {
                Messages.broadcastBlockToMiners msg = curMA.getMessageOfType(Messages.broadcastBlockToMiners.class);
                if (!curMA.blocksBeingVerified.contains(msg.block.getBlockId()))
                {
                    curMA.blocksBeingVerified.append(curMA.cloneBlock(msg.block));
                }
            }
        });
    }

    public static Action<MinerAgent> spawnNewBlocks() {
        return Action.create(MinerAgent.class, curMA -> {
            int blockLength = curMA.getGlobals().blockLength;
            if (curMA.ptq.getQueueLength()>=blockLength) {
                Block bl = new Block(curMA.gl, (int) curMA.getPrng().uniform(0,curMA.gl.maxBlockId).sample());
                curMA.fillBlock(bl);
                curMA.blocksBeingVerified.append(bl);
                curMA.getLinks(Links.MinerToMinerLink.class)
                        .send(Messages.broadcastBlockToMiners.class, (message, link) -> {
                            message.block = bl;
                        });
            }
        });
    }
}