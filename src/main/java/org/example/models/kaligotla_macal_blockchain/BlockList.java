package org.example.models.kaligotla_macal_blockchain;

import java.util.ArrayList;

public class BlockList{
    private final ArrayList<Block> blockList;
    BlockList(){
        blockList = new ArrayList<>();
    }
    public boolean contains(String blockId)
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

    public Block get(String blockId)
    {
        Block blockSearch = null;
        for (Block b : blockList) {
            if (b.getBlockId().compareTo(blockId)==0) {
                blockSearch = b;
            }
        }
        return blockSearch;
    }

    public void append(Block b)
    {
        if (get(b.getBlockId())==null)
        {
            blockList.add(b);
        }
    }

    public void remove(Block b)
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