package org.example.models.kaligotla_macal_blockchain;
import org.example.models.kaligotla_macal_blockchain.BlockchainModel.WalletPair;
import simudyne.core.abm.Action;
import java.util.ArrayList;

public class MarketAgent extends WalletAgent{
    public static Action<MarketAgent> assignCoinbaseAddress()
    {
        return Action.create(MarketAgent.class, currMA->{
            double coinbaseAgentWalletAddress = Globals.maxWalletId;
            if (currMA.gl.coinbaseAgent!=null)
            {
                coinbaseAgentWalletAddress = currMA.gl.coinbaseAgent.walletAddress;
            }
            if (currMA.walletAddress<coinbaseAgentWalletAddress)
            {
                currMA.gl.coinbaseAgent=currMA;
            }
        });
    }

    public static Action<MarketAgent> sumETHValue()
    {
        return Action.create(MarketAgent.class, currMA ->
            {
                if (currMA.walletAddress!=currMA.gl.coinbaseAgent.walletAddress)
                {
                    currMA.gl.totalETHValueInMarkets+=currMA.getBalance();
                }
                currMA.gl.ledgerBlocks = currMA.gl.ledgerBlocks + "[" + currMA.walletAddress +
                        " ($" + currMA.getBalance() + ") " + currMA.pl.getLedgerSize() +
                        " blocks, Last:" + currMA.pl.getLastBlockIDShort() +"]";
            });
    }

    public static Action<MarketAgent> broadcastTransactionsToMiners() {
        return Action.create(MarketAgent.class, curMarketAgent -> {
            if (curMarketAgent.hasMessagesOfType(Messages.candidateTransactionMessage.class))
            {
                Messages.candidateTransactionMessage msg =
                        curMarketAgent.getMessageOfType(Messages.candidateTransactionMessage.class);
                if (msg.to==curMarketAgent.walletAddress)
                {
                    curMarketAgent.getLinks(Links.MarketToMinerLink.class)
                            .send(Messages.broadcastTransactionsToMinersPTQ.class, (message, link) -> {
                                message.gas = msg.gas;
                                message.from = msg.from;
                                message.to = msg.to;
                                message.value = msg.value;
                                message.createTick = msg.createTick;
                            });
                }
            }
        });
    }

    public static Action<MarketAgent> sendMoneyFromCoinbaseToMarkets()
    {
        return Action.create(MarketAgent.class, curMA -> {
            ArrayList<Transaction> trAL = new ArrayList<>();
            if (curMA.walletAddress != curMA.gl.coinbaseAgent.walletAddress) return;
            ArrayList<Integer> addresses =curMA.gl.marketWalletAddresses;
            addresses.remove(Integer.valueOf(curMA.gl.coinbaseAgent.walletAddress));
            addresses.forEach(
                    wa-> trAL.add(new Transaction(0,0,curMA.gl.initialMarketAgentBalance,
                            curMA.gl.coinbaseAgent.walletAddress, wa,
                            (int) curMA.gl.random.uniform(0, Globals.maxTransactionId).sample()))
            );
            //Generate empty "padding" transactions to fill blocks
            System.out.println("trAL.size(): " + (trAL.size()));
            System.out.println("curMA.gl.blockLength:" + Globals.blockLength);
            System.out.println("numPaddingTrans: " + (Globals.blockLength - trAL.size() % Globals.blockLength));
            int numPaddingTrans = Globals.blockLength - trAL.size() % Globals.blockLength;
            for (int i =0; i<numPaddingTrans; i++)
            {
                trAL.add(new Transaction(0,0,0,curMA.gl.coinbaseAgent.walletAddress,
                        curMA.gl.coinbaseAgent.walletAddress,
                        //(int) curMA.getPrng().uniform(0,curMA.gl.maxTransactionId).sample()));
                        (int) Globals.random.uniform(0, Globals.maxTransactionId).sample()));
            }
            String lastBlockId=Globals.START_BLOCK_ID;
            System.out.println("range: " + (trAL.size() / Globals.blockLength));
            for (int i=0; i<trAL.size() / Globals.blockLength ;i++)
            {
                Block b = new Block(curMA);
                for (int j=0;j<Globals.blockLength;j++)
                {
                    System.out.println("n=" + (Globals.blockLength*i+j));
                    System.out.println("trId=" + trAL.get(Globals.blockLength*i+j).transactionId);
                    b.appendTransaction(trAL.get(Globals.blockLength*i+j));
                }
                b.previousBlockId=lastBlockId;
                boolean ab = curMA.pl.addBlock(b);
                lastBlockId=b.getBlockId();
                curMA.getLinks(Links.MarketToMinerLink.class).send(Messages.broadcastBlockToLedgers.class,
                        (message, link) -> message.block = b);
                curMA.getLinks(Links.MarketToMarketLink.class).send(Messages.broadcastBlockToLedgers.class,
                        (message, link) -> message.block = b);
            }
        });
    };

    public static Action<MarketAgent> fillUpMarketWalletAddressArray()
    {
        return Action.create(MarketAgent.class, curMA -> curMA.gl.marketWalletAddresses.add(curMA.walletAddress));
    }

    public static void generateTransactions(MarketAgent curMA, long tick, ArrayList<WalletPair> wpa)
    {
        Globals gl = curMA.getGlobals();
        wpa.forEach(wp->{
            if (curMA.walletAddress!=wp.originWallet ||
                curMA.walletAddress==curMA.gl.coinbaseAgent.walletAddress)  return;
            int gas = gl.gasFee; // (int) curMA.getPrng().uniform(0,gl.gasFee).sample();     // TODO: Improve
            //int value = (int) curMA.getPrng().uniform(0,100).sample();                     // TODO: Improve
            int value = (int) curMA.gl.random.uniform(0, 100).sample();                 // TODO: Improve
            if (curMA.getBalance() > gas + value)
            {
                //System.out.println("Sufficient funds");
                curMA.getLinks(Links.MarketToMarketLink.class)
                .send(Messages.candidateTransactionMessage.class, (message, link) -> {
                                message.gas = gas;
                                message.from = curMA.walletAddress;
                                message.to = wp.destinationWallet;
                                message.value = value;
                                message.createTick = tick;
                            });
            } else {
                System.out.println("Insufficient funds");
            }
        });
    }

    public static Action<MarketAgent> generateRandomTransactions(long tick) {
        return Action.create(MarketAgent.class, curMA ->
                {
                    ArrayList<WalletPair> wpa = new ArrayList<>();
                    ArrayList<Integer> walletAddresses = (ArrayList<Integer>)curMA.gl.marketWalletAddresses.clone();
                    Integer from, to;
                    for (int i=0;i<20;i++)
                    {
                        from = walletAddresses.get((int)curMA.gl.random.uniform(0, walletAddresses.size()).sample());
                        walletAddresses.remove(from);
                        to = walletAddresses.get((int)curMA.gl.random.uniform(0, walletAddresses.size()).sample());
                        walletAddresses.remove(to);
                        WalletPair wp = new WalletPair(from,to);
                        wpa.add(wp);
                    }
                    generateTransactions(curMA,tick, wpa);
                });
    }
}