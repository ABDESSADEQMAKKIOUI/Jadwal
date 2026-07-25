package ma.jadwal.solver.faisabilite;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ma.jadwal.solver.generation.BesoinSeances;
import ma.jadwal.solver.modele.EnseignantPlan;
import ma.jadwal.solver.modele.MatierePlan;
import ma.jadwal.solver.modele.SallePlan;

import static ma.jadwal.solver.contrainte.FabriqueTest.enseignant;
import static ma.jadwal.solver.contrainte.FabriqueTest.groupe;
import static ma.jadwal.solver.contrainte.FabriqueTest.matiere;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bilan de faisabilité H-01..H-06 : un cas OK et un cas ECHEC par contrôle,
 * avec vérification des messages chiffrés actionnables en français (H-06).
 */
class FaisabiliteServiceTest {

    private final FaisabiliteService service = new FaisabiliteService();

    private static BilanFaisabilite bilan(FaisabiliteRapport rapport, String code) {
        return rapport.bilans().stream()
                .filter(b -> b.code().equals(code))
                .findFirst()
                .orElseThrow();
    }

    private static DonneesFaisabilite donnees(int unitesGrille, List<BesoinSeances> besoins,
            List<EnseignantPlan> enseignants, List<SallePlan> salles,
            Map<Long, Integer> libresMixtes, Map<Long, Integer> quotasMixtes) {
        return new DonneesFaisabilite(unitesGrille, libresMixtes, besoins, enseignants, salles,
                Map.of(1L, "Français", 2L, "SVT", 3L, "Musique"), quotasMixtes);
    }

    @Test
    void h01_grilleParGroupe_okPuisEchec() {
        // 30 unités pour un plafond de 38 (95% de 40) : OK.
        var besoinsOk = List.of(new BesoinSeances(groupe(1), matiere(1), 30, null, null, 0,
                "AUCUN", List.of(), null, null));
        var rapportOk = service.verifier(donnees(40, besoinsOk,
                List.of(enseignant(9, 1)), List.of(), Map.of(), Map.of()));
        assertThat(bilan(rapportOk, "H-01").statut()).isEqualTo("OK");

        // 40 unités > 38 : ECHEC avec message chiffré.
        var besoinsKo = List.of(new BesoinSeances(groupe(1), matiere(1), 40, null, null, 0,
                "AUCUN", List.of(), null, null));
        var rapportKo = service.verifier(donnees(40, besoinsKo,
                List.of(enseignant(9, 1)), List.of(), Map.of(), Map.of()));
        var bilanKo = bilan(rapportKo, "H-01");
        assertThat(bilanKo.statut()).isEqualTo("ECHEC");
        assertThat(bilanKo.message()).contains("40 unités").contains("38").contains("retirez");
        assertThat(rapportKo.global()).isEqualTo("ECHEC");
    }

    @Test
    void h02_chaqueMatiereDemandeeAUnEnseignantHabilite_okPuisEchec() {
        // La matière 1 a un enseignant habilité : OK.
        var besoins = List.of(new BesoinSeances(groupe(1), matiere(1), 10, null, null, 0,
                "AUCUN", List.of(), null, null));
        var rapportOk = service.verifier(donnees(40, besoins,
                List.of(enseignant(9, 1)), List.of(), Map.of(), Map.of()));
        assertThat(bilan(rapportOk, "H-02").statut()).isEqualTo("OK");

        // Personne n'est habilité pour la matière 3 (Musique) : ECHEC.
        var besoinsKo = List.of(new BesoinSeances(groupe(1), matiere(3), 10, null, null, 0,
                "AUCUN", List.of(), null, null));
        var rapportKo = service.verifier(donnees(40, besoinsKo,
                List.of(enseignant(9, 1)), List.of(), Map.of(), Map.of()));
        var bilanKo = bilan(rapportKo, "H-02");
        assertThat(bilanKo.statut()).isEqualTo("ECHEC");
        assertThat(bilanKo.message()).contains("Aucun enseignant habilité").contains("Musique")
                .contains("10 unités").contains("5h00");
    }

    @Test
    void h03_quotasDesHabilitesCouvrentLeBesoin_okPuisEchec() {
        // Besoin SVT de 20 unités, quota habilité de 8 : il manque 12 unités (6h00).
        var besoins = List.of(new BesoinSeances(groupe(1), new MatierePlan(2, "SVT", 4, 3, null,
                Set.of(), 2, 4, false, false), 20, null, null, 0, "AUCUN", List.of(), null, null));
        var petitQuota = enseignant(9, 8, 100, null, false, Map.of(2L, Set.of()), List.of(), List.of());
        var rapportKo = service.verifier(donnees(40, besoins,
                List.of(petitQuota), List.of(), Map.of(), Map.of()));
        var bilanKo = bilan(rapportKo, "H-03");
        assertThat(bilanKo.statut()).isEqualTo("ECHEC");
        assertThat(bilanKo.message()).contains("Il manque 12 unités (6h00) d'enseignant pour SVT");

        // Avec un quota de 30 unités, le besoin est couvert.
        var grandQuota = enseignant(9, 30, 100, null, false, Map.of(2L, Set.of()), List.of(), List.of());
        var rapportOk = service.verifier(donnees(40, besoins,
                List.of(grandQuota), List.of(), Map.of(), Map.of()));
        assertThat(bilan(rapportOk, "H-03").statut()).isEqualTo("OK");
    }

