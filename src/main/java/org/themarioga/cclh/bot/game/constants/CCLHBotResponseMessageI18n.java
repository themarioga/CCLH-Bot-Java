package org.themarioga.cclh.bot.game.constants;

import org.themarioga.cclh.commons.enums.GameTypeEnum;

public class CCLHBotResponseMessageI18n {

	private CCLHBotResponseMessageI18n() {
		throw new UnsupportedOperationException();
	}

	// Game constants
	public static final String GAME_CREATING = "Un momentito, estoy creando la partida...";

	public static final String GAME_CREATED_GROUP = "¡Ya he creado la partida!";
	public static final String GAME_SELECTED_MODE = "El modo de juego es <b>{0}</b>.";
	public static final String GAME_SELECTED_DICTIONARY = "El mazo de cartas seleccionado es <b>{0}</b>.";
	public static final String GAME_SELECTED_POINTS_TO_WIN = "El número de puntos para ganar es <b>{0}</b>.";
	public static final String GAME_SELECTED_ROUNDS_TO_END = "El número de rondas es <b>{0}</b>.";
	public static final String GAME_SELECTED_MAX_PLAYER_NUMBER = "El número máximo de jugadores es <b>{0}</b>.";

	public static final String GAME_CREATED_CURRENT_PLAYER_NUMBER = "Se han unido estos <b>{0}</b> jugadores:";
	public static final String GAME_CREATED_CURRENT_VOTE_DELETION_NUMBER = "<b>{0}</b> jugador/es han votado borrar la partida.";
	public static final String GAME_SELECT_CARD = """
			<b>Ronda {0}</b>
			
			La carta negra de esta ronda es <b>{1}</b>
			
			Ahora los jugadores jugaran sus cartas blancas por privado.
			""";
	public static final String GAME_VOTE_CARD = """
			<b>Ronda {0}</b>
			
			La carta negra de esta ronda es <b>{1}</b>
			
			Los jugadores eligieron las siguientes cartas blancas:
			<b>{2}</b>
			
			Ahora los jugadores votaran por privado.
			""";
	public static final String GAME_END_ROUND = """
			¡Enhorabuena <b>{0}</b>! Tu carta <b>{1}</b> ha ganado la ronda <b>{2}</b>.
			
			La carta negra de esta ronda era <b>{3}</b>
			
			Los jugadores eligieron las siguientes cartas blancas:
			<b>{4}</b>
			
			Las puntuaciones son las siguientes:
			<b>{5}</b>
			""";
	public static final String GAME_END_GAME = "¡Fin de la partida! El ganador del juego es... <b>{0}</b>. ¡¡¡Enhorabuena!!!";
	public static final String GAME_DELETED = "Se ha borrado la partida.";

	public static final String GAME_DELETION_USER = "Se ha borrado la partida de {0}.";
	public static final String GAME_DELETION_ALL = "Se han borrado todas las partidas.";
	public static final String GAME_DELETION_FORCED = "Lo sentimos. Su partida ha sido borrada por la administración. Puede crear una partida nueva.";

	// Player constants
	public static final String PLAYER_WELCOME = """
		¡Bienvenido! Acabo de añadirte a mi base de datos.
		Siempre que necesites ayuda puedes escribir /help
		¡Gracias por unirte!""";
	public static final String PLAYER_CREATED_GAME = "He creado la partida en el grupo, puedes configurarla allí";
	public static final String PLAYER_JOINING = "Un momentito, estoy intentando unirte a la partida...";
	public static final String PLAYER_JOINED = "Te has unido a la partida.";
	public static final String PLAYER_SELECT_CARD = """
			<b>Ronda {0}</b>
			
			La carta negra de esta ronda es:
			
			<b>{1}</b>
			
			Debes elegir una carta blanca de las siguientes:
			""";
	public static final String PLAYER_SELECTED_CARD = """
			<b>Ronda {0}</b>
			
			La carta negra de esta ronda es:
			
			<b>{1}</b>
			
			Has elegido la carta blanca:
			
			<b>{2}</b>
			""";
	public static final String PLAYER_VOTE_CARD = """
			<b>Ronda {0}</b>
			
			La carta negra de esta ronda es:
			
			<b>{1}</b>
			
			Has elegido la carta blanca:
			
			<b>{2}</b>
			
			Debes votar una carta blanca de las siguientes:
			""";
	public static final String PLAYER_VOTED_CARD = """
			<b>Ronda {0}</b>
			
			La carta negra de esta ronda es:
			
			<b>{1}</b>
			
			Has elegido la carta blanca:
			
			<b>{2}</b>
			
			Has votado la carta blanca:
			
			<b>{3}</b>
			""";

	public static final String PLAYER_VOTED_DELETION = "Has votado borrar la partida";

	public static final String ALL_MESSAGES_SENT = "Se han enviado todos los mensajes.";

	public static final String HELP = """
			Bienvenido a la ayuda de {0} versión {1}
			
			Puedes consultar la ayuda en el siguiente enlace: {2}
			
			Disfrutad del bot y... ¡A jugar!
			
			
			Creado por {3}.
			""";

	public static String getGameTypeName(GameTypeEnum gameTypeEnum) {
		if (gameTypeEnum == GameTypeEnum.DEMOCRACY) {
			return "Democracia";
		} else if (gameTypeEnum == GameTypeEnum.CLASSIC) {
			return "Clásico";
		} else if (gameTypeEnum == GameTypeEnum.DICTATORSHIP) {
			return "Dictadura";
		} else {
			return null;
		}
	}

}
