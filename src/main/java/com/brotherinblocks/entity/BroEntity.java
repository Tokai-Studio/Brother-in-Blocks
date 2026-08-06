package com.brotherinblocks.entity;

import com.brotherinblocks.entity.ai.FollowBroGoal;
import com.brotherinblocks.entity.ai.GatherBlockGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

/**
 * El Bro: tu companero que aparece en el mundo contigo.
 *
 * v0.2.0: aparece junto al jugador, no muere tonto, no se pierde.
 * v0.3.0: te sigue a distancia prudente.
 * v0.4.0: tiene inventario propio (27 slots), tala madera y pica piedra
 *         cerca de ti, recoge el botin, y tiene hambre (come solo si
 *         tiene comida en su inventario).
 */
public class BroEntity extends PathfinderMob {

    /** Distancia maxima antes de teletransportarse hacia su dueno */
    private static final double MAX_FOLLOW_DISTANCE = 64.0D;
    /** Tamano del inventario (27 = un cofre) */
    private static final int INVENTORY_SIZE = 27;
    /** Cada cuanto baja el hambre (ticks) - 600 ticks = 30 segundos */
    private static final int HUNGER_INTERVAL = 600;
    /** Con hambre menor a esta, deja de trabajar */
    private static final int HUNGER_TO_WORK = 6;

    /** UUID del jugador dueno (el que lo "recluto") */
    private UUID ownerUUID;

    /** Inventario del Bro (se guarda con el mundo) */
    private final net.minecraft.world.SimpleContainer inventory =
            new net.minecraft.world.SimpleContainer(INVENTORY_SIZE);

    /** Nivel de hambre 0-20 (como el jugador) */
    private int hunger = 20;
    /** Contador para bajar el hambre cada cierto tiempo */
    private int hungerTimer = 0;

    public BroEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        // Nunca desaparece solo del mundo
        this.setPersistenceRequired();
        // Su nombre visible sobre la cabeza
        this.setCustomName(Component.literal("Bro"));
        this.setCustomNameVisible(true);
    }

    /** Atributos de la entidad: vida, velocidad, dano (se registran en EntityAttributeCreationEvent) */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                // 20 de vida = 10 corazones, igual que el jugador
                .add(Attributes.MAX_HEALTH, 20.0D)
                // Velocidad un poco menor que la del jugador corriendo, para
                // que camine a su ritmo y no se le pegue
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }

    /**
     * Los comportamientos (goals) del Bro.
     */
    @Override
    protected void registerGoals() {
        // Sigue al dueno: se queda a ~3 bloques (cerquita), y lo alcanza
        // si se aleja mas de 6 bloques (nada de timidez)
        this.goalSelector.addGoal(1, new FollowBroGoal(this, 1.0D, 3.0D, 6.0D));
        // Trabaja: tala arboles cerca del dueno
        this.goalSelector.addGoal(2, new GatherBlockGoal(
                this,
                (state) -> state.is(net.minecraft.tags.BlockTags.LOGS),
                10, 1.0D));
        // Trabaja: pica piedra si no hay arboles
        this.goalSelector.addGoal(3, new GatherBlockGoal(
                this,
                (state) -> state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE)
                        || state.is(Blocks.DEEPSLATE) || state.is(Blocks.ANDESITE)
                        || state.is(Blocks.DIORITE) || state.is(Blocks.GRANITE),
                10, 1.0D));
        // Te mira cuando esta parado
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    /** Le decimos quien es su dueno al generarlo */
    public void setOwner(Player player) {
        this.ownerUUID = player.getUUID();
    }

    /** Devuelve el UUID del jugador dueno (para evitar duplicados) */
    public UUID getOwnerUUID() {
        return this.ownerUUID;
    }

    /** Devuelve el nivel de hambre actual (0-20) */
    public int getHunger() {
        return this.hunger;
    }

    /**
     * Mete un item en el inventario del Bro.
     * Devuelve lo que NO cupo (vacio si entro todo).
     */
    public ItemStack addToInventory(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack leftover = this.inventory.addItem(stack);
        return leftover;
    }

    /** El jugador puede abrir el inventario del Bro con clic derecho */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.level().isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                    (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x3, id, inv, this.inventory, 3),
                    Component.literal("Inventario de Bro")));
            return InteractionResult.sidedSuccess(true);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // Todo el estado que toca el mundo se maneja SOLO en el servidor
        if (!this.level().isClientSide) {
            // No se ahoga: si esta en agua, mantiene el aire lleno
            if (this.isInWater()) {
                this.setAirSupply(this.getMaxAirSupply());
            }

            // Sistema de hambre: baja con el tiempo
            if (++this.hungerTimer >= HUNGER_INTERVAL) {
                this.hungerTimer = 0;
                if (this.hunger > 0) {
                    this.hunger--;
                }
                // Si tiene hambre y hay comida en su inventario, come solo
                if (this.hunger <= 12) {
                    this.tryEatFood();
                }
            }

            // No se pierde: si el dueno esta muy lejos (y en la MISMA dimension),
            // aparece cerca de el. Si esta en otra dimension, no se mueve.
            if (this.ownerUUID != null) {
                Player owner = this.level().getPlayerByUUID(this.ownerUUID);
                if (owner != null
                        && owner.level().dimension() == this.level().dimension()
                        && this.distanceTo(owner) > MAX_FOLLOW_DISTANCE) {
                    // randomTeleport busca un lugar seguro (no dentro de bloques)
                    this.randomTeleport(owner.getX(), owner.getY(), owner.getZ(), true);
                }
            }
        }
    }

    /** Busca comida en su inventario y se la come para recuperar hambre */
    private void tryEatFood() {
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack stack = this.inventory.getItem(i);
            if (!stack.isEmpty() && stack.getItem().isEdible()) {
                // Se come 1 de la comida
                stack.shrink(1);
                // Recupera 5 de hambre (como comer pan)
                this.hunger = Math.min(20, this.hunger + 5);
                // Sonido de comer
                this.playSound(this.getEatingSound(stack), 0.6F, this.random.nextFloat() * 0.1F + 0.9F);
                return;
            }
        }
    }

    /** No recibe dano de caidas (para que no muera tontamente por un barranco) */
    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        return false;
    }

    /** Guarda quien es su dueno, su inventario y su hambre (se guardan con el mundo) */
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerUUID != null) {
            tag.putUUID("BroOwner", this.ownerUUID);
        }
        // Guarda el inventario slot por slot (compatible con cualquier version)
        ListTag list = new ListTag();
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack stack = this.inventory.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag slot = new CompoundTag();
                slot.putByte("Slot", (byte) i);
                stack.save(slot);
                list.add(slot);
            }
        }
        tag.put("BroInventory", list);
        tag.putInt("BroHunger", this.hunger);
    }

    /** Recupera quien es su dueno, su inventario y su hambre al cargar el mundo */
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("BroOwner")) {
            this.ownerUUID = tag.getUUID("BroOwner");
        }
        if (tag.contains("BroInventory", 9)) {
            ListTag list = tag.getList("BroInventory", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag slot = list.getCompound(i);
                int slotIndex = slot.getByte("Slot") & 255;
                if (slotIndex >= 0 && slotIndex < this.inventory.getContainerSize()) {
                    this.inventory.setItem(slotIndex, ItemStack.of(slot));
                }
            }
        }
        this.hunger = tag.getInt("BroHunger");
    }
}
