package com.azukaar.ass;
import java.util.function.Supplier;

import org.slf4j.Logger;

import com.azukaar.ass.capabilities.IPlayerSkills;
import com.azukaar.ass.capabilities.PlayerSkillsProvider;
import com.azukaar.ass.client.ExpertiseParticleHandler;
import com.azukaar.ass.client.GUIClientModEvents;
import com.azukaar.ass.client.KeybindRegistry;
import com.azukaar.ass.client.particles.OrbParticle;
import com.azukaar.ass.client.particles.OrbParticleOptions;
import com.mojang.logging.LogUtils;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(AzukaarSkillsStats.MODID)
public class AzukaarSkillsStats
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "azukaarskillsstats";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "azukaarskillsstats" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "azukaarskillsstats" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "azukaarskillsstats" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Creates a new Block with the id "azukaarskillsstats:example_block", combining the namespace and path
    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
    // Creates a new BlockItem with the id "azukaarskillsstats:example_block", combining the namespace and path
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);

    // Creates a new food item with the id "azukaarskillsstats:example_id", nutrition 1 and saturation 2
    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", new Item.Properties().food(new FoodProperties.Builder()
            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));
            
    public static final EntityCapability<IPlayerSkills, Void> PLAYER_SKILLS = 
        EntityCapability.createVoid(
            ResourceLocation.fromNamespaceAndPath("ass", "player_skills"),
            IPlayerSkills.class
        );
    // Add this after your existing DeferredRegister declarations
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = 
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);

    // Register your PlayerSkillsProvider as an attachment
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerSkillsProvider>> PLAYER_SKILLS_ATTACHMENT = 
        ATTACHMENT_TYPES.register("player_skills", 
            () -> AttachmentType.serializable(() -> new PlayerSkillsProvider()).build()
        );

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = 
        DeferredRegister.create(Registries.PARTICLE_TYPE, AzukaarSkillsStats.MODID); // Replace with your mod ID

    public static final Supplier<ParticleType<OrbParticleOptions>> ORB_PARTICLE = 
        PARTICLE_TYPES.register("orb", () -> new ParticleType<OrbParticleOptions>(false) {
            @Override
            public MapCodec<OrbParticleOptions> codec() {
                return OrbParticleOptions.CODEC.fieldOf("orb");
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, OrbParticleOptions> streamCodec() {
                return StreamCodec.of(
                    (buffer, options) -> options.writeToNetwork(buffer),
                    (buffer) -> OrbParticleOptions.fromNetwork(this, buffer)
                );
            }
        });
        
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.azukaarskillsstats")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public AzukaarSkillsStats(IEventBus modEventBus, ModContainer modContainer)
    {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);
        
        ModAttributes.ATTRIBUTES.register(modEventBus);
        ModMobEffects.registerAll(modEventBus);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

        ATTACHMENT_TYPES.register(modEventBus);

        // Register the Deferred Register to the mod event bus so particle types get registered
        PARTICLE_TYPES.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (AzukaarSkillsStats) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        
        NeoForge.EVENT_BUS.register(ModEvents.class);

        BuiltinActiveEffects.registerAll();

        // // Debug values
        // System.out.println("Level from 150 XP: " + PlayerPath.getLevelFromXp(150));
        // System.out.println("Level from 300 XP: " + PlayerPath.getLevelFromXp(300));
        // System.out.println("Level from 500 XP: " + PlayerPath.getLevelFromXp(500));
        // System.out.println("Level from 1000 XP: " + PlayerPath.getLevelFromXp(1000));

        // System.out.println("XP for lvl1: " + PlayerPath.getTotalXpForLevel(1));
        // System.out.println("XP for lvl2: " + PlayerPath.getTotalXpForLevel(2));
        // System.out.println("XP for lvl5: " + PlayerPath.getTotalXpForLevel(5));
        // System.out.println("XP for lvl10: " + PlayerPath.getTotalXpForLevel(10));
        // System.out.println("XP for lvl20: " + PlayerPath.getTotalXpForLevel(20));
        /*

        int i = 0;
        while(i < 150) {
            System.out.println(i + "," + PlayerPath.getTotalXpForLevel(i) + 
               "," + PlayerManager.getPlayerMainLevel(i) +
               "," + PlayerManager.getPlayerMainLevel(i* 2) +
               "," + PlayerManager.getPlayerMainLevel(i * 3) +
               "," + PlayerManager.getPlayerMainLevel(i * 4)
            );
            i++;
        }

        // exit program
        System.exit(0);*/
    } 

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            event.accept(EXAMPLE_BLOCK_ITEM);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerEntity(
            PLAYER_SKILLS, // Use appropriate capability type
            EntityType.PLAYER,
            (player, context) -> player.getData(PLAYER_SKILLS_ATTACHMENT.get()).getSkills()
        );
    }
    
    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        // Register your skill manager with the datapack reload system
        event.addListener(SkillDataManager.INSTANCE);
        LOGGER.info("Registered SkillDataManager for datapack reloading");
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            NeoForge.EVENT_BUS.register(ExpertiseParticleHandler.class);
            NeoForge.EVENT_BUS.register(GUIClientModEvents.class);
                        
            KeybindRegistry.getInstance().init();

            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
        
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(GUIClientModEvents.OPEN_SKILLS_KEY);
        }

        @SubscribeEvent
        public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(ORB_PARTICLE.get(), OrbParticle.Provider::new);
        }
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class CommonModEvents
    {
        @SubscribeEvent
        public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
            // Add custom attributes to players
            event.add(EntityType.PLAYER, ModAttributes.HUNGER_EFFICIENCY);
            event.add(EntityType.PLAYER, ModAttributes.MINING_SPEED);
        }
    }

    @EventBusSubscriber(modid = AzukaarSkillsStats.MODID)
    public class CommandEventHandler {
        
        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            AssCommand.register(event.getDispatcher());
        }
    }
}
