package xyz.letzcollab.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.letzcollab.backend.dto.user.UserResponse;
import xyz.letzcollab.backend.dto.user.UserUpdateRequest;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.global.exception.CustomException;
import xyz.letzcollab.backend.repository.UserRepository;

import java.util.UUID;

import static xyz.letzcollab.backend.global.exception.ErrorCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public UserResponse getMyInfo(UUID publicId) {
		User user = userRepository.findByPublicId(publicId)
								  .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
		return UserResponse.from(user);
	}

	public UserResponse updateMyInfo(UUID publicId, UserUpdateRequest request) {
		User user = userRepository.findByPublicId(publicId)
								  .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

		user.updateProfile(request.name(), request.phoneNumber());

		return UserResponse.from(user);
	}

	public void withdraw(UUID publicId) {
		User user = userRepository.findByPublicId(publicId)
								  .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

		user.delete();
	}
}
