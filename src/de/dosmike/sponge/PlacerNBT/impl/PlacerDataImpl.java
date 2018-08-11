package de.dosmike.sponge.PlacerNBT.impl;

import de.dosmike.sponge.PlacerNBT.ImmutablePlacerData;
import de.dosmike.sponge.PlacerNBT.Placer;
import de.dosmike.sponge.PlacerNBT.PlacerData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.mutable.common.AbstractSingleData;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.value.mutable.Value;

import java.util.Optional;
import java.util.UUID;

public class PlacerDataImpl extends AbstractSingleData<UUID, PlacerData, ImmutablePlacerData> implements PlacerData {

    public PlacerDataImpl() {
        this(new UUID(0, 0));
    }

    public PlacerDataImpl(UUID placerUuid) {
        super(placerUuid, Placer.PLACER);
    }

    @Override
    public Optional<PlacerData> fill(DataHolder dataHolder, MergeFunction overlap) {
        PlacerData merged = overlap.merge(this, dataHolder.get(PlacerData.class).orElse(null));
        setValue(merged.placerUniqueId().get());
        return Optional.of(this);
    }

    @Override
    public Optional<PlacerData> from(DataContainer container) {
        return from((DataView)container);
    }

    public Optional<PlacerData> from(DataView container) {
        if (container.contains(Placer.PLACER)) {
            UUID placer = container.getObject(Placer.PLACER.getQuery(), UUID.class).get();
            return Optional.of(setValue(placer));
        }

        return Optional.empty();
    }

    @Override
    public PlacerData copy() {
        return new PlacerDataImpl(getValue());
    }

    @Override
    public ImmutablePlacerData asImmutable() {
        return new ImmutablePlacerDataImpl(getValue());
    }

    @Override
    public int getContentVersion() {
        return 1;
    }

    @Override
    public DataContainer toContainer() {
        return super.toContainer()
                .set(Placer.PLACER.getQuery(), getValue());
    }

    @Override
    public Value<UUID> placerUniqueId() {
        return Sponge.getRegistry().getValueFactory()
                .createValue(Placer.PLACER, getValue());
    }

    @Override
    protected Value<?> getValueGetter() {
        return placerUniqueId();
    }



}
