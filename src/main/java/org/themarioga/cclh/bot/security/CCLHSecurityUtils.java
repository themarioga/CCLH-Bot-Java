package org.themarioga.cclh.bot.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.themarioga.cclh.commons.exceptions.user.UserDoesntExistsException;
import org.themarioga.cclh.commons.models.Lang;
import org.themarioga.cclh.commons.models.User;

import java.util.function.Predicate;

public class CCLHSecurityUtils {

	private CCLHSecurityUtils() {
		throw new UnsupportedOperationException();
	}

	public static long getId() {
		CCLHUserDetails cclhUserDetails = getUserDetails();

		return cclhUserDetails.getId();
	}

	public static String getName() {
		CCLHUserDetails cclhUserDetails = getUserDetails();

		return cclhUserDetails != null ? cclhUserDetails.getUsername() : null;
	}

	public static Lang getLang() {
		CCLHUserDetails cclhUserDetails = getUserDetails();

		return cclhUserDetails != null ? cclhUserDetails.getLang() : null;
	}

	public static boolean isAdmin() {
		CCLHUserDetails cclhUserDetails = getUserDetails();

		return cclhUserDetails != null && cclhUserDetails.getAuthorities().stream().anyMatch((Predicate<GrantedAuthority>) grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
	}

	public static boolean isLoggedIn() {
		return getUserDetails() != null;
	}

	public static void setUserDetails(User user, CCLHUserRole cclhUserRole) {
		if (user == null || cclhUserRole == null) throw new UserDoesntExistsException();

		setUserDetails(new CCLHUserDetails(user, cclhUserRole));
	}

	public static void setUserDetails(CCLHUserDetails cclhUserDetails) {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(cclhUserDetails,
				null,
				cclhUserDetails.getAuthorities()));
	}

	public static CCLHUserDetails getUserDetails() {
		if (SecurityContextHolder.getContext().getAuthentication() == null
			|| !(SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CCLHUserDetails))
			return null;
		return (CCLHUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	}

}
