package dojo.sql.l3;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Reference solution: lock the rows the DECISION depends on. SELECT ... FOR UPDATE on all doctors
 * of the shift makes the second transaction wait until the first commits, and then it re-reads
 * the current rows (PostgreSQL re-checks locked rows in READ COMMITTED), sees one doctor on call,
 * and refuses. "Materialize the conflict": both transactions now touch the same rows.
 *
 * Alternative: run at SERIALIZABLE isolation, which detects the dangerous pattern and aborts one
 * transaction with SQLState 40001; the caller must then retry the whole transaction.
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
			try (PreparedStatement ps = db.prepareStatement("SELECT shift_id FROM doctors WHERE id = ?")) {
				ps.setLong(1, doctorId);
				try (ResultSet rs = ps.executeQuery()) {
					rs.next();
					shift = rs.getLong(1);
				}
			}
			int onCall = 0;
			try (PreparedStatement ps = db.prepareStatement(
					"SELECT on_call FROM doctors WHERE shift_id = ? ORDER BY id FOR UPDATE")) { // ORDER BY: same lock order for everyone
				ps.setLong(1, shift);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						if (rs.getBoolean(1)) {
							onCall++;
						}
					}
				}
			}
			afterCheck.run();
			if (onCall < 2) {
				db.rollback();
				return false;
			}
			try (PreparedStatement ps = db.prepareStatement("UPDATE doctors SET on_call = false WHERE id = ?")) {
				ps.setLong(1, doctorId);
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
