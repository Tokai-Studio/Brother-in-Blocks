package com.brotherinblocks.entity;

import com.brotherinblocks.chains.FollowChain;
import com.brotherinblocks.chat.ChatManager;
import com.brotherinblocks.tasksystem.TaskRunner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.block.state.BlockState;
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
 * v0.5.0: te defiende de los monstruos (prioriza a tu atacante, se retira
 *         con poca vida).
 * v1.0.0: te habla por el chat (saludo, reacciones, avisos) con anti-spam.
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

    // ---- Sistema de peticiones al jugador (v0.4.2) ----
    /** Estado: esperando respuesta del jugador en el chat */
    private boolean waitingForAnswer = false;
    /** Ticks de juego cuando puede volver a preguntar (tras un 'no') */
    private long askAgainAt = 0;
    /** Ticks de juego cuando caduca la espera de respuesta */
    private long answerDeadline = 0;
    /** Cuanta madera pidio el jugador (0 = sin orden) */
    private int requestedWood = 0;
    /** Madera recogida de la orden actual */
    private int woodCollected = 0;
    /** Contador para preguntar cada cierto tiempo */
    private int askTimer = 0;
    /** Ticks en los que empezo la orden actual (para cancelarla si se atasca) */
    private long orderStartedAt = 0;

    // ---- Sistema de chat (v1.0.0) ----
    /** El "cerebro" de las frases del Bro (anti-spam, sin repetir) */
    private final ChatManager chatManager = new ChatManager(this);
    /** Ya saludo al jugador? (una vez por mundo) */
    private boolean greeted = false;
    /** Como estaba el cielo la ultima vez (para avisar al anochecer/amanecer) */
    private boolean wasNight = false;
    /** Ticks de juego del ultimo aviso de creeper */
    private long lastCreeperWarnAt = 0;
    /** Contador para no escanear creepers cada tick (rendimiento) */
    private int creeperScanTimer = 0;

    // ---- Sistema de tareas (v2.0.2) ----
    /** El cerebro del Bro: decide cada tick que deseo ejecutar */
    private final TaskRunner taskRunner;

    public BroEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        // Nunca desaparece solo del mundo
        this.setPersistenceRequired();
        // Su nombre visible sobre la cabeza
        this.setCustomName(Component.literal("Bro"));
        this.setCustomNameVisible(true);

        // Crea el cerebro (v2.0.2): el TaskRunner con sus cadenas.
        // Cada cadena se registra sola al construirse.
        this.taskRunner = new TaskRunner(this);
        this.taskRunner.enable();
        new FollowChain(this.taskRunner);
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
     * Comportamientos del Bro (v2.0+):
     * TODO el comportamiento lo decide el TaskRunner (sistema de tareas).
     * El goalSelector de vanilla queda VACIO a proposito: los goals viejos
     * del MVP (defensa, madera, piedra) peleaban con el sistema nuevo por
     * el control del movimiento. Cada uno se migra a una cadena del
     * TaskRunner en su propia version.
     */
    @Override
    protected void registerGoals() {
        // Intencionalmente vacio: el cerebro es el TaskRunner.
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

    /** Devuelve el sistema de chat del Bro (para que otros lo hagan hablar) */
    public ChatManager getChatManager() {
        return this.chatManager;
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

    // ================= SISTEMA DE PETICIONES AL JUGADOR =================

    /** Devuelve true si el Bro esta esperando que el jugador responda en el chat */
    public boolean isWaitingForAnswer() {
        return this.waitingForAnswer;
    }

    /** Devuelve true si hay una orden de madera activa */
    public boolean hasWoodOrder() {
        return this.requestedWood > 0;
    }

    /** Cuanta madera se pidio */
    public int getRequestedWood() {
        return this.requestedWood;
    }

    /** Cuanta madera lleva recogida */
    public int getWoodCollected() {
        return this.woodCollected;
    }

    /** Suma madera recogida y comprueba si ya cumplio la orden */
    public void addWoodCollected(int amount) {
        this.woodCollected += amount;
        if (this.requestedWood > 0 && this.woodCollected >= this.requestedWood) {
            this.completeWoodOrder();
        }
    }

    /** El jugador dijo que SI con una cantidad: arranca la orden */
    public void startWoodOrder(int quantity) {
        this.waitingForAnswer = false;
        this.requestedWood = Math.max(1, quantity);
        this.woodCollected = 0;
        this.orderStartedAt = this.level().getGameTime();
        this.sayToOwner(Component.literal("Va! Te busco " + this.requestedWood + " de madera, dame un momento.")
                .withStyle(ChatFormatting.GREEN));
    }

    /** El jugador dijo que NO: no trabaja un rato y luego vuelve a preguntar */
    public void refuseWoodOrder() {
        this.waitingForAnswer = false;
        // Vuelve a preguntar en 5 minutos (6000 ticks)
        this.askAgainAt = this.level().getGameTime() + 6000;
        this.sayToOwner(Component.literal("Ok, cuando quieras me avisas.")
                .withStyle(ChatFormatting.GREEN));
    }

    /** Completo la orden: avisa al jugador y se toma un descanso */
    private void completeWoodOrder() {
        this.requestedWood = 0;
        this.woodCollected = 0;
        // Descansa 3 minutos antes de volver a preguntar (no molesta)
        this.askAgainAt = this.level().getGameTime() + 3600;
        this.sayToOwner(Component.literal("Listo bro! Te junte la madera, esta en mi mochila.")
                .withStyle(ChatFormatting.GREEN));
    }

    /** El jugador no respondio a tiempo: cancela la pregunta */
    private void timeoutAnswer() {
        this.waitingForAnswer = false;
        // Vuelve a preguntar en 2 minutos
        this.askAgainAt = this.level().getGameTime() + 2400;
    }

    /** Envia un mensaje al dueno por el chat (como si lo escribiera el Bro) */
    public void sayToOwner(Component message) {
        if (this.ownerUUID == null) {
            return;
        }
        Player owner = this.level().getPlayerByUUID(this.ownerUUID);
        if (owner != null) {
            // El nombre del Bro en el mensaje, como un jugador
            Component chat = Component.literal("<" + this.getName().getString() + "> ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(message);
            owner.displayClientMessage(chat, false);
        }
    }

    /**
     * Cada cierto tiempo, si hay madera cerca y no tenemos orden,
     * preguntamos al jugador si quiere que talemos.
     */
    private void askForWood() {
        long now = this.level().getGameTime();

        // Si estamos esperando respuesta y se acabo el tiempo, cancelar
        if (this.waitingForAnswer) {
            if (now >= this.answerDeadline) {
                this.timeoutAnswer();
            }
            return;
        }

        // Si hay una orden activa, comprobar que no este atascada
        // (por ejemplo si no hay arboles: se cancela sola a los 2 minutos)
        if (this.requestedWood > 0) {
            if (now - this.orderStartedAt > 2400) {
                this.requestedWood = 0;
                this.woodCollected = 0;
                this.askAgainAt = now + 2400; // vuelve a preguntar en 2 min
                this.sayToOwner(Component.literal("Bro no encontro madera cerca, "
                        + "te aviso cuando haya.")
                        .withStyle(ChatFormatting.GREEN));
            }
            return;
        }

        // Si no toca preguntar aun, no hacer nada
        if (now < this.askAgainAt) {
            return;
        }

        Player owner = this.level().getPlayerByUUID(this.ownerUUID);
        if (owner == null || owner.isSpectator()) {
            return;
        }
        // Solo pregunta si esta en la MISMA dimension que el Bro
        if (owner.level().dimension() != this.level().dimension()) {
            return;
        }

        // Solo pregunta si hay madera cerca (para no molestar en el desierto)
        if (!this.hasWoodNearby(owner)) {
            return;
        }

        // Solo pregunta de dia y con hambre decente
        if (this.level().isNight() || this.hunger < 6) {
            return;
        }

        // Pregunta al jugador
        this.waitingForAnswer = true;
        this.answerDeadline = now + 1200; // 60 segundos para responder
        this.sayToOwner(Component.literal("Ey bro, quieres que te busque madera? "
                + "Responde \"si 8\" (con la cantidad) o \"no\".")
                .withStyle(ChatFormatting.GREEN));
    }

    /** Comprueba si hay troncos de arbol cerca del dueno */
    private boolean hasWoodNearby(Player owner) {
        BlockPos center = owner.blockPosition();
        int radius = 10;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -3; dy <= 8; dy++) {
                    BlockState state = this.level().getBlockState(center.offset(dx, dy, dz));
                    if (!state.isAir() && state.is(net.minecraft.tags.BlockTags.LOGS)) {
                        return true;
                    }
                }
            }
        }
        return false;
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
            // El cerebro decide que deseo ejecutar (v2.0.2: seguir)
            this.taskRunner.tick();

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

            // Sistema de peticiones: pregunta por madera cada ~8 segundos
            if (++this.askTimer >= 160) {
                this.askTimer = 0;
                this.askForWood();
            }

            // Sistema de chat (v1.0.0)
            this.updateChat();

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

    /**
     * El chat del Bro (v1.0.0): saluda al aparecer, avisa del anochecer/
     * amanecer y delata a los creepers que se acercan sigilosamente.
     */
    private void updateChat() {
        Player owner = this.level().getPlayerByUUID(this.ownerUUID);
        if (owner == null || owner.isSpectator()) {
            return;
        }
        // Solo chatea si esta en la misma dimension que su dueno
        if (owner.level().dimension() != this.level().dimension()) {
            return;
        }

        long now = this.level().getGameTime();

        // 1) Saludo la primera vez que aparece en el mundo
        if (!this.greeted) {
            this.greeted = true;
            this.wasNight = this.level().isNight();
            this.chatManager.say(ChatManager.GREETINGS);
        }

        // 2) Avisa cuando anochece o amanece (una vez por cambio)
        boolean isNight = this.level().isNight();
        if (isNight != this.wasNight) {
            this.wasNight = isNight;
            if (isNight) {
                this.chatManager.say(ChatManager.NIGHT_MESSAGES);
            } else {
                this.chatManager.say(ChatManager.DAY_MESSAGES);
            }
        }

        // 3) Delata a los creepers que se acercan al dueno (una vez cada 60s,
        //    escaneando solo cada 200 ticks para no gastar rendimiento)
        if (--this.creeperScanTimer <= 0) {
            this.creeperScanTimer = 200;
            if (now - this.lastCreeperWarnAt > 1200) {
                Creeper creeper = this.findNearbyCreeper(owner);
                if (creeper != null) {
                    this.lastCreeperWarnAt = now;
                    this.chatManager.say(ChatManager.CREEPER_WARNINGS);
                }
            }
        }
    }

    /** Busca un creeper peligrosamente cerca del dueno (radio 5 bloques) */
    private Creeper findNearbyCreeper(Player owner) {
        return this.level().getEntitiesOfClass(Creeper.class,
                owner.getBoundingBox().inflate(5.0D)).stream()
                .filter(c -> !c.isDeadOrDying() && c.isAlive())
                .findFirst()
                .orElse(null);
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
        tag.putBoolean("BroWaitingAnswer", this.waitingForAnswer);
        tag.putLong("BroAskAgainAt", this.askAgainAt);
        tag.putLong("BroAnswerDeadline", this.answerDeadline);
        tag.putInt("BroRequestedWood", this.requestedWood);
        tag.putInt("BroWoodCollected", this.woodCollected);
        tag.putLong("BroOrderStartedAt", this.orderStartedAt);
        // Estado del chat
        tag.putBoolean("BroGreeted", this.greeted);
        tag.putBoolean("BroWasNight", this.wasNight);
        tag.putLong("BroLastCreeperWarn", this.lastCreeperWarnAt);
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
        this.waitingForAnswer = tag.getBoolean("BroWaitingAnswer");
        this.askAgainAt = tag.getLong("BroAskAgainAt");
        this.answerDeadline = tag.getLong("BroAnswerDeadline");
        this.requestedWood = tag.getInt("BroRequestedWood");
        this.woodCollected = tag.getInt("BroWoodCollected");
        this.orderStartedAt = tag.getLong("BroOrderStartedAt");
        // Estado del chat
        this.greeted = tag.getBoolean("BroGreeted");
        this.wasNight = tag.getBoolean("BroWasNight");
        this.lastCreeperWarnAt = tag.getLong("BroLastCreeperWarn");
    }
}
