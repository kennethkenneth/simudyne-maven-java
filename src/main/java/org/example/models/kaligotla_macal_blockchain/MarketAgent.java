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
                        " blocks, Last:" + currMA.pl.getLastBlockIDShort() + ", branches:" + currMA.pl.getNumBranches() + "]";
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
                                message.transactionId = msg.transactionId;
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
                            (int) Globals.random.uniform(0, Globals.maxTransactionId).sample()))
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
                        (int) Globals.random.uniform(0, Globals.maxTransactionId).sample()));
            }
            String lastBlockId=Globals.START_BLOCK_ID;
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
                curMA.pl.addBlock(b);
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
            int value = (int) Globals.random.uniform(0, 100).sample();                 // TODO: Improve
            if (curMA.getBalance() > gas + value)
            {
                curMA.getLinks(Links.MarketToMarketLink.class)
                .send(Messages.candidateTransactionMessage.class, (message, link) -> {
                                message.gas = gas;
                                message.from = curMA.walletAddress;
                                message.to = wp.destinationWallet;
                                message.value = value;
                                message.createTick = tick;
                                message.transactionId = (int) Globals.random.uniform(0, Globals.maxTransactionId).sample();
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
            for (int i=0;i<(int)Globals.random.uniform(curMA.gl.minTransactions, curMA.gl.maxTransactions).sample();i++)
            {
                from = walletAddresses.get((int)Globals.random.uniform(0, walletAddresses.size()).sample());
                walletAddresses.remove(from);
                to = walletAddresses.get((int)Globals.random.uniform(0, walletAddresses.size()).sample());
                walletAddresses.remove(to);
                wpa.add(new WalletPair(from,to));
            }
            generateTransactions(curMA,tick, wpa);
        });
    }
}