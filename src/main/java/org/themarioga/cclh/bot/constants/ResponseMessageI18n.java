package org.themarioga.cclh.bot.constants;

import org.themarioga.cclh.commons.enums.GameTypeEnum;

public class ResponseMessageI18n {

	private ResponseMessageI18n() {
		throw new UnsupportedOperationException();
	}

	public static final String WELCOME = """
		¡Bienvenido! Acabo de añadirte a mi base de datos.
		Siempre que necesites ayuda puedes escribir /help
		¡Gracias por unirte!""";
	public static final String GAME_CREATING = "Un momentito, estoy creando la partida...";
	public static final String GAME_CREATED_GROUP_EMPTY = """
		¡Ya he creado la partida!
		El modo de juego es {0}.
		El mazo de cartas seleccionado es {1}.
		El número de puntos para ganar es {2}.
		El número máximo de jugadores es {3}.
		""";
	public static final String GAME_CREATED_GROUP = """
		¡Ya he creado la partida!
		El modo de juego es {0}.
		El mazo de cartas seleccionado es {1}.
		El número de puntos para ganar es {2}.
		El número máximo de jugadores es {3}.
		
		El número actual de jugadores que se han unido es {4}.
		""";
	public static final String GAME_CREATED_PRIVATE = "He creado la partida en el grupo, puedes configurarla allí";
	public static final String GAME_DELETED = "Se ha borrado la partida.";
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
