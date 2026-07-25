package ma.jadwal.api.dto;

import java.time.LocalDate;

public record AbonnementActifReponse(
        Long id,
        String planNom,
        LocalDate dateDebut,
        LocalDate dateFin,
        String statut) {
}
