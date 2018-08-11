package de.dosmike.sponge.tileentitycost;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import de.dosmike.sponge.PlacerNBT.Placer;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializerCollection;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.event.service.ChangeServiceProviderEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.service.user.UserStorageService;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.*;

@Plugin(id="tileentitycost", name="Tile Entity Cost",
        version="0.1", authors={"DosMike"})
public class TileEntityCost {

    public static void main(String[] args) { System.err.println("This plugin can not be run as executable!"); }

    static TileEntityCost instance;
    public static TileEntityCost getInstance() { return instance; }

    private UserStorageService userStorage = null;
    private SpongeExecutorService asyncScheduler = null;
    private SpongeExecutorService syncScheduler = null;

    @Listener
    public void onChangeServiceProvider(ChangeServiceProviderEvent event) {
//        if (event.getService().equals(EconomyService.class)) {
//            economyService = (EconomyService) event.getNewProvider();
//        } else
      if (event.getService().equals(UserStorageService.class)) {
            userStorage = (UserStorageService) event.getNewProvider();
        }
    }
//    public static EconomyService getEconomy() { return instance.economyService; }
    public static UserStorageService getUserStorage() { return instance.userStorage; }
    public static SpongeExecutorService getAsyncScheduler() { return instance.asyncScheduler; }
    public static SpongeExecutorService getSyncScheduler() { return instance.syncScheduler; }

    public PluginContainer getContainer() { return Sponge.getPluginManager().fromInstance(this).get(); }

    @Inject
    private Logger logger;
    public static void l(String format, Object... args) { instance.logger.info(String.format(format, args)); }
    public static void w(String format, Object... args) { instance.logger.warn(String.format(format, args)); }

    public static Random rng = new Random(System.currentTimeMillis());

    /// --- === Main Plugin stuff === --- \\\


    @Inject
    @DefaultConfig(sharedRoot = false)
    private ConfigurationLoader<CommentedConfigurationNode> configManager;
    public static TypeSerializerCollection customSerializer = TypeSerializers.getDefaultSerializers().newChild();

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path privateConfigDir;

    @Listener
    public void onServerPreInit(GamePreInitializationEvent event) {
        instance = this;

        asyncScheduler = Sponge.getScheduler().createAsyncExecutor(this);
        syncScheduler = Sponge.getScheduler().createSyncExecutor(this);

        Sponge.getEventManager().registerListeners(this, new EventListeners());
        Sponge.getEventManager().registerListeners(this, new Placer());
    }

    @Listener
    public void onServerInit(GameInitializationEvent event) {

    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        TileEntityAccountManager.init();
        CommandRegistra.register();

        loadConfigs();

        l("Tile Entity Tracker now active!");
    }

    @Listener
    public void onServerStopping(GameStoppingEvent event) {
        saveConfigs();
    }

    @Listener
    public void onReload(GameReloadEvent event) {
        loadConfigs();
    }

    public void loadConfigs() {
        if (!privateConfigDir.toFile().exists() ||
                !new File(privateConfigDir.toFile(), getContainer().getId()+".conf").exists()) saveConfigs();
        try {
            CommentedConfigurationNode root = configManager.load(configManager.getDefaultOptions());
            CommentedConfigurationNode node, option;

            node = root.getNode("TileEntityCosts");
            defaultBalance = node.getNode("DefaultBalance").getInt(100);
            tecDefault = node.getNode("DefaultCost").getInt(0);

            tecByModID.clear();
            tecByBlockID.clear();
            isTileEntityCache.clear();

            node = root.getNode("CostsPerMod");
            for (Map.Entry<Object, ? extends CommentedConfigurationNode> entry : node.getChildrenMap().entrySet()) {
                addPriceMod(entry.getKey().toString(), entry.getValue().getInt(1));
            }

            node = root.getNode("CostsPerBlock");
            for (Map.Entry<Object, ? extends CommentedConfigurationNode> entry : node.getChildrenMap().entrySet()) {
                addPriceBlock(entry.getKey().toString(), entry.getValue().getInt(1));
            }

            node = root.getNode("IsTileEntityCache");
            for (Map.Entry<Object, ? extends CommentedConfigurationNode> entry : node.getChildrenMap().entrySet()) {
                isTileEntityCache.put(entry.getKey().toString(), entry.getValue().getBoolean());
            }

        } catch (Exception e) {
            w("Could not load config!");
            e.printStackTrace();
        }
    }

