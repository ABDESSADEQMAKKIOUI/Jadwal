package ma.jadwal.api.erreur;

import ma.jadwal.solver.validation.ConflitDur;

import java.util.List;

/**
 * Levée quand un déplacement manuel de séance viole des règles dures (I-04).
 * Convertie en réponse 409 avec la liste des conflits.
 */
public class ConflitsSeanceException extends RuntimeException {

    private final transient List<ConflitDur> conflits;

    public ConflitsSeanceException(List<ConflitDur> conflits) {
        super("Conflits détectés");
        this.conflits = conflits == null ? List.of() : List.copyOf(conflits);
    }

    public List<ConflitDur> getConflits() {
        return conflits;
    }
}
