package glowredman.gt2mm;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.Fluid;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import glowredman.modularmaterials.data.object.MM_Material;
import glowredman.modularmaterials.data.object.MM_OreVein;
import glowredman.modularmaterials.data.object.sub.BlockProperties;
import glowredman.modularmaterials.data.object.sub.ChemicalState;
import glowredman.modularmaterials.data.object.sub.FluidProperties;
import glowredman.modularmaterials.data.object.sub.OreProperties;
import glowredman.modularmaterials.data.object.sub.PulseMode;
import gregtech.GT_Mod;
import gregtech.api.GregTech_API;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.common.GT_Client;
import gregtech.common.GT_Proxy;
import gregtech.common.GT_Worldgen_GT_Ore_Layer;
import gregtech.common.blocks.GT_Block_Metal;

@Mod(
    acceptableRemoteVersions = "*",
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "required-after:gregtech",
    modid = "gt2mm",
    name = "GT2MM",
    version = Tags.VERSION)
public class GT2MM {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .create();
    private static List<Materials> posR, posG, posB, posA, negR, negG, negB, negA, posMR, posMG, posMB, posMA, negMR,
        negMG, negMB, negMA;
    private static final Logger LOGGER = LogManager.getLogger("GT2MM");
    private static final Map<OrePrefixes, String> KNOWN_TYPES = new LinkedHashMap<>();
    private static final String[] TOOLS = { "minecraft:needs_stone_tool", "minecraft:needs_iron_tool",
        "minecraft:needs_diamond_tool", "forge:needs_netherite_tool" };

    @EventHandler
    public static void postInit(final FMLServerAboutToStartEvent event) {
        initPulseLists();
        handleMaterials();
        handleOreVeins();
    }

    private static void initPulseLists() {
        try {
            GT_Client gtClient = new GT_Client();
            posR = ReflectionHelper.getPrivateValue(GT_Client.class, gtClient, "mPosR");
            posG = ReflectionHelper.getPrivateValue(GT_Client.class, gtClient, "mPosG");
            posB = ReflectionHelper.getPrivateValue(GT_Client.class, gtClient, "mPosB");
            posA = ReflectionHelper.getPrivateValue(GT_Client.class, gtClient, "mPosA");
            negR = ReflectionHelper.getPrivateValue(GT_Client.class, gtClient, "mNegR");
            negG = ReflectionHelper.getPrivateValue(GT_Client.class, gtClient, "mNegG");
            negB = ReflectionHelper.getPrivateValue(GT_Client.class, gtClient, "mNegB");
            negA = ReflectionHelper.getPrivateValue(GT_Client.class, gtClient, "mNegA");
            posMR = ReflectionHelper.getPrivateValue(GT_Client.class, gtClient, "mMoltenPosR");
            posMG = ReflectionHelper.getPrivateValue(GT_Client.class, gtClient, "mMoltenPosG");
            posMB = ReflectionHelper.getPrivateValue(GT_Client.class, gtClient, "mMoltenPosB");
            posMA = ReflectionHelper.getPrivateValue(GT_Client.class, gtClient, "mMoltenPosA");
            negMR = ReflectionHelper.getPrivateValue(GT_Client.class, gtClient, "mMoltenNegR");
            negMG = ReflectionHelper.getPrivateValue(GT_Client.class, gtClient, "mMoltenNegG");
            negMB = ReflectionHelper.getPrivateValue(GT_Client.class, gtClient, "mMoltenNegB");
            negMA = ReflectionHelper.getPrivateValue(GT_Client.class, gtClient, "mMoltenNegA");
        } catch (Exception e) {
            LOGGER.error("Failed to get color animation lists", e);
            posR = Collections.emptyList();
            posG = Collections.emptyList();
            posB = Collections.emptyList();
            posA = Collections.emptyList();
            negR = Collections.emptyList();
            negG = Collections.emptyList();
            negB = Collections.emptyList();
            negA = Collections.emptyList();
            posMR = Collections.emptyList();
            posMG = Collections.emptyList();
            posMB = Collections.emptyList();
            posMA = Collections.emptyList();
            negMR = Collections.emptyList();
            negMG = Collections.emptyList();
            negMB = Collections.emptyList();
            negMA = Collections.emptyList();
        }
    }

