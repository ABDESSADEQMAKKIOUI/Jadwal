package ma.jadwal.api.securite;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verrouille le refus de démarrage sur un secret de signature faible ou publié.
 * <p>
 * Ce contrôle existe parce que le dépôt est public : une valeur de repli codée en
 * dur y serait lisible par quiconque, et permettrait de forger un jeton de
 * directeur de n'importe quel établissement (voir docs/DECISIONS.md D-018).
 * Le mot de passe admin est couvert par {@code MotDePasseAdminTest}.
 */
class SecretsDemarrageTest {

    @Test
    void refuseUnSecretJwtAbsent() {
        assertThatThrownBy(() -> JwtService.verifierSecret(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JADWAL_JWT_SECRET");
        assertThatThrownBy(() -> JwtService.verifierSecret("   "))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refuseLeSecretJwtQuiACirculePubliquement() {
        assertThatThrownBy(() ->
                JwtService.verifierSecret("jadwal-dev-secret-change-me-0123456789abcdef"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("compromise");
    }

    @Test
    void refuseUnSecretJwtTropCourt() {
        // 31 octets : un cran sous le minimum de 32.
        assertThatThrownBy(() -> JwtService.verifierSecret("a".repeat(31)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    @Test
    void accepteUnSecretJwtFort() {
        assertThatCode(() -> JwtService.verifierSecret("Zx4Q8vK2mN7pR1sT5wY9bC3dF6gH0jL8nP2qS4uV6xZ8aB0c"))
                .doesNotThrowAnyException();
    }
}
