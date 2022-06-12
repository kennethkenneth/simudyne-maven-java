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
    private final TransactionListPTQ ptq = new TransactionListPTQ();
    private final BlockList blocksBeingVerified = new BlockList();

    /*
        A miner agent, selects a fixed number of transactions (blocklength) from the PTQ to form a candidate block
        of transactions to verify. The miner agent then begins the process of verifying the candidate block of
        transactions. When the candidate block is verified by at least μ other agents, and the candidate block is valid
        (none of the transactions in that candidate block have already been added to the Public Ledger),
        then the candidate block gets added to the Public Ledger.
    */

    public MinerAgent()
    {
        super();
        pl = new PublicLedger();
    }
    public static Action<MinerAgent> addCandidateTransactionsToPTQ() {
        return Action.create(MinerAgent.class, curMinerAgent -> {
            if (curMinerAgent.hasMessagesOfType(Messages.msgTransaction.class))
            {
                curMinerAgent.getMessagesOfType(Messages.msgTransaction.class).forEach(msg->
                    curMinerAgent.ptq.enqueueTransaction(new Transaction(msg.createTick, msg.gas, msg.value,
                            msg.from, msg.to, msg.transactionId)));
            }
        });
    }

    public static Action<MinerAgent> calculateQueueLength()
    {
        return Action.create(MinerAgent.class, curMA -> curMA.gl.queueLength = curMA.ptq.getQueueLength());
    }

    public static Action<MinerAgent> calculateLedgerLength()
    {
        return Action.create(MinerAgent.class,
                    curMA -> {
                        curMA.gl.ledgerLength = curMA.pl.getLedgerSize()*Globals.blockLength;
                    });
    }

    public static Action<MinerAgent> fillUpMinerWalletAddressArray()
    {
        return Action.create(MinerAgent.class, curMA -> curMA.gl.minerWalletAddresses
                .add(new Utils.AddressAgentPair(curMA.walletAddress, curMA)));
    }

    public static Action<MinerAgent> updateMinerLedger()
    {
        return Action.create(MinerAgent.class, curMinerAgent -> {
            //System.out.println("updateMinerLedger() 100");
            if (curMinerAgent.hasMessagesOfType(Messages.msgBlock.class))
            {
                //System.out.println("updateMinerLedger() 200");
                curMinerAgent.getMessagesOfType(Messages.msgBlock.class).forEach(msg->{
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
                    blocksToRemove.forEach(curMinerAgent.blocksBeingVerified::remove);
                });
            }
        });
    }

    public static int getIndexOfMin(BlockList data) {
        if (data.getList().size()==1) return 0;
        String min = "ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ";  //TODO: Add to Globals. Replace with Ethereum actual constant
        int index = 0; // -1;
        for (int i = 0; i < data.size(); i++) {
            String f = data.getList().get(i).getBlockId();
            if(f.compareTo(min)<0){
                min = f;
                index = i;
            }
        }
        return index;
    }

    public static Action<MinerAgent> verifyCandidateBlocksAndWriteToLedger() {
        return Action.create(MinerAgent.class, curMA -> {
            BlockList bli = new BlockList();
            curMA.blocksBeingVerified.getList().stream()
                    .filter(Block::isBlockVerified)
                    .filter(b->b.getVerifiers().contains(curMA))
                    .filter(b->b.previousBlockId.compareTo(curMA.pl.getLastBlockID())==0)
                    .forEach(bli::append);

            if (!bli.getList().isEmpty())
            {
                String minBlockId = bli.getList().get(getIndexOfMin(bli)).getBlockId();
                Block b = bli.get(minBlockId);
                curMA.pl.addBlock(b);
                curMA.blocksBeingVerified.remove(b);
                curMA.ptq.removeTransactionsOfBlock(b);
                bli.remove(b);
                //bli.getList().forEach(bl->{bl.getTransactions().forEach(curMA.ptq::enqueueTransaction);});    //???
                curMA.getLinks(Links.MinerToMinerLink.class).send(Messages.msgBlock.class,
                                (message, link) -> message.block = b);
                curMA.getLinks(Links.MinerToMarketLink.class).send(Messages.msgBlock.class,
                        (message, link) -> message.block = b);

            }
        });
    }

    public static Action<MinerAgent> sumETHValue()
    {
        return Action.create(MinerAgent.class, currMA ->
                {
                    currMA.gl.totalETHValueInMiners+=currMA.getBalance();
                    //String trans;
                    //System.out.println("Miner Transactions @" + currMA.walletAddress + ": " + currMA.pl.toString2());
                    currMA.gl.ledgerBlocks = currMA.gl.ledgerBlocks + "[" + currMA.walletAddress + " ($" + currMA.getBalance() + ") " + currMA.pl.getLedgerSize() + " blocks, Last:" + currMA.pl.getLastBlockIDShort() + ", branches:" + currMA.pl.getNumBranches() + "]";
                });
    }

    public Block getBlockFromPTQ()
    {
        //Give a temporary random Block ID
        Block b = new Block(this);
        b.previousBlockId=pl.getLastBlockID();
        //peek()	Retrieves, but does not remove, the head of this queue, or returns null if this queue is empty.
        //poll()	Retrieves and removes the head of this queue, or returns null if this queue is empty.
        Transaction trans = ptq.poll();
        /*while ((trans != null) && trans.isVerified() && !b.isBlockVerified() && b.getTransactions().size() < gl.blockLength)*/
        while( (trans!=null) && (b.getTransactions().size() < Globals.blockLength) && (!b.isBlockVerified()) )
        {
            if (trans.isVerified())
            {
                b.appendTransaction(trans);
            }
            if (b.getSize() < Globals.blockLength)
                trans = ptq.poll();
        }
        if (b.getTransactions().size()==Globals.blockLength)
        {
            b.addVerifiers(this);
            b.getTransactions().forEach(ptq::removeTransaction);
        }
        return b;
    }

    public static Action<MinerAgent> receiveCandidateBlocks(){
        return Action.create(MinerAgent.class, curMA -> {
            if (curMA.hasMessagesOfType(Messages.msgCandidateBlock.class)) {
                  curMA.getMessagesOfType(Messages.msgCandidateBlock.class)
                    .forEach(msg->{
                      BlockList bli = curMA.blocksBeingVerified;
                      if (!bli.contains(msg.block.getBlockId()))
                      {
                          Block blo = msg.block.cloneBlock(curMA);
                          if (blo.verifyTransactions())
                          {
                              blo.addVerifiers(curMA);
                          }
                          bli.append(blo);
                      }
                      else
                      {
                          Block blo = bli.get(msg.block.getBlockId());
                          msg.block.getVerifiers().forEach(blo::addVerifiers);
                      }
                  });
            }
        });
    }

    public static Action<MinerAgent> createAndBroadcastCandidateBlocks() {
        return Action.create(MinerAgent.class, curMA -> {
            if (curMA.ptq.getQueueLength()>=Globals.blockLength) {
                Block bl = curMA.getBlockFromPTQ();
                bl.previousBlockId=curMA.pl.getLastBlockID();       //!!!
                if (bl.verifyTransactions())
                {
                    bl.addVerifiers(curMA);
                    curMA.blocksBeingVerified.append(bl);
                }
            }
            System.out.println("Number of blocks being verified:" + curMA.blocksBeingVerified.size());
            curMA.blocksBeingVerified.getList().forEach(bl->
                    curMA.getLinks(Links.MinerToMinerLink.class).send(Messages.msgCandidateBlock.class,
                            (message, link) -> message.block = bl));
        });
    }
    public TransactionListPTQ getPTQ()
    {
        return ptq;
    }
    public int getBalanceFor(int walletAddress)
    {
        System.out.println("Miner Agent address: " + walletAddress);
        System.out.println("gl.minerWalletAddresses: " + gl.minerWalletAddresses);
        WalletAgent ma = gl.minerWalletAddresses.getByAddress(walletAddress);
        System.out.println("(MinerAgent) getBalanceFor " + ma + ".");
        if (ma == null) return 0;
        return pl.getBalance(gl.minerWalletAddresses.getByAddress(walletAddress));
    }
    public static Action<MinerAgent> displayBlocks(){
        return Action.create(MinerAgent.class, curMA -> {
            //System.out.println("displayBlocks(): " + curMA.walletAddress);
            System.out.println("Ledger (@Miner Agent):" + curMA.pl.toString());
        });
    }
}