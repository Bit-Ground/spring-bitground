package bit.bitgroundspring.config;

import com.p6spy.engine.spy.P6SpyOptions;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class P6SpyConfig implements InitializingBean {
    
    @Override
    public void afterPropertiesSet() throws Exception {
        P6SpyOptions.getActiveInstance().setLogMessageFormat(CustomP6SpyFormatter.class.getName());
    }
}