package ma.jadwal.scolarite.depot;

import ma.jadwal.scolarite.entite.DemiJournee;
import ma.jadwal.scolarite.entite.TypeAbsence;

import java.time.LocalDate;

/**
 * Projection d'agrégat des statistiques d'absentéisme : un décompte par groupe,
 * jour, nature, contexte de demi-journée et état de justification. C'est la
 * granularité la plus fine dont les séries, les totaux et la répartition par
 * groupe se déduisent, sans jamais charger d'entité.
 *
 * <p>Ne porte que des identifiants techniques : aucune donnée personnelle
 * d'élève ne transite par cette projection.
 *
 * @param groupeId groupe de l'élève, {@code null} si l'élève n'a pas de classe
 */
public record AgregatAbsences(
        Long groupeId,
        LocalDate date,
        TypeAbsence type,
        DemiJournee demiJournee,
        Boolean justifiee,
        Long nombre) {

    /** Nombre de saisies comptabilisées, jamais nul. */
    public long nombreOuZero() {
        return nombre == null ? 0L : nombre;
    }

    public boolean estJustifiee() {
        return Boolean.TRUE.equals(justifiee);
    }

    /**
     * Demi-journées représentées par ce décompte : une absence sur la journée
     * entière en vaut deux.
     */
    public long demiJournees() {
        return nombreOuZero() * (demiJournee == null ? 1L : demiJournee.poids());
    }
}
