package com.brotherinblocks.entity.ai;

import com.brotherinblocks.entity.BroEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
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
 * va hasta ellos, los mina de forma REALISTA (con grietas y tiempo de minado,
 * como un jugador), y recoge lo que sueltan en su inventario.
 *
 * v0.4.1 (mejoras):
 *  - Tala COMPLETA: sigue rompiendo bloques sin pausas muertas (bug arreglado)
 *  - Minería realista: grietas en el bloque + tiempo de minado + sonidos de golpe
 *  - Completa el arbol: sube por el tronco rompiendo tronco a tronco
 *  - No se atora: ignora bloques inalcanzables (troncos muy altos) y busca otro
 *  - Tiempo segun material: madera mas rapida, piedra mas lenta
 *  - Solo trabaja de dia, cerca del dueno, y con hambre suficiente
 */
public class GatherBlockGoal extends Goal {

    /** Cada cuantos ticks se reescanea el terreno (0.5s = 10 ticks) */
    private static final int SCAN_INTERVAL = 40;
    /** Distancia minima de hambre para trabajar */
    private static final int HUNGER_TO_WORK = 6;
    /** Ticks para romper madera (30 ticks = 1.5s) */
    private static final int WOOD_MINE_TIME = 30;
    /** Ticks para romper piedra (50 ticks = 2.5s) */
    private static final int STONE_MINE_TIME = 50;
    /** Altura maxima que puede alcanzar (para no atorarse con troncos altos) */
    private static final int MAX_REACH_HEIGHT = 3;

    private final BroEntity bro;
    /** Que bloques busca (ej: troncos o piedra) */
    private final Predicate<BlockState> blockFilter;
    /** Radio de busqueda alrededor del dueno */
    private final int searchRadius;
    /** Velocidad a la que camina hacia el bloque */
    private final double speedModifier;
    /** true = este goal es para madera (respeta la orden del jugador) */
    private final boolean isWoodGoal;
    /** El bloque que esta golpeando ahora mismo */
    private BlockPos target;
    /** Contador para no reescannear cada tick (rendimiento) */
    private int scanCooldown = 0;
    /** Progreso de minado del bloque actual (ticks golpeando) */
    private int miningProgress = 0;
    /** Ticks que faltan para romper el bloque actual */
    private int miningTicksNeeded = 0;

