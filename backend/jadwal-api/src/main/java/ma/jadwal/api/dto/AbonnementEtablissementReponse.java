package ma.jadwal.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Abonnement présenté dans le détail d'un établissement (sans les champs établissement).
 */
public record AbonnementEtablissementReponse(
        Long id,
        Long planId,
        String planNom,
        BigDecimal prixAnnuel,
        LocalDate dateDebut,
        LocalDate dateFin,
        String statut,
        BigDecimal totalPaye) {
}
