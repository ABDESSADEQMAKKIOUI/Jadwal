package ma.jadwal.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ma.jadwal.scolarite.entite.TypeAbsence;

/**
 * Une ligne soumise depuis la feuille d'appel.
 *
 * @param type      {@code null} signifie <strong>présent</strong> : la saisie
 *                  éventuelle de cet élève sera supprimée
 * @param justifiee {@code null} vaut « non justifiée »
 * @param motif     laissé vide, aucun motif n'est enregistré
 */
public record LigneAppelRequete(
        @NotNull(message = "L'élève est obligatoire") Long eleveId,
        TypeAbsence type,
        Boolean justifiee,
        @Size(max = 200, message = "Le motif ne peut pas dépasser 200 caractères") String motif) {

    public boolean present() {
        return type == null;
    }

    public boolean justifieeOuFaux() {
        return Boolean.TRUE.equals(justifiee);
    }
}
