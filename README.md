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

| Permission                           | Description                                          |
| ------------------------------------ | ---------------------------------------------------- |
| `tec.account.base`                   | Enable TileEntity Limiting for this player           |
| `tec.command.tecbalance.base`        | Use /tecbalance to see how much points you got left  |
| `tec.command.tecbalance.others`      | Use /tecbalance &lt;Name> to see how much points someone else got  |
| `tec.command.tecbalance.setdefault`  | Use /tecbalance default &lt;Amount> to set the default balance for new players  |
| `tec.command.teccost.base`           | Use /teccost to check how much points placing the block in your hand will cost  |
| `tec.command.teccost.set`            | Use /teccost set &lt;all|mod|block> <amount> to change the default/mod/block cost per TE   |
| `tec.command.techelp.base`           | Use /techelp display avaialable commands and usages  |
| `tec.command.tecpoints.base`         | Use /tecpoints or /tecs to change someones point balance  |
| `tec.command.tecsave.base`           | Use /tecsave to write all configs  |

## Commands

`/tecbalance [User]` will check how much TEC-Points a player has.    
`/tecbalance default <Amount>` will set the amount of TEC-Points a new player starts with.    
`/teccost` will check how much TEC-Points the currently held block will consume when placed / give when broken.    
`/teccost set all <amount>` will set the default price for Tile Entities.    
`/teccost set <mod|block> <amount>` will start the cost puncher, to mass-set the costs for TEs from specific mods or for single blocks.    
`/techelp` will display all available commands and how to use them.     
`/tecpoints <Player> <give|take|set> <Amount>` will change the TEC-Point balance for Player accordingly. A negative balance is no possible.    
`/tecsave` will write the config to disk.