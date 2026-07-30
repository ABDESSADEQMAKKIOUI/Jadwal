package ma.jadwal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Création d'un élève. L'établissement n'y figure pas : il vient du jeton.
 * {@code sexe} vaut M ou F, {@code statut} INSCRIT, PARTI ou REDOUBLANT
 * (INSCRIT par défaut).
 */
public record CreationEleveRequete(
        @NotBlank(message = "Le code Massar est obligatoire")
        @Size(max = 30, message = "Le code Massar dépasse 30 caractères") String codeMassar,

        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 100, message = "Le nom dépasse 100 caractères") String nom,

        @NotBlank(message = "Le prénom est obligatoire")
        @Size(max = 100, message = "Le prénom dépasse 100 caractères") String prenom,

        @Size(max = 100, message = "Le nom arabe dépasse 100 caractères") String nomAr,
        @Size(max = 100, message = "Le prénom arabe dépasse 100 caractères") String prenomAr,
        LocalDate dateNaissance,
        @Size(max = 120, message = "Le lieu de naissance dépasse 120 caractères") String lieuNaissance,
        String sexe,
        String statut,
        @Size(max = 150, message = "Le nom du tuteur dépasse 150 caractères") String tuteurNom,
        @Size(max = 30, message = "Le téléphone du tuteur dépasse 30 caractères") String tuteurTelephone,
        Long groupeId) {
}
