package dojo.spring.l3;

import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * KATA spring L3 "AuditPostProcessor" (tracks/06-spring-core/L3-production.md, section 3: BeanPostProcessors).
 *
 * This is how Spring itself turns @Transactional or @Async into behaviour: a BeanPostProcessor
 * sees every bean after it is created and may RETURN A DIFFERENT OBJECT (a proxy) to be used instead.
 * For every bean whose class is annotated @Audited, return a JDK proxy implementing all the bean's
 * interfaces that records "<beanName>.<methodName>" in the AuditLog before delegating (rethrow the
 * target's original exceptions). Other beans are returned unchanged.
 * Get the AuditLog through the constructor. (Your TimingProxy kata is the same mechanism.)
 */
public class AuditPostProcessor implements BeanPostProcessor {

	public AuditPostProcessor(AuditLog log) {
	}

}
