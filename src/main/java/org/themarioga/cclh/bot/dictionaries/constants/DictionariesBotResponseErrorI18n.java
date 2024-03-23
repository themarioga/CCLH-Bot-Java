package org.themarioga.cclh.bot.dictionaries.constants;

public class DictionariesBotResponseErrorI18n {

	private DictionariesBotResponseErrorI18n() {
		throw new UnsupportedOperationException();
	}

	public static final String DICTIONARY_ALREADY_EXISTS = "Ya existe un diccionario con este nombre.";

	public static final String DICTIONARY_NOT_FOUND = "Ese diccionario no existe.";

	public static final String DICTIONARY_NOT_YOURS = "Ese diccionario no te pertenece.";

	public static final String DICTIONARY_ALREADY_PUBLISHED = "No puedes editar un diccionario que ya está publicado. Despublicalo para poder editarlo.";

	public static final String DICTIONARY_SHARED = "El diccionario está compartido, contacta a {0} para borrarlo.";

	public static final String COLLABORATOR_ADD_USER_DOESNT_EXISTS = "El usuario que intentas añadir como colaborador es incorrecto o no está registrado en el bot.";

	public static final String COLLABORATOR_REMOVE_USER_DOESNT_EXISTS = "El usuario que intentas eliminar como colaborador es incorrecto o no es colaborador en este diccionario.";

	public static final String COLLABORATOR_ADD_ALREADY_EXISTS = "El usuario que intentas añadir ya es colaborador en este diccionario.";

	public static final String COLLABORATOR_ADD_MAX_REACHED = "Has alcanzado el número máximo de colaboradores y no puedes añadir más.";

}
