package org.example.models.kaligotla_macal_blockchain;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;

public class WalletAgent extends Agent<Globals> {
    protected int w;   //agents’ current balance of currency or value
    protected int walletAddress;
    protected Globals gl;
    public static Action<WalletAgent> assignWalletAddress()
    {
        return Action.create(WalletAgent.class, currWA->
            currWA.walletAddress = (int) currWA.getPrng().uniform(0, currWA.gl.maxWalletId).sample()
        );
    }
}