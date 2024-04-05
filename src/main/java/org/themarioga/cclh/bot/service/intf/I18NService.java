package org.themarioga.cclh.bot.service.intf;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.themarioga.cclh.commons.exceptions.ApplicationException;
import org.themarioga.cclh.commons.models.Lang;

import java.util.List;

public interface I18NService {
	Lang getLanguage(String lang);

	List<Lang> getLanguages();

	String get(String tag, String lang);

	String get(String tag);

	String get(String tag, Lang lang);
}
