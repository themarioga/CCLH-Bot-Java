package org.themarioga.cclh.bot.service.intf;

import org.themarioga.cclh.commons.models.Tag;

import java.util.List;

public interface TagService {

	List<Tag> getTagsByLang(String lang);

}
