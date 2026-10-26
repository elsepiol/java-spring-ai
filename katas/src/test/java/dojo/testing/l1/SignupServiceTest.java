package dojo.testing.l1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests with Mockito: the collaborators are mocks, so only SignupService's own logic runs. */
@Tag("testing")
@Tag("L1")
@ExtendWith(MockitoExtension.class)
class SignupServiceTest {

	@Mock
	UserAccounts accounts;

	@Mock
	Mailer mailer;

	@Captor
	ArgumentCaptor<Mailer.Email> sentMail;

	private SignupService service() {
		return new SignupService(accounts, mailer, raw -> "hashed:" + raw.length());
	}

	@Test
	void createsTheAccountWithTheHashAndAWelcomeMail() {
		when(accounts.emailTaken("ada@example.com")).thenReturn(false);
		when(accounts.create("ada@example.com", "hashed:21")).thenReturn(42L);

		long id = service().signUp("  Ada@Example.COM ", "correct horse battery");

		assertThat(id).isEqualTo(42L);
		verify(mailer).send(sentMail.capture()); // exactly once
		assertThat(sentMail.getValue().to()).isEqualTo("ada@example.com");
		assertThat(sentMail.getValue().subject()).isEqualTo("Welcome!");
		assertThat(sentMail.getValue().body()).contains("42");
	}

	@Test
	void neverStoresTheRawPassword() {
		when(accounts.create(anyString(), anyString())).thenReturn(1L);
		service().signUp("ada@example.com", "correct horse battery");
		verify(accounts, never()).create(anyString(), org.mockito.ArgumentMatchers.eq("correct horse battery"));
	}

	@Test
	void rejectsInvalidInputWithoutTouchingAnything() {
		assertThatIllegalArgumentException().isThrownBy(() -> service().signUp("  ", "correct horse battery"));
		assertThatIllegalArgumentException().isThrownBy(() -> service().signUp("ada@example.com", "short"));
		assertThatIllegalArgumentException().isThrownBy(() -> service().signUp(null, "correct horse battery"));
		verifyNoInteractions(accounts, mailer);
	}

	@Test
	void refusesTakenEmails() {
		when(accounts.emailTaken("ada@example.com")).thenReturn(true);

		assertThatThrownBy(() -> service().signUp("ada@example.com", "correct horse battery"))
			.isInstanceOf(SignupService.EmailTakenException.class);

		verify(accounts, never()).create(anyString(), anyString());
		verifyNoInteractions(mailer);
	}

	@Test
	void aFailingMailerDoesNotFailTheSignup() {
		when(accounts.create(anyString(), anyString())).thenReturn(7L);
		doThrow(new IllegalStateException("SMTP down")).when(mailer).send(any());

		assertThat(service().signUp("ada@example.com", "correct horse battery")).isEqualTo(7L);
	}

}
