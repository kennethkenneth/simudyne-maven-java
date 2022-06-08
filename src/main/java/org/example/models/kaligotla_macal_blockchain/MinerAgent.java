package org.example.models.kaligotla_macal_blockchain;

import java.util.ArrayList;
import simudyne.core.abm.Action;

public class MinerAgent extends WalletAgent {
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
    private BlockList blocksBeingVerified;

    private static class BlockList{
        ArrayList<Block> blockList;
        BlockList(){
            blockList = new ArrayList<>();
        }
        private boolean contains(String blockId)
        {
            boolean found = false;
            for (Block bl : blockList) {
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
            for (Block b : blockList) {
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
                blockList.add(b);
            }
        }

        private void remove(Block b)
        {
            String blockId = b.getBlockId();
            if (contains(blockId))
            {
                blockList.remove(get(blockId));
            }
        }

        public ArrayList<Block> getList()
        {
            return blockList;
        }

        public int size()
        {
            return blockList.size();
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
                curMinerAgent.getMessagesOfType(Messages.broadcastTransactionsToMinersPTQ.class).forEach(msg->
                    curMinerAgent.ptq.enqueueTransaction(new Transaction(msg.createTick, msg.gas, msg.value,
                            msg.senderAddress, msg.receiverAddress,
                            (int) curMinerAgent.getPrng().uniform(0,curMinerAgent.gl.maxTransactionId).sample()))
                );
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
                curMinerAgent.getMessagesOfType(Messages.broadcastBlockToLedgers.class).forEach(msg->{
                    curMinerAgent.ptq.removeTransactionsOfBlock(msg.block);
                    curMinerAgent.pl.addBlock(msg.block);
                    curMinerAgent.blocksBeingVerified.remove(msg.block);
                    // Remove blocks containing any of the transactions from the appended block
                    ArrayList<Block> blocksToRemove = new ArrayList<>();
                    curMinerAgent.blocksBeingVerified.getList()
                            .forEach(block-> block.getTransactions().forEach(trans->{
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

    public static Action<MinerAgent> verifyCandidateBlocksAndTransferValueAndGas() {
        return Action.create(MinerAgent.class, curMA -> {
            BlockList bli = new BlockList();
            curMA.blocksBeingVerified.getList().forEach(block -> {
                if (block.isBlockVerified() && block.getVerifiers().contains(curMA)
                        && (!block.hasValueBeenTransferred()
                        || !block.hasGasBeenPaidTo(curMA))) {
                    bli.append(block);
                }
            });
            bli.getList().forEach(b -> {
                if (!b.hasGasBeenPaidTo(curMA)) {
                    curMA.w += b.getTotalGas() / curMA.gl.agentsToVerifyTrans;
                    b.markBlockAsHavingGasPaidTo(curMA);
                }
                if (!b.hasValueBeenTransferred())
                {
                    //System.out.println("if (!b.hasValueBeenTransferred())");
                    for (Transaction tran : b.getTransactions()) {
                        //To avoid double-sending, only the last verifier is responsible to initiate payment
                        if (!tran.isTransferDone() && b.getVerifiers().get(curMA.gl.agentsToVerifyTrans-1)==curMA) {
                            /*System.out.println("//To avoid double-sending, only the last verifier is " +
                                                "responsible to initiate payment");*/
                            curMA.getLinks(Links.MinerToMarketLink.class).send(Messages.transactionValue.class,
                                    (message, link) -> message.t = tran);
                            tran.markTransferAsDone();
                            b.markBlockAsValueTransferred();
                        }
                    }
                }
                else {
                    System.out.println("Adding to ledger...");
                    curMA.ptq.removeTransactionsOfBlock(b);
                    curMA.pl.addBlock(b);
                    curMA.blocksBeingVerified.remove(b);
                    curMA.getLinks(Links.MinerToMinerLink.class).send(Messages.broadcastBlockToLedgers.class,
                            (message, link) -> message.block = b);
                }
            });
        });
    }

    public static Action<MinerAgent> sumETHValue()
    {
        return Action.create(MinerAgent.class, currMA ->
                currMA.gl.totalETHValueInMiners+=currMA.w);
    }

    public Block getBlockFromPTQ()
    {
        //Give a temporary random Block ID
        Block b = new Block(gl, Integer.toString((int) getPrng().uniform(0, gl.maxBlockId).sample()));
        //peek()	Retrieves, but does not remove, the head of this queue, or returns null if this queue is empty.
        //poll()	Retrieves and removes the head of this queue, or returns null if this queue is empty.
        Transaction trans = ptq.poll();
        /*while ((trans != null) && trans.isVerified() && !b.isBlockVerified() && b.getTransactions().size() < gl.blockLength)*/
        while( (trans!=null) && (b.getTransactions().size() < gl.blockLength) && (!b.isBlockVerified()) )
        {
            if (trans.isVerified())
            {
                b.appendTransaction(trans);
            }
            if (b.getSize() < gl.blockLength)
                trans = ptq.poll();
        }
        if (b.getTransactions().size()==gl.blockLength)
        {
            b.addVerifiers(this);
            b.getTransactions().forEach(ptq::removeTransaction);
        }
        return b;
    }

    public static Action<MinerAgent> receiveCandidateBlocks(){
        return Action.create(MinerAgent.class, curMA -> {
            if (curMA.hasMessagesOfType(Messages.broadcastCandidateBlockToMiners.class)) {
                  curMA.getMessagesOfType(Messages.broadcastCandidateBlockToMiners.class)
                    .forEach(msg->{
                      BlockList bli = curMA.blocksBeingVerified;
                      if (!bli.contains(msg.block.getBlockId()))
                      {
                          Block blo = msg.block.cloneBlock();
                          if (blo.verifyTransactions())
                          {
                              blo.addVerifiers(curMA);
                          }
                          bli.append(blo);
                      }
                      else
                      {
                          if (msg.block.hasValueBeenTransferred())
                          {
                              //System.out.println("Something is wrong 1"); // PUT IT BACK
                          }
                          if (msg.block.getGasPaidTo().size()!=0)
                          {
                              //System.out.println("Something is wrong 2");
                          }
                          Block blo = bli.get(msg.block.getBlockId());
                          msg.block.getVerifiers().forEach(blo::addVerifiers);
                          //msg.block.getGasPaidTo().forEach(blo::markBlockAsHavingGasPaidTo);
                          //if  (msg.block.hasValueBeenTransferred()) blo.markBlockAsValueTransferred();
                      }
                  });
            }
        });
    }

    public static Action<MinerAgent> createAndBroadcastCandidateBlocks() {
        return Action.create(MinerAgent.class, curMA -> {
            int blockLength = curMA.getGlobals().blockLength;
            if (curMA.ptq.getQueueLength()>=blockLength) {
                Block bl = curMA.getBlockFromPTQ();
                if (bl.verifyTransactions())
                {
                    bl.addVerifiers(curMA);
                    curMA.blocksBeingVerified.append(bl);
                }
            }
            System.out.println("Number of blocks being verified:" + curMA.blocksBeingVerified.size());
            curMA.blocksBeingVerified.getList().forEach(bl->
                    curMA.getLinks(Links.MinerToMinerLink.class).send(Messages.broadcastCandidateBlockToMiners.class,
                            (message, link) -> message.block = bl));
        });
    }
}