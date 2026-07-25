package ma.jadwal.api.dto;

import java.time.Instant;

public record EtablissementResumeReponse(
        Long id,
        String nom,
        String code,
        String ville,
        String telephone,
        String email,
        String statut,
        Instant dateCreation,
        AbonnementActifReponse abonnementActif) {
}
