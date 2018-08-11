# TileEntityCost

Let's you limit the TileEntity consumption on your server by adding a point system.

## Config

```
# This is a map that stores the default TileEntity Cost per Block. The key is the blocks full resource identifier (e.g. minecraft:furnace)
CostsPerBlock={
	"blockID"=cost
}

# This is a map that stores the default TileEntity Cost per ModID
CostsPerMod=={
	"modID"=cost
}

# There is no way to determine from a BlockState or Item/ItemStack if a Block holds a TileEntity or not. /teccost wants to know that tho, so the plugin caches whether a block as a TileEntity when players interact with them
IsTileEntityCache

# Generic configurations and defaults go here
TileEntityCosts {

    # A player will start with this balance when they first join the server
    DefaultBalance

    # If a Tile Entity is placed that's not in one of the lists, it'll cost this many points
    DefaultCost
}
```

## Permissions

| Permission                       | Description                                          |
| -------------------------------- | ---------------------------------------------------- |
| `tec.account.base`               | Enable TileEntity Limiting for this player           |
| `tec.command.tecbalance.base`    | Use /tecbalance to see how much points you got left  |
| `tec.command.tecbalance.others`  | Use /tecbalance &lt;Name> to see how much points someone else got  |
| `tec.command.teccost.base`       | Use /teccost to check how much points placing the block in your hand will cost  |
| `tec.command.tecpoints.base`     | Use /tecpoints or /tecs to change someones point balance  |

## Commands

`/tecbalance [User:tec.account.base]` will check how much TEC-Points a player has.    
`/teccost` will check how much TEC-Points the currently held block will consume when placed / give when broken.    
`/tecpoints <Player> <give|take|set> <Amount>` will change the TEC-Point balance for Player accordingly. A negative balance is no possible.