package dojo.spring.l2.payments;

/** Given. */
public interface PaymentGateway {
	String charge(long cents);
}
