package com.brotherinblocks.chat;

import com.brotherinblocks.entity.BroEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Random;

/**
 * El sistema de chat del Bro (v1.0.0).
 *
 *  - Pools de frases para cada situacion (saludo, muerte, noche, creeper...)
 *  - Anti-spam: minimo 45 segundos entre mensajes automaticos
 *  - No repite la misma frase dos veces seguidas
 */
public class ChatManager {

    /** Minimo de ticks entre mensajes (900 ticks = 45 segundos) */
    public static final int MIN_MESSAGE_INTERVAL = 900;

    private static final Random RANDOM = new Random();

    /** Frases al aparecer en el mundo (saludo) */
    public static final List<String> GREETINGS = List.of(
            "Ey bro! Que mundo mas guapo, vamos a pasarlo bien.",
            "Buenas! Listo para sobrevivir? Cuenta conmigo.",
            "Que tal? Soy tu bro, juntos esto es pan comido.",
            "Eyyy! Donde nos toco? A sobrevivir!"
    );

    /** Frases cuando el jugador muere */
    public static final List<String> PLAYER_DEATHS = List.of(
            "Brooo te mataron!? Jajaja tranqui, respawnea y vamos.",
            "Uy... eso dolio. A por ellos.",
            "No pasa nada bro, hasta los cracks caen.",
            "Jajaja te vio venir? Respawnea y vamos a por el."
    );

    /** Frases cuando empieza a anochecer */
    public static final List<String> NIGHT_MESSAGES = List.of(
            "Se hace de noche bro, mejor nos refugiamos.",
            "Va a anochecer... no me gustan los esqueletos.",
            "A oscuras me pongo nervioso, busquemos refugio."
    );

    /** Frases cuando amanece */
    public static final List<String> DAY_MESSAGES = List.of(
            "Amanecio! A trabajar bro.",
            "Buen dia! Vamos a picar algo.",
            "Luz del dia, me encanta."
    );

    /** Frases de alerta cuando hay un creeper cerca */
    public static final List<String> CREEPER_WARNINGS = List.of(
            "BROOO ATRAS DE TI UN CREEPER!",
            "CUIDADO CREEPER!",
            "Bro mira atras!! Es un creeper!!"
    );

    /** Frases cuando encuentra recursos trabajando */
    public static final List<String> FOUND_RESOURCES = List.of(
            "Oye, consegui recursos buenos.",
            "Mira lo que encontre!",
            "Junte material, esta en mi mochila."
    );

    /** Frases cuando el jugador consigue un logro */
    public static final List<String> ACHIEVEMENTS = List.of(
            "Bien hecho bro! Yo lo vi todo.",
            "Eso! Asi se hace, crack.",
            "Que pro eres, me lo apunto.",
            "Buena esa! Cuando quieras vamos por mas."
    );

    private final BroEntity bro;
    /** Ticks de juego del ultimo mensaje (para el anti-spam) */
    private long lastMessageAt = -1;
    /** Ultima frase dicha (para no repetirla) */
    private String lastPhrase = "";

    public ChatManager(BroEntity bro) {
        this.bro = bro;
    }

    /**
     * Dice una frase de una lista (una al azar).
     * Devuelve true si la dijo; false si estaba en cooldown (anti-spam).
     */
    public boolean say(List<String> phrases) {
        long now = this.bro.level().getGameTime();
        if (this.lastMessageAt >= 0 && now - this.lastMessageAt < MIN_MESSAGE_INTERVAL) {
            return false; // aun no toca hablar (anti-spam)
        }
        String phrase = this.pickPhrase(phrases);
        this.bro.sayToOwner(Component.literal(phrase).withStyle(ChatFormatting.GREEN));
        this.lastMessageAt = now;
        return true;
    }

    /** Elige una frase sin repetir la ultima */
    private String pickPhrase(List<String> phrases) {
        String phrase;
        int guard = 0;
        do {
            phrase = phrases.get(RANDOM.nextInt(phrases.size()));
        } while (phrase.equals(this.lastPhrase) && ++guard < 10);
        this.lastPhrase = phrase;
        return phrase;
    }

    public long getLastMessageAt() {
        return this.lastMessageAt;
    }
}