    @Test
    void h03_dedoublementTotalMultiplieLeBesoinParSousGroupe() {
        // 10 unités en dédoublement TOTAL sur 2 sous-groupes = 20 unités d'encadrement (H-02).
        var besoins = List.of(new BesoinSeances(groupe(1), new MatierePlan(2, "SVT", 4, 3, null,
                        Set.of(), 2, 4, false, false), 10, null, null, 0, "TOTAL",
                List.of(new ma.jadwal.solver.modele.GroupePlan(11, "G1", 15, 1L, 1, 1, null),
                        new ma.jadwal.solver.modele.GroupePlan(12, "G2", 15, 1L, 1, 1, null)),
                null, null));
        var quota15 = enseignant(9, 15, 100, null, false, Map.of(2L, Set.of()), List.of(), List.of());
        var rapport = service.verifier(donnees(40, besoins,
                List.of(quota15), List.of(), Map.of(), Map.of()));
        var bilanKo = bilan(rapport, "H-03");
        assertThat(bilanKo.statut()).isEqualTo("ECHEC");
        assertThat(bilanKo.message()).contains("Il manque 5 unités (2h30)");
    }

    @Test
    void h04_capaciteDesSallesParTypeRequis_okPuisEchec() {
        var svtLabo = new MatierePlan(2, "SVT", 4, 3, "LABO", Set.of(), 2, 4, false, false);
        var besoins = List.of(new BesoinSeances(groupe(1), svtLabo, 20, null, null, 0,
                "AUCUN", List.of(), null, null));
        var enseignantSvt = enseignant(9, 2);
        var labo = new SallePlan(1, "Labo SVT", 30, "LABO", Set.of(), "A");

        // Une salle LABO offre 40 unités pour 20 demandées : OK.
        var rapportOk = service.verifier(donnees(40, besoins,
                List.of(enseignantSvt), List.of(labo), Map.of(), Map.of()));
        assertThat(bilan(rapportOk, "H-04").statut()).isEqualTo("OK");

        // Aucune salle LABO : il manque 20 unités (10h00).
        var rapportKo = service.verifier(donnees(40, besoins,
                List.of(enseignantSvt), List.of(), Map.of(), Map.of()));
        var bilanKo = bilan(rapportKo, "H-04");
        assertThat(bilanKo.statut()).isEqualTo("ECHEC");
        assertThat(bilanKo.message()).contains("20 unités").contains("10h00").contains("LABO")
                .contains("Ajoutez");
    }

    @Test
    void h05_enseignantsMixtes_okPuisEchec() {
        var mixte = enseignant(5, 1);
        var besoins = List.of(new BesoinSeances(groupe(1), matiere(1), 10, null, null, 0,
                "AUCUN", List.of(), null, null));

        // 30 unités libres pour 20 attendues : OK.
        var rapportOk = service.verifier(donnees(40, besoins,
                List.of(mixte), List.of(), Map.of(5L, 30), Map.of(5L, 20)));
        assertThat(bilan(rapportOk, "H-05").statut()).isEqualTo("OK");

        // 10 unités libres pour 20 attendues : il manque 10 unités (5h00).
        var rapportKo = service.verifier(donnees(40, besoins,
                List.of(mixte), List.of(), Map.of(5L, 10), Map.of(5L, 20)));
        var bilanKo = bilan(rapportKo, "H-05");
        assertThat(bilanKo.statut()).isEqualTo("ECHEC");
        assertThat(bilanKo.message()).contains("10 unités").contains("5h00").contains("MIXTE");
    }

    @Test
    void h06_chaqueEchecProduitUnMessageChiffreActionnable() {
        // Scénario tout-OK : rapport global OK, aucun message d'échec.
        var besoins = List.of(new BesoinSeances(groupe(1), matiere(1), 10, null, null, 0,
                "AUCUN", List.of(), null, null));
        var rapportOk = service.verifier(donnees(40, besoins,
                List.of(enseignant(9, 1)), List.of(), Map.of(), Map.of()));
        assertThat(rapportOk.global()).isEqualTo("OK");
        assertThat(rapportOk.bilans()).hasSize(5)
                .allSatisfy(b -> assertThat(b.statut()).isEqualTo("OK"));

        // Scénario en échec : chaque bilan ECHEC porte un message chiffré (unités ET heures).
        var besoinsKo = List.of(new BesoinSeances(groupe(1), new MatierePlan(2, "SVT", 4, 3, "LABO",
                Set.of(), 2, 4, false, false), 40, null, null, 0, "AUCUN", List.of(), null, null));
        var rapportKo = service.verifier(donnees(40, besoinsKo, List.of(), List.of(),
                Map.of(), Map.of()));
        assertThat(rapportKo.global()).isEqualTo("ECHEC");
        assertThat(rapportKo.bilans().stream().filter(b -> b.statut().equals("ECHEC")))
                .isNotEmpty()
                .allSatisfy(b -> {
                    assertThat(b.message()).containsPattern("\\d+ unités");
                    assertThat(b.message()).containsPattern("\\d+h[0-3]0");
                });
    }
}