    private static void handleMaterials() {
        final long time = System.currentTimeMillis();
        LOGGER.info("Parsing materials...");

        final List<Materials> gtMaterials = Arrays.stream(GregTech_API.sGeneratedMaterials)
            .collect(Collectors.toList());

        final Map<String, MM_Material> materials = gtMaterials.stream()
            .filter(t -> t != null)
            .sorted((o1, o2) -> o1.mMetaItemSubID - o2.mMetaItemSubID)
            .map(GT2MM::convert)
            .collect(Collectors.toMap(Pair::getLeft, Pair::getRight, (t, u) -> {
                throw new IllegalStateException(
                    String.format("Duplicate key (attempted merging values %s and %s)", t.name, u.name));
            }, LinkedHashMap::new));
        addBlockInfo(materials);

        save(materials, "materials.json");

        LOGGER.info("Done! Created {} materials. Took {} ms.", materials.size(), System.currentTimeMillis() - time);
    }

    private static void handleOreVeins() {
        final long time = System.currentTimeMillis();
        LOGGER.info("Parsing ore veins...");

        final Map<String, MM_OreVein> oreveins = GT_Worldgen_GT_Ore_Layer.sList.stream()
            .map(GT2MM::convert)
            .collect(Collectors.toMap(Pair::getLeft, Pair::getRight, (t, u) -> {
                throw new IllegalStateException(
                    String.format("Duplicate key (attempted merging values %s and %s)", t.name, u.name));
            }, LinkedHashMap::new));

        save(oreveins, "oreveins.json");

        LOGGER.info("Done! Created {} ore veins. Took {} ms.", oreveins.size(), System.currentTimeMillis() - time);
    }

    private static Pair<String, MM_Material> convert(@Nonnull final Materials mat) {
        final String name = mat.name();

        LOGGER.info("Converting {}...", name);

        final MM_Material new_mat = new MM_Material();

        new_mat.color.red = mat.mRGBa[0];
        new_mat.color.green = mat.mRGBa[1];
        new_mat.color.blue = mat.mRGBa[2];
        new_mat.color.alpha = mat.mRGBa[3];
        setPulseMode(new_mat, mat);
        new_mat.enabled = mat.mMetaItemSubID > 0;
        new_mat.enabledTypes = getTypes(mat);
        new_mat.fluid = getFluidProps(mat);
        new_mat.burnTime = mat.mFuelPower;
        new_mat.name = mat.mDefaultLocalName;
        new_mat.ore = getOreProps(mat);
        new_mat.state = getState(mat);
        new_mat.tagNames = Arrays.asList(name.toLowerCase(Locale.ROOT));
        new_mat.texture = mat.mIconSet.mSetName.toLowerCase(Locale.ROOT);
        new_mat.tooltip.text = getTooltip(mat);

        return Pair.of(name.toLowerCase(Locale.ROOT), new_mat);
    }

