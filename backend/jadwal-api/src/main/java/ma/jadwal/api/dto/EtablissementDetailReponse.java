package ma.jadwal.api.dto;

import java.time.Instant;
import java.util.List;

public record EtablissementDetailReponse(
        Long id,
        String nom,
        String code,
        String ville,
        String telephone,
        String email,
        String statut,
        Instant dateCreation,
        List<CompteReponse> comptes,
        List<AbonnementEtablissementReponse> abonnements,
        List<PaiementReponse> paiements) {
}
