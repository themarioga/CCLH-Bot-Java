package org.themarioga.cclh.bot.constants;

public class ResponseErrorI18n {

    private ResponseErrorI18n() {
        throw new UnsupportedOperationException();
    }

    public static final String COMMAND_DOES_NOT_EXISTS = "Comando incorrecto, escribe /help para ver la ayuda.";
    public static final String COMMAND_SHOULD_BE_ON_PRIVATE = "Este comando debe ser enviado por privado al bot.";
    public static final String COMMAND_SHOULD_BE_ON_GROUP = "Este comando debe ser enviado en un grupo en el que esté el bot.";

    public static final String USER_ALREADY_REGISTERED = "Ya estas registrado. Consulta /help para mas información.";

    public static final String GAME_ALREADY_CREATED = "Ya existe un juego activo en este grupo o el creador tiene un juego activo en otro grupo.";
    public static final String GAME_ALREADY_FILLED = "Ya se ha superado el número máximo de jugadores.";
    public static final String GAME_ALREADY_STARTED = "La partida ya está iniciada.";
    public static final String GAME_ONLY_CREATOR_CAN_CONFIGURE = "Solo el creador de la partida puede configurarla.";
    public static final String GAME_ONLY_CREATOR_CAN_DELETE = "Solo el creador de la partida puede borrarla.";
    public static final String GAME_ONLY_CREATOR_CAN_START = "Solo el creador de la partida puede iniciarla.";
    public static final String GAME_DOESNT_EXISTS = "No hay ninguna partida iniciada en este grupo.";
    public static final String GAME_USER_DOESNT_EXISTS = "No estas registrado. Escribeme /start por privado.";

    public static final String PLAYER_NO_GAMES = "No tienes ninguna partida activa.";
    public static final String PLAYER_ALREADY_JOINED = "Ya estas participando en esta partida.";
    public static final String PLAYER_DOES_NOT_EXISTS = "No estas unido a ninguna partida.";
    public static final String PLAYER_ALREADY_PLAYED_CARD = "Ya has jugado una carta.";
    public static final String PLAYER_ALREADY_VOTED_CARD = "Ya has votado una carta.";
    public static final String PLAYER_ALREADY_VOTED_DELETION = "Ya has votado una carta.";

    public static final String UNKNOWN_ERROR = "Error desconocido";

}