    public void saveConfigs() {
        {
            File f = privateConfigDir.toFile();
            if (!f.exists()) f.mkdirs();
        }
        try {
            CommentedConfigurationNode root = configManager.createEmptyNode(configManager.getDefaultOptions());
            CommentedConfigurationNode node, option;

            node = root.getNode("TileEntityCosts");
            node.setComment("Generic configurations and defaults go here");
            option = node.getNode("DefaultCost");
            option.setComment("If a Tile Entity is placed that's not in one of the lists, it'll cost this many points");
            option.setValue(tecDefault);

            option = node.getNode("DefaultBalance");
            option.setComment("A player will start with this balance when they first join the server");
            option.setValue(defaultBalance);

            node = root.getNode("CostsPerMod");
            node.setComment("This is a map that stores the default TileEntity Cost per ModID");
            for (Map.Entry<String, Integer> entry : tecByModID.entrySet())
                node.getNode(entry.getKey()).setValue(entry.getValue());

            node = root.getNode("CostsPerBlock");
            node.setComment("This is a map that stores the default TileEntity Cost per Block. The key is the blocks full resource identifier (e.g. minecraft:furnace)");
            for (Map.Entry<String, Integer> entry : tecByBlockID.entrySet())
                node.getNode(entry.getKey()).setValue(entry.getValue());

            node = root.getNode("IsTileEntityCache");
            node.setComment("There is no way to determine from a BlockState or Item/ItemStack if a Block holds a TileEntity or not. /teccost wants to know that tho, so the plugin caches whether a block as a TileEntity when players interact with them");
            for (Map.Entry<String, Boolean> entry : isTileEntityCache.entrySet())
                node.getNode(entry.getKey()).setValue(entry.getValue());

            configManager.save(root);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /// --- === End Boilerplate Code === --- \\\

    private static Map<String, Integer> tecByBlockID = new HashMap<>();
    private static Map<String, Integer> tecByModID = new HashMap<>();
    private static Integer tecDefault = 0;
    private static int defaultBalance = 100;

    private static Map<String, Boolean> isTileEntityCache = new HashMap<>();

    public static int getDefaultBalance() {
        return defaultBalance;
    }
    public static BigInteger getCostFor(BlockType aFinal) {
        String bt = aFinal.getId();
        int price = tecDefault;

        if (tecByBlockID.containsKey(bt))
            price = tecByBlockID.get(bt);
        else {
            bt = bt.substring(0,bt.indexOf(':'));
            if (tecByModID.containsKey(bt))
                price = tecByModID.get(bt);
        }

        return BigInteger.valueOf(price);
    }
    public static boolean hasTecBlockId(BlockType type) {
        return tecByBlockID.containsKey(type.getId());
    }
    public static boolean hasTecModId(BlockType type) {
        String i = type.getId();
        return tecByModID.containsKey(i.substring(0, i.indexOf(':')));
    }

    public static void regTE(BlockType type, boolean isTE) {
        if (tecByBlockID.containsKey(type.getId())) return;
        isTileEntityCache.put(type.getId(), isTE);
    }
    public static Optional<Boolean> isTileEntity(BlockType type) {
        if (tecByBlockID.containsKey(type.getId())) return Optional.of(true);
        return Optional.ofNullable(isTileEntityCache.get(type.getId()));
    }
    /** try to guess price for item ids */
    public static boolean canGetPrice(String id) {
        return tecByBlockID.containsKey(id) ||
                (isTileEntityCache.containsKey(id) && isTileEntityCache.get(id));
    }

    private void addPrice(String s, int anInt) {
        if (anInt < 0) {
            w("Invalid Price for %s: %d", s, anInt);
            return;
        }
        if (s.indexOf(':')>0) {
            Optional<BlockType> bt = Sponge.getRegistry().getType(BlockType.class, s);
            if (!bt.isPresent()) {
                w("Invalid Block %s", s);
                return;
            }
            tecByBlockID.put(s.toLowerCase(), anInt);
        } else {
            if (!Sponge.getPluginManager().getPlugin(s).isPresent()) {
                w("Invalid ModID %s", s);
                return;
            }
            tecByModID.put(s.toLowerCase(), anInt);
        }
    }
    private void addPriceMod(String s, int anInt) {
        if (anInt < 0) {
            w("Invalid Price for %s: %d", s, anInt);
            return;
        }
        if (!Sponge.getPluginManager().getPlugin(s).isPresent()) {
            w("Invalid ModID %s", s);
            return;
        }
        tecByModID.put(s.toLowerCase(), anInt);
    }
    private void addPriceBlock(String s, int anInt) {
        if (anInt < 0) {
            w("Invalid Price for %s: %d", s, anInt);
            return;
        }
        Optional<BlockType> bt = Sponge.getRegistry().getType(BlockType.class, s);
        if (!bt.isPresent()) {
            w("Invalid Block %s", s);
            return;
        }
        tecByBlockID.put(s.toLowerCase(), anInt);
    }

}
