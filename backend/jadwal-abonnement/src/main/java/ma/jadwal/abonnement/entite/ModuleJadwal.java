package ma.jadwal.abonnement.entite;

import java.util.Locale;
import java.util.Optional;

/**
 * Fonctionnalités vendues séparément. La liste incluse dans un plan est
 * stockée dans {@code plan_abonnement.modules}, séparée par des virgules.
 */
public enum ModuleJadwal {

    PLANNING("Planning"),
    VIE_SCOLAIRE("Vie scolaire");

    private final String libelle;

    ModuleJadwal(String libelle) {
        this.libelle = libelle;
    }

    public String libelle() {
        return libelle;
    }

    /**
     * Lecture tolérante d'un code stocké en base : la valeur inconnue est ignorée
     * plutôt que de faire échouer tout le chargement de l'abonnement.
     */
    public static Optional<ModuleJadwal> depuisCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(code.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
