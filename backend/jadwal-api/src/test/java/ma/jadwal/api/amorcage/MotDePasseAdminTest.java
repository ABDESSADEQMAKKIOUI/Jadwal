package ma.jadwal.api.amorcage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verrouille le refus de démarrage sur un mot de passe de super-admin faible ou publié.
 * <p>
 * L'adresse {@code admin@jadwal.ma} est connue et le dépôt est public : un mot de passe
 * par défaut donnerait à quiconque l'administration de tout le parc d'établissements
 * (voir docs/DECISIONS.md D-018).
 */
class MotDePasseAdminTest {

    @Test
    void refuseUnMotDePasseAbsent() {
        assertThatThrownBy(() -> AmorcageDonnees.verifierMotDePasseAdmin(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JADWAL_ADMIN_PASSWORD");
        assertThatThrownBy(() -> AmorcageDonnees.verifierMotDePasseAdmin("  "))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refuseLesMotsDePasseQuiOntCirculePubliquement() {
        for (String publie : new String[] { "admin123", "demo123", "changeme", "ADMIN123", "password" }) {
            assertThatThrownBy(() -> AmorcageDonnees.verifierMotDePasseAdmin(publie))
                    .as("mot de passe publié : %s", publie)
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void refuseUnMotDePasseTropCourt() {
        assertThatThrownBy(() -> AmorcageDonnees.verifierMotDePasseAdmin("Court1!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("12");
    }

    @Test
    void accepteUnMotDePasseFort() {
        assertThatCode(() -> AmorcageDonnees.verifierMotDePasseAdmin("Tr3s-Long-Et-Unique!42"))
                .doesNotThrowAnyException();
    }
}
