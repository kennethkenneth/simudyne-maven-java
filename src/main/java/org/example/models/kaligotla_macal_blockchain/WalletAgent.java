package org.example.models.kaligotla_macal_blockchain;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.abm.testkit.TestKit;

public class WalletAgent extends Agent<Globals> {
    protected int walletAddress;
    protected Globals gl;
    public PublicLedger pl;

    WalletAgent()
    {
        System.out.println("WalletAgent() constructor");
        pl =  new PublicLedger();
    }
    public static Action<WalletAgent> assignWalletAddress()
    {
        return Action.create(WalletAgent.class, currWA->
            currWA.walletAddress = (int) Globals.random.uniform(0, Globals.maxWalletId).sample()
        );
    }

    public int getBalance()
    {
        return pl.getBalance(this);
    }

    public int getBalanceFor(int walletAddress)
    {
        //return pl.getBalance(wa);
        return 1000000000;   //TODO: To be implemented
    }
}