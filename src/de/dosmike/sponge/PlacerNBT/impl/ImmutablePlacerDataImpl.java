package de.dosmike.sponge.PlacerNBT.impl;

import de.dosmike.sponge.PlacerNBT.ImmutablePlacerData;
import de.dosmike.sponge.PlacerNBT.Placer;
import de.dosmike.sponge.PlacerNBT.PlacerData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.manipulator.immutable.common.AbstractImmutableSingleData;
import org.spongepowered.api.data.value.immutable.ImmutableValue;

import java.util.UUID;

public class ImmutablePlacerDataImpl extends AbstractImmutableSingleData<UUID, ImmutablePlacerData, PlacerData> implements ImmutablePlacerData {

    public ImmutablePlacerDataImpl() {
        this(new UUID(0, 0));
    }

    public ImmutablePlacerDataImpl(UUID value) {
        super(value, Placer.PLACER);
    }

    @Override
    public PlacerData asMutable() {
        return new PlacerDataImpl(this.value);
    }

    @Override
    public DataContainer toContainer() {
        return super.toContainer()
                .set(Placer.PLACER.getQuery(), this.value.toString());
    }

    @Override
    public ImmutableValue<UUID> placerUniqueId() {
        return Sponge.getRegistry().getValueFactory()
                .createValue(Placer.PLACER, getValue()).asImmutable();
    }

    @Override
    protected ImmutableValue<?> getValueGetter() {
        return placerUniqueId();
    }

    @Override
    public int getContentVersion() {
        return 1;
    }
}
