package org.example.models.kaligotla_macal_blockchain;
import org.example.models.kaligotla_macal_blockchain.BlockchainModel.WalletPair;
import simudyne.core.abm.Action;
import java.util.ArrayList;
import java.util.stream.IntStream;

public class MarketAgent extends WalletAgent{
    public static Action<MarketAgent> provideInitialBalanceToCoinbase()
    {
        return Action.create(MarketAgent.class, currMA->{
            if (currMA.walletAddress==currMA.gl.coinbaseAgent.walletAddress)
            {
                currMA.w = currMA.gl.initialMarketAgentBalance;
            }
        });
    }
    public static Action<MarketAgent> assignCoinbaseAddress()
    {
        return Action.create(MarketAgent.class, currMA->{
            double coinbaseAgentWalletAddress = currMA.gl.maxWalletId;
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

    public static Action<WalletAgent> sumETHValue()
    {
        return Action.create(WalletAgent.class, currMA ->
            currMA.gl.totalETHValueInMarkets+=currMA.w);
    }

    public static Action<MarketAgent> broadcastTransactionsToMiners() {
        return Action.create(MarketAgent.class, curMarketAgent -> {
            if (curMarketAgent.hasMessagesOfType(Messages.candidateTransactionMessage.class))
            {
                //System.out.println("And here...");
                Messages.candidateTransactionMessage msg =
                        curMarketAgent.getMessageOfType(Messages.candidateTransactionMessage.class);
                if (msg.receiverAddress==curMarketAgent.walletAddress)
                {
                    curMarketAgent.getLinks(Links.MarketToMinerLink.class)
                            .send(Messages.broadcastTransactionsToMinersPTQ.class, (message, link) -> {
                                message.gas = msg.gas;
                                message.senderAddress = msg.senderAddress;
                                message.receiverAddress = msg.receiverAddress;
                                message.value = msg.value;
                                message.createTick = msg.createTick;
                            });
                }
            }
        });
    }

    public static Action<MarketAgent> receiveTransferValue()
    {
        return Action.create(MarketAgent.class, curMA->{
            if (curMA.hasMessagesOfType(Messages.transactionValue.class))
            {
                curMA.getMessagesOfType(Messages.transactionValue.class).forEach(msg->{
                    /*System.out.println("NININI. agentJ=" + msg.t.agentJ.walletAddress +
                            ", curMA=" + curMA.walletAddress );*/
                    //if (msg.t.agentJ.walletAddress==curMA.walletAddress)
                    if (msg.t.receiverAddress==curMA.walletAddress)
                    {
                        //System.out.println("NININI 1: " + curMA.w);
                        curMA.w += msg.t.value;
                        //System.out.println("NININI 2: " + curMA.w);
                    }
                });
            }
        });
    }

    public static Action<MarketAgent> sendMoneyFromCoinbaseToMarkets(long tick)
    {
        return Action.create(MarketAgent.class, curMA -> {
            Globals gl = curMA.getGlobals();
            ArrayList<Integer> recipientAddresses = gl.marketWalletAddresses;
            recipientAddresses.remove(Integer.valueOf(gl.coinbaseAgent.walletAddress));                  // Don't send to oneself
            ArrayList<WalletPair> wpa = new ArrayList<>();
            IntStream.range(0, recipientAddresses.size()-1)
                    .forEach(i -> wpa.add(new WalletPair(gl.coinbaseAgent.walletAddress, recipientAddresses.get(i))));
            generateTransactions(curMA,tick,wpa);
        });
    }

    public static Action<MarketAgent> fillUpMarketWalletAddressArray()
    {
        return Action.create(MarketAgent.class, curMA -> curMA.gl.marketWalletAddresses.add(curMA.walletAddress));
    }

    public static void generateTransactions(MarketAgent curMA, long tick, ArrayList<WalletPair> wpa)
    {
        Globals gl = curMA.getGlobals();
        wpa.forEach(wp->{
            if (curMA.walletAddress!=wp.originWallet)  return;
            int gas = gl.gasFee; // (int) curMA.getPrng().uniform(0,gl.gasFee).sample();                // TODO: Improve
            int value = (int) curMA.getPrng().uniform(0,100).sample();                            // TODO: Improve
            if (curMA.w > gas + value)
            {
                //System.out.println("We do get here some times");
                curMA.w-=(gas+value);
                curMA.getLinks(Links.MarketToMarketLink.class)
                        /*.forEach(l->{
                            System.out.println("1. wp.originWallet: " + wp.originWallet);
                            System.out.println("2. wp.destinationWallet: " + wp.destinationWallet);
                            System.out.println("3. l.getVertex().walletAddress: " + ((MarketAgent)l.getVertex()).walletAddress);
                        });*/
                /*.filter(i -> ((MarketAgent)i.getVertex()).walletAddress == wp.destinationWallet)*/
                .send(Messages.candidateTransactionMessage.class, (message, link) -> {
                                message.gas = gas;
                                message.senderAddress = curMA.walletAddress;
                                message.receiverAddress = wp.destinationWallet;
                                message.value = value;
                                message.createTick = tick;
                            });
            } else {
                System.out.println("Insufficient funds");
            }
        });
    }

    public static Action<MarketAgent> generateRandomTransactions(long tick, ArrayList<WalletPair> wpa) {
        return Action.create(MarketAgent.class, curMA ->
            generateTransactions(curMA,tick, wpa));
    }
}