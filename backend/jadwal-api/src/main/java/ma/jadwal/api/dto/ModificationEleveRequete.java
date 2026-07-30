package ma.jadwal.api.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Modification partielle d'un élève : tout champ nul est laissé inchangé.
 * <p>
 * Une chaîne vide efface un champ facultatif (nom arabe, tuteur, lieu de
 * naissance…). Comme un {@code groupeId} nul signifie « inchangé », le retrait
 * de classe se demande explicitement avec {@code retirerDuGroupe}.
 * <p>
 * {@code retirerDuGroupe} est un {@link Boolean} et non un {@code boolean} :
 * Jackson 3 refuse un corps où un champ primitif est absent, ce qui interdirait
 * toute modification partielle.
 */
public record ModificationEleveRequete(
        @Size(max = 30, message = "Le code Massar dépasse 30 caractères") String codeMassar,
        @Size(max = 100, message = "Le nom dépasse 100 caractères") String nom,
        @Size(max = 100, message = "Le prénom dépasse 100 caractères") String prenom,
        @Size(max = 100, message = "Le nom arabe dépasse 100 caractères") String nomAr,
        @Size(max = 100, message = "Le prénom arabe dépasse 100 caractères") String prenomAr,
        LocalDate dateNaissance,
        @Size(max = 120, message = "Le lieu de naissance dépasse 120 caractères") String lieuNaissance,
        String sexe,
        String statut,
        @Size(max = 150, message = "Le nom du tuteur dépasse 150 caractères") String tuteurNom,
        @Size(max = 30, message = "Le téléphone du tuteur dépasse 30 caractères") String tuteurTelephone,
        Long groupeId,
        Boolean retirerDuGroupe) {

    /** Vrai uniquement si le client a explicitement demandé le retrait de classe. */
    public boolean retraitDemande() {
        return Boolean.TRUE.equals(retirerDuGroupe);
    }
}
