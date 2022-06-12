package org.example.models.kaligotla_macal_blockchain;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.HashMap;

public class Utils {

    static class AddressPair
    {
        public int originWallet;
        public int destinationWallet;
        AddressPair(int o, int d)
        {
            originWallet = o;
            destinationWallet = d;
        }
    }
    static class AddressAgentPair
    {
        public int walletAddress;
        public WalletAgent walletAgent;
        AddressAgentPair(int walletAddress, WalletAgent walletAgent)
        {
            this.walletAddress = walletAddress;
            this.walletAgent = walletAgent;
        }
    }
    static class AddressAgentMap
    {
        private final HashMap<Integer, WalletAgent> addressAgentMap;
        AddressAgentMap()
        {
            addressAgentMap = new HashMap<>();
        }
        public boolean add(AddressAgentPair addressAgent)
        {
            Integer addressInt = addressAgent.walletAddress;
            if (addressAgentMap.containsKey(addressInt))
            {
                return false;
            }
            addressAgentMap.put(addressInt, addressAgent.walletAgent);
            return true;
        }
        public WalletAgent getByAddress(int walletAddress)
        {
            //System.out.println("getByAddress()...");
            Integer addressInt = walletAddress;
            return addressAgentMap.get(addressInt);
        }
        public void removeByAddress(int walletAddress)
        {
            Integer addressInt = walletAddress;
            addressAgentMap.remove(addressInt);
        }
        public int size()
        {
            return addressAgentMap.size();
        }
        public ArrayList<Integer> getAddresses()
        {
            return Lists.newArrayList(addressAgentMap.keySet());
        }
        public ArrayList<WalletAgent> getAgents()
        {
            return Lists.newArrayList(addressAgentMap.values());
        }
    }
}
