package gigaherz.eyes;

import gigaherz.eyes.entity.EyesEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.passive.OcelotEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.registries.ObjectHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.temporal.ChronoUnit;
import java.util.*;

@Mod(EyesInTheDarkness.MODID)
public class EyesInTheDarkness
{
    public static final String MODID = "eyesinthedarkness";

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @ObjectHolder(MODID + ":eyes_laugh")
    public static SoundEvent eyes_laugh;

    @ObjectHolder(MODID + ":eyes_disappear")
    public static SoundEvent eyes_disappear;

    @ObjectHolder(MODID + ":eyes_jumpscare")
    public static SoundEvent eyes_jumpscare;

    @ObjectHolder(MODID + ":eyes")
    public static EntityType<EyesEntity> eyes;

    /*The EntityType is static-initialized because of the spawnEgg, which needs a nonnull EntityType by the time it is registered.*/
    /*If Forge moves/patches spawnEggs to use a delegate, remove this hack in favor of the ObjectHolder.*/
    public static final EntityType<EyesEntity> eyes_entity = EntityType.Builder.create(EyesEntity::new, EntityClassification.MONSTER)
            .setTrackingRange(80)
            .setUpdateInterval(3)
            .setCustomClientFactory((ent, world) -> eyes.create(world))
            .setShouldReceiveVelocityUpdates(true)
            .build(MODID + ":eyes");

    private static final String CHANNEL=MODID;
    private static final String PROTOCOL_VERSION = "1.0";

    public static SimpleChannel channel = NetworkRegistry.ChannelBuilder
            .named(location(CHANNEL))
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .simpleChannel();

    public EyesInTheDarkness()
    {
        final ModLoadingContext modLoadingContext = ModLoadingContext.get();
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addGenericListener(SoundEvent.class, this::registerSounds);
        modEventBus.addGenericListener(EntityType.class, this::registerEntities);
        modEventBus.addGenericListener(Item.class, this::registerItems);
        modEventBus.addListener(this::commonSetup);

        modLoadingContext.registerConfig(ModConfig.Type.SERVER, ConfigData.SERVER_SPEC);

        MinecraftForge.EVENT_BUS.addListener(this::entityInit);
    }

    public void registerSounds(RegistryEvent.Register<SoundEvent> event)
    {
        event.getRegistry().registerAll(
                new SoundEvent(location("mob.eyes.laugh")).setRegistryName(location("eyes_laugh")),
                new SoundEvent(location("mob.eyes.disappear")).setRegistryName(location("eyes_disappear")),
                new SoundEvent(location("mob.eyes.jumpscare")).setRegistryName(location("eyes_jumpscare"))
        );
    }

    public void registerEntities(RegistryEvent.Register<EntityType<?>> event)
    {
        event.getRegistry().registerAll(
                eyes_entity.setRegistryName(MODID + ":eyes")
        );
    }

    public void registerItems(RegistryEvent.Register<Item> event)
    {
        event.getRegistry().registerAll(
                new SpawnEggItem(eyes_entity, 0x000000, 0x7F0000, new Item.Properties().group(ItemGroup.MISC)).setRegistryName(location("eyes_spawn_egg"))
        );
    }

    public static int getDaysUntilNextHalloween()
    {
        Calendar now = Calendar.getInstance();
        Calendar nextHalloween = new Calendar.Builder()
                .setDate(now.get(Calendar.YEAR), 9, 31)
                .setTimeOfDay(23,59,59,999).build();
        if (now.after(nextHalloween))
        {
            nextHalloween.add(Calendar.YEAR, 1);
        }
        return (int)Math.min(ChronoUnit.DAYS.between(now.toInstant(), nextHalloween.toInstant()), 30);
    }

    public void commonSetup(FMLCommonSetupEvent event)
    {
        int messageNumber = 0;
        channel.registerMessage(messageNumber++, InitiateJumpscare.class, InitiateJumpscare::encode, InitiateJumpscare::new, InitiateJumpscare::handle);
        LOGGER.debug("Final message number: " + messageNumber);
    }

    @SuppressWarnings("unchecked")
    public void entityInit(EntityJoinWorldEvent event)
    {
        Entity e = event.getEntity();
        if (e instanceof WolfEntity)
        {
            WolfEntity wolf = (WolfEntity)e;
            wolf.targetSelector.addGoal(5,
                    new NearestAttackableTargetGoal<>(wolf, EyesEntity.class, false));
        }
        if (e instanceof OcelotEntity)
        {
            OcelotEntity cat = (OcelotEntity)e;
            cat.goalSelector.addGoal(3, new AvoidEntityGoal<>(cat, EyesEntity.class, 6.0F, 1.0D, 1.2D));
        }
    }

    public static ResourceLocation location(String location)
    {
        return new ResourceLocation(MODID, location);
    }
}
