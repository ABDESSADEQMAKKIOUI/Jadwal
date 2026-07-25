package ma.jadwal.api.amorcage;

import ma.jadwal.abonnement.service.PlanService;
import ma.jadwal.referentiel.service.UtilisateurService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Amorçage des données au démarrage : compte super-admin et plans par défaut.
 */
@Component
public class AmorcageDonnees implements ApplicationRunner {

    private static final Logger journal = LoggerFactory.getLogger(AmorcageDonnees.class);
    private static final String EMAIL_ADMIN = "admin@jadwal.ma";

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
        this.utilisateurService = utilisateurService;
        this.planService = planService;
        this.amorcageDemo = amorcageDemo;
        this.motDePasseAdmin = motDePasseAdmin;
        this.demo = demo;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (utilisateurService.chercherParEmail(EMAIL_ADMIN).isEmpty()) {
            utilisateurService.creerSuperAdmin(EMAIL_ADMIN, "Administrateur JADWAL", motDePasseAdmin);
            journal.info("Compte super-admin créé : {}", EMAIL_ADMIN);
        }
        if (planService.compter() == 0) {
            planService.creer("ESSENTIEL", "Essentiel", new BigDecimal("9900.00"), "Jusqu'à 20 classes");
            planService.creer("PREMIUM", "Premium", new BigDecimal("14900.00"),
                    "Classes illimitées + support prioritaire");
            journal.info("Plans d'abonnement par défaut créés (ESSENTIEL, PREMIUM)");
        }
        if (demo) {
            amorcageDemo.amorcer();
        }
    }
}
