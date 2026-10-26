package dojo.sql.l3;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * KATA sql L3 "OnCall" (find the bug) (tracks/04-sql/L3-production.md, section 4: write skew).
 *
 * Rule: every shift must have at least one doctor on call. A doctor may go off call only if
 * someone else stays on call. The code checks the rule inside a transaction, and in every test
 * with one user it works. When the two doctors of a shift both press "go off call" at the same
 * moment, both requests see "there are 2 on call", both go off, and the shift has nobody.
 * This is WRITE SKEW: each transaction writes a different row, so no row conflict stops them.
 *
 * Make goOffCall() correct under concurrency: exactly one of the two may succeed (true), the
 * other gets false. Keep calling the test hook `afterCheck` between your check and your update.
 * (Two classic fixes; the lesson explains both. If you pick SERIALIZABLE, you must retry the
 * transaction when PostgreSQL aborts it with SQLState 40001.)
 */
public final class OnCall {

	static volatile Runnable afterCheck = () -> {
	};

	private OnCall() {
	}

	public static boolean goOffCall(Connection db, long doctorId) throws SQLException {
		db.setAutoCommit(false);
		try {
			long shift;
			int onCall;
			try (PreparedStatement ps = db.prepareStatement(
					"SELECT d.shift_id, (SELECT count(*) FROM doctors o WHERE o.shift_id = d.shift_id AND o.on_call) "
							+ "FROM doctors d WHERE d.id = ?")) {
				ps.setLong(1, doctorId);
				try (ResultSet rs = ps.executeQuery()) {
					rs.next();
					shift = rs.getLong(1);
					onCall = rs.getInt(2);
				}
			}
			afterCheck.run();
			if (onCall < 2) {
				db.rollback();
				return false;
			}
			try (PreparedStatement ps = db.prepareStatement("UPDATE doctors SET on_call = false WHERE id = ? AND shift_id = ?")) {
				ps.setLong(1, doctorId);
				ps.setLong(2, shift);
				ps.executeUpdate();
			}
			db.commit();
			return true;
		} catch (SQLException | RuntimeException e) {
			db.rollback();
			throw e;
		} finally {
			db.setAutoCommit(true);
		}
	}

}
