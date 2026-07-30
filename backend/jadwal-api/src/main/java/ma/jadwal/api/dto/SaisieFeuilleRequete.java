package ma.jadwal.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ma.jadwal.scolarite.entite.DemiJournee;

import java.time.LocalDate;
import java.util.List;

/**
 * Saisie en lot d'une feuille d'appel. La feuille fait foi : seuls les élèves
 * signalés y figurent, et toute saisie antérieure du même contexte qui n'y
 * figure plus est supprimée. Rejouer la même feuille ne crée aucun doublon.
 *
 * <p>Aucun {@code etablissementId} ici, volontairement : il vient du JWT.
 *
 * @param demiJournee MATIN par défaut
 * @param seanceId    séance appelée, {@code null} pour un appel de demi-journée
 * @param saisies     élèves signalés ; une ligne de {@code type} nul déclare
 *                    l'élève présent et efface sa saisie
 */
public record SaisieFeuilleRequete(
        @NotNull(message = "Le groupe est obligatoire") Long groupeId,
        @NotNull(message = "La date est obligatoire") LocalDate date,
        DemiJournee demiJournee,
        Long seanceId,
        @Valid @Size(max = 500, message = "Une feuille d'appel ne peut pas dépasser 500 lignes")
        List<LigneAppelRequete> saisies) {
}
