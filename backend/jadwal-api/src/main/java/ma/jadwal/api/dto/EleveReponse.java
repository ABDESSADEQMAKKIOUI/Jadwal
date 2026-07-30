package ma.jadwal.api.dto;

import java.time.LocalDate;

/**
 * Représentation JSON d'un élève (contrat ELEVE_JSON).
 * <p>
 * Sert aussi de forme d'échange pour les lignes d'un import Massar : dans ce cas
 * les champs non renseignés par le fichier restent nuls, et {@code id} n'est
 * fourni que pour une ligne rapprochée d'un élève déjà inscrit. L'identifiant
 * transmis par le client n'est JAMAIS utilisé pour retrouver un élève : la
 * validation d'un import repasse toujours par le code Massar de l'établissement
 * du jeton.
 */
public record EleveReponse(
        Long id,
        String codeMassar,
        String nom,
        String prenom,
        String nomAr,
        String prenomAr,
        LocalDate dateNaissance,
        String lieuNaissance,
        String sexe,
        String statut,
        Long groupeId,
        String groupeLibelle,
        String niveauLibelle,
        String tuteurNom,
        String tuteurTelephone) {
}
