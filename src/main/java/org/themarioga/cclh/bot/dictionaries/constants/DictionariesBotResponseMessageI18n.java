package org.themarioga.cclh.bot.dictionaries.constants;

public class DictionariesBotResponseMessageI18n {

	private DictionariesBotResponseMessageI18n() {
		throw new UnsupportedOperationException();
	}

	public static final String PLAYER_WELCOME = """
		¡Bienvenido! Acabo de añadirte a mi base de datos.
		Siempre que necesites ayuda puedes escribir /help
		¡Gracias por unirte!""";

	public static final String HELP = """
			Bienvenido a la ayuda de {0} versión {1}
			
			Puedes consultar la ayuda en el siguiente enlace: {2}
			
			¡¡Comparte tus diccionarios!!
			
			
			Creado por {3}.
			""";

	// MENU

	public static final String MAIN_MENU = """
		¡Hola! Soy el bot de diccionarios de Cartas Contra la Humanidad.
		Estas son las acciones que puedes realizar:""";

	public static final String DICTIONARIES_LIST = """
		Estos son los diccionarios que has creado o eres colaborador:
		
		{0}""";

	public static final String DICTIONARIES_EDIT_LIST = """
		Estos son los diccionarios que has creado:
		
		{0}
		
		Ahora respondeme con el número de diccionario que deseas renombrar.""";

	public static final String DICTIONARY_CREATE = "¡Vamos a ello! Dime el nombre del diccionario que quieres crear.";
	public static final String DICTIONARY_CREATED = "Tu diccionario se ha creado correctamente, ahora toca añadir cartas.";

	public static final String DICTIONARY_RENAME = "¡Vamos a ello! Dime el nuevo nombre del diccionario que quieres renombrar.";
	public static final String DICTIONARY_RENAMED = "Tu diccionario se ha renombrado correctamente.";

}
