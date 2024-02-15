package org.themarioga.cclh.bot.constants;

import org.themarioga.cclh.commons.enums.GameTypeEnum;

public class ResponseMessageI18n {

	private ResponseMessageI18n() {
		throw new UnsupportedOperationException();
	}

	// Game constants
	public static final String GAME_CREATING = "Un momentito, estoy creando la partida...";
	public static final String GAME_STARTING = "Comenzando la partida...";

	public static final String GAME_CREATED_GROUP = """
		¡Ya he creado la partida!
		El modo de juego es <b>{0}</b>.
		El mazo de cartas seleccionado es <b>{1}</b>.
		El número de puntos para ganar es <b>{2}</b>.
		El número máximo de jugadores es <b>{3}</b>.
		""";
	public static final String GAME_CREATED_CURRENT_PLAYER_NUMBER = "El número actual de jugadores que se han unido es <b>{0}</b>.";
	public static final String GAME_CREATED_CURRENT_VOTE_DELETION_NUMBER = "<b>{0}</b> jugador/es han votado borrar la partida.";
	public static final String GAME_SELECT_CARD = """
			La carta negra de esta ronda es <b>{0}</b>
			
			Ahora los jugadores jugaran sus cartas blancas por privado.
			""";
	public static final String GAME_VOTE_CARD = """
			La carta negra de esta ronda es <b>{0}</b>
			
			Los jugadores eligieron las siguientes cartas blancas:
			{1}
			
			Ahora los jugadores votaran por privado.
			""";
	public static final String GAME_END_ROUND = """
			¡Enhorabuena <b>{0}</b>! Tu carta <b>{1}</b> ha ganado la ronda.
			
			La carta negra de esta ronda era <b>{2}</b>
			
			Los jugadores eligieron las siguientes cartas blancas:
			{3}
			
			Las puntuaciones son las siguientes:
			{4}
			""";
	public static final String GAME_END_GAME = "¡Fin de la partida! El ganador del juego es... <b>{0}</b>. ¡¡¡Enhorabuena!!!";
	public static final String GAME_DELETED = "Se ha borrado la partida.";

	// Player constants
	public static final String PLAYER_WELCOME = """
		¡Bienvenido! Acabo de añadirte a mi base de datos.
		Siempre que necesites ayuda puedes escribir /help
		¡Gracias por unirte!""";
	public static final String PLAYER_CREATED_GAME = "He creado la partida en el grupo, puedes configurarla allí";
	public static final String PLAYER_JOINING = "Un momentito, estoy intentando unirte a la partida...";
	public static final String PLAYER_JOINED = "Te has unido a la partida.";
	public static final String PLAYER_SELECT_CARD = """
			La carta negra de esta ronda es:
			
			{0}
			
			Debes elegir una carta blanca de las siguientes:
			""";
	public static final String PLAYER_SELECTED_CARD = """
			La carta negra de esta ronda es:
			
			{0}
			
			Has elegido la carta blanca:
			
			{1}
			""";
	public static final String PLAYER_VOTE_CARD = """
			La carta negra de esta ronda es:
			
			{0}
			
			Has elegido la carta blanca:
			
			{1}
			
			Debes votar una carta blanca de las siguientes:
			""";
	public static final String PLAYER_VOTED_CARD = """
			La carta negra de esta ronda es:
			
			{0}
			
			Has elegido la carta blanca:
			
			{1}
			
			Has votado la carta blanca:
			
			{2}
			""";

	public static final String PLAYER_PLAYING = "Jugando carta...";
	public static final String PLAYER_VOTING = "Votando carta...";
	public static final String PLAYER_VOTED_DELETION = "Has votado borrar la partida";

	public static final String HELP = "Esta es la ayuda";

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
