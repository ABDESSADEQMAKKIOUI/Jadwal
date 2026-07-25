package ma.jadwal.api.dto;

public record EtablissementEcoleReponse(
        Long id,
        String nom,
        String code,
        String ville,
        String telephone,
        String email,
        String statut) {
}
