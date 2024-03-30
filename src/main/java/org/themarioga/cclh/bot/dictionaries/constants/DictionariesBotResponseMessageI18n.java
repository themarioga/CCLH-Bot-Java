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

	public static final String DICTIONARIES_TOGGLE_LIST = """
		Estos son los diccionarios que has creado:
		
		{0}
		
		Ahora respondeme con el número de diccionario que deseas publicar/despublicar.""";

	public static final String DICTIONARIES_SHARE_LIST = """
		Estos son los diccionarios que has creado:
		
		{0}
		
		Ahora respondeme con el número de diccionario que deseas compartir/descompartir.""";

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

	public static final String DICTIONARY_TOGGLED_ON = "Se ha publicado el diccionario {0} correctamente.";
	public static final String DICTIONARY_TOGGLED_OFF = "Se ha despublicado el diccionario {0} correctamente.";

	public static final String DICTIONARY_SHARED_ON_REQUEST = "Se ha solicitado compartir el diccionario {0}.";
	public static final String DICTIONARY_SHARED_OFF_REQUEST = "Se ha solicitado descompartir el diccionario {0}.";
	public static final String DICTIONARY_SHARED_ON = "Se ha compartido el diccionario {0} correctamente.";
	public static final String DICTIONARY_SHARED_OFF = "Se ha descompartido el diccionario {0} correctamente.";
	public static final String DICTIONARY_SHARED_REJECTED = "Se ha rechazado compartir el diccionario {0}.";

	public static final String DICTIONARY_DELETE = """
		<b>¡CUIDADO! Este diccionario está PUBLICADO</b>. Se borrará el diccionario y también las <b>cartas</b> asociadas.
		
		¿Estás completamente seguro de que quieres borrarlo?
		· Responde SI para confirmar.
		· Responde cualquier otra cosa o ignora el mensaje para cancelar.""";

	public static final String DICTIONARY_DELETED = "Tu diccionario se ha borrado correctamente.";
	public static final String DICTIONARY_DELETE_CANCELLED = "Se ha cancelado el borrado del diccionario.";

	public static final String CARDS_MENU = "Estas gestionando las cartas del diccionario: {0}";

	public static final String CARDS_WHITE_LIST = "¡Marchando! Aqui tienes las cartas blancas. (Es posible que se envien en varios mensajes)";
	public static final String CARDS_WHITE_LIST_END = "Esas son todas las cartas blancas.";
	public static final String CARDS_WHITE_CARD_ADD = "Respondeme con el texto de la carta blanca que deseas añadir.";
	public static final String CARDS_WHITE_CARD_ADD_ANOTHER = "Respondeme con el texto de la carta blanca que deseas añadir o escribe :cancel: para volver al menú.";
	public static final String CARDS_WHITE_CARD_ADDED = "Carta blanca añadida correctamente. Llevas {0} de {1}.";
	public static final String CARDS_WHITE_CARD_EDIT = "Respondeme con el número de la carta blanca que deseas editar.";
	public static final String CARDS_WHITE_CARD_EDIT_NEW_TEXT = "Respondeme con el nuevo texto que quieres para esa carta.";
	public static final String CARDS_WHITE_CARD_EDITED = "Carta blanca editada correctamente.";
	public static final String CARDS_WHITE_CARD_DELETE = "Respondeme con el número de la carta blanca que deseas borrar.";
	public static final String CARDS_WHITE_CARD_DELETED = "Carta blanca borrada correctamente.";

	public static final String CARDS_BLACK_LIST = "¡Marchando! Aqui tienes las cartas negras. (Es posible que se envien en varios mensajes)";
	public static final String CARDS_BLACK_LIST_END = "Esas son todas las cartas negras.";
	public static final String CARDS_BLACK_CARD_ADD = "Respondeme con el texto de la carta negra que deseas añadir.";
	public static final String CARDS_BLACK_CARD_ADD_ANOTHER = "Respondeme con el texto de la carta negra que deseas añadir o escribe :cancel: para volver al menú.";
	public static final String CARDS_BLACK_CARD_ADDED = "Carta negra añadida correctamente. Llevas {0} de {1}.";
	public static final String CARDS_BLACK_CARD_EDIT = "Respondeme con el número de la carta blanca que deseas editar.";
	public static final String CARDS_BLACK_CARD_EDIT_NEW_TEXT = "Respondeme con el nuevo texto que quieres para esa carta.";
	public static final String CARDS_BLACK_CARD_EDITED = "Carta blanca editada correctamente.";
	public static final String CARDS_BLACK_CARD_DELETE = "Respondeme con el número de la carta blanca que deseas borrar.";
	public static final String CARDS_BLACK_CARD_DELETED = "Carta blanca borrada correctamente.";

	public static final String COLLABORATORS_MENU = "Estas gestionando los colaboradores del diccionario: {0}";

	public static final String COLLABORATORS_LIST = """
		Estos son los colaboradores de este diccionario:
		
		{0}""";

	public static final String COLLABORATORS_ADD = """
        ¡Vamos a ello! Dime el nombre (p.ej. @cclhbot) o el id (puedes obtenerlo con el comando /getmyid) del usuario que deseas añadir como colaborador.
        
        <b>OJO: El colaborador que desees añadir debe haber iniciado este bot de diccionarios primero (escribiendole /start por privado)</b>
        """;
	public static final String COLLABORATORS_ADDED = "Se ha enviado una invitación de colaborador a {0} correctamente. Ahora deberá aceptar la invitación.";

	public static final String COLLABORATORS_ACCEPT_MESSAGE = "{0} te ha invitado a colaborar en su diccionario {1}.";
	public static final String COLLABORATORS_ACCEPTED_CREATOR = "{0} ha aceptado ser colaborador.";
	public static final String COLLABORATORS_REJECTED_CREATOR = "{0} ha rechazado ser colaborador.";
	public static final String COLLABORATORS_ACCEPTED_MESSAGE = "¡Perfecto! Ya eres colaborador, ahora pide al creador del bot que te active.";
	public static final String COLLABORATORS_REJECTED_MESSAGE = "Has rechazado ser colaborador.";

	public static final String COLLABORATORS_DELETE = "¡Vamos a ello! Dime el nombre (p.ej. @cclhbot) o el id (puedes obtenerlo con el comando /getmyid) del usuario que deseas eliminar como colaborador.";
	public static final String COLLABORATORS_DELETED = "Se ha eliminado el colaborador correctamente.";
	public static final String COLLABORATORS_DELETED_MESSAGE = "Te han eliminado del diccionario {0}";

	public static final String COLLABORATORS_TOGGLE = "¡Vamos a ello! Dime el nombre (p.ej. @cclhbot) o el id (puedes obtenerlo con el comando /getmyid) del usuario que deseas activar/desactivar como colaborador.";
	public static final String COLLABORATORS_TOGGLED_ON = "Se ha activado al colaborador {0} correctamente.";
	public static final String COLLABORATORS_TOGGLED_OFF = "Se ha desactivado al colaborador {0} correctamente.";
	public static final String COLLABORATORS_TOGGLED_ON_MESSAGE = "Has recibido permisos de edición en el diccionario {0}.";
	public static final String COLLABORATORS_TOGGLED_OFF_MESSAGE = "Has perdido permisos de edición en el diccionario {0}.";

	public static final String VOLVER = "<- Volver";

}
