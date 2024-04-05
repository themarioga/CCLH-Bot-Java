package org.themarioga.cclh.bot.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.themarioga.cclh.commons.models.Lang;
import org.themarioga.cclh.commons.models.User;

import java.util.Collection;
import java.util.List;

public class CCLHUserDetails implements UserDetails {

	private final Long id;
	private final String name;
	private final Lang lang;
	private final CCLHUserRole role;

	public CCLHUserDetails(User user, CCLHUserRole role) {
		this.id = user.getId();
		this.name = user.getName();
		this.lang = user.getLang();
		this.role = role;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		if (this.role == CCLHUserRole.ADMIN) {
			return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"));
		}
		return List.of(new SimpleGrantedAuthority("ROLE_USER"));
	}

	public Long getId() {
		return id;
	}

	@Override
	public String getUsername() {
		return name;
	}

	public Lang getLang() {
		return lang;
	}

	@Override
	public String getPassword() {
		return null;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

}
