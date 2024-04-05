package org.themarioga.cclh.bot.security;

public enum CCLHUserRole {
	ADMIN("admin"),
	USER("user");

	private final String role;

	CCLHUserRole(String role) {
		this.role = role;
	}

	public String getValue() {
		return role;
	}
}
