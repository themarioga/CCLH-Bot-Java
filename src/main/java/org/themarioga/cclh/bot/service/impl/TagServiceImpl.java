package org.themarioga.cclh.bot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.themarioga.cclh.bot.service.intf.TagService;
import org.themarioga.cclh.commons.dao.intf.TagDao;
import org.themarioga.cclh.commons.models.Lang;
import org.themarioga.cclh.commons.models.Tag;
import org.themarioga.cclh.commons.services.intf.LanguageService;

import java.util.List;

@Service
public class TagServiceImpl implements TagService {

	private TagDao tagDao;
	private LanguageService languageService;

	@Autowired
	public TagServiceImpl(TagDao tagDao, LanguageService languageService) {
		this.tagDao = tagDao;
		this.languageService = languageService;
	}

	@Override
	public List<Tag> getTagsByLang(String lang) {
		Lang language = languageService.getLanguage(lang);
		if (language == null) language = languageService.getDefaultLanguage();

		return tagDao.getTagsByLang(language);
	}

}
