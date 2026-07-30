package ma.jadwal.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Deuxième temps de l'import : les lignes retenues par l'utilisateur après
 * lecture du rapport d'analyse.
 *
 * @param lignes               lignes à écrire ; les lignes en ERREUR sont ignorées
 * @param mettreAJourExistants met à jour les élèves déjà inscrits (champs
 *                             renseignés uniquement) au lieu de les ignorer ;
 *                             absent ou nul = ne rien mettre à jour
 */
public record ValidationImportRequete(
        @NotEmpty(message = "Aucune ligne à importer")
        @Size(max = 5000, message = "Un import est limité à 5000 lignes par lot")
        List<LigneImportEleve> lignes,
        Boolean mettreAJourExistants) {

    /** Vrai uniquement si le client a explicitement demandé la mise à jour. */
    public boolean miseAJourDemandee() {
        return Boolean.TRUE.equals(mettreAJourExistants);
    }
}
