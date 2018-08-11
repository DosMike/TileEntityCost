package de.dosmike.sponge.PlacerNBT.impl;

import de.dosmike.sponge.PlacerNBT.ImmutablePlacerData;
import de.dosmike.sponge.PlacerNBT.PlacerData;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.DataManipulatorBuilder;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.value.ValueContainer;

import javax.annotation.Nullable;
import java.util.Optional;

public class PlacerDataManipulatorBuilder extends AbstractDataBuilder<PlacerData> implements DataManipulatorBuilder<PlacerData, ImmutablePlacerData> {

    public PlacerDataManipulatorBuilder() {
        super(PlacerData.class, 1);
    }

    @Override
    public PlacerDataImpl create() {
        return new PlacerDataImpl();
    }

    @Override
    public Optional<PlacerData> createFrom(DataHolder dataHolder) {
        return new PlacerDataImpl().fill(dataHolder, new MergeFunction() {
            @Override
            public <C extends ValueContainer<?>> C merge(@Nullable C original, @Nullable C replacement) {
                return replacement;
            }
        });
    }

    @Override
    public Optional<PlacerData> buildContent(DataView container) throws InvalidDataException {
        return create().from(container);
    }
}
