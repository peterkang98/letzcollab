package xyz.letzcollab.backend;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import xyz.letzcollab.backend.dto.workspace.WorkspaceResponse;
import xyz.letzcollab.backend.entity.User;
import xyz.letzcollab.backend.repository.UserRepository;
import xyz.letzcollab.backend.service.WorkspaceService;

import javax.crypto.SecretKey;
import java.io.PrintWriter;
import java.util.Date;

@Tag("data-generation")
@SpringBootTest
@ActiveProfiles("local")
public class LoadTestDataGenerator {
	@Autowired
	UserRepository userRepository;

	@Autowired
	WorkspaceService workspaceService;

	private final SecretKey key;
	private static final long accessTokenValidityInMs = 1000L * 60 * 60 * 24 * 60;	// 2개월

	public LoadTestDataGenerator(@Value("${jwt.secret}") String secretKey){
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
		key = Keys.hmacShaKeyFor(keyBytes);
	}

	@Test
	void generateTokens() throws Exception {
		try (PrintWriter writer = new PrintWriter("tokens.txt")) {
			for (int i = 1; i <= 2000; i++) {
				String email = "loadtest" + i + "@letzcollab.xyz";

				User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음"));

				Date now = new Date();
				Date expiryDate = new Date(now.getTime() + accessTokenValidityInMs);

				String token = Jwts.builder()
								   .subject(user.getPublicId().toString())
								   .claim("email", email)
								   .claim("role", user.getRole().getAuthority())
								   .issuedAt(now)
								   .expiration(expiryDate)
								   .signWith(key)
								   .compact();

				writer.println(token);
			}
		}
	}

	@Test
	void generateWorkspacePublicIds() throws Exception {
		try (PrintWriter writer = new PrintWriter("workspacePublicIds.txt")) {
			for (int i = 1; i <= 2000; i++) {
				String email = "loadtest" + i + "@letzcollab.xyz";

				User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음"));
				WorkspaceResponse firstWorkspace = workspaceService.getMyWorkspaces(user.getPublicId()).getFirst();
				String workspacePublicId = firstWorkspace.publicId().toString();
				writer.println(workspacePublicId);
			}
		}
	}
}
