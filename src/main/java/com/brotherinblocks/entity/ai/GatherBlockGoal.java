package com.brotherinblocks.entity.ai;

import com.brotherinblocks.entity.BroEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * El Bro busca bloques utiles cerca de su dueno (troncos de arbol o piedra),
 * va hasta ellos, los rompe como si los golpeara, y recoge lo que sueltan
 * en su inventario.
 *
 * v0.4.0:
 *  - Solo trabaja si esta cerca del dueno (radio configurable)
 *  - Solo trabaja de dia
 *  - Solo trabaja si no tiene mucha hambre
 *  - Si el dueno se aleja, deja de trabajar y lo sigue
 *  - Recoge SOLO lo que sirve (troncos, madera, piedra, comida)
 */
public class GatherBlockGoal extends Goal {

    /** Cada cuantos ticks se reescanea el terreno (0.5s = 10 ticks) */
    private static final int SCAN_INTERVAL = 40;
    /** Distancia minima de hambre para trabajar */
    private static final int HUNGER_TO_WORK = 6;

    private final BroEntity bro;
    /** Que bloques busca (ej: troncos o piedra) */
    private final Predicate<BlockState> blockFilter;
    /** Radio de busqueda alrededor del dueno */
    private final int searchRadius;
    /** Velocidad a la que camina hacia el bloque */
    private final double speedModifier;
    /** El bloque que esta golpeando ahora mismo */
    private BlockPos target;
    /** Contador para no reescannear cada tick (rendimiento) */
    private int scanCooldown = 0;

    public GatherBlockGoal(BroEntity bro, Predicate<BlockState> blockFilter, int searchRadius, double speedModifier) {
        this.bro = bro;
        this.blockFilter = blockFilter;
        this.searchRadius = searchRadius;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    /** Se activa si hay bloques de interes cerca del dueno, de dia y con hambre */
    @Override
    public boolean canUse() {
        Player owner = getOwner();
        if (owner == null || owner.isSpectator()) {
            return false;
        }
        // Con mucha hambre no trabaja (va a buscar comida)
        if (this.bro.getHunger() < HUNGER_TO_WORK) {
            return false;
        }
        // Solo si el dueno esta cerca
        if (this.bro.distanceTo(owner) > this.searchRadius) {
            return false;
        }
        // Solo de dia (de noche el Bro se queda alerta contigo)
        if (this.bro.level().isNight()) {
            return false;
        }
        // Reescanea el terreno solo cada SCAN_INTERVAL ticks (rendimiento)
        if (this.scanCooldown-- > 0) {
            return this.target != null;
        }
        this.scanCooldown = SCAN_INTERVAL;
        this.target = this.findBlockNear(owner);
        return this.target != null;
    }

    /** Sigue trabajando mientras el bloque siga existiendo y el dueno cerca */
    @Override
    public boolean canContinueToUse() {
        if (this.target == null) {
            return false;
        }
        Player owner = getOwner();
        if (owner == null) {
            return false;
        }
        // Si el dueno se alejo mucho, deja de trabajar
        if (this.bro.distanceTo(owner) > this.searchRadius * 1.5D) {
            return false;
        }
        // Si el bloque ya no existe (lo rompio el), busca otro
        BlockState state = this.bro.level().getBlockState(this.target);
        if (state.isAir() || !this.blockFilter.test(state)) {
            this.target = null;
            return false;
        }
        return true;
    }

    @Override
    public void stop() {
        this.target = null;
        this.bro.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.target == null) {
            return;
        }

        double distance = this.bro.distanceToSqr(
                this.target.getX() + 0.5D, this.target.getY() + 0.5D, this.target.getZ() + 0.5D);

        if (distance > 2.5D) {
            // Camina hacia el bloque
            this.bro.getNavigation().moveTo(
                    this.target.getX() + 0.5D, this.target.getY(), this.target.getZ() + 0.5D,
                    this.speedModifier);
        } else {
            // Esta junto al bloque: lo golpea (lo rompe como un jugador)
            this.bro.getNavigation().stop();
            if (this.bro.tickCount % 10 == 0) {
                this.bro.swing(this.bro.getUsedItemHand());
                BlockState state = this.bro.level().getBlockState(this.target);
                if (!state.isAir()) {
                    // Rompe el bloque con drops reales (como si lo picaras)
                    this.bro.level().destroyBlock(this.target, true, this.bro);
                    this.target = null; // busca el siguiente
                }
            }
        }

        // Recoge los items que caen cerca
        this.collectNearbyDrops();
    }

    /**
     * Junta los items caidos cerca y los mete en el inventario del Bro.
     * SOLO recoge lo que sirve para el mod (troncos, madera, piedra, comida)
     * para no robarle los items al jugador.
     */
    private void collectNearbyDrops() {
        AABB area = this.bro.getBoundingBox().inflate(3.0D);
        List<ItemEntity> items = this.bro.level().getEntitiesOfClass(ItemEntity.class, area);
        for (ItemEntity item : items) {
            ItemStack stack = item.getItem();
            if (isUsefulItem(stack)) {
                ItemStack leftover = this.bro.addToInventory(stack);
                if (leftover.isEmpty()) {
                    item.discard(); // todo entro en la mochila
                } else {
                    item.setItem(leftover); // solo entro una parte
                }
            }
        }
    }

    /** Solo recoge items que sirven para trabajar o comer (no roba lo del jugador) */
    private boolean isUsefulItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        // Comida (para el sistema de hambre)
        if (stack.getItem().isEdible()) {
            return true;
        }
        // Troncos y madera (lo que sueltan los arboles)
        if (stack.is(net.minecraft.tags.ItemTags.LOGS)
                || stack.is(net.minecraft.tags.ItemTags.PLANKS)
                || stack.is(Items.STICK)) {
            return true;
        }
        // Piedra y derivados (lo que suelta al picar)
        return stack.is(Items.COBBLESTONE)
                || stack.is(Items.STONE)
                || stack.is(Items.DEEPSLATE)
                || stack.is(Items.COBBLED_DEEPSLATE)
                || stack.is(Items.ANDESITE)
                || stack.is(Items.DIORITE)
                || stack.is(Items.GRANITE);
    }

    /** Busca el bloque de interes mas cercano al dueno */
    private BlockPos findBlockNear(Player owner) {
        BlockPos ownerPos = owner.blockPosition();
        for (int dy = -3; dy <= 8; dy++) {
            for (int dx = -this.searchRadius; dx <= this.searchRadius; dx++) {
                for (int dz = -this.searchRadius; dz <= this.searchRadius; dz++) {
                    BlockPos pos = ownerPos.offset(dx, dy, dz);
                    BlockState state = this.bro.level().getBlockState(pos);
                    if (!state.isAir() && this.blockFilter.test(state)) {
                        return pos.immutable();
                    }
                }
            }
        }
        return null;
    }

    private Player getOwner() {
        if (this.bro.getOwnerUUID() == null) {
            return null;
        }
        return this.bro.level().getPlayerByUUID(this.bro.getOwnerUUID());
    }
}
