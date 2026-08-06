package com.brotherinblocks.entity;

import com.brotherinblocks.BrotherInBlocks;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registro de las entidades del mod.
 *
 * Aqui se "anuncia" a Forge que existe la entidad Bro:
 *  - ID: "bro" (brotherinblocks:bro)
 *  - Categoria: CREATURE (como los animales, no es hostil)
 *  - Tamano: 0.6 bloques de ancho, 1.8 de alto (como un jugador)
 */
public class ModEntities {

    /** Registro diferido de entidades de Forge */
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BrotherInBlocks.MOD_ID);

    /** La entidad Bro */
    public static final RegistryObject<EntityType<BroEntity>> BRO =
            ENTITY_TYPES.register("bro",
                    () -> EntityType.Builder.<BroEntity>of(BroEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(10)
                            .build("bro"));
}
