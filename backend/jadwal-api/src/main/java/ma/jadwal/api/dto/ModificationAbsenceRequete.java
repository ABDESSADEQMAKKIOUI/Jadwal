package ma.jadwal.api.dto;

import jakarta.validation.constraints.Size;
import ma.jadwal.scolarite.entite.TypeAbsence;

/**
 * Correction d'une saisie existante. Sémantique PATCH : un champ absent ou nul
 * laisse la valeur en place. Un motif vide efface le motif enregistré, seul
 * moyen de le retirer sans le confondre avec « ne pas y toucher ».
 */
public record ModificationAbsenceRequete(
        Boolean justifiee,
        TypeAbsence type,
        @Size(max = 200, message = "Le motif ne peut pas dépasser 200 caractères") String motif) {
}
