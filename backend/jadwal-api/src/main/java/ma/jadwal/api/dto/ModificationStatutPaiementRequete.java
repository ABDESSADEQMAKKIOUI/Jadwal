package ma.jadwal.api.dto;

import jakarta.validation.constraints.NotNull;
import ma.jadwal.abonnement.entite.StatutPaiement;

public record ModificationStatutPaiementRequete(
        @NotNull(message = "Le statut est obligatoire") StatutPaiement statut) {
}
