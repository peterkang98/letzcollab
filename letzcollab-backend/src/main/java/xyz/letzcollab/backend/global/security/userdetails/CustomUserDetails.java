package xyz.letzcollab.backend.global.security.userdetails;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.entity.vo.UserRole;
import xyz.letzcollab.backend.entity.vo.UserStatus;

import java.util.Collection;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {
	private final String name;
	private final String publicId;
	private final String email;
	private final String password;
	private final UserRole role;
	private final UserStatus status;

	public CustomUserDetails(User foundUser) {
		this.name = foundUser.getName();
		this.publicId = foundUser.getPublicId();
		this.email = foundUser.getEmail();
		this.password = foundUser.getPassword();
		this.role = foundUser.getRole();
		this.status = foundUser.getStatus();
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority(this.role.getAuthority()));
	}

	@Override
	public String getPassword() {
		return this.password;
	}

	@Override
	public String getUsername() {
		return this.email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return this.status.isNotLocked();
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return this.status.isEnabled();
	}
}
