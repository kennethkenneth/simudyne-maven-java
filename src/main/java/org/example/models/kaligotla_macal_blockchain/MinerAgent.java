package org.example.models.kaligotla_macal_blockchain;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MinerAgent extends Agent<Globals> {
    int w;  //agents’ current balance of currency or value
    /*
    Relative computing power of the agent. computingPower = 1,2,...N denoting the compute power
    required by a miner agent to solve a cryptographic puzzle as a proof of computation work.
     */
    int computingPower;
    int miningCost;
    int totalMineCost;
    int tEndVerify;
    int strategy; // 0: LARGEST, 1: POLAR, 2: PARTITION

    private final PublicLedger pl = new PublicLedger();
    private final TransactionListPTQ ptq = new TransactionListPTQ();

    Globals gl;

    private BlockList blocksBeingVerified;

    private static class BlockList{
        ArrayList<Block> blocksBeingVerified;
        BlockList(){
            blocksBeingVerified = new ArrayList<>();
        }
        private boolean contains(String blockId)
        {
            boolean found = false;
            for (Block bl : blocksBeingVerified) {
                if (bl.getBlockId().compareTo(blockId) == 0) {
                    found = true;
                    break;
                }
            }
            return found;
        }

        private Block get(String blockId)
        {
            Block blockSearch = null;
            for (Block b : blocksBeingVerified) {
                if (b.getBlockId().compareTo(blockId)==0) {
                    blockSearch = b;
                }
            }
            return blockSearch;
        }

        private void append(Block b)
        {
            if (get(b.getBlockId())==null)
            {
                blocksBeingVerified.add(b);
            }
        }

        private void remove(Block b)
        {
            String blockId = b.getBlockId();
            if (contains(blockId))
            {
                blocksBeingVerified.remove(get(blockId));
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
                List<Messages.broadcastTransactionsToMinersPTQ> lst =
                        curMinerAgent.getMessagesOfType(Messages.broadcastTransactionsToMinersPTQ.class);
                lst.forEach(msg->{
                    curMinerAgent.ptq.enqueueTransaction(new Transaction(msg.createTick, msg.gas, msg.value,
                            msg.sender, msg.receiver,
                            (int) curMinerAgent.getPrng().uniform(0,curMinerAgent.gl.maxTransactionId).sample()));
                });
            }
        });
    }

    public static Action<MinerAgent> calculateQueueLength()
    {
        return Action.create(MinerAgent.class, curMA -> curMA.getGlobals().queueLength = curMA.ptq.getQueueLength());
    }

    public static Action<MinerAgent> calculateLedgerLength()
    {
        return Action.create(MinerAgent.class, curMA -> curMA.getGlobals().ledgerLength = curMA.pl.getLedgerSize());
    }

    public static Action<MinerAgent> updateLedger()
    {
        return Action.create(MinerAgent.class, curMinerAgent -> {
            if (curMinerAgent.hasMessagesOfType(Messages.broadcastBlockToLedgers.class))
            {
                List<Messages.broadcastBlockToLedgers> list =
                        curMinerAgent.getMessagesOfType(Messages.broadcastBlockToLedgers.class);
                list.forEach(msg->{
                    curMinerAgent.ptq.removeTransactionsOfBlock(msg.block);
                    curMinerAgent.pl.addBlock(msg.block);
                    curMinerAgent.blocksBeingVerified.remove(msg.block);
                    // Remove blocks containing any of the transactions from the appended block
                    ArrayList<Block> blocksToRemove = new ArrayList<>();
                    curMinerAgent.blocksBeingVerified.getList().forEach(block-> block.getTransactions().forEach(trans->{
                        if (msg.block.getTransactions().contains(trans))
                        {
                            blocksToRemove.add(block);
                        }
                    }));
                    blocksToRemove.forEach(bl-> curMinerAgent.blocksBeingVerified.remove(bl));
                });
            }
        });
    }

    public static Action<MinerAgent> verifyBlocksAndTransferValue() {
        return Action.create(MinerAgent.class, curMA ->
                {
                    BlockList bli = new BlockList();
                    curMA.blocksBeingVerified.getList().forEach(block -> {
                        if (block.isBlockVerified() && block.getVerifiers().contains(curMA)
                                && (block.hasValueBeenTransferred()
                                || !block.hasGasBeenPaidTo(curMA))
                        ) {
                            bli.append(block);
                        }
                    });
                    bli.getList().forEach(b -> {
                        if (!b.hasGasBeenPaidTo(curMA))
                        {
                            curMA.w += b.getTotalGas() / curMA.gl.agentsToVerifyTrans;
                            b.markBlockAsHavingGasPaidTo(curMA);
                        }
                        if (!b.hasValueBeenTransferred())
                        {
                            b.sendValueToRecipients();
                            b.markBlockAsHavingValueTransferred();
                        }
                        curMA.ptq.removeTransactionsOfBlock(b);
                        curMA.pl.addBlock(b);
                        curMA.blocksBeingVerified.remove(b);
                        curMA.getLinks(Links.MinerToMinerLink.class).send(Messages.broadcastBlockToLedgers.class,
                                (message, link) -> message.block = b);
                    });
                }
        );
    }

    public static Action<MinerAgent> sumETHValue()
    {
        return Action.create(MinerAgent.class, currMA -> {
            Globals gl = currMA.getGlobals();
            gl.totalETHValueInMiners+=currMA.w;
        });
    }

    public Block fillUpBlockWithPTQ(Block b)
    {
        if (!b.isBlockVerified())
        {
            for (int i = 0; i < ptq.getQueueLength(); i++)
            {
                Transaction trans = ptq.get(i);
                if (trans != null)
                {
                    if (trans.isVerified() && b.getTransactions().size() < gl.blockLength) {
                        //System.out.println("toc3 " + i);
                        b.appendTransaction(trans);
                        if (b.getSize() == gl.blockLength) {
                            //System.out.println("Verified!" + trans.toString());
                            b.addVerifiers(this);
                        }
                    }
                }
            }
            b.getTransactions().forEach(ptq::removeTransaction);
        }
        return b;
    }

    public static Action<MinerAgent> receiveBroadcastBlocks(){
        return Action.create(MinerAgent.class, curMA -> {
            if (curMA.hasMessagesOfType(Messages.broadcastBlockToMiners.class)) {
                  List<Messages.broadcastBlockToMiners> lst = curMA.getMessagesOfType(Messages.broadcastBlockToMiners.class);
                  lst.stream().forEach(msg->{
                      if (!curMA.blocksBeingVerified.contains(msg.block.getBlockId()))
                      {
                          Block bl2 = msg.block.cloneBlock();
                          if (bl2.verifyTransactions())
                          {
                              bl2.addVerifiers(curMA);
                          }
                          curMA.blocksBeingVerified.append(bl2);
                      }
                      else
                      {
                          msg.block.getVerifiers().forEach(ma->curMA.blocksBeingVerified
                                  .get(msg.block.getBlockId()).addVerifiers(ma));
                      }
                  });
            }
        });
    }

    public static Action<MinerAgent> broadcastBlocks() {
        return Action.create(MinerAgent.class, curMA -> {
            int blockLength = curMA.getGlobals().blockLength;
            if (curMA.ptq.getQueueLength()>=blockLength) {
                Block bl = curMA.fillUpBlockWithPTQ(new Block(curMA.gl,
                        Integer.toString((int) curMA.getPrng().uniform(0, curMA.gl.maxBlockId).sample())));
                if (bl.verifyTransactions())
                {
                    bl.addVerifiers(curMA);
                }
                curMA.blocksBeingVerified.append(bl);
            }
            curMA.blocksBeingVerified.getList().forEach(bl-> curMA.getLinks(Links.MinerToMinerLink.class)
                    .send(Messages.broadcastBlockToMiners.class, (message, link) -> message.block = bl));
        });
    }
}