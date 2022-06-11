package org.example.models.kaligotla_macal_blockchain;

import simudyne.core.abm.GlobalState;
import simudyne.core.annotations.Input;
import simudyne.core.rng.SeededRandom;

public final class Globals extends GlobalState
{
    int totalETHValueInMiners = 0;
    int totalETHValueInMarkets = 0;
    int queueLength=0;
    int ledgerLength=0;
    public static final double maxBlockId = 10000000;                   //TODO: Replace with actual Ethereum constants
    public static final double maxTransactionId = 1000000000;           //TODO: Replace with actual Ethereum constants
    public static final int maxWalletId = 1000000000;                   //TODO: Replace with actual Ethereum constants
    public static final String START_BLOCK_ID = "START";

    Utils.AddressAgentMap marketWalletAddresses = new Utils.AddressAgentMap();
    Utils.AddressAgentMap minerWalletAddresses = new Utils.AddressAgentMap();

    MarketAgent coinbaseAgent;

    String ledgerBlocks = "";

    public static final long seed = 645902744249333L;                                    //TODO: Use getPRNG() where possible
    public static SeededRandom random = SeededRandom.create(seed);
    public static int blockLength = 5;  //number of transactions in a block from the PTQ //TODO: Adapt to Ethereum
    public static int agentsToVerifyTrans = 5; // number of miner agents the network requires
    // to verify a block. Also known as mu. (?) //TODO: Adapt to Ethereum

    /*list of relative gas levels agents can choose from to indicate the priority of a transaction
    highGas = 10, mediumGas = 5, lowGas = 2 */
    @Input(name = "Gas Fee (Ξ)")
    public int gasFee = 20;

    @Input(name = "Delta T (sec.)")
    public double deltaT = 15; //the time increment between the Blockchain system updates (secs.)

    @Input(name = "Simulation Time (sec.)")
    public double simTime = 10*60; // we simulate a Blockchain for a period of 10 minutes

    //@Input(name = "Network Reward (Ξ)")
    //public int networkReward = 5; // network reward split among the miner agents who
    // verify a block of transactions (?)

    @Input(name = "Number of Agents")
    public int numAgents = 50; // Number of Agents in the simulation (?)

    @Input(name = "Fraction of Miner Agents")
    public float fracMiners = (float) 0.10; // Fraction of agents who are miner agents

    /*
    tBegin is the beginning time for the simulation. tEnd is the ending time for the simulation.
    scenarioResults is the log of results for the simulation run.
    */

    // TODO: Find out numbers used in the actual paper
    @Input(name = "Initial Transactions")
    public int numInitialTransactions = 40;

    @Input(name = "min Transactions every 15 sec.")
    public int minTransactions = 2;//17; // 100,000 / 24 / 60 / 4

    @Input(name = "max Transactions every 15 sec.")
    public int maxTransactions = 20;//70; // 500,000 / 24 / 60 / 4

    //@Input(name = "Initial Transactions")
    //public double probAgentTransacting = 0.8; // (float) (maxTransactions-minTransactions) / numAgents;

    @Input(name = "Initial Market Agent balance (Ξ)")
    public int initialMarketAgentBalance = 1000;

    @Input(name = "Initial Miner Agent balance (Ξ)")
    public int initialMinerAgentBalance = 0;
}
