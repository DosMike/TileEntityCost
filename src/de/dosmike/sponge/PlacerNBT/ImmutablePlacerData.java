package de.dosmike.sponge.PlacerNBT;

import org.spongepowered.api.data.manipulator.ImmutableDataManipulator;
import org.spongepowered.api.data.value.immutable.ImmutableValue;

import java.util.UUID;

public interface ImmutablePlacerData extends ImmutableDataManipulator<ImmutablePlacerData, PlacerData> {

    ImmutableValue<UUID> placerUniqueId();

}
