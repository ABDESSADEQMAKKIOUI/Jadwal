package ma.jadwal.api.amorcage;

import ma.jadwal.abonnement.entite.ModuleJadwal;
import ma.jadwal.abonnement.service.PlanService;
import ma.jadwal.referentiel.service.UtilisateurService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;

/**
 * Amorçage des données au démarrage : compte super-admin et plans par défaut.
 */
@Component
public class AmorcageDonnees implements ApplicationRunner {

    private static final Logger journal = LoggerFactory.getLogger(AmorcageDonnees.class);
    private static final String EMAIL_ADMIN = "admin@jadwal.ma";

    private static final int LONGUEUR_MINIMALE_MOT_DE_PASSE = 12;

    /** Mots de passe ayant circulé publiquement (dépôt public, README, exemples). */
    private static final Set<String> MOTS_DE_PASSE_COMPROMIS =
            Set.of("admin123", "admin", "demo123", "changeme", "password", "motdepasse");

    private final UtilisateurService utilisateurService;
    private final PlanService planService;
    private final AmorcageDemo amorcageDemo;
    private final String motDePasseAdmin;
    private final boolean demo;

    public AmorcageDonnees(UtilisateurService utilisateurService,
                           PlanService planService,
                           AmorcageDemo amorcageDemo,
                           @Value("${jadwal.admin.password}") String motDePasseAdmin,
                           @Value("${jadwal.demo}") boolean demo) {
        verifierMotDePasseAdmin(motDePasseAdmin);
        this.utilisateurService = utilisateurService;
        this.planService = planService;
        this.amorcageDemo = amorcageDemo;
        this.motDePasseAdmin = motDePasseAdmin;
        this.demo = demo;
    }

    /**
     * Refuse le démarrage plutôt que de créer le super-admin de la plateforme avec
     * un mot de passe publié. L'adresse {@code admin@jadwal.ma} est connue : un mot
     * de passe par défaut donnerait à quiconque l'administration de tout le parc.
     */
    static void verifierMotDePasseAdmin(String motDePasse) {
        if (motDePasse == null || motDePasse.isBlank()) {
            throw new IllegalStateException(
                    "JADWAL_ADMIN_PASSWORD n'est pas défini. Choisissez un mot de passe fort.");
        }
        if (MOTS_DE_PASSE_COMPROMIS.contains(motDePasse.trim().toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("JADWAL_ADMIN_PASSWORD utilise une valeur publiée, "
                    + "donc compromise. Choisissez un mot de passe fort.");
        }
        if (motDePasse.length() < LONGUEUR_MINIMALE_MOT_DE_PASSE) {
            throw new IllegalStateException("JADWAL_ADMIN_PASSWORD fait " + motDePasse.length()
                    + " caractères ; il en faut au moins " + LONGUEUR_MINIMALE_MOT_DE_PASSE + ".");
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        if (utilisateurService.chercherParEmail(EMAIL_ADMIN).isEmpty()) {
            utilisateurService.creerSuperAdmin(EMAIL_ADMIN, "Administrateur JADWAL", motDePasseAdmin);
            journal.info("Compte super-admin créé : {}", EMAIL_ADMIN);
        }
        if (planService.compter() == 0) {
            planService.creer("ESSENTIEL", "Essentiel", new BigDecimal("9900.00"), "Jusqu'à 20 classes",
                    Set.of(ModuleJadwal.PLANNING));
            planService.creer("PREMIUM", "Premium", new BigDecimal("14900.00"),
                    "Classes illimitées + vie scolaire + support prioritaire",
                    Set.of(ModuleJadwal.PLANNING, ModuleJadwal.VIE_SCOLAIRE));
            journal.info("Plans d'abonnement par défaut créés (ESSENTIEL, PREMIUM)");
        }
        if (demo) {
            // Le jeu de démonstration crée un compte directeur à mot de passe faible
            // (demo123) : acceptable car explicitement activé, mais jamais en production.
            journal.warn("JADWAL_DEMO est actif : un établissement de démonstration et un compte "
                    + "à mot de passe faible vont être créés. À n'utiliser QU'en développement.");
            amorcageDemo.amorcer();
        }
    }
}
