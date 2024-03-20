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

	public static final String DICTIONARIES_RENAME_LIST = """
		Estos son los diccionarios que has creado:
		
		{0}
		
		Ahora respondeme con el número de diccionario que deseas renombrar.""";

	public static final String DICTIONARIES_DELETE_LIST = """
		Estos son los diccionarios que has creado:
		
		{0}
		
		Ahora respondeme con el número de diccionario que deseas borrar.""";

	public static final String DICTIONARIES_MANAGE_CARDS_LIST = """
		Estos son los diccionarios que has creado:
		
		{0}
		
		Ahora respondeme con el número de diccionario del que deseas editar las cartas.""";

	public static final String DICTIONARIES_MANAGE_COLLABORATORS_LIST = """
		Estos son los diccionarios que has creado:
		
		{0}
		
		Ahora respondeme con el número de diccionario del que deseas editar los colaboradores.""";

	public static final String DICTIONARY_CREATE = "¡Vamos a ello! Dime el nombre del diccionario que quieres crear.";
	public static final String DICTIONARY_CREATED = "Tu diccionario se ha creado correctamente, ahora ve al menú para añadir las cartas.";

	public static final String DICTIONARY_RENAME = "¡Vamos a ello! Dime el nuevo nombre del diccionario que quieres renombrar.";
	public static final String DICTIONARY_RENAMED = "Tu diccionario se ha renombrado correctamente.";

	public static final String DICTIONARY_DELETE = """
		<b>¡CUIDADO! Este diccionario está PUBLICADO</b>. Se borrará el diccionario y también las <b>cartas</b> asociadas.
		
		¿Estás completamente seguro de que quieres borrarlo?
		· Responde SI para confirmar.
		· Responde cualquier otra cosa o ignora el mensaje para cancelar.""";

	public static final String DICTIONARY_DELETED = "Tu diccionario se ha borrado correctamente.";
	public static final String DICTIONARY_DELETE_CANCELLED = "Se ha cancelado el borrado del diccionario.";

}
