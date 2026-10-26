package dojo.spring.l3;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Reference solution. postProcessAfterInitialization is where Spring's own auto-proxy creator wraps
 * beans; whatever we return is what other beans get injected.
 */
public class AuditPostProcessor implements BeanPostProcessor {

	private final AuditLog log;

	public AuditPostProcessor(AuditLog log) {
		this.log = log;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		if (!bean.getClass().isAnnotationPresent(Audited.class)) {
			return bean;
		}
		Class<?>[] interfaces = bean.getClass().getInterfaces();
		return Proxy.newProxyInstance(bean.getClass().getClassLoader(), interfaces, (proxy, method, args) -> {
			if (method.getDeclaringClass() != Object.class) {
				log.entries.add(beanName + "." + method.getName());
			}
			try {
				return method.invoke(bean, args);
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		});
	}

}
