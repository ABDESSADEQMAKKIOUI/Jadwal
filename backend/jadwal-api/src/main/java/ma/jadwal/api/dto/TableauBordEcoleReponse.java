package ma.jadwal.api.dto;

import java.util.List;

public record TableauBordEcoleReponse(
        EtablissementEcoleReponse etablissement,
        AbonnementEcoleReponse abonnement,
        List<PaiementReponse> paiements) {
}
