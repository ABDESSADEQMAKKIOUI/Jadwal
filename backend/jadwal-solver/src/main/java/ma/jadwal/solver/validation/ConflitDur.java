package ma.jadwal.solver.validation;

/**
 * Violation d'une règle DURE détectée lors d'une modification manuelle (I-04).
 *
 * @param regle   code de la règle violée ("B-01", "E-02", ...).
 * @param message explication chiffrée en français.
 */
public record ConflitDur(String regle, String message) {
}
