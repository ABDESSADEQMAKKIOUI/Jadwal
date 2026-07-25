package ma.jadwal.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ma.jadwal.abonnement.entite.ModePaiement;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreationPaiementRequete(
        @NotNull(message = "L'abonnement est obligatoire") Long abonnementId,
        @NotNull(message = "Le montant est obligatoire")
        @Positive(message = "Le montant doit être positif") BigDecimal montant,
        @NotNull(message = "Le mode de paiement est obligatoire") ModePaiement mode,
        String reference,
        @NotNull(message = "La date de paiement est obligatoire") LocalDate datePaiement,
        String note) {
}
