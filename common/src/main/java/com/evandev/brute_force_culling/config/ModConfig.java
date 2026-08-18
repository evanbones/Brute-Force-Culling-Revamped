package com.evandev.brute_force_culling.config;

import com.evandev.brute_force_culling.Constants;
import com.evandev.brute_force_culling.platform.Services;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = Services.PLATFORM.getConfigDirectory().resolve(Constants.MOD_ID + ".json").toFile();
    private static ModConfig INSTANCE;

    @SerializedName("enabled")
    public boolean enabled = true;

    @SerializedName("cullEntity")
    public boolean cullEntity = true;

    @SerializedName("cullBlockEntity")
    public boolean cullBlockEntity = true;

    @SerializedName("cullChunk")
    public boolean cullChunk = true;

    @SerializedName("sampling")
    public double sampling = 0.5;

    @SerializedName("updateDelay")
    public int updateDelay = 1;

    @SerializedName("asyncSignalHz")
    public int asyncSignalHz = 120;

    @SerializedName("entitySkip")
    public List<String> entitySkip = new ArrayList<>(List.of("create:stationary_contraption"));

    @SerializedName("blockEntitySkip")
    public List<String> blockEntitySkip = new ArrayList<>(List.of("minecraft:beacon"));

    @SerializedName("modSkip")
    public List<String> modSkip = new ArrayList<>(List.of("ars_nouveau"));

    public static ModConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, ModConfig.class);
                if (INSTANCE == null) {
                    INSTANCE = new ModConfig();
                    save();
                }
            } catch (Exception e) {
                Constants.LOG.error("Failed to load " + Constants.MOD_ID + ".json", e);
                INSTANCE = new ModConfig();
                save();
            }
        } else {
            INSTANCE = new ModConfig();
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            Constants.LOG.error("Failed to save " + Constants.MOD_ID + ".json", e);
        }
    }

    public static Screen createScreen(Screen parent) {
        YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.brute_force_culling.title"))
                .save(ModConfig::save);

        ConfigCategory.Builder general = ConfigCategory.createBuilder()
                .name(Component.translatable("config.brute_force_culling.category.general"))
                .option(createBoolOption("enabled", true, () -> get().enabled, val -> get().enabled = val))
                .option(createBoolOption("cullEntity", true, () -> get().cullEntity, val -> get().cullEntity = val))
                .option(createBoolOption("cullBlockEntity", true, () -> get().cullBlockEntity, val -> get().cullBlockEntity = val))
                .option(createBoolOption("cullChunk", true, () -> get().cullChunk, val -> get().cullChunk = val))
                .option(createDoubleSliderOption("sampling", 0.5, 0.0, 1.0, 0.05,
                        () -> get().sampling, val -> get().sampling = val))
                .option(createIntSliderOption("updateDelay", 1, 0, 10, 1,
                        () -> get().updateDelay, val -> get().updateDelay = val));

        ConfigCategory.Builder advanced = ConfigCategory.createBuilder()
                .name(Component.translatable("config.brute_force_culling.category.advanced"))
                .group(createStringListOption("entitySkip", () -> get().entitySkip, val -> get().entitySkip = val))
                .group(createStringListOption("blockEntitySkip", () -> get().blockEntitySkip, val -> get().blockEntitySkip = val))
                .group(createStringListOption("modSkip", () -> get().modSkip, val -> get().modSkip = val));

        return builder.category(general.build()).category(advanced.build()).build().generateScreen(parent);
    }

    private static Option<Boolean> createBoolOption(String name, boolean defaultValue, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(Component.translatable("config.brute_force_culling.option." + name))
                .binding(defaultValue, getter, setter)
                .controller(TickBoxControllerBuilder::create)
                .build();
    }

    private static Option<Double> createDoubleSliderOption(String name, double defaultValue, double min, double max, double step, Supplier<Double> getter, Consumer<Double> setter) {
        return Option.<Double>createBuilder()
                .name(Component.translatable("config.brute_force_culling.option." + name))
                .binding(defaultValue, getter, setter)
                .controller(opt -> DoubleSliderControllerBuilder.create(opt).range(min, max).step(step))
                .build();
    }

    private static Option<Integer> createIntSliderOption(String name, int defaultValue, int min, int max, int step, Supplier<Integer> getter, Consumer<Integer> setter) {
        return Option.<Integer>createBuilder()
                .name(Component.translatable("config.brute_force_culling.option." + name))
                .binding(defaultValue, getter, setter)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(min, max).step(step))
                .build();
    }

    private static ListOption<String> createStringListOption(String name, Supplier<List<String>> getter, Consumer<List<String>> setter) {
        return ListOption.<String>createBuilder()
                .name(Component.translatable("config.brute_force_culling.option." + name))
                .controller(StringControllerBuilder::create)
                .binding(getter.get(), getter, setter)
                .initial("")
                .build();
    }
}
