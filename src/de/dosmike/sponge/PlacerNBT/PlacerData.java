package de.dosmike.sponge.PlacerNBT;

import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.data.value.mutable.Value;

import java.util.UUID;

public interface PlacerData extends DataManipulator<PlacerData, ImmutablePlacerData> {

    Value<UUID> placerUniqueId();

}
