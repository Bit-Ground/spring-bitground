package bit.bitgroundspring.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct; // @PostConstruct를 위한 임포트

import java.io.IOException;
import java.io.InputStream;
import java.util.List; // FirebaseApp.getApps()를 위한 임포트

@Configuration
@Slf4j
public class FirebaseConfig {
    
    @Value("${firebase.config.path}")
    private String firebaseConfigPath;
    
    // FirebaseApp 인스턴스를 저장할 정적 변수입니다.
    // 애플리케이션 전체에서 단 한 번만 초기화됩니다.
    private static FirebaseApp firebaseAppInstance;
    
    @PostConstruct
    public void initializeFirebaseApp() {
        // 이미 Firebase 앱이 초기화되었는지 확인합니다.
        // 특히 Spring Boot DevTools를 사용할 때 애플리케이션 재시작 시 중복 초기화를 방지합니다.
        if (firebaseAppInstance == null) {
            // "DEFAULT" 이름으로 이미 초기화된 FirebaseApp이 있는지 확인합니다.
            List<FirebaseApp> apps = FirebaseApp.getApps();
            if (apps != null && !apps.isEmpty()) {
                for (FirebaseApp app : apps) {
                    if (app.getName().equals(FirebaseApp.DEFAULT_APP_NAME)) {
                        firebaseAppInstance = app;
                        log.info("FirebaseApp [DEFAULT]가 이미 존재하여 재사용합니다.");
                        return;
                    }
                }
            }
            
            try {
                // ClassPathResource를 사용하여 클래스패스에서 서비스 계정 키 파일을 로드합니다.
                ClassPathResource serviceAccount = new ClassPathResource(firebaseConfigPath);
                
                // InputStream으로 리소스를 열고 try-with-resources 구문을 사용하여 자동으로 닫히도록 합니다.
                try (InputStream inputStream = serviceAccount.getInputStream()) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(inputStream))
                            .setProjectId("bitground-94600") // Firebase 콘솔에서 확인 가능한 프로젝트 ID를 설정합니다.
                            .build();
                    
                    // FirebaseApp을 초기화하고 정적 변수에 저장합니다.
                    firebaseAppInstance = FirebaseApp.initializeApp(options);
                    log.info("Firebase 애플리케이션이 성공적으로 초기화되었습니다.");
                }
            } catch (IOException e) {
                // 초기화 중 IO 예외가 발생하면 에러를 출력하고 애플리케이션이 시작되지 않도록 합니다.
                log.error("Firebase 초기화 중 에러가 발생했습니다: ", e);
            }
        }
    }
    
    @Bean
    public FirebaseApp firebaseApp() {
        // @PostConstruct 메서드에서 이미 초기화된 인스턴스를 반환합니다.
        return firebaseAppInstance;
    }
}