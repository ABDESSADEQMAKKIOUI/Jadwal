package ma.jadwal.api.dto;

import ma.jadwal.referentiel.entite.StatutEtablissement;

/**
 * Modification partielle : seuls les champs non nuls sont appliqués.
 */
public record ModificationEtablissementRequete(
        String nom,
        String ville,
        String telephone,
        String email,
        StatutEtablissement statut) {
}