    public GatherBlockGoal(BroEntity bro, Predicate<BlockState> blockFilter, int searchRadius, double speedModifier, boolean isWoodGoal) {
        this.bro = bro;
        this.blockFilter = blockFilter;
        this.searchRadius = searchRadius;
        this.speedModifier = speedModifier;
        this.isWoodGoal = isWoodGoal;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    /** Se activa si hay bloques de interes cerca del dueno, de dia y con hambre */
    @Override
    public boolean canUse() {
        Player owner = getOwner();
        if (owner == null || owner.isSpectator()) {
            return false;
        }
        // El goal de madera SOLO trabaja si hay una orden del jugador activa
        if (this.isWoodGoal && !this.bro.hasWoodOrder()) {
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
            return false;
        }
        this.scanCooldown = SCAN_INTERVAL;
        this.target = this.findBlockNear(owner);
        return this.target != null;
    }

    /**
     * Sigue trabajando SIN pausas muertas: al romper un bloque, busca
     * el siguiente al instante (asi tala arboles completos, no solo uno).
     */
    @Override
    public boolean canContinueToUse() {
        // Si la orden de madera ya se cumplio (o el jugador la cancelo), para
        if (this.isWoodGoal && !this.bro.hasWoodOrder()) {
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
        // Si ya no hay target (rompio el bloque anterior) o el bloque desaparecio,
        // busca el siguiente inmediatamente (sin esperar el cooldown de canUse)
        boolean needNewTarget = this.target == null;
        if (!needNewTarget) {
            BlockState state = this.bro.level().getBlockState(this.target);
            needNewTarget = state.isAir() || !this.blockFilter.test(state);
        }
        if (needNewTarget) {
            this.target = this.findBlockNear(owner);
            this.miningProgress = 0;
            this.miningTicksNeeded = 0;
            if (this.target == null) {
                return false; // no hay mas bloques, se detiene
            }
        }
        return true;
    }

    @Override
    public void stop() {
        // Limpia las grietas del bloque si se interrumpio el minado
        if (this.target != null) {
            this.bro.level().destroyBlockProgress(this.bro.getId(), this.target, -1);
        }
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

        if (distance > 3.0D) {
            // Camina hacia el bloque
            this.bro.getNavigation().moveTo(
                    this.target.getX() + 0.5D, this.target.getY(), this.target.getZ() + 0.5D,
                    this.speedModifier);
        } else {
            // Esta junto al bloque: lo mina como un jugador (no en creativo)
            this.bro.getNavigation().stop();
            this.mineBlock();
        }

        // Recoge los items que caen cerca
        this.collectNearbyDrops();
    }

    /** Mina el bloque con progreso, grietas y sonidos (como un jugador real) */
    private void mineBlock() {
        BlockState state = this.bro.level().getBlockState(this.target);
        if (state.isAir()) {
            return;
        }

        // Primera vez que tocamos este bloque: calcula cuanto tarda
        if (this.miningTicksNeeded == 0) {
            this.miningTicksNeeded = this.getMineTime(state);
            this.miningProgress = 0;
        }

        // Golpea (animacion de brazo) cada 5 ticks
        if (this.miningProgress % 5 == 0) {
            this.bro.swing(InteractionHand.MAIN_HAND);
            // Sonido de golpear el bloque
            this.bro.level().playSound(null, this.target,
                    state.getSoundType().getHitSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
        }

        // Muestra las grietas en el bloque (0 a 9, como cuando tu minas)
        int crackStage = (int) ((float) this.miningProgress / this.miningTicksNeeded * 9.0F);
        this.bro.level().destroyBlockProgress(this.bro.getId(), this.target,
                Math.min(9, crackStage));

        this.miningProgress++;

        // Ya termino de minar: rompe el bloque
        if (this.miningProgress >= this.miningTicksNeeded) {
            // Limpia las grietas
            this.bro.level().destroyBlockProgress(this.bro.getId(), this.target, -1);
            // Sonido de romper
            this.bro.level().playSound(null, this.target,
                    state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F, 0.9F);
            // Rompe el bloque con drops reales
            this.bro.level().destroyBlock(this.target, true, this.bro);
            this.miningProgress = 0;
            this.miningTicksNeeded = 0;
            this.target = null; // canContinueToUse buscara el siguiente
        }
    }

    /** Cuanto tarda en romper el bloque segun el material */
    private int getMineTime(BlockState state) {
        if (state.is(net.minecraft.tags.BlockTags.LOGS)) {
            return WOOD_MINE_TIME;
        }
        return STONE_MINE_TIME;
    }

    /** Junta los items caidos cerca y los mete en el inventario del Bro */
    private void collectNearbyDrops() {
        AABB area = this.bro.getBoundingBox().inflate(3.0D);
        List<ItemEntity> items = this.bro.level().getEntitiesOfClass(ItemEntity.class, area);
        for (ItemEntity item : items) {
            ItemStack stack = item.getItem();
            if (isUsefulItem(stack)) {
                int beforeCount = stack.getCount();
                ItemStack leftover = this.bro.addToInventory(stack);
                int collected = beforeCount - leftover.getCount();

                // Si es madera y hay una orden activa, la cuenta para la orden
                if (collected > 0 && this.isWoodGoal && this.bro.hasWoodOrder()) {
                    this.bro.addWoodCollected(collected);
                }

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

    /**
     * Busca el bloque de interes mas cercano al dueno.
     * Ignora bloques inalcanzables (muy altos) para no atorarse.
     */
    private BlockPos findBlockNear(Player owner) {
        BlockPos ownerPos = owner.blockPosition();
        for (int dy = -3; dy <= MAX_REACH_HEIGHT; dy++) {
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
