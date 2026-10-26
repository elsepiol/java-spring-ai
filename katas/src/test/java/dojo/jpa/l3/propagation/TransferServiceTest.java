package dojo.jpa.l3.propagation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dojo.jpa.support.JpaKataTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Real transactions (NOT_SUPPORTED: no test transaction), so commits and rollbacks are what production does. */
@Tag("jpa")
@Tag("L3")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TransferServiceTest extends JpaKataTest {

	/** Picks up TransferService and any helper beans you add to the package. */
	@TestConfiguration(proxyBeanMethods = false)
	@ComponentScan(basePackageClasses = TransferService.class)
	static class ScanThePackage {
	}

	@Autowired
	TransferService transfers;

	@Autowired
	AccountRepository accounts;

	@Autowired
	JdbcTemplate jdbc;

	@BeforeEach
	void accounts() {
		jdbc.update("DELETE FROM jpa_audit");
		jdbc.update("DELETE FROM jpa_account");
		accounts.save(new Account(1, "Ann", 1_000));
		accounts.save(new Account(2, "Bob", 0));
	}

	private long balance(long id) {
		return jdbc.queryForObject("SELECT balance_cents FROM jpa_account WHERE id = ?", Long.class, id);
	}

	@Test
	void aSuccessfulTransferMovesMoneyAndIsAudited() {
		transfers.transfer(1, 2, 300);
		assertThat(balance(1)).isEqualTo(700);
		assertThat(balance(2)).isEqualTo(300);
		assertThat(jdbc.queryForList("SELECT action FROM jpa_audit", String.class)).containsExactly("transfer 1->2 300");
	}

	@Test
	void aFailedTransferChangesNoBalanceButIsStillAudited() {
		assertThatThrownBy(() -> transfers.transfer(1, 2, 5_000)).isInstanceOf(IllegalStateException.class);
		assertThat(balance(1)).isEqualTo(1_000);
		assertThat(balance(2)).isZero();
		assertThat(jdbc.queryForList("SELECT action FROM jpa_audit", String.class)).containsExactly("transfer 1->2 5000");
	}

}
