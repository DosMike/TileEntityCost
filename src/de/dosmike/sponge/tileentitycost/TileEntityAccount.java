package de.dosmike.sponge.tileentitycost;

import de.dosmike.sponge.autosql.AutoSQL;
import de.dosmike.sponge.autosql.H2Column;

import java.math.BigInteger;
import java.util.UUID;

public class TileEntityAccount {

    @H2Column(size = 36, method = AutoSQL.ReconstructionMethod.FROMSTRING)
    public UUID playerID;

    @H2Column(size = 32, method = AutoSQL.ReconstructionMethod.CONSTRUCTOR)
    public BigInteger balance;

    /** for database */
    public TileEntityAccount() {}
    public TileEntityAccount(UUID playerID) {
        this.playerID = playerID;
        this.balance = BigInteger.valueOf(TileEntityCost.getDefaultBalance());
    }

    public boolean canAfford(BigInteger cost) {
        return balance.compareTo(cost) >= 0;
    }

    public void withdraw(BigInteger cost) {
        if (cost.compareTo(BigInteger.ZERO)<=0) return;
        if (cost.compareTo(balance)<=0)
            balance = balance.subtract(cost);
        else //don't allow negative balances for tec accounts
            balance = BigInteger.ZERO;
    }

    public void deposit(BigInteger cost) {
        if (cost.compareTo(BigInteger.ZERO)<=0) return;
        balance = balance.add(cost);
    }

    public BigInteger getBalance() {
        return balance;
    }
}