    private static Pair<String, MM_OreVein> convert(@Nonnull final GT_Worldgen_GT_Ore_Layer oreLayer) {
        LOGGER.info("Converting {}...", oreLayer.mWorldGenName);

        final MM_OreVein new_orevein = new MM_OreVein();

        new_orevein.density = oreLayer.mDensity;
        new_orevein.dimensions = getDimensions(oreLayer);
        new_orevein.enabled = oreLayer.mEnabled;
        new_orevein.inbetween = GregTech_API.sGeneratedMaterials[oreLayer.mBetweenMeta].name()
            .toLowerCase(Locale.ROOT);
        new_orevein.maxY = oreLayer.mMaxY;
        new_orevein.minY = oreLayer.mMinY;
        new_orevein.name = StatCollector.translateToLocal(oreLayer.mWorldGenName);
        new_orevein.primary = GregTech_API.sGeneratedMaterials[oreLayer.mPrimaryMeta].name()
            .toLowerCase(Locale.ROOT);
        new_orevein.secondary = GregTech_API.sGeneratedMaterials[oreLayer.mSecondaryMeta].name()
            .toLowerCase(Locale.ROOT);
        new_orevein.size = oreLayer.mSize;
        new_orevein.sporadic = GregTech_API.sGeneratedMaterials[oreLayer.mSporadicMeta].name()
            .toLowerCase(Locale.ROOT);
        new_orevein.weight = oreLayer.mWeight;

        return Pair.of(oreLayer.mWorldGenName, new_orevein);
    }

