package de.dosmike.sponge.PlacerNBT;

import com.google.common.reflect.TypeToken;
import de.dosmike.sponge.PlacerNBT.impl.ImmutablePlacerDataImpl;
import de.dosmike.sponge.PlacerNBT.impl.PlacerDataImpl;
import de.dosmike.sponge.PlacerNBT.impl.PlacerDataManipulatorBuilder;
import de.dosmike.sponge.tileentitycost.TileEntityCost;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataManager;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameRegistryEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.util.generator.dummy.DummyObjectProvider;

import java.util.UUID;

public class Placer {

    public static Key<Value<UUID>> PLACER = DummyObjectProvider.createExtendedFor(Key.class, "PLACER");

    private PluginContainer container = TileEntityCost.getInstance().getContainer();
    private DataRegistration<PlacerData, ImmutablePlacerData> PLACER_DATA_REGISTRATION;

    @Listener
    public void onKeyRegistration(GameRegistryEvent.Register<Key<?>> event) {
        PLACER = Key.builder()
                .type(new TypeToken<Value<UUID>>() {})
                .id("placer")
                .name("Tile Entity Placer")
                .query(DataQuery.of("Placer"))
                .build();
        event.register(PLACER);
    }

    @Listener
    public void onDataRegistration(GameRegistryEvent.Register<DataRegistration<?, ?>> event) {
        this.PLACER_DATA_REGISTRATION = DataRegistration.builder()
                .dataClass(PlacerData.class)
                .immutableClass(ImmutablePlacerData.class)
                .dataImplementation(PlacerDataImpl.class)
                .immutableImplementation(ImmutablePlacerDataImpl.class)
                .builder(new PlacerDataManipulatorBuilder())
                .dataName("Block Placer Data")
                .manipulatorId("placerreg")
                .buildAndRegister(this.container);
    }

}