    private static void save(@Nullable final Map<String, ?> content, @Nonnull final String filename) {
        final Path dir = Launch.minecraftHome.toPath()
            .resolve("gt2mm");
        final Path file = dir.resolve(filename);
        try {
            Files.createDirectories(dir);
            Files.deleteIfExists(file);
            Files.write(
                file,
                GSON.toJson(content)
                    .getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            LOGGER.error("Could not create " + filename + " file", e);
        }
    }

    private static void setPulseMode(@Nonnull final MM_Material new_mat, @Nullable final Materials mat) {
        if (posR.contains(mat)) new_mat.color.pulseModeRed = PulseMode.POS;
        else if (negR.contains(mat)) new_mat.color.pulseModeRed = PulseMode.NEG;

        if (posG.contains(mat)) new_mat.color.pulseModeGreen = PulseMode.POS;
        else if (negG.contains(mat)) new_mat.color.pulseModeGreen = PulseMode.NEG;

        if (posB.contains(mat)) new_mat.color.pulseModeBlue = PulseMode.POS;
        else if (negB.contains(mat)) new_mat.color.pulseModeBlue = PulseMode.NEG;

        if (posA.contains(mat)) new_mat.color.pulseModeAlpha = PulseMode.POS;
        else if (negA.contains(mat)) new_mat.color.pulseModeAlpha = PulseMode.NEG;

        if (posMR.contains(mat)) new_mat.color.pulseModeMoltenRed = PulseMode.POS;
        else if (negMR.contains(mat)) new_mat.color.pulseModeMoltenRed = PulseMode.NEG;

        if (posMG.contains(mat)) new_mat.color.pulseModeMoltenGreen = PulseMode.POS;
        else if (negMG.contains(mat)) new_mat.color.pulseModeMoltenGreen = PulseMode.NEG;

        if (posMB.contains(mat)) new_mat.color.pulseModeMoltenBlue = PulseMode.POS;
        else if (negMB.contains(mat)) new_mat.color.pulseModeMoltenBlue = PulseMode.NEG;

        if (posMA.contains(mat)) new_mat.color.pulseModeMoltenAlpha = PulseMode.POS;
        else if (negMA.contains(mat)) new_mat.color.pulseModeMoltenAlpha = PulseMode.NEG;
    }

    private static void addBlockInfo(@Nonnull final Map<String, MM_Material> materials) {
        if (GregTech_API.VERSION < 509) return;

        try {
            // look for all public fields of type GT_Block_Metal
            for (Field field_block : GregTech_API.class.getFields()) {
                if (GT_Block_Metal.class.isAssignableFrom(field_block.getDeclaringClass())) {

                    // iterate over all Materials related to this block, add their properties to the materials map
                    final Materials[] mMats = ((GT_Block_Metal) field_block.get(null)).mMats;
                    for (int i = 0; i < mMats.length; i++) {
                        final Materials mat = mMats[i];
                        final BlockProperties blockProps = new BlockProperties();

                        blockProps.requiresToolForDrops = true;
                        blockProps.tags = Arrays.asList(
                            "[block]minecraft:beacon_base_blocks",
                            "minecraft:needs_stone_tool",
                            "minecraft:mineable/pickaxe");

                        final MM_Material new_mat = materials.get(
                            mat.name()
                                .toLowerCase(Locale.ROOT));
                        new_mat.block = blockProps;
                        new_mat.enabledTypes.add("block");
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("addBlockInfo failed", e);
        }
    }

    private static List<String> getTypes(@Nonnull final Materials mat) {
        List<String> types = KNOWN_TYPES.entrySet()
            .stream()
            .filter(
                e -> e.getKey()
                    .doGenerateItem(mat))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
        if (mat.mFluid != null) {
            types.add("liquid");
        }
        if (mat.mGas != null) {
            types.add("gas");
        }
        if (mat.mPlasma != null) {
            types.add("plasma");
        }
        return types;
    }

    private static FluidProperties getFluidProps(@Nonnull final Materials mat) {
        final FluidProperties props = new FluidProperties();

        final ChemicalState state = getState(mat);
        final Fluid liquid = state == ChemicalState.SOLID ? mat.mStandardMoltenFluid : mat.mFluid;

        props.currentTemperature = state == ChemicalState.SOLID || mat.mFluid == null ? 295
            : mat.mFluid.getTemperature();

        if (mat.mGas != null) {
            props.boilingTemperature = mat.mGas.getTemperature();
            props.gas.density = mat.mGas.getDensity();
            props.gas.luminosity = mat.mGas.getLuminosity();
            props.gas.viscosity = mat.mGas.getViscosity();
        }

        if (liquid != null) {
            props.liquid.density = liquid.getDensity();
            props.liquid.luminosity = liquid.getLuminosity();
            props.liquid.viscosity = liquid.getViscosity();
            props.meltingTemperature = liquid.getTemperature();
        }

        return props;
    }

    private static OreProperties getOreProps(@Nonnull final Materials mat) {
        final OreProperties props = new OreProperties();
        int harvestLevel = 0;

        if (GregTech_API.VERSION > 507) {
            try {
                if (GT_Proxy.class.getField("mChangeHarvestLevels")
                    .getBoolean(GT_Mod.gregtechproxy) && mat.mMetaItemSubID >= 0) {
                    harvestLevel = ((int[]) GT_Proxy.class.getField("mHarvestLevel")
                        .get(GT_Mod.gregtechproxy))[mat.mMetaItemSubID];
                } else {
                    harvestLevel = mat.mToolQuality;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to get harvest level", e);
            }
        } else {
            harvestLevel = mat.mToolQuality;
        }

        props.hardness = 1.0f + harvestLevel;
        props.requiresToolForDrops = true;
        if (harvestLevel > 0) {
            // TOOLS starts with stone, so harvestLevel must be decreased by 1
            props.tags = Arrays.asList(TOOLS[Math.min(harvestLevel, TOOLS.length) - 1]);
        }

        return props;
    }

    private static ChemicalState getState(@Nonnull final Materials mat) {
        if (mat.mStandardMoltenFluid != null) {
            return ChemicalState.SOLID;
        }
        if (mat.mFluid != null) {
            return ChemicalState.LIQUID;
        }
        return ChemicalState.GASEOUS;
    }

    private static String[] getTooltip(@Nonnull final Materials mat) {
        final String s = mat.getToolTip(false);
        if (s.isEmpty()) {
            return new String[0];
        }
        return new String[] { s.replace('0', '\u2080')
            .replace('1', '\u2081')
            .replace('2', '\u2082')
            .replace('3', '\u2083')
            .replace('4', '\u2084')
            .replace('5', '\u2085')
            .replace('6', '\u2086')
            .replace('7', '\u2087')
            .replace('8', '\u2088')
            .replace('9', '\u2089') };
    }

    private static List<String> getDimensions(@Nonnull final GT_Worldgen_GT_Ore_Layer oreLayer) {
        final List<String> dims = new ArrayList<>();

        if (oreLayer.mOverworld) {
            dims.add("minecraft:overworld");
        }
        if (oreLayer.mNether) {
            dims.add("minecraft:the_nether");
        }
        if (oreLayer.mEnd) {
            dims.add("minecraft:the_end");
        }

        return dims;
    }

    static {
        KNOWN_TYPES.put(OrePrefixes.ore, "ore");
        KNOWN_TYPES.put(OrePrefixes.dustTiny, "dust_tiny");
        KNOWN_TYPES.put(OrePrefixes.dustSmall, "dust_small");
        KNOWN_TYPES.put(OrePrefixes.dust, "dust");
        KNOWN_TYPES.put(OrePrefixes.dustImpure, "dust_impure");
        KNOWN_TYPES.put(OrePrefixes.dustPure, "dust_pure");
        KNOWN_TYPES.put(OrePrefixes.crushed, "crushed");
        KNOWN_TYPES.put(OrePrefixes.crushedPurified, "crushed_purified");
        KNOWN_TYPES.put(OrePrefixes.crushedCentrifuged, "crushed_centrifuged");
        KNOWN_TYPES.put(OrePrefixes.gem, "gem");
        KNOWN_TYPES.put(OrePrefixes.nugget, "nugget");
        KNOWN_TYPES.put(OrePrefixes.ingot, "ingot");
        KNOWN_TYPES.put(OrePrefixes.ingotHot, "ingot_hot");
        KNOWN_TYPES.put(OrePrefixes.ingotDouble, "ingot_double");
        KNOWN_TYPES.put(OrePrefixes.ingotTriple, "ingot_triple");
        KNOWN_TYPES.put(OrePrefixes.ingotQuadruple, "ingot_quadruple");
        KNOWN_TYPES.put(OrePrefixes.ingotQuintuple, "ingot_quintuple");
        KNOWN_TYPES.put(OrePrefixes.plate, "plate");
        KNOWN_TYPES.put(OrePrefixes.plateDouble, "plate_double");
        KNOWN_TYPES.put(OrePrefixes.plateTriple, "plate_triple");
        KNOWN_TYPES.put(OrePrefixes.plateQuadruple, "plate_quadruple");
        KNOWN_TYPES.put(OrePrefixes.plateQuintuple, "plate_quintuple");
        KNOWN_TYPES.put(OrePrefixes.plateDense, "plate_dense");
        KNOWN_TYPES.put(OrePrefixes.stick, "rod");
        KNOWN_TYPES.put(OrePrefixes.lens, "lens");
        KNOWN_TYPES.put(OrePrefixes.round, "round");
        KNOWN_TYPES.put(OrePrefixes.bolt, "bolt");
        KNOWN_TYPES.put(OrePrefixes.screw, "screw");
        KNOWN_TYPES.put(OrePrefixes.ring, "ring");
        KNOWN_TYPES.put(OrePrefixes.foil, "foil");
        KNOWN_TYPES.put(OrePrefixes.wireFine, "wire");
        KNOWN_TYPES.put(OrePrefixes.gearGtSmall, "gear_small");
        KNOWN_TYPES.put(OrePrefixes.rotor, "rotor");
        KNOWN_TYPES.put(OrePrefixes.stickLong, "rod_long");
        KNOWN_TYPES.put(OrePrefixes.springSmall, "spring_small");
        KNOWN_TYPES.put(OrePrefixes.spring, "spring");
        KNOWN_TYPES.put(OrePrefixes.gemChipped, "gem_chipped");
        KNOWN_TYPES.put(OrePrefixes.gemFlawed, "gem_flawed");
        KNOWN_TYPES.put(OrePrefixes.gemFlawless, "gem_flawless");
        KNOWN_TYPES.put(OrePrefixes.gemExquisite, "gem_exquisite");
        KNOWN_TYPES.put(OrePrefixes.gearGt, "gear");
    }

}
